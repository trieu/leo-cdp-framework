package leotech.system.util.keycloak;

import java.util.UUID;

import com.devskiller.friendly_id.FriendlyId;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.domain.SystemEventManagement;
import leotech.system.domain.SystemUserManagement;
import leotech.system.model.SystemUser;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.stream.cluster.ClusterDataManager;
import rfx.core.util.StringUtil;

/**
 * Encapsulates all Redis/Jedis complexity. Uses Vert.x Futures for async
 * results.
 * 
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public class SessionRepository {
	
	public static final int SSO_SESSION_LIFETIME = 60 * 60; // 60 min

	private final Vertx vertx;
	private final JedisPool jedisPool;
	protected static JedisPool DEFAULT_JEDIS_POOL = ClusterDataManager.getJedisClient();

	public SessionRepository(Vertx vertx) {
		this.vertx = vertx;
		this.jedisPool = DEFAULT_JEDIS_POOL;
	}
	
	public SessionRepository(Vertx vertx, JedisPool jedisPool) {
		this.vertx = vertx;
		this.jedisPool = jedisPool;
	}

	// Matches your executeBlocking pattern for retrieving a session
	public void getSession(String sid, Handler<AsyncResult<String>> resultHandler) {
		vertx.executeBlocking(future -> {
			RedisCommand<String> cmd = new RedisCommand<String>(jedisPool) {
				@Override
				protected String build(Jedis jedis) {
					return jedis.get(sid);
				}
			};
			// In Vert.x 3.8, 'future' acts as the promise
			future.complete(cmd.execute());
		}, resultHandler);
	}

	public void createSession(JsonObject userInfo, JsonObject tokenData, Handler<AsyncResult<String>> resultHandler) {
		String sessionId = "sid:" + UUID.randomUUID();
		JsonObject data = new JsonObject().put("user", userInfo).put(AuthKeycloakHandlers.TOKEN, tokenData).put("timestamp",
				System.currentTimeMillis() / 1000);

		vertx.executeBlocking(future -> {
			RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
				@Override
				protected Boolean build(Jedis jedis) {
					String ok = jedis.setex(sessionId, 3600L, data.toString());
					return StringUtil.isNotEmpty(ok);
				}
			};
			boolean success = cmd.execute();
			if (success) {
				future.complete(sessionId);
			} else {
				future.fail("Failed to set session in Redis");
			}
		}, resultHandler);
	}

	public void deleteSession(String sid, Handler<AsyncResult<Long>> resultHandler) {
		vertx.executeBlocking(future -> {
			RedisCommand<Long> cmd = new RedisCommand<Long>(jedisPool) {
				@Override
				protected Long build(Jedis jedis) {
					return jedis.del(sid);
				}
			};
			future.complete(cmd.execute());
		}, resultHandler);
	}

	public void updateSession(String sid, JsonObject data, Handler<AsyncResult<Boolean>> resultHandler) {
		vertx.executeBlocking(future -> {
			RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
				@Override
				protected Boolean build(Jedis jedis) {
					jedis.setex(sid, 3600, data.toString());
					return true;
				}
			};
			future.complete(cmd.execute());
		}, resultHandler);
	}
	
	public static SsoUserProfile getSsoUserProfileFromRedis(String sid) {
		if(StringUtil.isEmpty(sid)) {
			return null;
		}
		
		RedisCommand<String> cmd = new RedisCommand<String>(DEFAULT_JEDIS_POOL) {
			@Override
			protected String build(Jedis jedis) {
				return jedis.get(sid);
			}
		};			
		String rawSession = cmd.execute();
		SsoUserProfile ssoUser = null;
		if (StringUtil.isNotEmpty(rawSession)) {
			try {
				// Transform and response
				JsonObject session = new JsonObject(rawSession);
				String accessToken = session.getJsonObject(AuthKeycloakHandlers.TOKEN).getString(AuthKeycloakHandlers.ACCESS_TOKEN);
				JsonArray roles = KeycloakUtils.getUserRoles(accessToken);
				JsonObject userJson = session.getJsonObject(AuthKeycloakHandlers.USER);
				ssoUser = SsoUserProfile.fromJson(userJson, roles);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ssoUser;
	}
	
	/**
	 * @param uri
	 * @param userEmail
	 * @param userSession
	 * @param ssoSessionId
	 * @return
	 */
	public static String getSessionKeyFromSSO(String uri, String userEmail, String userSession, String ssoSessionId) {
		SsoUserProfile ssoUser = SessionRepository.getSsoUserProfileFromRedis(ssoSessionId);
		if (ssoUser != null) {
			boolean isSameEmail = ssoUser.getEmail().equals(userEmail);
			if(isSameEmail) {
				KeycloakConfig config = KeycloakConfig.getInstance();
				String ssoSource = config.ssoSource();
				
				SystemUser systemUser = SystemUserDaoUtil.getSystemUserByEmail(userEmail);
				
				JsonObject logObj = new JsonObject();
				if (systemUser == null) {
					System.out.println("---------- Create ");
					// no SystemUser for email from KeyCloak SSO
					systemUser = new SystemUser(ssoUser,config);
					SystemUserManagement.createNewSystemUser(systemUser);
					logObj.put("Create new SystemUser from", ssoSource);
				} 
				else {
					System.out.println("---------- Update ");
					systemUser.setSsoSource(ssoSource);
					SystemUserManagement.updateSystemUser(systemUser);
					logObj.put("Login as SystemUser from", ssoSource);
				}
				SystemEventManagement.log(systemUser, SystemUser.class, uri, logObj);
				
				
				String encryptionKey = FriendlyId.createFriendlyId();
				saveUserSessionSSO(systemUser.getUserLogin(), userSession, encryptionKey, SSO_SESSION_LIFETIME);
				return encryptionKey;
			}			
		}
		throw new IllegalArgumentException("ssoUser is null, invalid ssoSessionId " + ssoSessionId);
	}
	
	/**
	 * for SSO Session from Keycloak
	 * 
	 * @param userLogin
	 * @param usersession
	 * @param encryptionKey
	 * @param expiredTime
	 */
	private static void saveUserSessionSSO(String userLogin, String usersession, String encryptionKey, long expiredTime) {
		try {
			new RedisCommand<Void>(DEFAULT_JEDIS_POOL) {
				@Override
				protected Void build(Jedis jedis) throws JedisException {
					Pipeline p = jedis.pipelined();
					p.hset(usersession, SecuredHttpDataHandler.REDIS_KEY_ENCKEY, encryptionKey);
					p.hset(usersession, SecuredHttpDataHandler.REDIS_KEY_USERLOGIN, userLogin);
					p.expire(usersession, expiredTime);
					p.sync();
					return null;
				}
			}.executeAsync();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}