package leotech.cdp.domain.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;

import leotech.system.version.SystemMetaData;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;

/**
 * Thread-safe RedisCache using Java concurrent API.
 * 
 * Designed for Vert.x or any reactive environment to avoid blocking the event loop.
 * Uses an internal ExecutorService to offload Redis I/O to worker threads.
 *
 * @author Trieu
 * @since 2025
 */
public class RedisCache {

    public static final int DEFAULT_EXPIRATION = 60;
    public static final String MASTER_CACHE = "masterCache";
    private static final JedisPooled jedisClient = RedisConfigs.load().get(MASTER_CACHE).getJedisClient();

    // a lightweight thread pool for Redis I/O (you can tune this)
    private static final ExecutorService redisExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
    );

    private static String buildKey(String key) {
        return SystemMetaData.DOMAIN_CDP_ADMIN + "_" + key;
    }

    // -------------------------------------------------------------------------
    // ASYNC METHODS — return CompletableFuture (non-blocking)
    // -------------------------------------------------------------------------

    /** Async set cache with expiry */
    public static CompletableFuture<Boolean> setCacheWithExpiryAsync(String key, Object value, int expirySeconds, boolean valueIsJson) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String v = valueIsJson ? new Gson().toJson(value) : String.valueOf(value);
                String k = buildKey(key);
                jedisClient.setex(k, expirySeconds, v);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                throw new JedisException("Failed to set cache asynchronously", e);
            }
        }, redisExecutor);
    }

    /** Async get cache */
    public static CompletableFuture<String> getCacheAsync(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return jedisClient.get(buildKey(key));
            } catch (Exception e) {
                throw new JedisException("Failed to get cache asynchronously", e);
            }
        }, redisExecutor);
    }

    /** Async check cache existence */
    public static CompletableFuture<Boolean> cacheExistsAsync(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return jedisClient.exists(buildKey(key));
            } catch (Exception e) {
                throw new JedisException("Failed to check cache asynchronously", e);
            }
        }, redisExecutor);
    }

    /** Async delete cache */
    public static CompletableFuture<Void> deleteCacheAsync(String key) {
        return CompletableFuture.runAsync(() -> {
            try {
                jedisClient.del(buildKey(key));
            } catch (Exception e) {
                throw new JedisException("Failed to delete cache asynchronously", e);
            }
        }, redisExecutor);
    }

    /** Async TTL check */
    public static CompletableFuture<Long> ttlAsync(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return jedisClient.ttl(buildKey(key));
            } catch (Exception e) {
                throw new JedisException("Failed to get TTL asynchronously", e);
            }
        }, redisExecutor);
    }

    // -------------------------------------------------------------------------
    // SYNC METHODS — safe to call from background threads only
    // -------------------------------------------------------------------------

    public static void setCacheWithExpiry(String key, Object value, int expirySeconds, boolean valueIsJson) {
        new RedisCommand<Boolean>(jedisClient) {
            @Override
            protected Boolean build() {
                String v = valueIsJson ? new Gson().toJson(value) : String.valueOf(value);
                jedis.setex(buildKey(key), expirySeconds, v);
                return true;
            }
        }.execute();
    }

    public static String getCache(String key) {
        return new RedisCommand<String>(jedisClient) {
            @Override
            protected String build() {
                return jedis.get(buildKey(key));
            }
        }.execute();
    }

    public static boolean cacheExists(String key) {
        return new RedisCommand<Boolean>(jedisClient) {
            @Override
            protected Boolean build() {
                return jedis.exists(buildKey(key));
            }
        }.execute();
    }

    public static void deleteCache(String key) {
        new RedisCommand<Void>(jedisClient) {
            @Override
            protected Void build() {
                jedis.del(buildKey(key));
                return null;
            }
        }.execute();
    }

    // -------------------------------------------------------------------------
    // UTILITY HELPERS
    // -------------------------------------------------------------------------

    public static boolean shouldUpdateCache(String key, long minSecondsToLive) {
        long ttl = new RedisCommand<Long>(jedisClient) {
            @Override
            protected Long build() {
                return jedis.ttl(buildKey(key));
            }
        }.execute();
        return ttl < minSecondsToLive;
    }

    public static long getCacheAsLong(String key) {
        return StringUtil.safeParseLong(getCache(key));
    }

    public static int getCacheAsInteger(String key) {
        return StringUtil.safeParseInt(getCache(key));
    }

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

    // -------------------------------------------------------------------------
    // GRACEFUL SHUTDOWN
    // -------------------------------------------------------------------------
    public static void shutdownExecutor() {
        redisExecutor.shutdown();
    }

    // -------------------------------------------------------------------------
    // DEMO
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        // async write
        setCacheWithExpiryAsync("user:1001", "John Doe", 120, false)
                .thenRun(() -> System.out.println("Cache saved."));

        // async read
        getCacheAsync("user:1001")
                .thenAccept(val -> System.out.println("Got from cache: " + val))
                .exceptionally(e -> { e.printStackTrace(); return null; });

        // async check
        cacheExistsAsync("user:1001").thenAccept(exists ->
                System.out.println("Cache exists? " + exists));

        // async delete
        deleteCacheAsync("user:1001").thenRun(() ->
                System.out.println("Cache deleted."));

        shutdownExecutor();
    }
}
