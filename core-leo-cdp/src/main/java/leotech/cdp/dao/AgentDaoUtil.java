package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.activation.Agent;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;
import rfx.core.util.StringUtil;

/**
 * Data Service DAO
 * 
 * @author tantrieuf31
 * @since 2023
 */
public final class AgentDaoUtil extends AbstractCdpDatabaseUtil {

	static final String AQL_FIND_KEY_AQL = ArangoDbUtil.contentFindKeyAql(Agent.COLLECTION_NAME);
	static final String AQL_GET_AGENT_BY_ID = AqlTemplate.get("AQL_GET_AGENT_BY_ID");
	static final String AQL_LIST_AGENTS = AqlTemplate.get("AQL_LIST_AGENTS");
	static final String AQL_LIST_AGENTS_WITH_FILTER = AqlTemplate.get("AQL_LIST_AGENTS_WITH_FILTER");
	static final String AQL_LIST_ALL_ACTIVE_AGENTS = AqlTemplate.get("AQL_LIST_ALL_ACTIVE_AGENTS");

	/**
	 * save a service
	 * 
	 * @param agent
	 * @return the ID of service
	 */
	public static String save(Agent agent, boolean overideOldData) {
		if (agent.dataValidation()) {
			ArangoCollection col = agent.getDbCollection();
			if (col != null) {
				String id = agent.getId();
				agent.setUpdatedAt(new Date());
				
				String _key = ArangoDbUtil.findKey(AQL_FIND_KEY_AQL, "id", id);
				if(overideOldData || _key == null) {
					col.insertDocument(agent, optionToUpsertInSilent());
				}
				else {
					col.updateDocument(id, agent, getMergeOptions());
				}
				return id;
			}
		}
		else {
			throw new IllegalArgumentException("service.dataValidation is false \n " + agent.toString() );
		}
		return null;
	}

	/**
	 * @param id
	 * @return
	 */
	public static Agent getById(String id) {
		Agent cat = null;
		if(StringUtil.isNotEmpty(id)) {
			ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("id", id);
			cat = new ArangoDbCommand<Agent>(db, AQL_GET_AGENT_BY_ID, bindVars, Agent.class).getSingleResult();
		}
		return cat;
	}

	/**
	 * @param id
	 * @return
	 */
	public static String deleteById(String id) throws ArangoDBException {
		ArangoCollection col = new Agent().getDbCollection();
		if (col != null) {
			col.deleteDocument(id);
			return id;
		}
		return "";
	}

	
	/**
	 * @return
	 */
	public static List<Agent> getAllActiveAgents() {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(0);
		List<Agent> list = new ArangoDbCommand<Agent>(db, AQL_LIST_ALL_ACTIVE_AGENTS, bindVars, Agent.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param keywords
	 * @param onlyActive
	 * @return
	 */
	public static List<Agent> list(int startIndex, int numberResult, String keywords, String filterServiceValue, boolean forSynchronization, boolean forDataEnrichment, boolean forPersonalization) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(6);
		bindVars.put("keywords", "%"+keywords+"%");
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		String aql;
		if(Agent.ALL.equals(filterServiceValue) || StringUtil.isEmpty(filterServiceValue)) {
			aql = AQL_LIST_AGENTS;
		}
		else {
			aql = AQL_LIST_AGENTS_WITH_FILTER;
			bindVars.put("forSynchronization", forSynchronization);
			bindVars.put("forDataEnrichment", forDataEnrichment);
			bindVars.put("forPersonalization", forPersonalization);
		}
		
		List<Agent> list = new ArangoDbCommand<Agent>(db, aql, bindVars, Agent.class).getResultsAsList();
		return list;
	}

}
