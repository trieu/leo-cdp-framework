package leotech.system.util;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisCommand;

/**
 * Redis Client
 * 
 * @author tantrieuf31
 * @since 2023
 *
 */
public class RedisClient {

    private String host = "127.0.0.1";
    private int port = 6480;
    
    private Jedis jedis; 
    private JedisPool jedisPool; 
    private ShardedJedis shardedJedis; 
    private ShardedJedisPool shardedJedisPool; 

    private static Integer maxActive = 20;
    private static Integer maxIdle = 5;
    private static Long maxWait = 1000l;
    
    public static interface RedisPubSubCallback {
    	void process(String channel, String message);
    	void log(String s);
    }
 
    
    public static RedisCommand<Void> commandSubscribe(ShardedJedisPool jedisPool, final String channelName, RedisPubSubCallback callback) {
		return new RedisCommand<Void>(jedisPool) {
			@Override
			protected Void build() throws JedisException {
				/* Creating JedisPubSub object for subscribing with channels */
				JedisPubSub jedisPubSub = new JedisPubSub() {
					@Override
					public void onMessage(String channel, String message) {
						callback.process(channelName, message);
					}
					@Override
					public void onSubscribe(String channel, int subscribedChannels) {
						callback.log("Subscribed Redis at channel : " + channel);
						callback.log("Subscribed to " + subscribedChannels + " no. of channels");
					}
					@Override
					public void onUnsubscribe(String channel, int subscribedChannels) {
						callback.log("Unsubscribed Redis at channel : " + channel);
						callback.log("Subscribed to " + subscribedChannels + " no. of channels");
					}
				};
				jedis.subscribe(jedisPubSub, channelName);
				return null;
			}
		};
    }
    
    public static RedisCommand<Void> commandPubSub(String host, int port, final String channelName, RedisPubSubCallback callback) {
    	ShardedJedisPool jedisPool = new RedisClient(host, port).getShardedJedisPool();
		return RedisClient.commandSubscribe(jedisPool, channelName, callback);
    }
    
    /**
     * @param jedisPool
     * @param queueName
     * @param element
     */
    public static void enqueue(ShardedJedisPool jedisPool, final String queueName, final String element) {
		new RedisCommand<Void>(jedisPool) {
			@Override
			protected Void build() throws JedisException {
				jedis.lpush(queueName, element);
				return null;
			}
		}.execute();
    }
    
    /**
     * @param jedisPool
     * @param queueName
     * @return
     */
    public static String dequeue(ShardedJedisPool jedisPool, final String queueName) {
		return new RedisCommand<String>(jedisPool) {
			@Override
			protected String build() throws JedisException {
				return jedis.lpop(queueName);
			}
		}.execute();
    }


    public RedisClient(String host, int port) {
    	this.host = host;
    	this.port = port;
        initPool();
        initShardedPool();
    }

    private void initPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMaxWaitMillis(maxWait);
        config.setTestOnBorrow(false);
        jedisPool = new JedisPool(config, host, port);
    }

    private void initShardedPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(5);
        config.setMaxWaitMillis(maxWait);
        config.setTestOnBorrow(false);
        List<JedisShardInfo> shards = new ArrayList<>();
        shards.add(new JedisShardInfo(host, port, "master"));
        shardedJedisPool = new ShardedJedisPool(config, shards);
    }

    public Jedis getJedis() {
        return jedis;
    }

    public void setJedis(Jedis jedis) {
        this.jedis = jedis;
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public ShardedJedis getShardedJedis() {
        return shardedJedis;
    }

    public void setShardedJedis(ShardedJedis shardedJedis) {
        this.shardedJedis = shardedJedis;
    }

    public ShardedJedisPool getShardedJedisPool() {
        return shardedJedisPool;
    }

    public void setShardedJedisPool(ShardedJedisPool shardedJedisPool) {
        this.shardedJedisPool = shardedJedisPool;
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
