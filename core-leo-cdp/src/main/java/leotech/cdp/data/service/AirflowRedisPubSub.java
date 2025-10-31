package leotech.cdp.data.service;

import java.util.Map;

import com.google.gson.Gson;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;

/**
 * Airflow Redis PubSub <br>
 * similar to: redis-cli -p 6480 publish agent_pubsub_queue '{"dag_id":"redis_airflow_dag","params":{"foo":"1234"}}'
 * 
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public final class AirflowRedisPubSub {
	
	public static final String AI_AGENT_EVENTS_TOPIC = "agent_pubsub_queue";
	
	static JedisPooled jedisPool = RedisConfigs.load().get("pubSubQueue").getJedisClient();
	
	public static void triggerAgentUsingAirflowDAG(String dagId, Map<String,Object> params) {
		String jsonParams = new Gson().toJson(params);
		RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
			@Override
			protected Boolean build() throws JedisException {
				Long rs = jedis.publish(dagId, jsonParams);
				return StringUtil.safeParseLong(rs) > 0;
			}
		};
		boolean rs = cmd.execute().booleanValue();
		if (rs) {
			System.out.println("RedisCommand.publish " + AI_AGENT_EVENTS_TOPIC + " DAG ID:" + dagId + " params:\n " + jsonParams);
			// TODO
		}
	}
	
}
