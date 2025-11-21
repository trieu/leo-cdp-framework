package leotech.system.util.keycloak;

import java.util.UUID;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import rfx.core.nosql.jedis.RedisCommand;
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

	private final Vertx vertx;
	private final JedisPool jedisPool;

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
		JsonObject data = new JsonObject().put("user", userInfo).put("token", tokenData).put("timestamp",
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
}