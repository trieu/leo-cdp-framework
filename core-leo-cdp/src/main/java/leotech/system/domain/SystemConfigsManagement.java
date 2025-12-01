package leotech.system.domain;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class SystemConfigsManagement {
	
	static final int CACHE_SIZE = 200;
	static final long CACHE_TIME = 90;

	public static final String SYSTEM_INIT_CONFIG_JSON = "./resources/data-for-new-setup/init-system-services.json";
	public static final int DEFAULT_ITEM_FOR_PROFILE = 30;
	public static final String CDP_RECOMMENDER = "cdp_recommender";
	
	// for system meta data field configs
	public static final String LEO_CDP_METADATA = "leo_cdp_metadata";
	
	// for profile data field configs
	public static final String PROFILE_DATA_FIELDS = "profile_data_fields";
	
	static CacheLoader<String, SystemService> systemServicesDbLoader = new CacheLoader<String, SystemService>() {
		@Override
		public SystemService load(String id) {
			System.out.println("==> systemConfigsDbLoader load from database, id: " + id);
			return getSystemServiceById(id);
		}
	};

	static LoadingCache<String, SystemService> systemConfigsCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE)
			.expireAfterWrite(CACHE_TIME, TimeUnit.SECONDS).build(systemServicesDbLoader);
		
	public static void initDefaultSystemData(boolean resetAllConfigs) {
		try {
			initSystemConfigsFromJsonFile(resetAllConfigs);
			// init profile configs for all data field and data quality score
			saveProfileMetaData(null,null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static SystemService getSystemServiceById(String id) {
		return SystemServiceDaoUtil.getById(id);
	}

	/**
	 * @return
	 */
	public static List<SystemService> getAllSystemServices() {
		return SystemServiceDaoUtil.getAllSystemConfigs();
	}
	
	/**
	 * @return
	 */
	public static List<SystemService> getPublicSystemServices() {
		return SystemServiceDaoUtil.getPublicSystemConfigs();
	}
	
	/**
	 * @return
	 */
	public static Map<String, Map<String, Object>> getPublicSystemServiceMap() {
		List<SystemService> list =  getPublicSystemServices();
		Map<String, Map<String, Object>> map = new HashMap<String, Map<String,Object>>();
		for (SystemService config : list) {
			map.put(config.getId(), config.getConfigs());
		}
		return map;
	}
	
	
	/**
	 * @return
	 */
	public static  Map<String, Object> getLeoRecommenderConfigs() {
		SystemService cf;
		try {
			cf = systemConfigsCache.get(CDP_RECOMMENDER);
			System.out.println(cf);
			return cf.getConfigs();
		} catch (ExecutionException e) {
			//skip
		}
		return new HashMap<>(0);
	}
	
	/**
	 * @return
	 */
	public static Map<String,AttributeMetaData> getProfileMetadata() {
		SystemService cf;
		try {
			cf = systemConfigsCache.get(PROFILE_DATA_FIELDS);
			if(cf != null) {
				Map<String,AttributeMetaData> fieldConfigs = new HashMap<String, AttributeMetaData>(cf.getCoreFieldConfigs());
				//put ext fields
				fieldConfigs.putAll(cf.getExtFieldConfigs());
				return fieldConfigs;
			}
		} catch (ExecutionException e) {
			//skip
		}		
		return new HashMap<>(0);
	}
	
	/**
	 * LEO_CDP_METADATA for leocdp-metadata.properties, so we can replace it in the future
	 * 
	 * @return SystemService for LEO_CDP_METADATA
	 */
	public final static SystemService loadLeoCdpMetadata() {
		SystemService cf = null;
		try {
			cf = systemConfigsCache.get(LEO_CDP_METADATA);
			if(cf != null) {
				// return from database
				if( cf.getConfigs().isEmpty()) {
					cf.getConfigs().putAll(SystemMetaData.getMetaDataMap());
				}
				return cf;
			}	
		} catch (Exception e) {
			//skip
		}	
		if(cf == null) {
			// create a new and save into database
			Map<String,Object> metadata = new HashMap<String, Object>();
			cf = new SystemService(LEO_CDP_METADATA, "LEO CDP METADATA", metadata );
			if( cf.getConfigs().isEmpty()) {
				cf.getConfigs().putAll(SystemMetaData.getMetaDataMap());
			}
			systemConfigsCache.put(LEO_CDP_METADATA, cf);
			SystemServiceDaoUtil.save(cf);
		}
		return cf;
	}
	
	/**
	 * @param extAttributes
	 */
	public static String saveProfileMetaData(Map<String, AttributeMetaData> profileDataFields, Map<String,AttributeMetaData> extAttributes) {
		if(profileDataFields == null) {
			profileDataFields = ProfileModelUtil.initAndLoadProfileMetaData();	
		}
		// 
		SystemService cf = new SystemService(PROFILE_DATA_FIELDS, PROFILE_DATA_FIELDS, profileDataFields, extAttributes);
		cf.setIndex(-3);//not show in the list
		
		// save database
		String savedId = saveSystemConfig(cf);
		System.out.println("setAllDataFieldConfigsForProfileModel savedId: " + savedId);
		
		// refresh cache after save data into database
		if(cf.getId().equals(savedId)) {
			profileDataFields.keySet().forEach(fieldName->{
				DataQualityScoreUtil.refreshCache(fieldName);
			});
			if(extAttributes != null) {
				extAttributes.keySet().forEach(fieldName->{
					DataQualityScoreUtil.refreshCache(fieldName);
				});	
			}
		}
		return savedId;
	}
	
	/**
	 * @return
	 */
	public static String getPublicSystemConfigMapAsJson() {
		return new Gson().toJson(getPublicSystemServiceMap());
	}
	
	/**
	 * @param jsonFileUri
	 * @throws Exception
	 */
	static void initSystemConfigsFromJsonFile(boolean resetAllConfigs) throws Exception {
		// remove 
		SystemServiceDaoUtil.removeAllSystemConfigs();
		
		Type listType = new TypeToken<ArrayList<SystemService>>() {}.getType();
		String jsonStr = FileUtils.readFileAsString(SYSTEM_INIT_CONFIG_JSON);
		List<SystemService> configs = new Gson().fromJson(jsonStr, listType);
		for (SystemService config : configs) {
			// save
			if(resetAllConfigs) {
				SystemServiceDaoUtil.save(config);
			}
			else {
				SystemServiceDaoUtil.insert(config);
			}
		}
	}
	
	/**
	 * @param config
	 * @return
	 */
	public static String saveSystemConfig(SystemService config) {
		String id = SystemServiceDaoUtil.save(config);
		if(id != null) {
			systemConfigsCache.invalidate(id);
			if(id.equals(LEO_CDP_METADATA)) {
				
			}
		}
		return id;
	}
	
}
