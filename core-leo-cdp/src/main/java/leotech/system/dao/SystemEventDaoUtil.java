package leotech.system.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.google.gson.Gson;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.query.SystemEventQueryBuilder;
import leotech.cdp.query.filters.SystemEventFilter;
import leotech.system.config.AqlTemplate;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemEvent;
import leotech.system.util.database.ArangoDbCommand;

/**
 * System Event DAO
 * 
 * @author tantrieuf31
 * @since 2023
 *
 */
public class SystemEventDaoUtil extends AbstractCdpDatabaseUtil {

	static final String AQL_DELETE_SYSTEM_EVENTS_BY_USER_LOGIN = AqlTemplate.get("AQL_DELETE_SYSTEM_EVENTS_BY_USER_LOGIN");
	static final String AQL_GET_SYSTEM_EVENTS_BY_USER_LOGIN = AqlTemplate.get("AQL_GET_SYSTEM_EVENTS_BY_USER_LOGIN");
	static final String AQL_GET_SYSTEM_EVENTS_BY_OBJECT_NAME_AND_ID = AqlTemplate.get("AQL_GET_SYSTEM_EVENTS_BY_OBJECT_NAME_AND_ID");
	static final String AQL_GET_SYSTEM_EVENTS_BY_OBJECT_NAME = AqlTemplate.get("AQL_GET_SYSTEM_EVENTS_BY_OBJECT_NAME");
	

	/**
	 * @param systemEvent
	 * @return the ID of SystemEvent
	 */
	public static String save(SystemEvent systemEvent) {
		if (systemEvent.dataValidation()) {
			try {
				ArangoCollection col = systemEvent.getDbCollection();
				if (col != null) {
					col.insertDocument(systemEvent, optionToUpsertInSilent());
					return systemEvent.getId();
				}
			} catch (Exception e) {
				// skip
			}
		} else {
			String json = new Gson().toJson(systemEvent);
			System.err.println("invalid SystemEvent \n" + json);
		}
		return null;
	}

	/**
	 * @param userLogin
	 */
	public static void deleteByUserLogin(String userLogin) {
		try {
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("userLogin", userLogin);
			new ArangoDbCommand<String>(getCdpDatabase(), AQL_DELETE_SYSTEM_EVENTS_BY_USER_LOGIN, bindVars).update();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param userLogin
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<SystemEvent> getByUserLogin(String userLogin, int startIndex, int numberResult) {
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("userLogin", userLogin);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<SystemEvent> list = new ArangoDbCommand<>(getCdpDatabase(), AQL_GET_SYSTEM_EVENTS_BY_USER_LOGIN, bindVars, SystemEvent.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param objectName
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<SystemEvent> getByObjectName(String objectName, int startIndex, int numberResult) {
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("objectName", objectName);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<SystemEvent> list = new ArangoDbCommand<>(getCdpDatabase(), AQL_GET_SYSTEM_EVENTS_BY_OBJECT_NAME, bindVars, SystemEvent.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param objectName
	 * @param objectId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<SystemEvent> getByObjectNameAndId(String objectName, String objectId, int startIndex, int numberResult) {
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("objectName", objectName);
		bindVars.put("objectId", objectId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<SystemEvent> list = new ArangoDbCommand<>(getCdpDatabase(), AQL_GET_SYSTEM_EVENTS_BY_OBJECT_NAME_AND_ID, bindVars, SystemEvent.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public final static JsonDataTablePayload listByFilter(SystemEventFilter filter){
		long recordsTotal = getTotalRecordsFiltered(filter);
		List<SystemEvent> list;
		if(recordsTotal > 0) {
			list = getSystemEventsByFilter(filter);
		} else {
			list = new ArrayList<>(0);
		}
		int draw = filter.getDraw();
		JsonDataTablePayload payload = JsonDataTablePayload.data(filter.getUri(), list, recordsTotal, recordsTotal, draw);
		return payload;
	}
	
	public final static long getTotalRecordsFiltered(SystemEventFilter filter) {
		ArangoDatabase db = getCdpDatabase();
		String aql = SystemEventQueryBuilder.buildAqlFromSystemEventFilter(filter, true);
		long totalFiltered =  new ArangoDbCommand<Long>(db, aql, Long.class).getSingleResult();
		return totalFiltered;
	}
	
	
	public final static List<SystemEvent> getSystemEventsByFilter(SystemEventFilter filter) {
		
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("startIndex", filter.getStart());
		bindVars.put("numberResult", filter.getLength());
		
		String aql = SystemEventQueryBuilder.buildAqlFromSystemEventFilter(filter, false);
		
		ArangoDbCommand<SystemEvent> q = new ArangoDbCommand<>(getCdpDatabase(), aql, bindVars, SystemEvent.class);
		return q.getResultsAsList();
	}

}
