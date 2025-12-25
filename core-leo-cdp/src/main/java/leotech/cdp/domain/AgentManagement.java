package leotech.cdp.domain;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import leotech.cdp.dao.AgentDaoUtil;
import leotech.cdp.model.activation.ActivationRule;
import leotech.cdp.model.activation.ActivationRuleType;
import leotech.cdp.model.activation.Agent;
import leotech.cdp.model.customer.Segment;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.SystemUser;
import rfx.core.util.FileUtils;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * AI Agent Management
 * 
 * @author tantrieuf31
 * @since 2025
 *
 */
public final class AgentManagement {

	public static final String INIT_DATA_SERVICE_JSON = "./resources/data-for-new-setup/init-agent-configs.json";

	/**
	 * @return List<DataService> from file
	 */
	static List<Agent> loadFromJsonFile() {
		List<Agent> list = null;
		try {
			Type listType = new TypeToken<ArrayList<Agent>>() {
			}.getType();
			String json = FileUtils.readFileAsString(INIT_DATA_SERVICE_JSON);
			list = new Gson().fromJson(json, listType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (list == null) {
			list = new ArrayList<>(0);
		}
		return list;
	}

	/**
	 * init default data for database
	 */
	public static void initDefaultData(boolean overideOldData) {
		try {
			List<Agent> list = loadFromJsonFile();
			for (Agent service : list) {
				if (service.getStatus() >= 0) {
					AgentManagement.save(service, overideOldData);
					Utils.sleep(500);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * save the service
	 * 
	 * @param service
	 * @param overideOldData
	 * @return
	 */
	public static String save(Agent service, boolean overideOldData) {
		return AgentDaoUtil.save(service, overideOldData);
	}

	/**
	 * get the service
	 * 
	 * @param id
	 * @return DataService
	 */
	public static Agent getById(String id) {
		return AgentDaoUtil.getById(id);
	}

	/**
	 * delete the service
	 * 
	 * @param id
	 * @return
	 */
	public static String deleteById(String id) {
		return AgentDaoUtil.deleteById(id);
	}

	/**
	 * get all active dataServices in database
	 * 
	 * @return List<DataService>
	 */
	public static List<Agent> getAllActiveAgents() {
		return AgentDaoUtil.getAllActiveAgents();
	}

	/**
	 * @param keywords
	 * @return
	 */
	public static List<Agent> list(int startIndex, int numberResult, String keywords, String filterServiceValue,
			boolean forSynchronization, boolean forDataEnrichment, boolean forPersonalization) {
		return AgentDaoUtil.list(startIndex, numberResult, keywords, filterServiceValue, forSynchronization,
				forDataEnrichment, forPersonalization);
	}

	/**
	 * open the agent, then activate segment data with an event
	 * 
	 * @param purpose
	 * @param agentId
	 * @param delayMinutes
	 * @param schedulingTime
	 * @param segmentId
	 * @param eventName
	 * @return
	 */
	public static String activate(SystemUser loginUser, String purpose, String agentId, String timeToStart,
			int schedulingTime, int timeUnit, String segmentId, String triggerEventName) {
		String userLogin = loginUser.getUserLogin();

		Agent agent = AgentDaoUtil.getById(agentId);
		Segment segment = SegmentDataManagement.getSegmentWithActivationRulesById(segmentId);
		if (agent != null && segment != null) {
			if (agent.isReadyToRun()) {
				String activationType = ActivationRuleType.RUN_AGENT;
				int priority = segment.getIndexScore();
				String defaultName = StringUtil.join("-", purpose, agentId, segmentId);
				String description = "";
				ActivationRule activationRule = ActivationRule.create(userLogin, purpose, activationType, defaultName,
						description, priority, agentId, segmentId, timeToStart, schedulingTime, timeUnit,
						triggerEventName);
				String activationRuleId = ActivationRuleManagement.save(activationRule);
				if (activationRuleId != null) {
					return activationRuleId;
				} else {
					throw new InvalidDataException("Can not create DataServiceScheduler from agentId: " + agentId);
				}
			} else {
				throw new InvalidDataException(agent.getId() + " is not ready to run, please set service_api_key");
			}
		} else {
			throw new InvalidDataException(" In-valid segmentId:" + segmentId + " or In-valid agentId: " + agentId);
		}
	}

}
