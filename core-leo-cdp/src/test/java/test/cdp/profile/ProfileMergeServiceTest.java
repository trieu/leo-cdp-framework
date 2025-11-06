package test.cdp.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import leotech.cdp.domain.ProfileMergeService;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileSingleView;


/**
 * Unit Test for ProfileMergeService
 * 
 * @author Trieu Nguyen
 * @since 2025
 *
 */
class ProfileMergeServiceTest extends ProfileMergeService {

    private ProfileSingleView target;
    private Profile src;

    @BeforeEach
    void setUp() {
        target = new ProfileSingleView();
        target.setId("p123");
        src = new ProfileSingleView();
        src.setId("p456");
    }

    // ---------- mergeIdentityData ----------

    @Test
    void testMergeIdentityData_shouldCopyCrmRefIdIfEmpty() {
        src.setCrmRefId("CRM123");
        ProfileMergeService.mergeIdentityData(target, src);
        assertEquals("CRM123", target.getCrmRefId());
    }

    @Test
    void testMergeIdentityData_shouldSetDataVerificationTrue() {
        src.setDataVerification(true);
        ProfileMergeService.mergeIdentityData(target, src);
        assertTrue(target.isDataVerification());
    }

    // ---------- mergePersonalInfo ----------

    @Test
    void testMergePersonalInfo_shouldCopyMissingFields() {
        src.setFirstName("Alice");
        src.setLastName("Smith");
        src.setGender("female");
        src.setAge(30);

        ProfileMergeService.mergePersonalInfo(target, src);

        assertEquals("Alice", target.getFirstName());
        assertEquals("Smith", target.getLastName());
        assertEquals("female", target.getGenderAsText());
        assertEquals(30, target.getAge());
    }

    // ---------- mergeContactInfo ----------

    @Test
    void testMergeContactInfo_shouldSetPrimaryAndSecondaryEmail() {
        src.setPrimaryEmail("test@example.com");
        target.setPrimaryEmail("existing@domain.com");

        ProfileMergeService.mergeContactInfo(target, src);

        assertTrue(target.getSecondaryEmails().contains("test@example.com"));
    }

    @Test
    void testMergeContactInfo_shouldSetPrimaryPhoneIfEmpty() {
        src.setPrimaryPhone("12345");
        ProfileMergeService.mergeContactInfo(target, src);
        assertEquals("12345", target.getPrimaryPhone());
    }

    // ---------- mergeDemographicInfo ----------

    @Test
    void testMergeDemographicInfo_shouldCopyIfEmpty() {
        src.setPrimaryNationality("Vietnamese");
        src.setPermanentLocation("Hanoi");

        ProfileMergeService.mergeDemographicInfo(target, src);

        assertEquals("Vietnamese", target.getPrimaryNationality());
        assertEquals("Hanoi", target.getPermanentLocation());
    }

    // ---------- mergeLocationInfo ----------

    @Test
    void testMergeLocationInfo_shouldCopyLocationFields() {
        src.setLivingCity("Ho Chi Minh City");
        src.setLivingCountry("Vietnam");
        src.setLocationCode("VN-HCM");

        ProfileMergeService.mergeLocationInfo(target, src);

        assertEquals("Ho Chi Minh City", target.getLivingCity());
        assertEquals("Vietnam", target.getLivingCountry());
        assertEquals("VN-HCM", target.getLocationCode());
    }

    // ---------- mergeBehavioralAndBusinessData ----------

    @Test
    void testMergeBehavioralAndBusinessData_shouldReplaceLists() {
        src.setBehavioralEvents(Set.of("click", "view"));
        src.setBusinessData(Map.of("industry", Set.of("Retail")));
        src.setMediaChannels(Set.of("Facebook", "Zalo"));

        ProfileMergeService.mergeBehavioralAndBusinessData(target, src);

        assertTrue(target.getBehavioralEvents().contains("click"));
        assertEquals("Retail", target.getBusinessData().get("industry").iterator().next());
        assertTrue(target.getMediaChannels().contains("Zalo"));
    }

    // ---------- mergeScores ----------

    @Test
    void testMergeScores_shouldCopyNonZeroValues() {
        src.setTotalTransactionValue(999);
        src.setTotalCLV(888);
        src.setChurnScore(77);

        ProfileMergeService.mergeScores(target, src);

        assertEquals(999, target.getTotalTransactionValue());
        assertEquals(888, target.getTotalCLV());
        assertEquals(77, target.getChurnScore());
    }

    // ---------- mergeConsentSettings ----------

    @Test
    void testMergeConsentSettings_shouldCopyIfEmpty() {
        src.setReceiveAds(1);
        src.setReceiveEmail(1);

        ProfileMergeService.mergeConsentSettings(target, src);

        assertEquals(1, target.getReceiveAds());
        assertEquals(1, target.getReceiveEmail());
    }

    // ---------- High-level Integration ----------

    @Test
    void testMergeSingleProfile_shouldCombineAllData() {
        src.setPrimaryEmail("test@example.com");
        src.setFirstName("John");
        src.setPrimaryPhone("12345");
        src.setReceiveAds(1);

        ProfileMergeService.mergeSingleProfile(target, src);

        assertEquals("test@example.com", target.getPrimaryEmail());
        assertEquals("John", target.getFirstName());
        assertEquals("12345", target.getPrimaryPhone());
        assertEquals(1, target.getReceiveAds());
    }

    @Test
    void testIsSameProfile_shouldDetectEquality() {
    	Profile src2 = new ProfileSingleView();
    	src2.setId("123");
        assertTrue(ProfileMergeService.isSameProfile("123", src2));
        assertFalse(ProfileMergeService.isSameProfile("456", src2));
    }
}
