package test.cdp.campaign;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;

public class FrequencyCappingTest {

	public static final String MONTHS = "months";
	public static final String WEEKS = "weeks";
	public static final String HOURS = "hours";
	public static final String DAYS = "days";

	public static final String REDIS_PUB_SUB_QUEUE = "pubSubQueue";
	private static final JedisPooled jedisPooled = RedisConfigs.load().get(REDIS_PUB_SUB_QUEUE).getJedisClient();
	private static final String PREFIX = "frequency_cap:";

	/**
	 * Check and increment message count for a given user and time window. Returns
	 * true if within limit, false otherwise.
	 */
	public static boolean checkAndIncrementMessageCount(String type, String id, String timeUnit, int limit) {
		String key = PREFIX + type + ":" + id + ":" + timeUnit.toLowerCase();

		try {
			long currentCount = jedisPooled.incr(key);

			// Set expiry only if this is the first increment (avoids reset TTL each
			// time)
			if (currentCount == 1) {
				jedisPooled.expire(key, getTimeUnitInSeconds(timeUnit));
			}

			return currentCount <= limit;
		} catch (JedisException e) {
			System.err.println("Redis error while updating frequency cap: " + e.getMessage());
			return false;
		}
	}

	private static int getTimeUnitInSeconds(String timeUnit) {
		switch (timeUnit.toLowerCase()) {
		case DAYS:
			return 86400;
		case HOURS:
			return 3600;
		case WEEKS:
			return 604800;
		case MONTHS:
			return 2592000;
		default:
			throw new IllegalArgumentException("Unsupported time unit: " + timeUnit);
		}
	}

	public static void main(String[] args) {
		String timeUnit = DAYS;
		int limit = 2;

		testFrequency("email", "test@example.com", timeUnit, limit);
		testFrequency("zalo_oa", "12345", timeUnit, limit);
		testFrequency("zns", "84903122290", timeUnit, limit);
		testFrequency("sms", "84903122290", timeUnit, limit);
	}

	private static void testFrequency(String type, String id, String timeUnit, int limit) {
		boolean allowed = checkAndIncrementMessageCount(type, id, timeUnit, limit);
		String status = allowed ? "✅ Allowed" : "⛔ Limit reached";
		System.out.printf("%s: %s for %s (%s)%n", status, type, id, timeUnit);
	}
}
