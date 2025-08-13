package leotech.cdp.domain.scoring;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import leotech.system.domain.SystemConfigsManagement;
import leotech.system.model.AttributeMetaData;

/**
 * Data Quality Score Util to compute dataQualityScore in profile
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class DataQualityScoreUtil {

	static final int CACHE_SIZE = 500;
	static final long CACHE_TIME = 90;

	static CacheLoader<String, Integer> dataQualityFieldLoader = new CacheLoader<String, Integer>() {
		@Override
		public Integer load(String fieldName) {
			//System.out.println("==> queryBaseScoreByField load from database, fieldName: " + fieldName);
			return queryBaseScoreByField(fieldName);
		}
	};

	static LoadingCache<String, Integer> dataQualityFieldCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE)
			.expireAfterWrite(CACHE_TIME, TimeUnit.SECONDS).build(dataQualityFieldLoader);

	/**
	 * query from database
	 * 
	 * @param fieldName
	 * @return the base score for data quality scoring
	 */
	public static int queryBaseScoreByField(String fieldName) {
		Map<String, AttributeMetaData> fieldConfigs = SystemConfigsManagement.getProfileMetadata();
		AttributeMetaData fieldConfig = fieldConfigs.get(fieldName);
		if (fieldConfig != null) {
			return fieldConfig.getDataQualityScore();
		}
		return 0;
	}
	
	/**
	 * get from cache
	 * 
	 * @param fieldName
	 * @return the base score for data quality scoring
	 */
	public static int getBaseScoreByField(String fieldName) {
		Integer score = null;
		try {
			score = dataQualityFieldCache.get(fieldName);
		} catch (Exception e) {
			// skip
		}
		
		if (score != null) {
			//System.out.println("[found] dataQualityFieldCache is found fieldName:" + fieldName);
			return score.intValue();
		} 
		else {
			//System.err.println("[miss] dataQualityFieldCache not found fieldName:" + fieldName);
			return 0;
		}
	}
	
	public static void refreshCache(String fieldName) {
		System.out.println("DataQualityScoreUtil.refreshCache fieldName: "+ fieldName);
		dataQualityFieldCache.refresh(fieldName);
	}
	
	public static void clearAllCache() {
		System.out.println("DataQualityScoreUtil.clearAllCache ");
		dataQualityFieldCache.invalidateAll();
	}
}
