package test.cdp;

import leotech.system.domain.SystemUserManagement;
import rfx.core.util.Utils;

public class SystemUserTest {

	public static void main(String[] args) {
		String id = SystemUserManagement.resetLoginPassword("superadmin", "FullstackEngineer@2020");
		System.out.println("resetLoginPassword for userId " + id);
		Utils.exitSystemAfterTimeout(1000);
	}
}
