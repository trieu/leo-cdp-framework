package leotech.system.common;

import leotech.system.model.JsonDataPayload;

/**
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public abstract class BaseHttpHandler {

	public static final String ORIGIN = "Origin";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String USER_AGENT = "User-Agent";
	public static final String REFERER = "Referer";

	public static final String NO_DATA = "0";
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	public static final String CONTENT_TYPE_JSON = "application/json";
	public static final String CONTENT_TYPE_XML = "application/xml";
	public static final String CONTENT_TYPE_JAVASCRIPT = "text/javascript;charset=UTF-8";
	public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
	public static final String CONTENT_TYPE_TEXT = "text/plain";
	public static final String CONTENT_TYPE_HTML = "text/html;charset=UTF-8";
	public static final String CONTENT_TYPE_IMAGE_ICON = "image/x-icon";
	
	public final static class JsonErrorPayload {
		public static final JsonDataPayload INVALID_CAPCHA_NUMBER = JsonDataPayload.fail("Invalid captcha number", 203);
		public static final JsonDataPayload WRONG_USER_LOGIN = JsonDataPayload.fail("Invalid user login ID or password",202);
		
		public static final JsonDataPayload NO_HANDLER_FOUND = JsonDataPayload.fail("No handler found", 404);
		public static final JsonDataPayload NO_AUTHENTICATION = JsonDataPayload.fail("No authentication, you should login to access and view data", 501);
		
		public static final JsonDataPayload NO_AUTHORIZATION = JsonDataPayload.fail("No authorization, you can not access, view or update data", 504);
		public static final JsonDataPayload NO_AUTHORIZATION_TO_UPDATE = JsonDataPayload.fail("You have no authorization to update or create", 500);
		public static final JsonDataPayload UNKNOWN_EXCEPTION = JsonDataPayload.fail("Unknown Exception", 506);
		public static final JsonDataPayload INVALID_DATA_EXCEPTION = JsonDataPayload.fail("Invalid Data Exception", 507);
		public static final JsonDataPayload INVALID_SSO_USER_SESSION = JsonDataPayload.fail("Invalid data, can not create SSO User Session", 508);
		public static final JsonDataPayload ERROR = JsonDataPayload.fail("ERROR", 509);
	}
}
