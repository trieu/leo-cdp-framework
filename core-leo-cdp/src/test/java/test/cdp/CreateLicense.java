package test.cdp;

import leotech.system.domain.AccountLicenseManagement;
import rfx.core.util.Utils;

public class CreateLicense {

	public static void main(String[] args) {
		// generateFiles();
		
		String accountName = "demo";
		long quota = 1000000;
		String adminDomain = "example,com";
		String adminEmail = "trieu@example.com";
		int validYears = 5;
		boolean hasB2Bfeatures = true;
		
		try {
			AccountLicenseManagement.createLicense(accountName, quota, adminDomain, adminEmail, validYears, hasB2Bfeatures);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Utils.exitSystemAfterTimeout(3000);
	}
}
