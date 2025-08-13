package leotech.cdp.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.analytics.TouchpointHubReport;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;
import rfx.core.util.Utils;

/**
 * TouchpointHub Database Access Object
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public class TouchpointHubDaoUtil extends AbstractCdpDatabaseUtil {
	
	static final String AQL_GET_TOUCHPOINT_HUB_BY_HOSTNAME = AqlTemplate.get("AQL_GET_TOUCHPOINT_HUB_BY_HOSTNAME");
	static final String AQL_GET_TOUCHPOINT_HUB_BY_OBSERVER = AqlTemplate.get("AQL_GET_TOUCHPOINT_HUB_BY_OBSERVER");
	static final String AQL_GET_TOUCHPOINT_HUBS_BY_JOURNEY_MAP = AqlTemplate.get("AQL_GET_TOUCHPOINT_HUBS_BY_JOURNEY_MAP");
	static final String AQL_GET_TOUCHPOINT_HUB_BY_ID = AqlTemplate.get("AQL_GET_TOUCHPOINT_HUB_BY_ID");
	static final String AQL_GET_ALL_TOUCHPOINT_HUBS = AqlTemplate.get("AQL_GET_ALL_TOUCHPOINT_HUBS");
	
	static final String AQL_GET_TOUCHPOINT_HUB_REPORT_FOR_PROFILE = AqlTemplate.get("AQL_GET_TOUCHPOINT_HUB_REPORT_FOR_PROFILE");
	static final String AQL_GET_TOUCHPOINT_HUB_REPORT = AqlTemplate.get("AQL_GET_TOUCHPOINT_HUB_REPORT");
	static final String AQL_TOUCHPOINT_HUB_DETAIL_REPORT = AqlTemplate.get("AQL_TOUCHPOINT_HUB_DETAIL_REPORT");
	static final String AQL_TOUCHPOINT_HUB_PROFILE_REPORT = AqlTemplate.get("AQL_TOUCHPOINT_HUB_PROFILE_REPORT");
	
	
	/**
	 * @param d
	 * @return
	 */
	public static String save(TouchpointHub d) {
		if (d.dataValidation()) {
			ArangoCollection col = d.getDbCollection();
			String id = d.getId();
			if (col != null) {
				ArangoDatabase db = getCdpDatabase();
				boolean isExisted = ArangoDbUtil.isExistedDocument(db, TouchpointHub.COLLECTION_NAME, id);
				if(!isExisted) {
					col.insertDocument(d, optionToUpsertInSilent());
				} else {
					d.setUpdatedAt(new Date());
					col.updateDocument(id, d, getMergeOptions());
				}
				return id;
			}
		}
		return null;
	}
	
	/**
	 * @param m
	 * @return
	 */
	public static boolean delete(TouchpointHub m) {
		if( ! m.isDataObserver() ) {
			ArangoCollection col = m.getDbCollection();
			if (col != null) {
				col.deleteDocument(m.getId());
				return true;
			}	
		}
		return false;
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static TouchpointHub getById(String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		TouchpointHub p = new ArangoDbCommand<TouchpointHub>(db, AQL_GET_TOUCHPOINT_HUB_BY_ID, bindVars, TouchpointHub.class)
				.getSingleResult();
		return p;
	}
	
	/**
	 * @return
	 */
	public static List<TouchpointHub> getAll() {
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointHub> list = new ArangoDbCommand<TouchpointHub>(db, AQL_GET_ALL_TOUCHPOINT_HUBS, new HashMap<>(0) ,TouchpointHub.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param observerId
	 * @return
	 */
	public static TouchpointHub getByObserverId(String observerId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("observerId", observerId);
		TouchpointHub p = new ArangoDbCommand<TouchpointHub>(db, AQL_GET_TOUCHPOINT_HUB_BY_OBSERVER, bindVars, TouchpointHub.class).getSingleResult();
		return p;
	}

	/**
	 * @param journeyMapId
	 * @return
	 */
	public static List<TouchpointHub> getTouchpointHubsByJourneyMap(String journeyMapId){
		return getTouchpointHubsByJourneyMap(journeyMapId, false);
	}

	
	/**
	 * @param journeyMapId
	 * @param refreshData
	 * @return
	 */
	public static List<TouchpointHub> getTouchpointHubsByJourneyMap(String journeyMapId, boolean refreshData){
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("journeyMapId", journeyMapId);
		bindVars.put("refreshData", refreshData);
		List<TouchpointHub> list = new ArangoDbCommand<TouchpointHub>(db, AQL_GET_TOUCHPOINT_HUBS_BY_JOURNEY_MAP, bindVars ,TouchpointHub.class).getResultsAsList();
		return list;
	}

	/**
	 * @param hostname
	 * @return
	 */
	public static TouchpointHub getByHostName(String hostname) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("hostname", hostname);
		TouchpointHub p = new ArangoDbCommand<TouchpointHub>(db, AQL_GET_TOUCHPOINT_HUB_BY_HOSTNAME, bindVars, TouchpointHub.class).getSingleResult();
		return p;
	}
	
	/**
	 * @param journeyId
	 * @param touchpointType
	 * @param startIndex
	 * @param numberResult
	 * @return List<TouchpointReport>
	 */
	public static List<TouchpointHubReport> getTouchpointHubReport(String journeyId, String beginFilterDate, String endFilterDate, int startIndex, int numberResult) {
		Map<String, Object> bindVars = new HashMap<>(6);
		bindVars.put("journeyId", journeyId);
		bindVars.put("beginFilterDate", beginFilterDate);
		bindVars.put("endFilterDate", endFilterDate);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointHubReport> finalList = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINT_HUB_REPORT, bindVars, TouchpointHubReport.class).getResultsAsList();
		return finalList;
	}
	
	/**
	 * @param journeyId
	 * @return
	 */
	public static List<TouchpointHubReport> getTouchpointHubDetailReport(String journeyId) {
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("journeyId", journeyId);
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointHubReport> list = new ArangoDbCommand<>(db, AQL_TOUCHPOINT_HUB_DETAIL_REPORT, bindVars, TouchpointHubReport.class).getResultsAsList();
	
		Map<String,TouchpointHubReport> map = new HashMap<>();
		for (TouchpointHubReport e : list) {
			String id = e.getTouchpointHubId();
			TouchpointHubReport tpHub = map.get(id);
			
			if(tpHub == null) {
				tpHub = e;
				map.put(id, tpHub);
			}
			else if(tpHub.getTouchpointHubId().equals(id)) {
				tpHub.updateReport(e);
				map.put(id, tpHub);
			}
		}
		return new ArrayList<TouchpointHubReport>(map.values());
	}
	
	/**
	 * @param journeyId
	 * @return
	 */
	public static List<TouchpointHubReport> getTouchpointHubProfileReport(String journeyId) {
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("journeyId", journeyId);
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointHubReport> list = new ArangoDbCommand<>(db, AQL_TOUCHPOINT_HUB_PROFILE_REPORT, bindVars, TouchpointHubReport.class).getResultsAsList();
	
		Map<String,TouchpointHubReport> map = new HashMap<>();
		for (TouchpointHubReport e : list) {
			String id = e.getTouchpointHubId();
			TouchpointHubReport report = map.get(id);
			if(report == null) {
				report = e;
				map.put(id, report);
			}
			else if(report.getTouchpointHubId().equals(id)) {
				report.updateProfileCount(e);
				map.put(id, report);
			}
		}
		
		return new ArrayList<TouchpointHubReport>(map.values());
	}
	
	
	
	/**
	 * @param profileId
	 * @param journeyId
	 * @return
	 */
	public static List<TouchpointHubReport> getTouchpointHubReportForProfile(String profileId, String journeyId) {
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("journeyId", journeyId);
		bindVars.put("refProfileId", profileId);
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointHubReport> finalList = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINT_HUB_REPORT_FOR_PROFILE, bindVars, TouchpointHubReport.class).getResultsAsList();
		return finalList;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<TouchpointHubReport> list = getTouchpointHubDetailReport("6yD8rRImsBBXhEpNWHhcIF");
		for (TouchpointHubReport tp : list) {
			System.out.println(tp);
		}
		Utils.exitSystemAfterTimeout(3000);
	}
	
	
}
