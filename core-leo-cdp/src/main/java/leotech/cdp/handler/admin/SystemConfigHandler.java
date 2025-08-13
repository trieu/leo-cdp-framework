package leotech.cdp.handler.admin;

import java.util.List;

import com.google.gson.Gson;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.job.scheduled.JobCaller;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.domain.SystemConfigsManagement;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemService;
import leotech.system.model.SystemUser;
import rfx.core.util.StringUtil;

/**
 *  CDP System Config Handler
 * 
 * @author tantrieuf31
 *
 */
public final class SystemConfigHandler extends SecuredHttpDataHandler {

	static final String SAVE_SYSTEM_CONFIG = "/cdp/system-config/save";
	static final String CALL_DATA_QUALITY_COMPUTE_JOB = "/cdp/system-config/call-data-quality-compute-job";

	static final String LIST_ALL_SYSTEM_CONFIGS = "/cdp/system-config/list-all";

	static final String GET_BY_ID = "/cdp/system-config/get-by-id";

	public SystemConfigHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (isSuperAdminRole(loginUser)) {
			switch (uri) {
			case SAVE_SYSTEM_CONFIG: {
				String objectJson = paramJson.getString("objectJson", "");
				if (StringUtil.isNotEmpty(objectJson)) {
					SystemService config = new Gson().fromJson(objectJson, SystemService.class);
					String id = SystemConfigsManagement.saveSystemConfig(config);
					return JsonDataPayload.ok(uri, id, loginUser, SystemService.class);
				}
				return JsonDataPayload.fail("objectJson of SystemConfig is empty", 500);
			}
			case CALL_DATA_QUALITY_COMPUTE_JOB: {
				JobCaller.doDataQualityComputeJob();
				return JsonDataPayload.ok(uri, true, loginUser, SystemService.class);
			}
			default:
				return JsonErrorPayload.NO_HANDLER_FOUND;
			}
		}
		return JsonErrorPayload.NO_AUTHORIZATION;
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (isSuperAdminRole(loginUser)) {
			switch (uri) {
			case LIST_ALL_SYSTEM_CONFIGS: {
				List<SystemService> configs = SystemConfigsManagement.getAllSystemServices();
				return JsonDataPayload.ok(uri, configs, loginUser, SystemService.class);
			}
			case GET_BY_ID: {
				String id = StringUtil.safeString(params.get("id"));
				SystemService config = SystemConfigsManagement.getSystemServiceById(id);
				return JsonDataPayload.ok(uri, config, loginUser, SystemService.class);
			}
			default:
				return JsonErrorPayload.NO_HANDLER_FOUND;
			}
		} else {
			return JsonErrorPayload.NO_AUTHORIZATION;
		}
	}

}
