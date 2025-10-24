package leotech.system.common;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import java.util.Set;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.domain.DeviceManagement;
import leotech.system.model.JsonDataPayload;
import leotech.system.util.CookieUUIDUtil;
import leotech.system.util.CookieUserSessionUtil;
import leotech.system.util.HttpTrackingUtil;
import rfx.core.util.StringUtil;

/**
 * Base Web Router for  CDP
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public abstract class BaseWebRouter extends BaseHttpRouter {

	private static final int SYSTEM_ERROR_CODE = 500;

	public static final String P_USER_AGENT = "__userAgent";

	public static final String P_USER_IP = "__userIp";

	private static final String P_USER_SESSION = "usersession";
	
	public static final String SYSTEM_USER_PREFIX = "/user";
	
	////////////
	public static final String START_DATE = "/start-date";
	public static final String REQ_INFO = "/req-info";
	public static final String GEOLOCATION = "/geolocation";

	public static final String SEARCH_PREFIX = "/search";
	public static final String QUERY_PREFIX = "/query";
	
	public static final String SYSTEM_PREFIX = "/system";

	private JsonDataPayload defaultDataHttpGet = JsonDataPayload.fail("No HTTP GET handler found", 404);
	private JsonDataPayload defaultDataHttpPost = JsonDataPayload.fail("No HTTP POST handler found", 404);

	///////////
	public BaseWebRouter(RoutingContext context) {
		super(context);
	}

	// for implemented POST method
	abstract protected JsonDataPayload callHttpPostHandler(HttpServerRequest request, String userSession, String uri, JsonObject paramJson);

	// for implemented GET method
	abstract protected JsonDataPayload callHttpGetHandler(HttpServerRequest request, String userSession, String uri, MultiMap params);
	
	
	public void enableAutoRedirectToHomeIf404() {
		defaultDataHttpGet = JsonDataPayload.ok("/", "");
		defaultDataHttpGet.setHttpCode(301);
	}

	protected boolean handle(RoutingContext context) {

		HttpServerRequest request = context.request();
		HttpServerResponse resp = context.response();
		// ---------------------------------------------------------------------------------------------------
		MultiMap outHeaders = resp.headers();
		outHeaders.set(CONNECTION, HttpTrackingUtil.HEADER_CONNECTION_CLOSE);
		outHeaders.set(POWERED_BY, SERVER_VERSION);
		outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);

		MultiMap reqHeaders = request.headers();
		String origin = StringUtil.safeString(reqHeaders.get(BaseHttpHandler.ORIGIN), "*");
		String contentType = StringUtil.safeString(reqHeaders.get(BaseHttpHandler.CONTENT_TYPE),BaseHttpHandler.CONTENT_TYPE_JSON);

		// CORS Header
		BaseHttpRouter.setCorsHeaders(outHeaders, origin);

		String httpMethod = request.rawMethod();
		String uri = request.path();
		String host = request.host();
		String userAgent = DeviceManagement.getUserAgent(request);
		String userIp = CookieUUIDUtil.getRemoteIP(request);
		String userSessionHeader = StringUtil.safeString(reqHeaders.get(HEADER_SESSION));
		logger.info("HTTP "+httpMethod + " ==>> host: " + host + " uri: " + uri);

		if (HTTP_METHOD_POST.equalsIgnoreCase(httpMethod)) {
			String bodyStr = StringUtil.safeString(context.getBodyAsString(), "{}");
			JsonObject paramJson = buildHttpPostParams(request, contentType, userAgent, userIp, bodyStr);
			String userSession =  StringUtil.safeString(paramJson.remove(P_USER_SESSION), userSessionHeader);
			JsonDataPayload out = callHttpPostHandler(request, userSession, uri, paramJson);
			if (out != null) {
				if(out.getHttpCode() > 300) {
					resp.setStatusCode(out.getHttpCode());
				}
				resp.end(out.toString());
				return true;
			} else {
				resp.setStatusCode(SYSTEM_ERROR_CODE);
				defaultDataHttpPost.setUri(uri);
				resp.end(defaultDataHttpPost.toString());
				return false;
			}

		} else if (HTTP_METHOD_GET.equalsIgnoreCase(httpMethod)) {
			String userSession = CookieUserSessionUtil.getUserSession(context, userSessionHeader );
			MultiMap params = buildHttpGetParams(request, userAgent, userIp);
			JsonDataPayload out = callHttpGetHandler(request, userSession, uri, params);
			if (out != null) {
				resp.end(out.toString());
				return true;
			} else {
				if (defaultDataHttpGet.getHttpCode() == 301) {
					resp.putHeader("Location", defaultDataHttpGet.getUri());
					resp.setStatusCode(defaultDataHttpGet.getHttpCode());
					resp.end();
				} else {
					defaultDataHttpGet.setUri(uri);
					resp.end(defaultDataHttpGet.toString());
				}
				return false;
			}
		} else if (HTTP_GET_OPTIONS.equals(httpMethod)) {
			resp.end("");
			return true;
		} else {
			resp.end(JsonDataPayload.fail("Not HTTP handler found for uri:" + uri, 404).toString());
			return false;
		}
	}

	MultiMap buildHttpGetParams(HttpServerRequest request, String userAgent, String userIp) {
		MultiMap params = request.params();
		params.add(P_USER_IP, userIp);
		params.add(P_USER_AGENT, userAgent);
		return params;
	}

	JsonObject buildHttpPostParams(HttpServerRequest request, String contentType, String userAgent, String userIp, String bodyStr) {
		JsonObject paramJson = null;
		if (isJsonContentType(contentType)) {
			try {
				paramJson = new JsonObject(bodyStr);
			} catch (Exception e) {
				System.err.println(" buildHttpPostParams \n " + bodyStr);
				e.printStackTrace();
				paramJson = new JsonObject();
			}
		} else {
			MultiMap formAttributes = request.formAttributes();
			Set<String> fieldNames = formAttributes.names();
			paramJson = new JsonObject();
			for (String fieldName : fieldNames) {
				paramJson.put(fieldName, StringUtil.safeString(formAttributes.get(fieldName)));
			}
		}
		
		paramJson.put(P_USER_IP, userIp);
		paramJson.put(P_USER_AGENT, userAgent);
		return paramJson;
	}

	boolean isJsonContentType(String contentType) {
		return contentType.toLowerCase().contains(BaseHttpHandler.CONTENT_TYPE_JSON);
	}
}
