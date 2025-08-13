package leotech.cdp.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.system.util.RedisClient;
import leotech.system.util.RedisClient.RedisPubSubCallback;
import leotech.system.util.TaskRunner;
import rfx.core.util.Utils;

/**
 * Data Importing
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class DataImporterManagement {

	static Logger logger = LoggerFactory.getLogger(DataImporterManagement.class);
	
	static RedisPubSubCallback redisPubSubCallback = new RedisPubSubCallback() {
		
		@Override
		public void process(String channel, String message) {
			doProfileImportingJob(message);
		}
		
		@Override
		public void log(String s) {
			logger.info(s);
		}
	};
	
	/**
	 * @param host
	 * @param port
	 * @param channelName
	 */
	public final static void startProfileImportingJob(String host, int port, String channelName) {
		logger.info("host " + host);
		logger.info("port " + port);
		logger.info("channelName " + channelName);
		
		TaskRunner.run(() -> {
			Utils.sleep(1000);
			RedisClient.commandPubSub(host, port, channelName, redisPubSubCallback).execute();
		});
	}

	final static void doProfileImportingJob(String json) {		
		logger.info("doProfileImportingJob => " +  json);
		String profileId = ProfileDataManagement.updateFromJson(json);
		logger.info("doProfileImportingJob OK profileId " +  profileId);
	}
}
