package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.dao.DailyReportUnitDaoUtil;
import leotech.cdp.dao.FeedbackDataDao;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.TrackingEventDao;
import leotech.cdp.domain.processor.UpdateProfileEventProcessor;
import leotech.cdp.job.reactive.JobUpdateProfileSingleView;
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
	public static ResolutioResult profileDeduplication(Profile sourceProfile) {
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
			return mergeProfileData(targetProfile, candidates);
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
	public static ProfileSingleView mergeProfileData(ProfileSingleView finalProfile, ProfileSingleView toBeMergedProfile) {
		if(finalProfile == null && toBeMergedProfile != null) {
			return toBeMergedProfile;
		}
		else if(finalProfile != null && toBeMergedProfile == null) {
			return finalProfile;
		}
		else {
			mergeProfileData(finalProfile, Arrays.asList(toBeMergedProfile));
		}				
		return finalProfile;
	}

	/**
	 * this method is used to unify duplicated profiles into the unique profile
	 * 
	 * @param destProfileSingle
	 * @param listToBeUnified
	 * @param mergeCode
	 */
	public static int mergeProfileData(Profile destProfile, List<Profile> listToBeUnified) {
		String destProfileId = destProfile.getId();
		ProfileSingleView finalProfile = null;
		
		boolean checkToDo = destProfile != null && listToBeUnified.size() > 0;
		if (checkToDo) {
			for (Profile srcProfile : listToBeUnified) {
				String sourceProfileId = srcProfile.getId();
				boolean isNotSameProfile = ! sourceProfileId.equals(destProfileId);
				
				if ( isNotSameProfile ) {
					// the final to be merged
					finalProfile = ProfileDaoUtil.getProfileById(destProfileId);
					
					// identities
					finalProfile.setIdentities(srcProfile.getIdentities());
					
					// visitorId
					finalProfile.mergeWebVisitorId(srcProfile.getVisitorId(), srcProfile.getTotalLeadScore());
					
					if( !finalProfile.isDataVerification() && srcProfile.isDataVerification()) {
						finalProfile.setDataVerification(true);
					}
					
					// fintechSystemIDs
					finalProfile.setFintechSystemIDs(srcProfile.getFintechSystemIDs());
					
					// applicationIDs
					finalProfile.setApplicationIDs(srcProfile.getApplicationIDs());
					
					// crmRefId
					finalProfile.setIdentities(srcProfile.getCrmRefId());
					if ( StringUtil.isEmpty(finalProfile.getCrmRefId()) &&  StringUtil.isNotEmpty(srcProfile.getCrmRefId())) {
						finalProfile.setCrmRefId(srcProfile.getCrmRefId());
					}
					finalProfile.setApplicationID(finalProfile.getCrmRefId(), false);
					finalProfile.setApplicationID(srcProfile.getCrmRefId(), false);
					
					// personaUri
					if ( StringUtil.isEmpty(finalProfile.getPersonaUri()) && StringUtil.isNotEmpty(srcProfile.getPersonaUri())) {
						finalProfile.setPersonaUri(srcProfile.getPersonaUri());
					}
					
					// dataLabels
					finalProfile.setDataLabels(srcProfile.getDataLabels());
					
					// notes
					finalProfile.appendNotes(srcProfile.getNotes());
					
					// inSegments
					finalProfile.setInSegments(srcProfile.getInSegments());
					
					// inAccounts
					finalProfile.setInAccounts(srcProfile.getInAccounts());
					
					// topEngagedTouchpointIds
					finalProfile.setTopEngagedTouchpointIds(srcProfile.getTopEngagedTouchpointIds());
					
					// lastObserverId
					if ( StringUtil.isEmpty(finalProfile.getLastObserverId()) && StringUtil.isNotEmpty(srcProfile.getLastObserverId())) {
						finalProfile.setLastObserverId(srcProfile.getLastObserverId());
					}
					// lastTouchpointId
					if ( StringUtil.isEmpty(finalProfile.getLastTouchpointId()) && StringUtil.isNotEmpty(srcProfile.getLastTouchpointId())) {
						finalProfile.setLastTouchpointId(srcProfile.getLastTouchpointId());
					}
					
					// lastTouchpoint
					if ( finalProfile.getLastTouchpoint() == null && srcProfile.getLastTouchpoint() != null) {
						finalProfile.setLastTouchpoint(srcProfile.getLastTouchpoint());
					}
					
					// lastSeenIp
					if ( StringUtil.isEmpty(finalProfile.getLastSeenIp()) && StringUtil.isNotEmpty(srcProfile.getLastSeenIp()) ) {
						finalProfile.setLastSeenIp(srcProfile.getLastSeenIp());
					}
					
					// lastUsedDeviceId
					if(  StringUtil.isEmpty(finalProfile.getLastUsedDeviceId()) && StringUtil.isNotEmpty(srcProfile.getLastUsedDeviceId()) ){
						finalProfile.setLastUsedDeviceId(srcProfile.getLastUsedDeviceId());
					}
					
					// webCookies
					if( StringUtil.isEmpty(finalProfile.getWebCookies()) && StringUtil.isNotEmpty(srcProfile.getWebCookies()) ){
						finalProfile.setWebCookies(srcProfile.getWebCookies());
					}
					
					// usedDeviceIds
					finalProfile.setUsedDeviceIds(srcProfile.getUsedDeviceIds());
					
					// primaryUsername
					if( StringUtil.isEmpty(finalProfile.getPrimaryUsername()) && StringUtil.isNotEmpty(srcProfile.getPrimaryUsername()) ){
						finalProfile.setPrimaryUsername(srcProfile.getPrimaryUsername());
					}
					
					// password
					if( StringUtil.isEmpty(finalProfile.getPassword()) && StringUtil.isNotEmpty(srcProfile.getPassword()) ){
						finalProfile.setPassword(srcProfile.getPassword());
					}
					
					// firstName
					if ( StringUtil.isEmpty(finalProfile.getFirstName()) && StringUtil.isNotEmpty(srcProfile.getFirstName()) ) {
						finalProfile.setFirstName(srcProfile.getFirstName());
					}
					
					// middleName
					if ( StringUtil.isEmpty(finalProfile.getMiddleName()) && StringUtil.isNotEmpty(srcProfile.getMiddleName()) ) {
						finalProfile.setMiddleName(srcProfile.getMiddleName());
					}

					// lastName
					if ( StringUtil.isEmpty(finalProfile.getLastName()) && StringUtil.isNotEmpty(srcProfile.getLastName()) ) {
						finalProfile.setLastName(srcProfile.getLastName());
					}
					
					// gender
					if ( !finalProfile.hasGenderData() && srcProfile.hasGenderData() ) {
						finalProfile.setGender(srcProfile.getGender());
					}
					
					// age
					if (finalProfile.getAge() == 0 && srcProfile.getAge() > 0) {
						finalProfile.setAge(srcProfile.getAge());
					}
					
					// dateOfBirth
					if( finalProfile.getDateOfBirth() == null &  srcProfile.getDateOfBirth() != null) {
						finalProfile.setDateOfBirth(srcProfile.getDateOfBirth());
					}
					
					// primaryEmail
					String primaryEmailOfSrcProfile = srcProfile.getPrimaryEmail();
					if(StringUtil.isNotEmpty(primaryEmailOfSrcProfile)) {
						if (StringUtil.isEmpty(finalProfile.getPrimaryEmail())) {
							finalProfile.setPrimaryEmail(primaryEmailOfSrcProfile);
						}
						else {
							finalProfile.setSecondaryEmails(primaryEmailOfSrcProfile);
						}
					}
					
					// secondaryEmails
					finalProfile.setSecondaryEmails(srcProfile.getSecondaryEmails());
					
					// primaryPhone
					String primaryPhoneOfSrcProfile = srcProfile.getPrimaryPhone();
					if(StringUtil.isNotEmpty(primaryPhoneOfSrcProfile)) {
						if (StringUtil.isEmpty(finalProfile.getPrimaryPhone()) ) {
							finalProfile.setPrimaryPhone(primaryPhoneOfSrcProfile);
						}
						else {
							finalProfile.setSecondaryPhones(primaryPhoneOfSrcProfile);
						}
					}
					
					// secondaryPhones
					finalProfile.setSecondaryPhones(srcProfile.getSecondaryPhones());
					
					// primaryAvatar
					finalProfile.setPrimaryAvatar(srcProfile.getPrimaryAvatar());
					
					
					// primaryNationality
					if ( StringUtil.isEmpty(finalProfile.getPrimaryNationality()) && StringUtil.isNotEmpty(srcProfile.getPrimaryNationality())) {
						finalProfile.setPrimaryNationality(srcProfile.getPrimaryNationality());
					}

					// permanentLocation
					if ( StringUtil.isEmpty(finalProfile.getPermanentLocation()) && StringUtil.isNotEmpty(srcProfile.getPermanentLocation())) {
						finalProfile.setPermanentLocation(srcProfile.getPermanentLocation());
					}

					// livingProvince
					if ( StringUtil.isEmpty(finalProfile.getLivingProvince()) && StringUtil.isNotEmpty(srcProfile.getLivingProvince())) {
						finalProfile.setLivingProvince(srcProfile.getLivingProvince());
					}

					// livingCounty
					if ( StringUtil.isEmpty(finalProfile.getLivingCounty()) && StringUtil.isNotEmpty(srcProfile.getLivingCounty())) {
						finalProfile.setLivingCounty(srcProfile.getLivingCounty());
					}

					// livingDistrict
					if ( StringUtil.isEmpty(finalProfile.getLivingDistrict()) && StringUtil.isNotEmpty(srcProfile.getLivingDistrict())) {
						finalProfile.setLivingDistrict(srcProfile.getLivingDistrict());
					}
					
					// livingLocation and locationCode
					String updateLivingLocation = srcProfile.getLivingLocation();
					if (StringUtil.isEmpty(finalProfile.getLivingLocation()) && StringUtil.isNotEmpty(updateLivingLocation)) {
						finalProfile.setLivingLocation(updateLivingLocation);
					}
					
					// locationCode
					if(StringUtil.isEmpty(finalProfile.getLocationCode()) && StringUtil.isNotEmpty(srcProfile.getLocationCode())){
						finalProfile.setLocationCode(srcProfile.getLocationCode());
					}
					
					// livingCountry
					if(StringUtil.isEmpty(finalProfile.getLivingCountry()) && StringUtil.isNotEmpty(srcProfile.getLivingCountry())){
						finalProfile.setLivingCountry(srcProfile.getLivingCountry());
					}
					
					// livingState
					if( StringUtil.isEmpty(finalProfile.getLivingState()) &&  StringUtil.isNotEmpty(srcProfile.getLivingState()) ){
						finalProfile.setLivingState(srcProfile.getLivingState());
					}
					
					// livingCity
					if( StringUtil.isEmpty(finalProfile.getLivingCity()) && StringUtil.isNotEmpty(srcProfile.getLivingCity()) ){
						finalProfile.setLivingCity(srcProfile.getLivingCity());
					}
					
					// livingLocationType
					if ( StringUtil.isEmpty(finalProfile.getLivingLocationType()) &&  StringUtil.isNotEmpty(srcProfile.getLivingLocationType())) {
						finalProfile.setLivingLocationType(srcProfile.getLivingLocationType());
					}
					
					// livingWard
					if ( StringUtil.isEmpty(finalProfile.getLivingWard()) &&  StringUtil.isNotEmpty(srcProfile.getLivingWard())) {
						finalProfile.setLivingWard(srcProfile.getLivingWard());
					}
					
					// currentZipCode
					if( StringUtil.isEmpty(finalProfile.getCurrentZipCode()) && StringUtil.isNotEmpty(srcProfile.getCurrentZipCode()) ){
						finalProfile.setCurrentZipCode(srcProfile.getCurrentZipCode());
					}
					
					// fromTouchpointHubIds
					finalProfile.setFromTouchpointHubIds(srcProfile.getFromTouchpointHubIds());
					
					// personalAvatars
					finalProfile.setPersonalAvatars(srcProfile.getPersonalAvatars());
					
					// personalContacts
					finalProfile.setPersonalContacts(srcProfile.getPersonalContacts());
					
					// businessContacts
					finalProfile.setBusinessContacts(srcProfile.getBusinessContacts());
					
					// socialMediaProfiles
					finalProfile.setSocialMediaProfiles(srcProfile.getSocialMediaProfiles());

					// notificationUserIds
					Map<String, Set<String>> notifyIds = finalProfile.getNotificationUserIds();
					if (notifyIds.isEmpty() && !srcProfile.getNotificationUserIds().isEmpty()) {
						finalProfile.setNotificationUserIds(srcProfile.getNotificationUserIds());
					}
					
					// personalProblems
					finalProfile.setPersonalProblems(srcProfile.getPersonalProblems());
					
					// personalInterests
					finalProfile.setPersonalInterests(srcProfile.getPersonalInterests());
					
					// solutionsForCustomer
					finalProfile.setSolutionsForCustomer(srcProfile.getSolutionsForCustomer());
					
					// nextBestActions
					finalProfile.setNextBestActions(srcProfile.getNextBestActions());
					
					// shoppingItems and shoppingItemIds
					finalProfile.setShoppingItems(srcProfile.getShoppingItems());
					
					// purchasedItems and purchasedItemIds
					finalProfile.setPurchasedItems(srcProfile.getPurchasedItems());
					
					// purchasedBrands
					finalProfile.setPurchasedBrands(srcProfile.getPurchasedBrands());
					
					// financeRecords
					finalProfile.setFinanceRecords(srcProfile.getFinanceRecords());
					
					// mediaChannels
					finalProfile.setMediaChannels(srcProfile.getMediaChannels());
					
					// contentKeywords
					finalProfile.setContentKeywords(srcProfile.getContentKeywords());
					
					// productKeywords
					finalProfile.setProductKeywords(srcProfile.getProductKeywords());
					
					// learningHistory
					finalProfile.setLearningHistory(srcProfile.getLearningHistory());

					// jobTitles
					finalProfile.setJobTitles(srcProfile.getJobTitles());

					// workingHistory
					finalProfile.setWorkingHistory(srcProfile.getWorkingHistory());

					// businessIndustries
					finalProfile.setBusinessIndustries(srcProfile.getBusinessIndustries());

					// businessData
					finalProfile.setBusinessData(srcProfile.getBusinessData());
					
					// saleAgencies
					finalProfile.setSaleAgencies(srcProfile.getSaleAgencies());
					
					// saleAgents
					finalProfile.setSaleAgents(srcProfile.getSaleAgents());
					
					// referrerChannels
					finalProfile.setReferrerChannels(srcProfile.getReferrerChannels());
					
					// weeklyMobileUsage
					finalProfile.setWeeklyMobileUsage(srcProfile.getWeeklyMobileUsage());
					
					// subscribedChannels
					finalProfile.setSubscribedChannels(srcProfile.getSubscribedChannels());

					// behavioralEvents
					finalProfile.setBehavioralEvents(srcProfile.getBehavioralEvents());
					
					// extAttributes
					finalProfile.setExtAttributes(srcProfile.getExtAttributes());
					
					// consent
					
					// receiveAds
					if(finalProfile.getReceiveAds() == 0 && srcProfile.getReceiveAds() > 0) {
						finalProfile.setReceiveAds(srcProfile.getReceiveAds());	
					}
					
					// receiveSMS
					if(finalProfile.getReceiveSMS() == 0 &&  srcProfile.getReceiveSMS() > 0) {
						finalProfile.setReceiveSMS(srcProfile.getReceiveSMS());	
					}
					
					// receiveEmail
					if(finalProfile.getReceiveEmail() == 0 && srcProfile.getReceiveEmail() > 0) {
						finalProfile.setReceiveEmail(srcProfile.getReceiveEmail());	
					}
					
					// receiveAppPush
					if( finalProfile.getReceiveAppPush() == 0 && srcProfile.getReceiveAppPush() > 0) {
						finalProfile.setReceiveAppPush(srcProfile.getReceiveAppPush());	
					}
					
					// receiveWebPush
					if( finalProfile.getReceiveWebPush() == 0 && srcProfile.getReceiveWebPush() > 0) {
						finalProfile.setReceiveWebPush(srcProfile.getReceiveWebPush());	
					}
					
					// only updated metrics by customer can be merged directly

					// BEGIN Merge non-re-computation metrics --------------


					// extMetrics
					finalProfile.setExtMetrics(srcProfile.getExtMetrics());
					
					// authorizedViewers
					finalProfile.setAuthorizedViewers(srcProfile.getAuthorizedViewers());
					
					// authorizedEditors
					finalProfile.setAuthorizedEditors(srcProfile.getAuthorizedEditors());

					// END Merge non-re-computation metrics --------------

					
					// BEGIN COPY scores from external scoring model, non-computable from Event Metric
					if (finalProfile.getTotalTransactionValue() == 0 && srcProfile.getTotalTransactionValue() > 0) {
						finalProfile.setTotalTransactionValue(srcProfile.getTotalTransactionValue());
					}
					if (finalProfile.getTotalCAC() == 0 && srcProfile.getTotalCAC() > 0) {
						finalProfile.setTotalCAC(srcProfile.getTotalCAC());
					}
					if (finalProfile.getTotalCLV() == 0 && srcProfile.getTotalCLV() > 0) {
						finalProfile.setTotalCLV(srcProfile.getTotalCLV());
					}
					if (finalProfile.getTotalCreditScore() == 0 && srcProfile.getTotalCreditScore() > 0) {
						finalProfile.setTotalCreditScore(srcProfile.getTotalCreditScore());
					}
					if (finalProfile.getTotalEstimatedReach() == 0 && srcProfile.getTotalEstimatedReach() > 0) {
						finalProfile.setTotalEstimatedReach(srcProfile.getTotalEstimatedReach());
					}
					if (finalProfile.getRfmScore() == 0 && srcProfile.getRfmScore() > 0) {
						finalProfile.setRfmScore(srcProfile.getRfmScore());
					}
					if (finalProfile.getRfeScore() == 0 && srcProfile.getRfeScore() > 0) {
						finalProfile.setRfeScore(srcProfile.getRfeScore());
					}
					if (finalProfile.getChurnScore() == 0 && srcProfile.getChurnScore() > 0) {
						finalProfile.setChurnScore(srcProfile.getChurnScore());
					}
					// END COPY scores from external scoring model, non-computable from Event Metric
					
					// BEGIN Customer Experience Metric
					// totalCFS Customer Feedback Score CFS
					if (finalProfile.getTotalCFS() == 0 && srcProfile.getTotalCFS() != 0) {
						finalProfile.setTotalCFS(srcProfile.getTotalCFS());
					}
					if (finalProfile.getPositiveCFS() == 0 && srcProfile.getPositiveCFS() != 0) {
						finalProfile.setPositiveCFS(srcProfile.getPositiveCFS());
					}
					if (finalProfile.getNeutralCFS() == 0 && srcProfile.getNeutralCFS() != 0) {
						finalProfile.setNeutralCFS(srcProfile.getNeutralCFS());
					}
					if (finalProfile.getNegativeCFS() == 0 && srcProfile.getNegativeCFS() != 0) {
						finalProfile.setNegativeCFS(srcProfile.getNegativeCFS());
					}
					finalProfile.setTimeseriesCFS(srcProfile.getTimeseriesCFS());

					// totalCSAT Customer Satisfaction Score CSAT
					if (finalProfile.getTotalCSAT() == 0 && srcProfile.getTotalCSAT() != 0) {
						finalProfile.setTotalCSAT(srcProfile.getTotalCSAT());
					}
					if (finalProfile.getPositiveCSAT() == 0 && srcProfile.getPositiveCSAT() != 0) {
						finalProfile.setPositiveCSAT(srcProfile.getPositiveCSAT());
					}
					if (finalProfile.getNeutralCSAT() == 0 && srcProfile.getNeutralCSAT() != 0) {
						finalProfile.setNeutralCSAT(srcProfile.getNeutralCSAT());
					}
					if (finalProfile.getNegativeCSAT() == 0 && srcProfile.getNegativeCSAT() != 0) {
						finalProfile.setNegativeCSAT(srcProfile.getNegativeCSAT());
					}
					finalProfile.setTimeseriesCSAT(srcProfile.getTimeseriesCSAT());

					// totalCES Customer Effort Score
					if (finalProfile.getTotalCES() == 0 && srcProfile.getTotalCES() != 0) {
						finalProfile.setTotalCES(srcProfile.getTotalCES());
					}
					if (finalProfile.getPositiveCES() == 0 && srcProfile.getPositiveCES() != 0) {
						finalProfile.setPositiveCES(srcProfile.getPositiveCES());
					}
					if (finalProfile.getNeutralCES() == 0 && srcProfile.getNeutralCES() != 0) {
						finalProfile.setNeutralCES(srcProfile.getNeutralCES());
					}
					if (finalProfile.getNegativeCES() == 0 && srcProfile.getNegativeCES() != 0) {
						finalProfile.setNegativeCES(srcProfile.getNegativeCES());
					}
					finalProfile.setTimeseriesCES(srcProfile.getTimeseriesCES());

					// totalNPS Net Promoter Score (NPS)
					if (finalProfile.getTotalNPS() == 0 && srcProfile.getTotalNPS() != 0) {
						finalProfile.setTotalNPS(srcProfile.getTotalNPS());
					}
					if (finalProfile.getPositiveNPS() == 0 && srcProfile.getPositiveNPS() != 0) {
						finalProfile.setPositiveNPS(srcProfile.getPositiveNPS());
					}
					if (finalProfile.getNeutralNPS() == 0 && srcProfile.getNeutralNPS() != 0) {
						finalProfile.setNeutralNPS(srcProfile.getNeutralNPS());
					}
					if (finalProfile.getNegativeNPS() == 0 && srcProfile.getNegativeNPS() != 0) {
						finalProfile.setNegativeNPS(srcProfile.getNegativeNPS());
					}
					finalProfile.setTimeseriesNPS(srcProfile.getTimeseriesNPS());
					
					// END Customer Experience Metric
					
					// ContextSession 
					ContextSessionManagement.updateContextSessionToMergeProfile(srcProfile, finalProfile);

					// Tracking event
					TrackingEventDao.mergeTrackingEventToNewProfile(sourceProfileId, destProfileId);
					
					// Feedback Event
					FeedbackDataDao.mergeFeedbackEventToNewProfile(sourceProfileId, destProfileId);
					
					// re-computation to update inJourneyMaps
					finalProfile = UpdateProfileEventProcessor.recomputationFromEventStream(finalProfile);
					
					// Daily Report Units
					DailyReportUnitDaoUtil.updateDailyReportUnitsToNewProfile(sourceProfileId, destProfileId);
					
					// Profile Graph for Purchased data and Personalization
					ProfileGraphManagement.updateEdgeDataProfileToProfile(sourceProfileId, destProfileId);
					
					// FINAL STEP, MERGE DUPLICATE DATA
					destProfileId = ProfileDaoUtil.updateMergedProfile(srcProfile, destProfileId);
					
				} else {
					logger.info("isSameProfile, skip profile" + sourceProfileId);
				}
			}
			
			// after merge need a fully updating data process
			if(finalProfile != null && destProfileId != null) {
				String finalProfileId = ProfileDaoUtil.saveProfile(finalProfile);
				if(finalProfileId != null) {
					JobUpdateProfileSingleView.job().enqueue(finalProfileId);
					logger.info("==> Update finalProfileId  " + finalProfileId);
					return listToBeUnified.size();
				}
			}
		}
		return 0;
	}
}
