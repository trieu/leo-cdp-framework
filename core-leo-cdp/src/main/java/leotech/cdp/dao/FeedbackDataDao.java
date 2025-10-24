package leotech.cdp.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.google.gson.Gson;

import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.analytics.FeedbackGroupByDate;
import leotech.cdp.model.analytics.FeedbackRatingReport;
import leotech.cdp.model.analytics.ScoreCX;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;

/**
 * Feedback Data database
 * 
 * @author tantrieuf31
 *
 */
public final class FeedbackDataDao extends AbstractCdpDatabaseUtil {

	static final String AQL_GET_LATEST_FEEDBACK_SCORE = AqlTemplate.get("AQL_GET_LATEST_FEEDBACK_SCORE");
	
	static final String AQL_UPDATE_FEEDBACK_DATA_TO_NEW_PROFILE = AqlTemplate.get("AQL_UPDATE_FEEDBACK_DATA_TO_NEW_PROFILE");
	static final String AQL_GET_FEEDBACK_EVENTS_BY_TYPE_AND_DATES = AqlTemplate.get("AQL_GET_FEEDBACK_EVENTS_BY_TYPE_AND_DATES");
	
	static final String AQL_GET_FEEDBACK_EVENTS_BY_PROFILE_ID = AqlTemplate.get("AQL_GET_FEEDBACK_EVENTS_BY_PROFILE_ID");
	static final String AQL_GET_FEEDBACK_EVENTS_BY_TEMPLATE_ID = AqlTemplate.get("AQL_GET_FEEDBACK_EVENTS_BY_TEMPLATE_ID");
	static final String AQL_GET_FEEDBACK_EVENTS_BY_EVENT_NAME = AqlTemplate.get("AQL_GET_FEEDBACK_EVENTS_BY_EVENT_NAME");
	
	static final String AQL_DELETE_FEEDBACK_EVENTS_BY_PROFILE_ID = AqlTemplate.get("AQL_DELETE_FEEDBACK_EVENTS_BY_PROFILE_ID");
	
	/**
	 * record any event of profile
	 * 
	 * @param e
	 * @return
	 */
	public static String save(FeedbackEvent e) {
		if (e.dataValidation()) {
			ArangoCollection col = e.getDbCollection();
			if (col != null) {
				col.insertDocument(e, optionToUpsertInSilent());
				return e.getId();
			}
		} else {
			String json = new Gson().toJson(e);
			System.err.println("invalid FeedbackEvent \n" + json);
		}
		return null;
	}
	
	/**
	 * @param refTemplateId
	 * @param refVisitorId
	 * @param touchpointUrl
	 * @return
	 */
	public static int loadLatestFeedbackScore(String refTemplateId, String refVisitorId, String touchpointUrl) {
		ArangoDatabase db = getCdpDatabase();
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("refTemplateId", refTemplateId);
		bindVars.put("refVisitorId", refVisitorId);
		bindVars.put("touchpointUrl", touchpointUrl);
		
		Integer score = new ArangoDbCommand<Integer>(db, AQL_GET_LATEST_FEEDBACK_SCORE,bindVars, Integer.class).getSingleResult();
		if(score != null) {
			return score.intValue();
		}
		return -1;
	}
	
	/**
	 * @param refTemplateId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<FeedbackEvent> getFeedbackEventsBySurveyTemplateId(String refTemplateId, String fromDate, String toDate) {
		ArangoDatabase db = getCdpDatabase();
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("refTemplateId", refTemplateId);
		bindVars.put("fromDate", fromDate);
		bindVars.put("toDate", toDate);
		
		List<FeedbackEvent> list = new ArangoDbCommand<FeedbackEvent>(db, AQL_GET_FEEDBACK_EVENTS_BY_TEMPLATE_ID,bindVars, FeedbackEvent.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param refTemplateId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<FeedbackEvent> getFeedbackEventsByEventName(String eventName, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("eventName", eventName);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		List<FeedbackEvent> list = new ArangoDbCommand<FeedbackEvent>(db, AQL_GET_FEEDBACK_EVENTS_BY_EVENT_NAME, bindVars, FeedbackEvent.class).getResultsAsList();
		return list;
	}
	
	
	/**
	 * @param refProfileId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<FeedbackEvent> getFeedbackEventsByProfileId(String refProfileId, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("refProfileId", refProfileId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		CallbackQuery<FeedbackEvent> callback = new CallbackQuery<FeedbackEvent>() {
			@Override
			public FeedbackEvent apply(FeedbackEvent obj) {
				obj.enrichProfileExtAttributes();
				return obj;
			}
		};
		List<FeedbackEvent> list = new ArangoDbCommand<FeedbackEvent>(db, AQL_GET_FEEDBACK_EVENTS_BY_PROFILE_ID,bindVars, FeedbackEvent.class, callback ).getResultsAsList();
		return list;
	}
	
	
	public static List<FeedbackGroupByDate> getFeedbackGroupByDate(String feedbackType, String fromDate, String toDate) {
		ArangoDatabase db = getCdpDatabase();
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("feedbackType", feedbackType);
		bindVars.put("fromDate", fromDate);
		bindVars.put("toDate", toDate);
		
		List<FeedbackGroupByDate> list = new ArangoDbCommand<FeedbackGroupByDate>(db, AQL_GET_FEEDBACK_EVENTS_BY_TYPE_AND_DATES, bindVars, FeedbackGroupByDate.class)
				.getResultsAsList();
		return list;
	}
	
	/**
	 * @param oldProfileId
	 * @param newProfileId
	 */
	public static void mergeFeedbackEventToNewProfile(String oldProfileId, String newProfileId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("oldProfileId", oldProfileId);
		bindVars.put("newProfileId", newProfileId);
		new ArangoDbCommand<FeedbackEvent>(db, AQL_UPDATE_FEEDBACK_DATA_TO_NEW_PROFILE, bindVars).update();
	}
	
	/**
	 * @param feedbackDataType
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static FeedbackRatingReport getFeedbackReport(String feedbackDataType, String fromDate, String toDate) {
		FeedbackRatingReport fbReport = new FeedbackRatingReport(fromDate, toDate, feedbackDataType); 
		
		List<FeedbackGroupByDate> groups = getFeedbackGroupByDate(feedbackDataType, fromDate, toDate);

		Set<String> positive = new HashSet<>();
		Set<String> neutral = new HashSet<>();
		Set<String> negative = new HashSet<>();

		ScoreCX summaryScoreCX = new ScoreCX();
		Map<String, ScoreCX> dailyScoreCX = new HashMap<String, ScoreCX>();
		for (FeedbackGroupByDate g : groups) {
			String dateKey = g.getDateKey();
			

			List<FeedbackEvent> feedbacks = g.getFeedbackEvents();
			for (FeedbackEvent fbe : feedbacks) {
				// event
				ScoreCX score = fbe.getScoreCX();
				
				// daily
				ScoreCX dailyScore = dailyScoreCX.getOrDefault(dateKey, new ScoreCX());
				dailyScoreCX.put(dateKey, dailyScore.increase(score));
				
				// overall
				summaryScoreCX.increase(score);
				
				String profileId = fbe.getRefProfileId();
				int sentimentType = score.getSentimentType();
				
				if (sentimentType == 1) {
					positive.add(profileId);
					neutral.remove(profileId);
					negative.remove(profileId);
				}
				else if (sentimentType == -1) {
					negative.add(profileId);
					neutral.remove(profileId);
					positive.remove(profileId);
				}
				else {
					neutral.add(profileId);
					positive.remove(profileId);
					negative.remove(profileId);
				}
			}
		}
		
		fbReport.setScoreCX(summaryScoreCX);
		fbReport.setDailyScoreCX(dailyScoreCX);
		fbReport.setPositiveProfileCount(positive.size());
		fbReport.setNegativeProfileCount(negative.size());
		fbReport.setNeutralProfileCount(neutral.size());
		return fbReport;
	}

	/**
	 * @param refProfileId
	 */
	public static void deleteDataByProfileId(String refProfileId) {
		try {
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("refProfileId", refProfileId);
			new ArangoDbCommand<String>(getCdpDatabase(), AQL_DELETE_FEEDBACK_EVENTS_BY_PROFILE_ID, bindVars).update();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
