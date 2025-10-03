package leotech.cdp.handler.api;

import java.util.Date;
import java.util.List;
import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.EventDataManagement;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.ProfileQueryManagement;
import leotech.cdp.domain.TouchpointManagement;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.analytics.OrderTransaction;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.customer.Device;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileModelUtil;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointType;
import leotech.system.model.JsonDataPayload;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.LogUtil;
import leotech.system.util.UrlUtil;
import leotech.system.util.XssFilterUtil;
import rfx.core.util.StringUtil;

/**
 * Event Data API
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class EventApiHandler extends BaseApiHandler {

	static final JsonDataPayload FAIL = JsonDataPayload.fail("You should set parameter " + HttpParamKey.PRIMARY_EMAIL + " or " + HttpParamKey.PRIMARY_PHONE);
	static final String API_EVENT_LIST = "/api/event/list";
	static final String API_EVENT_SAVE = "/api/event/save";

	/**
	 * to save tracking event
	 * 
	 * @param observer
	 * @param req
	 * @param uri
	 * @param paramJson
	 * @return
	 */
	@Override
	protected JsonDataPayload handlePost(EventObserver observer, HttpServerRequest req, String uri,
			JsonObject paramJson) {
		System.out.println(paramJson);

		JsonDataPayload payload = null;
		
		try {
			switch (uri) {
			case API_EVENT_SAVE:
				String eventName = StringUtil.safeString(paramJson.getString(HttpParamKey.EVENT_METRIC_NAME)).toLowerCase();
				String eventId = saveEventHandler(observer, req, eventName, paramJson);
				payload = JsonDataPayload.ok(uri, eventId);
				break;
			default:
				payload = notFoundHttpHandler(uri);
				break;
			}
		} catch (Exception e) {
			payload = JsonDataPayload.fail(e.getMessage());
			e.printStackTrace();
		}

		return payload;
	}

	/**
	 * to list events from HTTP GET
	 * 
	 * @param observer
	 * @param req
	 * @param uri
	 * @param urlParams
	 * @return
	 */
	@Override
	protected JsonDataPayload handleGet(EventObserver observer, HttpServerRequest req, String uri, MultiMap urlParams) {
		JsonDataPayload payload = null;
		if (uri.equals(API_EVENT_LIST)) {
			payload = listEventStreamOfProfile(uri, urlParams);
		}
		return payload;
	}

	/**
	 * @param uri
	 * @param urlParams
	 * @return
	 */
	private JsonDataPayload listEventStreamOfProfile(String uri, MultiMap urlParams) {
		JsonDataPayload payload;
		Profile profile = null;
		String email = urlParams.get(HttpParamKey.PRIMARY_EMAIL);
		String phone = urlParams.get(HttpParamKey.PRIMARY_PHONE);
		String crmRefId = urlParams.get(HttpParamKey.CRM_REF_ID);
		String applicationIDs = urlParams.get(HttpParamKey.APPLICATION_IDS);
		String governmentIssuedIDs = urlParams.get(HttpParamKey.GOVERNMENT_ISSUED_IDS);
		String socialMediaProfiles = urlParams.get(HttpParamKey.SOCIAL_MEDIA_PROFILES);

		if (StringUtil.isNotEmpty(email)) {
			profile = ProfileQueryManagement.getByPrimaryEmail(email);
		} 
		else if (StringUtil.isNotEmpty(phone)) {
			profile = ProfileQueryManagement.getByPrimaryPhone(phone);
		} 
		else if (StringUtil.isNotEmpty(crmRefId)) {
			profile = ProfileQueryManagement.getByCrmId(crmRefId);
		} 
		else if (StringUtil.isNotEmpty(applicationIDs)) {
			// KiotViet, Pancake, Facebook User ID, Google User ID,...
			profile = ProfileQueryManagement.getByApplicationID(applicationIDs);
		}
		else if (StringUtil.isNotEmpty(governmentIssuedIDs)) {
			// CCCD, CMND, Social Security number (SSN) ,...
			profile = ProfileQueryManagement.getByGovernmentIssuedID(governmentIssuedIDs);
		}
		else if (StringUtil.isNotEmpty(socialMediaProfiles)) {
			// "zalo:123456789", "facebook:123456789", "linkedin:123456789"
			profile = ProfileQueryManagement.getBySocialMediaId(socialMediaProfiles);
		}

		if (profile != null) {
			String profileId = profile.getId();
			System.out.println("PREFIX_API_EVENT_LIST profileId " + profileId);

			int startIndex = HttpWebParamUtil.getInteger(urlParams, "startIndex", 0);
			int result = HttpWebParamUtil.getInteger(urlParams, "numberResult", 20);
			List<TrackingEvent> list = EventDataManagement.getTrackingEventsOfProfile(profileId, "", startIndex, result);
			payload = JsonDataPayload.ok(uri, list);
		} else {
			payload = JsonDataPayload.fail("Not found any profile from urlParams: " + urlParams);
		}
		return payload;
	}

	/**
	 * to create a tracking event for specific profile
	 * 
	 * @param req
	 * @param params
	 * @param eventName
	 * @param jsonData
	 * @param email
	 * @return
	 */
	protected static String saveEventHandler(EventObserver observer, HttpServerRequest req, String eventName, JsonObject jsonData) {
		LogUtil.logInfo(EventApiHandler.class,"["+ new Date()+ "] processTrackedEvent.jsonData " + jsonData);
		
		String environment = jsonData.getString(HttpParamKey.DATA_ENVIRONMENT, HttpParamKey.PRO_ENV);
		
		// observer 
		String observerId = observer.getId();
		String journeyMapId = observer.getJourneyMapId();

		// profile identities
		String firstName = jsonData.getString(HttpParamKey.FIRST_NAME, "");
		String lastName = jsonData.getString(HttpParamKey.LAST_NAME, "");
		String name = jsonData.getString(HttpParamKey.NAME, Profile.UNKNOWN_PROFILE);
		if(firstName.isBlank() && lastName.isBlank()) {
			firstName = name;
		}
		
		// update vent for who ?
		String email = jsonData.getString(HttpParamKey.TARGET_UPDATE_EMAIL, "");
		String phone = jsonData.getString(HttpParamKey.TARGET_UPDATE_PHONE, "");
		String crmId = jsonData.getString(HttpParamKey.TARGET_UPDATE_CRMID, "");
		String appId = jsonData.getString(HttpParamKey.TARGET_UPDATE_APPLICATION_ID, "");
		String socialId = jsonData.getString(HttpParamKey.TARGET_UPDATE_SOCIAL_MEDIA_ID, "");
		String govId = jsonData.getString(HttpParamKey.TARGET_UPDATE_GOVERNMENT_ISSUED_ID, "");
		
		// from IP
		String sourceIP =   jsonData.getString(HttpParamKey.SOURCE_IP, HttpWebParamUtil.getRemoteIP(req)); 
		String userAgent =   jsonData.getString(HttpParamKey.USER_AGENT, ""); 
		Device userDevice = new Device(userAgent);
		
		int touchpointType = HttpWebParamUtil.getInteger(jsonData, HttpParamKey.TOUCHPOINT_TYPE, TouchpointType.DATA_OBSERVER);
		String srcTouchpointName = XssFilterUtil.safeGet(jsonData, HttpParamKey.TOUCHPOINT_NAME, observer.getName());
		String srcTouchpointUrl = XssFilterUtil.safeGet(jsonData, HttpParamKey.TOUCHPOINT_URL, observer.getDataSourceUrl());
		Touchpoint fromTouchpoint = TouchpointManagement.getOrCreateNew(srcTouchpointName, touchpointType, srcTouchpointUrl);
		
		String refTouchpointUrl = XssFilterUtil.safeGet(jsonData, HttpParamKey.TOUCHPOINT_REFERRER_URL);
		String touchpointRefDomain = UrlUtil.getHostName(refTouchpointUrl);
		
		// save event fro profile
		String eventtime = XssFilterUtil.safeGet(jsonData, HttpParamKey.EVENT_TIME);
		Date createdAt;
		if(StringUtil.isNotEmpty(eventtime)) {
			// parse datetime in UTC format: https://stackoverflow.com/questions/2201925/converting-iso-8601-compliant-string-to-java-util-date
			createdAt = ProfileModelUtil.parseDate(eventtime);
		}
		else {
			createdAt = new Date();
		}
		
		// query profile by identities
		Profile profile = queryProfileByKeys(email, phone, crmId, appId, socialId, govId);
		
		if(profile == null) {
			// create and save new profile from event
			profile = ProfileDataManagement.createNewProfileAndSave(firstName, lastName, createdAt, observerId, fromTouchpoint, 
					sourceIP, govId, phone, email, socialId, appId, crmId);
		}
		
		int ratingScore = jsonData.getInteger(HttpParamKey.RATING_SCORE, -1);
		String message = XssFilterUtil.safeGet(jsonData, HttpParamKey.MESSAGE_TEXT);
		String rawJsonData = XssFilterUtil.safeGet(jsonData, HttpParamKey.EVENT_RAW_JSON_DATA);
		String imageUrls = XssFilterUtil.safeGet(jsonData, HttpParamKey.IMAGE_URLS);
		String videoUrls = XssFilterUtil.safeGet(jsonData, HttpParamKey.VIDEO_URLS);
		Map<String, Object> eventdata = HttpWebParamUtil.getMapFromRequestParams(jsonData,HttpParamKey.EVENT_DATA);
		
		// save transaction
		boolean computeTotalTransactionValue = BehavioralEvent.STR_PURCHASE.equalsIgnoreCase(eventName) || BehavioralEvent.STR_MADE_PAYMENT.equalsIgnoreCase(eventName); 
		OrderTransaction transaction = new OrderTransaction(createdAt, jsonData, computeTotalTransactionValue);
		
		// save event
		String eventId = EventObserverManagement.saveEventFromApi(observerId, createdAt, profile, journeyMapId, environment,
				sourceIP, userDevice, touchpointType, srcTouchpointName, srcTouchpointUrl, refTouchpointUrl,
				touchpointRefDomain, eventName, message, eventdata, rawJsonData, transaction, ratingScore, imageUrls, videoUrls);
		return eventId;
	}

	/**
	 * @param email
	 * @param phone
	 * @param crmId
	 * @param appId
	 * @param socialId
	 * @param govId
	 * @param profile
	 * @return
	 */
	private static Profile queryProfileByKeys(String email, String phone, String crmId, String appId, String socialId,
			String govId) {
		Profile profile = null;
		if (StringUtil.isNotEmpty(govId)) {
			// CCCD, CMND, Social Security number (SSN) ,...
			profile = ProfileQueryManagement.getByGovernmentIssuedID(govId);
		}
		else if (StringUtil.isNotEmpty(phone)) {
			profile = ProfileQueryManagement.getByPrimaryPhone(phone);
		} 
		else if (StringUtil.isNotEmpty(email)) {
			profile = ProfileQueryManagement.getByPrimaryEmail(email);
		}
		else if (StringUtil.isNotEmpty(crmId)) {
			profile = ProfileQueryManagement.getByCrmId(crmId);
		}
		else if (StringUtil.isNotEmpty(appId)) {
			// KiotViet, Pancake, Facebook User ID, Google User ID,...
			profile = ProfileQueryManagement.getByApplicationID(appId);
		}
		else if (StringUtil.isNotEmpty(socialId)) {
			// "zalo": "123456789", "facebook": "123456789", "linkedin": "123456789"
			profile = ProfileQueryManagement.getBySocialMediaId(socialId);
		}
		return profile;
	}

}
