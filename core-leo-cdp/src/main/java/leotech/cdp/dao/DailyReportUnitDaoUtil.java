package leotech.cdp.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.analytics.DailyReportUnit;
import leotech.cdp.model.analytics.DashboardEventReport;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.Segment;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;
import rfx.core.util.StringUtil;

/**
 * Daily Report Unit DAO
 * 
 * @author tantrieuf31
 * @since 2021
 */
public final class DailyReportUnitDaoUtil extends AbstractCdpDatabaseUtil {
	
	// daily report units
	static final String AQL_GET_DAILY_REPORT_UNITS_FOR_ONE_OBJECT = AqlTemplate.get("AQL_GET_DAILY_REPORT_UNITS_FOR_ONE_OBJECT");
	static final String AQL_GET_DAILY_REPORT_UNITS = AqlTemplate.get("AQL_GET_DAILY_REPORT_UNITS");
	static final String AQL_OBJECT_COUNT_FROM_DAILY_REPORT_UNITS = AqlTemplate.get("AQL_OBJECT_COUNT_FROM_DAILY_REPORT_UNITS");
	static final String AQL_UPDATE_DAILY_REPORT_UNITS_TO_NEW_PROFILE = AqlTemplate.get("AQL_UPDATE_DAILY_REPORT_UNITS_TO_NEW_PROFILE");
	
	static final String AQL_SEGMENT_SIZE_DAILY_REPORT_UNITS = AqlTemplate.get("AQL_SEGMENT_SIZE_DAILY_REPORT_UNITS");
	static final String AQL_SEGMENT_DAILY_REPORT_UNITS = AqlTemplate.get("AQL_SEGMENT_DAILY_REPORT_UNITS");
	

	// ---- Profile Statistics ---- //
	
	/**
	 * Get daily report units for ONE profile
	 * 
	 * @param profileId
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static Map<String, DailyReportUnit> getDailyReportUnitMapForProfile(String profileId, String fromDate, String toDate) {
		List<DailyReportUnit> dailyReportUnits = queryDailyReportUnitsForOneObject("", Profile.COLLECTION_NAME, profileId, fromDate, toDate, null);
		
		Map<String, DailyReportUnit> reportMap = new HashMap<String, DailyReportUnit>(dailyReportUnits.size());
		
		for (DailyReportUnit dailyReportUnit : dailyReportUnits) {
			String journeyMapId = dailyReportUnit.getJourneyMapId();
			String key = DailyReportUnit.buildMapKey(dailyReportUnit.getCreatedAt(), dailyReportUnit.getEventName(), journeyMapId );
			reportMap.put(key , dailyReportUnit);
		}
		return reportMap;
	}

	/**
	 * Get daily report units for one objects by objectName and objectId
	 * 
	 * @param objectName
	 * @param objectId
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static List<DailyReportUnit> queryDailyReportUnitsForOneObject(String journeyMapId, String objectName, String objectId, String fromDate, String toDate, CallbackQuery<DailyReportUnit> callback) {
		ArangoDatabase db = getCdpDatabase();
		
		Map<String, Object> bindVars = new HashMap<>(5);
		bindVars.put("journeyMapId", journeyMapId);
		bindVars.put("objectId", objectId);
		bindVars.put("objectName", objectName );
		bindVars.put("fromDate", fromDate);
		bindVars.put("toDate", toDate);
		
		String aql = AQL_GET_DAILY_REPORT_UNITS_FOR_ONE_OBJECT;
		ArangoDbCommand<DailyReportUnit> q = new ArangoDbCommand<DailyReportUnit>(db, aql, bindVars, DailyReportUnit.class, callback);
		List<DailyReportUnit> dailyReportUnits = q.getResultsAsList();

		return dailyReportUnits;
	}
	
	/**
	 * Get daily report units for one objects by objectName and objectId
	 * 
	 * @param objectName
	 * @param objectId
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static List<DailyReportUnit> queryDailyReportUnitsForSegment(String segmentId, String fromDate, String toDate, CallbackQuery<DailyReportUnit> callback) {
		ArangoDatabase db = getCdpDatabase();
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("segmentId", segmentId);
		bindVars.put("fromDate", fromDate);
		bindVars.put("toDate", toDate);
		
		String aql = AQL_SEGMENT_DAILY_REPORT_UNITS;
		ArangoDbCommand<DailyReportUnit> q = new ArangoDbCommand<DailyReportUnit>(db, aql, bindVars, DailyReportUnit.class, callback);
		List<DailyReportUnit> dailyReportUnits = q.getResultsAsList();
		return dailyReportUnits;
	}
	
	/**
	 * Get daily report units for all objects by objectName
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static void queryDailyReportUnits(String journeyMapId, String objectName, String fromDate, String toDate, CallbackQuery<DailyReportUnit> callback) {
		ArangoDatabase db = getCdpDatabase();
		
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("journeyMapId", journeyMapId );
		bindVars.put("objectName", objectName );
		bindVars.put("fromDate", fromDate);
		bindVars.put("toDate", toDate);
		
		ArangoDbCommand<DailyReportUnit> q = new ArangoDbCommand<DailyReportUnit>(db, AQL_GET_DAILY_REPORT_UNITS, bindVars, DailyReportUnit.class, callback);
		q.applyCallback();
	}
	
	/**
	 * how many object that create all event from beginFilterDate to endFilterDate
	 * 
	 * @param objectName
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static long getObjectCountFromDailyReportUnits(String objectName, String fromDate, String toDate) {
		ArangoDatabase db = getCdpDatabase();
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("objectName", objectName );
		bindVars.put("fromDate", fromDate);
		bindVars.put("toDate", toDate);
		
		long c = new ArangoDbCommand<Long>(db, AQL_OBJECT_COUNT_FROM_DAILY_REPORT_UNITS, bindVars, Long.class).getSingleResult();
		return c;
	}
	
	/**
	 * @param objectName
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @return
	 */
	public static DashboardEventReport getDashboardEventReport(String journeyMapId, String objectName, String beginFilterDate, String endFilterDate) {
		return getDashboardEventReport(journeyMapId, objectName, null, beginFilterDate, endFilterDate);
	}
	
	/**
	 * Get DashboardEventReport of daily report units
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static DashboardEventReport getDashboardEventReport(String journeyMapId, String objectName, String objectId, String beginFilterDate, String endFilterDate) {
		DashboardEventReport report = new DashboardEventReport(beginFilterDate, endFilterDate);
		// callback
		CallbackQuery<DailyReportUnit> callback = new CallbackQuery<DailyReportUnit>() {
			@Override
			public DailyReportUnit apply(DailyReportUnit obj) {
				String eventName = obj.getEventName();
				long dailyCount = obj.getDailyCount();
				report.addReportSummary(eventName, dailyCount);
				report.addReportMap(eventName, obj);				
				//System.out.println("objectName " + objectName + " objectId " + objectId + " eventName " + eventName + " dailyCount " + dailyCount);
				return obj;
			}
		};
		long profileCount = 0;
		if(StringUtil.isEmpty(objectId)) {
			queryDailyReportUnits(journeyMapId, objectName, beginFilterDate, endFilterDate, callback);
			profileCount = getObjectCountFromDailyReportUnits(objectName, beginFilterDate, endFilterDate);
		} else {
			queryDailyReportUnitsForOneObject(journeyMapId, objectName, objectId, beginFilterDate, endFilterDate, callback);
			profileCount = 1;
		}
		report.setProfileCount(profileCount);
		return report;
	}
	

	
	/**
	 * Get Segment DashboardEventReport of daily report units
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static DashboardEventReport getSegmentDashboardEventReport(Segment segment, String beginFilterDate, String endFilterDate) {
		DashboardEventReport report = new DashboardEventReport(beginFilterDate, endFilterDate);
		// callback
		CallbackQuery<DailyReportUnit> callback = new CallbackQuery<DailyReportUnit>() {
			@Override
			public DailyReportUnit apply(DailyReportUnit obj) {
				String eventName = obj.getEventName();
				long dailyCount = obj.getDailyCount();
				report.addReportSummary(eventName, dailyCount);
				report.addReportMap(eventName, obj);
				return obj;
			}
		};
		queryDailyReportUnitsForSegment(segment.getId(), beginFilterDate, endFilterDate, callback);
		return report;
	}
	
	
	/**
	 * Update daily report units for a profile
	 * 
	 * @param profileId
	 * @param reportMap
	 * @param insert
	 */
	public static void updateProfileDailyReportUnit(String profileId, Map<String, DailyReportUnit> reportMap) {
		Collection<DailyReportUnit> values = reportMap.values();
		ArangoCollection col = DailyReportUnit.getCollectionInstance();
		col.insertDocuments(values, insertDocumentOverwriteModeReplace());
	}
	
	/**
	 * @param reportMap
	 */
	public static void deleteProfileDailyReportUnit(Map<String, DailyReportUnit> reportMap) {
		Collection<DailyReportUnit> values = reportMap.values();
		ArangoCollection col = DailyReportUnit.getCollectionInstance();
		col.deleteDocuments(values);
	}
	
	/**
	 * @param oldProfileId
	 * @param newProfileId
	 */
	public static void updateDailyReportUnitsToNewProfile(String oldProfileId, String newProfileId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("oldProfileId", oldProfileId);
		bindVars.put("newProfileId", newProfileId);
		new ArangoDbCommand<FeedbackEvent>(db, AQL_UPDATE_DAILY_REPORT_UNITS_TO_NEW_PROFILE, bindVars).update();
	}
}
