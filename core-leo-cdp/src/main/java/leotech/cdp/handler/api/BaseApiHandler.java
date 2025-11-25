package leotech.cdp.handler.api;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import leotech.cdp.model.journey.EventObserver;
import leotech.system.common.SecuredApiHandler;
import leotech.system.model.JsonDataPayload;
import rfx.core.util.StringPool;

/**
 * Base Data API
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public abstract class BaseApiHandler {
	final static String DEBUG = "debug";
	final static JsonDataPayload INVALID_ACCESS_TOKEN_VALUE = JsonDataPayload.fail("ACCESS_TOKEN_VALUE to the CDP API is not valid !");
	final static JsonDataPayload NO_SUPPORT_HTTP_REQUEST = JsonDataPayload.fail("HTTP request is not support !");
	
	// Standard failure payload reused across methods
	public static final JsonDataPayload FAIL = JsonDataPayload.fail("You must set parameter primary_email OR primary_phone");



	// --------------------------
	// Reusable JSON / param keys
	// --------------------------
	// Generic / meta fields
	public static final String FIELD_METRIC = "metric";
	public static final String FIELD_ENVIRONMENT = "environment";

	// Identity fields
	public static final String FIELD_FIRST_NAME = "first_name";
	public static final String FIELD_LAST_NAME = "last_name";
	public static final String FIELD_FULL_NAME = "name";
	public static final String FIELD_PRIMARY_EMAIL = "primary_email";
	public static final String FIELD_PRIMARY_PHONE = "primary_phone";
	public static final String FIELD_CRM_REF_ID = "crm_ref_id";
	public static final String FIELD_APPLICATION_IDS = "application_ids";
	public static final String FIELD_SOCIAL_MEDIA_IDS = "social_media_ids";
	public static final String FIELD_GOVERNMENT_ISSUED_IDS = "government_issued_ids";

	// Device / request meta
	public static final String FIELD_SOURCE_IP = "source_ip";
	public static final String FIELD_USER_AGENT = "user_agent";
	public static final String FIELD_USER_DEVICE_UUID = "user_device_uuid";
	

	// Touchpoint
	public static final String FIELD_TOUCHPOINT_TYPE = "touchpoint_type";
	public static final String FIELD_TOUCHPOINT_NAME = "touchpoint_name";
	public static final String FIELD_TOUCHPOINT_URL = "touchpoint_url";
	public static final String FIELD_TOUCHPOINT_REFERER_URL = "touchpoint_referer_url";

	// Event details
	public static final String FIELD_EVENT_TIME = "event_time";
	public static final String FIELD_RATING_SCORE = "rating_score";
	public static final String FIELD_MESSAGE = "message";
	public static final String FIELD_RAW_JSON_DATA = "raw_json_data";
	public static final String FIELD_IMAGE_URLS = "image_urls";
	public static final String FIELD_VIDEO_URLS = "video_urls";
	public static final String FIELD_EVENT_DATA = "event_data";
	public static final String FIELD_PROFILE_DATA = "profile_data";

	// Query params for list endpoint
	public static final String PARAM_START_INDEX = "start_index";
	public static final String PARAM_NUMBER_RESULT = "number_result";
	public static final String SEGMENT_ID = "segment_id";
	
	public static JsonDataPayload notFoundHttpHandler(String uri) {
		return JsonDataPayload.fail(uri + " is not valid URI, not found any API handler!");
	}
	
	public static final boolean isDebugMode(HttpServerRequest req) {
		return StringPool.TRUE.equalsIgnoreCase(req.getParam(DEBUG));
	}
	
	public JsonDataPayload handlePost(HttpServerRequest req, String uri, JsonObject jsonObject) { 
		JsonDataPayload payload = null;
		EventObserver observer = SecuredApiHandler.checkApiTokenAndGetEventObserver(req);
		if (observer != null) {
			payload = handlePost(observer, req, uri, jsonObject);
		}
		else {
			payload = INVALID_ACCESS_TOKEN_VALUE;
		}
		return payload;
	}
	

	public JsonDataPayload handleGet(HttpServerRequest req, String uri, MultiMap urlParams) {
		JsonDataPayload payload = null;
		EventObserver observer = SecuredApiHandler.checkApiTokenAndGetEventObserver(req);
		if (observer != null) {
			payload = handleGet(observer, req, uri, urlParams);
		}
		else {
			payload = INVALID_ACCESS_TOKEN_VALUE;
		}
		return payload;
	}
	
	abstract protected JsonDataPayload handlePost(EventObserver observer, HttpServerRequest req, String uri, JsonObject jsonObject);	
	abstract protected JsonDataPayload handleGet(EventObserver observer, HttpServerRequest req, String uri, MultiMap urlParams);

}
