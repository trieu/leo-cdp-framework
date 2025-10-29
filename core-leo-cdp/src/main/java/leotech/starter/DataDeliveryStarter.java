package leotech.starter;

import leotech.system.HttpWorker;
import leotech.system.util.LogUtil;

/**
 * 
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public class DataDeliveryStarter {

	public static void main(String[] args) throws Exception {
		LogUtil.loadLoggerConfigs();
		if (args.length == 1) {
			String workerName = args[0];
			HttpWorker.start(workerName);
		} else {
			HttpWorker.start("localLeoDeliveryWorker");
		}
	}
}