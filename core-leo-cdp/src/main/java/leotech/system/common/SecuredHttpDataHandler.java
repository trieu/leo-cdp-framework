package leotech.system.common;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.devskiller.friendly_id.FriendlyId;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.starter.router.NotifyUserHandler;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.domain.SystemEventManagement;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.system.util.CaptchaUtil;
import leotech.system.util.CaptchaUtil.CaptchaData;
import leotech.system.util.EncryptorAES;
import leotech.system.util.IdGenerator;
import leotech.system.util.LogUtil;
import leotech.system.version.SystemMetaData;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;

/**
 * Secured Web Data Handler for Leo Admin
 * 
 * @author tantrieuf31
 * @since 2020
 */
public abstract class SecuredHttpDataHandler extends BaseHttpHandler {
	
	
	public static final int AFTER_15_MINUTES = 60 * 15;
	public static final int AFTER_3_DAYS = 60 * 60 * 24 * 3;
	public static final int AFTER_7_DAYS = 60 * 60 * 24 * 7;
	public static final int SESSION_LIVE_TIME = AFTER_7_DAYS * 3; // 3 weeks = 21 days

	private static final String CAPTCHA_IMAGE = "captchaImage";
	private static final String USER_SESSION = "userSession";
	public static final boolean DEV_MODE = SystemMetaData.isDevMode();

	// USER SESSION KEYS
	public static final String REDIS_KEY_ENCKEY = "enckey";
	public static final String REDIS_KEY_USERLOGIN = "userlogin";
	public static final String LAST_ACCESS_TIME = "lastaccesstime";
	
	public static final String API_LOGIN_SESSION = "/user/login-session";
	public static final String API_CHECK_LOGIN = "/user/check-login";

	public static final String SESSION_SPLITER = "_";

	public static final String ADMIN_HANDLER_SESSION_KEY = "leouss";
	public static final String DATA_ACCESS_KEY = "dataAccessKey";
	
	final static int MAX_NUMBER = 1000000;
	
	// Define classes requiring operational role
	private static final Set<Class<?>> OPERATION_ROLE_CLASS = Set.of(
        leotech.cdp.model.customer.Profile.class,
        leotech.cdp.model.customer.Segment.class,
        leotech.cdp.model.journey.JourneyMap.class,
        leotech.cdp.model.asset.AssetCategory.class,
        leotech.cdp.model.asset.AssetGroup.class,
        leotech.cdp.model.asset.AssetItem.class,
        leotech.cdp.model.activation.Agent.class
    );
	
	private BaseHttpRouter baseHttpRouter;
	
	public SecuredHttpDataHandler(BaseHttpRouter baseHttpRouter) {
		this.baseHttpRouter = baseHttpRouter;
	}
	
	public BaseHttpRouter getBaseHttpRouter() {
		return baseHttpRouter;
	}
	
	protected static int randomNumber() {
		Random rand = new Random();
		int n = rand.nextInt(MAX_NUMBER) + 1;
		return n;
	}
	
	/**
	 * @param usersession
	 * @return
	 */
	public static boolean isValidUserSession(String usersession) {
		if (StringUtil.isNotEmpty(usersession)) {
			String decrypt = EncryptorAES.decrypt(usersession);
			String[] toks = decrypt.split(SESSION_SPLITER);
			if (toks.length >= 3) {
				int ran = StringUtil.safeParseInt(toks[2]);
				if (toks[0].equals(ADMIN_HANDLER_SESSION_KEY) && ran > 0 && ran <= MAX_NUMBER) {
					return true;
				}
			} 
			else {
				System.err.println("[isValidUserSession] error in parsing usersession, needs 3 tokens");
			}
		}
		return false;
	}
	
	
	/**
	 * @param usersession
	 * @return
	 */
	public static boolean isValidUserSessionWithCaptcha(String usersession,  String captchaFromUser) {
		if (StringUtil.isNotEmpty(usersession)) {
			String decrypt = EncryptorAES.decrypt(usersession);
			String[] toks = decrypt.split(SESSION_SPLITER);
			if(toks.length == 4) {
				String savedCaptcha = toks[3];
				int ran = StringUtil.safeParseInt(toks[2]);
				logger.info("savedCaptcha " + savedCaptcha);
				logger.info("captchaFromUser " + captchaFromUser);
				
				if(captchaFromUser.equals(savedCaptcha) && toks[0].equals(ADMIN_HANDLER_SESSION_KEY) && ran > 0 && ran <= MAX_NUMBER) {
					return true;
				} else {
					System.err.println(" ==> check login failed with 4 tokens");
					return false;
				}
			}
			else {
				System.err.println("usersession is not 4 tokens");
				return false;
			}
		}
		return false;
	}
	
	
	/**
	 * for data exporting and notebook API access
	 * 
	 * @param dataAccessKey
	 * @param systemUserId
	 */
	public static void setDataAccessKeyForSystemUser(String dataAccessKey, String systemUserId) {
		try {
			new RedisCommand<Void>(redisLocalCache) {
				@Override
				protected Void build(JedisPooled jedis) throws JedisException {
					Pipeline p = jedis.pipelined();
					p.set(dataAccessKey,systemUserId);
					p.expire(dataAccessKey, AFTER_15_MINUTES);
					p.sync();
					return null;
				}
			}.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param usersession
	 * @return
	 */
	public static boolean isAuthorization(String usersession) {
		if (isValidUserSession(usersession)) {
			try {
				return new RedisCommand<Boolean>(redisLocalCache) {
					@Override
					protected Boolean build(JedisPooled jedis) throws JedisException {
						return jedis.exists(usersession);
					}
				}.execute();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return false;
	}
	
	/**
	 * for data exporting and notebook API access
	 * 
	 * @param dataAccessKey
	 * @return
	 */
	public static SystemUser getUserByDataAccessKey(String dataAccessKey) {
		SystemUser systemUser = null;
		try {
			systemUser = new RedisCommand<SystemUser>(redisLocalCache) {
				@Override
				protected SystemUser build(JedisPooled jedis) throws JedisException {
					
					String userId = jedis.get(dataAccessKey);
					if(userId != null) {
						SystemUser user = SystemUserDaoUtil.getSystemUserByKey(userId);
						if (user != null) {
							if (user.getStatus() == SystemUser.STATUS_ACTIVE) {
								return user;
							} else {
								System.err.println("userlogin: " + userId + " not active");
							}
						}
					}
					return null;
				}
			}.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return systemUser;
	}

	
	/**
	 * get SystemUser from Session to check authentication and authorization
	 * 
	 * @param userSession
	 * @return
	 */
	public static SystemUser getSystemUserFromSession(String userSession) {
		if (isValidUserSession(userSession)) {
			try {
				RedisCommand<SystemUser> cmd = new RedisCommand<SystemUser>(redisLocalCache) {
					@Override
					protected SystemUser build(JedisPooled jedis) throws JedisException {
						Pipeline p = jedis.pipelined();
						Response<String> resp1 = p.hget(userSession, REDIS_KEY_USERLOGIN);
						Response<String> resp2 = p.hget(userSession, REDIS_KEY_ENCKEY);
						p.hset(userSession, LAST_ACCESS_TIME, new Date().toString());
						p.sync();

						String userlogin = resp1.get();
						String enckey = resp2.get();
						SystemUser user = SystemUserDaoUtil.getByUserLogin(userlogin);
						if (user != null) {
							if (user.getStatus() == SystemUser.STATUS_ACTIVE) {
								user.setEncryptionKey(enckey);
								if(user.shouldCheckNotifications()) {
									NotifyUserHandler.checkNotification(user.getKey());
								}
								return user;
							} 
							else {
								LogUtil.logError(SecuredHttpDataHandler.class,"userlogin: " + userlogin + " not active");
							}
						}
						return null;
					}
				};
				return cmd.execute();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("User Session is expired !");
		}
		// no authentication
		return null;
	}
	
	/**
	 * for HTTP POST, get user info from login session and track SystemEvent 
	 * 
	 * @param userSession
	 * @param clazz
	 * @param uri
	 * @param paramJson
	 * @return
	 */
	public static SystemUser initSystemUser(String userSession, String uri, JsonObject paramJson) {
		SystemUser systemUser = getSystemUserFromSession(userSession);
		SystemEventManagement.log(systemUser, SystemUser.class, uri, paramJson);
		return systemUser;
	}
	
	/**
	 * for HTTP GET, get user info from login session and track SystemEvent 
	 * 
	 * @param userSession
	 * @param uri
	 * @param params
	 * @return
	 */
	public static SystemUser initSystemUser(String userSession, String uri, MultiMap params) {
		SystemUser systemUser = getSystemUserFromSession(userSession);
		SystemEventManagement.log(systemUser, SystemUser.class, uri, params);
		return systemUser;
	}
	
	/**
	 * get user info from login session and no tracking SystemEvent
	 * 
	 * @param userSession
	 * @return
	 */
	public static SystemUser initSystemUser(String userSession) {
		return getSystemUserFromSession(userSession);
	}

	
	/**
	 * Checks if a user is authorized to access a given class type.
	 *
	 * @param loginUser the logged-in user
	 * @param clazz     the target class to authorize
	 * @return true if authorized, false otherwise
	 */
	public static boolean isAuthorized(SystemUser loginUser, Class<?> clazz) {
	    if (loginUser == null || clazz == null) {
	        return false;
	    }

	    // Must be active to proceed
	    if (loginUser.getStatus() != SystemUser.STATUS_ACTIVE) {
	        return false;
	    }

	    // If the class belongs to the operation set, check that role
	    if (OPERATION_ROLE_CLASS.contains(clazz)) {
	        return loginUser.hasOperationRole();
	    }

	    // Default authorization rule
	    return loginUser.getRole() >= 0;
	}

	
	/**
	 * @param loginUser
	 * @param clazz
	 * @return
	 */
	public static boolean isAuthorizedForSystemAdmin(SystemUser loginUser, Class<?> clazz) {
		return loginUser.hasAdminRole();
	}
	
	/**
	 * @param loginUser
	 * @return
	 */
	public static boolean isDataOperator(SystemUser loginUser) {
		return loginUser.hasOperationRole();
	}
	
	
	/**
	 * @param loginUser
	 * @return
	 */
	public static boolean isDataOperator(SystemUser loginUser, Class<?> clazz) {
		return loginUser.hasOperationRole();
	}

	
	/**
	 * @param loginUser
	 * @return
	 */
	public static boolean isAdminRole(SystemUser loginUser) {
		if(loginUser != null) {
			return loginUser.hasAdminRole();
		}
		return false;
	}
	
	/**
	 * @param loginUser
	 * @return
	 */
	public static boolean isSuperAdminRole(SystemUser loginUser) {
		if(loginUser != null) {
			return loginUser.hasSuperAdminRole();	
		}
		return false;
	}
	
	public static boolean isAuthorizedToGetUserDetailsByKey(SystemUser loginUser, String userKey) {
		return isSuperAdminRole(loginUser) || loginUser.getKey().equals(userKey);
	}
	
	public static boolean isAuthorizedToGetUserDetailsByUserLogin(SystemUser loginUser, String userLogin) {
		String sessionUserLogin = loginUser.getUserLogin();
		return StringUtil.isNotEmpty(userLogin) && (isSuperAdminRole(loginUser) || sessionUserLogin.equals(userLogin));
	}

	
	///////////////////////////// 3 API for login
	///////////////////////////// ///////////////////////////////////////////////////////////////////


	/**
	 * step 1: 
	 * 
	 * @param userSession
	 * @param uri
	 * @param paramJson
	 * @return
	 */
	protected static JsonDataPayload userLoginHandler(String userSession, String uri, JsonObject paramJson) {
		if (uri.equalsIgnoreCase(API_LOGIN_SESSION)) {
			if (StringUtil.isEmpty(userSession)) {
				return loginSessionInit(uri);
			} else {
				return JsonDataPayload.ok(uri, userSession);
			}
		} 
		else if (uri.equalsIgnoreCase(API_CHECK_LOGIN)) {
			String userlogin = paramJson.getString("userlogin","").trim();
			String userpass = paramJson.getString("userpass","").trim();
			String captcha = paramJson.getString("captcha","").trim();
			
			if(StringUtil.isEmpty(captcha)) {
				return JsonErrorPayload.INVALID_CAPCHA_NUMBER;
			}
			
			boolean valid = StringUtil.isNotEmpty(userlogin) && StringUtil.isNotEmpty(userpass);
			if(valid) {
				// verify from database
				JsonDataPayload checkLoginResult = checkLoginHandler(uri, userSession, userlogin, userpass, captcha);
				
				// log to monitor
				paramJson.remove("userpass");
				
				// tracking with System Analytics
				SystemEventManagement.log(userlogin, SystemUser.class, uri, paramJson);
				
				return checkLoginResult;
			} else {
				return JsonErrorPayload.WRONG_USER_LOGIN;
			}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}


	/**
	 * step 2: 
	 * 
	 * @param uri
	 * @return
	 */
	private static JsonDataPayload loginSessionInit(String uri) {
		CaptchaData cd = CaptchaUtil.getRandomCaptcha();
		String encryptedValue = ADMIN_HANDLER_SESSION_KEY + SESSION_SPLITER + System.currentTimeMillis() + SESSION_SPLITER + randomNumber() + SESSION_SPLITER + cd.content;
		String userSession = EncryptorAES.encrypt(encryptedValue);
		Map<String, String> data = new HashMap<>(2);
		data.put(USER_SESSION, userSession);
		data.put(CAPTCHA_IMAGE, cd.base64Image);
		return JsonDataPayload.ok(uri, data);
	}


	/**
	 * step 3: 
	 * 
	 * @param uri
	 * @param userSession
	 * @param userlogin
	 * @param userpass
	 * @param usersession
	 * @param inputedCaptcha
	 * @return
	 */
	private static JsonDataPayload checkLoginHandler(String uri, String userSession, String userLogin, String userpass, String inputedCaptcha) {
		if (isValidUserSessionWithCaptcha(userSession, inputedCaptcha)) {
			boolean ok = SystemUserDaoUtil.checkSystemUserLogin(userLogin, userpass);
			if (ok) {
				String encryptionKey = FriendlyId.createFriendlyId();
				// valid token in 7 days
				saveUserSession(userLogin, userSession, encryptionKey);
				return JsonDataPayload.ok(uri, encryptionKey);
			} else {
				return JsonErrorPayload.WRONG_USER_LOGIN;
			}
		}
		return JsonErrorPayload.INVALID_CAPCHA_NUMBER;
	}
	
	/**
	 * @param segmentId
	 * @param systemUserId
	 * @param fullPath
	 * @return accessUri
	 */
	public static String createAccessUriForExportedFile(String segmentId, String systemUserId, String fullPath) {
	    if (segmentId == null || systemUserId == null || fullPath == null) {
	        throw new IllegalArgumentException("segmentId, systemUserId, and fullPath must not be null");
	    }

	    // Generate key
	    String dataAccessKey = IdGenerator.generateDataAccessKey(segmentId, systemUserId);

	    try {
	        // Ensure key is URL-safe
	        String encodedKey = URLEncoder.encode(dataAccessKey, StandardCharsets.UTF_8.toString());

	        // Build URI safely, preserving existing query params
	        URI baseUri = URI.create(fullPath);
	        String separator = (baseUri.getQuery() == null) ? "?" : "&";
	        return fullPath + separator + DATA_ACCESS_KEY + "=" + encodedKey;
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to build access URI for exported file", e);
	    }
	}


	/**
	 * step 4: 
	 * 
	 * @param userLogin
	 * @param usersession
	 * @param encryptionKey
	 * @return
	 */
	private static boolean saveUserSession(String userLogin, String usersession, String encryptionKey) {
		try {
			new RedisCommand<Void>(redisLocalCache) {
				@Override
				protected Void build(JedisPooled jedis) throws JedisException {
					Pipeline p = jedis.pipelined();
					p.hset(usersession, REDIS_KEY_ENCKEY, encryptionKey);
					p.hset(usersession, REDIS_KEY_USERLOGIN, userLogin);
					p.expire(usersession, SESSION_LIVE_TIME);
					p.sync();
					return null;
				}
			}.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * HTTP post data handler for JSON
	 * 
	 * @param uri
	 * @param paramJson
	 * @param userSession
	 * @return
	 * @throws Exception
	 */
	abstract public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson) throws Exception;

	/**
	 * HTTP get data handler for JSON
	 * 
	 * @param uri
	 * @param paramJson
	 * @param userSession
	 * @return
	 * @throws Exception
	 */
	abstract public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params) throws Exception;

}
