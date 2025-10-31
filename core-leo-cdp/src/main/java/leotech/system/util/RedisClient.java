package leotech.system.util;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisCommand;

/**
 * Redis Client utility for Jedis 7.x
 * 
 * Thread-safe, uses JedisPooled internally
 * 
 * @author tantrieuf31
 */
public class RedisClient {

    private String host = "127.0.0.1";
    private int port = 6480;

    private JedisPooled jedisPooled;

    // Connection pool-like settings (for legacy semantics)
    private static Integer maxActive = 20;
    private static Integer maxIdle = 5;
    private static Long maxWait = 1000L;

    public interface RedisPubSubCallback {
        void process(String channel, String message);
        void log(String s);
    }

    // ------------------------------------------------------------------------

    /** Create and configure the pooled client */
    public RedisClient(String host, int port) {
        this.host = host;
        this.port = port;
        initClient();
    }

    private void initClient() {
        // Build a JedisClientConfig instead.
        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(maxWait.intValue())
                .socketTimeoutMillis(maxWait.intValue())
                .clientName("leotech-client")
                .build();

        this.jedisPooled = new JedisPooled(new HostAndPort(host, port), clientConfig);
    }

    // ------------------------------------------------------------------------
    // Pub/Sub
    // ------------------------------------------------------------------------

    public static RedisCommand<Void> commandSubscribe(
            JedisPooled jedisClient,
            final String channelName,
            RedisPubSubCallback callback) {

        return new RedisCommand<Void>(jedisClient) {
            @Override
            protected Void build() throws JedisException {
                JedisPubSub jedisPubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        callback.process(channel, message);
                    }

                    @Override
                    public void onSubscribe(String channel, int subscribedChannels) {
                        callback.log("Subscribed Redis at channel: " + channel);
                        callback.log("Subscribed to " + subscribedChannels + " channels");
                    }

                    @Override
                    public void onUnsubscribe(String channel, int subscribedChannels) {
                        callback.log("Unsubscribed Redis at channel: " + channel);
                        callback.log("Remaining subscriptions: " + subscribedChannels);
                    }
                };
                jedis.subscribe(jedisPubSub, channelName);
                return null;
            }
        };
    }

    public static RedisCommand<Void> commandPubSub(
            String host, int port,
            final String channelName,
            RedisPubSubCallback callback) {
        JedisPooled jedisClient = new RedisClient(host, port).getJedisPooled();
        return RedisClient.commandSubscribe(jedisClient, channelName, callback);
    }

    // ------------------------------------------------------------------------
    // Simple queue operations
    // ------------------------------------------------------------------------

    public static void enqueue(JedisPooled jedisClient, final String queueName, final String element) {
        new RedisCommand<Void>(jedisClient) {
            @Override
            protected Void build() throws JedisException {
                jedis.lpush(queueName, element);
                return null;
            }
        }.execute();
    }

    public static String dequeue(JedisPooled jedisClient, final String queueName) {
        return new RedisCommand<String>(jedisClient) {
            @Override
            protected String build() throws JedisException {
                return jedis.lpop(queueName);
            }
        }.execute();
    }

    // ------------------------------------------------------------------------
    // Getters / setters
    // ------------------------------------------------------------------------

    public JedisPooled getJedisPooled() {
        return jedisPooled;
    }

    public void setJedisPooled(JedisPooled jedisPooled) {
        this.jedisPooled = jedisPooled;
    }

    public static Integer getMaxActive() {
        return maxActive;
    }

    public static void setMaxActive(Integer maxActive) {
        RedisClient.maxActive = maxActive;
    }

    public static Integer getMaxIdle() {
        return maxIdle;
    }

    public static void setMaxIdle(Integer maxIdle) {
        RedisClient.maxIdle = maxIdle;
    }

    public static Long getMaxWait() {
        return maxWait;
    }

    public static void setMaxWait(Long maxWait) {
        RedisClient.maxWait = maxWait;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
