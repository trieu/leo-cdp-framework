package leotech.cdp.domain.cache;

import java.util.function.Consumer;

import com.google.gson.Gson;

import leotech.system.version.SystemMetaData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;

/**
 * Thread-safe RedisCache using Java concurrent API.
 *
 * Designed for Vert.x or any reactive environment to avoid blocking the event loop.
 * All Redis I/O runs in a dedicated thread pool (no Vert.x executeBlocking needed).
 *
 * Works perfectly with Vert.x 3.8.5 and Java 11.
 *
 * @author Trieu
 * @since 2025
 */
public class RedisCache {

    public static final int DEFAULT_EXPIRATION = 60;
    public static final String MASTER_CACHE = "masterCache";
 
    static JedisPool jedisPool =  RedisClientFactory.buildRedisPool(MASTER_CACHE);


    private static String buildKey(String key) {
        return SystemMetaData.DOMAIN_CDP_ADMIN + "_" + key;
    }

    // -------------------------------------------------------------------------
    // ASYNC (safe for Vert.x)
    // -------------------------------------------------------------------------

    /** Async set with expiry */
    public static void  setCacheWithExpiryAsync(String key, Object value, int expirySeconds, boolean valueIsJson) {
      
        new RedisCommand<Void>(jedisPool) {
            @Override
            protected Void build(Jedis jedis) {
            	String json = valueIsJson ? new Gson().toJson(value) : String.valueOf(value);
            	jedis.setex(buildKey(key), expirySeconds, json);
                return null;
            }
        }.executeAsync();
    }

    /** Async get */
    public static void getCacheAsync(String key, Consumer<String> done) {        
        new RedisCommand<Void>(jedisPool) {
            @Override
            protected Void build(Jedis jedis) {
                done.accept(jedis.get(buildKey(key)));
                return null;
            }
        }.executeAsync();
    }

  

    /** Async TTL check */
    public static void ttlAsync(String key, Consumer<Long> done) {
        new RedisCommand<Void>(jedisPool) {
            @Override
            protected Void build(Jedis jedis) {
            	long ttl = jedis.ttl(buildKey(key));
                // Handle special Redis responses (-2: no key, -1: no expiry)
                if (ttl < 0)  done.accept(0L);
                else done.accept(ttl);
                
                return null;
            }
        }.executeAsync();
    }

    /** Async delete */
    public static void deleteCacheAsync(String key) {
    	new RedisCommand<Boolean>(jedisPool) {
            @Override
            protected Boolean build(Jedis jedis) {
            	jedis.del(buildKey(key));
                return true;
            }
        }.executeAsync();
    }

    // -------------------------------------------------------------------------
    // SYNC METHODS (for internal or testing use)
    // -------------------------------------------------------------------------

    protected static void setCacheWithExpiry(String key, Object value, int expirySeconds, boolean valueIsJson) {
        new RedisCommand<Boolean>(jedisPool) {
            @Override
            protected Boolean build(Jedis jedis) {
                String json = valueIsJson ? new Gson().toJson(value) : String.valueOf(value);
                jedis.setex(buildKey(key), expirySeconds, json);
                return true;
            }
        }.execute();
    }

    protected static String getCache(String key) {
        return new RedisCommand<String>(jedisPool) {
            @Override
            protected String build(Jedis jedis) {
                return jedis.get(buildKey(key));
            }
        }.execute();
    }

    protected static boolean cacheExists(String key) {
        return new RedisCommand<Boolean>(jedisPool) {
            @Override
            protected Boolean build(Jedis jedis) {
                return jedis.exists(buildKey(key));
            }
        }.execute();
    }

    protected static void deleteCache(String key) {
        new RedisCommand<Void>(jedisPool) {
            @Override
            protected Void build(Jedis jedis) {
                jedis.del(buildKey(key));
                return null;
            }
        }.execute();
    }

    // -------------------------------------------------------------------------
    // UTILITIES
    // -------------------------------------------------------------------------


    public static long getCacheAsLong(String key) {
        return StringUtil.safeParseLong(getCache(key));
    }

    public static int getCacheAsInteger(String key) {
        return StringUtil.safeParseInt(getCache(key));
    }

}
