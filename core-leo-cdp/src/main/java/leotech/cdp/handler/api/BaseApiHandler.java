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
	
	public static JsonDataPayload notFoundHttpHandler(String uri) {
		return JsonDataPayload.fail(uri + " is not valid URI, not found any API handler!");
	}
	
	public static final boolean isDebugMode(HttpServerRequest req) {
		return StringPool.TRUE.equalsIgnoreCase(req.getParam(DEBUG));
	}
	
	public void handlePost(HttpServerRequest req, String uri, JsonObject jsonObject) { 
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
	

	public void handleGet(HttpServerRequest req, String uri, MultiMap urlParams) {
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
