package leotech.system.model;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import leotech.system.util.IdGenerator;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.database.PersistentArangoObject;
import leotech.system.version.SystemMetaData;
import rfx.core.util.FileUtils;
import rfx.core.util.HashUtil;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * the CDP License, only for USPA Technology in AWS Cloud
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class LeoCdpLicense implements PersistentArangoObject {
	
	public static final String LICENSE_KEYS_URI = "./license-keys/";

	public static final String FILE_ROOT_LICENSE_BUILD_KEY_TXT = "./ROOT_LICENSE_BUILD_KEY.txt";

	public static final String LEO_SCHEDULER_STARTER_PREFIX = "leo-scheduler-starter";

	public static final String LEO_OBSERVER_STARTER_PREFIX = "leo-observer-starter";

	public static final String LEO_MAIN_STARTER_PREFIX = "leo-main-starter";
	
	public static final String GOOGLE_CLOUD_BUCKET_LEOCDP_LICENSE = "leocdp-license";
	
	public static final String GOOGLE_CLOUD_PUBLIC_BASE_URL = "https://storage.googleapis.com/leocdp-license/";

	public static final int DEFAULT_QUOTA = 5000;

	public static final String COLLECTION_NAME = "system_leocdp_license";
	static ArangoCollection instance;

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			ArangoDatabase arangoDatabase = ArangoDbUtil.getCdpDatabase();

			instance = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields
			instance.ensurePersistentIndex(Arrays.asList("adminEmail"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("accountName"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("adminDomain"), new PersistentIndexOptions().unique(false));
		}
		return instance;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(accountName) && StringUtil.isNotEmpty(adminDomain) && createdAt != null;
	}
	
	@Key
	@Expose
	private String id;

	@Expose
	private String accountName; // bigdatavietnam

	@Expose
	private String adminDomain; // cdp.bigdatavietnam.org

	@Expose
	private String adminEmail; // cdp.bigdatavietnam.org

	@Expose
	private long quotaProfile = DEFAULT_QUOTA;

	@Expose
	private List<String> accountProfileIds = new ArrayList<String>();

	@Expose
	private String licenseHash;
	
	@Expose
	private String licenseContent = "";
	
	@Expose
	private String licenseFileUrl = "";

	@Expose
	private String versionId = SystemMetaData.BUILD_VERSION; // e.g: v_0.8.9
	
	@Expose
	private String jarMain = LEO_MAIN_STARTER_PREFIX+".jar";
	
	@Expose
	private String jarObserver = LEO_OBSERVER_STARTER_PREFIX+".jar";
	
	@Expose
	private String jarScheduler = LEO_SCHEDULER_STARTER_PREFIX+".jar";

	@Expose
	Date createdAt;

	@Expose
	Date updatedAt;

	@Expose
	Date expiredAt;

	@Expose
	Map<String, String> extConfigs = new HashMap<String, String>();
	
	public LeoCdpLicense() {
		// 
	}

	public LeoCdpLicense(String adminDomain, String adminEmail, long quotaProfile) {
		super();
		this.id = IdGenerator.createHashedId(adminDomain + adminEmail + quotaProfile);
		this.adminDomain = adminDomain;
		this.adminEmail = adminEmail;
		this.quotaProfile = quotaProfile;
		this.createdAt = new Date();
		this.updatedAt = new Date();
	}

	// https://niels.nu/blog/2016/java-rsa.html
	final static String sign(String plainText, PrivateKey privateKey) throws Exception {
		Signature privateSignature = Signature.getInstance("SHA256withRSA");
		privateSignature.initSign(privateKey);
		privateSignature.update(plainText.getBytes(StringPool.UTF_8));

		byte[] signature = privateSignature.sign();

		return Base64.getEncoder().encodeToString(signature);
	}

	public final static long signedNumber(long quota, String adminDomain, String adminEmail) {
		String s1 = SystemMetaData.BUILD_EDITION;
		return HashUtil.hashUrl128Bit(s1 + quota + adminDomain + adminEmail);
	}

	/**
	 * @param quota
	 * @param adminDomain
	 * @param adminEmail
	 * @return
	 */
	public static LeoCdpLicense buildLicenseKey(String accountName, long quotaProfile, String adminDomain, String adminEmail, int validYears) {
		String ROOT_LICENSE_BUILD_KEY;
		try {
			ROOT_LICENSE_BUILD_KEY = FileUtils.readFileAsString(FILE_ROOT_LICENSE_BUILD_KEY_TXT);
			String buildStr = SystemMetaData.BUILD_EDITION;
			long licenseNum = signedNumber(quotaProfile, adminDomain, adminEmail);
			
			String keyHint = licenseNum + ROOT_LICENSE_BUILD_KEY;
			String licenseHash = FriendlyId.toFriendlyId(UUID.nameUUIDFromBytes(keyHint.getBytes()));
			String licenseContent = buildStr + "_" + licenseNum + "_" + licenseHash;

			LeoCdpLicense cdpLicense = new LeoCdpLicense(adminDomain, adminEmail, quotaProfile);
			cdpLicense.setAccountName(accountName);
			cdpLicense.setLicenseContent(licenseContent);
			cdpLicense.setLicenseHash(licenseHash);

			// set expiration
			Calendar c = Calendar.getInstance();
			c.setTime(cdpLicense.getUpdatedAt());
			c.add(Calendar.YEAR, validYears);
			cdpLicense.setExpiredAt(c.getTime());
			
			return cdpLicense;
		} catch (Exception e) {
			System.out.println("need ROOT_LICENSE_BUILD_KEY.txt");
		}
		return null;
	}

	public String getFileName() {
		StringBuilder f = new StringBuilder();
		f.append(SystemMetaData.BUILD_EDITION).append("-");
		f.append(adminDomain).append("-");
		f.append(this.licenseHash).append(".txt");
		return f.toString();
	}
	

	public String getId() {
		return id;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getAdminDomain() {
		return adminDomain;
	}

	public void setAdminDomain(String adminDomain) {
		this.adminDomain = adminDomain;
	}

	public String getAdminEmail() {
		return adminEmail;
	}

	public void setAdminEmail(String adminEmail) {
		this.adminEmail = adminEmail;
	}

	public long getQuotaProfile() {
		return quotaProfile;
	}

	public void setQuotaProfile(long quotaProfile) {
		this.quotaProfile = quotaProfile;
	}

	public List<String> getAccountProfileIds() {
		return accountProfileIds;
	}

	public void setAccountProfileIds(List<String> accountProfileIds) {
		this.accountProfileIds = accountProfileIds;
	}

	public String getJarMain() {
		return jarMain;
	}

	public String getJarObserver() {
		return jarObserver;
	}

	public String getJarScheduler() {
		return jarScheduler;
	}

	public String getLicenseHash() {
		return licenseHash;
	}

	public void setLicenseHash(String licenseHash) {
		this.licenseHash = licenseHash;
	}

	public String getLicenseContent() {
		return licenseContent;
	}

	public void setLicenseContent(String licenseContent) {
		this.licenseContent = licenseContent;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}


	public Date getCreatedAt() {
		return createdAt;
	}
	

	public String getLicenseFileUrl() {
		return licenseFileUrl;
	}

	public void setLicenseFileUrl(String licenseFileUrl) {
		this.licenseFileUrl = licenseFileUrl;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Date getExpiredAt() {
		return expiredAt;
	}

	public void setExpiredAt(Date expiredAt) {
		this.expiredAt = expiredAt;
	}

	public Map<String, String> getExtConfigs() {
		return extConfigs;
	}

	public void setExtConfigs(Map<String, String> extConfigs) {
		this.extConfigs = extConfigs;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}

}
