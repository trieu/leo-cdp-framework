package test.feedbacksurvey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import leotech.cdp.dao.AssetTemplateDaoUtil;
import leotech.cdp.dao.FeedbackDataDao;
import leotech.cdp.domain.FeedbackDataManagement;
import leotech.cdp.domain.scoring.ProcessorScoreCX;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.analytics.FeedbackSurveyReport;
import leotech.cdp.model.analytics.ScoreCX;
import leotech.cdp.model.analytics.SurveyChoice;
import leotech.cdp.model.analytics.SurveyResult;
import leotech.cdp.model.asset.AssetTemplate;
import rfx.core.util.Utils;

public class TestBuildSurveyReport {

	public static void main(String[] args) throws IOException {
		// String jsonStr =
		// FileUtils.readFileAsString("./data/sample-survey-answers.json");
		// System.out.println(jsonStr);
		// List<FeedbackEvent> list = new Gson().fromJson(jsonStr, new
		// TypeToken<ArrayList<FeedbackEvent>>(){}.getType());

		testExportData();
		//test();

		//testScore();
	}
	
	static void testExportData() {
		String refTemplateId = "YV42cUqruU3jglC50sioN";
		String fromDate = "2022-08-11";
		String toDate = "2022-09-26";
		
		List<FeedbackEvent> events = FeedbackDataDao.getFeedbackEventsBySurveyTemplateId(refTemplateId, fromDate, toDate);
		List<List<String>> table = new ArrayList<List<String>>();
		AssetTemplate tpl = AssetTemplateDaoUtil.getById(refTemplateId);
		int eventSize = events.size();
		for (FeedbackEvent event : events) {
			System.out.println(event);
			FeedbackSurveyReport fbReport = new FeedbackSurveyReport(tpl, eventSize, event.getCreatedAt());
		}
	}

	private static void testScore() {
		double newFeedbackScore = 3.5;
		ScoreCX currentScore = new ScoreCX();
		currentScore = ProcessorScoreCX.computeScale5double(newFeedbackScore , currentScore);
		System.out.println(currentScore);
	}

	private static void test() {
		String refTemplateId = "6E6vaNfWlfafgK3qzVmnEG";
		String fromDate = "2021-07-08 20:31:36";
		String toDate = "2021-09-08 20:31:36";
		List<FeedbackSurveyReport> fbReports = FeedbackDataManagement.getSurveyFeedbackReport(refTemplateId, fromDate, toDate, true, true, false, true, false);

		for (FeedbackSurveyReport fbReport : fbReports) {
			System.out.println("\n ==> " + fbReport.getSurveyKey() );
			
			SortedSet<SurveyResult> results = fbReport.getSurveyResults();
			
			for (SurveyResult result : results) {
				System.out.println("\n "+result.getQuestion());
				SortedSet<SurveyChoice> answerResults = result.getAnswerResults();
				for (SurveyChoice c : answerResults) {
					System.out.println(c.getAnswer() + " " + c.getResponseCount());
				}
			}
			
			System.out.println("CreatedAt " + fbReport.getCreatedAt());
			System.out.println("SurveyCount " + fbReport.getSurveyCount());
			System.out.println("Profiles " + fbReport.getProfileIds());
			System.out.println("AvgScore " + fbReport.getAvgFeedbackScore());
			System.out.println("TotalScore " + fbReport.getTotalScore());
			System.out.println("CX Score " + fbReport.getScoreCX());
			System.out.println("==> RankingScore " + fbReport.getRankingScore());
			
			break;
		}

		Utils.exitSystemAfterTimeout(1000);
	}
}
