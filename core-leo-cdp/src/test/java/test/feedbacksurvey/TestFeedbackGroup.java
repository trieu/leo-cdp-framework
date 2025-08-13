package test.feedbacksurvey;

import leotech.cdp.dao.FeedbackDataDao;
import leotech.cdp.model.analytics.FeedbackRatingReport;
import leotech.cdp.model.customer.FeedbackType;
import rfx.core.util.Utils;

public class TestFeedbackGroup {

	public static void main(String[] args) {
		String feedbackDataType = FeedbackType.getFeedbackTypeAsText(FeedbackType.RATING);
		String fromDate = "2021-06-01";
		String toDate = "2021-06-11";
		
		FeedbackRatingReport fbReport = FeedbackDataDao.getFeedbackReport(feedbackDataType, fromDate, toDate);
		
		System.out.println(fbReport);
		
		Utils.exitSystemAfterTimeout(1000);
	}
}
