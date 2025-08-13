package test.ad.ssp;

import java.util.Date;
import java.util.Map;

import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.DateTimeUtil;

public class GetData {
	public static final String DATE_HOURLY = "yyyy-MM-dd-HH";
	public static final String DATE = "yyyy-MM-dd";
	
	static ShardedJedisPool jedisPool = RedisConfigs.load().get("adsPlayData").getShardedJedisPool();
	public static void main(String[] args) {
		Date date = new Date();
		final String dateHourStr = DateTimeUtil.formatDate(date ,DATE_HOURLY);
		Map<String, String> objCb = new RedisCommand<Map<String, String>>(jedisPool) {
			@Override
			protected Map<String, String> build() throws JedisException {
				String keyH = "m:"+dateHourStr;				
				Pipeline p = jedis.pipelined();				
				Response<Map<String, String>> objCb = p.hgetAll(keyH);
				p.sync();
				return objCb.get();
			}
		}.execute();
		System.out.println(objCb);
	}
}
