package leotech.cdp.domain.cache;

import java.util.List;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.model.customer.Profile;
import leotech.system.util.TaskRunner;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;

/**
 * the Redis cache util for admin worker
 * 
 * @author tantrieuf31
 *
 */
public final class AdminRedisCacheUtil {

	static ShardedJedisPool jedisPool = RedisConfigs.load().get("pubSubQueue").getShardedJedisPool();

	public static void clearCacheAllObservers() {
		TaskRunner.run(() -> {
			RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
				@Override
				protected Boolean build() throws JedisException {
					String pattern = ObserverRedisCacheUtil.LEO_OBSERVER + "*";
					List<String> channels = jedis.pubsubChannels(pattern);
					System.out.println("pubsub channels " + channels.size());
					for (String channel : channels) {
						System.out.println("Clear cache for the observer: " + channel);
						jedis.publish(channel, ObserverRedisCacheUtil.RELOAD_ALL_CACHES);
					}
					return true;
				}
			};
			cmd.execute();
		});
	}

	/**
	 * @param visitorId
	 */
	public static boolean setLeoChatBotForProfile(String visitorId, String displayName) {
		Profile profile = ProfileDaoUtil.getByVisitorId(visitorId);
		if(profile != null) {
			setLeoChatBotForProfile(visitorId, profile.getId(), displayName);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * @param visitorId
	 * @param profileId
	 * @param firstName
	 */
	public static void setLeoChatBotForProfile(String visitorId, String profileId, String firstName) {
		RedisCommand<Void> cmd = new RedisCommand<>(jedisPool) {
			@Override
			protected Void build() throws JedisException {
				jedis.hset(visitorId, "chatbot", "leobot");
				jedis.hset(visitorId, "profile_id", profileId);
				jedis.hset(visitorId, "name", firstName);
				return null;
			}
		};
		cmd.execute();
		System.out.println("setLeoChatBotForProfile " + visitorId + " profileId " + profileId);
	}

	

}
