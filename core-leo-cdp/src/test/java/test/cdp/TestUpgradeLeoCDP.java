package test.cdp;

import leotech.system.domain.SystemControl;
import rfx.core.util.Utils;

public class TestUpgradeLeoCDP {

	public static void main(String[] args) {

		// test run test or update DEV
		SystemControl.upgradeSystem(true, true, false, null);
		Utils.exitSystemAfterTimeout(3210);
	}

	static void test() {
		System.out.println(SystemControl.runUpdateShellScript());
		Utils.exitSystemAfterTimeout(10000);
	}
}
