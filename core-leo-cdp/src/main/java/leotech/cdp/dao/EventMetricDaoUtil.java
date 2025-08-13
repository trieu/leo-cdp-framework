package leotech.cdp.dao;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.journey.EventMetric;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;

/**
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class EventMetricDaoUtil extends AbstractCdpDatabaseUtil {
	
	static final String AQL_GET_EVENT_METRICS_BY_FLOW_NAME = AqlTemplate.get("AQL_GET_EVENT_METRICS_BY_FLOW_NAME");
	static final String AQL_DELETE_EVENT_METRICS_BY_FLOW_NAME = AqlTemplate.get("AQL_DELETE_EVENT_METRICS_BY_FLOW_NAME");
	
	/**
	 * @param eventMetric
	 * @return
	 */
	public static String save(EventMetric eventMetric, boolean forceUpdate) {
		if (eventMetric.dataValidation()) {
			eventMetric.buildHashedId();
			ArangoCollection col = eventMetric.getDbCollection();
			if (col != null) {
				String id = eventMetric.getId();
				boolean isUpdate = ArangoDbUtil.isExistedDocument(EventMetric.COLLECTION_NAME, id );
				if (isUpdate) {
					if(forceUpdate) {
						eventMetric.setUpdatedAt(new Date());
						col.updateDocument(id, eventMetric, getMergeOptions());
					}
				} else {
					id = col.insertDocument(eventMetric).getKey();
				}
				return id;
			}
		}
		return null;
	}
	
	/**
	 * @param eventMetric
	 * @return
	 */
	public static String delete(EventMetric eventMetric) {
		ArangoCollection col = eventMetric.getDbCollection();
		if (col != null) {
			String id = eventMetric.getId();
			col.deleteDocument(id);
			return id;
		}
		return null;
	}
	
	/**
	 * @param flowName
	 */
	public static void deleteByFlowName(String flowName) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("flowName", flowName);
		new ArangoDbCommand<>(db, AQL_DELETE_EVENT_METRICS_BY_FLOW_NAME, bindVars).update();
	}
	
	/**
	 * @param flowName
	 * @return
	 */
	public static List<EventMetric> getEventMetricsByFlowName(String flowName) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("flowName", flowName);
		List<EventMetric> list = new ArangoDbCommand<EventMetric>(db, AQL_GET_EVENT_METRICS_BY_FLOW_NAME, bindVars, EventMetric.class).getResultsAsList();
		Collections.sort(list, new Comparator<EventMetric>() {
			@Override
			public int compare(EventMetric o1, EventMetric o2) {
				if (o1.getScore() < o2.getScore()) {
					return 1;
				} else if (o1.getScore() > o2.getScore()) {
					return -1;
				}
				return 0;
			}
		});
		return list;
	}
}
