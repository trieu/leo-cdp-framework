package leotech.cdp.model.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.arangodb.ArangoCollection;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.scoring.ProcessorScoreCX;
import leotech.cdp.model.asset.AssetTemplate;
import rfx.core.util.StringUtil;

public final class FeedbackSurveyReport extends FeedbackData  {

	private static final double RATIO_TO_BE_OUTLIER = 0.05;

	@Expose
	String surveyKey = "";
	
	@Expose
	int surveyCount = 0;
	
	@Expose
	String fromDate = "";

	@Expose
	String toDate = "";

	@Expose
	Map<String, SurveyQuestionAnswer> surveyAnswers = new HashMap<>();

	@Expose
	SortedSet<SurveyResult> surveyResults = new TreeSet<SurveyResult>();

	
	@Expose
	Set<String> profileIds;	
	
	@Expose
	int totalScore = 0;
	
	@Expose
	double avgFeedbackScore = 0;
	
	@Expose
	double rankingScore = 0;
	

	@Override
	public ArangoCollection getDbCollection() {
		return dbCollection();
	}

	public FeedbackSurveyReport() {
		this.profileIds = new HashSet<String>();
		this.scoreCX = new ScoreCX();
	}
	
	public FeedbackSurveyReport(AssetTemplate tpl) {
		this.refTemplateId = tpl.getId();
		this.profileIds = new HashSet<String>(1);
		this.surveyCount = 1;
		this.scoreCX = new ScoreCX();
		
		String tplJsonMetadata = tpl.getJsonMetadata();
		if(tplJsonMetadata != null) {
			this.parseSurveyChoices(new JsonObject(tplJsonMetadata));
		}
	}

	public FeedbackSurveyReport(AssetTemplate tpl, int estimateTotalVote, Date createdAt) {
		this.refTemplateId = tpl.getId();
		this.profileIds = new HashSet<String>(estimateTotalVote);
		this.surveyCount = 1;
		this.scoreCX = new ScoreCX();
		this.createdAt = createdAt;
		
		String tplJsonMetadata = tpl.getJsonMetadata();
		if(tplJsonMetadata != null) {
			this.parseSurveyChoices(new JsonObject(tplJsonMetadata));
		}
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.refTemplateId) && StringUtil.isNotEmpty(this.header);
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (dataValidation()) {
			if(StringUtil.isEmpty(timePeriod) && StringUtil.isNotEmpty(fromDate) && StringUtil.isNotEmpty(toDate)) {
				timePeriod = fromDate + "-" + toDate;
			}
			String keyHint = timePeriod + header + group + evaluatedObject  + evaluatedItem + evaluatedPerson + feedbackType + surveyChoicesId + refTemplateId;
			this.id = createId(this.id, keyHint);
			
			StringBuilder s = new StringBuilder();
			s.append("[Survey: ").append(this.header).append("]");
			
			if(StringUtil.isNotEmpty(this.timePeriod)) {
				s.append("[Period: ").append(this.timePeriod).append("]");
			}
			
			if(StringUtil.isNotEmpty(this.group)) {
				s.append("[Group: ").append(this.group).append("]");
			}
			
			if(StringUtil.isNotEmpty(this.group)) {
				s.append("[Object: ").append(this.evaluatedObject).append("]");
			}
			
			if(StringUtil.isNotEmpty(this.evaluatedPerson)) {
				s.append("[Person: ").append(this.evaluatedPerson).append("]");
			}
			
			if(StringUtil.isNotEmpty(this.refProductItemId)) {
				s.append("[Product Item ID: ").append(this.refProductItemId).append("]");
			}
			
			if(StringUtil.isNotEmpty(this.refContentItemId)) {
				s.append("[Content Item ID: ").append(this.refContentItemId).append("]");
			}
			
			if(StringUtil.isNotEmpty(this.refTouchpointId)) {
				s.append("[Touchpoint ID: ").append(this.refTouchpointId).append("]");
			}
			
			this.surveyKey = s.toString();
		} else {
			System.err.println(new Gson().toJson(this));
			throw new IllegalArgumentException("check isReadyForSave is failed ");
		}
		return this.id;
	}
	
	public String getSurveyKey() {
		return surveyKey;
	}
	
	
	@Override
	public String getFeedbackDataType() {
		return feedbackDataType;
	}

	@Override
	public void setFeedbackDataType(String feedbackDataType) {
		this.feedbackDataType = feedbackDataType;
	}

	public final void addSurveyStats(int index, String questionGroup, String question, String selected) {
		SurveyQuestionAnswer answer = new SurveyQuestionAnswer(index, questionGroup, question, selected);
		String answerId = answer.getId();
		SurveyQuestionAnswer theAnswer = this.surveyAnswers.get(answerId);

		if(theAnswer != null) {
			theAnswer.updateResponseCount();
			answer = theAnswer;
		}
		this.surveyAnswers.put(answerId, answer);
		
		this.totalScore += answer.getAnswerScore();
		
		int size = this.surveyAnswers.size();
		if(size > 0 && totalScore > 0) {
			this.avgFeedbackScore = (double)totalScore / size;
			this.avgFeedbackScore = Math.round(avgFeedbackScore * 100.00) / 100.0;
		}
		
		if(this.avgFeedbackScore >=0 && this.avgFeedbackScore <= 5) {
			ScoreCX newScoreCX = ProcessorScoreCX.computeScale5double(this.avgFeedbackScore, this.scoreCX);
			this.scoreCX = newScoreCX;
		}
	}
	
	public void updateAvgScore(double updateAvgScore) {
		this.surveyCount += 1;
		
		this.avgFeedbackScore = (this.avgFeedbackScore + updateAvgScore)/ 2;
		this.avgFeedbackScore = Math.round(this.avgFeedbackScore * 100.00) / 100.0;
		
		if(this.avgFeedbackScore >=0 && this.avgFeedbackScore <= 5) {
			ScoreCX newScoreCX = ProcessorScoreCX.computeScale5double(this.avgFeedbackScore, this.scoreCX);
			this.scoreCX = newScoreCX;
		}
	}
	
	/**
	 * 
	 */
	public final void computeFinalScoreAndResults() {
		
		// compute the surveyResults
		buildSurveyResult();
		
		// ranking score
		computeRankingScore();
	}

	private void computeRankingScore() {
		int totalVoter = this.profileIds.size();
		int totalChoices = this.surveyChoices.size();
		int sentimentScore = this.scoreCX.getSentimentScore();
		if(totalVoter > 0 && totalChoices > 0 && this.surveyCount > 0) {
			this.rankingScore = (this.totalScore / totalChoices) + ((this.avgFeedbackScore * totalVoter)/this.surveyCount);
			
			int outlierSize = (int) Math.ceil(totalVoter * RATIO_TO_BE_OUTLIER);
			// boosting if the group is not an outlier
			if(totalVoter > outlierSize) {
				this.rankingScore = this.rankingScore + (10 * sentimentScore * totalVoter);
			}
			this.rankingScore = Math.round(this.rankingScore * 100.00) / 100.0;
		}
	}

	private void buildSurveyResult() {
		List<SurveyQuestionAnswer> answers = new ArrayList<SurveyQuestionAnswer>(this.surveyAnswers.values());
		Collections.sort(answers);
		Map<String, SurveyResult> map = new HashMap<String, SurveyResult>(answers.size());
		for (SurveyQuestionAnswer a : answers) {
			// the questionKey is used for grouping questions
			String questionKey = a.getQuestionKey();
			SurveyResult r = map.getOrDefault(questionKey, new SurveyResult(a, this.surveyChoices));
			r.setAnswerResults(a);

			map.put(questionKey, r);
			//finalize
			this.surveyResults.remove(r);
			this.surveyResults.add(r);
		}
	}
	
	public double getRankingScore() {
		return rankingScore;
	}

	public Set<String> getProfileIds() {
		return profileIds;
	}

	public void setProfileIds(Set<String> profileIds) {
		this.profileIds = profileIds;
	}
	
	public void updateProfileId(String profileId) {
		this.profileIds.add(profileId);
	}
	
	public void updateProfileIds(Set<String> profileIds) {
		this.profileIds.addAll(profileIds);
	}

	public Map<String, SurveyQuestionAnswer> getSurveyAnswers() {
		return surveyAnswers;
	}

	public SurveyQuestionAnswer getSurveyAnswer(String k) {
		return surveyAnswers.get(k);
	}

	public void setSurveyAnswers(Map<String, SurveyQuestionAnswer> surveyAnswers) {
		this.surveyAnswers = surveyAnswers;
	}
	
	public void updateSurveyAnswers(Map<String, SurveyQuestionAnswer> updatedSurveyAnswers) {
		updatedSurveyAnswers.forEach(( id, answer)->{
			SurveyQuestionAnswer a = this.surveyAnswers.get(id);
			if(a == null) {
				a = answer;
			} else {
				a.updateResponseCount();
			}
			this.surveyAnswers.put(id, a);
		});
	}

	public SortedSet<SurveyResult> getSurveyResults() {
		return this.surveyResults;
	}

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}

	public String getToDate() {
		return toDate;
	}

	public void setToDate(String toDate) {
		this.toDate = toDate;
	}

	public int getTotalScore() {
		return totalScore;
	}
	
	public void updateTotalScore(int totalScore) {
		this.totalScore += totalScore;
	}

	public double getAvgFeedbackScore() {
		return avgFeedbackScore;
	}
	
	public int getSurveyCount() {
		return surveyCount;
	}
	


	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
