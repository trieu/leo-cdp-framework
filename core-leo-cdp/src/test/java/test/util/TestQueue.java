package test.util;

import leotech.system.util.RedisClient;
import redis.clients.jedis.JedisPooled;
import rfx.core.configs.RedisConfigs;
import rfx.core.util.Utils;

public class TestQueue {
	
	static JedisPooled jedisPool = RedisConfigs.load().get("pubSubQueue").getJedisClient();

	public static void main(String[] args) {
		String segmentId = "segment1";
		RedisClient.enqueue(jedisPool, segmentId, "job1");
		RedisClient.enqueue(jedisPool, segmentId, "job2");
		RedisClient.enqueue(jedisPool, segmentId, "job3");
		
		String rs = RedisClient.dequeue(jedisPool, segmentId);
		while (rs != null) {
			System.out.println("dequeue "+rs);
			rs = RedisClient.dequeue(jedisPool, segmentId);
		}
		
		Utils.exitSystemAfterTimeout(1);
	}
}
