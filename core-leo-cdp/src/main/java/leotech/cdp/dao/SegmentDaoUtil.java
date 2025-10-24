package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.google.common.collect.Sets;

import leotech.cdp.model.RefKey;
import leotech.cdp.model.activation.ActivationRule;
import leotech.cdp.model.activation.Agent;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.customer.Segment.SegmentRef;
import leotech.cdp.query.SegmentQuery;
import leotech.cdp.query.filters.SegmentFilter;
import leotech.system.config.AqlTemplate;
import leotech.system.exception.InvalidDataException;
import leotech.system.util.TaskRunner;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * Segment Data Access Object
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class SegmentDaoUtil extends AbstractCdpDatabaseUtil {
	
	static Logger logger = LoggerFactory.getLogger(SegmentDaoUtil.class);
	
	static final String AQL_COUNT_TOTAL_ACTIVE_SEGMENTS = "RETURN LENGTH(FOR s in " + Segment.COLLECTION_NAME+" FILTER s.status >= 0 RETURN s._key)";
	
	static final String AQL_GET_SEGMENTS_BY_PAGINATION = AqlTemplate.get("AQL_GET_SEGMENTS_BY_PAGINATION");
	static final String AQL_GET_SEGMENTS_TO_REFRESH = AqlTemplate.get("AQL_GET_SEGMENTS_TO_REFRESH");
	
	
	static final String AQL_COUNT_SEGMENT_FOR_PAGINATION = AqlTemplate.get("AQL_COUNT_SEGMENT_FOR_PAGINATION");
	static final String AQL_GET_ALL_SEGMENTS_BY_AUTHORIZATION = AqlTemplate.get("AQL_GET_ALL_SEGMENTS_BY_AUTHORIZATION");
	static final String AQL_GET_ALL_SEGMENT_REFS_BY_AUTHORIZATION = AqlTemplate.get("AQL_GET_ALL_SEGMENT_REFS_BY_AUTHORIZATION");
	
	static final String AQL_GET_ALL_SEGMENTS_BY_FILTERING = AqlTemplate.get("AQL_GET_ALL_SEGMENTS_BY_FILTERING");
	static final String AQL_GET_ALL_SEGMENTS = AqlTemplate.get("AQL_GET_ALL_SEGMENTS");
	static final String AQL_GET_ALL_ACTIVE_SEGMENTS = AqlTemplate.get("AQL_GET_ALL_ACTIVE_SEGMENTS");
	
	static final String AQL_GET_SEGMENT_REFKEYS_BY_IDS = AqlTemplate.get("AQL_GET_SEGMENT_REFKEYS_BY_IDS");
	
	static final String AQL_GET_SEGMENT_BY_ID = AqlTemplate.get("AQL_GET_SEGMENT_BY_ID");
	static final String AQL_GET_SEGMENTS_TO_DELETE_FOREVER = AqlTemplate.get("AQL_GET_SEGMENTS_TO_DELETE_FOREVER");

	static final String AQL_GET_SEGMENTS_FOR_RECOMMENDER = AqlTemplate.get("AQL_GET_SEGMENTS_FOR_RECOMMENDER");
	static final String AQL_GET_MAX_INDEX_SCORE_IN_SEGMENTS = AqlTemplate.get("AQL_GET_MAX_INDEX_SCORE_IN_SEGMENTS");
	
	static final String AQL_UPDATE_SEGMENT_TOTAL_COUNT = AqlTemplate.get("AQL_UPDATE_SEGMENT_TOTAL_COUNT");
	
	static final String AQL_REMOVE_VIEWABLE_SEGMENTS_FOR_USER = AqlTemplate.get("AQL_REMOVE_VIEWABLE_SEGMENTS_FOR_USER");
	static final String AQL_REMOVE_EDITABLE_SEGMENTS_FOR_USER = AqlTemplate.get("AQL_REMOVE_EDITABLE_SEGMENTS_FOR_USER");
	
	static final int MIN_MINUTES_TO_UPDATE_PROFILES = 5; // minutes 
	
	public final static long getSegmentSizeById(String segmentId) {
		Segment segment = getSegmentById(segmentId);
		return getSegmentSizeByQuery(segment.buildQuery());
	}
	
	/**
	 * @param segment
	 * @return
	 */
	public final static long getSegmentSizeByQuery(Segment segment) {
		return getSegmentSizeByQuery(segment.buildQuery());
	}
	
	/**
	 * @param profileQuery
	 * @return
	 */
	public final static long getSegmentSizeByQuery(SegmentQuery profileQuery) {
		Long c = null;
		try {
			c = getSegmentSizeByQueryAsync(profileQuery).get();
		} catch (Exception e) {}
		if(c == null) {
			return 0L;
		}
		return c.longValue();
	}
	
	/**
	 * @param profileQuery
	 * @return
	 */
	public static CompletableFuture<Long> getSegmentSizeByQueryAsync(SegmentQuery query) {
        return CompletableFuture.supplyAsync(() -> {
            ArangoDatabase db = getCdpDatabase();
            String aql = query.getCountingQueryWithDateTimeFilter();
            
            if(SystemMetaData.isDevMode()) {
            	logger.info(" ==> getSegmentSizeByQuery " + aql);
            }
            
            Long c = new ArangoDbCommand<Long>(db, aql, Long.class).getSingleResult();
            if (c == null) {
                return 0L;
            }
            return c.longValue();
        });
    }
	
	/**
	 * @param profileId
	 * @param query
	 * @return
	 */
	public static boolean isProfileInSegment(String profileId, SegmentQuery query) {
		 ArangoDatabase db = getCdpDatabase();
         String aql = query.getQueryToCheckProfile(profileId);
         
         if(StringUtil.isEmpty(profileId) || StringUtil.isEmpty(aql)) {
        	 return false;
         }
         if(SystemMetaData.isDevMode()) {
        	 logger.info(" ==> checkProfileInSegment " + aql);
         }
         
         Integer c = new ArangoDbCommand<>(db, aql, Integer.class).getSingleResult();
         if (c == null) {
             return false;
         }
         return c > 0;
    }
	
	/**
	 * @param filter
	 * @return
	 */
	public static List<Segment> loadSegmentsByFilter(SegmentFilter filter){
		List<Segment> list = runFilterQuery(filter);
		return list;
	}
	
	/**
	 * check All Segments To Update Profile Count
	 * @return number of checked segment
	 */
	public static int refreshAllSegmentsAndUpdateProfiles() {
		ArangoDbCommand<Segment> q = new ArangoDbCommand<>(getCdpDatabase(), AQL_GET_ALL_SEGMENTS, Segment.class);
		List<Segment> segments = q.getResultsAsList();
		
		TaskRunner.run(()->{
			segments.stream().forEach(segment->{
				long diff = segment.getMinutesSinceLastUpdate();
				if(diff > 5) {
					long newSize = getSegmentSizeByQuery(segment);
					if(segment.getTotalCount() != newSize) {
						// update the latest count
						saveSegmentSize(segment.getId(), newSize);
					}
				}
			});
		});
		return segments.size();
	}
	
	/**
	 * save the new size of segment
	 * 
	 * @param segmentId
	 * @param totalCount
	 */
	public static void saveSegmentSize(String segmentId, long newSize) {
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("segmentId", segmentId);
		bindVars.put("totalCount", newSize);
		bindVars.put("currentDateTime", new Date());
		
		logger.info("updateSegmentSize segmentId: " + segmentId + " newSize: " + newSize);
		new ArangoDbCommand<Segment>(getCdpDatabase(), AQL_UPDATE_SEGMENT_TOTAL_COUNT, bindVars).update();
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static List<Segment> runFilterQuery(SegmentFilter filter) {
		ArangoDatabase db = getCdpDatabase();
		
		// dynamic query builder for filtering data
		String aql;
		
		Map<String, Object> bindVars = new HashMap<>(10);
		if(filter.isFilteringAndGetAll()) {
			aql = AQL_GET_ALL_SEGMENTS_BY_FILTERING;
			bindVars.put("forDeepAnalytics", filter.isForDeepAnalytics());
			bindVars.put("forPredictiveAnalytics", filter.isForPredictiveAnalytics());
			bindVars.put("forPersonalization", filter.isForPersonalization());
			bindVars.put("forEmailMarketing", filter.isForEmailMarketing());
			bindVars.put("forRealtimeMarketing", filter.isForRealtimeMarketing());
			bindVars.put("forReTargeting", filter.isForReTargeting());
			bindVars.put("forLookalikeTargeting", filter.isForLookalikeTargeting());
		} else {
			aql = AQL_GET_SEGMENTS_BY_PAGINATION;
			bindVars.put("startIndex", filter.getStart());
			bindVars.put("numberResult", filter.getLength());
			bindVars.put("sortField", filter.getSortField("indexScore"));
			bindVars.put("sortDirection", filter.getSortDirection());
			bindVars.put("searchValue", filter.getFormatedSearchValue());
			
			// check permission
			bindVars.put("hasAdminRole", filter.hasAdminRole());
			bindVars.put("loginUsername", filter.getLoginUsername());
			
			// filter by authorization
			bindVars.put("authorizedViewer", filter.getAuthorizedViewer());
			bindVars.put("authorizedEditor", filter.getAuthorizedEditor());
		}
		ArangoDbCommand<Segment> q = new ArangoDbCommand<Segment>(db,aql ,bindVars, Segment.class);
		List<Segment> list = q.getResultsAsList();
		return list;
	}
	
	/**
	 * get All Segments By Authorization
	 * 
	 * @param filter
	 * @return
	 */
	public static List<Segment> getAllSegmentsByAuthorization(SegmentFilter filter) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("authorizedViewer", filter.getAuthorizedViewer());
		bindVars.put("authorizedEditor", filter.getAuthorizedEditor());
		ArangoDbCommand<Segment> q = new ArangoDbCommand<Segment>(db, AQL_GET_ALL_SEGMENTS_BY_AUTHORIZATION , bindVars, Segment.class);
		List<Segment> list = q.getResultsAsList();
		return list;
	}
	
	/**
	 * to get all SegmentRef
	 * 
	 * @param filter
	 * @return all SegmentRef with (id, name, totalCount)
	 */
	public static List<SegmentRef> getAllSegmentRefs(SegmentFilter filter) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("authorizedViewer", filter.getAuthorizedViewer());
		bindVars.put("authorizedEditor", filter.getAuthorizedEditor());
		ArangoDbCommand<SegmentRef> q = new ArangoDbCommand<SegmentRef>(db, AQL_GET_ALL_SEGMENT_REFS_BY_AUTHORIZATION , bindVars, SegmentRef.class);
		List<SegmentRef> list = q.getResultsAsList();
		return list;
	}
	
	/**
	 * @param idList
	 * @returngetSegmentById
	 */
	public static Set<RefKey> getRefKeysByIds(List<String> idList) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("idList", idList);
		Set<RefKey> set = new ArangoDbCommand<>(db, AQL_GET_SEGMENT_REFKEYS_BY_IDS, bindVars, RefKey.class).getResultsAsSet();
		return set;
	}
	
	/**
	 * @param segment
	 * @return id of segment
	 */
	public static String createSegment(Segment segment) {
		if (segment.dataValidation() ) {
			ArangoCollection col = segment.getDbCollection();
			if (col != null) {
				
				// update total size of segment
				// set total size of segment
				long size = getSegmentSizeByQuery(segment);
				segment.setTotalCount(size);
				col.insertDocument(segment);
				
				// update segment ref
				TaskRunner.runInThreadPools(()->{
					ProfileDaoUtil.setSegmentRefForMatchedProfiles(segment);
					// TODO update cache all profiles into REDIS
				});
				return segment.getId();
			}
		}
		return null;
	}



	/**
	 * update segment and set segmentIds for all matched profiles
	 * 
	 * @param segment
	 * @return
	 */
	public static String updateSegment(Segment segment) {
		return updateSegment(segment, false, true);
	}
	
	/**
	 * Just update segment, NO refresh inSegments for all matched profiles
	 * 
	 * @param segment
	 * @return
	 */
	public static String updateSegmentMetadata(Segment segment) {
		return updateSegment(segment, false, false);
	}


	/**
	 * update with option to set segment status as removed
	 * 
	 * @param segment
	 * @param isDeletingSegment
	 * @return
	 */
	public static String updateSegment(Segment segment, boolean isDeletingSegment, boolean refreshAllMatchedProfiles) {
		if(segment != null) {
			ArangoCollection col = segment.getDbCollection();
			if (segment.dataValidation()) {
				String segmentId = segment.getId();
				
				if(isDeletingSegment) {
					segment.setStatus(Segment.STATUS_DELETED);
					segment.setName("[DELETED] " + segment.getName());
					segment.setTotalCount(0);
					
					// remove all segment ref keys for all matched profiles
					TaskRunner.runInThreadPools(()->{
						HashSet<String> removeSegmentId = Sets.newHashSet(segmentId);
						Set<String> profileIdSet = ProfileDaoUtil.getAllProfileIdsBySegmentId(segmentId);
						profileIdSet.parallelStream().forEach(profileId->{
							ProfileDaoUtil.removeSegmentRefKeyInProfile(profileId, removeSegmentId);
						});
					});
				} else {
					long newSize = getSegmentSizeByQuery(segment);
					segment.setTotalCount(newSize);
				}
				
				// commit to ArangoDB
				segment.setUpdatedAt(new Date()); // update time
				col.updateDocument(segmentId, segment, getUpdateOptions());
				
				// refresh inSegments for all matched profiles
				if(refreshAllMatchedProfiles) {
					TaskRunner.runJob(()->{
						ProfileDaoUtil.setSegmentRefForMatchedProfiles(segment);
					});
				}
				
				return segmentId;
			}
		}
		return null;
	}
	
	/**
	 * @param segmentId
	 * @return
	 */
	public static boolean refreshSegmentSizeAndProfileRefs(String segmentId) {
		Segment segment = getSegmentById(segmentId);
		if(segment != null) {
			refreshSegmentSizeAndProfileRefs(segment);
			return true;
		}
		throw new InvalidDataException("Not found any segment with id: " + segmentId);
	}
	
	/**
	 * @param segment
	 */
	public static void refreshSegmentSizeAndProfileRefs(Segment segment) {
		boolean refreshAllProfiles = segment.shouldUpdateSize(MIN_MINUTES_TO_UPDATE_PROFILES) ;
		if(refreshAllProfiles) {
			// update total size of segment
			long newSize = getSegmentSizeByQuery(segment);
			segment.setTotalCount(newSize);
			saveSegmentSize(segment.getId(), newSize);
			
			// update segment ref of all matched profiles
			TaskRunner.runJob(()->{
				ProfileDaoUtil.setSegmentRefForMatchedProfiles(segment);
				// TODO update cache all profiles into REDIS
			});
		}
		else {
			long lastUpdate = segment.getMinutesSinceLastUpdate();
			logger.info("Skip refreshSegmentSizeAndProfileRefs getNumberOfMinutesSinceLastUpdate = " + lastUpdate);
		}
	}
	
	
	/**
	 * @param id
	 * @return
	 */
	public static Segment getSegmentById(String id) {
		return getSegmentById(id, false);
	}


	/**
	 * @param id
	 * @param fullDataLoad
	 * @return
	 */
	public static Segment getSegmentById(String id, boolean fullDataLoad) {
		if(StringUtil.isEmpty(id)) {
			return null;
		}
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		Segment segment = new ArangoDbCommand<Segment>(db, AQL_GET_SEGMENT_BY_ID, bindVars, Segment.class).getSingleResult();
		if(segment != null && fullDataLoad) {
			// load all activation rules
			List<ActivationRule> activationRules = ActivationRuleDao.getAllActivationRulesForSegment(id);
			// collect all rules
			activationRules.forEach( rule -> {					
				// template
				String assetTemplateId = rule.getAssetTemplateId();
				if(StringUtil.isNotEmpty(assetTemplateId)) {
					AssetTemplate tpl = AssetTemplateDaoUtil.getById(assetTemplateId);
					if(tpl != null) {
						rule.setAssetTemplateName(tpl.getActivationName());
					}
				}
				String dataServiceId = rule.getDataServiceId();
				if(StringUtil.isNotEmpty(dataServiceId)) {
					Agent c = AgentDaoUtil.getById(dataServiceId);
					if(c != null) {
						rule.setDataServiceName(c.getName());
					}
				}
			});
			// set data for render in the UI data table
			segment.setActivationRules(activationRules);
		}
		return segment;
	}
	
	/**
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Segment> loadSegmentsToRefresh(int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(6);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);

		List<Segment> list = new ArangoDbCommand<Segment>(db, AQL_GET_SEGMENTS_TO_REFRESH, bindVars, Segment.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @return
	 */
	public static List<Segment> getAllSegmentsForRecommender() {
		ArangoDatabase db = getCdpDatabase();
		List<Segment> list = new ArangoDbCommand<Segment>(db, AQL_GET_SEGMENTS_FOR_RECOMMENDER, Segment.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @return List<Segment>
	 */
	public static List<Segment> getAllActiveSegments() {
		ArangoDatabase db = getCdpDatabase();
		List<Segment> list = new ArangoDbCommand<Segment>(db, AQL_GET_ALL_ACTIVE_SEGMENTS, Segment.class).getResultsAsList();
		return list;
	}
	
	
	
	/**
	 * @return
	 */
	public static List<Segment> getAllRemovedSegmentsToDeleteForever() {
		ArangoDatabase db = getCdpDatabase();
		List<Segment> list = new ArangoDbCommand<Segment>(db, AQL_GET_SEGMENTS_TO_DELETE_FOREVER, Segment.class).getResultsAsList();
		return list;
	}
	
	/**
	 * delete a segment in database
	 * 
	 * @param segment
	 * @return
	 */
	public static boolean deleteSegment(Segment segment, boolean deleteSegmentRefKeyInProfile) {
		ArangoCollection col = segment.getDbCollection();
		if (col != null) {
			String segmentId = segment.getId();
			
			// delete all RefKey of profile
			if(deleteSegmentRefKeyInProfile) {
				TaskRunner.run(()->{
					Set<String> profileIdSet = ProfileDaoUtil.getAllProfileIdsBySegmentId(segmentId);
					for (String profileId : profileIdSet) {
						ProfileDaoUtil.removeSegmentRefKeyInProfile(profileId, Sets.newHashSet(segmentId));
					}
				});
			}
			String deletedKey = col.deleteDocument(segmentId).getKey();
			return segmentId.equals(deletedKey);
		}
		return false;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static long getTotalRecordsFiltered(SegmentFilter filter) {
		String aql = AQL_COUNT_SEGMENT_FOR_PAGINATION;
		
		Map<String, Object> bindVars = new HashMap<>(5);
		bindVars.put("searchValue", filter.getFormatedSearchValue());
		bindVars.put("hasAdminRole", filter.hasAdminRole());
		bindVars.put("loginUsername", filter.getLoginUsername());
		bindVars.put("authorizedViewer", filter.getAuthorizedViewer());
		bindVars.put("authorizedEditor", filter.getAuthorizedEditor());
		
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, aql, bindVars, Long.class).getSingleResult();
		return c;
	}
	
	/**
	 * @return
	 */
	public static int countTotalSegments() {
		ArangoDatabase db = getCdpDatabase();
		return new ArangoDbCommand<>(db, AQL_COUNT_TOTAL_ACTIVE_SEGMENTS, Integer.class).getSingleResult();
	}
	
	
	/**
	 * @return
	 */
	public static int getMaxIndexScoreInSegments() {
		ArangoDatabase db = getCdpDatabase();
		Integer c =  new ArangoDbCommand<Integer>(db, AQL_GET_MAX_INDEX_SCORE_IN_SEGMENTS, Integer.class).getSingleResult();
		return c != null ? c.intValue() : 0;
	}
	
	/**
	 * @param userLogin
	 */
	public static void removeAllViewableSegments(String userLogin) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("userLogin", userLogin);
		new ArangoDbCommand<>(db, AQL_REMOVE_VIEWABLE_SEGMENTS_FOR_USER, bindVars).update();
	}
	
	/**
	 * @param userLogin
	 */
	public static void removeAllEditableSegments(String userLogin) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("userLogin", userLogin);
		new ArangoDbCommand<>(db, AQL_REMOVE_EDITABLE_SEGMENTS_FOR_USER, bindVars).update();
	}
	

}
