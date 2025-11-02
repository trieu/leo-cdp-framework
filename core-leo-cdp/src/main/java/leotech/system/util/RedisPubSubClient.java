package leotech.system.util;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;

/**
 * Redis Client utility for Jedis 7.x and Redis 8.x
 *
 * Thread-safe producer methods; subscriber runs in its own thread.
 * Provides queue helpers and Pub/Sub.
 *
 * @author tantrieuf31
 */
public class RedisPubSubClient {

    public static final String PUB_SUB_QUEUE_REDIS = "pubSubQueue";
    private static final JedisPool jedisPool = RedisClientFactory.buildRedisPool(PUB_SUB_QUEUE_REDIS);
    private static final ExecutorService SUBSCRIBER_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "RedisPubSub-Subscriber");
        t.setDaemon(true);
        return t;
    });

    public interface RedisPubSubCallback {
        void process(String channel, String message);
        void log(String s);
    }

    // ------------------------------------------------------------------------
    // Publish / Subscribe
    // ------------------------------------------------------------------------

    /** Subscribe to a Redis channel asynchronously. */
    public static void subscribeAsync(final String channelName, RedisPubSubCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        Objects.requireNonNull(channelName, "channelName must not be null");

        SUBSCRIBER_EXECUTOR.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                JedisPubSub listener = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        try {
                            callback.process(channel, message);
                        } catch (Exception e) {
                            callback.log("Error processing message: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onSubscribe(String channel, int count) {
                        callback.log("Subscribed to " + channel + " (" + count + " total)");
                    }

                    @Override
                    public void onUnsubscribe(String channel, int count) {
                        callback.log("Unsubscribed from " + channel + " (" + count + " remaining)");
                    }
                };
                jedis.subscribe(listener, channelName); // blocking
            } catch (Exception e) {
                callback.log("Subscription error: " + e.getMessage());
            }
        });
    }

    /** Publish a message to a channel. */
    public static void publish(final String channelName, final String message, Consumer<Long> done) {
        new RedisCommand<Long>(jedisPool) {
            @Override
            protected Long build(Jedis jedis) throws JedisException {
                return jedis.publish(channelName, message);
            }
        }.executeAsync(done, null);
    }

    // ------------------------------------------------------------------------
    // Simple queue operations
    // ------------------------------------------------------------------------

    public static void enqueue(final String queueName, final String element, Consumer<Long> done) {
        new RedisCommand<Long>(jedisPool) {
            @Override
            protected Long build(Jedis jedis) throws JedisException {
                return jedis.lpush(queueName, element);
            }
        }.executeAsync(done, null);
    }

    public static void dequeue(final String queueName, Consumer<String> done) {
        new RedisCommand<String>(jedisPool) {
            @Override
            protected String build(Jedis jedis) throws JedisException {
                return jedis.lpop(queueName);
            }
        }.executeAsync(done, null);
    }

    // ------------------------------------------------------------------------
    // Cleanup
    // ------------------------------------------------------------------------

    public static void shutdown() {
        SUBSCRIBER_EXECUTOR.shutdownNow();
        jedisPool.close();
    }
}
