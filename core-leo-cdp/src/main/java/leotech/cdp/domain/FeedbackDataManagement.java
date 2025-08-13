package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import leotech.cdp.dao.AssetTemplateDaoUtil;
import leotech.cdp.dao.FeedbackDataDao;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.analytics.FeedbackRatingReport;
import leotech.cdp.model.analytics.FeedbackSurveyReport;
import leotech.cdp.model.analytics.SurveyChoice;
import leotech.cdp.model.analytics.SurveyResult;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.system.util.IdGenerator;
import rfx.core.util.FileUtils;

/**
 * Feedback Data Management
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class FeedbackDataManagement {
	
	public static final String PUBLIC_EXPORTED_FILES_SURVEY = "/public/exported-files/survey-";

	/**
	 * @param refTemplateId
	 * @param refVisitorId
	 * @param touchpointUrl
	 * @return
	 */
	public static int loadLatestFeedbackScore(String refTemplateId, String refVisitorId, String touchpointUrl) {
		return FeedbackDataDao.loadLatestFeedbackScore(refTemplateId, refVisitorId, touchpointUrl);
	}
	
	
	/**
	 * @param feedbackDataType
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static FeedbackRatingReport getFeedbackReport(String feedbackDataType, String fromDate, String toDate) {
		FeedbackRatingReport fbReport = FeedbackDataDao.getFeedbackReport(feedbackDataType, fromDate, toDate);
		return fbReport;
	}
	
	/**
	 * @param FeedbackEvent fe
	 * @return FeedbackSurveyReport
	 */
	public static FeedbackSurveyReport buildSurveyFeedbackReport(FeedbackEvent fe) {
		String refTemplateId = fe.getRefTemplateId();
		AssetTemplate tpl = AssetTemplateDaoUtil.getById(refTemplateId);
		if(tpl != null) {
			FeedbackSurveyReport fbReport = new FeedbackSurveyReport(tpl);
			
			String refProfileId = fe.getRefProfileId();
			System.out.println("refProfileId " + refProfileId + " DeviceId " +fe.getDeviceId());
			fbReport.updateProfileId(refProfileId);
			
			// common for report key
			fbReport.setFeedbackType(fe.getFeedbackType());
			fbReport.setHeader(fe.getHeader());
			fbReport.setGroup(fe.getGroup());
			fbReport.setEvaluatedObject(fe.getEvaluatedObject());
			fbReport.setEvaluatedItem(fe.getEvaluatedItem());
			fbReport.setEvaluatedPerson(fe.getEvaluatedPerson());
			
			// can be updated for new survey answer
			Map<String, Map<String, String>> questionAnswer = fe.getRatingQuestionAnswer();
			int index = 0;
			for (String questionGroup : questionAnswer.keySet()) {
				Map<String, String> questionGroupResults = questionAnswer.get(questionGroup);
				Set<String> keys = questionGroupResults.keySet();
				for (String question : keys) {
					++index;
					String answer = questionGroupResults.get(question);
					fbReport.addSurveyStats(index, questionGroup, question, answer);
				}
			}
			
			fbReport.computeFinalScoreAndResults();
			
			// build the unique ID
			fbReport.buildHashedId();
			
			return fbReport;
		}
		return null;
	}
	
	

	/**
	 * @param refTemplateId
	 * @param fromDate
	 * @param toDate
	 * @param includeGroup
	 * @param includeObject
	 * @param includeItem
	 * @param includePerson
	 * @param rankingByScore
	 * @return
	 */
	public static List<FeedbackSurveyReport> getSurveyFeedbackReport(String refTemplateId, String fromDate, String toDate, 
			boolean includeGroup, boolean includeObject, boolean  includeItem, boolean includePerson, boolean rankingByScore) {
		AssetTemplate tpl = AssetTemplateDaoUtil.getById(refTemplateId);
		if(tpl != null) {
			List<FeedbackEvent> events = FeedbackDataDao.getFeedbackEventsBySurveyTemplateId(refTemplateId, fromDate, toDate);
			int eventSize = events.size();
			
			Map<String, FeedbackSurveyReport> reports = new HashMap<>(eventSize);
			
			for (FeedbackEvent event : events) {
				FeedbackSurveyReport fbReport = new FeedbackSurveyReport(tpl, eventSize, event.getCreatedAt());
				
				String refProfileId = event.getRefProfileId();
				fbReport.updateProfileId(refProfileId);
				
				// common for report key
				fbReport.setFeedbackType(event.getFeedbackType());
				fbReport.setHeader(event.getHeader());
				
				if(includeGroup) {
					fbReport.setGroup(event.getGroup());
				}
				
				if(includeObject) {
					fbReport.setEvaluatedObject(event.getEvaluatedObject());
				}
				
				if(includeItem) {
					fbReport.setEvaluatedItem(event.getEvaluatedItem());
				}
				
				if(includePerson) {
					fbReport.setEvaluatedPerson(event.getEvaluatedPerson());
				}
				
				// can be updated for new survey answer
				Map<String, Map<String, String>> questionAnswer = event.getRatingQuestionAnswer();
				int index = 0;
				for (String questionGroup : questionAnswer.keySet()) {
					Map<String, String> questionGroupResults = questionAnswer.get(questionGroup);
					Set<String> keys = questionGroupResults.keySet();
					for (String question : keys) {
						++index;
						String answer = questionGroupResults.get(question);
						fbReport.addSurveyStats(index, questionGroup, question, answer);
					}
				}
				
				// TODO
				//event.getSingleChoiceQuestionAnswer();
				//event.getMultipleChoiceQuestionAnswer();
				//event.getExtraTextQuestionsAnswer();
				
				// build the unique ID
				String id = fbReport.buildHashedId();
				
				// get report in map
				FeedbackSurveyReport cfbReport = reports.get(id);
				
				if(cfbReport == null) {
					cfbReport = fbReport;
					//System.out.println("new cfbReport");
				}
				else {
					cfbReport.updateSurveyAnswers(fbReport.getSurveyAnswers());
					cfbReport.updateAvgScore(fbReport.getAvgFeedbackScore());
					cfbReport.updateTotalScore(fbReport.getTotalScore());
					cfbReport.updateProfileIds(fbReport.getProfileIds());
					//System.out.println("update cfbReport");
				}
				
				// final step
				cfbReport.computeFinalScoreAndResults();
				reports.put(id, cfbReport);
			}
			
			if(rankingByScore) {
				return createSortedListByRankingScore(reports);
			}
			else {
				return createSortedListByReportDate(reports);
			}
		} 
		System.err.println("refTemplateId is not found, " + refTemplateId);
		return new ArrayList<>(0);
	}

	/**
	 * @param reports
	 * @return
	 */
	public static List<FeedbackSurveyReport> createSortedListByReportDate(Map<String, FeedbackSurveyReport> reports) {
		List<FeedbackSurveyReport> fbReports = new ArrayList<>(reports.values());
		Collections.sort(fbReports, new Comparator<FeedbackSurveyReport>() {
			@Override
			public int compare(FeedbackSurveyReport o1, FeedbackSurveyReport o2) {
				return -1 * o1.getCreatedAt().compareTo(o2.getCreatedAt());
			}
		});
		return fbReports;
	}

	/**
	 * @param reports
	 * @return
	 */
	public static List<FeedbackSurveyReport> createSortedListByRankingScore(Map<String, FeedbackSurveyReport> reports) {
		List<FeedbackSurveyReport> fbReports = new ArrayList<>(reports.values());
		Collections.sort(fbReports, new Comparator<FeedbackSurveyReport>() {
			@Override
			public int compare(FeedbackSurveyReport o1, FeedbackSurveyReport o2) {
				double rankingScore1 = o1.getRankingScore();
				double rankingScore2 = o2.getRankingScore();
				if(rankingScore1 > rankingScore2) {
					return -1;
				}
				if(rankingScore1 < rankingScore2) {
					return 1;
				}
				return 0;
			}
		});
		return fbReports;
	}


	public static Map<String,String> exportSurveyFeedbackData(String refTemplateId, String beginFilterDate, String endFilterDate, 
			boolean includeGroup, boolean includeObject, boolean  includeItem, boolean includePerson, boolean rankingByScore, String systemUserId) {
		List<FeedbackSurveyReport> fbReports = FeedbackDataManagement.getSurveyFeedbackReport(refTemplateId, beginFilterDate, endFilterDate, 
				includeGroup, includeObject, includeItem, includePerson, rankingByScore);
		Map<String,String> map = new HashMap<>(fbReports.size());
		for (FeedbackSurveyReport report : fbReports) {
			String id = report.getId();
			
			StringBuilder exportedStr = new StringBuilder();
			
			SortedSet<SurveyResult> survetResults = report.getSurveyResults();
			
			// building header
			SurveyResult headResult = survetResults.first();
			if(headResult != null) {
				String question = "\"[" + headResult.getQuestionGroup() + "]" +headResult.getQuestion()+ "\"";
				exportedStr.append(question).append(",");
				SortedSet<SurveyChoice> answerResults = headResult.getAnswerResults();
				answerResults.forEach(answer->{
					exportedStr.append(answer.getAnswer()).append(",");
				});
				exportedStr.append("\n");
			}
			
			for (SurveyResult rs : survetResults) {
				String question = "\"["+ rs.getQuestionGroup() + "]" +rs.getQuestion()+ "\"";
				exportedStr.append(question).append(",");
				SortedSet<SurveyChoice> answerResults = rs.getAnswerResults();
				answerResults.forEach(answer->{
					exportedStr.append(answer.getResponseCount()).append(",");
				});
				exportedStr.append("\n");
			}
			exportedStr.append("\n");
			
			
			
			// TODO
			
			String exportId = refTemplateId + "-"+ beginFilterDate + "-" + endFilterDate;
			String dataAccessKey = IdGenerator.generateDataAccessKey(exportId, systemUserId);
			String fullPath = PUBLIC_EXPORTED_FILES_SURVEY + exportId + ".csv";
			FileUtils.writeStringToFile("." + fullPath, exportedStr.toString());
			String accessUri = fullPath + "?dataAccessKey=" + dataAccessKey;
			map.put(id, accessUri);
		}
		return map;
	}
}
