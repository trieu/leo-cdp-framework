package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.TouchpointType;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;
import rfx.core.util.StringUtil;

/**
 * The report counting object for touchpoint and event observer
 * 
 * @author tantrieuf31
 * @since 2022
 */
public final class EventObserverDaoUtil extends AbstractCdpDatabaseUtil {

	static final String AQL_GET_EVENT_OBSERVER_BY_ID = AqlTemplate.get("AQL_GET_EVENT_OBSERVER_BY_ID");
	static final String AQL_GET_EVENT_OBSERVERS_BY_TYPE = AqlTemplate.get("AQL_GET_EVENT_OBSERVERS_BY_TYPE");
	
	static final String AQL_GET_EVENT_OBSERVERS_BY_JOURNEY_MAP = AqlTemplate.get("AQL_GET_EVENT_OBSERVERS_BY_JOURNEY_MAP");
	static final String AQL_FILTER_EVENT_OBSERVERS_BY_JOURNEY_MAP = AqlTemplate.get("AQL_FILTER_EVENT_OBSERVERS_BY_JOURNEY_MAP");
	
	
	static final String AQL_GET_EVENT_OBSERVER_BY_TOUCHPOINT_HUB = AqlTemplate.get("AQL_GET_EVENT_OBSERVER_BY_TOUCHPOINT_HUB");
	static final String AQL_GET_ALL_EVENT_OBSERVERS = AqlTemplate.get("AQL_GET_ALL_EVENT_OBSERVERS");
	static final String AQL_GET_EVENT_OBSERVER_BY_HOSTNAME = AqlTemplate.get("AQL_GET_EVENT_OBSERVER_BY_HOSTNAME");
	
	
	static final String AQL_FIND_KEY_AQL = ArangoDbUtil.contentFindKeyAql(EventObserver.COLLECTION_NAME);

	
	/**
	 * @param e
	 * @return
	 */
	public final static String save(EventObserver e) {
		if (e.dataValidation()) {
			ArangoCollection col = e.getDbCollection();
			if (col != null) {
				String id = e.getId();
				String _key = ArangoDbUtil.findKey(AQL_FIND_KEY_AQL, "id", id);
				if (_key == null) {
					col.insertDocument(e);
				} else {
					e.setUpdatedAt(new Date());
					col.updateDocument(_key, e, getMergeOptions());
				}
				return id;
			}
		}
		return null;
	}
	
	/**
	 * @param o
	 * @return
	 */
	public static boolean delete(EventObserver o) {
		ArangoCollection col = o.getDbCollection();
		if (col != null) {
			col.deleteDocument(o.getId());
			return true;
		}
		return false;
	}

	/**
	 * @param id
	 * @return
	 */
	public static EventObserver getById(String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		EventObserver p = new ArangoDbCommand<EventObserver>(db, AQL_GET_EVENT_OBSERVER_BY_ID, bindVars, EventObserver.class).getSingleResult();
		return p;
	}
	
	/**
	 * @param type
	 * @return
	 */
	public static List<EventObserver> getAllByType(int type) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("type", type);
		List<EventObserver> list = new ArangoDbCommand<EventObserver>(db, AQL_GET_EVENT_OBSERVERS_BY_TYPE, bindVars, EventObserver.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @return
	 */
	public static EventObserver getDefaultDataObserver() {
		List<EventObserver> list = getAllByType(TouchpointType.DATA_OBSERVER);
		if( list.size() > 0 ) {
			return list.get(0);
		}
		return null;
	}
	
	/**
	 * @return
	 */
	public static List<EventObserver> getAllInputDataSourceObserver() {
		List<EventObserver> list = getAllByType(TouchpointType.REDIS_DATA_SOURCE);
		return list;
	}
	
	/**
	 * @param channelId
	 * @return
	 */
	public static EventObserver getByTouchpointHubId(String touchpointHubId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("touchpointHubId", touchpointHubId);
		EventObserver p = new ArangoDbCommand<>(db, AQL_GET_EVENT_OBSERVER_BY_TOUCHPOINT_HUB, bindVars, EventObserver.class).getSingleResult();
		return p;
	}

	/**
	 * @return
	 */
	public static List<EventObserver> listAll() {
		ArangoDatabase db = getCdpDatabase();
		List<EventObserver> list = new ArangoDbCommand<>(db, AQL_GET_ALL_EVENT_OBSERVERS, new HashMap<>(0), EventObserver.class).getResultsAsList();
		return list;
	}

	/**
	 * @param journeyMapId
	 * @return
	 */
	public static List<EventObserver> listAllByJourneyMap(String journeyMapId, String filterKeywords) {
		List<EventObserver> list;
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("journeyMapId", journeyMapId);
		
		if(StringUtil.isNotEmpty(filterKeywords)) {
			String search = "%"+filterKeywords+"%";
			bindVars.put("filterKeywords", search);
			list = new ArangoDbCommand<>(db, AQL_FILTER_EVENT_OBSERVERS_BY_JOURNEY_MAP, bindVars, EventObserver.class).getResultsAsList();
		}
		else {
			list = new ArangoDbCommand<>(db, AQL_GET_EVENT_OBSERVERS_BY_JOURNEY_MAP, bindVars, EventObserver.class).getResultsAsList();
		}
		
		return list;
	}
	
	/**
	 * @param hostname
	 * @return
	 */
	public static EventObserver getEventObserversByHostname(String hostname) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("hostname", hostname);
		EventObserver o = new ArangoDbCommand<>(db, AQL_GET_EVENT_OBSERVER_BY_HOSTNAME, bindVars, EventObserver.class).getSingleResult();
		return o;
	}
}
