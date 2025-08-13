package leotech.cdp.dao;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.journey.DataFlowStage;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;

public class DataFlowStageDaoUtil extends AbstractCdpDatabaseUtil {
	
	static final String AQL_GET_ALL_DATA_FLOW_STAGES = AqlTemplate.get("AQL_GET_ALL_DATA_FLOW_STAGES");
	static final String AQL_GET_DATA_FLOW_STAGES_BY_FLOW_NAME = AqlTemplate.get("AQL_GET_DATA_FLOW_STAGES_BY_FLOW_NAME");
	static final String AQL_DELETE_DATA_FLOW_STAGES_BY_FLOW_NAME = AqlTemplate.get("AQL_DELETE_DATA_FLOW_STAGES_BY_FLOW_NAME");
	static final String AQL_GET_DATA_FLOW_STAGE_BY_ID = AqlTemplate.get("AQL_GET_DATA_FLOW_STAGE_BY_ID");

	public static String save(DataFlowStage flowStage, boolean forceUpdate) {
		if (flowStage.dataValidation()) {
			ArangoCollection col = flowStage.getDbCollection();
			if (col != null) {
				String id = flowStage.getId();
				boolean isUpdate = ArangoDbUtil.isExistedDocument(DataFlowStage.COLLECTION_NAME, id );
				if (isUpdate) {
					if(forceUpdate) {
						flowStage.setUpdatedAt(new Date());
						col.updateDocument(id, flowStage, getUpdateOptions());
					}
				} else {
					id = col.insertDocument(flowStage).getKey();
				}
				return id;
			}
		}
		return null;
	}
	
	public static List<DataFlowStage> getDataFlowStages(String flowName) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("flowName", flowName);
		List<DataFlowStage> list = new ArangoDbCommand<DataFlowStage>(db, AQL_GET_DATA_FLOW_STAGES_BY_FLOW_NAME, bindVars, DataFlowStage.class).getResultsAsList();
		Collections.sort(list, new Comparator<DataFlowStage>() {
			@Override
			public int compare(DataFlowStage o1, DataFlowStage o2) {
				if (o1.getOrderIndex() < o2.getOrderIndex()) {
					return -1;
				} else if (o1.getOrderIndex() > o2.getOrderIndex()) {
					return 1;
				}
				return 0;
			}
		});
		return list;
	}
	
	public static List<DataFlowStage> getAllDataFlowStages() {
		ArangoDatabase db = getCdpDatabase();
		List<DataFlowStage> list = new ArangoDbCommand<DataFlowStage>(db, AQL_GET_ALL_DATA_FLOW_STAGES, DataFlowStage.class).getResultsAsList();
		return list;
	}
	
	public static DataFlowStage getDataFlowStageById(String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		DataFlowStage obj = new ArangoDbCommand<DataFlowStage>(db, AQL_GET_DATA_FLOW_STAGE_BY_ID, bindVars, DataFlowStage.class).getSingleResult();
		return obj;
	}
	
	
	/**
	 * @param flowName
	 */
	public static void deleteByFlowName(String flowName) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("flowName", flowName);
		new ArangoDbCommand<>(db, AQL_DELETE_DATA_FLOW_STAGES_BY_FLOW_NAME, bindVars).update();
	}
}
