package leotech.system.version;

import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import leotech.system.util.LogUtil;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.StringUtil;

/**
 * System Meta-data about CDP instance
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class SystemMetaData {

	private static final Logger logger = LoggerFactory.getLogger(SystemMetaData.class);

	public static final int NUMBER_CORE_CPU = Runtime.getRuntime().availableProcessors();
	
	public static final String DEV = "DEV";
	public static final String FREE_VERSION = "free_for_dataism";
	public static final String DCDP = "dcdp";
	public static final String DEFAULT_INDUSTRIES = "SERVICE,COMMERCE";
	public static final String B2B = "b2b";
	public static final String LEO_CDP_LOGO_URL = "https://gcore.jsdelivr.net/gh/USPA-Technology/leo-cdp-static-files@latest/images/leo-cdp-logo.png";
	public static final String LEOCDP_ADMIN = "leocdp-admin";
	public static final String LEOCDP_LICENSE_URL = "https://storage.googleapis.com/leocdp-license/";
	public static final String LEOCDP_METADATA_DEFAULT_VALUE = "leocdp-metadata.properties";
	
	private static Map<String, String> metaDataMap = new HashMap<>();
	
	public static final String BUILD_ID;	
	static {
		LogUtil.reloadConfig("./configs/log4j.xml");
		BUILD_ID = readVersionInfoInManifest();
		initSystem();
	}
	
	
	/**
	 * set Time Zone GMT for system date time
	 */
	public final static void initTimeZoneGMT() {
		// default system time is GMT
		TimeZone timeZone = TimeZone.getTimeZone("GMT");
		TimeZone.setDefault(timeZone);
	}
	
	/**
	 * @return
	 */
	private final static String readVersionInfoInManifest() {
		SystemMetaData object = new SystemMetaData();
		Package objPackage = object.getClass().getPackage();
		
		// examine the package object
		String name = objPackage.getImplementationTitle();
		String version = objPackage.getImplementationVersion();

		// some jars may use 'Implementation Version' entries in the manifest instead
		logger.info("Package name: " + name);
		logger.info("Package version: " + version);
		
		String buildVersion = StringUtil.safeString(metaDataMap.get("buildVersion"),DCDP);
		String devVersion = buildVersion + "_DEV_" + DateTimeUtil.formatDate(new Date());
		return version != null ? version : devVersion;
	}

	private static void initSystem() {
		loadDefaultMetaDataMap(); 
		String runtimePath = metaDataMap.get("runtimePath");
		if (StringUtil.isEmpty(runtimePath)) {
			metaDataMap.put("runtimePath", Paths.get(".").toString());
		}
		logger.debug(LogUtil.toPrettyJson(metaDataMap));
	}

	private static void loadDefaultMetaDataMap() {
		Properties props = new Properties();
		try {
			String currentRelativePath = Paths.get(".").toString();
			String configPath = currentRelativePath + "/" + LEOCDP_METADATA_DEFAULT_VALUE;
			props.load(new FileInputStream(configPath));
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
		metaDataMap.putAll(Maps.newHashMap(Maps.fromProperties(props)));
	}
	
	
	public static final int getInt(String name, int defaultValue) {
		return StringUtil.safeParseInt(metaDataMap.get(name), defaultValue);
	}
	
	public static final long getLong(String name, long defaultValue) {
		return StringUtil.safeParseLong(metaDataMap.get(name), defaultValue);
	}
	
	public static final String DEFAULT_DATA_REGION = metaDataMap.getOrDefault("defaultDataRegion", "VN");
	public static final String DEFAULT_TIME_ZONE = metaDataMap.getOrDefault("defaultTimeZone", "Asia/Saigon");
	
	/**
	 * The main ArangoDB configs for non-root users
	 */
	public static final String MAIN_DATABASE_CONFIG = metaDataMap.get("mainDatabaseConfig");
	
	/**
	 * The system ArangoDB configs for root users
	 */
	public static final String SYSTEM_DATABASE_CONFIG = metaDataMap.get("systemDatabaseConfig");

	public static final String RUNTIME_ENVIRONMENT = metaDataMap.getOrDefault("runtimeEnvironment",DEV);
	public static final String MINIFY_SUFFIX = metaDataMap.getOrDefault("minifySuffix","");

	public static final String DOMAIN_CDP_ADMIN = metaDataMap.get("httpAdminDomain");
	public static final String DOMAIN_WEB_SOCKET = metaDataMap.getOrDefault("webSocketDomain","");
	
	public static final String DOMAIN_CDP_OBSERVER = metaDataMap.get("httpObserverDomain");
	public static final String DOMAIN_CHATBOT = metaDataMap.get("httpLeoBotDomain");
	public static final String DOMAIN_STATIC_CDN = metaDataMap.get("httpStaticDomain");
	public static final String DOMAIN_UPLOADED_FILES = metaDataMap.get("httpUploadedFileDomain");

	public static final String HTTP_ROUTING_CONFIG_ADMIN = metaDataMap.get("httpRoutingConfigAdmin");
	public static final String HTTP_ROUTING_CONFIG_OBSERVER = metaDataMap.get("httpRoutingConfigObserver");
	
	// format: YYYY MM DD HHmm
	public static final String BUILD_VERSION = metaDataMap.getOrDefault("buildVersion","");
	public static final String BUILD_EDITION = metaDataMap.getOrDefault("buildEdition","");
	
	public static final String DEFAULT_PERSONALIZATION_SERVICE = metaDataMap.getOrDefault("defaultPersonalizationService","");
	public static final int MAX_TOTAL_RECOMMENDED_ITEMS = getInt("maxTotalRecommendedItems", 50);
	
	public static final String INDUSTRY_DATA_MODELS_STRING = metaDataMap.getOrDefault("industryDataModels",DEFAULT_INDUSTRIES).toUpperCase();
	
	public static final String ADMIN_LOGO_URL = metaDataMap.getOrDefault("adminLogoUrl", LEO_CDP_LOGO_URL).trim();

	public static final String CURRENT_RUNTIME_PATH = metaDataMap.getOrDefault("runtimePath",".");
	public static final String SUPER_ADMIN_EMAIL = metaDataMap.getOrDefault("superAdminEmail","");

	public static final String CDP_LICENSE_KEY = metaDataMap.getOrDefault("leoCdpLicenseKey",FREE_VERSION);
	public static final String PATH_MAXMIND_DATABASE_FILE = metaDataMap.getOrDefault("pathMaxmindDatabaseFile","");
	
	public static final String PROFILE_MERGE_STRATEGY = metaDataMap.getOrDefault("profileMergeStrategy","none").trim();
	public static final String MESSAGE_QUEUE_TYPE = metaDataMap.getOrDefault("messageQueueType","local").trim();
	public static final String ENABLE_CACHING_VIEW_TEMPLATES = metaDataMap.getOrDefault("enableCachingViewTemplates","false");
	
	// kafka
	public static final String KAFKA_BOOTSTRAP_SERVER = metaDataMap.getOrDefault("kafkaBootstrapServer","").trim();
	public static final String KAFKA_TOPIC_EVENT = metaDataMap.getOrDefault("kafkaTopicEvent","").trim();
	public static final int KAFKA_TOPIC_EVENT_PARTITIONS = getInt("kafkaTopicEventPartitions", 0); 
	public static final String KAFKA_TOPIC_PROFILE = metaDataMap.getOrDefault("kafkaTopicProfile","").trim();
	public static final int KAFKA_TOPIC_PROFILE_PARTITIONS = StringUtil.safeParseInt(metaDataMap.getOrDefault("kafkaTopicProfilePartitions","0").trim());
	
	// for system upgrade when from GitHub
	public static final String UPDATE_SHELL_SCRIPT_PATH = metaDataMap.getOrDefault("updateShellScriptPath","").trim();
	public static final String STR_UPDATE_LEO_SYSTEM_SECRET_KEY = "updateLeoSystemSecretKey";
	public static final String UPDATE_LEO_SYSTEM_SECRET_KEY = metaDataMap.getOrDefault(STR_UPDATE_LEO_SYSTEM_SECRET_KEY,"").trim();
	
	public static final int NUMBER_OF_DAYS_TO_KEEP_DEAD_VISITOR = getInt("numberOfDaysToKeepDeadVisitor", 30);
	public static final int MAX_SEGMENT_SIZE_TO_RUN_IN_QUEUE = getInt("maxSegmentSizeToRunInQueue", 10000);
	public static final int BATCH_SIZE_OF_SEGMENT_DATA_EXPORT = getInt("batchSizeOfSegmentDataExport", 250);
	public static final String DATABASE_BACKUP_PATH = metaDataMap.getOrDefault("databaseBackupPath","").trim();
	
	public static final boolean HAS_B2B_FEATURES = metaDataMap.getOrDefault("hasB2Bfeatures","false").trim().equalsIgnoreCase("true");
	public static final boolean USE_LOCAL_STORAGE = metaDataMap.getOrDefault("useLocalStorage","true").trim().equalsIgnoreCase("true");
	
	// constants
	public static final String AUTOMATION = "automation";
	
	public static final Set<String> INDUSTRY_DATA_MODELS = new HashSet<String>(10);
	static {
		String[] industries = INDUSTRY_DATA_MODELS_STRING.split(",");
		for (String industry : industries) {
			INDUSTRY_DATA_MODELS.add(industry);
		}
	}
	
	public static final boolean isAutoMergeProfileData() {
		return PROFILE_MERGE_STRATEGY.equals(AUTOMATION);
	}
	
	public static final boolean isMessageQueueLocal() {
		return MESSAGE_QUEUE_TYPE.equalsIgnoreCase("local");
	}

	public static final boolean isMessageQueueRedis() {
		return MESSAGE_QUEUE_TYPE.equalsIgnoreCase("redis");
	}
	
	public static final boolean isMessageQueueKafka() {
		return MESSAGE_QUEUE_TYPE.equalsIgnoreCase("kafka");
	}
	
	public static final boolean isEnableCachingViewTemplates() {
		return ENABLE_CACHING_VIEW_TEMPLATES.equalsIgnoreCase("true");
	}


	
	/**
	 * to show or hide the Business Account menu navigation in Admin
	 * 
	 * @return
	 */
	public static boolean hasB2Bfeatures() {
		return HAS_B2B_FEATURES;
	}


	
	public final static boolean isDevMode() {
		return DEV.equalsIgnoreCase(RUNTIME_ENVIRONMENT);
	}


}
