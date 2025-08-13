package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.analytics.TouchpointFlowReport;
import leotech.cdp.model.analytics.TouchpointReport;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointType;
import leotech.cdp.query.filters.DataFilter;
import leotech.system.config.AqlTemplate;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;

/**
 * Touchpoint Database Access Object Utility
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class TouchpointDaoUtil extends AbstractCdpDatabaseUtil {

	private static final int MAX_TOUCHPOINT_FOR_ENRICHMENT = 50;
	static final String AQL_GET_TOUCHPOINT_BY_ID = AqlTemplate.get("AQL_GET_TOUCHPOINT_BY_ID");
	static final String AQL_GET_TOUCHPOINT_BY_URL = AqlTemplate.get("AQL_GET_TOUCHPOINT_BY_URL");
	static final String AQL_GET_TOUCHPOINT_BY_NAME = AqlTemplate.get("AQL_GET_TOUCHPOINT_BY_NAME");
	static final String AQL_GET_TOUCHPOINT_BY_URL_AND_NAME = AqlTemplate.get("AQL_GET_TOUCHPOINT_BY_URL_AND_NAME");
	static final String AQL_GET_TOUCHPOINTS_BY_FILTER  = AqlTemplate.get("AQL_GET_TOUCHPOINTS_BY_FILTER");
	
	static final String AQL_GET_TOUCHPOINTS = AqlTemplate.get("AQL_GET_TOUCHPOINTS");
	static final String AQL_GET_TOUCHPOINT_REPORT = AqlTemplate.get("AQL_GET_TOUCHPOINT_REPORT");
	static final String AQL_GET_TOUCHPOINT_REPORT_FOR_PROFILE = AqlTemplate.get("AQL_GET_TOUCHPOINT_REPORT_FOR_PROFILE");
	
	static final String AQL_GET_TOUCHPOINT_FLOW_STATISTICS  = AqlTemplate.get("AQL_GET_TOUCHPOINT_FLOW_STATISTICS");
	
	static final String AQL_GET_TOUCHPOINT_STATISTICS = AqlTemplate.get("AQL_GET_TOUCHPOINT_STATISTICS");

	/**
	 * @param tp
	 * @return
	 */
	public static String save(Touchpoint tp) {
		if (tp.dataValidation()) {
			String id = tp.getId();
			ArangoCollection col = tp.getDbCollection();
			if (col != null) {
				tp.setUpdatedAt(new Date());
				col.insertDocument(tp, optionToUpsertInSilent());
			}
			return id;
		}
		return "";
	}

	/**
	 * @param id
	 * @return
	 */
	public static Touchpoint getById(String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		CallbackQuery<Touchpoint> cb = new CallbackQuery<Touchpoint>() {
			@Override
			public Touchpoint apply(Touchpoint obj) {
				String typeAsType  = TouchpointType.getTypeAsText(obj.getType());
				obj.setTypeAsType(typeAsType);
				return obj;
			}
		};
		Touchpoint p = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINT_BY_ID, bindVars, Touchpoint.class, cb ).getSingleResult();
		return p;
	}
	
	/**
	 * @param url
	 * @param name
	 * @return
	 */
	public static Touchpoint getByUrlAndName(String url, String name) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("url", url);
		bindVars.put("name", name);
		CallbackQuery<Touchpoint> cb = new CallbackQuery<Touchpoint>() {
			@Override
			public Touchpoint apply(Touchpoint obj) {
				String typeAsType  = TouchpointType.getTypeAsText(obj.getType());
				obj.setTypeAsType(typeAsType);
				return obj;
			}
		};
		Touchpoint p = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINT_BY_URL_AND_NAME, bindVars, Touchpoint.class, cb).getSingleResult();
		return p;
	}

	/**
	 * @param url
	 * @return
	 */
	public static Touchpoint getByUrl(String url) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("url", url);
		CallbackQuery<Touchpoint> cb = new CallbackQuery<Touchpoint>() {
			@Override
			public Touchpoint apply(Touchpoint obj) {
				String typeAsType  = TouchpointType.getTypeAsText(obj.getType());
				obj.setTypeAsType(typeAsType);
				return obj;
			}
		};
		Touchpoint p = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINT_BY_URL, bindVars, Touchpoint.class, cb).getSingleResult();
		return p;
	}
	
	/**
	 * @param name
	 * @return
	 */
	public static Touchpoint getByName(String name) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("name", name);
		CallbackQuery<Touchpoint> cb = new CallbackQuery<Touchpoint>() {
			@Override
			public Touchpoint apply(Touchpoint obj) {
				String typeAsType  = TouchpointType.getTypeAsText(obj.getType());
				obj.setTypeAsType(typeAsType);
				return obj;
			}
		};
		Touchpoint p = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINT_BY_NAME, bindVars, Touchpoint.class, cb).getSingleResult();
		return p;
	}

	/**
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Touchpoint> list(int startIndex, int numberResult, String searchPhrase) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		bindVars.put("searchPhrase", searchPhrase);
		CallbackQuery<Touchpoint> cb = new CallbackQuery<Touchpoint>() {
			@Override
			public Touchpoint apply(Touchpoint obj) {
				String typeAsType  = TouchpointType.getTypeAsText(obj.getType());
				obj.setTypeAsType(typeAsType);
				return obj;
			}
		};
		List<Touchpoint> list = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINTS, bindVars, Touchpoint.class, cb).getResultsAsList();
		return list;
	}
	
	/**
	 * @param journeyId
	 * @param touchpointType
	 * @param startIndex
	 * @param numberResult
	 * @return List<TouchpointReport>
	 */
	public static List<TouchpointReport> getTouchpointReport(String journeyId, int touchpointType, String beginFilterDate, String endFilterDate, int startIndex, int numberResult) {
		Map<String, Object> bindVars = new HashMap<>(6);
		bindVars.put("journeyId", journeyId);
		bindVars.put("touchpointType", touchpointType);
		bindVars.put("beginFilterDate", beginFilterDate);
		bindVars.put("endFilterDate", endFilterDate);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointReport> list = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINT_REPORT, bindVars, TouchpointReport.class).getResultsAsList();
		return list;
	}
	
	

	/**
	 * @param refProfileId
	 * @param journeyId
	 * @param touchpointType
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<TouchpointReport> getTouchpointReportForProfile(String refProfileId, String journeyId, int touchpointType, int startIndex, int numberResult) {
		Map<String, Object> bindVars = new HashMap<>(5);
		bindVars.put("refProfileId", refProfileId);
		bindVars.put("journeyId", journeyId);
		bindVars.put("touchpointType", touchpointType);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointReport> list = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINT_REPORT_FOR_PROFILE, bindVars, TouchpointReport.class).getResultsAsList();
		return list;
	}
	
	
	/**
	 * 
	 * get Touchpoint Flow Report Statistics
	 * 
	 * @param refProfileId
	 * @param refJourneyId
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @return
	 */
	public static List<TouchpointFlowReport> getTouchpointFlowReportStatistics(String refProfileId, String refJourneyId, String beginFilterDate, String endFilterDate, int startIndex, int numberFlow) {
		Map<String, Object> bindVars = new HashMap<>(6);
		bindVars.put("refProfileId", refProfileId);
		bindVars.put("refJourneyId", refJourneyId);
		bindVars.put("beginFilterDate", beginFilterDate);
		bindVars.put("endFilterDate", endFilterDate);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberFlow", numberFlow);
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointFlowReport> list = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINT_FLOW_STATISTICS, bindVars, TouchpointFlowReport.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param refProfileId
	 * @param refJourneyId
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @param startIndex
	 * @param numberFlow
	 * @return
	 */
	public static List<TouchpointFlowReport> getTouchpointStatistics(String refProfileId, String refJourneyId, String beginFilterDate, String endFilterDate, int startIndex, int numberFlow) {
		Map<String, Object> bindVars = new HashMap<>(6);
		bindVars.put("refProfileId", refProfileId);
		bindVars.put("refJourneyId", refJourneyId);
		bindVars.put("beginFilterDate", beginFilterDate);
		bindVars.put("endFilterDate", endFilterDate);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberFlow", numberFlow);
		
		ArangoDatabase db = getCdpDatabase();
		List<TouchpointFlowReport> list = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINT_STATISTICS, bindVars, TouchpointFlowReport.class).getResultsAsList();
		return list;
	}
	

	/**
	 * @param profileId
	 * @return
	 */
	public static List<TouchpointReport> getTopTouchpointsForEnrichment(String profileId) {
		List<TouchpointReport> tps = TouchpointDaoUtil.getTouchpointReportForProfile(profileId, "", -1, 0, MAX_TOUCHPOINT_FOR_ENRICHMENT);
		return tps;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload filter(DataFilter filter) {
		ArangoDatabase db = getCdpDatabase();

		//TODO dynamic query builder for filtering data
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("startIndex", filter.getStart());
		bindVars.put("numberResult", filter.getLength());
		
		CallbackQuery<Touchpoint> cb = new CallbackQuery<Touchpoint>() {
			@Override
			public Touchpoint apply(Touchpoint obj) {
				String typeAsType  = TouchpointType.getTypeAsText(obj.getType());
				obj.setTypeAsType(typeAsType);
				return obj;
			}
		};
		List<Touchpoint> list = new ArangoDbCommand<>(db, AQL_GET_TOUCHPOINTS_BY_FILTER, bindVars, Touchpoint.class, cb).getResultsAsList();
		
		long recordsTotal = countTotalOfTouchpoints();
		int recordsFiltered = list.size();
		int draw = filter.getDraw();
		JsonDataTablePayload payload =  JsonDataTablePayload.data(filter.getUri(), list, recordsTotal, recordsFiltered, draw);
		return payload;
	}
	
	/**
	 * @return
	 */
	public static long countTotalOfTouchpoints() {
		ArangoDatabase db = getCdpDatabase();
		long c = db.collection(Touchpoint.COLLECTION_NAME).count().getCount();
		return c;
	}



}
