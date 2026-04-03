package leotech.cdp.model.analytics;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.customer.BasicContactData;
import leotech.cdp.model.customer.ProfileAgeGroup;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointType;
import leotech.system.util.XssFilterUtil;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.StringUtil;

/**
 * FeedbackEvent is the data model for
 * ./resources/app-templates/web-form/template-survey-form.html
 * 
 * @author tantrieuf31
 * @since 2021
 */
public final class FeedbackEvent extends FeedbackData {

	public static final String CONTACT = "CONTACT";
	public static final String SURVEY = "SURVEY";
	public static final String RATING = "RATING";

	// Thread-safe formatter to replace heavy SimpleDateFormat instantiation
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
			.ofPattern(DateTimeUtil.DATE_FORMAT_PATTERN).withZone(ZoneId.systemDefault());

	@Expose
	String eventName = "";

	@Expose
	boolean onSharedDevices = false;

	@Expose
	int status = 1; // 1 recorded, 0 computed, -1 deleted

	// ----------------------------------------------------------------------
	// TOUCHPOINT METADATA
	// ----------------------------------------------------------------------

	@Expose
	String touchpointId = "";

	@Expose
	String touchpointName = "";

	@Expose
	String touchpointUrl = "";

	@Expose
	String deviceId = "";

	@Expose
	String fingerprintId = "";

	@Expose
	String refVisitorId = "";

	// ----------------------------------------------------------------------
	// PROFILE METADATA
	// ----------------------------------------------------------------------

	@Expose
	String refProfileId = "";

	@Expose
	String profileFirstName = "";

	@Expose
	String profileLastName = "";

	@Expose
	String profileEmail = "";

	@Expose
	String profilePhone = "";

	@Expose
	int profileGender = -1;

	@Expose
	int profileAge = -1;

	@Expose
	int profileAgeGroup = -1;

	@Expose
	String profileDateOfBirth = "";

	@Expose
	String profileLivingLocation;

	@Expose
	String profileLocationCode;

	@Expose
	String profileNationality;

	@Expose
	Map<String, Object> profileExtAttributes = new HashMap<>();

	@Expose
	Map<String, String> profileExtId = new HashMap<>();

	// ----------------------------------------------------------------------
	// SURVEY ANSWERS
	// ----------------------------------------------------------------------

	@Expose
	Map<String, Map<String, String>> ratingQuestionAnswer = new HashMap<>();

	@Expose
	Map<String, QuestionAnswer> extraTextQuestionsAnswer = new HashMap<>();

	@Expose
	Map<String, QuestionAnswer> singleChoiceQuestionAnswer = new HashMap<>();

	@Expose
	Map<String, QuestionAnswer> multipleChoiceQuestionAnswer = new HashMap<>();

	// ----------------------------------------------------------------------
	// FEEDBACK CONFIGURATION
	// ----------------------------------------------------------------------

	@Expose
	double feedbackScore = -1; // 1 - 5 or 0 - 10

	@Expose
	List<String> decisionMakers = new ArrayList<>();

	@Expose
	List<String> originalSources = new ArrayList<>();

	@Expose
	List<String> mediaSources = new ArrayList<>();

	@Expose
	String comment = ""; // comment text

	@Expose
	String language = "en";

	@Expose
	String dateKey = "";

	@Expose
	double geoLatitude;

	@Expose
	double geoLongitude;

	public FeedbackEvent() {
		// Default constructor for Gson
	}

	public FeedbackEvent(TrackingEvent event) {
		setCreatedAt(event.getCreatedAt());
		setRefProfileId(event.getRefProfileId());
		setFeedbackType(RATING);
		setFeedbackScore(event.getMetricValue());
		setEventName(event.getMetricName());
		setRefTouchpointId(event.getSrcTouchpointId());
		setRefTouchpointHubId(event.getRefTouchpointHubId());
		setHeader("FEEDBACK RATING - " + event.getSrcTouchpointName());
		setRefTouchpointHubId(event.getSrcTouchpointHubId());
		setTouchpointId(event.getSrcTouchpointId());
		setTouchpointName(event.getSrcTouchpointName());
		setTouchpointUrl(event.getSrcTouchpointUrl());
		this.buildHashedId();
	}

	public FeedbackEvent(String metricName, Date createdAt, String refProfileId, int ratingScore,
			Touchpoint srcTouchpoint) {
		setCreatedAt(createdAt);
		setRefProfileId(refProfileId);
		setFeedbackType(RATING);
		setFeedbackScore(ratingScore);
		setEventName(metricName);
		setHeader("FEEDBACK RATING at " + srcTouchpoint.getName());
		setTouchpointId(srcTouchpoint.getId());
		setTouchpointName(srcTouchpoint.getName());
		setTouchpointUrl(srcTouchpoint.getUrl());
		this.buildHashedId();
	}

	@Override
	public boolean dataValidation() {
		boolean hasAnswer = false;

		if (SURVEY.equals(this.feedbackType)) {
			hasAnswer = !this.getRatingQuestionAnswer().isEmpty();
			if (!hasAnswer) {
				// for simple survey to collect just basic profile data
				hasAnswer = (StringUtil.isNotEmpty(profileFirstName) || StringUtil.isNotEmpty(profileLastName))
						&& (StringUtil.isNotEmpty(profileEmail) || StringUtil.isNotEmpty(profilePhone));
			}
		} else if (CONTACT.equals(this.feedbackType)) {
			hasAnswer = StringUtil.isNotEmpty(this.profileEmail) || StringUtil.isNotEmpty(this.profilePhone);
		} else {
			hasAnswer = this.feedbackScore >= 0 && this.feedbackScore <= 10;
		}

		return hasAnswer && this.createdAt != null && StringUtil.isNotEmpty(this.refProfileId)
				&& StringUtil.isNotEmpty(this.touchpointId) && StringUtil.isNotEmpty(this.touchpointUrl);
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (dataValidation()) {
			super.cleanXss();
			XssFilterUtil.cleanAllHtmlTags(this, FeedbackEvent.class);

			this.buildDateKey();
			String keyHint = refProfileId + header + group + evaluatedObject + feedbackType + surveyChoicesId
					+ refTemplateId + refTouchpointHubId + refProductItemId + refCampaignId + refContentItemId
					+ refTouchpointId + touchpointId + this.createdAt.getTime();

			this.id = createId(this.id, keyHint);
			return this.id;
		} else {
			// FIX: Replaced System.err.println with a meaningful exception string
			throw new IllegalArgumentException(
					"FeedbackEvent data validation failed. Missing required answers, profile ID, or touchpoint URLs. Payload: "
							+ this.toString());
		}
	}

	public String buildDateKey() {
		if (this.createdAt != null) {
			// FIX: Upgraded to thread-safe DateTimeFormatter
			this.dateKey = DATE_FORMATTER.format(this.createdAt.toInstant());
		}
		return this.dateKey;
	}

	public Map<String, Object> getUpdatingProfileAttributes() {
		// -------------------------------------------------------------------------
		// 1. CORE IDENTITY & CONTACT
		// Keys exactly match the property names in Profile.java / AbstractProfile.java
		// -------------------------------------------------------------------------
		this.profileExtAttributes.put("firstName", profileFirstName);
		this.profileExtAttributes.put("lastName", profileLastName);
		this.profileExtAttributes.put("primaryEmail", profileEmail);
		this.profileExtAttributes.put("primaryPhone", profilePhone);
		
		// -------------------------------------------------------------------------
		// 2. DEMOGRAPHICS
		// -------------------------------------------------------------------------
		this.profileExtAttributes.put("gender", profileGender);
		this.profileExtAttributes.put("age", profileAge);
		this.profileExtAttributes.put("ageGroup", profileAgeGroup);
		this.profileExtAttributes.put("dateOfBirth", profileDateOfBirth);
		this.profileExtAttributes.put("primaryNationality", profileNationality);

		// -------------------------------------------------------------------------
		// 3. LOCATION
		// -------------------------------------------------------------------------
		this.profileExtAttributes.put("livingLocation", profileLivingLocation);
		this.profileExtAttributes.put("locationCode", profileLocationCode);
		
		// -------------------------------------------------------------------------
		// 4. DEVICE & DIGITAL FOOTPRINT
		// Maps exactly to AbstractProfile tracking fields for Identity Resolution
		// -------------------------------------------------------------------------
		this.profileExtAttributes.put("lastUsedDeviceId", deviceId);
		this.profileExtAttributes.put("fingerprintId", fingerprintId);
		this.profileExtAttributes.put("visitorId", refVisitorId);


		// -------------------------------------------------------------------------
		// 5. B2B, ACQUISITION & MARKETING CHANNELS
		// Maps to Profile.java's Sets/Lists
		// -------------------------------------------------------------------------
		if (mediaSources != null && !mediaSources.isEmpty()) {
			this.profileExtAttributes.put("mediaChannels", mediaSources);
		}
		if (originalSources != null && !originalSources.isEmpty()) {
			this.profileExtAttributes.put("referrerChannels", originalSources);
		}
		if (decisionMakers != null && !decisionMakers.isEmpty()) {
			// Closest match in Profile.java for decision makers is businessContacts
			this.profileExtAttributes.put("businessContacts", decisionMakers);
		}

		// -------------------------------------------------------------------------
		// 6. EXTERNAL IDENTITIES (CRM, Social, etc.)
		// -------------------------------------------------------------------------
		if (profileExtId != null && !profileExtId.isEmpty()) {
			// If a CRM ID is passed in the feedback context, map it directly
			if (profileExtId.containsKey("crmRefId")) {
				this.profileExtAttributes.put("crmRefId", profileExtId.get("crmRefId"));
			}
			// Dump the rest into the profile's custom extAttributes map
			this.profileExtAttributes.put("extAttributes", profileExtId);
		}

		// -------------------------------------------------------------------------
		// 7. CUSTOM EXPERIENCE (CX) & FEEDBACK CONTEXT
		// These will likely fall into Profile.extAttributes or dynamic variables
		// -------------------------------------------------------------------------
		
		
		if (StringUtil.isNotEmpty(language)) {
			this.profileExtAttributes.put("primaryLanguage", language);
		}
		if (geoLatitude > 0 && geoLongitude > 0) {
			String locUrl = "https://www.google.com/maps/search/?api=1&query="+geoLatitude+","+geoLongitude;
			Touchpoint loc = new Touchpoint(TouchpointType.FEEDBACK_SURVEY, locUrl);
			this.profileExtAttributes.put("lastTouchpoint", loc);
		}

		// -------------------------------------------------------------------------
		// 8. ENRICH & CLEANUP
		// Automatically drops keys resolving to -1, empty strings, or nulls.
		// Translates gender integers to Strings (1 -> "Male") automatically.
		// -------------------------------------------------------------------------
		this.enrichProfileExtAttributes();

		// Safely return the newly enriched and cleaned map
		return this.profileExtAttributes;
	}

	public void enrichProfileExtAttributes() {
		Map<String, Object> newMap = new HashMap<>(this.profileExtAttributes.size());

		this.profileExtAttributes.forEach((key, val) -> {
			if (val == null)
				return;

			String stringVal = String.valueOf(val);
			int intValue = StringUtil.safeParseInt(stringVal);
			Object enrichedVal = val;

			if (key.equalsIgnoreCase("gender")) {
				if (intValue == 1) {
					enrichedVal = "Male";
				} else if (intValue == 0) {
					enrichedVal = "Female";
				}
			} else if (key.equalsIgnoreCase("ageGroup")) {
				enrichedVal = ProfileAgeGroup.getAsLabelString(intValue);
			}

			// Maintains original logic: Drops properties resolving to -1
			if (intValue != -1 && StringUtil.isNotEmpty(String.valueOf(enrichedVal))) {
				newMap.put(key, enrichedVal);
			}
		});

		this.profileExtAttributes = newMap;
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	public boolean isOnSharedDevices() {
		return onSharedDevices;
	}

	public void setOnSharedDevices(boolean onSharedDevices) {
		this.onSharedDevices = onSharedDevices;
	}

	public String getProfileNationality() {
		return profileNationality;
	}

	public void setProfileNationality(String profileNationality) {
		this.profileNationality = profileNationality;
	}

	public String getDateKey() {
		return dateKey;
	}

	public void setDateKey(String dateKey) {
		this.dateKey = dateKey;
	}

	public String getRefProfileId() {
		return refProfileId;
	}

	public void setRefProfileId(String refProfileId) {
		this.refProfileId = refProfileId;
	}

	public String getTouchpointId() {
		return touchpointId;
	}

	public void setTouchpointId(String touchpointId) {
		this.touchpointId = touchpointId;
	}

	public String getTouchpointName() {
		return touchpointName;
	}

	public void setTouchpointName(String touchpointName) {
		this.touchpointName = touchpointName;
	}

	public String getTouchpointUrl() {
		return touchpointUrl;
	}

	public void setTouchpointUrl(String touchpointUrl) {
		this.touchpointUrl = touchpointUrl;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public double getFeedbackScore() {
		return feedbackScore;
	}

	public void setFeedbackScore(double feedbackScore) {
		this.feedbackScore = feedbackScore;
	}

	public void setFeedbackScore(int feedbackScore) {
		this.feedbackScore = feedbackScore;
	}

	public int getFeedbackScoreInteger() {
		return (int) Math.floor(feedbackScore);
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getProfileFirstName() {
		return profileFirstName;
	}

	public void setProfileFirstName(String profileFirstName) {
		this.profileFirstName = profileFirstName;
	}

	public String getProfileLastName() {
		return profileLastName;
	}

	public void setProfileLastName(String profileLastName) {
		this.profileLastName = profileLastName;
	}

	public String getProfileEmail() {
		return profileEmail;
	}

	public void setProfileEmail(String profileEmail) {
		this.profileEmail = profileEmail;
	}

	public String getProfilePhone() {
		return profilePhone;
	}

	public void setProfilePhone(String profilePhone) {
		this.profilePhone = profilePhone;
	}

	public Map<String, String> getProfileExtId() {
		return profileExtId;
	}

	public void setProfileExtId(Map<String, String> profileExtId) {
		this.profileExtId = profileExtId;
	}

	public List<String> getOriginalSources() {
		return originalSources;
	}

	public void setOriginalSources(List<String> originalSources) {
		this.originalSources = originalSources;
	}

	public List<String> getMediaSources() {
		return mediaSources;
	}

	public void setMediaSources(List<String> mediaSources) {
		this.mediaSources = mediaSources;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public String getRefVisitorId() {
		return refVisitorId;
	}

	public void setRefVisitorId(String refVisitorId) {
		this.refVisitorId = refVisitorId;
	}

	public List<String> getDecisionMakers() {
		return decisionMakers;
	}

	public void setDecisionMakers(List<String> decisionMakers) {
		this.decisionMakers = decisionMakers;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Map<String, Map<String, String>> getRatingQuestionAnswer() {
		if (ratingQuestionAnswer == null) {
			ratingQuestionAnswer = new HashMap<>();
		}
		return ratingQuestionAnswer;
	}

	public void setRatingQuestionAnswer(Map<String, Map<String, String>> ratingQuestionAnswer) {
		this.ratingQuestionAnswer = ratingQuestionAnswer;
	}

	public Map<String, QuestionAnswer> getExtraTextQuestionsAnswer() {
		return extraTextQuestionsAnswer;
	}

	public void setExtraTextQuestionsAnswer(Map<String, QuestionAnswer> extraTextQuestionsAnswer) {
		this.extraTextQuestionsAnswer = extraTextQuestionsAnswer;
	}

	public Map<String, QuestionAnswer> getSingleChoiceQuestionAnswer() {
		return singleChoiceQuestionAnswer;
	}

	public void setSingleChoiceQuestionAnswer(Map<String, QuestionAnswer> singleChoiceQuestionAnswer) {
		this.singleChoiceQuestionAnswer = singleChoiceQuestionAnswer;
	}

	public Map<String, QuestionAnswer> getMultipleChoiceQuestionAnswer() {
		return multipleChoiceQuestionAnswer;
	}

	public void setMultipleChoiceQuestionAnswer(Map<String, QuestionAnswer> multipleChoiceQuestionAnswer) {
		this.multipleChoiceQuestionAnswer = multipleChoiceQuestionAnswer;
	}

	public int getProfileGender() {
		return profileGender;
	}

	public void setProfileGender(int profileGender) {
		this.profileGender = profileGender;
	}

	public int getProfileAge() {
		return profileAge;
	}

	public void setProfileAge(int profileAge) {
		this.profileAge = profileAge;
	}

	public int getProfileAgeGroup() {
		return profileAgeGroup;
	}

	public void setProfileAgeGroup(int profileAgeGroup) {
		this.profileAgeGroup = profileAgeGroup;
	}

	public String getProfileDateOfBirth() {
		return profileDateOfBirth;
	}

	public void setProfileDateOfBirth(String profileDateOfBirth) {
		this.profileDateOfBirth = profileDateOfBirth;
	}

	public String getProfileLivingLocation() {
		return profileLivingLocation;
	}

	public void setProfileLivingLocation(String profileLivingLocation) {
		this.profileLivingLocation = profileLivingLocation;
	}

	public String getProfileLocationCode() {
		return profileLocationCode;
	}

	public void setProfileLocationCode(String profileLocationCode) {
		this.profileLocationCode = profileLocationCode;
	}

	public double getGeoLatitude() {
		return geoLatitude;
	}

	public void setGeoLatitude(double geoLatitude) {
		this.geoLatitude = geoLatitude;
	}

	public double getGeoLongitude() {
		return geoLongitude;
	}

	public void setGeoLongitude(double geoLongitude) {
		this.geoLongitude = geoLongitude;
	}

	public String getFingerprintId() {
		return fingerprintId;
	}

	public void setFingerprintId(String fingerprintId) {
		this.fingerprintId = fingerprintId;
	}

	public Map<String, Object> getProfileExtAttributes() {
		return profileExtAttributes;
	}

	public void setProfileExtAttributes(Map<String, Object> profileExtAttributes) {
		this.profileExtAttributes = profileExtAttributes;
	}

	public BasicContactData getBasicContactData() {
		return new BasicContactData(profileEmail, profilePhone, profileFirstName, profileLastName);
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}