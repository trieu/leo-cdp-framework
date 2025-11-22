package leotech.cdp.handler.admin;

import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.query.filters.SystemEventFilter;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.domain.SystemControl;
import leotech.system.domain.SystemEventManagement;
import leotech.system.domain.SystemUserManagement;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.LogUtil;
import leotech.system.version.SystemEnviroment;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 *  CDP System Config Handler
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class SystemControlHandler extends SecuredHttpDataHandler {

	static final String SAVE_SYSTEM_CONTROL_SETUP = "/cdp/system-control/setup";
	static final String LIST_SYSTEM_ACTION_LOGS = "/cdp/system-control/action-logs";
	static final String UPGRADE_SYSTEM_CONTROL = "/cdp/system-control/upgrade";
	static final String SYSTEM_CONTROL_LOGS = "/cdp/system-control/logs";
	
	public SystemControlHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap) throws Exception {
		switch (uri) {
		case SAVE_SYSTEM_CONTROL_SETUP: {
			String superAdminPassword = StringUtil.safeString(paramJson.getString("superAdminPassword"));
			if (superAdminPassword.length() >= 8) {
				SystemControl.setupNewSystem(superAdminPassword);
				return JsonDataPayload.ok(uri, true);
			}
			return JsonDataPayload.fail("The password for Super Admin must be at least 8 characters", 500);
		}
		
		case LIST_SYSTEM_ACTION_LOGS : {
			SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
			if (isSuperAdminRole(loginUser)) {
				// the list-view component at datatables.net needs AJAX POST method to avoid long URL 
				SystemEventFilter filter = new SystemEventFilter(loginUser, uri, paramJson);
				JsonDataTablePayload payload = SystemEventManagement.filter(filter);
				return payload.setUserLoginPermission(loginUser, Profile.class);
			} 
			else {
				return JsonErrorPayload.NO_AUTHORIZATION;
			}
		}
		
		case UPGRADE_SYSTEM_CONTROL: {
			SystemUser loginUser;
			String secretKey = paramJson.getString(SystemMetaData.STR_UPDATE_LEO_SYSTEM_SECRET_KEY, "");
			System.out.println("secretKey " + secretKey);
			if(SystemEnviroment.checkUpdateSecretKey(secretKey)) {
				loginUser = SystemUserManagement.getByUserLogin(SystemUser.SUPER_ADMIN_LOGIN);
			}
			else if(StringUtil.isEmpty(userSession)) {
				return JsonDataPayload.fail("The secret key is wrong", 500);
			}
			else {
				loginUser = initSystemUser(userSession, uri, paramJson);
			}
			
			if (isSuperAdminRole(loginUser) ) {
				// TODO improve security here
				String rs = SystemControl.sendCommandToUpgradeSystem(loginUser, uri, paramJson);
				return JsonDataPayload.ok(uri, rs);
			}
			return JsonDataPayload.fail("You need to login and must have the role SuperAdmin", 500);
		}
		default:
			return JsonErrorPayload.NO_HANDLER_FOUND;
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (isSuperAdminRole(loginUser)) {
			if(uri.equalsIgnoreCase(SYSTEM_CONTROL_LOGS)) {
				String filename = HttpWebParamUtil.getString(params, "filename");
				int maxRows = HttpWebParamUtil.getInteger(params, "maxrows", 100);
				String rs = LogUtil.readLastRowsLog(filename, maxRows);
				return JsonDataPayload.ok(uri, rs);
			}
			return JsonErrorPayload.NO_HANDLER_FOUND;
		} else {
			return JsonErrorPayload.NO_AUTHORIZATION;
		}
	}

}
