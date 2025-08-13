package test.cdp.campaign;

import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;

public class FrequencyCappingTest {

	public static final String MONTHS = "months";
	public static final String WEEKS = "weeks";
	public static final String HOURS = "hours";
	public static final String DAYS = "days";

	public static final String REDIS_PUB_SUB_QUEUE = "pubSubQueue";
	static ShardedJedisPool jedisPool = RedisConfigs.load().get(REDIS_PUB_SUB_QUEUE).getShardedJedisPool();
	private static final String PREFIX = "frequency_cap:";

	public static boolean checkAndIncrementMessageCount(String type, String id, String timeUnit, int limit) {
		RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
			@Override
			protected Boolean build() throws JedisException {
				String key = PREFIX + type + ":" + id + ":" + timeUnit.toString().toLowerCase();
				long currentCount = jedis.incr(key);
				jedis.expire(key, getTimeUnitInSeconds(timeUnit));

				return currentCount <= limit;
			}
		};
		return cmd.execute();
	}

	private static int getTimeUnitInSeconds(String timeUnit) {
		String t = timeUnit.toLowerCase();
		switch (t) {
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
		String email = "test@example.com";
		String timeUnit = DAYS;
		int limit = 2;

		if (checkAndIncrementMessageCount("email", email, timeUnit, limit)) {
			System.out.println("Email can be sent to " + email);
		} else {
			System.out.println("Frequency limit reached for " + email + " in " + timeUnit.toString().toLowerCase());
		}

		String zalo_user_id = "12345";
		if (checkAndIncrementMessageCount("zalo_oa", zalo_user_id, timeUnit, limit)) {
			System.out.println("zalo_oa can be sent to " + email);
		} else {
			System.out.println(
					"Frequency limit reached for " + zalo_user_id + " in " + timeUnit.toString().toLowerCase());
		}

		String phone1 = "84903122290";
		if (checkAndIncrementMessageCount("zns", phone1, timeUnit, limit)) {
			System.out.println("zns can be sent to " + phone1);
		} else {
			System.out.println("Frequency limit reached for " + phone1 + " in " + timeUnit.toString().toLowerCase());
		}

		String phone2 = "84903122290";
		if (checkAndIncrementMessageCount("sms", phone2, timeUnit, limit)) {
			System.out.println("sms can be sent to " + phone2);
		} else {
			System.out.println("Frequency limit reached for " + phone2 + " in " + timeUnit.toString().toLowerCase());
		}
	}
}
