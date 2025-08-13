package leotech.system.util;

import leotech.system.model.LogData;
import rfx.core.util.SecurityUtil;

/**
 * beacon util tp convert encrypted data to
 * 
 * @author trieu
 *
 */
public class TrackingBeaconUtil {

	public static String decryptBeaconValue(String beacon) {
		return SecurityUtil.decryptBeaconValue(beacon);
	}

	public static LogData parseBeaconData(String metric, String encodedStr, long loggedTime) {
		try {
			return new LogData(metric, encodedStr, loggedTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String createBeaconValue(String uuid, int placementId, int lineItemId, int orderId, int priceModel,
			int adType, int adFormatId, int publisherId, int advertiserId, long deliveredTime) {
		LogData d = new LogData();
		// TODO
		return d.getBeaconData();
	}

	public static long currentSystemTimeInSecond() {
		return System.currentTimeMillis() / 1000L;
	}

}
