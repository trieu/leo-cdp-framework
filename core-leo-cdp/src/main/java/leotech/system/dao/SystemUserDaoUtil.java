package leotech.system.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.google.gson.Gson;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.domain.cache.AdminRedisCacheUtil;
import leotech.system.config.AqlTemplate;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.Notification;
import leotech.system.model.SystemEvent;
import leotech.system.model.SystemUser;
import leotech.system.model.SystemUserRole;
import leotech.system.util.EncryptorAES;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;
import rfx.core.util.HashUtil;
import rfx.core.util.StringUtil;

/**
 * System User DAO
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public final class SystemUserDaoUtil extends AbstractCdpDatabaseUtil {

	static final String AQL_COUNT_TOTAL_ACTIVE_USERS = "RETURN LENGTH(FOR s in " + SystemUser.COLLECTION_NAME+" FILTER s.status == 1 RETURN s._key)";
	static final String AQL_FIND_KEY_BY_USERLOGIN = AqlTemplate.get("AQL_FIND_KEY_BY_USERLOGIN");
	static final String AQL_GET_USER_BY_USERLOGIN = AqlTemplate.get("AQL_GET_USER_BY_USERLOGIN");
	static final String AQL_GET_USER_BY_KEY = AqlTemplate.get("AQL_GET_USER_BY_KEY");
	static final String AQL_GET_USERLOGIN_BY_EMAIL = AqlTemplate.get("AQL_GET_USERLOGIN_BY_EMAIL");
	static final String AQL_GET_ALL_USERS_IN_NETWORK = AqlTemplate.get("AQL_GET_ALL_USERS_IN_NETWORK");
	
	public static int countTotalActiveUsers() {
		ArangoDatabase db = getCdpDatabase();
		return new ArangoDbCommand<>(db, AQL_COUNT_TOTAL_ACTIVE_USERS, Integer.class).getSingleResult();
	}

	/**
	 * @param user
	 * @return
	 */
	public static String createNewSystemUser(SystemUser user) {
		if (user.dataValidation()) {
			ArangoCollection col = user.getDbCollection();
			if (col != null) {
				String _key = ArangoDbUtil.findKey(AQL_FIND_KEY_BY_USERLOGIN, "userLogin", user.getUserLogin());
				if (_key == null) {
					long currentTime = System.currentTimeMillis();
					String activationKey = HashUtil.sha1(currentTime + user.getUserEmail());
					user.setActivationKey(activationKey);
					user.setCreationTime(currentTime);
					user.setModificationTime(currentTime);
					_key = col.insertDocument(user).getKey();
					return _key;
				} else {
					throw new IllegalArgumentException(user.getUserLogin() + " is existed in database");
				}
			}
		}
		return "";
	}
	
	

	/**
	 * @param key
	 * @return
	 */
	public static String deleteSystemUserByKey(String key) {
		ArangoCollection col = new SystemUser().getDbCollection();
		if (col != null) {
			col.deleteDocument(key);
			return key;
		}
		throw new InvalidDataException("Can not deleteSystemUserByKey: " + key);
	}

	/**
	 * @param userLogin
	 * @return
	 */
	public static String deleteSystemUserByUserLogin(String userLogin) {
		if(SystemUser.SUPER_ADMIN_LOGIN.equals(userLogin)) {
			throw new InvalidDataException("You can not delete userLogin:" + userLogin);
		}
		else {
			SystemUser u = getByUserLogin(userLogin);
			if (u != null) {			
				return deleteSystemUserByKey(u.getKey());
			}
			else {
				throw new InvalidDataException("Not found data ! Invalid userLogin: " + userLogin);
			}
		}	
	}

	/**
	 * @param userLogin
	 * @param displayName
	 * @return
	 */
	public static String updateSystemUserDisplayName(String userLogin, String displayName) {
		SystemUser u = getByUserLogin(userLogin);
		if (u != null) {
			u.setDisplayName(displayName);
			return updateSystemUser(u);
		}
		return "";
	}

	/**
	 * @param userLogin
	 * @param role
	 * @return
	 */
	public static String updateSystemUserRole(String userLogin, int role) {
		SystemUser u = getByUserLogin(userLogin);
		if (u != null) {
			u.setRole(role);
			return updateSystemUser(u);
		}
		return "";
	}
	

	/**
	 * @param userKey
	 * @param noti
	 * @param clearOldData
	 * @return
	 */
	public static String setNotification(String userKey, Notification noti, boolean clearOldData) {
		SystemUser user = getSystemUserByKey(userKey);
		if (user != null) {
			if(clearOldData) {
				user.clearAllNotifications();
			}
			user.setNotification(noti);
			
			// log as system event
			String data = noti.getMessage() + " " + noti.getUrl();
			SystemEvent log = new SystemEvent(user.getUserLogin(), Notification.class, noti.getId(), "setNotification", data);
			SystemEventDaoUtil.save(log);
			
			return updateSystemUser(user);
		}
		return "";
	}
	
	/**
	 * @param userKey
	 * @param noti
	 * @return
	 */
	public static String setNotification(String userKey, Notification noti) {
		return setNotification(userKey, noti, false);
	}

	/**
	 * @param userLogin
	 * @param isOnline
	 * @return
	 */
	public static String updateSystemUserOnlineStatus(String userLogin, boolean isOnline) {
		SystemUser u = getByUserLogin(userLogin);
		if (u != null) {
			u.setOnline(isOnline);
			return updateSystemUser(u);
		}
		return "";
	}

	public static String updateSystemUserPassword(String userLogin, String userPass) {
		SystemUser u = getByUserLogin(userLogin);
		if (u != null) {
			u.setUserPass(userPass);
			return updateSystemUser(u);
		}
		return "";
	}

	/**
	 * @param userLogin
	 * @param avatarUrl
	 * @return
	 */
	public static String updateSystemUserAvatar(String userLogin, String avatarUrl) {
		SystemUser u = getByUserLogin(userLogin);
		if (u != null) {
			u.setAvatarUrl(avatarUrl);
			return updateSystemUser(u);
		}
		return "";
	}

	/**
	 * @param userLogin
	 * @param customData
	 * @return
	 */
	public static String updateSystemUserCustomData(String userLogin, Map<String, String> customData) {
		SystemUser u = getByUserLogin(userLogin);
		if (u != null) {
			u.setCustomData(customData);
			return updateSystemUser(u);
		}
		return "";
	}

	/**
	 * @param userLogin
	 * @return
	 */
	public static SystemUser getByUserLogin(String userLogin) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("userLogin", userLogin);
		SystemUser user = new ArangoDbCommand<>(db, AQL_GET_USER_BY_USERLOGIN, bindVars, SystemUser.class).getSingleResult();
		
		// TODO load authorized data to access
		
		return user;
	}
	
	/**
	 * @param email
	 * @return
	 */
	public static String getUserLoginByEmail(String email) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("email", email);
		String userLogin = new ArangoDbCommand<>(db, AQL_GET_USERLOGIN_BY_EMAIL, bindVars, String.class).getSingleResult();
		return userLogin;
	}

	/**
	 * @param key
	 * @return
	 */
	public static SystemUser getSystemUserByKey(String key) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("key", key);
		SystemUser user = new ArangoDbCommand<>(db, AQL_GET_USER_BY_KEY, bindVars, SystemUser.class).getSingleResult();
		return user;
	}


	/**
	 * @param forManagement
	 * @param networkId
	 * @return List<SystemUser>
	 */
	public static List<SystemUser> listAllUsersInNetwork(boolean forManagement, long networkId) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("networkId", networkId);
		bindVars.put("forManagement", forManagement);
		List<SystemUser> users = new ArangoDbCommand<>(db, AQL_GET_ALL_USERS_IN_NETWORK, bindVars, SystemUser.class).getResultsAsList();
		return users;
	}

	/**
	 * @param userLogin
	 * @return
	 */
	public static boolean activateAsGuest(String userLogin) {
		return activate(userLogin, SystemUserRole.REPORT_VIEWER);
	}

	/**
	 * @param userLogin
	 * @return
	 */
	public static boolean activateAsStandarUser(String userLogin) {
		return activate(userLogin, SystemUserRole.STANDARD_USER);
	}

	/**
	 * @param userLogin
	 * @return
	 */
	public static boolean activateAsEditor(String userLogin) {
		return activate(userLogin, SystemUserRole.DATA_OPERATOR);
	}

	/**
	 * @param userLogin
	 * @return
	 */
	public static boolean activateAsAdmin(String userLogin) {
		return activate(userLogin, SystemUserRole.DATA_ADMIN);
	}

	/**
	 * @param userLogin
	 * @return
	 */
	public static boolean activateAsSuperAdmin(String userLogin) {
		return activate(userLogin, SystemUserRole.SUPER_SYSTEM_ADMIN);
	}

	/**
	 * @param userLogin
	 * @param role
	 * @return
	 */
	private static boolean activate(String userLogin, int role) {
		SystemUser user = getByUserLogin(userLogin);
		if (user != null) {
			user.setActivationKey("");
			user.setRegisteredTime(System.currentTimeMillis());
			user.setStatus(SystemUser.STATUS_ACTIVE);
			user.setRole(role);
			updateSystemUser(user);
			return true;
		}
		return false;
	}

	/**
	 * @param userLogin
	 * @return
	 */
	public static boolean deactivateSystemUser(String userLogin) {
		if(SystemUser.SUPER_ADMIN_LOGIN.equals(userLogin)) {
			throw new InvalidDataException("You can not deactivate SystemUser: " + userLogin);
		}
		else {
			SystemUser user = getByUserLogin(userLogin);
			if (user != null) {
				user.setStatus(SystemUser.STATUS_DISABLED);
				user.setRole(SystemUserRole.REPORT_VIEWER);
				updateSystemUser(user);
				return true;
			}	
		}		
		return false;
	}

	/**
	 * @param userLogin
	 * @param activationKey
	 * @return
	 */
	public static boolean activateSystemUser(String userLogin, String activationKey) {
		SystemUser user = getByUserLogin(userLogin);
		if (StringUtil.isNotEmpty(activationKey)) {
			if (activationKey.equals(user.getActivationKey())) {
				user.setActivationKey("");
				user.setRegisteredTime(System.currentTimeMillis());
				user.setStatus(SystemUser.STATUS_ACTIVE);
				updateSystemUser(user);
				return true;
			}
		}
		return false;
	}
	
	public static String setStatusSentAllNotifications(SystemUser user) {
		user.clearAllNotifications();
		return updateSystemUser(user);
	}
	
	/**
	 * @param user
	 * @return
	 */
	public static String clearAllNotifications(SystemUser user) {
		user.clearAllNotifications();
		return updateSystemUser(user);
	}

	/**
	 * @param user
	 * @return
	 */
	public static String updateSystemUser(SystemUser user) {
		if (user.dataValidation()) {
			ArangoCollection col = user.getDbCollection();
			if (col != null) {
				String key = user.getKey();
				user.setModificationTime(System.currentTimeMillis());				
				col.updateDocument(key, user, getUpdateOptions() );
				return key;
			}
		} else {
			System.out.println("check fail isReadyForSave " + new Gson().toJson(user));
		}
		return "";
	}

	/**
	 * @param userLogin
	 * @param password
	 * @return
	 */
	public static boolean checkSystemUserLogin(String userLogin, String password) {
		boolean check = false;
		SystemUser user = getByUserLogin(userLogin);
		if (user != null && StringUtil.isNotEmpty(password)) {
			String hash = EncryptorAES.passwordHash(userLogin, password);
			check = user.getStatus() == SystemUser.STATUS_ACTIVE && hash.equals(user.getUserPass());			
		}
		return check;
	}

	/**
	 * @param loginUser
	 * @param visitorId
	 * @return
	 */
	public static boolean updateVisitorProfileAndActivateLeoBot(SystemUser loginUser, String visitorId) {
		
		if(StringUtil.isNotEmpty(visitorId)) {
			loginUser.setProfileVisitorId(visitorId);		
			boolean ok = updateSystemUser(loginUser) != null;
			if(ok) {
				AdminRedisCacheUtil.setLeoChatBotForProfile(visitorId, loginUser.getDisplayName());	
			}			
			return ok;	
		}
		return false;
	}

}
