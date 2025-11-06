package leotech.cdp.domain;

import java.util.List;

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
import rfx.core.util.StringUtil;

/**
 * this Service is used to unify duplicated profiles into the unique profile
 * 
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public class ProfileMergeService {

    protected static final Logger logger = LoggerFactory.getLogger(ProfileMergeService.class);

    protected ProfileMergeService() {}

    /**
	 * this method is used to unify duplicated profiles into the unique profile
	 * 
	 * @param destProfileSingle
	 * @param listToBeUnified
	 */
    public static int mergeProfileData(Profile destProfile, List<Profile> listToBeUnified) {
        if (destProfile == null || listToBeUnified == null || listToBeUnified.isEmpty()) {
            return 0;
        }

        String destProfileId = destProfile.getId();
        ProfileSingleView finalProfile = ProfileDaoUtil.getProfileById(destProfileId);

        for (Profile srcProfile : listToBeUnified) {
            if (isSameProfile(destProfileId, srcProfile)) {
                logger.info("Skipping identical profile: {}", srcProfile.getId());
                continue;
            }
            mergeSingleProfile(finalProfile, srcProfile);
            performSideEffects(srcProfile, destProfileId, finalProfile);
        }

        return finalizeMerge(finalProfile, listToBeUnified.size());
    }

    // ----------------- Core merge logic -----------------

    protected static boolean isSameProfile(String destId, Profile src) {
        return src.getId().equals(destId);
    }

    protected static void mergeSingleProfile(ProfileSingleView target, Profile src) {
        mergeIdentityData(target, src);
        mergePersonalInfo(target, src);
        mergeContactInfo(target, src);
        mergeDemographicInfo(target, src);
        mergeLocationInfo(target, src);
        mergeBehavioralAndBusinessData(target, src);
        mergeScores(target, src);
        mergeConsentSettings(target, src);
    }

    // ----------------- Section merges -----------------

    protected static void mergeIdentityData(ProfileSingleView target, Profile src) {
        target.setIdentities(src.getIdentities());
        target.mergeWebVisitorId(src.getVisitorId(), src.getTotalLeadScore());
        if (!target.isDataVerification() && src.isDataVerification()) {
            target.setDataVerification(true);
        }
        System.out.println("target " + target.getCrmRefId());
        System.out.println("src " + src.getCrmRefId());
        if (StringUtil.isEmpty(target.getCrmRefId()) && StringUtil.isNotEmpty(src.getCrmRefId())) {
            target.setCrmRefId(src.getCrmRefId());
        }
    }

    protected static void mergePersonalInfo(ProfileSingleView target, Profile src) {
        if (StringUtil.isEmpty(target.getFirstName())) target.setFirstName(src.getFirstName());
        if (StringUtil.isEmpty(target.getLastName())) target.setLastName(src.getLastName());
        if (!target.hasGenderData() && src.hasGenderData()) target.setGender(src.getGender());
        if (target.getAge() == 0 && src.getAge() > 0) target.setAge(src.getAge());
        if (target.getDateOfBirth() == null && src.getDateOfBirth() != null)
            target.setDateOfBirth(src.getDateOfBirth());
    }

    public static void mergeContactInfo(ProfileSingleView target, Profile src) {
        String srcEmail = src.getPrimaryEmail();
        if (StringUtil.isNotEmpty(srcEmail)) {
            if (StringUtil.isEmpty(target.getPrimaryEmail())) target.setPrimaryEmail(srcEmail);
            else target.setSecondaryEmails(srcEmail);
        }
        target.setSecondaryEmails(src.getSecondaryEmails());

        String srcPhone = src.getPrimaryPhone();
        if (StringUtil.isNotEmpty(srcPhone)) {
            if (StringUtil.isEmpty(target.getPrimaryPhone())) target.setPrimaryPhone(srcPhone);
            else target.setSecondaryPhones(srcPhone);
        }
        target.setSecondaryPhones(src.getSecondaryPhones());
    }

    protected static void mergeDemographicInfo(ProfileSingleView target, Profile src) {
        if (StringUtil.isEmpty(target.getPrimaryNationality()))
            target.setPrimaryNationality(src.getPrimaryNationality());
        if (StringUtil.isEmpty(target.getPermanentLocation()))
            target.setPermanentLocation(src.getPermanentLocation());
    }

    protected static void mergeLocationInfo(ProfileSingleView target, Profile src) {
        if (StringUtil.isEmpty(target.getLivingCity()))
            target.setLivingCity(src.getLivingCity());
        if (StringUtil.isEmpty(target.getLocationCode()))
            target.setLocationCode(src.getLocationCode());
        if (StringUtil.isEmpty(target.getLivingCountry()))
            target.setLivingCountry(src.getLivingCountry());
    }

    protected static void mergeBehavioralAndBusinessData(ProfileSingleView target, Profile src) {
        target.setBehavioralEvents(src.getBehavioralEvents());
        target.setBusinessData(src.getBusinessData());
        target.setMediaChannels(src.getMediaChannels());
    }

    public static void mergeScores(ProfileSingleView target, Profile src) {
        if (target.getTotalTransactionValue() == 0 && src.getTotalTransactionValue() > 0)
            target.setTotalTransactionValue(src.getTotalTransactionValue());
        if (target.getTotalCLV() == 0 && src.getTotalCLV() > 0)
            target.setTotalCLV(src.getTotalCLV());
        if (target.getChurnScore() == 0 && src.getChurnScore() > 0)
            target.setChurnScore(src.getChurnScore());
    }

    protected static void mergeConsentSettings(ProfileSingleView target, Profile src) {
        if (target.getReceiveAds() == 0 && src.getReceiveAds() > 0)
            target.setReceiveAds(src.getReceiveAds());
        if (target.getReceiveEmail() == 0 && src.getReceiveEmail() > 0)
            target.setReceiveEmail(src.getReceiveEmail());
    }

    // ----------------- DAO + recomputation logic -----------------

    protected static void performSideEffects(Profile source, String destId, ProfileSingleView target) {
    	String sourceId = source.getId();
        ContextSessionManagement.updateContextSessionToMergeProfile(source, target);
        TrackingEventDao.mergeTrackingEventToNewProfile(sourceId, destId);
        FeedbackDataDao.mergeFeedbackEventToNewProfile(sourceId, destId);
        UpdateProfileEventProcessor.recomputationFromEventStream(target);
        DailyReportUnitDaoUtil.updateDailyReportUnitsToNewProfile(sourceId, destId);
        ProfileGraphManagement.updateEdgeDataProfileToProfile(sourceId, destId);
        ProfileDaoUtil.updateMergedProfile(source, destId);
    }

    protected static int finalizeMerge(ProfileSingleView finalProfile, int mergedCount) {
        if (finalProfile == null) return 0;
        String finalProfileId = ProfileDaoUtil.saveProfile(finalProfile);
        if (finalProfileId != null) {
            JobUpdateProfileSingleView.job().enqueue(finalProfileId);
            logger.info("Updated finalProfileId {}", finalProfileId);
            return mergedCount;
        }
        return 0;
    }
}
