package leotech.cdp.domain;

import static leotech.cdp.model.journey.EventMetric.FIRST_PARTY_DATA;
import static leotech.cdp.model.journey.EventMetric.SCORING_EFFORT_METRIC;
import static leotech.cdp.model.journey.EventMetric.SCORING_ENGAGEMENT_METRIC;
import static leotech.cdp.model.journey.EventMetric.SCORING_FEEDBACK_METRIC;
import static leotech.cdp.model.journey.EventMetric.SCORING_LEAD_METRIC;
import static leotech.cdp.model.journey.EventMetric.SCORING_LIFETIME_VALUE_METRIC;
import static leotech.cdp.model.journey.EventMetric.SCORING_LOYALTY_METRIC;
import static leotech.cdp.model.journey.EventMetric.SCORING_PROMOTER_METRIC;
import static leotech.cdp.model.journey.EventMetric.SCORING_PROSPECT_METRIC;
import static leotech.cdp.model.journey.EventMetric.SCORING_SATISFACTION_METRIC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoDBException;
import com.google.gson.Gson;

import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.DailyReportUnitDaoUtil;
import leotech.cdp.dao.EventObserverDaoUtil;
import leotech.cdp.dao.FeedbackDataDao;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.TrackingEventDao;
import leotech.cdp.dao.graph.GraphProfile2TouchpointHub;
import leotech.cdp.domain.cache.AdminRedisCacheUtil;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.domain.schema.CustomerFunnel;
import leotech.cdp.domain.schema.FunnelMetaData;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.handler.api.DataApiPayload;
import leotech.cdp.job.reactive.JobUpdateProfileByEvent;
import leotech.cdp.model.RefKey;
import leotech.cdp.model.analytics.DailyReportUnit;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.analytics.UpdateProfileEvent;
import leotech.cdp.model.customer.AbstractProfile;
import leotech.cdp.model.customer.BasicContactData;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.customer.ProfileModelUtil;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.model.customer.ProfileType;
import leotech.cdp.model.customer.ProfileUpdateData;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.customer.UpdateProfileCallback;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.model.social.ZaloUserDetail;
import leotech.cdp.query.filters.ProfileFilter;
import leotech.system.domain.SystemUserManagement;
import leotech.system.model.CsvDataParser;
import leotech.system.model.ImportingResult;
import leotech.system.model.SystemUser;
import leotech.system.util.GeoLocationUtil;
import leotech.system.util.SystemDateTimeUtil;
import leotech.system.util.TaskRunner;
import leotech.system.util.XssFilterUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * Profile Data Management for updating and saving
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class ProfileDataManagement {

	private static final int MINIMUM_DAYS_TO_KEEP_DATA = 3;

	private static final String IMPORTED_FROM_CSV = "imported-from-csv";

	static Logger logger = LoggerFactory.getLogger(ProfileDataManagement.class);

	public static final String FROM_CDP_API = "FROM_CDP_API";
	static final String DIRECT_INPUT = "DIRECT_INPUT";

	/**
	 * this method is used by Data Observer
	 * 
	 * @param UpdateProfileEvent event
	 */
	public static void updateProfileByEvent(UpdateProfileEvent event) {
		JobUpdateProfileByEvent.job().enqueue(event);
	}

	/**
	 * @param observerId
	 * @param srcTouchpoint
	 * @param touchpointRefDomain
	 * @param lastSeenIp
	 * @param visitorId
	 * @param userDeviceId
	 * @param fingerprintId
	 * @return
	 */
	public static Profile updateOrCreateFromWebTouchpoint(String observerId, Touchpoint srcTouchpoint,
			String touchpointRefDomain, String lastSeenIp, String visitorId, String userDeviceId,
			String fingerprintId) {
		Profile p = updateOrCreate(observerId, srcTouchpoint, touchpointRefDomain, lastSeenIp, userDeviceId, visitorId, fingerprintId, new BasicContactData());
		return p;
	}

	/**
	 * @param observerId
	 * @param srcTouchpoint
	 * @param lastSeenIp
	 * @param deviceId
	 * @param contactData
	 * @return
	 */
	public static Profile updateOrCreateFromWebTouchpoint(String observerId, Touchpoint srcTouchpoint,
			String lastSeenIp, String deviceId, BasicContactData contactData) {
		Profile p = updateOrCreate(observerId, srcTouchpoint, srcTouchpoint.getHostname(), lastSeenIp, deviceId, "", "", contactData);
		return p;
	}

	/**
	 * @param observerId
	 * @param srcTouchpoint
	 * @param touchpointRefDomain
	 * @param lastSeenIp
	 * @param userDeviceId
	 * @param visitorId
	 * @param fingerprintId
	 * @param loginProvider
	 * @param loginId
	 * @param contact
	 * @return
	 */
	public static Profile updateOrCreate(String observerId, Touchpoint srcTouchpoint, String touchpointRefDomain,
			String lastSeenIp, String userDeviceId, String visitorId, String fingerprintId, BasicContactData contact) {

		// FIXME use optimize for performance 
		
		// query
		ProfileSingleView profile = ProfileQueryManagement.getProfileByPrimaryKeys(visitorId, contact.email,
				contact.phone, "", "", lastSeenIp, userDeviceId, fingerprintId);

		if (profile == null) {
			if (contact.hasData()) {
				profile = ProfileSingleView.newContactProfile(observerId, srcTouchpoint, lastSeenIp, visitorId,
						userDeviceId, fingerprintId, contact);
			} else {
				profile = ProfileSingleView.newAnonymousProfile(observerId, srcTouchpoint, lastSeenIp, visitorId,
						userDeviceId, fingerprintId);
			}
			profile.engageAtTouchpoint(srcTouchpoint, true);
			profile.updateReferrerChannel(touchpointRefDomain);
			ProfileDaoUtil.insertAsAsynchronousJob(profile);
		} else {
			profile.engageAtTouchpoint(srcTouchpoint, true);
			profile.updateReferrerChannel(touchpointRefDomain);
			profile.setPrimaryEmail(contact.email);
			profile.setPrimaryPhone(contact.phone);
			TouchpointHub hub = TouchpointHubManagement.getByObserverId(observerId);
			if (hub != null) {
				profile.setObserverAndTouchpointHub(observerId, hub.getId());
			}
			profile.setLastSeenIp(lastSeenIp);
			profile.setLastUsedDeviceId(userDeviceId);
			ProfileDaoUtil.updateAsAsynchronousJob(profile, false);
		}
		return profile;
	}

	/**
	 * @param paramJson
	 * @param loginUser
	 * @return
	 */
	public static ProfileSingleView createNewCrmProfile(JsonObject paramJson, SystemUser loginUser) {
		String firstName = paramJson.getString("firstName", "");
		String lastName = paramJson.getString("lastName", "");
		String email = paramJson.getString("email", "");
		String phone = paramJson.getString("phone", "");
		String crmRefId = paramJson.getString("crmRefId", "");
		String funnelStage = paramJson.getString("funnelStage", FunnelMetaData.STAGE_LEAD);

		ProfileSingleView pf = ProfileSingleView.newCrmProfile(email, phone, crmRefId, funnelStage);
		pf.setFirstName(firstName);
		pf.setLastName(lastName);
		ProfileDaoUtil.insert(pf);
		return pf;
	}

	/**
	 * @param observerId
	 * @param fromTouchpoint
	 * @param sourceIP
	 * @param govId
	 * @param phone
	 * @param email
	 * @param socialId
	 * @param appId
	 * @param crmId
	 * @return
	 */
	public static ProfileSingleView createNewProfileAndSave(String fName, String lName, Date createdAt,
			String observerId, Touchpoint fromTouchpoint, String sourceIP, String govId, String phone, String email,
			String socialId, String appId, String crmId) {
		// create new profile from input data
		ProfileSingleView profile = ProfileSingleView.newProfileForAPI(createdAt, observerId, fromTouchpoint, sourceIP);
		profile.setFirstName(fName);
		profile.setLastName(lName);
		profile.setGovernmentIssuedID(govId);
		profile.setPrimaryPhone(phone);
		profile.setPrimaryEmail(email);
		profile.setSocialMediaId(socialId);
		profile.setApplicationID(appId);
		profile.setCrmRefId(crmId);
		String profileId = ProfileDaoUtil.insert(profile);
		if (profileId != null) {
			logger.info("createNewProfile OK with profileId " + profileId);
		}
		return profile;
	}

	/**
	 * @param pf as Profile
	 * @return
	 */
	public static String saveProfile(ProfileSingleView pf) {
		return ProfileDaoUtil.saveProfile(pf);
	}

	/**
	 * @param updateData
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static String updateBasicProfileInfo(ProfileUpdateData updateData) throws IllegalArgumentException {
		String profileId = updateData.getProfileId();
		logger.info("updateBasicProfileInfo ProfileId " + profileId);
		ProfileSingleView profile = ProfileDaoUtil.getProfileById(profileId);

		if (profile == null) {
			throw new IllegalArgumentException("profileId is invalid, not found any profile in database !");
		}

		// BEGIN collect contact data
		boolean isLead = false;
		profile.setFirstName(updateData.getFirstName());
		profile.setLastName(updateData.getLastName());
		isLead = profile.setEmail(updateData.getEmail());
		;
		isLead = profile.setPhone(updateData.getPhone()) || isLead;
		// only an anonymous profile can be set as Lead
		if (isLead && profile.isAnonymousProfile()) {
			profile.setType(ProfileType.CUSTOMER_CONTACT);
		}
		// END collect contact data

		if (StringUtil.isNotEmpty(updateData.getLoginId()) && StringUtil.isNotEmpty(updateData.getLoginProvider())) {
			profile.setSocialMediaProfile(updateData.getLoginProvider(), updateData.getLoginId());
			profile.setType(ProfileType.LOGIN_USER_CONTACT);
		}

		profile.setAge(updateData.getAge());
		profile.setDateOfBirth(updateData.getDateOfBirth());
		

		String genderStr = updateData.getGenderStr();
		profile.setGender(genderStr);

		String livingLocation = updateData.getLivingLocation();
		profile.setLivingLocation(livingLocation);

		String workingHistory = updateData.getWorkingHistory();
		profile.setWorkingHistory(workingHistory);

		String jobTitles = updateData.getJobTitles();
		profile.setJobTitles(jobTitles);

		String personalProblems = updateData.getPersonalProblems();
		profile.setPersonalProblems(personalProblems);

		String usedDeviceId = updateData.getUsedDeviceId();
		profile.setLastUsedDeviceId(usedDeviceId);

		String observerId = updateData.getObserverId();
		TouchpointHub hub = TouchpointHubManagement.getByObserverId(observerId);
		if (hub != null) {
			profile.setObserverAndTouchpointHub(observerId, hub.getId());
		}

		Touchpoint lastTouchpoint = updateData.getSrcTouchpoint();
		profile.engageAtTouchpoint(lastTouchpoint, true);
		profile.setLastSeenIp(updateData.getLastSeenIp());
		profile.setContentKeywords(updateData.getContentKeywords());
		profile.setProductKeywords(updateData.getProductKeywords());
		profile.setLearningCourses(updateData.getLearningCourses());

		if (profile.hasContactData()) {
			// enable chatbot for profile that has contact info
			AdminRedisCacheUtil.setLeoChatBotForProfile(profile.getVisitorId(), profile.getId(),
					profile.getFirstName());

			// TODO
			// ActivationFlowManagement.sendThanksEmail(updatedProfileId, email, firstName);
		}

		// commit to database
		String id = ProfileDaoUtil.saveProfile(profile);

		return id;
	}

	/**
	 * @param profileId
	 * @param provider
	 * @param userId
	 * @return
	 */
	public static void setWebNotificationUserId(String profileId, String provider, String userId) {
		ProfileSingleView p = ProfileDaoUtil.getProfileById(profileId);
		logger.info("setNotificationUserIds " + profileId + " provider " + provider + " userId " + userId);

		// web push User Id
		Map<String, Set<String>> notificationUserIds = new HashMap<>(1);
		notificationUserIds.put(provider, new HashSet<String>(Arrays.asList(userId)));
		p.setWebNotificationUserIds(notificationUserIds);

		// for id resulution
		String extId = userId + "#" + provider;
		p.setIdentities(extId);

		ProfileDaoUtil.updateAsAsynchronousJob(p, false);
	}

	public static final ProfileSingleView searchAndMergeAsUniqueProfile(ProfileIdentity profileIdentity,
			boolean importMode, String funnelStage) {
		EventObserver observer = EventObserverDaoUtil.getDefaultDataObserver();
		Touchpoint fromTouchpoint = TouchpointManagement.LEO_CDP_IMPORTER;
		return searchAndMergeAsUniqueProfile(observer, fromTouchpoint, profileIdentity, importMode, funnelStage);
	}

	/**
	 * @param observer
	 * @param fromTouchpoint
	 * @param profileIdentity
	 * @param importMode
	 * @return
	 */
	public static final ProfileSingleView searchAndMergeAsUniqueProfile(EventObserver observer,
			Touchpoint fromTouchpoint, ProfileIdentity profileIdentity, boolean importMode, String funnelStage) {
		ProfileSingleView finalProfile = ProfileDaoUtil.getByProfileIdentity(profileIdentity);
		// not found, create new profile
		if (finalProfile == null) {
			// create new
			if (importMode) {
				String primaryEmail = profileIdentity.getPrimaryEmail();
				String primaryPhone = profileIdentity.getPrimaryPhone();
				String crmRefId = profileIdentity.getCrmRefId();
				logger.info("newCrmProfile " + primaryEmail + " " + primaryPhone);
				finalProfile = ProfileSingleView.newCrmProfile(primaryEmail, primaryPhone, crmRefId, funnelStage);
			} else {
				finalProfile = ProfileSingleView.newProfileFromTouchpoint(profileIdentity, fromTouchpoint, observer,
						funnelStage);
			}
		}
		return finalProfile;
	}

	/**
	 * @param observerId
	 * @param zaloUser
	 * @return
	 */
	public static Profile getOrCreateFromZaloOA(String observerId, ZaloUserDetail zaloUser) {
		EventObserver observer = EventObserverManagement.getById(observerId);
		return getOrCreateFromZaloOA(observer, zaloUser);
	}

	/**
	 * @param observer
	 * @param zaloUser
	 * @return
	 */
	public static Profile getOrCreateFromZaloOA(EventObserver observer, ZaloUserDetail zaloUser) {
		String observerId = observer.getId();
		ProfileIdentity profileIdentity = new ProfileIdentity(zaloUser);

		// event observer and touchpoint

		Touchpoint fromTouchpoint = TouchpointManagement.getOrCreateNew(observer.getName(), observer.getType(),
				observer.getDataSourceUrl());
		String funnelStage = FunnelMetaData.STAGE_LEAD;

		// search profile
		ProfileSingleView profile = ProfileDataManagement.searchAndMergeAsUniqueProfile(observer, fromTouchpoint, profileIdentity, false, funnelStage);
		if (profile != null) {
			profile.addDataLabel(ZaloUserDetail.ZALO_USER);
			profile.setFirstName(zaloUser.getDisplayName());
			profile.setLastObserverId(observerId);
			profile.setLastTouchpoint(fromTouchpoint);
			profile.setPrimaryAvatar(zaloUser.getAvatar240Url());
			profile.setExtAttributes("Last Interaction Date", zaloUser.getLastInteractionDate());

			// get journey map
			String theJourneyMapId = observer.getJourneyMapId();
			JourneyMap journeyMap = JourneyMapManagement.getById(theJourneyMapId);
			profile.setAuthorizedViewers(journeyMap.getAuthorizedViewers());
			profile.setAuthorizedEditors(journeyMap.getAuthorizedEditors());
			profile.setInJourneyMap(journeyMap.getJourneyMapRefKey(AbstractProfile.CONTACT));

			// save profile into database
			if (profile.isInsertingData()) {
				ProfileDaoUtil.insertProfile(profile, null);
			} else {
				ProfileDaoUtil.saveProfile(profile);
			}

			// update the touchpoint-hub graph
			String touchpointHubId = observer.getTouchpointHubId();
			if (StringUtil.isNotEmpty(touchpointHubId)) {
				EventMetric eventMetric = EventMetricManagement.getEventMetricByName(BehavioralEvent.STR_DATA_IMPORT);
				GraphProfile2TouchpointHub.updateEdgeData(profile, touchpointHubId, theJourneyMapId, eventMetric);
			}
		}
		return profile;
	}

	/**
	 * @param data
	 * @return Profile
	 */
	public static String saveProfileFromApi(DataApiPayload data) {
		System.out.println("BEGIN saveProfileFromApi: " + data);

		try {
			// data from JSON
			JsonObject jsonObjData = data.getJsonObject();
			ProfileIdentity idObj = new ProfileIdentity(jsonObjData);

			// event observer and touchpoint
			EventObserver observer = data.getObserver();
			Touchpoint fromTouchpoint = TouchpointManagement.getOrCreateNew(observer.getName(), observer.getType(),
					observer.getDataSourceUrl());
			String funnelStage = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.FUNNEL_STAGE,
					FunnelMetaData.STAGE_LEAD);

			// search profile
			ProfileSingleView profile = ProfileDaoUtil.getByProfileIdentity(idObj);
			String foundId = (profile != null) ? profile.getId() : " NOT FOUND ANY ID, CREATE NEW PROFILE";
			System.out.println(" [saveProfileFromApi] ProfileIdentity:" + idObj + " profile.getId: " + foundId);

			// not found any matched profile from profileIdentity
			if (profile == null) {
				profile = ProfileSingleView.newProfileFromTouchpoint(idObj, fromTouchpoint, observer, funnelStage);
			}

			// commit to database
			ProfileSingleView finalProfile = profile;
			if (finalProfile != null) {
				// return
				System.out.println("END saveProfileFromApi profileId " + profile.getId());
				TaskRunner.runInThreadPools(() -> {
					saveJsonDataIntoProfile(idObj, finalProfile, observer, jsonObjData);
				});
				return finalProfile.getId();
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("FAIL saveProfileFromApi: " + data);
		}

		return null;
	}

	/**
	 * @param profile
	 * @param jsonObjData
	 * @param pIdentity
	 * @param observer
	 * @return
	 */
	private static boolean saveJsonDataIntoProfile(ProfileIdentity pIdentity, ProfileSingleView profile,
			EventObserver observer, JsonObject jsonObjData) {
		String profileId = profile.getId();
		boolean ok = false;
		System.out.println("BEGIN saveJsonDataIntoProfile.id: " + profileId);

		try {
			// remove keys data from HashMap
			jsonObjData.remove(HttpParamKey.ACCESS_TOKEN_KEY);
			jsonObjData.remove(HttpParamKey.ACCESS_TOKEN_VALUE);

			// set keys for profile
			profile.setCrmRefId(pIdentity.getCrmRefId());
			profile.setEmail(pIdentity.getPrimaryEmail());
			profile.setPhone(pIdentity.getPrimaryPhone());
			profile.setApplicationIDs(pIdentity.getApplicationIDs());
			profile.setGovernmentIssuedIDs(pIdentity.getGovernmentIssuedIDs());
			profile.setLoyaltyIDs(pIdentity.getLoyaltyIDs());
			profile.setFintechSystemIDs(pIdentity.getFintechSystemIDs());
			profile.setSocialMediaProfiles(pIdentity.getSocialMediaProfiles());

			String dataLabels = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.DATA_LABELS);
			profile.setDataLabels(dataLabels, pIdentity.isOverwriteData());
			profile.addDataLabel(FROM_CDP_API);
			jsonObjData.remove(HttpParamKey.DATA_LABELS);

			// to auto merge duplicated profile
			profile.setDeduplicate(pIdentity.isDeduplicate());

			String firstName = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.FIRST_NAME);
			jsonObjData.remove(HttpParamKey.FIRST_NAME);

			String lastName = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.LAST_NAME);
			jsonObjData.remove(HttpParamKey.LAST_NAME);

			String gender = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.GENDER);
			jsonObjData.remove(HttpParamKey.GENDER);

			String createdAtStr = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.CREATED_AT);
			jsonObjData.remove(HttpParamKey.CREATED_AT);
			profile.setCreatedAt(createdAtStr);

			String updatedAtStr = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.UPDATED_AT);
			jsonObjData.remove(HttpParamKey.UPDATED_AT);
			profile.setUpdatedByCrmAt(updatedAtStr);

			// date of birth of profile
			int age = StringUtil.safeParseInt(jsonObjData.getValue(HttpParamKey.AGE), 0);
			jsonObjData.remove(HttpParamKey.AGE);
			profile.setAge(age);

			String dateOfBirthStr = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.DATE_OF_BIRTH);
			jsonObjData.remove(HttpParamKey.DATE_OF_BIRTH);
			Date dateOfBirth = ProfileModelUtil.parseDate(dateOfBirthStr);
			profile.setDateOfBirth(dateOfBirth);

			// personal
			profile.setFirstName(firstName);
			profile.setLastName(lastName);
			profile.setGender(gender);
			profile.setLastObserverId(observer.getId());

			Touchpoint fromTouchpoint = TouchpointManagement.getOrCreateNew(observer.getName(), observer.getType(),
					observer.getDataSourceUrl());
			profile.setLastTouchpoint(fromTouchpoint);

			// get journey map
			String theJourneyMapId = observer.getJourneyMapId();
			String[] journeyMapIds = (jsonObjData.getString("journeyMapIds", "") + ";" + theJourneyMapId)
					.split(StringPool.SEMICOLON);
			jsonObjData.remove("journeyMapIds");

			// set journey map and data authorization
			boolean updateObserverOfJourney = false;
			for (String journeyMapId : journeyMapIds) {
				JourneyMap journeyMap = JourneyMapManagement.getById(journeyMapId.trim());
				profile.setAuthorizedViewers(journeyMap.getAuthorizedViewers());
				profile.setAuthorizedEditors(journeyMap.getAuthorizedEditors());
				profile.setInJourneyMap(journeyMap.getJourneyMapRefKey(AbstractProfile.CONTACT));

				// mark as updated
				if (theJourneyMapId.equals(journeyMapId)) {
					updateObserverOfJourney = true;
				}
			}

			// set theJourneyMapId if not update in the list
			if (!updateObserverOfJourney) {
				JourneyMap journeyMap = JourneyMapManagement.getById(theJourneyMapId);
				profile.setInJourneyMap(journeyMap.getJourneyMapRefKey(AbstractProfile.CONTACT));
			}

			// updateWithAttributeMap from jsonObjData
			Map<String, Object> updatingAttributes = new HashMap<String, Object>(jsonObjData.size());
			jsonObjData.forEach(e -> {
				String key = e.getKey();
				Object value = e.getValue();
				if (!key.startsWith("__")) {
					updatingAttributes.put(key, value);
				}
			});
			profile.updateDataWithAttributeMap(updatingAttributes);

			System.out.println("==> getCrmRefId " + profile.getCrmRefId());
			System.out.println("==> getApplicationID " + profile.getApplicationID());

			// save profile into database

			if (profile.isInsertingData()) {
				ok = ProfileDaoUtil.insertProfile(profile, null) != null;
			} else {
				ok = ProfileDaoUtil.saveProfile(profile) != null;
			}

			// update the touchpoint-hub graph
			String touchpointHubId = observer.getTouchpointHubId();
			if (StringUtil.isNotEmpty(touchpointHubId)) {
				EventMetric eventMetric = EventMetricManagement.getEventMetricByName(BehavioralEvent.STR_DATA_IMPORT);
				GraphProfile2TouchpointHub.updateEdgeData(profile, touchpointHubId, theJourneyMapId, eventMetric);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("FAIL saveJsonDataIntoProfile: " + e.getMessage() + "\n" + jsonObjData);
		}
		System.out.println("END saveJsonDataIntoProfile.id: " + profileId + " saveOK: " + ok);
		return ok;
	}

	/**
	 * @param email
	 * @param visitorId
	 * @param firstName
	 * @param lastName
	 * @param refId
	 * @param source
	 * @param observerId
	 * @param srcTouchpoint
	 * @param refTouchpointId
	 * @param touchpointRefDomain
	 * @param userDeviceId
	 * @param gender
	 * @param createdAt
	 * @return
	 */
	public static Profile saveSocialLoginProfile(String email, String visitorId, String firstName, String lastName,
			String refId, String source, String observerId, Touchpoint srcTouchpoint, String refTouchpointId,
			String touchpointRefDomain, String userDeviceId, int gender, Date createdAt) {
		ProfileSingleView pf = ProfileDaoUtil.getByPrimaryEmail(email);
		boolean createNew = false;
		if (pf == null) {
			pf = ProfileSingleView.newSocialLoginProfile(visitorId, firstName, lastName, email, refId, source);
			createNew = true;
		} else {
			pf.setFirstName(firstName);
			pf.setLastName(lastName);
			pf.setSocialMediaProfile(source, refId);
			pf.setIdentities(visitorId);
		}
		// session touchpoint
		pf.engageAtTouchpoint(srcTouchpoint, true);
		pf.updateReferrerChannel(touchpointRefDomain);
		pf.setLastUsedDeviceId(userDeviceId);
		pf.setCreatedAt(createdAt);
		pf.setGender(gender);

		TouchpointHub hub = TouchpointHubManagement.getByObserverId(observerId);
		if (hub != null) {
			pf.setObserverAndTouchpointHub(observerId, hub.getId());
		}

		if (createNew) {
			ProfileDaoUtil.insert(pf);
		} else {
			ProfileDaoUtil.saveProfile(pf);
		}

		return pf;
	}

	public final static String updateFromJson(String json) {
		return updateFromJson(null, json);
	}

	/**
	 * update profile data must be an instance of ProfileSingleDataView (list -> get
	 * -> update)
	 * 
	 * @param json
	 * @return
	 */
	public final static String updateFromJson(SystemUser loginUser, String json) {
		ProfileSingleView profile = null;
		try {
			profile = new Gson().fromJson(json, ProfileSingleView.class);
			System.out.println("profile.getType " + profile.getType());
			System.out.println("profile.getAuthorizedViewers " + profile.getAuthorizedViewers());
			System.out.println("profile.getAuthorizedEditors " + profile.getAuthorizedEditors());

			if (profile != null) {
				if (loginUser != null) {
					boolean canUpdateProfile = loginUser.checkToEditProfile(profile);
					System.out.println("checkToEditProfile " + loginUser.checkToEditProfile(profile));
					System.out.println("canUpdateProfile " + canUpdateProfile);
					if (!canUpdateProfile) {
						throw new IllegalArgumentException("No authorization to update profile ID: " + profile.getId());
					}
				}
				profile.buildIdentityData();

				// TODO update locationCode , call geolocation query for living location

				if (profile.isNewProfile()) {
					// save as new from Customer Editor
					profile.buildHashedId();
					profile.addDataLabel(DIRECT_INPUT);
				}

				// background
				// make sure segment is correct
				Set<RefKey> inSegments = profile.getInSegments();
				int size = inSegments.size();
				Set<RefKey> inSegmentsVerified = new HashSet<>(size);
				for (RefKey refKey : inSegments) {
					if (refKey != null) {
						String segmentId = refKey.getId();
						Segment sm = SegmentDataManagement.getSegmentById(segmentId);
						if (sm != null) {
							if (sm.getTotalCount() > 0) {
								inSegmentsVerified.add(refKey);
							}
						}
					}
				}
				profile.setInSegmentsAsNew(inSegmentsVerified);

				// save
				ProfileDaoUtil.saveProfile(profile);

				logger.info("updateFromJson profileID = " + profile.getId());
				return profile.getId();
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(" Profile JSON: \n " + json);
		}
		return null;
	}

	/**
	 * @param profileId
	 * @return
	 */
	public final static boolean checkAndRemove(SystemUser loginUser, String profileId) {
		ProfileSingleView profile = ProfileDaoUtil.getProfileById(profileId);
		if (loginUser != null) {
			boolean canDeleteProfile = !loginUser.checkToEditProfile(profile);
			if (canDeleteProfile) {
				throw new IllegalArgumentException("No authorization to remove " + profile.getId());
			}
		}

		boolean ok = false;
		// ready to remove
		if (profile.checkToDeleteForever()) {
			deleteAllRelatedDataOfProfile(profileId);
		} else {
			profile.setStatus(Profile.STATUS_REMOVED);
			ok = ProfileDaoUtil.saveProfile(profile) != null;
			
			if(ok) {
				Analytics360Management.clearCacheProfileReport();
			}
		}
		return ok;
	}

	/**
	 * @param profile
	 * @param newEvents as the list of TrackingEvent
	 * @return true if update OK
	 */
	public final static boolean updateProfileFromEvents(ProfileSingleView profile, List<TrackingEvent> newEvents) {
		return updateProfileFromEvents(profile, false, newEvents);
	}

	/**
	 * @param profile
	 * @param newEvent as TrackingEvent
	 * @return true if update OK
	 */
	public final static boolean updateProfileFromEvents(ProfileSingleView profile, TrackingEvent newEvent) {
		return updateProfileFromEvents(profile, false, Arrays.asList(newEvent));
	}

	/**
	 * @param visitorId
	 * @return
	 */
	public final static TrackingEvent getLastTrackingEventForRetargeting(String visitorId) {
		Profile profile = ProfileDaoUtil.getByVisitorId(visitorId);
		if (profile != null) {
			String profileId = profile.getId();
			logger.info(" [#Activation Trigger#] getLastTrackingEventForRetargeting Profile is found, profileId "
					+ profileId);

			// get last 5 products that profile watched
			List<TrackingEvent> events = EventDataManagement.getProductViewActivityFlowOfProfile(profileId, 0, 5);
			logger.info("getProductViewActivityFlowOfProfile events.size() = " + events.size());
			if (events.size() > 0) {
				// find the last recent product-view to get Product SKU
				return events.get(0);
			}
		} else {
			logger.info(" [#Activation Trigger#] getLastTrackingEventForRetargeting Profile is not found for visitorId "
					+ visitorId);
		}
		return null;
	}

	/**
	 * the primary method to update single-view 360 analytics for profile
	 * 
	 * @param profile
	 * @param computeAllEventsInDatabase
	 * @return
	 */
	public final static boolean updateProfileFromEvents(final ProfileSingleView profile, boolean startAtBeginning, List<TrackingEvent> eventDataList) {
		String profileId = profile.getId();
		int daysSinceLastUpdate = SystemDateTimeUtil.getDaysSinceLastUpdate(profile.getUpdatedAt());

		// scoring fields
		int totalProspectScore = 0;
		int totalLeadScore = 0;
		int totalEngageScore = 0;
		int loyaltyPoint = 0;
		// TODO need a unit test case

		boolean noEventData = CollectionUtils.isEmpty(eventDataList);
		boolean processAll = startAtBeginning && noEventData;
		if (processAll) {
			// now ignore unprocessed event, reset to zero to recompute
			profile.clearAllReportData();
			DailyReportUnitDaoUtil.deleteProfileDailyReportUnit(profile.getDailyReportUnits());
		} else {
			totalLeadScore = profile.getTotalLeadScore();
			totalProspectScore = profile.getTotalProspectScore();
			totalEngageScore = profile.getTotalEngagementScore();
			loyaltyPoint = profile.getTotalLoyaltyScore();
		}

		// init to get first 200 unprocessed events of profile
		int startIndex = 0;
		int numberResult = 200;

		// TODO refactoring
		// get current funnel of profile
		// to find what is highest score from event data flow
		EventMetric highestMetric = null;

		// the list to be processed

		if (processAll) {
			eventDataList = EventDataManagement.getTrackingEventsOfProfile(profileId, "", startIndex, numberResult);
			if (eventDataList.size() == 0 && profile.setInJourneyMapDefaultForLead()) {
				TrackingEvent dataImportEvent = TrackingEvent.newImportEventForProfile(profile, null);
				TrackingEventDao.save(dataImportEvent);
				eventDataList.add(dataImportEvent);
			}
		} else if (startAtBeginning) {
			// try to process all event state == 0
			eventDataList = EventDataManagement.getUnprocessedEventsOfProfile(profileId, startIndex, numberResult);
		}

		// loop to update all events
		while (!eventDataList.isEmpty()) {
			logger.info("# updateProfileSingleDataView eventDataList.size = " + eventDataList.size());
			// loop in the page
			for (TrackingEvent event : eventDataList) {
				String eventName = event.getMetricName();

				// get metadata of event
				EventMetric eventMetric = EventMetricManagement.getEventMetricByName(eventName);
				// only 1st-data is computed in real-time
				boolean isFirstPartyData = eventMetric.getDataType() == FIRST_PARTY_DATA;
				// ignore all unknow event
				boolean isEventInFlowSchema = eventMetric.getScore() == 0;

				// only event in defined funnel is processed
				if (eventMetric != null) {
					String journeyId = event.getRefJourneyId();
					String observerId = event.getObserverId();
					String srcTouchpointId = event.getSrcTouchpointId();
					Map<String, Object> eventData = event.getEventData();

					EventObserver eventObserver = EventObserverManagement.getById(observerId);
					Touchpoint srcTouchpoint = TouchpointManagement.getById(srcTouchpointId);

					if (StringUtil.isEmpty(journeyId)) {
						journeyId = JourneyMapManagement.getIdFromObserverOrTouchpoint(eventObserver, srcTouchpoint);
					}

					// loyalty
					loyaltyPoint += eventMetric.getCumulativePoint();

					int eventMetricScore = eventMetric.getScore();
					int scoreModel = eventMetric.getScoreModel();
					if (scoreModel == SCORING_LEAD_METRIC) {
						totalLeadScore += eventMetricScore;
					} else if (scoreModel == SCORING_PROSPECT_METRIC) {
						totalProspectScore += eventMetricScore;
					} else if (scoreModel == SCORING_ENGAGEMENT_METRIC || scoreModel == SCORING_PROMOTER_METRIC
							|| scoreModel == SCORING_LOYALTY_METRIC || scoreModel == SCORING_SATISFACTION_METRIC) {
						totalEngageScore += eventMetricScore;
					} else if (scoreModel == SCORING_LIFETIME_VALUE_METRIC || scoreModel == SCORING_FEEDBACK_METRIC
							|| scoreModel == SCORING_EFFORT_METRIC) {
						boolean hasTransactionalData = profile.hasTransactionalData(journeyId);
						if (hasTransactionalData) {
							totalEngageScore += eventMetricScore;
						} else if (totalProspectScore > 0) {
							totalProspectScore += eventMetricScore;
						} else {
							totalLeadScore += eventMetricScore;
						}
					}

					// get to update profile at the highest score funnel stage
					if (highestMetric == null) {
						if (isFirstPartyData && isEventInFlowSchema) {
							highestMetric = eventMetric;
						}
					} else {
						if (eventMetricScore > highestMetric.getScore()) {
							highestMetric = eventMetric;
						}
					}

					// commit event to Database
					if (processAll) {
						boolean isEventFromProfile = !eventName.equals(BehavioralEvent.STR_DATA_IMPORT);
						if (isEventFromProfile) {
							JourneyMap journeyMap = JourneyMapManagement.getById(journeyId);
							int funnelIndex = eventMetric.getFunnelStage().getOrderIndex();
							int journeyStage = eventMetric.getJourneyStage();
							profile.setInJourneyMap(journeyMap, journeyStage, funnelIndex);
							event.setRefJourneyId(journeyMap.getId());
							event.setJourneyStage(journeyStage);
							TrackingEventDao.save(event);
						}
					} else if (startAtBeginning) {
						// update state of event is done when recompute data
						TrackingEventDao.updateProcessedStateForEvent(event);
					}

					// UTM tracking and contact
					profile.updateFromEventData(eventData);
					

					// update to Daily Report Unit of profile
					profile.updateDailyReportUnit(event.getCreatedAt(), eventName, journeyId);

					// using the last attribution to activate the next best actions
					// reset to empty
					event.setRawJsonData("");
					profile.setLastTrackingEvent(event);
					if (BehavioralEvent.STR_ITEM_VIEW.equals(eventName)) {
						profile.setLastItemViewEvent(event);
					} 
					else if (eventMetric.isPurchasingEvent()) {
						profile.setLastPurchaseEvent(event);
					}

				}
			}

			// loop to the end to reach all events of profile in database
			startIndex = startIndex + numberResult;

			// go to next page of events
			if (processAll && highestMetric != null) {
				eventDataList = EventDataManagement.getTrackingEventsOfProfile(profileId, "", startIndex, numberResult);
			} else {
				eventDataList = EventDataManagement.getUnprocessedEventsOfProfile(profileId, startIndex, numberResult);
			}
		}

		// update score
		profile.setTotalLeadScore(totalLeadScore);
		profile.setTotalProspectScore(totalProspectScore);
		profile.setTotalEngagementScore(totalEngageScore);
		profile.setTotalLoyaltyScore(loyaltyPoint);

		// check Is Anonymous Profile and update if data has email or phone and first
		// name or last name
		if (profile.isAnonymousProfile() && profile.hasContactData()) {
			profile.setType(ProfileType.CUSTOMER_CONTACT);
		}

		// total data quality
		profile.computeDataQualityScore(daysSinceLastUpdate);

		// FINAL update profile data into database
		boolean updateResult = ProfileDaoUtil.saveProfile(profile) != null;

		if (updateResult) {
			Map<String, DailyReportUnit> dailyReportUnits = profile.getDailyReportUnits();
			DailyReportUnitDaoUtil.updateProfileDailyReportUnit(profileId, dailyReportUnits);
		}
		return updateResult;
	}

	/**
	 * parse the imported CSV File
	 * 
	 * @param importFileUrl
	 * @param previewTop30
	 * @return
	 */
	public static List<ProfileSingleView> parseToImportProfiles(String importFileUrl, boolean previewTop20) {
		logger.info(" parseToImportProfiles importFileUrl= " + importFileUrl);
		CsvDataParser<ProfileSingleView> parser = new CsvDataParser<ProfileSingleView>(importFileUrl, previewTop20) {
			@Override
			public ProfileSingleView createObjectFromCsvRow(String[] headers, String[] csvDataRow) {
				return ProfileModelUtil.importProfileFromCsvData(headers, csvDataRow);
			}
		};
		return parser.parseImportedCsvFile();
	}

	/**
	 * @param importFileUrl
	 * @param observerId
	 * @param dataLabelsStr
	 * @param funnelStage
	 * @return
	 */
	public static ImportingResult importCsvAndSaveProfile(String importFileUrl, String observerId, String dataLabelsStr,
			String funnelStage, boolean overwriteOldData) {
		String userLogin = SystemUser.SUPER_ADMIN_LOGIN;
		String funnelStageId = StringUtil.isEmpty(funnelStage) ? CustomerFunnel.FUNNEL_STAGE_LEAD.getId() : funnelStage;

		EventObserver observer = EventObserverManagement.getById(observerId);
		TouchpointHub tpHub = TouchpointHubManagement.getByObserverId(observer.getId());
		String touchpointHubId = tpHub.getId();
		String journeyMapId = tpHub.getJourneyMapId();

		String dataLabels = StringUtil.isEmpty(dataLabelsStr) ? IMPORTED_FROM_CSV : dataLabelsStr;
		dataLabels = dataLabels + ";" + tpHub.getName();

		int journeyStage = EventMetric.JOURNEY_STAGE_ATTRACTION;
		int contactType = ProfileType.CRM_IMPORTED_CONTACT;

		ImportingResult rs = ProfileDataManagement.importAndSave(userLogin, importFileUrl, dataLabels, funnelStageId,
				journeyMapId, touchpointHubId, journeyStage, contactType, overwriteOldData, null);
		return rs;
	}

	/**
	 * @param ownerUserLogin                the owner of data
	 * @param importFileUrl                 in CSV format
	 * @param dataLabelsStr
	 * @param funnelStageId
	 * @param journeyMapId
	 * @param touchpointHubId
	 * @param journeyStage
	 * @param contactType
	 * @param verifiedDataAndUpdateExisting
	 * @return
	 */
	public static ImportingResult importAndSave(String ownerUserLogin, String importFileUrl, String dataLabelsStr,
			String funnelStageId, String journeyMapId, String touchpointHubId, int journeyStage, int contactType,
			boolean verifiedDataAndUpdateExisting) {
		return importAndSave(ownerUserLogin, importFileUrl, dataLabelsStr, funnelStageId, journeyMapId, touchpointHubId,
				journeyStage, contactType, verifiedDataAndUpdateExisting, null);
	}

	/**
	 * @param ownerUserLogin                the owner of data
	 * @param importFileUrl                 in CSV format
	 * @param dataLabelsStr
	 * @param funnelStageId
	 * @param journeyMapId
	 * @param touchpointHubId
	 * @param journeyStage
	 * @param contactType
	 * @param verifiedDataAndUpdateExisting
	 * @param callback
	 * @return
	 */
	public static ImportingResult importAndSave(String ownerUserLogin, String importFileUrl, String dataLabelsStr,
			String funnelStageId, String journeyMapId, String touchpointHubId, int journeyStage, int contactType,
			boolean verifiedDataAndUpdateExisting, UpdateProfileCallback callback) {
		AtomicInteger okCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();
		logger.info(" importeFromCsvAndSaveProfile importFileUrl= " + importFileUrl);

		if (StringUtil.isNotEmpty(importFileUrl)) {
			// parse data labels
			Set<String> dataLabels = new HashSet<String>(10);
			if (StringUtil.isNotEmpty(dataLabelsStr)) {
				String[] toks = dataLabelsStr.split(StringPool.SEMICOLON);
				for (String dataLabel : toks) {
					dataLabel = dataLabel.trim();
					dataLabels.add(dataLabel);
				}
			}

			// parse CSV file into profile list
			List<ProfileSingleView> profiles = parseToImportProfiles(importFileUrl, false);
			profiles.parallelStream().forEach(profile -> {
				String id = null;
				try {
					profile.setDataLabels(dataLabels);
					profile.setFunnelStage(funnelStageId);
					profile.setType(contactType);

					// default rule: the SalesAgents, has matched email of system user, can view
					// data
					List<String> saleAgents = new ArrayList<>(profile.getSaleAgents());
					for (String saleAgentEmail : saleAgents) {
						String userLogin = SystemUserManagement.getUserLoginByEmail(saleAgentEmail);
						if (StringUtil.isNotEmpty(userLogin)) {
							profile.setAuthorizedViewer(userLogin);
						}
					}

					// set default journey map
					JourneyMap journeyMap = JourneyMapManagement.getById(journeyMapId);
					int funnelIndex = DataFlowManagement.getFunnelStageById(funnelStageId).getOrderIndex();
					profile.setInJourneyMap(journeyMap, journeyStage, funnelIndex);

					// set data authorization for the user, who imported data
					profile.setAuthorizedViewer(ownerUserLogin);
					profile.setAuthorizedEditor(ownerUserLogin);

					TouchpointHub touchpointHub = TouchpointHubManagement.getById(touchpointHubId);
					if (touchpointHub != null) {
						profile.setFromTouchpointHubId(touchpointHubId);
						profile.setLastObserverId(touchpointHub.getObserverId());
						profile.updateReferrerChannel(touchpointHub.getHostname());
					}

					boolean identityResolution = false;
					if (verifiedDataAndUpdateExisting) {
						profile.setDataVerification(true);
						identityResolution = true;
					}

					// SAVE profile
					id = ProfileDaoUtil.saveProfile(profile, identityResolution);

					TrackingEvent dataImportEvent = TrackingEvent.newImportEventForProfile(profile, touchpointHub);
					TrackingEventDao.save(dataImportEvent);

					EventMetric eventMetric = EventMetricManagement
							.getEventMetricByName(dataImportEvent.getMetricName());
					GraphProfile2TouchpointHub.updateEdgeData(profile, touchpointHubId, journeyMapId, eventMetric);

					if (callback != null) {
						callback.setDataAndRun(profile);
					}
				} catch (ArangoDBException e) {
					e.printStackTrace();
				}

				if (id != null) {
					int ok = okCount.incrementAndGet();
					System.out.println(ok + " save profile " + id);
					logger.info(okCount + " importeFromCsvAndSaveProfile id= " + id);
				} else {
					failCount.incrementAndGet();
				}
			});

			if (okCount.get() > 0) {
				TaskRunner.runInThreadPools(() -> {
					Utils.sleep(1000);
					SegmentDataManagement.refreshDataOfAllSegments();
				});
			}
		}
		return new ImportingResult(okCount.get(), failCount.get());
	}

	/**
	 * @param profile
	 */
	public static void updategGeoLocationForProfile(Profile profile) {
		boolean check = StringUtil.isNotEmpty(profile.getLivingLocation())
				&& StringUtil.isEmpty(profile.getLocationCode());
		if (check) {
			String lc = GeoLocationUtil.getLocationCodeFromLocationQuery(profile.getLivingLocation());
			profile.setLocationCode(lc);
		}
	}

	/**
	 * set default ext attribute map for all profiles
	 * 
	 * @param extAttributes
	 */
	public static void setProfileDefaultExtAttributes(Map<String, Object> extAttributes) {
		ProfileDaoUtil.saveProfileExtAttributes(extAttributes);
	}

	/**
	 * delete all not-active profiles
	 * 
	 * @param status
	 * @return
	 */
	public static int deleteNotActiveProfile(int status) {
		if (status == Profile.STATUS_ACTIVE) {
			throw new IllegalArgumentException("The status value must be not equals to 1 (Profile.STATUS_ACTIVE) !");
		}
		int batchLength = 500;
		int start = 0;

		ProfileFilter filter = new ProfileFilter();
		filter.setStatus(status);
		int recordsToDelete = ProfileDaoUtil.getTotalRecordsFiltered(filter);

		filter.setStart(start);
		filter.setLength(batchLength);

		List<Profile> list = ProfileDaoUtil.getProfilesByFilter(filter);

		while (!list.isEmpty()) {
			List<String> deletedIds = new ArrayList<String>(list.size());

			// loop and delete
			for (Profile profile : list) {
				deletedIds.add(profile.getId());
			}
			start += batchLength;
			filter.setStart(start);
			list = ProfileDaoUtil.getProfilesByFilter(filter);

			TaskRunner.runInThreadPools(() -> {
				for (String deletedId : deletedIds) {
					deleteAllRelatedDataOfProfile(deletedId);
				}
			});
		}

		if (recordsToDelete > 0) {
			SegmentDataManagement.refreshDataOfAllSegments();
			Analytics360Management.clearCacheProfileReport();
			logger.info(" deleteNotActiveProfile status: " + status + " recordsToDelete: " + recordsToDelete);
		}
		return recordsToDelete;
	}

	/**
	 * delete all dead visitors with FILTER <br>
	 * p.totalLeadScore == 0 AND p.firstName == "" AND p.lastName == "" AND
	 * p.primaryEmail == "" AND p.primaryPhone == "" AND p.crmRefId == "" AND p.type
	 * == 0
	 * 
	 * @return deleted profile
	 */
	public static final long deleteAllDeadVisitors() {
		long c1 = ProfileDaoUtil.deleteAllDeadVisitors();
		logger.info("deleteAllDeadVisitors " + c1);

		int n = SystemMetaData.NUMBER_OF_DAYS_TO_KEEP_DEAD_VISITOR;
		logger.info("numberOfDaysToKeepDeadVisitor " + n);
		int c2 = removeAllInactiveProfiles(3000, n);
		logger.info("removeAllInactiveProfiles " + c2);

		int e = removeEventsofDeadProfiles(3000);
		logger.info("removeEventsofDeadProfiles " + e);

		long rs = c1 + c2;
		if(rs > 0) {
			Analytics360Management.clearCacheProfileReport();
		}
		return rs;
	}

	/**
	 * 
	 * 
	 * @param limitSize
	 * @param numberOfDays
	 * @return the number of removed profile
	 */
	public static final int removeAllInactiveProfiles(int limitSize, int numberOfDays) {
		int maxRetry = 100;
		int totalDeleted = 0;
		if (numberOfDays >= MINIMUM_DAYS_TO_KEEP_DATA) {
			int deleted = ProfileDaoUtil.removeInactiveProfiles(limitSize, numberOfDays);
			totalDeleted += deleted;
			while (deleted > 0 && maxRetry > 0) {
				deleted = ProfileDaoUtil.removeInactiveProfiles(limitSize, numberOfDays);
				maxRetry--;
				totalDeleted += deleted;
			}

			Analytics360Management.clearCacheProfileReport();
		}
		return totalDeleted;
	}

	/**
	 * @param limitSize
	 * @return
	 */
	public static final int removeEventsofDeadProfiles(int limitSize) {
		int maxRetry = 100;
		int totalDeleted = 0;
		int deleted = ProfileDaoUtil.deleteDataOfDeadProfiles(limitSize);
		while (deleted > 0 && maxRetry > 0) {
			deleted = ProfileDaoUtil.deleteDataOfDeadProfiles(limitSize);
			maxRetry--;
			totalDeleted += deleted;
		}
		Analytics360Management.clearCacheProfileReport();
		return totalDeleted;
	}

	/**
	 * clean all deleted profiles in database
	 * 
	 * @return
	 */
	public static long cleanData() {

		long d2 = deleteNotActiveProfile(Profile.STATUS_REMOVED);
		logger.info(" delete STATUS_REMOVED profile " + d2);

		long d3 = deleteNotActiveProfile(Profile.STATUS_INVALID);
		logger.info(" delete STATUS_INVALID profile " + d3);

		long d1 = deleteAllDeadVisitors();
		logger.info(" deleteAllDeadVisitors " + d1);

		return d1 + d2 + d3;
	}

	/**
	 * delete a profile and all related data
	 * 
	 * @param profileId
	 * @return
	 */
	static boolean deleteAllRelatedDataOfProfile(String profileId) {
		boolean ok = ProfileDaoUtil.delete(profileId) != null;
		if (ok) {
			// delete feedback 
			FeedbackDataDao.deleteDataByProfileId(profileId);
			// delete event
			TrackingEventDao.deleteDataByProfileId(profileId);
			// clear cache
			Analytics360Management.clearCacheProfileReport();
			// TODO delete touchpoints and target media unit 
		}
		return ok;
	}

	/**
	 * @param userLogin
	 */
	public static void removeAllViewableProfiles(String userLogin) {
		ProfileDaoUtil.removeAllViewableProfiles(userLogin);
	}

	/**
	 * @param userLogin
	 */
	public static void removeAllEditableProfiles(String userLogin) {
		ProfileDaoUtil.removeAllEditableProfiles(userLogin);
	}

	/**
	 * @param paramJson
	 * @return
	 */
	public static int batchUpdateProfiles(JsonObject jsonObject) {
		JsonObject paramJson = jsonObject.copy(); // clone data to avoid ConcurrentModificationException
		Set<String> selectedProfileIds = new HashSet<String>(paramJson.size());
		paramJson.forEach(e -> {
			String updateId = e.getKey();
			String command = StringUtil.safeString(e.getValue(), "");
			if ("removed".equals(command)) {
				logger.info("batchUpdate.removed profileId " + updateId);
				selectedProfileIds.add(updateId);
			}
		});
		ProfileDaoUtil.batchUpdateProfileStatus(selectedProfileIds, Profile.STATUS_REMOVED);
		return selectedProfileIds.size();
	}

}
