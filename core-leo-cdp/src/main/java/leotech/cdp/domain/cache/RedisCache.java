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

    // thread-safe JedisPooled instance
    private static final JedisPooled jedisClient =
            RedisConfigs.load().get(MASTER_CACHE).getJedisClient();

    // Redis I/O thread pool
    // Using cached thread pool to handle bursty load and prevent blocking
    private static final ExecutorService redisExecutor =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "redis-io-thread");
                t.setDaemon(true);
                return t;
            });

    private static String buildKey(String key) {
        return SystemMetaData.DOMAIN_CDP_ADMIN + "_" + key;
    }

    // -------------------------------------------------------------------------
    // ASYNC (safe for Vert.x)
    // -------------------------------------------------------------------------

    /** Async set with expiry */
    public static CompletableFuture<Boolean> setCacheWithExpiryAsync(String key, Object value, int expirySeconds, boolean valueIsJson) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = valueIsJson ? new Gson().toJson(value) : String.valueOf(value);
                jedisClient.setex(buildKey(key), expirySeconds, json);
                return true;
            } catch (Exception e) {
                throw new JedisException("Failed to set cache", e);
            }
        }, redisExecutor);
    }

    /** Async get */
    public static CompletableFuture<String> getCacheAsync(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return jedisClient.get(buildKey(key));
            } catch (Exception e) {
                throw new JedisException("Failed to get cache", e);
            }
        }, redisExecutor);
    }

    /** Async check existence */
    public static CompletableFuture<Boolean> cacheExistsAsync(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return jedisClient.exists(buildKey(key));
            } catch (Exception e) {
                throw new JedisException("Failed to check cache", e);
            }
        }, redisExecutor);
    }

    /** Async TTL check */
    public static CompletableFuture<Long> ttlAsync(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long ttl = jedisClient.ttl(buildKey(key));
                // Handle special Redis responses (-2: no key, -1: no expiry)
                if (ttl < 0) return 0L;
                return ttl;
            } catch (Exception e) {
                throw new JedisException("Failed to get TTL", e);
            }
        }, redisExecutor);
    }

    /** Async delete */
    public static CompletableFuture<Void> deleteCacheAsync(String key) {
        return CompletableFuture.runAsync(() -> {
            try {
                jedisClient.del(buildKey(key));
            } catch (Exception e) {
                throw new JedisException("Failed to delete cache", e);
            }
        }, redisExecutor);
    }

    // -------------------------------------------------------------------------
    // SYNC METHODS (for internal or testing use)
    // -------------------------------------------------------------------------

    protected static void setCacheWithExpiry(String key, Object value, int expirySeconds, boolean valueIsJson) {
        new RedisCommand<Boolean>(jedisClient) {
            @Override
            protected Boolean build() {
                String json = valueIsJson ? new Gson().toJson(value) : String.valueOf(value);
                jedis.setex(buildKey(key), expirySeconds, json);
                return true;
            }
        }.execute();
    }

    protected static String getCache(String key) {
        return new RedisCommand<String>(jedisClient) {
            @Override
            protected String build() {
                return jedis.get(buildKey(key));
            }
        }.execute();
    }

    protected static boolean cacheExists(String key) {
        return new RedisCommand<Boolean>(jedisClient) {
            @Override
            protected Boolean build() {
                return jedis.exists(buildKey(key));
            }
        }.execute();
    }

    protected static void deleteCache(String key) {
        new RedisCommand<Void>(jedisClient) {
            @Override
            protected Void build() {
                jedis.del(buildKey(key));
                return null;
            }
        }.execute();
    }

    // -------------------------------------------------------------------------
    // UTILITIES
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

    // -------------------------------------------------------------------------
    // SHUTDOWN (optional)
    // -------------------------------------------------------------------------

    public static void shutdownExecutor() {
        redisExecutor.shutdown();
    }

    // -------------------------------------------------------------------------
    // DEMO
    // -------------------------------------------------------------------------
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Testing async Redis...");

        setCacheWithExpiryAsync("user:1001", "John Doe", 60, false)
                .thenAccept(ok -> System.out.println("Cache set OK: " + ok))
                .exceptionally(e -> { e.printStackTrace(); return null; });

        getCacheAsync("user:1001")
                .thenAccept(v -> System.out.println("Cache value: " + v))
                .exceptionally(e -> { e.printStackTrace(); return null; });

        cacheExistsAsync("user:1001")
                .thenAccept(exists -> System.out.println("Cache exists: " + exists));

        ttlAsync("user:1001")
                .thenAccept(ttl -> System.out.println("TTL: " + ttl + "s"));

        deleteCacheAsync("user:1001")
                .thenRun(() -> System.out.println("Cache deleted."));

        // Keep main thread alive for async tasks
        Thread.sleep(2000);
        shutdownExecutor();
    }
}
