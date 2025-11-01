package leotech.cdp.domain.cache;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.model.customer.Profile;
import leotech.system.util.LogUtil;
import leotech.system.util.TaskRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;

/**
 * the Redis cache util for admin worker
 * 
 * @author tantrieuf31
 *
 */
public final class AdminRedisCacheUtil {

	static JedisPool jedisPool = RedisClientFactory.buildRedisPool("pubSubQueue");

    /**
     * Clears cache across all active observer Redis channels.
     * Runs asynchronously via TaskRunner.
     */
    public static void clearCacheAllObservers() {
        TaskRunner.run(() -> {
            try {
                new RedisCommand<Boolean>(jedisPool) {
                    @Override
                    protected Boolean build(Jedis jedis) throws JedisException {
                        String pattern = ObserverRedisCacheUtil.LEO_OBSERVER + "*";

                        List<String> activeChannels = getPubSubChannels(jedis, pattern);
                        if (activeChannels.isEmpty()) {
                            LogUtil.println("No active observer channels found for pattern: " + pattern);
                            return false;
                        }

                        LogUtil.println("Found " + activeChannels.size() + " active observer channels.");
                        for (String channel : activeChannels) {
                            LogUtil.println("Publishing cache-clear event to " + channel);
                            jedis.publish(channel, ObserverRedisCacheUtil.RELOAD_ALL_CACHES);
                        }
                        return true;
                    }
                }.execute();
            } catch (Exception e) {
                LogUtil.logError(AdminRedisCacheUtil.class, "Failed to clear observer caches: " + e.getMessage());
            }
        });
    }

    /**
     * Utility method to get list of active pub/sub channels matching a pattern.
     * Executes "PUBSUB CHANNELS <pattern>" manually since Jedis 7+ no longer provides a wrapper.
     */
    @SuppressWarnings("unchecked")
    private static List<String> getPubSubChannels(Jedis jedis, String pattern) {
        List<String> channels = new ArrayList<>();
        try {
            // JedisPooled#sendCommand returns Object, cast to List<Object>
            List<Object> rawChannels = (List<Object>) jedis.sendCommand(
                    Protocol.Command.PUBSUB,
                    "CHANNELS", pattern
            );

            if (rawChannels != null) {
                for (Object obj : rawChannels) {
                    if (obj instanceof byte[]) {
                        channels.add(new String((byte[]) obj, StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.logError(AdminRedisCacheUtil.class, "Error retrieving pub/sub channels: " + e.getMessage());
        }
        return channels;
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
			protected Void build(Jedis jedis) throws JedisException {
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
