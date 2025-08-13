package leotech.cdp.domain.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.domain.EventObserverManagement;
import leotech.system.util.RedisClient;
import leotech.system.util.RedisClient.RedisPubSubCallback;
import leotech.system.util.TaskRunner;
import redis.clients.jedis.ShardedJedisPool;
import rfx.core.configs.RedisConfigs;
import rfx.core.util.Utils;

/**
 * the Redis cache util for data observer worker
 * 
 * @author tantrieuf31
 *
 */
public final class ObserverRedisCacheUtil {
	
	private static final String PUB_SUB_QUEUE_REDIS = "pubSubQueue";

	static Logger logger = LoggerFactory.getLogger(ObserverRedisCacheUtil.class);

	public static final String LEO_OBSERVER = "leo_observer_";
	public static final String RELOAD_ALL_CACHES = "reload_all_caches";

	
	static RedisPubSubCallback redisPubSubCallback = new RedisPubSubCallback() {
		
		@Override
		public void process(String channel, String message) {
			logger.info("Channel " + channel + " has sent a message : " + message);
			if (RELOAD_ALL_CACHES.equalsIgnoreCase(message)) {
				reloadAllCaches();
			}
		}
		
		@Override
		public void log(String s) {
			logger.info(s);
		}
	};

	public final static void start(String workerName, boolean initPubSubObserver) {
		String channelName = LEO_OBSERVER + workerName;
		
		if(initPubSubObserver) {
			// start Pub Sub 
			logger.info("initPubSubObserver " + initPubSubObserver);
			EventObserverManagement.initAllInputDataObservers();
		}
		
		// start redis pubsub
		TaskRunner.run(() -> {
			Utils.sleep(4000);
			ShardedJedisPool jedisPool = RedisConfigs.load().get(PUB_SUB_QUEUE_REDIS).getShardedJedisPool();
			
			RedisClient.commandSubscribe(jedisPool, channelName, redisPubSubCallback).execute();
		});
	}

	final static void reloadAllCaches() {
		EventMetricManagement.loadCache();
		// TODO add more
		
		logger.info("RELOAD_ALL_CACHES => OK");
	}

}
