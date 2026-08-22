package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.query.filters.ProfileFilter;

/**
 * Identity Resolution for Profile
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class IdentityResolutionManagement {
	
	static Logger logger = LoggerFactory.getLogger(IdentityResolutionManagement.class);

	@FunctionalInterface
	public interface ProfileFinder {
		List<Profile> find(ProfileFilter filter);
	}

	@FunctionalInterface
	public interface ProfileMerger {
		int merge(Profile targetProfile, List<Profile> candidates);
	}

	@FunctionalInterface
	public interface ProfileReportCache {
		void clear();
	}

	private final ProfileDeduplicationFilterFactory filterFactory;
	private final ProfileFinder profileFinder;
	private final ProfileMerger profileMerger;
	private final ProfileReportCache profileReportCache;

	public IdentityResolutionManagement() {
		this(new ProfileDeduplicationFilterFactory(), ProfileDaoUtil::getProfilesByFilter,
				ProfileMergeService::mergeProfileData, Analytics360Management::clearCacheProfileReport);
	}

	public IdentityResolutionManagement(ProfileFinder profileFinder, ProfileMerger profileMerger,
			ProfileReportCache profileReportCache) {
		this(new ProfileDeduplicationFilterFactory(), profileFinder, profileMerger, profileReportCache);
	}

	IdentityResolutionManagement(ProfileDeduplicationFilterFactory filterFactory, ProfileFinder profileFinder,
			ProfileMerger profileMerger, ProfileReportCache profileReportCache) {
		this.filterFactory = Objects.requireNonNull(filterFactory, "filterFactory");
		this.profileFinder = Objects.requireNonNull(profileFinder, "profileFinder");
		this.profileMerger = Objects.requireNonNull(profileMerger, "profileMerger");
		this.profileReportCache = Objects.requireNonNull(profileReportCache, "profileReportCache");
	}

	public static final class ResolutioResult {
		int count;
		String keyProfileId;
		public ResolutioResult() {
			
		}
		public ResolutioResult(int count, String keyProfileId) {
			super();
			this.count = count;
			this.keyProfileId = keyProfileId;
		}
		public int getDuplicatedProfile() {
			return count;
		}
		public String getKeyProfileId() {
			return keyProfileId;
		}
		
	}
	


	/**
	 * the main function to de-duplicate data
	 * 
	 * @param sourceProfile
	 * @return
	 */
	public final static ResolutioResult profileDeduplication(Profile sourceProfile) {
		return new IdentityResolutionManagement().deduplicate(sourceProfile);
	}

	/**
	 * Finds and merges profiles matching the source profile's identity fields.
	 */
	public ResolutioResult deduplicate(Profile sourceProfile) {
		Objects.requireNonNull(sourceProfile, "sourceProfile");
		String sourceProfileId = sourceProfile.getId();
		ProfileFilter filter = filterFactory.create(sourceProfile);

		logger.info("\n ==> [pivotProfile] " + sourceProfileId + " " + sourceProfile.getFirstName());
		List<Profile> profiles = profileFinder.find(filter);
		int size = profiles == null ? 0 : profiles.size();
		logger.info("\n ==> allProfilesToMerge size = " + size);

		if (size <= 1) {
			return new ResolutioResult(0, sourceProfileId);
		}

		Profile targetProfile = profiles.get(0);
		List<Profile> candidates = new ArrayList<>(profiles.subList(1, size));
		int mergeResult = profileMerger.merge(targetProfile, candidates);
		if (mergeResult > 0) {
			profileReportCache.clear();
		}
		return new ResolutioResult(mergeResult, sourceProfileId);
	}

	/**
	 * @param primaryProfile
	 * @param candidates
	 * @param compareDataQualityScore
	 * @return
	 */
	static int buildFinalListAndMerge(Profile targetProfile, List<Profile> candidates) {
		if (targetProfile != null && !candidates.isEmpty()) {
			return ProfileMergeService.mergeProfileData(targetProfile, candidates);
		}
		return 0;
	}
	

	
	/**
	 * this method is used to merge profile to the final profile
	 * 
	 * @param destProfile
	 * @param toBeMergedProfile
	 * @return
	 */
	public final static ProfileSingleView mergeProfileData(ProfileSingleView finalProfile, ProfileSingleView toBeMergedProfile) {
		if(finalProfile == null && toBeMergedProfile != null) {
			return toBeMergedProfile;
		}
		else if(finalProfile != null && toBeMergedProfile == null) {
			return finalProfile;
		}
		else {
			ProfileMergeService.mergeProfileData(finalProfile, Arrays.asList(toBeMergedProfile));
		}				
		return finalProfile;
	}

	
	
}
