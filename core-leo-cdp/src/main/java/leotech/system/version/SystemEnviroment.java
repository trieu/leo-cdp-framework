package leotech.system.version;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import leotech.system.util.HttpClientGetUtil;
import rfx.core.util.HashUtil;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;


/**
 * System Meta-data about CDP instance to upgrade
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class SystemEnviroment {
	
	private static final Logger logger = LoggerFactory.getLogger(SystemMetaData.class);
	
	/**
	 * check License Key to start or shut down all JAVA instances of LEOCDP
	 * 
	 * @return
	 */
	public final static int checkLicenseKey() {
		int rs = 0;
		logger.info("------------------------------------------------");
		logger.info("CDP BUILD_VERSION " + SystemMetaData.BUILD_VERSION);
		logger.info("CURRENT_RUNTIME_PATH " + SystemMetaData.CURRENT_RUNTIME_PATH);

		if (SystemMetaData.CDP_LICENSE_KEY.startsWith(SystemMetaData.BUILD_EDITION)) {
			String licenseInCloud = getLicenseFromCloud();
			if (StringUtil.isNotEmpty(licenseInCloud)) {
				String[] lines = licenseInCloud.split("\n");
				for (int i = 0; i < lines.length; i++) {
					String line = lines[i];
					logger.info(line);

					if (i == 0) {
						String[] tokens = line.split(StringPool.SPACE);
						if (tokens.length >= 2) {
							String licenseKeyFromCloud = tokens[0];
							long quotaProfile = StringUtil.safeParseLong(tokens[1]);
	

							logger.info("Quota Profile = " + quotaProfile);
							logger.info("LicenseKey From Cloud = " + licenseKeyFromCloud);
							
							// check local data and remote data
							if (SystemMetaData.CDP_LICENSE_KEY.equals(licenseKeyFromCloud)) {
								rs = 2;
							}

							// checksum numbeSystemMetaDatar
							long licenseNum = signedNumber(quotaProfile, SystemMetaData.DOMAIN_CDP_ADMIN, SystemMetaData.SUPER_ADMIN_EMAIL);
							String[] toks = licenseKeyFromCloud.split("_");
							if (licenseNum == StringUtil.safeParseLong(toks[2])) {
								rs++;
							}
						} else {
							System.err.println("Invalid format when parsing licenseMetaData: " + licenseInCloud);
							return 0;
						}
					}

				}
			} 
			else {
				System.err.println("licenseMetaData is empty");
			}
		}
		else if (SystemMetaData.CDP_LICENSE_KEY.equalsIgnoreCase(SystemMetaData.FREE_VERSION)) {
			rs = 1;
		}

		logger.info("------------------------------------------------");
		logger.info(" || checkCdpLicenseKey rs = " + rs + " || CDP_LICENSE_KEY: " + SystemMetaData.CDP_LICENSE_KEY);
		logger.info("------------------------------------------------");
		return rs;
	}

	/**
	 * @return
	 */
	private final static String getLicenseFromCloud() {
		String[] toks = SystemMetaData.CDP_LICENSE_KEY.split("_");
		String licenseMetaData = "";
		if (toks.length == 3) {
			String lcHash = toks[2];

			StringBuilder callUrl = new StringBuilder(SystemMetaData.LEOCDP_LICENSE_URL);
			callUrl.append(SystemMetaData.BUILD_EDITION).append("-");
			callUrl.append(SystemMetaData.DOMAIN_CDP_ADMIN).append("-").append(lcHash).append(".txt");
			logger.info(" GET_LICENSE_URL: " + callUrl);
			licenseMetaData =  HttpClientGetUtil.call(callUrl.toString()).trim();

		} else {
			System.err.println("Invalid leoCdpLicenseKey in leocdp-metadata.properties, toks.length = " + toks.length);
		}
		return licenseMetaData;
	}

	/**
	 * check to update software
	 * 
	 * @param secretKey
	 * @return true of 
	 */
	public static boolean checkUpdateSecretKey(String secretKey) {
		return StringUtil.isNotEmpty(secretKey) && SystemMetaData.UPDATE_LEO_SYSTEM_SECRET_KEY.equals(secretKey);
	}
	
	public final static long signedNumber(long quota, String adminDomain, String adminEmail) {
		String s1 = SystemMetaData.BUILD_EDITION;
		return HashUtil.hashUrl128Bit(s1 + quota + adminDomain + adminEmail);
	}

}
