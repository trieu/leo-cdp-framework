package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.analytics.JourneyEventStatistics;
import leotech.cdp.model.analytics.JourneyReport;
import leotech.cdp.model.analytics.TouchpointHubReport;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.JourneyMapRefKey;
import leotech.cdp.query.filters.DataFilter;
import leotech.system.config.AqlTemplate;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;
import rfx.core.util.StringUtil;

/**
 * Journey Map Database Access Object
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class JourneyMapDao extends AbstractCdpDatabaseUtil {

	static final String AQL_GET_JOURNEY_MAP_BY_ID = AqlTemplate.get("AQL_GET_JOURNEY_MAP_BY_ID");
	static final String AQL_GET_JOURNEY_MAP_BY_NAME = AqlTemplate.get("AQL_GET_JOURNEY_MAP_BY_NAME");
	static final String AQL_GET_ALL_JOURNEY_MAPS = AqlTemplate.get("AQL_GET_ALL_JOURNEY_MAPS");
	static final String AQL_GET_ALL_JOURNEY_MAPS_FOR_USER = AqlTemplate.get("AQL_GET_ALL_JOURNEY_MAPS_FOR_USER");
	static final String AQL_GET_JOURNEY_MAP_REFKEYS_BY_IDS = AqlTemplate.get("AQL_GET_JOURNEY_MAP_REFKEYS_BY_IDS");
	
	static final String AQL_COUNT_PROFILE_WITH_JOURNEY = AqlTemplate.get("AQL_COUNT_PROFILE_WITH_JOURNEY");
	static final String AQL_COUNT_JOURNEY_EVENT_PROFILE = AqlTemplate.get("AQL_COUNT_JOURNEY_EVENT_PROFILE");
	
	// list by pagination
	private static final String AQL_COUNT_TOTAL_ACTIVE_JOURNEYS = "RETURN LENGTH(FOR e in " + JourneyMap.COLLECTION_NAME+" FILTER e.status >= 0 RETURN e._key)";
	private static final String AQL_COUNT_JOURNEY_FOR_PAGINATION = AqlTemplate.get("AQL_COUNT_JOURNEY_FOR_PAGINATION");
	private static final String AQL_GET_JOURNEYS_BY_PAGINATION = AqlTemplate.get("AQL_GET_JOURNEYS_BY_PAGINATION");
	
	private static final String AQL_REMOVE_AUTHORIZED_JOURNEY_MAPS_FOR_USER = AqlTemplate.get("AQL_REMOVE_AUTHORIZED_JOURNEY_MAPS_FOR_USER");
	
	static final String AQL_JOURNEY_REPORT_FOR_PROFILE  = AqlTemplate.get("AQL_JOURNEY_REPORT_FOR_PROFILE");
	static final String AQL_PROFILE_REPORT_FOR_JOURNEY = AqlTemplate.get("AQL_PROFILE_REPORT_FOR_JOURNEY");
	
	
	/**
	 * @param d
	 * @return
	 */
	public static String save(JourneyMap d) {
		if (d.dataValidation()) {
			ArangoCollection col = d.getDbCollection();
			String journeyMapId = d.getId();
			if (col != null && StringUtil.isNotEmpty(journeyMapId)) {
				ArangoDatabase db = getCdpDatabase();
				boolean isExisted = ArangoDbUtil.isExistedDocument(db, JourneyMap.COLLECTION_NAME, journeyMapId);
				if (!isExisted) {
					col.insertDocument(d);
				} else {
					d.setUpdatedAt(new Date());
					col.updateDocument(journeyMapId, d, getMergeOptions());
				}
				return journeyMapId;
			}
		}
		return null;
	}

	/**
	 * @param m
	 * @return
	 */
	public static boolean delete(JourneyMap m) {
		ArangoCollection col = m.getDbCollection();
		if (col != null) {
			col.deleteDocument(m.getId());
			return true;
		}
		return false;
	}
	
	public static void removeAllAuthorizedJourneys(String userLogin) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("userLogin", userLogin);
		new ArangoDbCommand<>(db, AQL_REMOVE_AUTHORIZED_JOURNEY_MAPS_FOR_USER, bindVars).update();
	}

	/**
	 * @param id
	 * @return
	 */
	public static JourneyMap getById(String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		JourneyMap p = new ArangoDbCommand<>(db, AQL_GET_JOURNEY_MAP_BY_ID, bindVars, JourneyMap.class)
				.getSingleResult();
		return p;
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static JourneyMap getByName(String name) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("name", name);
		JourneyMap p = new ArangoDbCommand<>(db, AQL_GET_JOURNEY_MAP_BY_NAME, bindVars, JourneyMap.class)
				.getSingleResult();
		return p;
	}
	
	public static List<JourneyMap> getAllJourneyMaps() {
		ArangoDatabase db = getCdpDatabase();
		return new ArangoDbCommand<>(db, AQL_GET_ALL_JOURNEY_MAPS, new HashMap<>(0), JourneyMap.class).getResultsAsList();
	}

	/**
	 * @return
	 */
	public static List<JourneyMap> getAllJourneyMapsForUser(String loginUsername) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("loginUsername", loginUsername);
		return new ArangoDbCommand<>(db, AQL_GET_ALL_JOURNEY_MAPS_FOR_USER, bindVars, JourneyMap.class).getResultsAsList();
	}
	
	/**
	 * @param idList
	 * @return
	 */
	public static Set<JourneyMapRefKey> getRefKeysByIds(List<String> idList) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("idList", idList);
		Set<JourneyMapRefKey> set = new ArangoDbCommand<>(db, AQL_GET_JOURNEY_MAP_REFKEYS_BY_IDS, bindVars, JourneyMapRefKey.class).getResultsAsSet();
		return set;
	}
	
	/**
	 * @param journeyMapId
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @return
	 */
	public static JourneyReport getJourneyProfileStatistics(String journeyMapId, String beginFilterDate, String endFilterDate) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("beginFilterDate", beginFilterDate);
		bindVars.put("endFilterDate", endFilterDate);
		bindVars.put("journeyMapId", journeyMapId);
		JourneyReport rs = new ArangoDbCommand<>(db, AQL_COUNT_PROFILE_WITH_JOURNEY, bindVars,JourneyReport.class).getSingleResult();
		return rs;
	}

	/**
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @return
	 */
	public static JourneyReport getJourneyProfileStatistics(String beginFilterDate, String endFilterDate) {
		return getJourneyProfileStatistics("", beginFilterDate, endFilterDate);
	}
	
	public static JourneyEventStatistics getJourneyMapStatisticsForProfile(String profileId, String journeyMapId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("profileId", profileId);
		bindVars.put("journeyMapId", journeyMapId);
		JourneyEventStatistics rs = new ArangoDbCommand<>(db, AQL_COUNT_JOURNEY_EVENT_PROFILE, bindVars,JourneyEventStatistics.class).getSingleResult();
		if(rs == null) {
			rs = new JourneyEventStatistics();
		}
		return rs;
	}
	
	public static long countTotalJourneyMaps() {
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, AQL_COUNT_TOTAL_ACTIVE_JOURNEYS, Long.class).getSingleResult();
		return c;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static long getTotalRecordsFiltered(DataFilter filter) {
		String aql = AQL_COUNT_JOURNEY_FOR_PAGINATION;
		
		Map<String, Object> bindVars = new HashMap<>(5);
		bindVars.put("searchValue", filter.getFormatedSearchValue());
		bindVars.put("loginUsername", filter.getLoginUsername());
		bindVars.put("hasAdminRole", filter.hasAdminRole());
		
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, aql, bindVars, Long.class).getSingleResult();
		return c;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static List<JourneyMap> queryJourneyMapsByFilter(DataFilter filter) {
		ArangoDatabase db = getCdpDatabase();
		
		// dynamic query builder for filtering data
		String aql = AQL_GET_JOURNEYS_BY_PAGINATION;
		
		Map<String, Object> bindVars = new HashMap<>(10);
		
		bindVars.put("startIndex", filter.getStart());
		bindVars.put("numberResult", filter.getLength());
		bindVars.put("sortField", filter.getSortField("indexScore"));
		bindVars.put("sortDirection", filter.getSortDirection());
		bindVars.put("searchValue", filter.getFormatedSearchValue());
		
		// check permission
		bindVars.put("hasAdminRole", filter.hasAdminRole());
		bindVars.put("loginUsername", filter.getLoginUsername());

		ArangoDbCommand<JourneyMap> q = new ArangoDbCommand<JourneyMap>(db,aql ,bindVars, JourneyMap.class);
		List<JourneyMap> list = q.getResultsAsList();
		return list;
	}

	public static JsonDataTablePayload loadJourneyMapsByFilter(DataFilter filter) {
		int draw = filter.getDraw();
		
		long recordsTotal = countTotalJourneyMaps();
		long recordsFiltered = getTotalRecordsFiltered(filter);
		List<JourneyMap> list = queryJourneyMapsByFilter(filter);
		
		return JsonDataTablePayload.data(filter.getUri(), list, recordsTotal, recordsFiltered, draw);
	}
	
	public static List<TouchpointHubReport> getJourneyReportForProfile(String profileId, String journeyId) {
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("profileId", profileId);
		bindVars.put("journeyId", journeyId);
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointHubReport> finalList = new ArangoDbCommand<>(db, AQL_JOURNEY_REPORT_FOR_PROFILE, bindVars, TouchpointHubReport.class).getResultsAsList();
		return finalList;
	}
	
	public static List<TouchpointHubReport> getProfileReportForJourney(String journeyId) {
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("journeyMapId", journeyId);
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointHubReport> finalList = new ArangoDbCommand<>(db, AQL_PROFILE_REPORT_FOR_JOURNEY, bindVars, TouchpointHubReport.class).getResultsAsList();
		return finalList;
	}

}
