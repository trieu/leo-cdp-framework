package leotech.starter;

import leotech.cdp.domain.cache.ObserverRedisCacheUtil;
import leotech.system.HttpWorker;
import leotech.system.util.LogUtil;
import leotech.system.version.SystemMetaData;

public class DataObserverStarter {
	
	private static final String INPUT_DATA_SOURCE = "input_data_source";

	public static void main(String[] args) throws Exception {
		SystemMetaData.initTimeZoneGMT();
		LogUtil.setLogLevelToInfo();
		boolean initPubSubObserver = false;
		
		if (args.length >= 1) {	
			String workerName = args[0];
			if(args.length == 2) {
				initPubSubObserver = INPUT_DATA_SOURCE.equalsIgnoreCase(args[1]);
			}
			ObserverRedisCacheUtil.start(workerName, initPubSubObserver);
			HttpWorker.start(workerName);
			
		} else {
			// DEFAULT
			String workerName = SystemMetaData.HTTP_ROUTING_CONFIG_OBSERVER;
			ObserverRedisCacheUtil.start(workerName, initPubSubObserver);
			HttpWorker.start(workerName);
		}
		
	}
	
}
