package leotech.cdp.model.customer;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import leotech.cdp.dao.TrackingEventDao;
import leotech.cdp.domain.ActivationFlowManagement;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.AutoSetData;
import leotech.cdp.model.ExposeInSegmentList;
import leotech.cdp.model.ProfileMetaDataField;
import leotech.cdp.model.analytics.GoogleUTM;
import leotech.cdp.model.analytics.LastTrackingEventMap;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.utils.ProfileDataValidator;
import leotech.system.exception.InvalidDataException;
import leotech.system.util.IdGenerator;
import rfx.core.util.RandomUtil;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * 
 * Customer profile data entity to store human information
 * 
 * @author Trieu Nguyen (Thomas)
 * @since 2020
 *
 */
public class Profile extends AbstractProfile implements Comparable<Profile> {
	
	public static final String PROFILE_DEFAULT_AVATAR_PNG = "profile-default-avatar.png";
	public static final String COLLECTION_NAME = getCdpCollectionName(Profile.class);

	static ArangoCollection dbCol;
	
	@Override
	public ArangoCollection getDbCollection() {
		ArangoCollection collection = getCollection();
		if(collection == null) {
			throw new NullPointerException("ArangoCollection is NULL for collection name: " + COLLECTION_NAME);
		}
		return collection;
	}
	
	/**
	 * @return ArangoCollection
	 */
	public static ArangoCollection getCollection() {
		if (dbCol == null) {
			initCollectionAndIndex();
		}
		return dbCol;
	}

	public static void initCollectionAndIndex() {
		ArangoDatabase arangoDatabase = getArangoDatabase();
		dbCol = arangoDatabase.collection(COLLECTION_NAME);
		ProfileModelUtil.checkAndBuild(dbCol);
		
		// key for web visitor
		dbCol.ensurePersistentIndex(Arrays.asList("visitorId"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("fingerprintId"),createNonUniquePersistentIndex());	
		
		// core personal data
		dbCol.ensurePersistentIndex(Arrays.asList("firstName"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("lastName"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("livingLocation"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("permanentLocation"), createNonUniquePersistentIndex());
		
		// personal device access
		dbCol.ensurePersistentIndex(Arrays.asList("rootProfileId"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("visitorId","identities","status"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("lastUsedDeviceId","status"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("usedDeviceIds[*]","status"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("visitorIds[*]","status"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("governmentIssuedIDs[*]","status"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("applicationIDs[*]","status"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("bankingSystemIDs[*]","status"),createNonUniquePersistentIndex());
		
		// business data
		dbCol.ensurePersistentIndex(Arrays.asList("jobType"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("accountIds[*].id"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("softSkills[*]"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("studyCertificates[*]"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("jobTitles[*]"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("learningHistory[*]"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("workingHistory[*]"),createNonUniquePersistentIndex());
		
		// extends personal data
		dbCol.ensurePersistentIndex(Arrays.asList("age"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("gender"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("ageGroup"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("primaryUsername"),createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("primaryNationality"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("personalityTypes[*].type"),createNonUniquePersistentIndex());
		
		// location data
		dbCol.ensurePersistentIndex(Arrays.asList("livingCity"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("livingState"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("livingProvince"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("livingCounty"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("livingWard"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("livingDistrict"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("livingCountry"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("currentZipCode"), createNonUniquePersistentIndex());
		dbCol.ensurePersistentIndex(Arrays.asList("locationCode"),createNonUniquePersistentIndex());
	}


	@Expose
	@ProfileMetaDataField(identityResolutionKey = true, label="Web Visitor ID")
	@ExposeInSegmentList
	protected String visitorId = "";
	
	@Expose
	protected Set<String> visitorIds = new HashSet<>(20);
	
	@Expose
	@ProfileMetaDataField(identityResolutionKey = true, label="Fingerprint ID")
	@ExposeInSegmentList
	protected String fingerprintId = "";
	
	@Expose
	@ProfileMetaDataField(identityResolutionKey = true, dataQualityScore=5, label="Application IDs")
	@ExposeInSegmentList
	protected Set<String> applicationIDs = new HashSet<>(100);
	
	@Expose
	@ProfileMetaDataField(identityResolutionKey = true, dataQualityScore=5, label="Loyalty IDs")
	@ExposeInSegmentList
	protected Set<String> loyaltyIDs = new HashSet<>(100);
	
	@Expose
	@ProfileMetaDataField(identityResolutionKey = true, dataQualityScore=5, label="Fintech System IDs")
	@ExposeInSegmentList
	protected Set<String> fintechSystemIDs = new HashSet<>(100);
	
	@Expose
	protected String lastUsedDeviceId = "";
	
	@Expose
	@ProfileMetaDataField(dataQualityScore=5, label="Used Device IDs")
	protected Set<String> usedDeviceIds = new HashSet<>(20);
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(identityResolutionKey = true, dataQualityScore=50, label="Government-issued IDs")
	@ExposeInSegmentList
	protected Set<String> governmentIssuedIDs = new HashSet<>(50);
	
	// --- BEGIN metadata of business engagement
	
	@Expose	
	protected String webCookies = "";
	
	// --- END metadata of business engagement
	
	// --- CDP for user and customer 
	
	// --- BEGIN key Personal attributes
	
	@Expose
	@ProfileMetaDataField(dataQualityScore=5, label="Primary Username")
	protected String primaryUsername = "";

	// sensitive data
	protected String password = "";
	
	@Expose
	protected Map<String,Date> contextSessionKeys = new HashMap<>(100);
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="First Name", synchronizable = true)
	@ExposeInSegmentList
	protected String firstName = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Middle Name")
	@ExposeInSegmentList
	protected String middleName = "";

	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Last Name", synchronizable = true)
	@ExposeInSegmentList
	protected String lastName = "";
	
	/**
	 * 0: Female, 1: Male, 2: LGBT, 3: Lesbian, 4: Gay, 5: Bisexual, 6: Transgender, 7: Unknown
	 */
	@Expose
	@AutoSetData(setDataAsInteger = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Gender", invalidWhenEqual = "7")
	@ExposeInSegmentList
	protected int gender = ProfileGenderCode.UNKNOWN; 
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected String genderAsText;
	
	@Expose
	@AutoSetData(setDataAsInteger = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Marital Status")
	@ExposeInSegmentList
	protected int maritalStatus = ProfileMaritalCode.Unknown;	
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected String maritalStatusAsText;

	@Expose
	@AutoSetData(setDataAsInteger = true)
	@ExposeInSegmentList
	protected int genderProbability = 50;
	
	@Expose
	@AutoSetData(setDataAsInteger = true)
	@ProfileMetaDataField(dataQualityScore=3, label="Age")
	@ExposeInSegmentList
	protected int age = 0;
	
	@Expose
	@AutoSetData(setDataAsInteger = true)
	@ProfileMetaDataField(dataQualityScore=3, label="Age Group")
	@ExposeInSegmentList
	protected int ageGroup = ProfileAgeGroup.NO_DATA;
	
	/**
	 * a profile can have 5 personality types, the default is the first
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=20, label="Personality Types")
	protected Set<PersonalityType> personalityTypes = new HashSet<>(5);
	
	@Expose
	@AutoSetData(setDataAsDate = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Date Of Birth")
	@ExposeInSegmentList
	protected Date dateOfBirth = null;

	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Primary Image Avatar")
	@ExposeInSegmentList
	protected String primaryAvatar = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Primary Nationality")
	@ExposeInSegmentList
	protected String primaryNationality = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Housing Type")
	@ExposeInSegmentList
	protected String housingType = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Location Code")
	@ExposeInSegmentList
	protected String locationCode = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Living Location Type")
	@ExposeInSegmentList
	protected String livingLocationType = "";// CURRES or PERMNENT
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Permanent Location")
	@ExposeInSegmentList
	protected String permanentLocation = "";// the home Address

	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Living Location")
	@ExposeInSegmentList
	protected String livingLocation = "";// Residential Address
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Living Country")
	@ExposeInSegmentList
	protected String livingCountry = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Living Ward")
	@ExposeInSegmentList
	protected String livingWard = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Living District")
	@ExposeInSegmentList
	protected String livingDistrict = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Living City")
	@ExposeInSegmentList
	protected String livingCity = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Living State")
	@ExposeInSegmentList
	protected String livingState = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Living Province")
	@ExposeInSegmentList
	protected String livingProvince = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Living County")
	@ExposeInSegmentList
	protected String livingCounty = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Current Living Zip Code")
	@ExposeInSegmentList
	protected String currentZipCode = "";
	
	/**
	 * the history of all living locations or contact address
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=5, label="Contact Addresses")
	@ExposeInSegmentList
	protected Set<ContactAddress> contactAddresses = new HashSet<ContactAddress>(10);
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=10, label="Personal Photo Avatars")
	protected Set<String> personalAvatars = new HashSet<>(10);
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Personal Interests")
	@ExposeInSegmentList
	protected Set<String> personalInterests = new HashSet<>(50);
	
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Personal Contacts")
	@ExposeInSegmentList
	protected Map<String, String> personalContacts = new HashMap<>(20);

	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Business Contacts")
	@ExposeInSegmentList
	protected Map<String, String> businessContacts = new HashMap<>(20);
	
	// --- END key Personal attributes
	
	// --- BEGIN Business Data Model
	
	// education
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Learning Courses")
	@ExposeInSegmentList
	protected Set<LearningCourse> learningCourses = new HashSet<>(20);
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Soft Skills")
	@ExposeInSegmentList
	protected Set<String> softSkills = new HashSet<>(50);

	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Learning History")
	@ExposeInSegmentList
	protected Set<String> learningHistory = new HashSet<>(50);
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Study Certificates")
	@ExposeInSegmentList
	protected Set<String> studyCertificates = new HashSet<>(50);
	
	// working
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Job Titles")
	@ExposeInSegmentList
	protected Set<String> jobTitles = new HashSet<>(50);
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Working History")
	@ExposeInSegmentList
	protected Set<String> workingHistory = new HashSet<>(50);
	
	
	// Job (numeric: 0 - unskilled and non-resident, 1 - unskilled and resident, 2 - skilled, 3 - highly skilled)
	@Expose
	@AutoSetData(setDataAsInteger = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Job Type", invalidWhenEqual="-1")
	@ExposeInSegmentList
	protected int jobType = -1; 
	
	/**
	 * income history: 2007-2009 => 9000000
	 */
	@Expose
	@AutoSetData(setDataAsJsonMap  = true)
	@ProfileMetaDataField(dataQualityScore=10, label="Income History")
	@ExposeInSegmentList
	protected Map<String, Double> incomeHistory = new HashMap<>(150);
	
	/**
	 * statistics summary about personal mobile usage
	 */
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Weekly Mobile Usage")
	@ExposeInSegmentList
	protected Map<String, Integer> weeklyMobileUsage = new HashMap<>(7);
	
	// --- END Business Data Model
	
	@Expose
	@ExposeInSegmentList
	protected TrackingEvent lastTrackingEvent = null;
	
	@Expose
	@ExposeInSegmentList
	protected TrackingEvent lastItemViewEvent = null;
	
	@Expose
	@ExposeInSegmentList
	protected TrackingEvent lastPurchaseEvent = null;
	
	@Override
	public int compareTo(Profile o) {
		long myValue = this.getTotalValueOfData();
		long otherValue = o.getTotalValueOfData();
		if(myValue > otherValue) {
			return 1;
		}
		else if(myValue < otherValue) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean dataValidation() {
		return this.id != null && this.visitorId != null && this.lastObserverId != null && this.lastTouchpointId != null && this.identities.size() > 0 && ProfileDataValidator.isValidBirthDate(this.dateOfBirth);
	}
	

	
	@Override
	public String getDocumentUUID() {
		return getDocumentUUID(this.id);
	}
	
	public static String getDocumentUUID(String id) {
		return COLLECTION_NAME + "/" + id;
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if(this.identities.size() == 0) {
			newIllegalArgumentException("identities of Profile must not be empty");
		}
		if (StringUtil.isEmpty(this.id)) {
			String keyHint = System.nanoTime() + String.join("_", this.identities) + RandomUtil.getRandom(Integer.MAX_VALUE) + "-" + this.type;
			this.id = createId(this.id, keyHint);
			this.partitionId = createPartitionId(id, Profile.class);
		}
		if(StringUtil.isEmpty(this.visitorId)) {
			String keyHint2 = this.id + RandomUtil.getRandom(9999999) + RandomUtil.getRamdomString(100);
			this.visitorId = IdGenerator.createHashedId(keyHint2);
		}
		
		return this.id;
	}

	/**
	 * for Gson and ArangoDB loader
	 */
	public Profile() {}



	/**
	 * @param partitionId
	 * @param visitorId
	 * @param type
	 * @param observerId
	 * @param srcTouchpoint
	 * @param lastSeenIp
	 * @param usedDeviceId
	 * @param email
	 * @param phone
	 * @param fingerprintId
	 * @param crmRefId
	 * @param citizenId
	 * @param applicationId
	 * @param fintechSystemId
	 * @param loyaltyId
	 */
	protected final void initNewProfile(int partitionId, String visitorId, int type, String observerId, Touchpoint srcTouchpoint, 
			String lastSeenIp, String usedDeviceId, String email, String phone, String fingerprintId, 
			String crmRefId, String citizenId, String applicationId, String fintechSystemId, String loyaltyId) {
		// hash for unique id key
		this.partitionId = partitionId;
		this.type = type;
		this.lastSeenIp = lastSeenIp;
		
		setLastObserverId(observerId);
		setFingerprintId(fingerprintId);
		setVisitorId(visitorId);
		
		// web visitor if visitorId and fingerprintId are not empty
		if (StringUtil.isNotEmpty(visitorId) && StringUtil.isNotEmpty(fingerprintId)) {
			dataLabels.add(ProfileDataLabels.WEB_USER);
		}		

		// save srcTouchpoint as queue
		if(srcTouchpoint != null) {
			this.lastTouchpoint = srcTouchpoint;
			this.lastTouchpointId = srcTouchpoint.getId();
			this.topEngagedTouchpointIds.add(srcTouchpoint.getId());
		}
		
		if(StringUtil.isNotEmpty(usedDeviceId)) {
			this.lastUsedDeviceId = usedDeviceId;
			this.usedDeviceIds.add(usedDeviceId);			
		}
		
		if(ProfileDataValidator.isValidEmail(email)) {
			this.primaryEmail = email;			
		}
		
		if(ProfileDataValidator.isValidPhoneNumber(phone)) {
			this.primaryPhone = phone;			
		}
		
		if(StringUtil.isNotEmpty(crmRefId)) {
			this.crmRefId = crmRefId;
		}
		
		if(StringUtil.isNotEmpty(citizenId)) {
			this.governmentIssuedIDs.add(citizenId);
		}
		
		if(StringUtil.isNotEmpty(applicationId)) {
			this.applicationIDs.add(applicationId);			
		}
		
		if(StringUtil.isNotEmpty(fintechSystemId)) {
			this.fintechSystemIDs.add(fintechSystemId);			
		}
		
		if(StringUtil.isNotEmpty(loyaltyId)) {
			this.loyaltyIDs.add(loyaltyId);			
		}
		
		// IDENTITY data for indexing
		buildIdentityData();

		// hash for UUID
		this.buildHashedId();
	}
	
	public void buildIdentityData() {
		if(StringUtil.isNotEmpty(webCookies)) {
			if( ! this.identities.contains(webCookies)) {
				this.setIdentity(ProfileIdentity.ID_PREFIX_WEB,  webCookies);	
			}
		}
		
		if (StringUtil.isNotEmpty(this.primaryEmail)) {
			if( ! this.identities.contains(primaryEmail)) {
				this.setIdentity(ProfileIdentity.ID_PREFIX_EMAIL,  primaryEmail);	
			}
		}		
		
		if(this.secondaryEmails != null) {
			this.secondaryEmails.forEach(email->{
				this.setIdentity(ProfileIdentity.ID_PREFIX_EMAIL, email);	
			});			
		}
		
		if (StringUtil.isNotEmpty(this.primaryPhone)) {
			if( ! this.identities.contains(primaryPhone)) {
				this.setIdentity(ProfileIdentity.ID_PREFIX_PHONE, primaryPhone);	
			}
		}
		
		if(this.secondaryPhones != null) {			
			this.secondaryPhones.forEach(phone->{
				this.setIdentity(ProfileIdentity.ID_PREFIX_PHONE, phone);	
			});
		}
		
		if (StringUtil.isNotEmpty(this.crmRefId)) {
			if( ! this.identities.contains(crmRefId)) {
				this.setIdentity(ProfileIdentity.ID_PREFIX_CRM, crmRefId);	
			}
		}
		
		if (governmentIssuedIDs != null) {
			governmentIssuedIDs.forEach(e -> {
				this.setIdentity(ProfileIdentity.ID_PREFIX_CITIZEN, e);
			});
		}
		
		if (applicationIDs != null) {
			applicationIDs.forEach(e -> {
				this.setIdentity(ProfileIdentity.ID_PREFIX_APPLICATION, e);	
			});
		}
		
		if (fintechSystemIDs != null) {
			fintechSystemIDs.forEach(e -> {
				this.setIdentity(ProfileIdentity.ID_PREFIX_FINTECH, e);	
			});
		}	
		
		if (loyaltyIDs != null) {
			loyaltyIDs.forEach(e -> {
				this.setIdentity(ProfileIdentity.ID_PREFIX_LOYALTY, e);	
			});
		}	
		
		if(this.usedDeviceIds != null) {
			usedDeviceIds.forEach(e -> {
				this.setIdentity(ProfileIdentity.ID_PREFIX_DEVICE, e);	
			});
		}
		
		
		if (personalContacts != null) {
			personalContacts.forEach((String k, String v) -> {
				this.setIdentity(k, v);
			});
		}
		
		if (businessContacts != null) {
			businessContacts.forEach((String k, String v) -> {				
				this.setIdentity(k, v);
			});
		}
		
		if (socialMediaProfiles != null) {
			socialMediaProfiles.forEach((String k, String v) -> {
				this.setIdentity(k, v);
			});
		}
		
		// TODO add full name
		
		// if empty, try random
		if(this.identities.isEmpty()) {
			this.identities.add("randomid_"+ RandomUtil.secureRandomLong());
		}
	}
	
	/**
	 * @param contactCrmId
	 * @param firstName
	 * @param lastName
	 * @param email
	 * @param phone
	 * @param genderStr
	 * @param age
	 * @param dateOfBirth
	 * @param permanentLocation
	 * @param livingLocation
	 */
	public final void setPersonalInformation(String contactCrmId, String firstName, String lastName, String email, String phone, String genderStr, int age, 
			String dateOfBirth, String permanentLocation, String livingLocation ) {
		this.setCrmRefId(contactCrmId);
		this.setFirstName(firstName);
		this.setLastName(lastName);
		this.setPrimaryEmail(email);
		this.setPrimaryPhone(phone);
		this.setGender(genderStr);
		this.setPermanentLocation(permanentLocation);
		this.setLivingLocation(livingLocation);
		this.setAge(age);	
		this.setDateOfBirth(dateOfBirth);
	}

	// -- getter and setter methods --
	
	@Override
	public void setType(int type) {
		if(type >= ProfileType.ANONYMOUS_VISITOR && type <= ProfileType.SYSTEM_USER_CONTACT) {
			this.type = type;
		}
		else {
			throw new IllegalArgumentException("Value Type is from 1 to 11, but you set Profile Type: " + type);
		}
	}

	public String getLastUsedDeviceId() {
		return lastUsedDeviceId;
	}

	public void setLastUsedDeviceId(String lastUsedDeviceId) {
		if(StringUtil.isNotEmpty(lastUsedDeviceId)) {
			this.lastUsedDeviceId = lastUsedDeviceId;
			this.usedDeviceIds.add(lastUsedDeviceId);
		}
	}

	public String getPrimaryAvatar() {
		return primaryAvatar;
	}

	public void setPrimaryAvatar(String primaryAvatar) {
		if(StringUtil.isNotEmpty(primaryAvatar)) {
			this.primaryAvatar = primaryAvatar.trim();
		}
	}
	
	public final boolean hasAvatarUrl() {
		return StringUtil.isNotEmpty(this.primaryAvatar);
	}
	
	public int getAgeGroup() {
		return ageGroup;
	}

	public void setAgeGroup(int ageGroup) {
		String s = ProfileAgeGroup.getAsLabelString(ageGroup, null);
		if( s != null ) {
			this.ageGroup = ageGroup;
		}
	}

	public Set<String> getUsedDeviceIds() {
		return usedDeviceIds;
	}

	public void setUsedDeviceIds(Set<String> list) {
		if(list != null) {
			list.forEach(e->{
				this.usedDeviceIds.add(e);
			});
		}
	}

	public void addUsedDeviceId(String usedDeviceId) {
		if(StringUtil.isNotEmpty(usedDeviceId)) {
			this.usedDeviceIds.add(usedDeviceId);	
		}
	}
	

	public Set<ContactAddress> getContactAddresses() {
		return contactAddresses;
	}

	public void setContactAddresses(Set<ContactAddress> list) {
		if(list != null) {
			list.forEach(e->{
				this.contactAddresses.add(e);
			});
		}
	}
	
	public void addContactAddress(ContactAddress contactAddress) {
		if(contactAddress != null) {
			this.contactAddresses.add(contactAddress);
		}
	}

	public Set<String> getPersonalAvatars() {
		return personalAvatars;
	}

	public void setPersonalAvatars(Set<String> list) {
		if(list != null) {
			list.forEach(e->{
				this.personalAvatars.add(e);
			});
		}
	}
	
	public void addPersonalAvatar(String personalAvatarStr) {
		if(StringUtil.isNotEmpty(personalAvatarStr)) {
			String[] toks = personalAvatarStr.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.personalAvatars.add(tok);
			}
		}
	}
	
	public Set<String> getPersonalInterests() {
		return personalInterests;
	}

	public void setPersonalInterests(Set<String> personalInterests) {
		if(personalInterests != null) {
			personalInterests.forEach(e->{
				this.personalInterests.add(e);
			});
		}
	}
	
	public void setPersonalInterests(String personalInterests) {
		if(StringUtil.isNotEmpty(personalInterests)) {
			String[] toks = personalInterests.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.personalInterests.add(tok.trim());
			}
		}
	}

	public Map<String, String> getPersonalContacts() {
		return personalContacts;
	}

	public void setPersonalContacts(Map<String, String> personalContacts) {
		if(personalContacts != null) {
			personalContacts.forEach((key,val)->{
				this.personalContacts.putIfAbsent(key, val);
			});
		}
	}
	
	public void setPersonalContacts(String personalContactStr) {
		if(StringUtil.isNotEmpty(personalContactStr)) {
			String[] toks = personalContactStr.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				String[] toks2 = tok.split(StringPool.COLON);
				if(toks2.length == 2) {
					String key = toks2[0].trim();
					String value = toks2[1].trim();
					if(StringUtil.isNotEmpty(key) && StringUtil.isNotEmpty(value)) {
						this.personalContacts.put(key, value);
					}
				}	
			}
		}
	}

	public Map<String, String> getBusinessContacts() {
		return businessContacts;
	}

	public void setBusinessContacts(Map<String, String> businessContacts) {
		if(businessContacts != null) {
			businessContacts.forEach((key,val)->{
				this.businessContacts.putIfAbsent(key, val);
			});
		}
	}
	
	public void setBusinessContacts(String businessContactsStr) {
		if(StringUtil.isNotEmpty(businessContactsStr)) {
			String[] toks = businessContactsStr.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				String[] toks2 = tok.split(StringPool.COLON);
				if(toks2.length == 2) {
					String key = toks2[0].trim();
					String value = toks2[1].trim();
					if(StringUtil.isNotEmpty(key) && StringUtil.isNotEmpty(value)) {
						this.businessContacts.put(key, value);
					}
				}	
			}
		}
	}
	
	
	public Set<LearningCourse> getLearningCourses() {
		return learningCourses;
	}

	public void setLearningCourses(Set<LearningCourse> learningCourses) {
		if(learningCourses != null) {
			this.learningCourses.addAll(learningCourses);
		}
	}
	
	public void setLearningCourses(String learningCoursesStr) {
		if(StringUtil.isNotEmpty(learningCoursesStr)) {
			learningCoursesStr = learningCoursesStr.trim();
			if(learningCoursesStr.startsWith("[") && learningCoursesStr.endsWith("]")) {
		        Type type = new TypeToken<HashSet<LearningCourse>>() {}.getType();
		        Set<LearningCourse> set = new Gson().fromJson(learningCoursesStr, type);
		        setLearningCourses(set); 
			}
			else {
				String[] toks = learningCoursesStr.split(StringPool.SEMICOLON);
				for (String tok : toks) {
					String[] toks2 = tok.split(StringPool.COLON);
					if(toks2.length == 2) {
						String id = toks2[0].trim();
						String name = toks2[1].trim();
						if(StringUtil.isNotEmpty(name)) {
							this.learningCourses.add(new LearningCourse(id, name));
						}
					}	
				}
			}
		}
	}

	public int getGender() {
		if (gender >= 0 && gender <= 7) {
			setGenderProbability(100);
		}
		return gender;
	}
	
	public boolean hasGenderData() {
		if (gender >= 0 && gender < 7) {
			return true;
		}
		return false;
	}

	public int getMaritalStatus() {
		return maritalStatus;
	}

	/**
	 * the value must from ProfileMaritalCode
	 * @param maritalStatus code
	 */
	public void setMaritalStatus(Integer maritalStatusObj) {
		int maritalStatus = maritalStatusObj == null ? ProfileMaritalCode.Unknown : maritalStatusObj.intValue();
		if(maritalStatus >= ProfileMaritalCode.Common_Law && maritalStatus <= ProfileMaritalCode.Legally_Separated) {
			this.maritalStatus = maritalStatus;
		}
		else {
			throw new InvalidDataException("maritalStatus must have value from 1 to 12, but the input is " + maritalStatus);
		}
	}


	/**
	 * 0: Female, 1: Male, 2: LGBT, 3: Lesbian, 4: Gay, 5: Bisexual, 6: Transgender, 7: Unknown
	 * 
	 * @param gender
	 */
	public void setGender(int gender) {
		if (gender >= 0 && gender <= 7) {
			this.gender = gender;
			this.genderAsText = ProfileGenderCode.getStringValue(gender);
			setGenderProbability(100);
		}
	}
	
	public void setGender(String genderStr) {
		int genderInt = ProfileGenderCode.getIntegerValue(genderStr);
		if (genderInt >= 0 && genderInt <= 7) {
			this.gender = genderInt;
			this.genderAsText = genderStr;
			setGenderProbability(100);
		} 
	}
	
	public String getGenderAsText() {
		if(genderAsText == null) {
			genderAsText = ProfileGenderCode.getStringValue(this.gender);
		}
		return genderAsText;
	}

	public void setGenderAsText(String genderAsText) {
		this.genderAsText = genderAsText;
	}
	
	public String getMaritalStatusAsText() {
		return maritalStatusAsText;
	}
	
	public void setMaritalStatus(int maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	public void setMaritalStatusAsText(String maritalStatusAsText) {
		if(StringUtil.isNotEmpty(maritalStatusAsText)) {
			this.maritalStatusAsText = maritalStatusAsText;
			this.maritalStatus = ProfileMaritalCode.getMaritalCodeAsInt(maritalStatusAsText);
		}
	}

	public int getGenderProbability() {
		return genderProbability;
	}

	public void setGenderProbability(int genderProbability) {
		if(genderProbability >=0 && genderProbability <=100) {
			this.genderProbability = genderProbability;
		}
	}
	
	public void setGenderProbability(int genderProbability, int gender) {
		setGenderProbability(genderProbability);
		setGender(gender);
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		if(age > 0) {
			this.age = age;
			this.ageGroup = ProfileAgeGroup.convertAgeNumberToAgeGroup(age);
		}
	}

	public String getLivingCity() {
		return livingCity;
	}

	public void setLivingCity(String livingCity) {
		if(StringUtil.isNotEmpty(livingCity)) {
			this.livingCity = livingCity;
		}
	}

	public String getLivingState() {
		return livingState;
	}

	public void setLivingState(String livingState) {
		if(StringUtil.isNotEmpty(livingState)) {
			this.livingState = livingState;
		}
	}

	public String getLivingCountry() {
		return livingCountry;
	}

	public void setLivingCountry(String livingCountry) {
		if(StringUtil.isNotEmpty(livingCountry)) {
			this.livingCountry = livingCountry;
		}
	}

	public String getCurrentZipCode() {
		return currentZipCode;
	}

	public void setCurrentZipCode(String currentZipCode) {
		if(StringUtil.isNotEmpty(currentZipCode)) {
			this.currentZipCode = currentZipCode;
		}
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}
	
	public void setDateOfBirth(String dateOfBirth) {
		setDateOfBirth(ProfileModelUtil.parseDate(dateOfBirth));
	}

	public void setDateOfBirth(Date dateOfBirth) {
		if(dateOfBirth != null) {
			this.dateOfBirth = dateOfBirth;			
			setAge(ProfileModelUtil.getAgeFromDateOfBirth(dateOfBirth));
		}
	}
	

	public Set<String> getWorkingHistory() {
		return workingHistory;
	}

	public void setWorkingHistory(Set<String> workingHistory) {
		if(workingHistory != null) {
			workingHistory.forEach(s->{
				this.workingHistory.add(s);
			});
		}
	}
	
	/**
	 * set workingHistory, split data by SEMICOLON
	 * 
	 * @param companies
	 */
	public void setWorkingHistory(String workingHistoryStr) {
		if(StringUtil.isNotEmpty(workingHistoryStr)) {
			String[] toks = workingHistoryStr.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.workingHistory.add(tok.trim());
			}
		}
	}
	
	public void addWorkingCompany(String company) {
		this.workingHistory.add(company);
	}
	
	public Map<String, Integer> getWeeklyMobileUsage() {
		return weeklyMobileUsage;
	}

	public void setWeeklyMobileUsage(Map<String, Integer> weeklyMobileUsage) {
		if(weeklyMobileUsage != null) {
			weeklyMobileUsage.forEach((dayNameOfWeek, updateCount )->{
				int count = this.weeklyMobileUsage.getOrDefault(dayNameOfWeek, 0) + updateCount;
				this.weeklyMobileUsage.put(dayNameOfWeek, count);
			});
		}
	}

	public Set<String> getSoftSkills() {
		return softSkills;
	}

	public void setSoftSkills(Set<String> softSkills) {
		if( softSkills != null ) {
			softSkills.forEach(s->{
				this.softSkills.add(s);
			});
		}
	}
	
	public void setSoftSkills(String softSkills) {
		if(StringUtil.isNotEmpty(softSkills)) {
			String[] toks = softSkills.split(StringPool.SEMICOLON);
			for (String s : toks) {
				this.softSkills.add(s.trim());
			}
		}
	}

	public Set<String> getLearningHistory() {
		return learningHistory;
	}

	public void setLearningHistory(Set<String> learningHistory) {
		if( learningHistory != null ) {
			learningHistory.forEach(s->{
				this.learningHistory.add(s);
			});
		}
	}

	public void setLearningHistory(String learningHistory) {
		if(StringUtil.isNotEmpty(learningHistory)) {
			String[] toks = learningHistory.split(StringPool.SEMICOLON);
			for (String s : toks) {
				this.learningHistory.add(s.trim());
			}
		}
	}

	public Set<String> getStudyCertificates() {
		return studyCertificates;
	}

	public void setStudyCertificates(Set<String> studyCertificates) {
		if( studyCertificates != null ) {
			studyCertificates.forEach(s->{
				this.studyCertificates.add(s);
			});
		}
	}
	
	public void setStudyCertificates(String studyCertificates) {
		if(StringUtil.isNotEmpty(studyCertificates)) {
			String[] toks = studyCertificates.split(StringPool.SEMICOLON);
			for (String s : toks) {
				this.studyCertificates.add(s.trim());
			}
		}
	}

	public String getWebCookies() {
		return webCookies;
	}

	public void setWebCookies(String webCookies) {
		if(StringUtil.isNotEmpty(webCookies)) {
			this.webCookies = webCookies;
		}
	}
	
	/**
	 * merge visitorId if srcLeadScore > 0 and srcLeadScore > the final profile 
	 * 
	 * @param srcVisitorId
	 * @param srcLeadScore
	 */
	public void mergeWebVisitorId(String srcVisitorId, int srcLeadScore) {
		if(this.totalLeadScore < srcLeadScore && srcLeadScore > 0) {
			setVisitorId(srcVisitorId);
		}
		this.visitorIds.add(srcVisitorId);
	}
	
	public void setVisitorId(String visitorId) {
		if(StringUtil.isNotEmpty(visitorId)) {			
			this.visitorId = visitorId;
			this.setIdentity(ProfileIdentity.ID_PREFIX_VISITOR, visitorId);
		}
	}

	public String getVisitorId() {
		if(StringUtil.isEmpty(this.visitorId)) {
			this.visitorId = FriendlyId.toFriendlyId(UUID.nameUUIDFromBytes(this.id.getBytes()));
			this.visitorIds.add(this.visitorId);
		}
		return this.visitorId;
	}
	

	public Set<String> getVisitorIds() {
		return visitorIds;
	}

	public void setVisitorIds(Set<String> visitorIds) {		
		if(visitorIds != null) {
			for (String visitorId : visitorIds) {
				this.visitorIds.add(visitorId);
			}
		}
	}

	public String getFingerprintId() {
		return fingerprintId;
	}

	public void setFingerprintId(String fingerprintId) {
		if(StringUtil.isNotEmpty(fingerprintId)) {
			this.fingerprintId = fingerprintId;
			this.setIdentity(ProfileIdentity.ID_PREFIX_FINGERPRINT, fingerprintId);
		}
	}

	public String getLivingLocation() {
		return StringUtil.safeString(livingLocation);
	}

	public void setLivingLocation(String livingLocation) {
		if(StringUtil.isNotEmpty(livingLocation)) {
			this.livingLocation = livingLocation;
		}
	}
	
	public Set<PersonalityType> getPersonalityTypes() {
		return personalityTypes;
	}

	public void setPersonalityTypes(Set<PersonalityType> personalityTypes) {
		if(personalityTypes != null) {
			this.personalityTypes = personalityTypes;
		}
	}
	
	public void setPersonalityTypes(String personalityType) {
		if(StringUtil.isNotEmpty(personalityType)) {
			this.personalityTypes.add(new PersonalityType(personalityType));
		}
	}

	public String getPermanentLocation() {
		return StringUtil.safeString(permanentLocation);
	}

	public void setPermanentLocation(String permanentLocation) {
		if(StringUtil.isNotEmpty(permanentLocation)) {
			this.permanentLocation = permanentLocation;
		}
	}

	public String getLivingProvince() {
		return StringUtil.safeString(livingProvince);
	}

	public void setLivingProvince(String livingProvince) {
		if(StringUtil.isNotEmpty(livingProvince)) {
			this.livingProvince = livingProvince;
		}
	}

	public String getLivingCounty() {
		return StringUtil.safeString(livingCounty);
	}

	public void setLivingCounty(String livingCounty) {
		if(StringUtil.isNotEmpty(livingCounty)) {
			this.livingCounty = livingCounty;
		}
	}

	public String getLocationCode() {
		return StringUtil.safeString(locationCode);
	}

	public void setLocationCode(String locationCode) {
		if(StringUtil.isNotEmpty(locationCode)) {
			this.locationCode = locationCode;
		}
	}
	
	public Set<String> getJobTitles() {
		return jobTitles;
	}

	public void setJobTitles(Set<String> jobTitles) {
		if(jobTitles != null) {
			jobTitles.forEach(s->{
				this.jobTitles.add(s);
			});
		}
	}
	
	public void setJobTitles(String jobTitles) {
		if(StringUtil.isNotEmpty(jobTitles)) {
			String[] toks = jobTitles.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.jobTitles.add(tok);
			}
		}
	}
	
	public void addJobTitle(String jobTitle) {
		this.jobTitles.add(jobTitle);
	}

	public String getPrimaryNationality() {
		return primaryNationality;
	}

	public void setPrimaryNationality(String primaryNationality) {
		if(StringUtil.isNotEmpty(primaryNationality)) {
			this.primaryNationality = primaryNationality.trim();
		}
	}

	public String getLivingLocationType() {
		return livingLocationType;
	}

	public void setLivingLocationType(String livingLocationType) {
		if(StringUtil.isNotEmpty(livingLocationType)) {
			this.livingLocationType = livingLocationType;
		}
	}

	public String getLivingWard() {
		return livingWard;
	}

	public void setLivingWard(String livingWard) {
		if(StringUtil.isNotEmpty(livingWard)) {
			this.livingWard = livingWard;
		}
	}

	public String getLivingDistrict() {
		return livingDistrict;
	}

	public void setLivingDistrict(String livingDistrict) {
		if(StringUtil.isNotEmpty(livingDistrict)) {
			this.livingDistrict = livingDistrict;
		}
	}
	
	public String getFullName() {
		return StringUtil.join(StringPool.SPACE, StringUtil.safeString(this.firstName), StringUtil.safeString(this.middleName),  StringUtil.safeString(this.lastName));
	}

	public String getFirstName() {
		if(this.firstName == null) {
			this.firstName = "";
		}
		return firstName;
	}

	public void setFirstName(String firstName) {
		if(isValidLength(firstName)) {
			if(StringUtil.isEmpty(this.firstName) && Profile.UNKNOWN_PROFILE.equals(firstName)) {
				this.firstName = Profile.UNKNOWN_PROFILE;
			}
			else {
				this.firstName = firstName.trim();
			}
		}
	}
	
	public String getMiddleName() {
		if(this.middleName == null) {
			this.middleName = "";
		}
		return middleName;
	}

	public void setMiddleName(String middleName) {
		if(StringUtil.isNotEmpty(middleName)) {
			this.middleName = middleName;
		}
	}

	public String getLastName() {
		if(this.lastName == null) {
			this.lastName = "";
		}
		return lastName;
	}

	public void setLastName(String lastName) {
		if(isValidLength(lastName)) {
			this.lastName = lastName.trim();
		}
	}

	public String getPrimaryUsername() {
		return primaryUsername;
	}

	public void setPrimaryUsername(String primaryUsername) {
		if(StringUtil.isNotEmpty(primaryUsername)) {
			this.primaryUsername = primaryUsername.trim();
			this.setIdentity(ProfileIdentity.ID_PREFIX_USERNAME, primaryUsername);
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if(StringUtil.isNotEmpty(password)) {
			this.password = password;
		}
	}
	
	public Map<String, Date> getContextSessionKeys() {
		return contextSessionKeys;
	}
	
	public void clearContextSessionKeys() {
		contextSessionKeys.clear();
	}

	public void setContextSessionKeys(Map<String, Date> contextSessionKeys) {
		this.contextSessionKeys.putAll(contextSessionKeys);
	}
	
	public void setContextSessionKey(String contextSessionKey, Date updatedDate) {
		this.contextSessionKeys.put(contextSessionKey, updatedDate);
	}

	public final boolean checkToStartMergeDataJob() {
		boolean check = this.status == STATUS_ACTIVE && this.hasContactData();
		return check;
	}
	
	
	@Override
	public final boolean hasContactData() {
		return StringUtil.isNotEmpty(this.primaryEmail) || StringUtil.isNotEmpty(this.primaryPhone);
	}
	
	@Override
	public boolean recommendProductItems(AssetTemplate assetTemplate, List<ProductItem> productItems) {
		return ActivationFlowManagement.recommendProductItems(this, assetTemplate, productItems);
	}
	
	@Override
	public boolean recommendContentItems(AssetTemplate assetTemplate, List<AssetContent> contentItems) {
		return ActivationFlowManagement.recommendContentItems(this, assetTemplate, contentItems);
	}
	
	@Override
	public final boolean checkToDeleteForever() {
		return this.status != STATUS_ACTIVE;
	}
	
	/**
	 * 
	 */
	public void loadGenderAndMarialStatus() {
		this.genderAsText = "-";
		if (this.genderProbability == 100) {
			if (this.gender == 1) {
				this.genderAsText = "Male";
			} else if (this.gender == 0) {
				this.genderAsText = "Female";
			} else if (this.gender == 2) {
				this.genderAsText = "LGBT";
			}
		} else {
			if (this.gender == 1) {
				this.genderAsText = "Male with probability " + this.genderProbability + " %";
			} else if (this.gender == 0) {
				this.genderAsText = "Female with probability " + this.genderProbability + " %";
			}
		}
		this.maritalStatusAsText = ProfileMaritalCode.getMaritalCodeAsText(this.maritalStatus);
	}
	
	/**
	 * to clear Personal Information Data
	 */
	public void clearPersonalInformation() {
		this.firstName = "";
		this.middleName = "";
		this.lastName = "";
		this.primaryEmail = "";
		this.secondaryEmails.clear();
		this.primaryPhone = "";
		this.secondaryPhones.clear();
		this.age = 0;
		this.gender = ProfileGenderCode.UNKNOWN;
		this.maritalStatus = ProfileMaritalCode.Unknown;
		this.dateOfBirth = null;
		this.socialMediaProfiles.clear();
		this.governmentIssuedIDs.clear();
	}
	
	public String getHousingType() {
		return housingType;
	}

	public void setHousingType(String housingType) {
		if(StringUtil.isNotEmpty(housingType)) {
			this.housingType = housingType;
		}
	}

	
	public Set<String> getGovernmentIssuedIDs() {
		return governmentIssuedIDs;
	}

	public void setGovernmentIssuedIDs(Set<String> governmentIssuedIDs) {
		if(governmentIssuedIDs != null) {
			governmentIssuedIDs.forEach(govId->{
				setGovernmentIssuedID(govId);
			});
		}
	}
	
	public void setGovernmentIssuedIDs(String strGovernmentIssuedIDs) {
		if(StringUtil.isNotEmpty(strGovernmentIssuedIDs)) {
			String[] govIds = strGovernmentIssuedIDs.split(StringPool.SEMICOLON);
			for (String govId : govIds) {
				setGovernmentIssuedID(govId);
			}
		}
	}
	
	
	public void setGovernmentIssuedID(String governmentIssuedID) {
		if(StringUtil.isNotEmpty(governmentIssuedID)) {
			this.governmentIssuedIDs.add(governmentIssuedID);	
			this.setIdentity(ProfileIdentity.ID_PREFIX_CITIZEN, governmentIssuedID);
		}
	}

	public Set<String> getApplicationIDs() {
		return applicationIDs;
	}
	
	/**
	 * @return the first application ID in the list
	 */
	public String getApplicationID() {
		Iterator<String> it = applicationIDs.iterator();
		return it.hasNext() ? it.next() : StringPool.BLANK;
	}

	public void setApplicationIDs(Set<String> applicationIDs) {
		if(applicationIDs != null) {
			applicationIDs.forEach(applicationID->{
				setApplicationID(applicationID);
			});
		}
	}
	
	public void setApplicationID(String applicationID) {
		this.setApplicationID(applicationID, true);
	}
	
	/**
	 * @param applicationID
	 * @param withPrefixIndentity
	 */
	public void setApplicationID(String applicationID, boolean withPrefixIndentity) {
		if(StringUtil.isNotEmpty(applicationID)) {
			this.applicationIDs.add(applicationID);
			if(withPrefixIndentity) {
				this.setIdentity(ProfileIdentity.ID_PREFIX_APPLICATION, applicationID);
			}
			else {
				this.identities.add(applicationID);
			}
		}
	}
	
	public Set<String> getFintechSystemIDs() {
		return fintechSystemIDs;
	}
	
	public void setFintechSystemID(String fintechSystemID) {
		if(StringUtil.isNotEmpty(fintechSystemID)) {
			this.fintechSystemIDs.add(fintechSystemID);
			this.setIdentity(ProfileIdentity.ID_PREFIX_FINTECH, fintechSystemID);
		}
	}

	public void setFintechSystemIDs(Set<String> fintechSystemIDs) {
		if(fintechSystemIDs != null) {
			fintechSystemIDs.forEach(fintechSystemID->{
				setFintechSystemID(fintechSystemID);
			});
		}
	}

	public Set<String> getLoyaltyIDs() {
		return loyaltyIDs;
	}
	
	public void setLoyaltyIDs(String loyaltyID) {
		if(loyaltyID != null) {
			loyaltyIDs.add(loyaltyID);
		}
	}

	public void setLoyaltyIDs(Set<String> loyaltyIDs) {
		if(loyaltyIDs != null) {
			loyaltyIDs.forEach(loyaltyID->{
				setLoyaltyIDs(loyaltyID);
			});
		}
	}

	public int getJobType() {
		return jobType;
	}

	/**
	 * The value must be 0: unskilled and non-resident, 1: unskilled and resident, 2: skilled, 3: highly skilled
	 * 
	 * @param jobType
	 */
	public void setJobType(int jobType) {
		if( jobType >= 0 && jobType <= 3) {
			this.jobType = jobType;
		}
	}
	
	/**
	 * The value must be 0: unskilled and non-resident, 1: unskilled and resident, 2: skilled, 3: highly skilled
	 * 
	 * @param jobType
	 */
	public void setJobType(Integer jobType) {
		if( jobType != null) {
			setJobType(jobType.intValue());
		}
	}
	
	

	public Map<String, Double> getIncomeHistory() {
		return incomeHistory;
	}

	public void setIncomeHistory(Map<String, Double> incomeHistory) {
		if(incomeHistory != null) {
			incomeHistory.forEach((k,v)->{
				this.incomeHistory.put(k, v);
			});
		}
	}
	
	public void setIncomeHistory(String key, String value) {
		if(StringUtil.isNotEmpty(key) && StringUtil.isNotEmpty(value)) {
			double income = StringUtil.safeParseDouble(value);
			this.incomeHistory.put(key, income);
		}
	}
	
	public void setIncomeHistory(String json) {
		Type type = new TypeToken<Map<String, String>>(){}.getType();
		Map<String, String> myMap = new Gson().fromJson(json, type);
		myMap.forEach((k,v)->{
			double income = StringUtil.safeParseDouble(v);
			this.incomeHistory.put(k, income);
		});
		
	}
	

	public TrackingEvent getLastTrackingEvent() {
		return lastTrackingEvent;
	}

	public void setLastTrackingEvent(TrackingEvent lastTrackingEvent) {
		this.lastTrackingEvent = lastTrackingEvent;
	}

	public TrackingEvent getLastItemViewEvent() {
		return lastItemViewEvent;
	}

	public void setLastItemViewEvent(TrackingEvent lastItemViewEvent) {
		this.lastItemViewEvent = lastItemViewEvent;
	}

	public TrackingEvent getLastPurchaseEvent() {
		return lastPurchaseEvent;
	}

	public void setLastPurchaseEvent(TrackingEvent lastPurchaseEvent) {
		this.lastPurchaseEvent = lastPurchaseEvent;
	}


	/**
	 * 
	 * 
	 * @param eventData
	 */
	public void updateFromEventData(Map<String, Object> eventData) {
		if(eventData != null) {
			// 
			this.setCrmRefId(eventData.getOrDefault("user_id","").toString());
			this.setCrmRefId(eventData.getOrDefault("crm_ref_id","").toString());
			
			this.setFirstName(eventData.getOrDefault("name","").toString());
			this.setFirstName(eventData.getOrDefault("first_name","").toString());
			this.setLastName(eventData.getOrDefault("last_name","").toString());
			
			this.setEmail(eventData.getOrDefault("email","").toString());
			this.setPhone( eventData.getOrDefault("phone","").toString());
			this.setPrimaryUsername(eventData.getOrDefault("user_name","").toString());
			
			if(this.hasContactData() && this.type == ProfileType.ANONYMOUS_VISITOR) {
				// convert visitor into user login
				this.type = ProfileType.CUSTOMER_CONTACT;
			}
			
			// UTM data of Google Analytics
			this.setEmail(eventData.getOrDefault("utm_email","").toString());
			this.setPhone( eventData.getOrDefault("utm_phone","").toString());
			this.updateReferrerChannel(eventData.getOrDefault("utm_source","").toString());
			this.setContentKeywords(eventData.getOrDefault("utm_term","").toString());
			this.setGoogleUtmData(new GoogleUTM(eventData));
		}
	}

	/**
	 * for export data to CSV row's format
	 * @return CSV string
	 */
	public String toCSV(int csvType) {
		return ProfileExportingDataUtil.exportDataAsFileCSV(this, csvType);
	}

	/**
	 * for export data to Google Sheet row's values
	 * @return two-sided List
	 */
	public List<Object> toGoogleSheet(int csvType) {
		return ProfileExportingDataUtil.exportDataAsGoogleSheetRow(this, csvType);
	}

	/**
	 * to JSON
	 * 
	 * @return
	 */
	public String toJson() {
		return new Gson().toJson(this);
	}
	
	@Override
	public int hashCode() {
		if(this.id != null) {
			 return Objects.hash(id);
		}
		return 0;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj != null) {
			return this.hashCode() == obj.hashCode();	
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "id:"+this.id + " firstName:" + this.firstName + " lastName:" + this.lastName + " primaryEmail:" + this.primaryEmail + " primaryPhone:" + this.primaryPhone;
	}
	
	public ProfileIdentity toProfileIdentity() {
		return new ProfileIdentity(this);
	}
	
	/**
	 *  compute the data quality score
	 */
	public void computeDataQualityScore() {
		this.computeDataQualityScore(0);
	}
	

	
	/**
	 * compute the data quality score
	 * 
	 * @param daysSinceLastUpdate
	 */
	public void computeDataQualityScore(int daysSinceLastUpdate) {
		this.dataQualityScore = ProfileModelUtil.computeDataQualityScore(this, daysSinceLastUpdate);
	}
	

	
	/**
	 *  for marketing campaigns: notify to item-view, email or send ZNS for last transaction
	 * 
	 * @return true if has data
	 */
	public final boolean loadLastTrackingEvents() {
		List<String> metrics = Arrays.asList(BehavioralEvent.General.ITEM_VIEW, BehavioralEvent.Commerce.PURCHASE);
		// get last product that profile watched
		LastTrackingEventMap eventMap = TrackingEventDao.getLastEventsOfProfileIdByMetricNames(this.id, "", metrics);
		
		this.lastItemViewEvent = eventMap.getEventByMetricName(BehavioralEvent.General.ITEM_VIEW);
		this.lastPurchaseEvent= eventMap.getEventByMetricName(BehavioralEvent.Commerce.PURCHASE);
		this.lastTrackingEvent = eventMap.getLastEvent();
		
		if(this.lastTrackingEvent == null) {
			this.lastTrackingEvent = new TrackingEvent();
			this.lastTrackingEvent.setMetricName(BehavioralEvent.General.DATA_IMPORT);
			this.lastTrackingEvent.setObserverId(this.lastObserverId);
		}
		
		boolean hasData = this.lastTrackingEvent != null && this.lastItemViewEvent != null && this.lastPurchaseEvent != null;
		return hasData;
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}
}
