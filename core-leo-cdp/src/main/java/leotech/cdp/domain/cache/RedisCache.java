package leotech.cdp.domain.cache;

import com.google.gson.Gson;

import leotech.system.version.SystemMetaData;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class RedisCache {

	public static final int DEFAULT_EXPIRATION = 60;
	public static final String MASTER_CACHE = "masterCache";
	static ShardedJedisPool jedisPool = RedisConfigs.load().get(MASTER_CACHE).getShardedJedisPool();

	static String buildKey(String key) {
		return SystemMetaData.DOMAIN_CDP_ADMIN + "_" + key;
	}

	// Set cache with expiration (in seconds)
	public static void setCacheWithExpiry(String key, Object value, int expiryTimeInSeconds, boolean valueIsJsonStr) {
		RedisCommand<Boolean> cmd = new RedisCommand<>(jedisPool) {
			@Override
			protected Boolean build() throws JedisException {
				String v = null;
				if (valueIsJsonStr) {
					v = new Gson().toJson(value);
				} else {
					v = String.valueOf(value);
				}

				String k = buildKey(key);
				jedis.set(k, v);
				jedis.expire(k, expiryTimeInSeconds);
				return true;
			}
		};
		cmd.execute();
	}

	// Retrieve data from cache
	public static String getCache(String key) {
		RedisCommand<String> cmd = new RedisCommand<>(jedisPool) {
			@Override
			protected String build() throws JedisException {
				return jedis.get(buildKey(key));
			}
		};
		return cmd.execute();
	}

	/**
	 * @param key
	 * @param minimumSecondsToLive
	 * @return true if TTL of key is less than
	 */
	public static boolean shouldUpdateCache(String key, long minimumSecondsToLive) {
		RedisCommand<Long> cmd = new RedisCommand<>(jedisPool) {
			@Override
			protected Long build() throws JedisException {
				return jedis.ttl(buildKey(key));
			}
		};
		long ttl = cmd.execute();
		return ttl < minimumSecondsToLive;
	}

	/**
	 * Check if a key exists in the cache
	 * 
	 * @param key
	 * @return true if cache is existed
	 */
	public static boolean cacheExists(String key) {
		RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
			@Override
			protected Boolean build() throws JedisException {
				return jedis.exists(buildKey(key));
			}
		};
		return cmd.execute();
	}

	// Delete a key from cache
	public static void deleteCache(String key) {
		RedisCommand<Void> cmd = new RedisCommand<>(jedisPool) {
			@Override
			protected Void build() throws JedisException {
				jedis.del(buildKey(key));
				return null;
			}
		};
		cmd.execute();
	}

	/////////////////////////////////////////////////////////////////

	public static long getCacheAsLong(String key) {
		return StringUtil.safeParseLong(getCache(key));
	}

	public static int getCacheAsInteger(String key) {
		return StringUtil.safeParseInt(getCache(key));
	}

	// Store data in cache
	public static void setCache(String key, String value) {
		setCacheWithExpiry(key, value, DEFAULT_EXPIRATION, false);
	}

	public static void setCache(String key, String value, int expiryTimeInSeconds) {
		setCacheWithExpiry(key, value, expiryTimeInSeconds, false);
	}

	public static void setCache(String key, long value, int expiryTimeInSeconds) {
		setCacheWithExpiry(key, String.valueOf(value), expiryTimeInSeconds, false);
	}

	public static void setCache(String key, int value, int expiryTimeInSeconds) {
		setCacheWithExpiry(key, String.valueOf(value), expiryTimeInSeconds, false);
	}

	/////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		// Example usage of the RedisCache class

		// Setting cache
		RedisCache.setCache("user:1001", "John Doe");

		// Retrieving cache
		String user = RedisCache.getCache("user:1001");
		System.out.println("Cached value: " + user);

		// Set cache with expiration
		RedisCache.setCacheWithExpiry("session:12345", "active", 3600, false);

		// Check if cache exists
		if (RedisCache.cacheExists("session:12345")) {
			System.out.println("Session is active.");
		}

		// Delete cache
		RedisCache.deleteCache("user:1001");

	}
}
