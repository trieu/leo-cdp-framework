package leotech.cdp.model.analytics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import leotech.system.util.XssFilterUtil;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.StringUtil;

/**
 * FeedbackEvent is the data model for ./resources/app-templates/web-form/template-survey-form.html
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class FeedbackEvent extends FeedbackData {

	public static final String CONTACT = "CONTACT";

	public static final String SURVEY = "SURVEY";
	
	public static final String RATING = "RATING";

	@Expose
	String eventName = "";
	
	@Expose
	boolean onSharedDevices = false;

	@Expose
	int status = 1;// 1 recorded, 0 computed, -1 deleted

	////////////////////////////////////////////

	@Expose
	String touchpointId = "";

	@Expose
	String touchpointName = "";

	@Expose
	String touchpointUrl = "";

	@Expose
	String deviceId = "";

	@Expose
	String refVisitorId = "";

	////////////////////////////////////////////

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

	////////////////////////////////////////////

	
	// ------------------------------------ survey answers for questions ------------------------------------

	@Expose
	Map<String, Map<String, String>> ratingQuestionAnswer = new HashMap<>();

	@Expose
	Map<String, QuestionAnswer> extraTextQuestionsAnswer = new HashMap<>();
	
	@Expose
	Map<String, QuestionAnswer> singleChoiceQuestionAnswer = new HashMap<>();
	
	@Expose
	Map<String, QuestionAnswer> multipleChoiceQuestionAnswer = new HashMap<>();
	
	// -------------------------------------------------------------------------------------------------------
	
	@Expose
	double feedbackScore = -1;// 1 - 5 or 0 - 10

	@Expose
	List<String> decisionMakers = new ArrayList<String>();

	@Expose
	List<String> originalSources = new ArrayList<String>();

	@Expose
	List<String> mediaSources = new ArrayList<String>();

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

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (dataValidation()) {
			super.cleanXss();
			XssFilterUtil.cleanAllHtmlTags(this, FeedbackEvent.class);
			
			this.buildDateKey();
			String keyHint = refProfileId + header + group + evaluatedObject + feedbackType
					+ surveyChoicesId + refTemplateId + refTouchpointHubId + refProductItemId + refCampaignId
					+ refContentItemId + refTouchpointId + touchpointId + this.createdAt.getTime();
			this.id = createId(this.id, keyHint);
		} 
		else {
			System.err.println(new Gson().toJson(this));
			throw new IllegalArgumentException("check isReadyForSave is failed ");
		}
		return this.id;
	}

	public FeedbackEvent() {
		// gson
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
	
	public FeedbackEvent(String metricName, Date createdAt, String refProfileId, int ratingScore, Touchpoint srcTouchpoint) {
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
			hasAnswer = this.getRatingQuestionAnswer().size() > 0;
			if(!hasAnswer) {
				// for simple survey to collect just basic profile data
				hasAnswer = (StringUtil.isNotEmpty(profileFirstName) || StringUtil.isNotEmpty(profileLastName)) && (StringUtil.isNotEmpty(profileEmail) || StringUtil.isNotEmpty(profilePhone));
			}
		} 
		else if (CONTACT.equals(this.feedbackType)) {
			hasAnswer = StringUtil.isNotEmpty(this.profileEmail) || StringUtil.isNotEmpty(this.profilePhone);
		} 
		else {
			hasAnswer = this.feedbackScore >= 0 && this.feedbackScore <= 10;
		}
		return hasAnswer && this.createdAt != null && StringUtil.isNotEmpty(this.refProfileId)  && StringUtil.isNotEmpty(this.touchpointId) && StringUtil.isNotEmpty(this.touchpointUrl);
	}

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

	public String buildDateKey() {
		DateFormat dateFormat = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_PATTERN);
		Date reportedDate = this.createdAt;
		this.dateKey = dateFormat.format(reportedDate);
		return this.dateKey;
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

	public int getFeedbackScoreInteger() {
		return (int) Math.floor(feedbackScore);
	}

	public void setFeedbackScore(int feedbackScore) {
		this.feedbackScore = feedbackScore;
	}

	public void setFeedbackScore(double feedbackScore) {
		this.feedbackScore = feedbackScore;
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
	
	
	
	public Map<String, Object> getUpdatingProfileAttributes() {
		Map<String, Object> map = this.profileExtAttributes;
		
		map.put("firstName", profileFirstName);
		map.put("lastName", profileLastName);
		map.put("primaryEmail", profileEmail);
		map.put("primaryPhone", profilePhone);
		map.put("gender", profileGender);
		
		map.put("age", profileAge);
		map.put("ageGroup", profileAgeGroup);
		map.put("dateOfBirth", profileDateOfBirth);
		
		map.put("livingLocation", profileLivingLocation);
		map.put("locationCode", profileLocationCode);
		map.put("primaryNationality", profileNationality);
		// TODO add more fields
		
		this.enrichProfileExtAttributes();
		return map;
	}
	
	public Map<String, Object> getProfileExtAttributes() {
		return profileExtAttributes;
	}

	public void setProfileExtAttributes(Map<String, Object> profileExtAttributes) {
		this.profileExtAttributes = profileExtAttributes;
	}
	
	public void enrichProfileExtAttributes() {
		Map<String, Object> newMap = new HashMap<>(this.profileExtAttributes.size());
		this.profileExtAttributes.forEach((t, u) -> {
			int intValue = StringUtil.safeParseInt(u);
			if(t.equalsIgnoreCase("gender")) {
				if(intValue == 1) {
					u = "Male";
				}
				else if(intValue == 0) {
					u = "Female";
				}
			}
			else if(t.equalsIgnoreCase("ageGroup")) {
				u = ProfileAgeGroup.getAsLabelString(intValue);
			}
			if(intValue != -1 && !StringUtil.isEmpty(u)) {
				newMap.put(t, u);
			}
		});
		this.profileExtAttributes = newMap;
	}
	
	public BasicContactData getBasicContactData() {
		return new BasicContactData(profileEmail, profilePhone, profileFirstName, profileLastName);
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}