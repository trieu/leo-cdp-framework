package leotech.system.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.vertx.core.json.JsonObject;
import leotech.cdp.utils.ProfileDataValidator;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.AppMetadata;
import leotech.system.model.Notification;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;
import rfx.core.util.StringUtil;

/**
 * System User Management for Login Account
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public final class SystemUserManagement {

	/**
	 * @param systemUserId
	 * @param notification
	 */
	public static void sendNotification(String systemUserId, Notification notification) {
		SystemUserDaoUtil.setNotification(systemUserId, notification);
	}

	/**
	 * @param email
	 * @return
	 */
	public static String getUserLoginByEmail(String email) {
		return SystemUserDaoUtil.getUserLoginByEmail(email);
	}

	/**
	 * @param forManagement
	 * @return
	 */
	public static List<SystemUser> listAllUsers(boolean forManagement) {
		return SystemUserDaoUtil.listAllUsers(forManagement);
	}

	/**
	 * @param userLogin
	 * @param newPass
	 * @return
	 */
	public final static String resetLoginPassword(String userLogin, String newPass) {
		SystemUser u = SystemUserDaoUtil.getByUserLogin(userLogin);
		if (u != null) {
			if (StringUtil.isNotEmpty(newPass)) {
				u.setUserPass(newPass);
			}
			String userId = SystemUserDaoUtil.updateSystemUser(u);
			return userId;
		}
		return null;
	}

	public final static String createNewSystemUser(SystemUser user) {
		SystemUserDaoUtil.createNewSystemUser(user);
		return user.getUserLogin();
	}
	
	public final static String updateSystemUser(SystemUser user) {
		SystemUserDaoUtil.updateSystemUser(user);
		return user.getUserLogin();
	}
	
	

	/**
	 * @param loginUser
	 * @param paramJson
	 * @param createNew
	 * @return
	 */
	public final static String save(SystemUser loginUser, JsonObject paramJson, boolean createNew) {
		String userLogin = paramJson.getString("userLogin", "").trim();
		String userEmail = paramJson.getString("userEmail", "").trim();
		String displayName = paramJson.getString("displayName", "").trim();
		String profileVisitorId = paramJson.getString("profileVisitorId", "").trim();
		String userPass = paramJson.getString("userPass", "").trim();
		String businessUnit = paramJson.getString("businessUnit", "").trim();
		Set<String> inGroups = HttpWebParamUtil.getHashSet(paramJson, "inGroups");

		// customData
		JsonObject jsonCustomData = paramJson.getJsonObject("customData", new JsonObject());
		Map<String, String> customData = new HashMap<>(jsonCustomData.size());
		jsonCustomData.forEach(e -> {
			String key = e.getKey();
			String val = e.getValue().toString();
			if (!key.isEmpty()) {
				customData.put(key, val);
			}
		});

		boolean isValidProfile = ProfileDataValidator.isValidEmail(userEmail) && StringUtil.isNotEmpty(userLogin)
				&& StringUtil.isNotEmpty(displayName);
		if (isValidProfile) {
			String userId = "";
			if (createNew) {
				int status = paramJson.getInteger("status");
				int role = paramJson.getInteger("role");

				SystemUser user = new SystemUser(userLogin, userPass, displayName, userEmail, AppMetadata.DEFAULT_ID);

				user.setProfileVisitorId(profileVisitorId);
				user.setRole(role);
				user.setBusinessUnit(businessUnit);
				user.setInGroups(inGroups);
				user.setStatus(status);
				user.setCustomData(customData);
				userId = SystemUserDaoUtil.createNewSystemUser(user);
			} else {
				SystemUser dbSystemUser = SystemUserDaoUtil.getByUserLogin(userLogin);

				// a normal user can only update non-sensitive data
				dbSystemUser.setUserEmail(userEmail);
				dbSystemUser.setDisplayName(displayName);
				dbSystemUser.setProfileVisitorId(profileVisitorId);
				dbSystemUser.setBusinessUnit(businessUnit);
				dbSystemUser.setInGroups(inGroups);
				dbSystemUser.setCustomData(customData);

				// to make sure only superadmin can update this information
				if (loginUser.hasSuperAdminRole()) {
					int role = paramJson.getInteger("role", dbSystemUser.getRole());
					int status = paramJson.getInteger("status", dbSystemUser.getStatus());
					dbSystemUser.setStatus(status);
					dbSystemUser.setRole(role);
				}

				// just update is password is not empty
				if (!userPass.isBlank()) {
					if (userPass.length() >= 6) {
						dbSystemUser.setUserPass(userPass);
					} else {
						throw new InvalidDataException("The password must contain at least 6 characters!");
					}
				}

				userId = SystemUserDaoUtil.updateSystemUser(dbSystemUser);
			}

			return userId;
		} else {
			throw new InvalidDataException(
					"userEmail, userLogin, userPass and displayName must a valid string and not empty!");
		}
	}

	/**
	 * @param id
	 * @return SystemUser
	 */
	public static SystemUser getByUserId(String id) {
		return SystemUserDaoUtil.getSystemUserByKey(id);
	}

	/**
	 * @param userLogin
	 * @return
	 */
	public static SystemUser getByUserLogin(String userLogin) {
		return SystemUserDaoUtil.getByUserLogin(userLogin);
	}

}
