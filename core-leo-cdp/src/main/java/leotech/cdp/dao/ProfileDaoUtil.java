package leotech.cdp.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;

import leotech.cdp.domain.SegmentListManagement;
import leotech.cdp.job.reactive.JobMergeDuplicatedProfiles;
import leotech.cdp.model.RefKey;
import leotech.cdp.model.analytics.TouchpointReport;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.customer.ProfileModelUtil;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.customer.UpdateProfileCallback;
import leotech.cdp.query.ProfileMatchingResult;
import leotech.cdp.query.ProfileQueryBuilder;
import leotech.cdp.query.SegmentQuery;
import leotech.cdp.query.filters.ProfileFilter;
import leotech.cdp.utils.ProfileDataValidator;
import leotech.system.config.AqlTemplate;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import leotech.system.util.LogUtil;
import leotech.system.util.TaskRunner;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * Profile Database Access Object
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class ProfileDaoUtil extends AbstractCdpDatabaseUtil {

	static Logger logger = LoggerFactory.getLogger(ProfileDaoUtil.class);

	static final String AQL_COUNT_TOTAL_ACTIVE_PROFILES = "RETURN LENGTH( FOR p in "+Profile.COLLECTION_NAME+" FILTER  p.status > 0 RETURN p._key)";
	
	static final String AQL_COUNT_TOTAL_ANONYMOUS_PROFILES = AqlTemplate.get("AQL_COUNT_TOTAL_ANONYMOUS_PROFILES");
	static final String AQL_COUNT_TOTAL_CONTACT_PROFILES = AqlTemplate.get("AQL_COUNT_TOTAL_CONTACT_PROFILES");
	static final String AQL_COUNT_TOTAL_BUSINESS_PROFILES = AqlTemplate.get("AQL_COUNT_TOTAL_BUSINESS_PROFILES");
	static final String AQL_COUNT_TOTAL_UNHAPPY_PROFILES = AqlTemplate.get("AQL_COUNT_TOTAL_UNHAPPY_PROFILES");
	
	static final String AQL_GET_PROFILE_BY_ID_FOR_SYSTEM = AqlTemplate.get("AQL_GET_PROFILE_BY_ID_FOR_SYSTEM");
	static final String AQL_GET_ACTIVE_PROFILE_IDENTITY_BY_ID = AqlTemplate.get("AQL_GET_ACTIVE_PROFILE_IDENTITY_BY_ID");
	static final String AQL_GET_PROFILE_BY_ID_FOR_ADMIN = AqlTemplate.get("AQL_GET_PROFILE_BY_ID_FOR_ADMIN");
	
	static final String AQL_GET_PROFILE_ID_BY_VISITOR_ID = AqlTemplate.get("AQL_GET_PROFILE_ID_BY_VISITOR_ID");
	static final String AQL_GET_PROFILE_ID_BY_PRIMARY_KEYS = AqlTemplate.get("AQL_GET_PROFILE_ID_BY_PRIMARY_KEYS");
	
	static final String AQL_GET_PROFILE_BY_VISITOR_ID = AqlTemplate.get("AQL_GET_PROFILE_BY_VISITOR_ID");
	static final String AQL_GET_PROFILE_BY_FINGERPRINT_OR_VISITOR_ID = AqlTemplate.get("AQL_GET_PROFILE_BY_FINGERPRINT_OR_VISITOR_ID");
	
	static final String AQL_GET_PROFILE_BY_LOGIN_INFO = AqlTemplate.get("AQL_GET_PROFILE_BY_LOGIN_INFO");
	static final String AQL_GET_PROFILE_BY_LOGIN_INFO_WITH_PASSWORD = AqlTemplate.get("AQL_GET_PROFILE_BY_LOGIN_INFO_WITH_PASSWORD");
	
	static final String AQL_GET_PROFILE_BY_PRIMARY_EMAIL = AqlTemplate.get("AQL_GET_PROFILE_BY_PRIMARY_EMAIL");
	static final String AQL_GET_PROFILE_BY_PRIMARY_PHONE = AqlTemplate.get("AQL_GET_PROFILE_BY_PRIMARY_PHONE");
	static final String AQL_GET_PROFILE_BY_CRM_ID_FOR_ADMIN = AqlTemplate.get("AQL_GET_PROFILE_BY_CRM_ID_FOR_ADMIN");
	static final String AQL_GET_PROFILE_BY_FINGERPRINT_ID_FOR_ADMIN = AqlTemplate.get("AQL_GET_PROFILE_BY_FINGERPRINT_ID_FOR_ADMIN");
	static final String AQL_GET_PROFILE_BY_VISITOR_ID_FOR_ADMIN = AqlTemplate.get("AQL_GET_PROFILE_BY_VISITOR_ID_FOR_ADMIN");
	
	
	static final String AQL_GET_PROFILE_BY_KEY_IDENTITIES = AqlTemplate.get("AQL_GET_PROFILE_BY_KEY_IDENTITIES");
	static final String AQL_GET_PROFILE_BY_IDENTITY_RESOLUTION = AqlTemplate.get("AQL_GET_PROFILE_BY_IDENTITY_RESOLUTION");
	
	static final String AQL_GET_ALL_PROFILE_IDS_IN_SEGMENT = AqlTemplate.get("AQL_GET_ALL_PROFILE_IDS_IN_SEGMENT");
	static final String AQL_COUNT_PROFILE_BY_SEGMENT_ID = AqlTemplate.get("AQL_COUNT_PROFILE_BY_SEGMENT_ID");
	
	static final String AQL_GET_PROFILES_BY_PAGINATION = AqlTemplate.get("AQL_GET_PROFILES_BY_PAGINATION");
	static final String AQL_GET_ACTIVE_PROFILES_BY_PAGINATION = AqlTemplate.get("AQL_GET_ACTIVE_PROFILES_BY_PAGINATION");
	
	// for batch processing from Kafka
	static final String AQL_GET_PROFILES_BY_EMAILS = AqlTemplate.get("AQL_GET_PROFILES_BY_EMAILS");
	static final String AQL_GET_PROFILES_BY_PHONES = AqlTemplate.get("AQL_GET_PROFILES_BY_PHONES");
	static final String AQL_GET_PROFILES_BY_APPLICATION_IDS = AqlTemplate.get("AQL_GET_PROFILES_BY_APPLICATION_IDS");
	static final String AQL_GET_PROFILES_BY_LOYALTY_IDS = AqlTemplate.get("AQL_GET_PROFILES_BY_LOYALTY_IDS");
	static final String AQL_GET_PROFILES_BY_GOVERNMENT_ISSUED_IDS = AqlTemplate.get("AQL_GET_PROFILES_BY_GOVERNMENT_ISSUED_IDS");
	
	// segment Ref management
	static final String AQL_INSERT_SEGMENT_REF_KEY_FOR_PROFILE = AqlTemplate.get("AQL_INSERT_SEGMENT_REF_KEY_FOR_PROFILE");
	static final String AQL_UPDATE_SEGMENT_REF_KEY_FOR_PROFILE = AqlTemplate.get("AQL_UPDATE_SEGMENT_REF_KEY_FOR_PROFILE");
	static final String AQL_APPEND_SEGMENT_REF_KEY_FOR_PROFILE = AqlTemplate.get("AQL_APPEND_SEGMENT_REF_KEY_FOR_PROFILE");
	static final String AQL_REMOVE_SEGMENT_REF_KEY_FOR_PROFILE = AqlTemplate.get("AQL_REMOVE_SEGMENT_REF_KEY_FOR_PROFILE");
	
	static final String AQL_DELETE_ALL_INVALID_PROFILES = AqlTemplate.get("AQL_DELETE_ALL_INVALID_PROFILES");
	static final String AQL_DELETE_PROFILE_BY_ID = AqlTemplate.get("AQL_DELETE_PROFILE_BY_ID");
	
	static final String AQL_REMOVE_VIEWABLE_PROFILES_FOR_USER = AqlTemplate.get("AQL_REMOVE_VIEWABLE_PROFILES_FOR_USER");
	static final String AQL_REMOVE_EDITABLE_PROFILES_FOR_USER = AqlTemplate.get("AQL_REMOVE_EDITABLE_PROFILES_FOR_USER");
	static final String AQL_PROFILE_BATCH_UPDATE_STATUS = AqlTemplate.get("AQL_PROFILE_BATCH_UPDATE_STATUS");
	static final String AQL_PROFILE_BATCH_REMOVE = AqlTemplate.get("AQL_PROFILE_BATCH_REMOVE");
	
	static final String AQL_UPDATE_AUTHORIZATION_FOR_PROFILES_BY_JOURNEY = AqlTemplate.get("AQL_UPDATE_AUTHORIZATION_FOR_PROFILES_BY_JOURNEY");
	
	static final String AQL_DELETE_ALL_DEAD_VISITORS = AqlTemplate.get("AQL_DELETE_ALL_DEAD_VISITORS");
	static final String AQL_REMOVE_INACTIVE_PROFILES = AqlTemplate.get("AQL_REMOVE_INACTIVE_PROFILES");
	static final String AQL_DELETE_DATA_OF_DEAD_PROFILES = AqlTemplate.get("AQL_DELETE_DATA_OF_DEAD_PROFILES");
	
	private static final ExecutorService dataUpdateJob = Executors.newSingleThreadExecutor();
	
	/**
	 * @return total of profiles in the database
	 */
	public static long countTotalProfiles() {
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, AQL_COUNT_TOTAL_ACTIVE_PROFILES, Long.class).getSingleResult();
		return c;
	}
	
	/**
	 * @return total of Anonymous profiles in the database
	 */
	public static final long countTotalAnonymousProfiles() {
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, AQL_COUNT_TOTAL_ANONYMOUS_PROFILES, Long.class).getSingleResult();
		return c;
	}
	
	/**
	 * @return total of human profiles in the database
	 */
	public static final long countTotalContactProfiles() {
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, AQL_COUNT_TOTAL_CONTACT_PROFILES, Long.class).getSingleResult();
		return c;
	}
	
	/**
	 * @return total of customer/business profiles in the database
	 */
	public static final long countTotalBusinessProfiles() {
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, AQL_COUNT_TOTAL_BUSINESS_PROFILES, Long.class).getSingleResult();
		return c;
	}
	
	/**
	 * @return total of churned customer profiles in the database
	 */
	public static final long countTotalUnhappyProfiles() {
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, AQL_COUNT_TOTAL_UNHAPPY_PROFILES, Long.class).getSingleResult();
		return c;
	}
	
	
	/**
	 * delete all dead visitors with FILTER <br> 
	 * p.totalLeadScore == 0 AND p.firstName == "" AND p.lastName == "" AND p.primaryEmail == "" AND p.primaryPhone == "" AND p.crmRefId == "" AND p.type == 0
	 * 
	 * @return the number of deleted profile
	 */
	public static final int deleteAllDeadVisitors() {
		ArangoDatabase db = getCdpDatabase();
		Integer c = new ArangoDbCommand<>(db, AQL_DELETE_ALL_DEAD_VISITORS, Integer.class).getSingleResult();
		return c != null ? c.intValue(): 0;
	}
	
	/**
	 * This query identifies and removes inactive profiles from the cdp_profile collection.
	 * 
	 * @param maxProfiles
	 * @param numberOfDays
	 * @return the number of removed profile
	 */
	public static final int removeInactiveProfiles(int limitSize, int numberOfDays) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("limitSize", limitSize);
		bindVars.put("numberOfDays", numberOfDays);
		Integer c = new ArangoDbCommand<>(db, AQL_REMOVE_INACTIVE_PROFILES, bindVars, Integer.class).getSingleResult();
		return c != null ? c : 0;
	}
	
	
	
	/**
	 * @param limitSize
	 * @return
	 */
	public static final int deleteDataOfDeadProfiles(int limitSize) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("limitSize", limitSize);
		Integer c = new ArangoDbCommand<>(db, AQL_DELETE_DATA_OF_DEAD_PROFILES, bindVars, Integer.class).getSingleResult();
		return c != null ? c.intValue() : 0;
	}
	
	
	// --------------------------------------------------------- //

	/**
	 * @param profile
	 */
	public final static void insertAsAsynchronousJob(ProfileSingleView profile) {
		TaskRunner.runInThreadPools(() -> {
			insertProfile(profile, null);
		});
	}	
	
	/**
	 * @param profile
	 * @return
	 */
	public final static String insert(ProfileSingleView profile) {
		return insertProfile(profile, null);
	}
	

	/**
	 * @param profile
	 * @param callback
	 * @return profileId if insert successfully
	 */
	public final static String insertProfile(ProfileSingleView profile, UpdateProfileCallback callback) {
		String profileId = profile.getId();
		ArangoCollection col = profile.getDbCollection();
		if (profile.dataValidation() && col != null) {
			// scoring data quality
			List<TouchpointReport> engagedTouchpoints = TouchpointDaoUtil.getTopTouchpointsForEnrichment(profileId);
			profile.doDataEnrichment(engagedTouchpoints);
			try {
				col.insertDocument(profile, optionToUpsertInSilent());
				
				// check and run callback
				if(callback != null) {
					dataUpdateJob.execute(callback);
				}
				
				dataUpdateJob.execute(()->{
					checkAndUpdateSegmentRefKeys(profile.getId(), profile.getInSegments());
				});
			
				return profileId;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("ProfileDaoUtil.create.dataValidation is failed for ID: "+profileId);
		}
		return null;
	}

	/**
	 * @param profileId
	 * @param inSegments
	 */
	private static void checkAndUpdateSegmentRefKeys(String profileId, Set<RefKey> inSegments) {
		if(SystemMetaData.isUsingEventQueue()) {
			try {
				List<Segment> segments = SegmentListManagement.getAllActiveSegments();
				ProfileDaoUtil.updateProfileSegmentRefs(profileId, segments, inSegments);
			} catch (Exception e) {
				logger.error("checkAndUpdateSegmentRefKeys profileId:"+profileId, e);
			} 
		}
	}
	
	
	/**
	 * this method is used for update profile, after merging
	 * 
	 * @param srcProfileToMerge
	 * @param rootProfileId
	 * @return
	 */
	public final static String updateMergedProfile(Profile srcProfileToMerge, String rootProfileId) {
		try {
			if (srcProfileToMerge.dataValidation()) {
				ArangoCollection col = srcProfileToMerge.getDbCollection();
				if (col != null) {
					String profileId = srcProfileToMerge.getId();
					if (profileId != null) {
						
						// set profile status is inactive
						srcProfileToMerge.setStatus(Profile.STATUS_INACTIVE);
						srcProfileToMerge.setMergeCode(Profile.MERGED_BY_ID_KEYS);
						srcProfileToMerge.setRootProfileId(rootProfileId);
						
						col.updateDocument(profileId, srcProfileToMerge, getUpdateOptions());
						
						dataUpdateJob.execute(()->{
							checkAndUpdateSegmentRefKeys(srcProfileToMerge.getId(), srcProfileToMerge.getInSegments());
						});
						
						return profileId;
					}
				}
			}
		} catch (ArangoDBException e) {
			e.printStackTrace();
		}
		return null;
	}
	

	
	/**
	 * update profile data in database, if not found document in DB, a new profile is created 
	 * 
	 * @param profile
	 * @return
	 */
	public final static String updateProfile(ProfileSingleView profile, UpdateProfileCallback callback) {
		if (profile.dataValidation()) {
			String profileId = profile.getId();
			try {
				List<TouchpointReport> engagedTouchpoints = TouchpointDaoUtil.getTopTouchpointsForEnrichment(profileId);
				profile.doDataEnrichment(engagedTouchpoints);
				
				// try update
				profile.getDbCollection().updateDocument(profileId, profile, getMergeOptions());
				
				// check and run callback
				if(callback != null) {
					dataUpdateJob.execute(callback);
				}
				
				
				dataUpdateJob.execute(()->{
					// FIXME try a queue to update profile or field update when import events 
					// Error: 1200 - write-write conflict;
					checkAndUpdateSegmentRefKeys(profile.getId(), profile.getInSegments());
				});
				
				
			} catch (ArangoDBException e) {
				if(e.getErrorNum() == 1202) {
					// Error: 1202 - document not found, try to create a new profile in database
					insert(profile);
				}
				else if(e.getErrorNum() == 1200) {
					
					System.err.println("write-write conflict " + profile.getId());
				}
				else {
					e.printStackTrace();
				}
			} 
			return profileId;
		} 
		else {
			throw new InvalidDataException("ProfileDaoUtil.update is failed, dataValidation is error");
		}
	}
	
	/**
	 *  save Profile Single View Data for Profile Importing Jobs
	 * 
	 * @param profile
	 * @param autoMergeDuplicate
	 * @return profileId
	 */
	public final static String saveProfile(ProfileSingleView profile, boolean autoMergeDuplicate) {
		UpdateProfileCallback callback = null;
		if(autoMergeDuplicate) {
			callback = new UpdateProfileCallback(profile) {
				@Override
				public void processProfile() {
					JobMergeDuplicatedProfiles.execute(profile, true);
				}
			};
		}
		return updateProfile(profile, callback);
	}
	

	/**
	 *  save Profile Single View Data for Profile Importing Jobs
	 * 
	 * @param profile
	 * @return profileId
	 */
	public final static String saveProfile(ProfileSingleView profile) {
		return saveProfile(profile, profile.isDeduplicate());
	}
	
	
	/**
	 * update As Asynchronous Job
	 * 
	 * @param profile
	 * @return profile.getId
	 */
	public final static String updateAsAsynchronousJob(final ProfileSingleView profile) {
		return updateAsAsynchronousJob(profile, false);
	}

	
	/**
	 * update As Asynchronous Job
	 * 
	 * @param profile
	 * @param autoMergeDuplicate
	 * @return profile.getId
	 */
	public final static String updateAsAsynchronousJob(final ProfileSingleView profile, boolean autoMergeDuplicate) {
		TaskRunner.runInThreadPools(() -> {
			if(autoMergeDuplicate) {
				UpdateProfileCallback callback = new UpdateProfileCallback(profile) {
					@Override
					public void processProfile() {
						if(profile.checkToStartMergeDataJob()) {
							JobMergeDuplicatedProfiles.execute(profile, true);
						}
					}
				};
				updateProfile(profile, callback);
			}
			else {
				updateProfile(profile, null);
			}
		});
		return profile.getId();
	}
	
	/**
	 * delete a profile by ID
	 * 
	 * @param profileId
	 * @return
	 */
	public static String delete(String profileId) {
		ArangoCollection col = Profile.getCollection();
		if (col != null) {
			col.deleteDocument(profileId);
			return profileId;
		}
		return null;
	}

	 // Helper function to create profile update data
    static Map<String, Object> createProfileUpdate(String profileId, String segmentId, int indexScore, String segmentName, 
    		Set<String> authorizedViewers, Set<String> authorizedEditors, String queryHashedId) {
    	Map<String, Object> bindVars = new HashMap<>(10);
		bindVars.put("segmentId", segmentId);
		bindVars.put("segmentName", segmentName);
		bindVars.put("segmentIndexScore", indexScore);
		bindVars.put("authorizedViewers", authorizedViewers);
		bindVars.put("authorizedEditors", authorizedEditors);
		bindVars.put("queryHashedId", queryHashedId);
		bindVars.put("profileId", profileId);
        return bindVars;
    }

	/**
	 * update Segment RefKey for all profiles 
	 * 
	 * @param segment
	 * @param isUpdate
	 * @return
	 */
	public static void setSegmentRefForMatchedProfiles(Segment segment) {
		long begin = System.currentTimeMillis();
		
		SegmentQuery segmentQuery = segment.buildQueryToIndexProfiles();
		
		List<String> profileIds;
		if(segmentQuery.isQueryReadyToRun()) {
			profileIds = ProfileDaoUtil.getProfileIdsToBuildSegmentIndexByQuery(segmentQuery);
		}
		else {
			profileIds = new ArrayList<>(0);
		}
		//logger.info("setSegmentForProfiles.authorizedViewers" + authorizedViewers);
		List<Segment> allActiveSegments = SegmentListManagement.getAllActiveSegments();
		
		// loop for each profiles to update inSegments
		profileIds.parallelStream().forEach(profileId->{
			logger.info(" \n [Update Profile Segment Refs] profileId: " + profileId );
			ProfileIdentity profileIdx = getProfileIdentityById(profileId);
			Set<RefKey> inSegments = profileIdx.getInSegments();
			ProfileDaoUtil.updateProfileSegmentRefs(profileId, allActiveSegments, inSegments);
		});
		
		long done = System.currentTimeMillis() - begin;
		LogUtil.logInfo(ProfileDaoUtil.class, "setSegmentRefForMatchedProfiles DONE TIME " + done + " millis, profileIds.size " + profileIds.size());
	}
	


	/**
	 * @param profileId
	 * @param segmentsToCheck
	 * @param currentSegmentRefs
	 */
	public static void updateProfileSegmentRefs(String profileId, List<Segment> segmentsToCheck, Set<RefKey> currentSegmentRefs) {
		
		Set<String> removedSegmentIds = new HashSet<>();
		segmentsToCheck.forEach(segment->{
			
			String segmentName = segment.getName();
			int indexScore = segment.getIndexScore();
			Set<String> authorizedViewers = segment.getAuthorizedViewers();
			Set<String> authorizedEditors = segment.getAuthorizedEditors();
			
			SegmentQuery segmentQuery = segment.buildQuery();
			RefKey refKey = segmentQuery.buildRefKey();
			String segmentIdToCheck = segment.getId();
			
			// if profile is matching query of segment, update it
			boolean updateSegmentRef = SegmentDaoUtil.isProfileInSegment(profileId, segmentQuery);
			if(updateSegmentRef) {
				logger.info("\n ====> profileId " + profileId + " in segment " + segment.getName());
				updateSegmentRefForProfile(segmentIdToCheck, segmentName, indexScore, authorizedViewers, authorizedEditors, refKey, profileId, currentSegmentRefs);
			}
			else if(currentSegmentRefs.contains(refKey)){
				// if profile is not matching , check is in the current segments, then remove
				removedSegmentIds.add(segmentIdToCheck);
			}
		});
		
		Utils.sleep(500);
		removeSegmentRefKeyInProfile(profileId, removedSegmentIds);
	}
	
	/**
	 * @param segmentId
	 * @param segmentName
	 * @param indexScore
	 * @param authorizedViewers
	 * @param authorizedEditors
	 * @param updatedProfileIdSet
	 * @param refKey
	 * @param profileId
	 * @param profileInSegments
	 */
	public static void updateSegmentRefForProfile(String segmentId, String segmentName, int indexScore,
			Set<String> authorizedViewers, Set<String> authorizedEditors, 
			RefKey refKey, String profileId, Set<RefKey> profileInSegments) {
		// params
		Map<String, Object> bindVars = new HashMap<>(10);
		bindVars.put("segmentId", segmentId);
		bindVars.put("segmentName", segmentName);
		bindVars.put("segmentIndexScore", indexScore);
		bindVars.put("authorizedViewers", authorizedViewers);
		bindVars.put("authorizedEditors", authorizedEditors);
		bindVars.put("queryHashedId", refKey.getQueryHashedId());
		bindVars.put("profileId", profileId);

		try {
			// database instance
			ArangoDatabase db = getCdpDatabase();
			
			if(profileInSegments.size() == 0) {
				// insert new list with RefKey
				new ArangoDbCommand<String>(db, AQL_INSERT_SEGMENT_REF_KEY_FOR_PROFILE, bindVars).update();
			} else {
				
				if(profileInSegments.contains(refKey)) {
					// update RefKey
					new ArangoDbCommand<String>(db, AQL_UPDATE_SEGMENT_REF_KEY_FOR_PROFILE, bindVars).update();
				}
				else {
					// append new RefKey
					new ArangoDbCommand<String>(db, AQL_APPEND_SEGMENT_REF_KEY_FOR_PROFILE, bindVars).update();
				}
			}
			
		} catch (ArangoDBException e) {
			logger.error("ERROR updateSegmentRefForProfile profileId " + profileId);
			e.printStackTrace();
		} 
		
	}
	
	/**
	 * delete RefKey of segment
	 * 
	 * @param segmentId
	 * @param profileId
	 */
	public static void removeSegmentRefKeyInProfile(String profileId, Set<String> removedSegmentIds) {
		try {
			Map<String, Object> bindVars = new HashMap<>(4);
			bindVars.put("removedSegmentIds", removedSegmentIds);
			bindVars.put("profileId", profileId);
			new ArangoDbCommand<String>(getCdpDatabase(), AQL_REMOVE_SEGMENT_REF_KEY_FOR_PROFILE, bindVars).update();
		} catch (Exception e) {
			logger.error(e.toString());
		}
	}

	
	/**
	 * delete all invalid profiles
	 */
	public static void deleteAllInvalidProfiles() {
		new ArangoDbCommand<String>(getCdpDatabase(), AQL_DELETE_ALL_INVALID_PROFILES, new HashMap<>(0)).update();
	}
	
	/**
	 * safe delete profile
	 * 
	 * @param profileId
	 */
	public static void safeDeleteProfileById(String profileId) {
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("profileId", profileId);
		new ArangoDbCommand<String>(getCdpDatabase(), AQL_DELETE_PROFILE_BY_ID, bindVars).update();
	}
	
	/**
	 * @param extAttributes
	 */
	public static void saveProfileExtAttributes(Map<String,Object> extAttributes) {
		StringBuilder s = new StringBuilder();
		s.append("FOR p IN ").append(Profile.COLLECTION_NAME).append(" UPDATE p WITH { extAttributes : {");
		
		final AtomicInteger count = new AtomicInteger(1);
		final int size = extAttributes.size();
		Map<String, Object> bindVars = new HashMap<>(size*2);
		extAttributes.forEach((String k, Object v) -> {
			int c = count.get();
			String str = " ";
			if(c < size) {
				str = ", ";
			}
			s.append("@k_").append(c).append(" : ").append("@v_").append(c).append(str);
			bindVars.put("k_" + c, k);
			bindVars.put("v_" + c, v);
			
			count.incrementAndGet();
		});
		s.append("} } IN cdp_profile OPTIONS { mergeObjects: true }");
		String aql = s.toString();
		//update database
		new ArangoDbCommand<String>(getCdpDatabase(), aql, bindVars).update();
	}
	
	/**
	 * to save core attributes of profile: firstName, lastName, primaryEmail, totalCLV, totalCAC, ...
	 * 
	 * @param profileId
	 * @param coreAttributes
	 */
	public static void saveProfileCoreAttributes(String profileId, Map<String,Object> coreAttributes) {
		StringBuilder s = new StringBuilder();
		s.append("FOR p IN ").append(Profile.COLLECTION_NAME).append(" FILTER p._key == @profileId UPDATE p WITH { ");
		
		final AtomicInteger count = new AtomicInteger(1);
		final int size = coreAttributes.size();
		Map<String, Object> bindVars = new HashMap<>(size*2+1);
		bindVars.put("profileId", profileId);
		coreAttributes.forEach((String k, Object v) -> {
			int c = count.get();
			String str = " ";
			if(c < size) {
				str = ", ";
			}
			s.append("@k_").append(c).append(" : ").append("@v_").append(c).append(str);
			bindVars.put("k_" + c, k);
			bindVars.put("v_" + c, v);
			
			count.incrementAndGet();
		});
		s.append("} IN ").append(Profile.COLLECTION_NAME).append(" OPTIONS { mergeObjects: true }");
		String aql = s.toString();
		//update database
		new ArangoDbCommand<String>(getCdpDatabase(), aql, bindVars).update();
	}
	
	/**
	 * @param userLogin
	 */
	public static void removeAllViewableProfiles(String userLogin) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("userLogin", userLogin);
		new ArangoDbCommand<>(db, AQL_REMOVE_VIEWABLE_PROFILES_FOR_USER, bindVars).update();
	}
	
	/**
	 * @param userLogin
	 */
	public static void removeAllEditableProfiles(String userLogin) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("userLogin", userLogin);
		new ArangoDbCommand<>(db, AQL_REMOVE_EDITABLE_PROFILES_FOR_USER, bindVars).update();
	}
	
	/**
	 * @param selectedProfileIds
	 * @param newStatus
	 */
	public static void batchUpdateProfileStatus(Set<String> selectedProfileIds, int newStatus) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("selectedProfileIds", selectedProfileIds);
		bindVars.put("newStatus", newStatus);
		
		if(newStatus != Profile.STATUS_ACTIVE) {
			new ArangoDbCommand<>(db, AQL_PROFILE_BATCH_REMOVE, bindVars).update();
		}
		else {
			bindVars.put("newStatus", newStatus);
			new ArangoDbCommand<>(db, AQL_PROFILE_BATCH_UPDATE_STATUS, bindVars).update();
		}
	}
	
	/**
	 * @param journeyMapId
	 * @param authorizedViewers
	 * @param authorizedEditors
	 */
	public static void updateProfileAuthorizationByJourneyId(String journeyMapId, Set<String> authorizedViewers, Set<String> authorizedEditors, 
			Set<String> removedViewers, Set<String> removedEditors, boolean updateAllProfilesInJourney) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(5);
		bindVars.put("journeyMapId", journeyMapId);
		bindVars.put("newAuthorizedViewers", authorizedViewers);
		bindVars.put("newAuthorizedEditors", authorizedEditors);
		TaskRunner.run(()->{
			if(updateAllProfilesInJourney) {
				bindVars.put("removedViewers", removedViewers);
				bindVars.put("removedEditors", removedEditors);
				new ArangoDbCommand<>(db, AQL_UPDATE_AUTHORIZATION_FOR_PROFILES_BY_JOURNEY, bindVars).update();
			}
			else {
				Set<String> emptySet = new HashSet<>(0);
				bindVars.put("removedViewers", emptySet);
				bindVars.put("removedEditors", emptySet);
				new ArangoDbCommand<>(db, AQL_UPDATE_AUTHORIZATION_FOR_PROFILES_BY_JOURNEY, bindVars).update();
			}
		});
	}
	
	
	/////////////////////////////////////////////////////////////////////// PROFILE DATA QUERY //////////////////////////////////////////////////////////////////////////////
	
	/**
	 * @param visitorId
	 * @return
	 */
	public final static String getProfileIdByVisitorId(String visitorId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("visitorId", visitorId);
		return new ArangoDbCommand<>(db, AQL_GET_PROFILE_ID_BY_VISITOR_ID, bindVars, String.class).getSingleResult();
	}
	
	public final static String getProfileIdByPrimaryKeys(String profileId, String crmRefId, String primaryEmail, String primaryPhone) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("profileId", profileId);
		bindVars.put("crmRefId", crmRefId);
		bindVars.put("primaryEmail", primaryEmail);
		bindVars.put("primaryPhone", primaryPhone);
		return new ArangoDbCommand<>(db, AQL_GET_PROFILE_ID_BY_PRIMARY_KEYS, bindVars, String.class).getSingleResult();
	}


	/**
	 * @param email
	 * @param phone
	 * @param citizenId
	 * @return
	 */
	public final static ProfileSingleView getByIdentityResolution(String email, String phone, String citizenId) {
		Map<String, Object> bindVars = new HashMap<>(3);
		
		// deterministic
		bindVars.put("email", email);
		bindVars.put("phone", phone);
		bindVars.put("citizenId", citizenId);
		
		ArangoDatabase db = getCdpDatabase();
		ProfileMatchingResult matchRs = new ArangoDbCommand<ProfileMatchingResult>(db, AQL_GET_PROFILE_BY_IDENTITY_RESOLUTION, bindVars, ProfileMatchingResult.class).getSingleResult();
		ProfileSingleView profile = matchRs.getProfileByDeterministicProcessing();
		return profile;
	}

	
	/**
	 * @param email
	 * @return ProfileSingleView
	 */
	public final static ProfileSingleView getByPrimaryEmail(String email) {
		return getByPrimaryEmail(email, true);
	}
	
	/**
	 * @param email
	 * @return ProfileSingleView
	 */
	public final static ProfileSingleView getByPrimaryEmail(String email, boolean unifyData) {
		ProfileSingleView p = null;
		if(StringUtil.isNotEmpty(email)) {
			ArangoDatabase db = getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("email", email);
			
			if(unifyData) {
				CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
					@Override
					public ProfileSingleView apply(ProfileSingleView obj) {
						obj.unifyData();
						return obj;
					}
				};
				p = new ArangoDbCommand<>(db, AQL_GET_PROFILE_BY_PRIMARY_EMAIL, bindVars, ProfileSingleView.class, callback).getSingleResult();
			}
			else {
				p = new ArangoDbCommand<>(db, AQL_GET_PROFILE_BY_PRIMARY_EMAIL, bindVars, ProfileSingleView.class).getSingleResult();
			}
		}
		return p;
	}
	
	/**
	 * @param visitorId
	 * @return ProfileSingleView
	 */
	public final static ProfileSingleView getByVisitorId(String visitorId) {
		if(StringUtil.isNotEmpty(visitorId)) {
			ArangoDatabase db = getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("visitorId", visitorId);
			CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
				@Override
				public ProfileSingleView apply(ProfileSingleView obj) {
					obj.unifyData();
					return obj;
				}
			};
			return new ArangoDbCommand<>(db, AQL_GET_PROFILE_BY_VISITOR_ID, bindVars, ProfileSingleView.class, callback).getSingleResult();
		}
		return null;
	}
	
	/**
	 * @param lastSeenIp
	 * @param lastUsedDeviceId
	 * @param fingerprintId
	 * @param visitorId
	 * @return
	 */
	public final static ProfileSingleView getByFingerprintIdOrVisitorId(String lastSeenIp, String lastUsedDeviceId, String fingerprintId, String visitorId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("lastSeenIp", lastSeenIp);
		bindVars.put("lastUsedDeviceId", lastUsedDeviceId);
		bindVars.put("fingerprintId", fingerprintId);
		bindVars.put("visitorId", visitorId);
		CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
			@Override
			public ProfileSingleView apply(ProfileSingleView obj) {
				obj.unifyData();
				return obj;
			}
		};
		ArangoDbCommand<ProfileSingleView> cmd = new ArangoDbCommand<>(db, AQL_GET_PROFILE_BY_FINGERPRINT_OR_VISITOR_ID, bindVars, ProfileSingleView.class, callback);
		return cmd.getSingleResult();
	}
	
	
	
	/**
	 * @param loginId
	 * @param loginProvider
	 * @return ProfileSingleView
	 */
	public final static ProfileSingleView getByLoginInfo(String loginId, String loginProvider) {
		String loginInfo = ProfileIdentity.buildIdPrefix(loginProvider, loginId);
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("loginInfo", loginInfo);
		CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
			@Override
			public ProfileSingleView apply(ProfileSingleView obj) {
				obj.unifyData();
				return obj;
			}
		};
		ProfileSingleView p = new ArangoDbCommand<>(db, AQL_GET_PROFILE_BY_LOGIN_INFO, bindVars, ProfileSingleView.class, callback).getSingleResult();
		return p;
	}
	
	/**
	 * @param loginId
	 * @param loginProvider
	 * @return ProfileSingleView
	 */
	public final static ProfileSingleView getByLoginInfoWithPassword(String loginId, String loginProvider, String password) {
		String loginInfo = ProfileIdentity.buildIdPrefix(loginProvider, loginId);
		
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("loginInfo", loginInfo);
		bindVars.put("password", password);
		
		CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
			@Override
			public ProfileSingleView apply(ProfileSingleView obj) {
				obj.unifyData();
				return obj;
			}
		};
		ProfileSingleView p = new ArangoDbCommand<>(db, AQL_GET_PROFILE_BY_LOGIN_INFO_WITH_PASSWORD, bindVars, ProfileSingleView.class, callback).getSingleResult();
		return p;
	}

	/**
	 * @param phone
	 * @param unifyData
	 * @return ProfileSingleView
	 */
	public final static ProfileSingleView getByPrimaryPhone(String phone, boolean unifyData) {
		ProfileSingleView p = null;
		if(StringUtil.isNotEmpty(phone)) {
			ArangoDatabase db = getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("phone", phone);
			if(unifyData) {
				CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
					@Override
					public ProfileSingleView apply(ProfileSingleView obj) {
						obj.unifyData();
						return obj;
					}
				};
				p = new ArangoDbCommand<ProfileSingleView>(db, AQL_GET_PROFILE_BY_PRIMARY_PHONE, bindVars, ProfileSingleView.class, callback).getSingleResult();
			}
			else {
				p = new ArangoDbCommand<ProfileSingleView>(db, AQL_GET_PROFILE_BY_PRIMARY_PHONE, bindVars, ProfileSingleView.class).getSingleResult();
			}
		}
		return p;
	}
	
	/**
	 * @param loginUser
	 * @param id
	 * @return
	 */
	public final static ProfileSingleView checkAndGetProfileById(SystemUser loginUser, String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("id", id);
		bindVars.put("loginUsername", loginUser.getUserLogin());
		bindVars.put("profileVisitorId", loginUser.getProfileVisitorId());
		CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
			@Override
			public ProfileSingleView apply(ProfileSingleView obj) {
				obj.unifyData();
				return obj;
			}
		};
		return new ArangoDbCommand<>(db, AQL_GET_PROFILE_BY_ID_FOR_ADMIN, bindVars, ProfileSingleView.class, callback).getSingleResult();
	}
	
	/**
	 * @param loginUser
	 * @param crmRefId
	 * @return ProfileSingleView
	 */
	public final static ProfileSingleView checkAndGetProfileByCrmId(SystemUser loginUser, String crmRefId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("hasAdminRole", loginUser.hasAdminRole());
		bindVars.put("loginUsername", loginUser.getUserLogin());
		bindVars.put("crmRefId", crmRefId);
		//loading touchpoint
		CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
			@Override
			public ProfileSingleView apply(ProfileSingleView obj) {
				obj.unifyData();
				return obj;
			}
		};
		ProfileSingleView p = new ArangoDbCommand<ProfileSingleView>(db, AQL_GET_PROFILE_BY_CRM_ID_FOR_ADMIN, bindVars, ProfileSingleView.class, callback).getSingleResult();
		return p;
	}
	
	/**
	 * @param loginUser
	 * @param visitorId
	 * @return ProfileSingleView
	 */
	public final static ProfileSingleView checkAndGetProfileByVisitorId(SystemUser loginUser, String visitorId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("hasAdminRole", loginUser.hasAdminRole());
		bindVars.put("loginUsername", loginUser.getUserLogin());
		bindVars.put("profileVisitorId", loginUser.getProfileVisitorId());
		bindVars.put("visitorId", visitorId);
		CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
			@Override
			public ProfileSingleView apply(ProfileSingleView obj) {
				obj.unifyData();
				return obj;
			}
		};
		ProfileSingleView p = new ArangoDbCommand<ProfileSingleView>(db, AQL_GET_PROFILE_BY_VISITOR_ID_FOR_ADMIN, bindVars, ProfileSingleView.class, callback).getSingleResult();
		return p;
	}
	
	
	/**
	 * for admin query or data processing by system
	 * 
	 * @param id
	 * @return ProfileSingleView with unifyData = true
	 */
	public final static ProfileSingleView getProfileById(String profileId) {
		return getProfileById(profileId, true);
	}
	

	/**
	 * for data processing by system 
	 * 
	 * @param profileId
	 * @param unifyData
	 * @return
	 */
	public final static ProfileSingleView getProfileById(String profileId, boolean unifyData) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", profileId);
		ProfileSingleView p;
		if(unifyData) {
			CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
				@Override
				public ProfileSingleView apply(ProfileSingleView obj) {
					obj.unifyData();
					return obj;
				}
			};
			p = new ArangoDbCommand<>(db, AQL_GET_PROFILE_BY_ID_FOR_SYSTEM, bindVars, ProfileSingleView.class, callback).getSingleResult();
		}
		else {
			p = new ArangoDbCommand<>(db, AQL_GET_PROFILE_BY_ID_FOR_SYSTEM, bindVars, ProfileSingleView.class).getSingleResult();
		}
		return p;
	}
	
	/**
	 * @param profileId
	 * @return
	 */
	public final static ProfileIdentity getProfileIdentityById(String profileId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", profileId);
		return new ArangoDbCommand<>(db, AQL_GET_ACTIVE_PROFILE_IDENTITY_BY_ID, bindVars, ProfileIdentity.class).getSingleResult();
	}
	
	/**
	 * @param crmId
	 * @param unifyData
	 * @return
	 */
	public final static ProfileSingleView getByCrmId(String crmId, boolean unifyData) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("crmRefId", crmId);
		bindVars.put("hasAdminRole", true);
		bindVars.put("loginUsername", "");
		ProfileSingleView p;
		if(unifyData) {
			//loading touchpoint
			CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
				@Override
				public ProfileSingleView apply(ProfileSingleView obj) {
					obj.unifyData();
					return obj;
				}
			};
			p = new ArangoDbCommand<ProfileSingleView>(db, AQL_GET_PROFILE_BY_CRM_ID_FOR_ADMIN, bindVars, ProfileSingleView.class, callback).getSingleResult();
		}
		else {
			p = new ArangoDbCommand<ProfileSingleView>(db, AQL_GET_PROFILE_BY_CRM_ID_FOR_ADMIN, bindVars, ProfileSingleView.class).getSingleResult();
		}
		return p;
	}
	
	/**
	 * @param fingerprintId
	 * @param unifyData
	 * @return
	 */
	public final static ProfileSingleView getByFingerprintId(String fingerprintId, boolean unifyData) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("fingerprintId", fingerprintId);
		bindVars.put("hasAdminRole", true);
		bindVars.put("loginUsername", "");
		ProfileSingleView p;
		if(unifyData) {
			//loading touchpoint
			CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
				@Override
				public ProfileSingleView apply(ProfileSingleView obj) {
					obj.unifyData();
					return obj;
				}
			};
			p = new ArangoDbCommand<ProfileSingleView>(db, AQL_GET_PROFILE_BY_FINGERPRINT_ID_FOR_ADMIN, bindVars, ProfileSingleView.class, callback).getSingleResult();
		}
		else {
			p = new ArangoDbCommand<ProfileSingleView>(db, AQL_GET_PROFILE_BY_FINGERPRINT_ID_FOR_ADMIN, bindVars, ProfileSingleView.class).getSingleResult();
		}
		return p;
	}
	
	
	
	/**
	 * @param socialMediaIds
	 * @param unifyData
	 * @return
	 */
	public final static ProfileSingleView getBySocialMediaIds(Map<String,String> socialMediaIds, boolean unifyData) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(0);

		StringBuilder s = new StringBuilder(" FOR p IN cdp_profile FILTER false ");
		socialMediaIds.entrySet().forEach(e->{
			 // "zalo:123456789" IN p.identities OR "linkedin:123456789" IN p.identities
			 s.append(" OR '").append(e.getKey()).append(":").append(e.getValue()).append("' IN p.identities ");
		});
		s.append(" AND p.status == 1 SORT p.dataQualityScore DESC RETURN p ");
		
		String aql = s.toString();
		logger.info(aql);
		
		ProfileSingleView p;
		if(unifyData) {
			//loading touchpoint
			CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
				@Override
				public ProfileSingleView apply(ProfileSingleView obj) {
					obj.unifyData();
					return obj;
				}
			};
			p = new ArangoDbCommand<ProfileSingleView>(db, aql, bindVars, ProfileSingleView.class, callback).getSingleResult();
		}
		else {
			p = new ArangoDbCommand<ProfileSingleView>(db, aql, bindVars, ProfileSingleView.class).getSingleResult();
		}
		return p;
	}
	
	/**
	 * @param profileIdentity
	 * @param unifyData
	 * @return
	 */
	public final static ProfileSingleView getByExternalIds(ProfileIdentity profileIdentity, boolean unifyData) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(0);
		Set<String> applicationIDs = profileIdentity.getApplicationIDs();
		Set<String> governmentIssuedIDs = profileIdentity.getGovernmentIssuedIDs();
		Set<String> loyaltyIDs = profileIdentity.getLoyaltyIDs();
		Set<String> fintechSystemIDs = profileIdentity.getFintechSystemIDs();
		Map<String, String> socialProfileIds = profileIdentity.getSocialMediaProfiles();

		StringBuilder aql = new StringBuilder(" FOR p IN cdp_profile FILTER false ");
		socialProfileIds.entrySet().forEach(e->{
			 // "zalo:123456789" IN p.identities OR "linkedin:123456789" IN p.identities
			 aql.append(" OR '").append(e.getKey()).append(":").append(e.getValue()).append("' IN p.identities ");
		});
		applicationIDs.forEach(e->{
			 aql.append(" OR '").append(e).append("' IN p.applicationIDs ");
		});
		governmentIssuedIDs.forEach(e->{
			 aql.append(" OR '").append(e).append("' IN p.governmentIssuedIDs ");
		});
		loyaltyIDs.forEach(e->{
			 aql.append(" OR '").append(e).append("' IN p.loyaltyIDs ");
		});
		fintechSystemIDs.forEach(e->{
			 aql.append(" OR '").append(e).append("' IN p.fintechSystemIDs ");
		});
		aql.append(" AND p.status == 1 SORT p.dataQualityScore LIMIT 1 RETURN p ");
		
		String aqlStr =  aql.toString();
		logger.info(aqlStr);
		
		ProfileSingleView p;
		if(unifyData) {
			//loading touchpoint
			CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
				@Override
				public ProfileSingleView apply(ProfileSingleView obj) {
					obj.unifyData();
					return obj;
				}
			};
			p = new ArangoDbCommand<ProfileSingleView>(db, aqlStr, bindVars, ProfileSingleView.class, callback).getSingleResult();
		}
		else {
			p = new ArangoDbCommand<ProfileSingleView>(db, aqlStr, bindVars, ProfileSingleView.class).getSingleResult();
		}
		return p;
	}
	
	/**
	 * @param pIdentity
	 * @param unifyData
	 * @return
	 */
	public final static ProfileSingleView getByProfileIdentity(ProfileIdentity pIdentity) {
		boolean isUpdate = StringUtil.isNotEmpty(pIdentity.getUpdateByKey());
		String primaryEmail = pIdentity.getPrimaryEmail();		
		String primaryPhone = pIdentity.getPrimaryPhone();
		String crmRefId = pIdentity.getCrmRefId();
		ProfileSingleView finalProfile = null;
		Set<String> applicationIDs = pIdentity.getApplicationIDs();
		Set<String> loyaltyIDs = pIdentity.getLoyaltyIDs();
		
		if(isUpdate) {
			// search to update by exactly the value of updateByKey
			if(pIdentity.shouldUpdateByCrmRefId()) {
				// try search by CRM ID
				finalProfile = getByCrmId(crmRefId, false);
			}
			else if(pIdentity.shouldUpdateByApplicationIDs()) {
				// try search by CRM ID
				finalProfile = getSingleProfileByApplicationIDs(applicationIDs);
			}
			else if(pIdentity.shouldUpdateByLoyaltyIDs()) {
				finalProfile = getSingleProfileByLoyaltyIDs(loyaltyIDs);
			}
			else if(pIdentity.shouldUpdateByPhone()) {
				// try to search by phone
				finalProfile = getByPrimaryPhone(primaryPhone, false);
			}			
			else if(pIdentity.shouldUpdateByEmail()) {
				// try to search by email
				finalProfile = getByPrimaryEmail(primaryEmail, false);
			}
		}
		else {
			// search to avoid create a new profile
			// try search by crmRefId
			if( StringUtil.isNotEmpty(crmRefId)) {
				finalProfile = getByCrmId(crmRefId, false);
			}
			// try to search by phone
			if(finalProfile== null && ProfileDataValidator.isValidPhoneNumber(primaryPhone)) {
				finalProfile = getByPrimaryPhone(primaryPhone, false);
			}
			// try to search by email
			if(finalProfile== null && ProfileDataValidator.isValidEmail(primaryEmail)) {
				finalProfile = getByPrimaryEmail(primaryEmail, false);
			}
			// try search by applicationIDs
			if(finalProfile == null && applicationIDs.size()>0) {
				finalProfile = getSingleProfileByApplicationIDs(applicationIDs);
			}
			if(pIdentity.hasExternalIds() && finalProfile== null){
				//
				finalProfile = getByExternalIds(pIdentity, isUpdate);
			}
		}
		return finalProfile;
	}
	
	
	/**
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public final static List<Profile> listByPagination(int startIndex, int numberResult) {
		return listByPagination(startIndex, numberResult, true, false);
	}
	
	
	/**
	 * @param startIndex
	 * @param numberResult
	 * @param onlyActiveProfile
	 * @param sortByDataQualityScore
	 * @return
	 */
	public final static List<Profile> listByPagination(int startIndex, int numberResult, boolean onlyActiveProfile, boolean sortByDataQualityScore) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		
		if(sortByDataQualityScore) {
			bindVars.put("sortBy", "dataQualityScore");
		}
		else {
			bindVars.put("sortBy", "updatedAt");
		}
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		String aql = AQL_GET_PROFILES_BY_PAGINATION;
		if(onlyActiveProfile) {
			aql = AQL_GET_ACTIVE_PROFILES_BY_PAGINATION;
		}
		List<Profile> list = new ArangoDbCommand<Profile>(db, aql , bindVars, Profile.class).getResultsAsList();
		return list;
	}
	
	
	
	
	
	/**
	 * @param filter
	 * @return
	 */
	public final static int getTotalRecordsFiltered(ProfileFilter filter) {
		ArangoDatabase db = getCdpDatabase();
		String aql = ProfileQueryBuilder.buildAqlFromProfileFilter(filter, true);
		int totalFiltered =  new ArangoDbCommand<>(db, aql, Integer.class).getSingleResult();
		return totalFiltered;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public final static JsonDataTablePayload listByFilter(ProfileFilter filter){
		long recordsFiltered = getTotalRecordsFiltered(filter);
		List<Profile> list;
		if(recordsFiltered > 0) {
			list = getProfilesByFilter(filter);
		} else {
			list = new ArrayList<>(0);
		}
		long recordsTotal = ProfileDaoUtil.countTotalProfiles();
		int draw = filter.getDraw();
		JsonDataTablePayload payload = JsonDataTablePayload.data(filter.getUri(), list, recordsTotal, recordsFiltered, draw);
		return payload;
	}
	
	/**
	 * 
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public final static List<ProfileSingleView> listSingleViewAllWithPagination(int startIndex, int numberResult) {
		return listSingleViewAllWithPagination(startIndex, numberResult, false);
	}
	
	
	/**
	 * 	 for ProfileSingleViewAllDataJob
	 * 
	 * @param startIndex
	 * @param numberResult
	 * @param sortByDataQualityScore
	 * @return
	 */
	public final static List<ProfileSingleView> listSingleViewAllWithPagination(int startIndex, int numberResult, boolean sortByDataQualityScore) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		if(sortByDataQualityScore) {
			bindVars.put("sortBy", "dataQualityScore");
		}
		else {
			bindVars.put("sortBy", "updatedAt");
		}
		
		CallbackQuery<ProfileSingleView> callback = new CallbackQuery<ProfileSingleView>() {
			@Override
			public ProfileSingleView apply(ProfileSingleView obj) {
				obj.unifyData();
				return obj;
			}
		};
		ArangoDbCommand<ProfileSingleView> q = new ArangoDbCommand<ProfileSingleView>(db, AQL_GET_ACTIVE_PROFILES_BY_PAGINATION, bindVars, ProfileSingleView.class, callback);
		List<ProfileSingleView> list = q.getResultsAsList();
		return list;
	}
	

	/**
	 * @param filter
	 * @return
	 */
	public final static List<Profile> getProfilesByFilter(ProfileFilter filter) {
		ArangoDatabase db = getCdpDatabase();
		
		Map<String, Object> bindVars = new HashMap<>(2);
		
		if(!filter.isDataDeduplicationJob()) {
			bindVars.put("startIndex", filter.getStart());
			bindVars.put("numberResult", filter.getLength());
		}
		
		String aql = ProfileQueryBuilder.buildAqlFromProfileFilter(filter, false);
		logger.info(" buildAqlFromProfileFilter: \n " + aql);
		
		ArangoDbCommand<Profile> q = new ArangoDbCommand<Profile>(db, aql, bindVars, Profile.class);
		return q.getResultsAsList();
	}
	
	/**
	 * segmentQuery
	 * 
	 * @param profileQuery
	 * @return List of Profile
	 */
	public final static List<Profile> getProfilesBySegmentQuery(SegmentQuery segmentQuery) {
		List<Profile> list = null;
		ArangoDatabase db = getCdpDatabase();
		boolean realtimeQuery = segmentQuery.isRealtimeQuery();
		if(realtimeQuery) {
			String aql = segmentQuery.getQueryWithFiltersAndPagination();
			ArangoDbCommand<Profile> q = new ArangoDbCommand<>(db, aql, Profile.class);
			list = q.getResultsAsList();
			
			String s = aql.substring(0, aql.indexOf("RETURN DISTINCT"));
			logger.info(" => realtime FULL Query getProfilesByQuery run AQL \n " + s);
		}
		else {
			String segmentId = segmentQuery.getSegmentId();
			int startIndex = segmentQuery.getStartIndex();
			int numberResult = segmentQuery.getNumberResult();
			List<String> profileFields = ProfileModelUtil.getExposedFieldNamesInSegmentList();
			list = getProfilesBySegmentId(segmentId, startIndex, numberResult, profileFields);
			String msg = " => query profile.inSegments with segmentId:" + segmentId;
			logger.info(msg);
		}
		return list;
	}
	
	
	
	/**
	 * @param segmentQuery
	 * @return List of ProfileIdentity
	 */
	public final static List<ProfileIdentity> getProfileIdentitiesByQuery(SegmentQuery segmentQuery) {
		ArangoDatabase db = getCdpDatabase();
		segmentQuery.setSelectedFields(ProfileModelUtil.getExportedProfileIdentitiesForSegment());
		String aql = segmentQuery.getQueryWithFiltersAndPagination();
		
		//logger.info(" AQL getProfileIdentitiesByQuery \n: " + aql);
		
		ArangoDbCommand<ProfileIdentity> q = new ArangoDbCommand<ProfileIdentity>(db, aql, ProfileIdentity.class);
		List<ProfileIdentity> list = q.getResultsAsList();
		return list;
	}
	
	/**
	 * @param segmentQuery
	 * @return
	 */
	public final static List<String> getProfileIdsToBuildSegmentIndexByQuery(SegmentQuery segmentQuery) {
		ArangoDatabase db = getCdpDatabase();
	
		String aql = segmentQuery.getQueryToBuildSegmentIndex();
		
		logger.info(" AQL getProfileIdsToBuildSegmentIndexByQuery \n " + aql);
		
		ArangoDbCommand<String> q = new ArangoDbCommand<String>(db, aql, String.class);
		return q.getResultsAsList();
	}
	
	
	/**
	 * @param segmentId
	 * @return List of Profile ID
	 */
	public final static Set<String> getAllProfileIdsBySegmentId(String segmentId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("segmentId", segmentId);
		ArangoDbCommand<String> q = new ArangoDbCommand<String>(db, AQL_GET_ALL_PROFILE_IDS_IN_SEGMENT, bindVars, String.class);
		Set<String> set = q.getResultsAsSet();
		return set;
	}
	
	
	/**
	 * @param segmentId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public final static List<Profile> getProfilesBySegmentId(String segmentId, int startIndex, int numberResult, List<String> profileFields) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("segmentId", segmentId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		StringBuilder aql = new StringBuilder();
		aql.append(" FOR p in cdp_profile  ");
		aql.append(" FILTER @segmentId IN p.inSegments[*].id AND p.status > 0 ");
		aql.append(" SORT p.updatedAt DESC ");
		aql.append(" LIMIT @startIndex, @numberResult ");
		
		int size = profileFields.size();
		if (size == 0) {
			aql.append(" RETURN p ");
		} 
		else {
			aql.append(" RETURN { ");
			for (int i = 0; i < size; i++) {
				String field = profileFields.get(i);
				if (field.equals("id") || field.equals("_key")) {
					aql.append(field).append(":").append("p._key");
				} else {
					aql.append(field).append(":").append("p.").append(field);
				}

				if (i < size - 1) {
					aql.append(", ");
				}
			}
			aql.append(" } ");
		}
		logger.debug(" ===> getProfilesBySegmentId with AQL: \n " + aql);
		List<Profile> ps = new ArangoDbCommand<>(db, aql.toString(), bindVars, Profile.class).getResultsAsList();
		return ps;
	}
	
	/**
	 * @param segmentId
	 * @return
	 */
	public static long countTotalProfileBySegmentId(String segmentId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("segmentId", segmentId);
		long c =  new ArangoDbCommand<Long>(db, AQL_COUNT_PROFILE_BY_SEGMENT_ID, bindVars, Long.class).getSingleResult();
		return c;
	}
	
	
	/**
	 * @param applicationID
	 * @return
	 */
	public final static List<Profile> getByApplicationIDs(Set<String> applicationIDs) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("applicationIDs", applicationIDs);
		List<Profile> ps = new ArangoDbCommand<>(db, AQL_GET_PROFILES_BY_APPLICATION_IDS, bindVars, Profile.class).getResultsAsList();
		return ps;
	}
	
	/**
	 * @param applicationIDs
	 * @return
	 */
	public final static ProfileSingleView getSingleProfileByApplicationIDs(Set<String> applicationIDs) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("applicationIDs", applicationIDs);
		ProfileSingleView p = new ArangoDbCommand<>(db, AQL_GET_PROFILES_BY_APPLICATION_IDS, bindVars, ProfileSingleView.class).getSingleResult();
		return p;
	}
	
	/**
	 * @param loyaltyIDs
	 * @return
	 */
	public final static ProfileSingleView getSingleProfileByLoyaltyIDs(Set<String> loyaltyIDs) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("loyaltyIDs", loyaltyIDs);
		ProfileSingleView p = new ArangoDbCommand<>(db, AQL_GET_PROFILES_BY_LOYALTY_IDS, bindVars, ProfileSingleView.class).getSingleResult();
		return p;
	}
	
	/**
	 * @param governmentIssuedID
	 * @return
	 */
	public final static List<Profile> getByGovernmentIssuedIDs(Set<String> governmentIssuedIDs) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("governmentIssuedIDs", governmentIssuedIDs);
		List<Profile> ps = new ArangoDbCommand<>(db, AQL_GET_PROFILES_BY_GOVERNMENT_ISSUED_IDS, bindVars, Profile.class).getResultsAsList();
		return ps;
	}
	
}
