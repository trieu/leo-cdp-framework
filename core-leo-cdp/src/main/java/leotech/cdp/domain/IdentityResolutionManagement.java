package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.query.filters.ProfileFilter;
import leotech.cdp.utils.ProfileDataValidator;
import rfx.core.util.StringUtil;

/**
 * Identity Resolution for Profile
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class IdentityResolutionManagement {
	
	static Logger logger = LoggerFactory.getLogger(IdentityResolutionManagement.class);

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
		String sourceProfileId = sourceProfile.getId();
		String crmRefId = sourceProfile.getCrmRefId();
		String visitorId = sourceProfile.getVisitorId();
		
		String primaryEmail = sourceProfile.getPrimaryEmail();
		Set<String> secondaryEmails = sourceProfile.getSecondaryEmails();
		
		String primaryPhone = sourceProfile.getPrimaryPhone();
		Set<String> secondaryPhones = sourceProfile.getSecondaryPhones();
		
		String fingerprintId = sourceProfile.getFingerprintId();
		String lastSeenIp = sourceProfile.getLastSeenIp();
		String lastUsedDeviceId = sourceProfile.getLastUsedDeviceId();
		
		Set<String> applicationIDs = sourceProfile.getLoyaltyIDs();
		Set<String> loyaltyIDs = sourceProfile.getApplicationIDs();
		Set<String> fintechSystemIDs = sourceProfile.getFintechSystemIDs();
		Set<String> governmentIssuedIDs = sourceProfile.getGovernmentIssuedIDs();
		
		ProfileFilter srcProfileFilter = new ProfileFilter(sourceProfileId);
		
		// add filter to find sources
		
		// 1 Email
		List<String> emails = new ArrayList<String>();
		if (ProfileDataValidator.isValidEmail(primaryEmail)) {
			emails.add(primaryEmail);
		}
		emails.addAll(secondaryEmails);
		srcProfileFilter.setEmails(emails);
		
		// 2 Phones
		List<String> phones = new ArrayList<String>();
		if (ProfileDataValidator.isValidPhoneNumber(primaryPhone)) {
			phones.add(primaryPhone);
		}
		phones.addAll(secondaryPhones);
		srcProfileFilter.setPhones(phones);
		
		// 3 Crm ID
		if (StringUtil.isNotEmpty(crmRefId)) {
			srcProfileFilter.setCrmRefId(crmRefId);
		}
		// 4 Visitor ID
		if (StringUtil.isNotEmpty(visitorId)) {
			srcProfileFilter.setVisitorId(visitorId);
		}
		// 5 Fingerprint ID, last-seen IP and last-used device ID
		if (StringUtil.isNotEmpty(fingerprintId) && StringUtil.isNotEmpty(lastSeenIp) && StringUtil.isNotEmpty(lastUsedDeviceId)) {
			srcProfileFilter.setFingerprintId(fingerprintId);
			srcProfileFilter.setLastSeenIp(lastSeenIp);
			srcProfileFilter.setLastUsedDeviceId(lastUsedDeviceId);
		}
		// 8 Application IDs
		if ( applicationIDs.size() > 0) {
			srcProfileFilter.setApplicationIDs(new ArrayList<String>(applicationIDs));
		}
		// 9 Fintech systen IDs
		if ( fintechSystemIDs.size() > 0) {
			srcProfileFilter.setFintechSystemIDs(new ArrayList<String>(fintechSystemIDs));
		}
		// 10 Government issued IDs
		if ( governmentIssuedIDs.size() > 0) {
			srcProfileFilter.setGovernmentIssuedIDs(new ArrayList<String>(governmentIssuedIDs));
		}	
		// 11 Loyalty IDs
		if ( loyaltyIDs.size() > 0) {
			srcProfileFilter.setLoyaltyIDs(new ArrayList<String>(loyaltyIDs));
		}
		
		// TODO add dynamic query and improve for more identity fields
		
		logger.info("\n ==> [pivotProfile] " + sourceProfileId + " " + sourceProfile.getFirstName() );
		List<Profile> allProfilesOfOnePerson = ProfileDaoUtil.getProfilesByFilter(srcProfileFilter);

		int allProfilesOfOnePersonSize = allProfilesOfOnePerson.size();
		logger.info("\n ==> allProfilesOfOnePersonSize = " + allProfilesOfOnePersonSize );
		for (Profile p : allProfilesOfOnePerson) {
			logger.info("==> Profile Of One Person.ID = " + p.getId() );
		}
		
		int mergeResult = 0;
		// minimum is 2 to de-duplicate
		if(allProfilesOfOnePersonSize >= 2) {
			// pick the first profile that has maximum data quality score
			Profile targetProfile = allProfilesOfOnePerson.get(0);
			allProfilesOfOnePerson.remove(0);
			mergeResult = buildFinalListAndMerge(targetProfile, allProfilesOfOnePerson);
			
			if(mergeResult > 0) {
				Analytics360Management.clearCacheProfileReport();
			}
			return new ResolutioResult(mergeResult, sourceProfileId);
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
