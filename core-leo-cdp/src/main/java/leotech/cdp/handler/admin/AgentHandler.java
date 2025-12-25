package leotech.cdp.handler.admin;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import leotech.cdp.data.DataServiceJob;
import leotech.cdp.domain.ActivationRuleManagement;
import leotech.cdp.domain.AgentManagement;
import leotech.cdp.model.activation.ActivationRule;
import leotech.cdp.model.activation.Agent;
import leotech.cdp.model.activation.TimeUnitCode;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class AgentHandler extends SecuredHttpDataHandler {

	// for Admin, only for ROLE_SUPER_ADMIN
	static final String CONFIG_OAUTH2_CALLBACK = "/cdp/agent/config/oauth2-callback";

	static final String LIST = "/cdp/agent/list";
	static final String SAVE = "/cdp/agent/save";
	static final String GET = "/cdp/agent/get";
	static final String DELETE = "/cdp/agent/delete";
	
	static final String CREATE_ACTIVATION = "/cdp/agent/create-activation";
	static final String STOP_ACTIVATION = "/cdp/agent/stop-activation";
	static final String START_ACTIVATION = "/cdp/agent/start-activation";
	static final String REMOVE_ACTIVATION = "/cdp/agent/remove-activation";
	static final String MANUALLY_RUN_ACTIVATION = "/cdp/agent/manually-run-activation";

	
	
	public AgentHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}

	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			if (isAuthorized(loginUser, Agent.class)) {
				switch (uri) {
				case SAVE: {
					String objectJson = paramJson.getString("objectJson", "");
					if (StringUtil.isNotEmpty(objectJson)) {
						Agent c = new Gson().fromJson(objectJson, Agent.class);
						String id;
						try {
							id = AgentManagement.save(c, true);
							if(c.getId().equals(id)) {
								return JsonDataPayload.ok(uri, id, loginUser, Agent.class);
							}
							return JsonDataPayload.fail("Can not save DataService.id: " + id, 500);
						} catch (Exception e) {
							return JsonDataPayload.fail(e.getMessage(), 500);
						}
					}
					return JsonDataPayload.fail("objectJson of DataService is empty", 500);
				}
				case DELETE: {
					String dataServiceId = paramJson.getString("dataServiceId", "");
					if (StringUtil.isNotEmpty(dataServiceId)) {
						String id = AgentManagement.deleteById(dataServiceId);
						if(dataServiceId.equals(id)) {
							return JsonDataPayload.ok(uri, id, loginUser, Agent.class);
						}
						else {
							return JsonDataPayload.fail("Can not delete dataServiceId "+id, 500);
						}
					}
					else {
						return JsonDataPayload.fail("dataServiceId is empty ", 500);
					}
				}
				case CREATE_ACTIVATION: {
					String purpose = HttpWebParamUtil.getString(paramJson,"purpose","");
					
					String dataServiceId = HttpWebParamUtil.getString(paramJson,"dataServiceId", "");
					if (StringUtil.isEmpty(dataServiceId)) {
						return JsonDataPayload.fail("dataServiceId is empty ", 500);
					}

					String segmentId = HttpWebParamUtil.getString(paramJson,"segmentId", "");
					if (StringUtil.isEmpty(segmentId)) {
						return JsonDataPayload.fail("segmentId is empty ", 500);
					}
					
					// default Do immediately and only once
					String timeToStart = HttpWebParamUtil.getString(paramJson, "timeToStart", "");
					int schedulingTime = HttpWebParamUtil.getInteger(paramJson, "schedulingTime", 0);
					int timeUnit = HttpWebParamUtil.getInteger(paramJson, "timeUnit", TimeUnitCode.SECOND);

					String triggerEventName = HttpWebParamUtil.getString(paramJson,"triggerEventName", DataServiceJob.EVENT_RUN_DEFAULT_JOB);
					if (StringUtil.isEmpty(triggerEventName)) {
						return JsonDataPayload.fail("eventName is empty ", 500);
					}
					
					// ok, ready to run
					String id = AgentManagement.activate(loginUser, purpose, dataServiceId, timeToStart, schedulingTime, timeUnit, segmentId, triggerEventName);
					if (id != null) {
						return JsonDataPayload.ok(uri, id, loginUser, Agent.class);
					}
					return JsonDataPayload.fail("Create Activation Rule is failed for the dataServiceId: " + dataServiceId, 500);
				}
				case START_ACTIVATION: {
					String activationRuleId = paramJson.getString("activationRuleId", "");
					if (StringUtil.isNotEmpty(activationRuleId)) {
						boolean ok = ActivationRuleManagement.startActivation(activationRuleId);
						return JsonDataPayload.ok(uri, ok, loginUser, ActivationRule.class);
					}
					else {
						return JsonDataPayload.fail("activationRuleId is empty ", 500);
					}
				}
				case MANUALLY_RUN_ACTIVATION: {
					String activationRuleId = paramJson.getString("activationRuleId", "");
					if (StringUtil.isNotEmpty(activationRuleId)) {
						boolean ok = ActivationRuleManagement.manuallyRunActivation(activationRuleId);
						return JsonDataPayload.ok(uri, ok, loginUser, ActivationRule.class);
					}
					else {
						return JsonDataPayload.fail("activationRuleId is empty ", 500);
					}
				}
				case STOP_ACTIVATION: {
					String activationRuleId = paramJson.getString("activationRuleId", "");
					if (StringUtil.isNotEmpty(activationRuleId)) {
						boolean ok = ActivationRuleManagement.stopActivation(activationRuleId);
						return JsonDataPayload.ok(uri, ok, loginUser, ActivationRule.class);
					}
					else {
						return JsonDataPayload.fail("activationRuleId is empty ", 500);
					}
				}
				case REMOVE_ACTIVATION: {
					String activationRuleId = paramJson.getString("activationRuleId", "");
					if (StringUtil.isNotEmpty(activationRuleId)) {
						boolean ok = ActivationRuleManagement.removeActivation(activationRuleId);
						return JsonDataPayload.ok(uri, ok, loginUser, ActivationRule.class);
					}
					else {
						return JsonDataPayload.fail("activationRuleId is empty ", 500);
					}
				}
				default:
					return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			}
			return JsonErrorPayload.NO_AUTHORIZATION;
		} else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
		if (uri.startsWith(CONFIG_OAUTH2_CALLBACK)) {
			return JsonDataPayload.ok(uri, "OK");
		}
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			if (isAuthorized(loginUser, Agent.class)) {
				switch (uri) {
				case GET: {
					String dataServiceId = HttpWebParamUtil.getString(params, "dataServiceId");
					String dataServiceName = HttpWebParamUtil.getString(params, "dataServiceName");
					Agent c;
					if (StringUtil.isNotEmpty(dataServiceId)) {
						c = AgentManagement.getById(dataServiceId);
					} 
					else if (StringUtil.isNotEmpty(dataServiceName)) {
						c = new Agent(dataServiceName);
					}
					else {
						c = new Agent();
					}
					return JsonDataPayload.ok(uri, c, loginUser, Agent.class);
				}
				case LIST: {
					String keywords = HttpWebParamUtil.getString(params, "keywords");
					String filterServiceValue = HttpWebParamUtil.getString(params, "filterServiceValue", "all");
					boolean forSynchronization = HttpWebParamUtil.getBoolean(params, "forSynchronization", true);
					boolean forDataEnrichment = HttpWebParamUtil.getBoolean(params, "forDataEnrichment", true);
					boolean forPersonalization = HttpWebParamUtil.getBoolean(params, "forPersonalization", true);
					int startIndex = HttpWebParamUtil.getInteger(params, "startIndex", 0);
					int numberResult  = HttpWebParamUtil.getInteger(params, "numberResult", 2000);
					
					List<Agent> services = AgentManagement.list(startIndex,numberResult, keywords, filterServiceValue, forSynchronization, forDataEnrichment, forPersonalization);
					return JsonDataPayload.ok(uri, services, loginUser, Agent.class);
				}
				default:
					return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			} else {
				return JsonErrorPayload.NO_AUTHORIZATION;
			}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}
}
