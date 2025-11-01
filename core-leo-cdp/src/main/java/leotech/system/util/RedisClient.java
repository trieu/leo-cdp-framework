package leotech.system.util;

import java.util.function.Consumer;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;

/**
 * Redis Client utility for Jedis 7.x
 * 
 * Thread-safe, uses JedisPooled internally
 * 
 * @author tantrieuf31
 */
public class RedisClient {
	
	public static final String PUB_SUB_QUEUE_REDIS = "pubSubQueue";

	private static JedisPool jedisPool = RedisClientFactory.buildRedisPool(PUB_SUB_QUEUE_REDIS);

	public interface RedisPubSubCallback {
		void process(String channel, String message);

		void log(String s);
	}

	// ------------------------------------------------------------------------

	/** Create and configure the pooled client */
	public RedisClient() {

	}

	// ------------------------------------------------------------------------
	// Pub/Sub
	// ------------------------------------------------------------------------

	public static void commandSubscribe(final String channelName, RedisPubSubCallback callback) {
		new RedisCommand<Void>(jedisPool) {
			@Override
			protected Void build(Jedis jedis) throws JedisException {
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
		}.executeAsync(null, null);
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

}
