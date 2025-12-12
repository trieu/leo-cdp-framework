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
import rfx.core.util.HashUtil;
import rfx.core.util.StringUtil;

/**
 * Cleaner, consistent Event API handler for Mobile/Flutter/CDP integration.
 *
 * - Extracts JSON field names and URL parameter keys into constants for reuse.
 * - Adds comments and small clarifications to improve maintainability.
 */
public class EventApiHandler extends BaseApiHandler {
	
	// API endpoints handled by this class
	static final String API_EVENT_LIST = "/api/event/list";
	static final String API_EVENT_SAVE = "/api/event/save";


	// --------------------------
	// HTTP handlers
	// --------------------------
	@Override
	protected JsonDataPayload handlePost(EventObserver observer, HttpServerRequest req, String uri,
			JsonObject jsonData) {

		try {
			switch (uri) {
			case API_EVENT_SAVE:
				// Normalise metric, require it
				String metric = jsonData.getString(FIELD_METRIC, "").trim().toLowerCase();

				if (metric.isEmpty()) {
					return JsonDataPayload.fail("Missing event 'metric'");
				}

				String eventId = saveEventHandler(observer, req, metric, jsonData);

				return JsonDataPayload.ok(uri, eventId);

			default:
				return notFoundHttpHandler(uri);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return JsonDataPayload.fail(e.getMessage());
		}
	}

	@Override
	protected JsonDataPayload handleGet(EventObserver observer, HttpServerRequest req, String uri, MultiMap urlParams) {

		if (uri.equals(API_EVENT_LIST)) {
			return listEventStreamOfProfile(uri, urlParams);
		}
		return notFoundHttpHandler(uri);
	}

	/**
	 * List tracking events for a profile identified by identity keys passed as
	 * request parameters. Uses strict identity resolution priority.
	 */
	private JsonDataPayload listEventStreamOfProfile(String uri, MultiMap params) {

		Profile profile = null;

		// read the identity params from the request (query params)
		String email = params.get(FIELD_PRIMARY_EMAIL);
		String phone = params.get(FIELD_PRIMARY_PHONE);
		String crmId = params.get(FIELD_CRM_REF_ID);
		String appId = params.get(FIELD_APPLICATION_IDS);
		String govId = params.get(FIELD_GOVERNMENT_ISSUED_IDS);
		String socialId = params.get(FIELD_SOCIAL_MEDIA_IDS);

		// Identity Lookup Hierarchy (strongest -> weakest)
		if (StringUtil.isNotEmpty(govId)) {
			profile = ProfileQueryManagement.getByGovernmentIssuedID(govId);
		} else if (StringUtil.isNotEmpty(phone)) {
			profile = ProfileQueryManagement.getByPrimaryPhone(phone);
		} else if (StringUtil.isNotEmpty(email)) {
			profile = ProfileQueryManagement.getByPrimaryEmail(email);
		} else if (StringUtil.isNotEmpty(crmId)) {
			profile = ProfileQueryManagement.getByCrmId(crmId);
		} else if (StringUtil.isNotEmpty(appId)) {
			profile = ProfileQueryManagement.getByApplicationID(appId);
		} else if (StringUtil.isNotEmpty(socialId)) {
			profile = ProfileQueryManagement.getBySocialMediaId(socialId);
		}

		if (profile == null) {
			return JsonDataPayload.fail("No profile found for params: " + params);
		}

		// pagination params with sensible defaults
		int startIndex = HttpWebParamUtil.getInteger(params, PARAM_START_INDEX, 0);
		int limit = HttpWebParamUtil.getInteger(params, PARAM_NUMBER_RESULT, 20);

		// fetch events for profile
		List<TrackingEvent> events = EventDataManagement.getTrackingEventsOfProfile(profile.getId(), "", startIndex,
				limit);

		return JsonDataPayload.ok(uri, events);
	}

	/**
	 * Save incoming event JSON payload into the system:
	 * - parse identity and device/touchpoint info
	 * - resolve or create profile
	 * - build transaction if event is purchase/payment
	 * - delegate to EventObserverManagement to persist
	 *
	 * All JSON field names pulled from constants at top of class.
	 *
	 * @param observer current EventObserver handling the request
	 * @param req      HTTP request (used for fallback IP/user-agent retrieval)
	 * @param eventName normalized event name (metric)
	 * @param json     payload from client
	 * @return id of saved event (string) or error marker as produced by downstream
	 */
	protected static String saveEventHandler(EventObserver observer, HttpServerRequest req, String eventName,
			JsonObject json) {

		LogUtil.logInfo(EventApiHandler.class, "[EVENT] " + new Date() + " payload=" + json);

		// environment defaults to the configured PRO_ENV constant when not provided
		String environment = json.getString(FIELD_ENVIRONMENT, HttpParamKey.PRO_ENV);

		// --- Extract Observer Info (source of event) ---
		String observerId = observer.getId();
		String journeyMapId = observer.getJourneyMapId();

		// --- Parse Customer Identity ---
		String firstName = json.getString(FIELD_FIRST_NAME, "");
		String lastName = json.getString(FIELD_LAST_NAME, "");
		String fullName = json.getString(FIELD_FULL_NAME, "");

		// if only 'name' provided, promote to firstName for profile creation
		if (firstName.isBlank() && lastName.isBlank() && fullName != null) {
			firstName = fullName;
		}

		String email = json.getString(FIELD_PRIMARY_EMAIL, "");
		String phone = json.getString(FIELD_PRIMARY_PHONE, "");
		String crmId = json.getString(FIELD_CRM_REF_ID, "");
		String appId = json.getString(FIELD_APPLICATION_IDS, "");
		String socialId = json.getString(FIELD_SOCIAL_MEDIA_IDS, "");
		String govId = json.getString(FIELD_GOVERNMENT_ISSUED_IDS, "");

		// --- Device Info (with fallbacks to HTTP request) ---
		String sourceIP = json.getString(FIELD_SOURCE_IP, HttpWebParamUtil.getRemoteIP(req));
		String userAgent = json.getString(FIELD_USER_AGENT, req.headers().get("User-Agent"));
		Device userDevice = new Device(userAgent);
		String userDeviceUUID = json.getString(FIELD_USER_DEVICE_UUID,"");
		String fingerprintId = HashUtil.sha256(userDeviceUUID + observerId + userAgent + environment +journeyMapId);

		// --- Touchpoint Info and creation/retrieval of Touchpoint entity ---
		int tpType = json.getInteger(FIELD_TOUCHPOINT_TYPE, TouchpointType.DATA_OBSERVER);

		// sanitize incoming touchpoint name / url via XSS filter util; fallback to observer defaults
		String tpName = XssFilterUtil.safeGet(json, FIELD_TOUCHPOINT_NAME, observer.getName());
		String tpUrl = XssFilterUtil.safeGet(json, FIELD_TOUCHPOINT_URL, observer.getDataSourceUrl());

		Touchpoint sourceTp = TouchpointManagement.getOrCreateNew(tpName, tpType, tpUrl);

		String refUrl = XssFilterUtil.safeGet(json, FIELD_TOUCHPOINT_REFERER_URL);
		String refDomain = UrlUtil.getHostName(refUrl);

		// --- Event Time parsing: if provided, parse it; otherwise use now ---
		String eventTimeStr = json.getString(FIELD_EVENT_TIME);
		Date createdAt;

		if (StringUtil.isNotEmpty(eventTimeStr)) {
			createdAt = ProfileModelUtil.parseDate(eventTimeStr);
		} else {
			createdAt = new Date();
		}

		// --- Profile Lookup or Create using identity hierarchy ---
		Profile profile = queryProfileByKeys(email, phone, crmId, appId, socialId, govId, fingerprintId);

		if (profile != null) {
			profile.setFirstName(firstName);
			profile.setLastName(lastName);
			profile.setPhone(phone);
			profile.setEmail(email);
			profile.setCrmRefId(crmId);
			profile.setSocialMediaId(socialId);
			profile.setGovernmentIssuedID(govId);
			profile.setLastSeenIp(sourceIP);
			profile.setLastTouchpoint(sourceTp);
			profile.setFingerprintId(fingerprintId);
		}
		else {
			// create a minimal profile when none found
			profile = ProfileDataManagement.createNewProfileAndSave(firstName, lastName, createdAt, observerId,
					sourceTp, sourceIP, govId, phone, email, socialId, appId, crmId);
			
		}

		// --- Extra Event Data fields ---
		int rating = json.getInteger(FIELD_RATING_SCORE, -1);
		String message = json.getString(FIELD_MESSAGE, "");
		String rawJson = json.getString(FIELD_RAW_JSON_DATA, "");
		String imageUrls = json.getString(FIELD_IMAGE_URLS, "");
		String videoUrls = json.getString(FIELD_VIDEO_URLS, "");

		// event-specific key/value map (nested object)
		Map<String, Object> eventData = HttpWebParamUtil.getMapFromRequestParams(json, FIELD_EVENT_DATA);
		

		// --- Transaction construction (if this is a purchase/payment event) ---
		boolean computeTotal = BehavioralEvent.Commerce.PURCHASE.equalsIgnoreCase(eventName)
				|| BehavioralEvent.Commerce.MADE_PAYMENT.equalsIgnoreCase(eventName);

		OrderTransaction txn = new OrderTransaction(createdAt, json, computeTotal);

		// --- Persist the event via the observer management layer ---
		return EventObserverManagement.saveEventFromApi(profile, observerId, fingerprintId, createdAt, journeyMapId, environment,
				sourceIP, userDevice, tpType, tpName, tpUrl, refUrl, refDomain, eventName, message, eventData, rawJson,
				txn, rating, imageUrls, videoUrls);
	}

	/**
	 * Resolve a Profile using identity keys in strict priority order.
	 *
	 * Identity resolution hierarchy:
	 *   1. Government-issued ID   (strongest, unique)
	 *   2. Phone number
	 *   3. Email address
	 *   4. CRM reference ID
	 *   5. Application ID         (KiotViet, Pancake, Google, FB...)
	 *   6. Social media ID        (zalo, facebook, linkedIn...)
	 *   7. Fingerprint ID      = sha256(userDeviceUUID + observerId + userAgent + environment +journeyMapId)
	 *
	 * The method returns as soon as one match is found.
	 * If no identity matches, returns null.
	 */
	private static Profile queryProfileByKeys(String email,
	                                          String phone,
	                                          String crmId,
	                                          String appId,
	                                          String socialId,
	                                          String govId,
	                                          String fingerprintId
	                                          ) {

		
		
	    // 1. Government ID
	    if (StringUtil.isNotEmpty(govId)) {
	        return ProfileQueryManagement.getByGovernmentIssuedID(govId);
	    }

	    // 2. Phone
	    if (StringUtil.isNotEmpty(phone)) {
	        return ProfileQueryManagement.getByPrimaryPhone(phone);
	    }

	    // 3. Email
	    if (StringUtil.isNotEmpty(email)) {
	        return ProfileQueryManagement.getByPrimaryEmail(email);
	    }

	    // 4. CRM ID
	    if (StringUtil.isNotEmpty(crmId)) {
	        return ProfileQueryManagement.getByCrmId(crmId);
	    }

	    // 5. Application ID
	    if (StringUtil.isNotEmpty(appId)) {
	        return ProfileQueryManagement.getByApplicationID(appId);
	    }

	    // 6. Social Media ID
	    if (StringUtil.isNotEmpty(socialId)) {
	        return ProfileQueryManagement.getBySocialMediaId(socialId);
	    }
	    
	    // 7. fingerprintId (only for mobile app)
	    if (StringUtil.isNotEmpty(fingerprintId)) {
	        return ProfileQueryManagement.getByFingerprintId(fingerprintId);
	    }

	    // No identities found
	    return null;
	}

}
