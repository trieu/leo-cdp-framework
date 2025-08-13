package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.activation.ActivationRule;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import rfx.core.util.StringUtil;

/**
 * Activation Rule Database Access Object
 * 
 * @author tantrieuf31 
 * @since 2023
 *
 */
public class ActivationRuleDao extends AbstractCdpDatabaseUtil {
	
	static final String AQL_GET_ACTIVE_ACTIVATION_RULES_BY_SERVICE_ID = AqlTemplate.get("AQL_GET_ACTIVE_ACTIVATION_RULES_BY_SERVICE_ID");
	static final String AQL_GET_ACTIVATION_RULES_BY_SEGMENT_ID = AqlTemplate.get("AQL_GET_ACTIVATION_RULES_BY_SEGMENT_ID");
	static final String AQL_GET_ACTIVATION_RULE_BY_ID = AqlTemplate.get("AQL_GET_ACTIVATION_RULE_BY_ID");
	static final String AQL_REMOVE_ACTIVATION_RULE_BY_ID = AqlTemplate.get("AQL_REMOVE_ACTIVATION_RULE_BY_ID");
	static final String AQL_IS_ACTIVATION_RULE_READY_TO_RUN = AqlTemplate.get("AQL_IS_ACTIVATION_RULE_READY_TO_RUN");
	

	/**
	 * save ActivationRule
	 * 
	 * @param rule
	 * @return saved ID
	 */
	public static String save(ActivationRule rule) {
		if (rule.dataValidation()) {
			ArangoCollection col = rule.getDbCollection();
			if (col != null) {
				String id = rule.getId();
				rule.setUpdatedAt(new Date());
				col.insertDocument(rule, optionToUpsertInSilent());
				return id;
			}
		}
		else {
			throw new IllegalArgumentException("ActivationRule.dataValidation is false \n " + rule.toString() );
		}
		return null;
	}
	
	/**
	 * delete ActivationRule
	 * 
	 * @param rule
	 * @return deleted ID
	 */
	public static String delete(ActivationRule rule) {
		if (rule != null) {
			ArangoCollection col = rule.getDbCollection();
			if (col != null) {
				String id = rule.getId();
				col.deleteDocument(id);
				return id;
			}
		}
		return null;
	}
	
	/**
	 * get all active activation rules to start data service service
	 * 
	 * @param dataServiceId
	 * @return
	 */
	public static List<ActivationRule> getAllActiveActivationRulesForDataService(String dataServiceId){
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("dataServiceId", dataServiceId);
		String aql = AQL_GET_ACTIVE_ACTIVATION_RULES_BY_SERVICE_ID;
		List<ActivationRule> list = new ArangoDbCommand<>(db, aql, bindVars, ActivationRule.class).getResultsAsList();
		return list;
	}
	
	/**
	 * get all activation rules for a segment
	 * 
	 * @param segmentId
	 * @return
	 */
	public static List<ActivationRule> getAllActivationRulesForSegment(String segmentId){
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("segmentId", segmentId);
		String aql = AQL_GET_ACTIVATION_RULES_BY_SEGMENT_ID;
		List<ActivationRule> list = new ArangoDbCommand<>(db, aql, bindVars, ActivationRule.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static ActivationRule getActivationRuleById(String id){
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		String aql = AQL_GET_ACTIVATION_RULE_BY_ID;
		ActivationRule obj = new ArangoDbCommand<>(db, aql, bindVars, ActivationRule.class).getSingleResult();
		return obj;
	}
	
	/**
	 * delete activation rule and return true if the removed id == the parameter id 
	 * 
	 * @param id
	 */
	public static boolean removeActivationRuleById(String id){
		if(StringUtil.isNotEmpty(id)) {
			ArangoDatabase db = getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("id", id);
			String removedId = new ArangoDbCommand<>(db, AQL_REMOVE_ACTIVATION_RULE_BY_ID, bindVars).remove();
			return id.equals(removedId);
		}
		return false;
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static boolean isActivationRuleReadyToRun(String id){
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("activationRuleId", id);
		Boolean rs = new ArangoDbCommand<Boolean>(db,
				AQL_IS_ACTIVATION_RULE_READY_TO_RUN, bindVars, Boolean.class).getSingleResult();
		return Boolean.TRUE.equals(rs);
	}
}
