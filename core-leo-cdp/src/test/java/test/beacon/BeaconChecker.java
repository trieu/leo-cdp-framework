package test.beacon;

import rfx.core.util.SecurityUtil;

public class BeaconChecker {

	public static void main(String[] args) {
		System.out.println(SecurityUtil.decryptBeaconValue(
				"zr1tzk1t1v1tzrznzezhzn20zlzeznzjzn1yze1vzg1tzkzezkzkzlzmzrzh1vzgzj201tzr2pzj2pzj2pzj2pzj"));
	}
}
