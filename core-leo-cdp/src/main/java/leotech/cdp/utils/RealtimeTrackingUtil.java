package leotech.cdp.utils;

import java.util.Date;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.DateTimeUtil;

/**
 * Created by TrieuNT
 */
public class RealtimeTrackingUtil {

	final public static JedisPooled redisAdDataStats = RedisConfigs.load().get("realtimeDataStats").getJedisClient();
	public static final int AFTER_3_DAYS = 60 * 60 * 24 * 3;
	public static final int AFTER_7_DAYS = 60 * 60 * 24 * 7;

	public static boolean trackContentViewFromUser(long unixTime, String contentId, String userId) {
		return addUser("up:", unixTime, contentId, userId);
	}

	public static boolean addUser(String keyPrefix, long unixTime, String contentId, final String uuid) {
		return queueLogsUser.add(new UserEventLog(keyPrefix, unixTime, contentId, uuid));
	}

	final static class UserEventLog {
		String keyPrefix;
		long unixTime;
		String contentId;
		String uuid;

		public UserEventLog(String keyPrefix, long unixTime, String contentId, String uuid) {
			super();
			this.keyPrefix = keyPrefix;
			this.unixTime = unixTime;
			this.contentId = contentId;
			this.uuid = uuid;
		}

		public String getKeyPrefix() {
			return keyPrefix;
		}

		public long getUnixTime() {
			return unixTime;
		}

		public String getUuid() {
			return uuid;
		}

		public String getContentId() {
			return contentId;
		}
	}

	private static Queue<UserEventLog> queueLogsUser = new ConcurrentLinkedQueue<>();

	private static Timer timer = new Timer(true);

	static {
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				for (int i = 0; i < 200; i++) {
					UserEventLog log = queueLogsUser.poll();
					if (log == null) {
						break;
					}
					try {
						String keyPrefix = log.getKeyPrefix();
						long unixTime = log.getUnixTime();
						String cId = log.getContentId();
						String uuid = log.getUuid();

						Date date = new Date(unixTime * 1000L);
						final String dateStr = DateTimeUtil.formatDate(date, DateTimeUtil.DATE_FORMAT_PATTERN);

						new RedisCommand<Boolean>(redisAdDataStats) {
							@Override
							protected Boolean build() throws JedisException {
								Pipeline p = jedis.pipelined();
								String keyTotal = keyPrefix + "t";
								String keyDaily = keyPrefix + dateStr + ":t";
								String keyHourly = keyPrefix + dateStr + ":" + cId;

								p.pfadd(keyTotal, uuid);
								p.pfadd(keyDaily, uuid);
								p.pfadd(keyHourly, uuid);
								p.expire(keyDaily, AFTER_7_DAYS);
								p.expire(keyHourly, AFTER_3_DAYS);

								p.sync();
								return true;
							}
						}.execute();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}, 2000, 2000);	
	}
}
