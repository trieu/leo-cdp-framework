package leotech.web.handler.delivery;

import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.domain.SystemUserManagement;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.system.util.LogUtil;

public class UserApiHandler extends SecuredHttpDataHandler {
	static final String API_CREATE = "/user/create";
	static final String API_UPDATE = "/user/update";
	static final String API_GET_INFO = "/user/get";
	static final String API_ACTIVATE = "/user/activate";
	static final String API_RESET_PASSWORD = "/user/reset-password";

	public UserApiHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap)
			throws Exception {
		// input params
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser == null) {
			return userLoginHandler(userSession, uri, paramJson, cookieMap);
		} else {
			switch (uri) {
				case API_GET_INFO : {
					String key = paramJson.getString("key", "");
					SystemUser userInfo = loginUser;
					if (!key.isEmpty()) {
						if (key.equals("newuser")) {
							userInfo = new SystemUser();
						} else {
							userInfo = SystemUserDaoUtil.getSystemUserByKey(key);
						}
					}
					return JsonDataPayload.ok(uri, userInfo, false);
				}
				case API_CREATE : {
					String userId = SystemUserManagement.save(loginUser, paramJson, true);
					LogUtil.logInfo(this.getClass(), "API_CREATE.saveUserInfo " + userId);
					return JsonDataPayload.ok(uri, userId, true);
				}
				case API_UPDATE : {
					String userId = SystemUserManagement.save(loginUser, paramJson, false);
					LogUtil.logInfo(this.getClass(),"API_UPDATE.saveUserInfo " + userId);
					return JsonDataPayload.ok(uri, userId, true);
				}
				case API_ACTIVATE : {
					String userLogin = paramJson.getString("userLogin", "");
					String activationKey = paramJson.getString("activationKey", "");
					boolean ok = SystemUserDaoUtil.activateSystemUser(userLogin, activationKey);
					return JsonDataPayload.ok(uri, ok, true);
				}
				case API_RESET_PASSWORD : {
					String userLogin = loginUser.getUserLogin();
					String userPass = paramJson.getString("userPass", "");
					boolean ok = false;
					if (userPass.length() > 5) {
						ok = !SystemUserDaoUtil.updateSystemUserPassword(userLogin, userPass).isEmpty();
					} else {
						throw new IllegalArgumentException("New password must have more than 6 characters");
					}
					return JsonDataPayload.ok(uri, ok, true);
				}
				default : {
					return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			}
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
		// TODO Auto-generated method stub
		SystemUser user = initSystemUser(userSession, uri, params);
		if (user != null) {
			return JsonDataPayload.ok(uri, user, false);
		} else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}

	}
}
