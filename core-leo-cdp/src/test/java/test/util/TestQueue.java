package test.util;

import leotech.system.util.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.Utils;

public class TestQueue {
	
	static JedisPool jedisPool =  RedisClientFactory.buildRedisPool("pubSubQueue");

	public static void main(String[] args) {
		
		new RedisCommand<Void>(jedisPool) {
			@Override
			protected Void build(Jedis jedis) throws JedisException {
				return null;
			}
		}.executeAsync();
		
		new RedisCommand<Void>(jedisPool) {
			@Override
			protected Void build(Jedis jedis) throws JedisException {
				String segmentId = "segment1";
				RedisClient.enqueue(jedisPool, segmentId, "job1");
				RedisClient.enqueue(jedisPool, segmentId, "job2");
				RedisClient.enqueue(jedisPool, segmentId, "job3");
				
				String rs = RedisClient.dequeue(jedisPool, segmentId);
				while (rs != null) {
					System.out.println("dequeue "+rs);
					rs = RedisClient.dequeue(jedisPool, segmentId);
				}
				
				return null;
			}
		}.executeAsync();
		
		
		
		Utils.exitSystemAfterTimeout(1);
	}
}
