package leotech.cdp.domain;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.model.RefKey;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.customer.Segment.SegmentRef;
import leotech.cdp.query.SegmentQuery;
import leotech.cdp.query.filters.SegmentFilter;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import leotech.system.util.JsonFileImporter;
import leotech.system.util.LogUtil;
import leotech.system.util.TaskRunner;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * Segment Data Management
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class SegmentDataManagement {
	
	private static Logger logger = LoggerFactory.getLogger(SegmentDataManagement.class);
	final static int SLEEP_AFTER_REFRESH_SEGMENT = 10000;
	final private static int CACHE_TIME = 30;
	
	private static final String SEGMENTATION_INIT_CONFIG_JSON = "./resources/data-for-new-setup/init-segment-list.json";
	/**clearAllCacheSegments
	 * init for new setup 
	 */
	public static void initDefaultSystemData() {
		try {
			String jsonFileUri = SEGMENTATION_INIT_CONFIG_JSON;
			int s = createSegmentFromJsonFile(jsonFileUri);
			logger.info("createSegmentFromJsonFile with the number of segment: "+s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param jsonFileUri
	 * @throws Exception
	 */
	static int createSegmentFromJsonFile(String jsonFileUri) throws Exception {
		JsonFileImporter<Segment> importer = new JsonFileImporter<Segment>(jsonFileUri, Segment[].class);
		List<Segment> segments = importer.getDataAsList();
		
		String rootUsername = SystemUser.SUPER_ADMIN_LOGIN;
		for (Segment segment : segments) {
			segment.setTotalCount(0);
			segment.setOwnerUsername(rootUsername);
			segment.setCreatedAt(new Date());
			segment.setUpdatedAt(new Date());
			segment.buildHashedId();
			// save
			Segment dbSegment = SegmentDataManagement.getSegmentById(segment.getId());
			if (dbSegment == null) {
				createSegment(segment);
			} else {
				logger.error("In database, these is a duplicated segment with name: " + segment.getName());
			}
		}
		return segments.size();
	}
	
	// ------- BEGIN Cache Profile in Segment

	
	static CacheLoader<SegmentFilter, JsonDataTablePayload> cacheLoaderSegments = new CacheLoader<>() {
		@Override
		public JsonDataTablePayload load(SegmentFilter filter) {
			logger.info("MISS CACHE cacheSegments");
			return querySegmentsByFilter(filter);
		}
	};

	static final LoadingCache<SegmentFilter, JsonDataTablePayload> cacheSegments = CacheBuilder.newBuilder().maximumSize(20000)
			.expireAfterWrite(CACHE_TIME, TimeUnit.SECONDS).build(cacheLoaderSegments);
	
	public static void refreshCacheSegments(SegmentFilter filter) {
		cacheSegments.refresh(filter);
	}
	
	public static void clearAllCacheSegments() {
		// FIXME so stupid to clear all, think better solution
		cacheSegments.invalidateAll();
	}
	
	
	static CacheLoader<String, Set<RefKey> > cacheSegmentRefKeyLoader = new CacheLoader<>() {
		@Override
		public Set<RefKey> load(String idsAsStr) {
			List<String> ids = Arrays.asList(idsAsStr.split(StringPool.UNDERLINE));
			return SegmentDaoUtil.getRefKeysByIds(ids);
		}
	};
	static final LoadingCache<String, Set<RefKey>> cacheSegmentRefKey = CacheBuilder.newBuilder()
			.maximumSize(10000).expireAfterWrite(5, TimeUnit.MINUTES).build(cacheSegmentRefKeyLoader);
	
	// ------ END Cache Profile in Segment
	
	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload loadSegmentsByFilter(SegmentFilter filter) {
		JsonDataTablePayload r = null;
		try {
			r = cacheSegments.get(filter);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(r == null) {
			r = querySegmentsByFilter(filter);
			cacheSegments.put(filter, r);
		}
		return r;
	}
	
	/**
	 * @param segmentId
	 * @return
	 */
	public static boolean refreshSegmentData(String segmentId) {
		boolean ok = SegmentDaoUtil.refreshSegmentSizeAndProfileRefs(segmentId);
		if(ok) {
			// TODO update cache
		}
		return ok;
	}
	

	/**
	 * @return
	 */
	public static int refreshAllSegmentRefs() {
		int startIndex = 0;
		int numberResults = 2;
		List<Segment> list = SegmentDaoUtil.loadSegmentsToRefresh(startIndex, numberResults);
		int size = list.size();
		while (size > 0) {
			list.stream().forEach(segment->{
				// refresh segments
				SegmentDaoUtil.refreshSegmentSizeAndProfileRefs(segment);
				Utils.sleep(SLEEP_AFTER_REFRESH_SEGMENT);
			});
			startIndex += numberResults;
			list = SegmentDaoUtil.loadSegmentsToRefresh(startIndex, numberResults);
		}
		return size;
	}

	/**
	 * @param sm
	 * @return
	 */
	public static Segment createSegment(Segment sm) {
		SegmentDaoUtil.createSegment(sm);
		clearAllCacheSegments();
		return sm;
	}


	/**
	 * @param name
	 * @param jsonQueryRules
	 * @param selectedFields
	 * @return
	 */
	public static Segment createSegment(String name, String jsonQueryRules, List<String> selectedFields) {
		Segment sm = new Segment(name, jsonQueryRules, selectedFields);
		SegmentDaoUtil.createSegment(sm);
		clearAllCacheSegments();
		return sm;
	}

	/**
	 * @param id
	 * @param name
	 * @param jsonQueryRules
	 * @param selectedFields
	 * @return
	 */
	public static Segment updateSegment(String id, String name, String jsonQueryRules, List<String> selectedFields) {
		// set data for existed data
		Segment sm = SegmentDaoUtil.getSegmentById(id);
		if(sm != null) {
			sm.setName(name);
			sm.setJsonQueryRules(jsonQueryRules);
			sm.setSelectedFields(selectedFields);
			// update DB
			SegmentDaoUtil.updateSegment(sm);
			clearAllCacheSegments();
		}
		return sm;
	}

	/**
	 * @param id
	 * @param name
	 * @param jsonQueryRules
	 * @return
	 */
	public static Segment updateSegment(String id, String name, String jsonQueryRules) {
		Segment sm = SegmentDaoUtil.getSegmentById(id);
		if(sm != null) {
			sm.setName(name);
			sm.setJsonQueryRules(jsonQueryRules);
			SegmentDaoUtil.updateSegment(sm);
			clearAllCacheSegments();
		}
		return sm;
	}

	/**
	 * @param sm
	 * @return
	 */
	public static Segment updateSegment(Segment sm) {
		String id = SegmentDaoUtil.updateSegment(sm);
		return id != null ? sm : null;
	}

	/**	
	 * 
	 * for admin UI to save segment details
	 * 
	 * @param loginUser
	 * @param json
	 * @return
	 */
	public static String saveFromJson(SystemUser loginUser, String json) {
		Segment segment = new Gson().fromJson(json, Segment.class);
		String segmentId = segment.getId();
		if(loginUser.hasOperationRole()) {
			String userLoginName = loginUser.getUserLogin();
			segment.setOwnerUsername(userLoginName);
			if (segment.isCreateNew()) {
				segment.buildHashedId();
				return createSegment(segment).getId();
			} 
			else {
				if(segment.isEditable(loginUser)) {
					return updateSegment(segment).getId();
				}
				else {
					throw new IllegalArgumentException("No authorization to update segment ID: " + segmentId);
				}
			}
		}
		else {
			throw new IllegalArgumentException("No authorization to update segment ID: " + segmentId);
		}
	}


	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload querySegmentsByFilter(SegmentFilter filter) {
		logger.info("SET CACHE cacheSegments");
		int draw = filter.getDraw();
		long recordsTotal = countTotalSegments();
		long recordsFiltered = SegmentDaoUtil.getTotalRecordsFiltered(filter);
		List<Segment> list = SegmentDaoUtil.loadSegmentsByFilter(filter);
		return JsonDataTablePayload.data(filter.getUri(), list, recordsTotal, recordsFiltered, draw);
	}
	

	/**
	 * @param name
	 * @param jsonQueryRules
	 * @param selectedFields

	 * @return
	 */
	public static List<Profile> previewTopProfilesSegment(String name, String jsonQueryRules, List<String> selectedFields) {
		Segment sm = new Segment(name, jsonQueryRules, selectedFields);
		SegmentQuery profileQuery = sm.buildQuery();
		List<Profile> rs = ProfileDaoUtil.getProfilesBySegmentQuery(profileQuery);
		return rs;
	}

	/**
	 * @param segmentId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Profile> getProfilesInSegment(String segmentId, int startIndex, int numberResult) {
		Segment sm = SegmentDaoUtil.getSegmentById(segmentId);
		return getProfilesInSegment(sm, startIndex, numberResult);
	}
	
	/**
	 * @param segment
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Profile> getProfilesInSegment(Segment segment, int startIndex, int numberResult) {
		SegmentQuery profileQuery = segment.buildQuery(startIndex, numberResult, true);
		List<Profile> rs = ProfileDaoUtil.getProfilesBySegmentQuery(profileQuery);
		return rs;
	}
	
	/**
	 * @param segment
	 * @param batchSize
	 * @param action
	 */
	public static void processProfilesInSegment(Segment segment, int batchSize, Consumer<Profile> action) {
		if(segment != null && action != null) {
			long segmentSizeIndex = segment.getTotalCount(); // segment size
			int startIndex = 0;
			while (segmentSizeIndex > 0) {
				List<Profile> profiles = SegmentDataManagement.getProfilesInSegment(segment, startIndex, batchSize);
				int profileCount = profiles.size();
				
				String message = "=> processProfilesInSegment segmentId: " + segment.getId() + " profileCount: " + profileCount;
				LogUtil.logInfo(SegmentDataManagement.class, message);
				
				profiles.parallelStream().forEach(action);
				// next loop
				startIndex += batchSize;
				segmentSizeIndex -= batchSize;
			}
		}
	}

	
	/**
	 * get All Segments By Authorization
	 * 
	 * @param filter
	 * @return
	 */
	public static List<Segment> getAllSegmentsByAuthorization(SegmentFilter filter) {
		return SegmentDaoUtil.getAllSegmentsByAuthorization(filter);
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static List<SegmentRef> getAllSegmentRefs(SegmentFilter filter) {
		return SegmentDaoUtil.getAllSegmentRefs(filter);
	}

	/**
	 * @param filter
	 * @return
	 */
	public static List<Segment> filterAndGetSegments(SegmentFilter filter) {
		return SegmentDaoUtil.runFilterQuery(filter);
	}
	
	/**
	 * @param idList
	 * @return
	 */
	public static Set<RefKey> getRefKeysByIds(List<String> ids) {
		Set<RefKey> set = null;
		try {
			set = cacheSegmentRefKey.get(StringUtil.joinFromList(StringPool.UNDERLINE, ids));
		} catch (Exception e) {
			set = SegmentDaoUtil.getRefKeysByIds(ids);
		}
		return set;
	}

	/**
	 * @param id
	 * @return
	 */
	public final static Segment getSegmentById(String id) {
		return SegmentDaoUtil.getSegmentById(id);
	}
	
	/**
	 * @return Segment
	 */
	public final static Segment getSegmentWithActivationRulesById(String id) {
		return SegmentDaoUtil.getSegmentById(id, true);
	}

	/**
	 * @return
	 */
	public final static Segment newInstance() {
		// default is 16 weeks
		int indexScore = SegmentDaoUtil.getMaxIndexScoreInSegments() + 1;
		return new Segment(indexScore);
	}


	/**
	 * @return Total of Segments
	 */
	public final static int countTotalSegments() {
		return SegmentDaoUtil.countTotalSegments();
	}

	
	/**
	 * the data is not deleted the system just remove it from valid data view, <br> 
	 * if segment.size > 0, set status of object = -1 the job scheduler will delete it after 1 week if no campaigns are linked to segment
	 * 
	 * @param segmentId
	 * @return deleted profiles
	 */
	public static int deleteSegment(SystemUser loginUser, String segmentId, boolean deleteAllProfiles) {
		int deleted = -1;
		// query deleted segment ID
		Segment sm = SegmentDaoUtil.getSegmentById(segmentId);
		// check permission
		if(sm.isEditable(loginUser)) {
			boolean autoUpdateRefSegment = !deleteAllProfiles;// in delete case, no need to update ref 
			
			// if segment is not activated, delete else just set status is removed
			boolean checkToDelete = sm.getTotalCount() == 0 || ! sm.hasActivationRules();
			if( checkToDelete ) {
				deleted = SegmentDaoUtil.deleteSegment(sm, autoUpdateRefSegment) ? 0 : -1;
			}
			else {
				deleted = SegmentDaoUtil.updateSegment(sm, autoUpdateRefSegment, false) != null ? 0 : -1;
			}
			
			// check to delete profiles
			if(deleteAllProfiles) {
				deleted += deleteAllProfilesInSegment(sm);
			}
			else {
				deleted = 0;
			}
			clearAllCacheSegments();
			
			// clear caches of deleted profiles
			if(deleteAllProfiles) {
				Analytics360Management.clearCacheProfileReport();
			}
		} 
		else {
			throw new IllegalArgumentException("No authorization to update segment ID: " + segmentId);
		}
		return deleted;
	}
	
	/**
	 * @param csvType
	 * @param segment
	 * @param exportDataUrl
	 */
	public static void saveExportedFileUrlCsvForSegment(int csvType, Segment segment, String exportDataUrl) {
		// save CSV
		if(csvType > 0) {
			segment.setExportedFileUrlCsvForAds(exportDataUrl);
		}
		else {
			segment.setExportedFileUrlCsvForExcel(exportDataUrl);
		}
		logger.info("saveExportedFileUrlCsvForSegment csvType: " + csvType + " ; exportDataUrl: "+exportDataUrl);
		SegmentDaoUtil.updateSegmentMetadata(segment);
	}

	/**
	 * delete All Profiles InSegment
	 * 
	 * @param segment
	 */
	static int deleteAllProfilesInSegment(Segment segment) {
		int startIndex = 0;
		int numberResult = 500;
		boolean realtimeQuery = true;
		List<Profile> profiles = ProfileDaoUtil.getProfilesBySegmentQuery(segment.buildQuery(startIndex, numberResult, realtimeQuery));
		int count = 0;
		int size = profiles.size();
		Set<String> removeProfileIds = new HashSet<String>();
		Set<String> deleteProfileIds = new HashSet<String>();
		while (size > 0) {
			count += size;
			// 
			profiles.forEach(profile->{
				if(profile.checkToDeleteForever()) {
					// delete profile forever
					deleteProfileIds.add(profile.getId());
				} 
				else {
					// mark as removed profile
					removeProfileIds.add(profile.getId());
				}
			});
			
			// next loop
			startIndex += size;
			profiles = ProfileDaoUtil.getProfilesBySegmentQuery(segment.buildQuery(startIndex, numberResult, realtimeQuery));
			size = profiles.size(); 
			
			logger.info("deleteAllProfilesInSegment size " + size);
			if(size <= 0) {
				break;
			}
		}
		
		// remove profiles
		ProfileDaoUtil.batchUpdateProfileStatus(removeProfileIds, Profile.STATUS_REMOVED);
		
		// delete profiles
		for (String id : deleteProfileIds) {
			TaskRunner.runInThreadPools(()->{
				ProfileDaoUtil.delete(id);
			});
		}
		logger.info("deleteAllProfilesInSegment count " + count);
		return count;
	}
	
	
	/**
	 * check All Segments To Update Profile Count
	 * @return number of checked segment
	 */
	public static int refreshDataOfAllSegments() {
		clearAllCacheSegments();
		return countTotalSegments();
	}
	
	/**
	 * @param userLogin
	 */
	public static void removeAllViewableSegments(String userLogin) {
		SegmentDaoUtil.removeAllViewableSegments(userLogin);
	}
	
	/**
	 * @param userLogin
	 */
	public static void removeAllEditableSegments(String userLogin) {
		SegmentDaoUtil.removeAllEditableSegments(userLogin);
	}
	
	/**
	 * clean deleted data in segment collection
	 */
	public static void cleanData() {
		List<Segment> segments = SegmentDaoUtil.getAllRemovedSegmentsToDeleteForever();
		for (Segment segment : segments) {
			boolean ok = SegmentDaoUtil.deleteSegment(segment, true);
			logger.info(ok + " Deleted segment: " + segment.getName());
		}
	}

}
