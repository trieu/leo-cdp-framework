package leotech.cdp.handler.admin;

import java.util.List;
import java.util.Set;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.JourneyMapManagement;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.domain.SystemUserManagement;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import rfx.core.util.StringUtil;

/**
 * System User Login Handler for Web Admin
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public final class SystemUserLoginHandler extends SecuredHttpDataHandler {

	private static final String MY_LOGIN_INFO = "my-login-info";

	private static final String NEWUSER = "newuser";
	
	// for Admin CMS, only for ROLE_DATA_ADMIN and ROLE_SUPER_ADMIN
	static final String USER_LIST_FOR_MANAGEMENT = "/user/list-for-management";
	static final String USER_LIST_FOR_SELECTION = "/user/list-for-selection";
	static final String USER_CREATE = "/user/create";
	static final String USER_UPDATE = "/user/update";
	static final String USER_GET = "/user/get";
	static final String USER_GET_BY_USERNAME = "/user/get-by-username";
	static final String USER_ACTIVATE_LOGIN = "/user/activate-login";	
	static final String USER_DEACTIVATE_LOGIN = "/user/deactivate-login";
	static final String USER_DELETE = "/user/delete";
	static final String USER_ACTIVATE_LEOBOT = "/user/activate-leobot";

	public SystemUserLoginHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson) throws Exception {
		// get user info from login session
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);

		System.out.println(uri);
		if (loginUser != null) {
			if (isAuthorized(loginUser, SystemUser.class)) {
				switch (uri) {
					case USER_ACTIVATE_LEOBOT : {
						String visitorId = paramJson.getString("visitorId", "");
						boolean ok = SystemUserDaoUtil.updateVisitorProfileAndActivateLeoBot(loginUser, visitorId);
						return JsonDataPayload.ok(uri, ok, loginUser, SystemUser.class);
					}
					case USER_GET : {
						String key = paramJson.getString("key", "");
						SystemUser systemUser = null;
						if (StringUtil.isEmpty(key) || key.equalsIgnoreCase(MY_LOGIN_INFO)) {
							systemUser = loginUser;
						} 
						else if (key.equals(NEWUSER)) {
							systemUser = new SystemUser();
						}
						else if(isAuthorizedToGetUserDetailsByKey(loginUser, key)){
							systemUser = SystemUserDaoUtil.getSystemUserByKey(key);
						} 
						//
						if(systemUser != null) {
							systemUser.removeSensitiveData();
							return JsonDataPayload.ok(uri, systemUser, loginUser, SystemUser.class);
						}
						return JsonDataPayload.fail("No Authorization To Get User Login Data by Key: " + key, 504);
					}					
					case USER_GET_BY_USERNAME : {
						String userLogin = paramJson.getString("userLogin", "");
						SystemUser systemUser = null;
						boolean check = isAuthorizedToGetUserDetailsByUserLogin(loginUser, userLogin);
						if (StringUtil.isNotEmpty(userLogin) && check) {
							systemUser = SystemUserDaoUtil.getByUserLogin(userLogin);
						}
						//
						if(systemUser != null) {
							return JsonDataPayload.ok(uri, systemUser, loginUser, SystemUser.class);
						}
						return JsonDataPayload.fail("No Authorization To Get User Login Data by userLogin: " + userLogin, 500);
					}
					case USER_LIST_FOR_SELECTION : {
						if (isAdminRole(loginUser)) {
							List<SystemUser> list = SystemUserManagement.listAllUsers(false);
							return JsonDataPayload.ok(uri, list, loginUser, SystemUser.class, false);
						} else {
							return JsonErrorPayload.NO_AUTHORIZATION;
						}
					}
					case USER_LIST_FOR_MANAGEMENT : {
						if (isSuperAdminRole(loginUser)) {
							List<SystemUser> list = SystemUserManagement.listAllUsers(true);
							return JsonDataPayload.ok(uri, list, loginUser, SystemUser.class);
						} else {
							return JsonDataPayload.fail("No Authorization to list user", 500);
						}
					}
					case USER_CREATE : {
						if (isSuperAdminRole(loginUser)) {
							String userId = SystemUserManagement.save(loginUser, paramJson, true);
							System.out.println("USER_CREATE.saveUserInfo " + userId);
							return JsonDataPayload.ok(uri, userId, loginUser, SystemUser.class);
						} 
						else {
							return JsonErrorPayload.NO_AUTHORIZATION_TO_UPDATE;
						}
					}
					case USER_UPDATE : {
						String userLogin = paramJson.getString("userLogin", "");
						if (isAuthorizedToGetUserDetailsByUserLogin(loginUser, userLogin)) {
							// update general information and role
							String userId = SystemUserManagement.save(loginUser, paramJson, false);
							if(loginUser.hasSuperAdminRole()) {
								this.checkAndUpdateDataAuthorization(paramJson, userLogin);
							}
							return JsonDataPayload.ok(uri, userId, loginUser, SystemUser.class);
						} else {
							return JsonErrorPayload.NO_AUTHORIZATION_TO_UPDATE;
						}
					}
					case USER_ACTIVATE_LOGIN : {
						if (isSuperAdminRole(loginUser)) {
							String userLogin = paramJson.getString("userLogin", "");
							String activationKey = paramJson.getString("activationKey", "");
							boolean ok = SystemUserDaoUtil.activateSystemUser(userLogin, activationKey);
							return JsonDataPayload.ok(uri, ok, loginUser, SystemUser.class);
						} 
						else {
							return JsonErrorPayload.NO_AUTHORIZATION_TO_UPDATE;
						}
					}
					case USER_DEACTIVATE_LOGIN : {
						if (isSuperAdminRole(loginUser)) {
							String userLogin = paramJson.getString("userLogin", "");
							boolean ok = SystemUserDaoUtil.deactivateSystemUser(userLogin);
							return JsonDataPayload.ok(uri, ok, loginUser, SystemUser.class);
						} 
						else {
							 
							return JsonErrorPayload.NO_AUTHORIZATION_TO_UPDATE;
						}
					}
					case USER_DELETE : {
						if (isSuperAdminRole(loginUser)) {
							String userLogin = paramJson.getString("userLogin", "");
							String ukey = SystemUserDaoUtil.deleteSystemUserByUserLogin(userLogin);
							return JsonDataPayload.ok(uri, ukey, loginUser, SystemUser.class);
						} 
						else {
							return JsonErrorPayload.NO_AUTHORIZATION_TO_UPDATE;
						}
					}
					default : {
						return JsonErrorPayload.NO_HANDLER_FOUND;
					}
				}
			}
			return JsonErrorPayload.NO_AUTHORIZATION;
		} 
		else {
			return userLoginHandler(userSession, uri, paramJson);
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params) throws Exception {
		return JsonErrorPayload.NO_HANDLER_FOUND;
	}
	
	/**
	 * @param paramJson
	 * @param userLogin
	 */
	protected void checkAndUpdateDataAuthorization(JsonObject paramJson, String userLogin) {
		// update authorized journeys
		boolean removeAllAuthorizedJourneys = paramJson.getBoolean("removeAllAuthorizedJourneys", false);
		if(removeAllAuthorizedJourneys) {
			JourneyMapManagement.removeAllAuthorizedJourneys(userLogin);
			ProfileDataManagement.removeAllViewableProfiles(userLogin);
			ProfileDataManagement.removeAllEditableProfiles(userLogin);
			SegmentDataManagement.removeAllViewableSegments(userLogin);
			SegmentDataManagement.removeAllEditableSegments(userLogin);
		}
		else {
			JsonObject updatedJourneyMaps = paramJson.getJsonObject("updatedJourneyMaps");
			if(updatedJourneyMaps != null) {
				Set<String> updateJourneyMapIds = updatedJourneyMaps.fieldNames();
				for(String updateJourneyId : updateJourneyMapIds) {
					String[] toks = updateJourneyId.split("#");
					String journeyMapId = toks[0];
					String command = updatedJourneyMaps.getString(updateJourneyId,""); 
					String authorizedViewer = "", authorizedEditor = "";
					if(command.contains("viewer")) {
						authorizedViewer = userLogin;
					}
					if(command.contains("editor")) {
						authorizedEditor = userLogin;
					}
					
					boolean updateAllProfilesInJourney = paramJson.getBoolean("updateAllProfilesInJourney", true);
					JourneyMapManagement.updateJourneyDataAuthorization(journeyMapId, authorizedViewer, authorizedEditor, userLogin, updateAllProfilesInJourney);
				}
			}
			
			boolean removeAllViewableProfiles = paramJson.getBoolean("removeAllViewableProfiles", false);
			boolean removeAllEditableProfiles = paramJson.getBoolean("removeAllEditableProfiles", false);
			boolean removeAllViewableSegments = paramJson.getBoolean("removeAllViewableSegments", false);
			boolean removeAllEditableSegments = paramJson.getBoolean("removeAllEditableSegments", false);
			
			// update authorized profiles
			if(removeAllViewableProfiles) {
				ProfileDataManagement.removeAllViewableProfiles(userLogin);
			}
			if(removeAllEditableProfiles) {
				ProfileDataManagement.removeAllEditableProfiles(userLogin);
			}
			
			// update authorized segments
			if(removeAllViewableSegments) {
				SegmentDataManagement.removeAllViewableSegments(userLogin);
			}
			if(removeAllEditableSegments) {
				SegmentDataManagement.removeAllEditableSegments(userLogin);
			}
		}
	}

}
