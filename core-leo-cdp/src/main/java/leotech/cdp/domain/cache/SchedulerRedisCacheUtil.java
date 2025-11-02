package leotech.cdp.domain.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.system.util.RedisPubSubClient;
import leotech.system.util.RedisPubSubClient.RedisPubSubCallback;
import redis.clients.jedis.JedisPool;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.util.Utils;

/**
 * @author tantrieuf31
 *
 */
public final class SchedulerRedisCacheUtil {
	
	static Logger logger = LoggerFactory.getLogger(SchedulerRedisCacheUtil.class);
	
	static JedisPool jedisPool = RedisClientFactory.buildRedisPool("pubSubQueue");
	
	static RedisPubSubCallback redisPubSubCallback = new RedisPubSubCallback() {
		@Override
		public void process(String channel, String message) {
			logger.info("Channel " + channel + " has sent a message : " + message);
			doJob(message);
		}
		@Override
		public void log(String s) {
			logger.info(s);
		}
	};

	public final static void start(String workerName) {
		Utils.sleep(3000);
		RedisPubSubClient.subscribeAsync(workerName, redisPubSubCallback);
	}

	final static void doJob(String jobName) {
		
		// TODO add more
		
		System.out.println("DataSchedulerRedisCacheUtil => " +  jobName);
	}
}
