package leotech.system.domain;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import leotech.cdp.domain.scoring.DataQualityScoreUtil;
import leotech.cdp.model.customer.ProfileModelUtil;
import leotech.system.dao.SystemServiceDaoUtil;
import leotech.system.model.AttributeMetaData;
import leotech.system.model.SystemService;
import leotech.system.version.SystemMetaData;
import rfx.core.util.FileUtils;

/**
 *  SystemConfigsManagement
 * 
 * @author tantrieuf31
 * @since 2021
 */
public final class SystemConfigsManagement {

	private static final Logger logger = LoggerFactory.getLogger(SystemConfigsManagement.class);

	static final int CACHE_SIZE = 200;
	static final long CACHE_TIME = 15;

	public static final String SYSTEM_INIT_CONFIG_JSON = "./resources/data-for-new-setup/init-system-services.json";
	public static final int DEFAULT_ITEM_FOR_PROFILE = 30;
	public static final String CDP_RECOMMENDER = "cdp_recommender";
	public static final String LEO_CDP_METADATA = "leo_cdp_metadata";
	public static final String PROFILE_DATA_FIELDS = "profile_data_fields";

	private static final CacheLoader<String, SystemService> systemServicesDbLoader = new CacheLoader<String, SystemService>() {
		@Override
		public SystemService load(String id) {
			logger.debug("Loading SystemService from database for ID: {}", id);
			return getSystemServiceById(id);
		}
	};

	private static final LoadingCache<String, SystemService> systemConfigsCache = CacheBuilder.newBuilder()
			.maximumSize(CACHE_SIZE).expireAfterWrite(CACHE_TIME, TimeUnit.SECONDS).build(systemServicesDbLoader);

	public static void initDefaultSystemData(boolean resetAllConfigs) {
		logger.info("Initializing default system data. Reset all configs: {}", resetAllConfigs);
		try {
			initSystemConfigsFromJsonFile(resetAllConfigs);
			saveProfileMetaData(null, null);
		} catch (Exception e) {
			logger.error("Failed to initialize default system data", e);
		}
	}

	public static SystemService getSystemServiceById(String id) {
		try {
			logger.debug("Fetching SystemService by ID: {}", id);
			return SystemServiceDaoUtil.getById(id);
		} catch (Exception e) {
			logger.error("Error retrieving SystemService for ID: {}", id, e);
			return null;
		}
	}

	public static List<SystemService> getAllSystemServices() {
		logger.debug("Fetching all system services");
		return SystemServiceDaoUtil.getAllSystemConfigs();
	}

	public static List<SystemService> getPublicSystemServices() {
		logger.debug("Fetching public system services");
		return SystemServiceDaoUtil.getPublicSystemConfigs();
	}

	public static Map<String, Map<String, Object>> getPublicSystemServiceMap() {
		List<SystemService> list = getPublicSystemServices();
		Map<String, Map<String, Object>> map = new HashMap<>();
		for (SystemService config : list) {
			map.put(config.getId(), config.getConfigs());
		}
		return map;
	}

	public static Map<String, Object> getLeoRecommenderConfigs() {
		try {
			SystemService cf = systemConfigsCache.get(CDP_RECOMMENDER);
			logger.debug("Retrieved LeoRecommenderConfigs: {}", cf);
			return cf.getConfigs();
		} catch (ExecutionException e) {
			logger.warn("Could not retrieve LeoRecommenderConfigs from cache: {}", e.getMessage());
			logger.debug("Stacktrace: ", e);
		}
		return new HashMap<>(0);
	}

	public static Map<String, AttributeMetaData> getProfileMetadata() {
		try {
			SystemService cf = systemConfigsCache.get(PROFILE_DATA_FIELDS);
			if (cf != null) {
				Map<String, AttributeMetaData> fieldConfigs = new HashMap<>(cf.getCoreFieldConfigs());
				fieldConfigs.putAll(cf.getExtFieldConfigs());
				return fieldConfigs;
			}
		} catch (ExecutionException e) {
			logger.warn("Could not retrieve ProfileMetadata from cache: {}", e.getMessage());
		}
		return new HashMap<>(0);
	}

	public static SystemService loadLeoCdpMetadata() {
		SystemService cf = null;
		try {
			cf = systemConfigsCache.get(LEO_CDP_METADATA);
			if (cf != null && cf.getConfigs().isEmpty()) {
				logger.info("Leo CDP Metadata found in DB but empty, populating from SystemMetaData");
				cf.getConfigs().putAll(SystemMetaData.getMetaDataMap());
			}
		} catch (Exception e) {
			logger.debug("LeoCdpMetadata not found in cache/DB, will create new. Error: {}", e.getMessage());
		}

		if (cf == null) {
			logger.info("Creating new SystemService for LEO_CDP_METADATA");
			Map<String, Object> metadata = new HashMap<>(SystemMetaData.getMetaDataMap());
			cf = new SystemService(LEO_CDP_METADATA, "LEO CDP METADATA", metadata);
			systemConfigsCache.put(LEO_CDP_METADATA, cf);
			SystemServiceDaoUtil.save(cf);
		}
		return cf;
	}

	public static String saveProfileMetaData(Map<String, AttributeMetaData> profileDataFields,
			Map<String, AttributeMetaData> extAttributes) {
		if (profileDataFields == null) {
			logger.debug("No profile data fields provided, loading defaults.");
			profileDataFields = ProfileModelUtil.initAndLoadProfileMetaData();
		}

		SystemService cf = new SystemService(PROFILE_DATA_FIELDS, PROFILE_DATA_FIELDS, profileDataFields,
				extAttributes);
		cf.setIndex(-3); // Hidden from UI lists

		String savedId = saveSystemConfig(cf);
		logger.info("Profile MetaData saved with ID: {}", savedId);

		if (PROFILE_DATA_FIELDS.equals(savedId)) {
			profileDataFields.keySet().forEach(DataQualityScoreUtil::refreshCache);
			if (extAttributes != null) {
				extAttributes.keySet().forEach(DataQualityScoreUtil::refreshCache);
			}
			logger.debug("DataQualityScore cache refreshed for all profile fields.");
		}
		return savedId;
	}

	public static String getPublicSystemConfigMapAsJson() {
		return new Gson().toJson(getPublicSystemServiceMap());
	}

	static void initSystemConfigsFromJsonFile(boolean resetAllConfigs) throws Exception {
		logger.info("Importing system configs from JSON file: {}", SYSTEM_INIT_CONFIG_JSON);

		// Caution: This removes all existing configs
		SystemServiceDaoUtil.removeAllSystemConfigs();

		Type listType = new TypeToken<ArrayList<SystemService>>() {
		}.getType();
		String jsonStr = FileUtils.readFileAsString(SYSTEM_INIT_CONFIG_JSON);
		List<SystemService> configs = new Gson().fromJson(jsonStr, listType);

		if (configs != null) {
			for (SystemService config : configs) {
				if (resetAllConfigs) {
					SystemServiceDaoUtil.save(config);
				} else {
					SystemServiceDaoUtil.insert(config);
				}
			}
			logger.info("Successfully imported {} system configurations.", configs.size());
		}
	}

	public static String saveSystemConfig(SystemService config) {
		if (config == null) {
			logger.error("Attempted to save a null SystemService config.");
			return null;
		}

		String id = SystemServiceDaoUtil.save(config);
		if (id != null) {
			logger.debug("Invalidating cache for SystemService ID: {}", id);
			systemConfigsCache.invalidate(id);
		} else {
			logger.error("Failed to save SystemService: {}", config.getId());
		}
		return id;
	}
}