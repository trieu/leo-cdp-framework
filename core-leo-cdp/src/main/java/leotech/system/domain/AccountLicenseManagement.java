package leotech.system.domain;

import java.io.File;

import leotech.system.dao.LeoCdpLicenseDaoUtil;
import leotech.system.model.LeoCdpLicense;
import leotech.system.util.GoogleStorageUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.util.FileUtils;

/**
 * LeoCdpLicense Management
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class AccountLicenseManagement {
	
	/**
	 * @param accountName
	 * @param quota
	 * @param adminDomain
	 * @param adminEmail
	 * @param validYears
	 * @param hasB2Bfeatures
	 * @return boolean
	 */
	public static boolean createLicense(String accountName, long quota, String adminDomain, String adminEmail, int validYears, boolean hasB2Bfeatures) {
		LeoCdpLicense licenseKey = LeoCdpLicense.buildLicenseKey(accountName, quota, adminDomain, adminEmail, validYears);
		if(licenseKey == null) {
			return false;
		}
		
		StringBuilder features = new StringBuilder();
		if(hasB2Bfeatures) {
			features.append(SystemMetaData.B2B);
		}
		String jarNames = licenseKey.getJarMain() + "\n";
		jarNames += licenseKey.getJarObserver() + "\n";
		jarNames += licenseKey.getJarScheduler();

		String content = licenseKey.getLicenseContent() + " " + quota + " " + features.toString() + "\n" + jarNames;
		String fileName = licenseKey.getFileName();
		FileUtils.writeStringToFile(LeoCdpLicense.LICENSE_KEYS_URI + fileName, content);
		
		String bucketName = LeoCdpLicense.GOOGLE_CLOUD_BUCKET_LEOCDP_LICENSE;
		String objectName = fileName;
		String baseLicenseFolder = "file://" + new File(LeoCdpLicense.LICENSE_KEYS_URI).getAbsolutePath() + "/";
		String contentType = "text/plain";
		String licenseFileUrl = GoogleStorageUtil.uploadPublicFile(contentType, bucketName, objectName, baseLicenseFolder, LeoCdpLicense.GOOGLE_CLOUD_PUBLIC_BASE_URL);
		licenseKey.setLicenseFileUrl(licenseFileUrl);
		
		return LeoCdpLicenseDaoUtil.save(licenseKey);
	}
}
