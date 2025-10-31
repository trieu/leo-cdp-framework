package test.cdp.profile;

import java.util.HashMap;

import com.google.gson.Gson;

import leotech.cdp.domain.ProfileQueryManagement;
import leotech.cdp.model.customer.Profile;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.Utils;

public class TestImportProfileByRedisPubSub {

	static JedisPooled jedisPool = RedisConfigs.load().get("pubSubQueue").getJedisClient();
	
	public static void main(String[] args) {
		
		Profile profile = ProfileQueryManagement.getByIdForSystem("5Xn9rVXDUcYa2hIkN4mUaX");
		profile.setFirstName("Trieu");
		profile.setLastName("Nguyen");
		
		HashMap<String, Object> extAttributes = new HashMap<String, Object>();
		extAttributes.put("bank_account", "12345");
		extAttributes.put("trading_account", "656546");
		
		profile.setExtAttributes(extAttributes);
		
		
		RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
			@Override
			protected Boolean build() throws JedisException {

				String channel = "profile-import";
				jedis.publish(channel, new Gson().toJson(profile));
	
				return true;
			}
		};
		cmd.execute();
		
		Utils.exitSystemAfterTimeout(3000);
	}
}
