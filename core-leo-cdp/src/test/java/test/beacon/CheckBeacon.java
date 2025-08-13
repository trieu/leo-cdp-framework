package test.beacon;

import leotech.system.model.LogData;
import leotech.system.util.TrackingBeaconUtil;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.SecurityUtil;

public class CheckBeacon {

	public static void main(String[] args) {
		String encodedStr = "zr1u211t20211u1vzrzn21zhzh1t1y1y1v1y1yzh21zm21202pzhzjzrzn2pzgzgzlzl2pzj2pzh2pzm2pzizjzhzr2pzizjznzj2pzj2pzizmzgzqzqznzqzrzlzg";
		String raw = SecurityUtil.decryptBeaconValue(encodedStr);
		System.out.println(raw);

		int loggedTime = DateTimeUtil.currentUnixTimestamp();
		LogData data = TrackingBeaconUtil.parseBeaconData("view", encodedStr, loggedTime);
		System.out.println(data);

	}
}
