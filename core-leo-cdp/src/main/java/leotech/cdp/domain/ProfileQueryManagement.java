package leotech.cdp.domain;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.cache.RedisCache;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.query.filters.ProfileFilter;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import rfx.core.util.StringUtil;

/**
 * Profile query Management for loading and query filtering
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class ProfileQueryManagement {
	
	public static final String CACHE_COUNT_TOTAL_PROFILE = "cdp_countTotalProfiles";
	
	static Logger logger = LoggerFactory.getLogger(ProfileQueryManagement.class);
	
	/**
	 * @param visitorId
	 * @param email
	 * @param phone
	 * @param loginId
	 * @param loginProvider
	 * @param lastSeenIp
	 * @param userDeviceId
	 * @param fingerprintId
	 * @return
	 */
	public static ProfileSingleView getProfileByPrimaryKeys(String visitorId, String email, String phone, String loginId, String loginProvider, 
			String lastSeenIp, String userDeviceId, String fingerprintId) {
		ProfileSingleView pf = null;
		
		boolean isWebProfile = isWebUser(visitorId, lastSeenIp, userDeviceId, fingerprintId);
		if (isWebProfile) {
			pf = ProfileDaoUtil.getByFingerprintIdOrVisitorId(lastSeenIp, userDeviceId, fingerprintId, visitorId);
		} 
		else if (StringUtil.isNotEmpty(email)) {
			pf = ProfileDaoUtil.getByPrimaryEmail(email);
		}
		else if (StringUtil.isNotEmpty(phone)) {
			pf = ProfileDaoUtil.getByPrimaryPhone(phone, true);
		}
		else if (StringUtil.isNotEmpty(loginId) && StringUtil.isNotEmpty(loginProvider)) {
			pf = ProfileDaoUtil.getByLoginInfo(loginId, loginProvider);
		}
		return pf;
	}

	private static boolean isWebUser(String visitorId, String lastSeenIp, String userDeviceId, String fingerprintId) {
		boolean isWebProfile = StringUtil.isNotEmpty(lastSeenIp) && StringUtil.isNotEmpty(userDeviceId) && StringUtil.isNotEmpty(fingerprintId);
		isWebProfile = isWebProfile ||  StringUtil.isNotEmpty(visitorId);
		return isWebProfile;
	}
	
	/**
	 * for SSO login
	 * 
	 * @param loginId
	 * @param loginProvider
	 * @param hashedPassword
	 * @return
	 */
	public static ProfileSingleView getProfileByLogin(String loginId, String loginProvider, String hashedPassword) {
		ProfileSingleView pf = null;
		if (StringUtil.isNotEmpty(loginId) && StringUtil.isNotEmpty(loginProvider)) {
			 pf = ProfileDaoUtil.getByLoginInfoWithPassword(loginId, loginProvider, hashedPassword);
		}
		return pf;
	}

	/**
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Profile> list(int startIndex, int numberResult) {
		List<Profile> list = ProfileDaoUtil.listByPagination(startIndex, numberResult);
		return list;
	}


	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload filter(ProfileFilter filter) {
		return ProfileDaoUtil.listByFilter(filter);
	}
	
	public static long countTotalOfProfiles() {
	    try {
	        return countTotalOfProfilesAsync().get(2, TimeUnit.SECONDS);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ProfileDaoUtil.countTotalProfiles();
	    }
	}


	/**
	 * Get total raw profiles asynchronously.
	 * 
	 * Works safely in Vert.x event loop using CompletableFuture.
	 */
	public static CompletableFuture<Long> countTotalOfProfilesAsync() {
	    final String key = CACHE_COUNT_TOTAL_PROFILE;

	    // Step 1: Try to get from Redis asynchronously
	    return RedisCache.getCacheAsync(key)
	        .thenApply(json -> {
	            if (StringUtil.isNotEmpty(json)) {
	                long cachedValue = StringUtil.safeParseLong(json);
	                if (cachedValue > 0) {
	                    return cachedValue;
	                }
	            }
	            // Redis miss → mark as needing DB fetch
	            return 0L;
	        })
	        // Step 2: If no Redis value, query DB in same async chain
	        .thenCompose(totalProfile -> {
	            if (totalProfile > 0) {
	                // Return cached value
	                return CompletableFuture.completedFuture(totalProfile);
	            } else {
	                // Run DB call in a background thread (off event loop)
	                return CompletableFuture.supplyAsync(() -> {
	                    long dbCount = ProfileDaoUtil.countTotalProfiles();
	                    // Async update Redis (fire and forget)
	                    RedisCache.setCacheWithExpiryAsync(key, dbCount, 90, false);
	                    return dbCount;
	                });
	            }
	        })
	        .exceptionally(e -> {
	            e.printStackTrace();
	            // fallback to direct DB call if Redis completely fails
	            long dbCount = ProfileDaoUtil.countTotalProfiles();
	            RedisCache.setCacheWithExpiryAsync(key, dbCount, 90, false);
	            return dbCount;
	        });
	}


	/**
	 * total human profiles
	 * 
	 * @return
	 */
	public final static long countTotalOfHumanProfiles() {
		return ProfileDaoUtil.countTotalContactProfiles();
	}
	
	/**
	 * for admin UI app
	 * 
	 * @param loginUser
	 * @param crmRefId
	 * @return
	 */
	public static ProfileSingleView checkAndGetProfileByCrmId(SystemUser loginUser, String crmRefId) {
		if(StringUtil.isNotEmpty(crmRefId) && loginUser != null) {
			ProfileSingleView profile = ProfileDaoUtil.checkAndGetProfileByCrmId(loginUser, crmRefId);
			return profile;
		}
		return null;
	}
	
	/**
	 * for admin UI app
	 * 
	 * @param loginUser
	 * @param visitorId
	 * @return
	 */
	public static ProfileSingleView checkAndGetProfileByVisitorId(SystemUser loginUser, String visitorId) {
		if(StringUtil.isNotEmpty(visitorId) && loginUser != null) {
			ProfileSingleView profile = ProfileDaoUtil.checkAndGetProfileByVisitorId(loginUser, visitorId);
			return profile;
		}
		return null;
	}
	
	/**
	 * use to update by submit form
	 * 
	 * @param email
	 * @param unifyData
	 * @return
	 */
	public static Profile getByPrimaryEmail(String email, boolean unifyData) {
		Profile profile = ProfileDaoUtil.getByPrimaryEmail(email, unifyData);
		return profile;
	}
	
	/**
	 * use to update by submit form
	 * 
	 * @param email
	 * @return Profile
	 */
	public static ProfileSingleView getByPrimaryEmail(String email) {
		return ProfileDaoUtil.getByPrimaryEmail(email, false);
	}
	
	
	/**
	 * @param phone
	 * @return Profile
	 */
	public static Profile getByPrimaryPhone(String phone) {
		return ProfileDaoUtil.getByPrimaryPhone(phone, false);
	}
	
	/**
	 * @param crmId
	 * @return Profile
	 */
	public static Profile getByCrmId(String crmId) {
		return ProfileDaoUtil.getByCrmId(crmId, false);
	}
	
	/**
	 *  get profile by Application Id
	 * 
	 * @param appId
	 * @return Profile
	 */
	public static Profile getByApplicationID(String appId) {
		Set<String> appIds = Sets.newHashSet(appId);
		List<Profile> profiles = ProfileDaoUtil.getByApplicationIDs(appIds);
		return profiles.size() > 0 ? profiles.get(0) : null;
	}
	
	
	/**
	 * get profile by Social Security number (SSN) or Citizen ID
	 * 
	 * @param govId
	 * @return Profile
	 */
	public static Profile getByGovernmentIssuedID(String govId) {
		Set<String> govIds = Sets.newHashSet(govId);
		List<Profile> profiles = ProfileDaoUtil.getByGovernmentIssuedIDs(govIds);
		return profiles.size() > 0 ? profiles.get(0) : null;
	}
	
	/**
	 * @param socialId
	 * @return
	 */
	public static Profile getBySocialMediaId(String socialId) {
		ProfileIdentity pid = new ProfileIdentity();
		pid.setSocialMediaId(socialId);
		ProfileSingleView profile = ProfileDaoUtil.getByExternalIds(pid , false);
		return profile;
	}
	
	
	
	/**
	 * for system background tasks
	 * 
	 * @param refProfileId
	 * @return
	 */
	public static ProfileSingleView getByIdForSystem(String refProfileId) {
		ProfileSingleView profile = ProfileDaoUtil.getProfileById(refProfileId);
		return profile;
	}

	/**
	 * for admin UI app
	 * 
	 * @param systemUserId
	 * @param id
	 * @return
	 */
	public static ProfileSingleView checkAndGetProfileById(SystemUser loginUser, String id) {
		ProfileSingleView profile = null;
		if(loginUser.hasAdminRole()) {
			profile = ProfileDaoUtil.getProfileById(id);
		} 
		else {
			profile = ProfileDaoUtil.checkAndGetProfileById(loginUser, id);
		}
		return profile;
	}

}
