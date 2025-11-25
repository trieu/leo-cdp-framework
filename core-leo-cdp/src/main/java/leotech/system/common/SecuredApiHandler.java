package leotech.system.common;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.dao.EventObserverDaoUtil;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.journey.EventObserver;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.system.util.CookieUserSessionUtil;
import leotech.system.util.HttpClientGetUtil;
import rfx.core.util.StringUtil;

/**
 * Secured API handler for internal authenticated and token-based endpoints.
 * <br>
 *
 * Improvements: <br>
 * - Added documentation and clear intent. <br>
 * - Extracted magic strings and clarified flows. <br>
 * - Reduced object creation and unnecessary conversions. <br>
 * - Centralized cache logic in a readable form. <br>
 */
public abstract class SecuredApiHandler extends BaseHttpHandler {

	/**
	 * Represents a unique GET-proxy request for caching. <br>
	 * Used to avoid refetching from provider repeatedly.
	 */
	static final class ApiProxyHttpGetRequest {

		final String provider;
		final String uri;

		public ApiProxyHttpGetRequest(String provider, String uri) {
			this.provider = provider;
			this.uri = uri;
		}

		public String getProvider() {
			return provider;
		}

		public String getUri() {
			return uri;
		}

		@Override
		public int hashCode() {
			// Avoid building large strings here — use tuple-style hash
			int h1 = provider != null ? provider.hashCode() : 0;
			int h2 = uri != null ? uri.hashCode() : 0;
			return 31 * h1 + h2;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof ApiProxyHttpGetRequest))
				return false;
			ApiProxyHttpGetRequest o = (ApiProxyHttpGetRequest) other;
			return provider.equals(o.provider) && uri.equals(o.uri);
		}

		@Override
		public String toString() {
			return provider + ":" + uri;
		}
	}

	// Cache web requests to reduce proxy traffic and latency
	// Uses LoadingCache so request execution logic stays clean.
	public static final int CACHE_TIME_OUT_SEC = 30;

	private static final LoadingCache<ApiProxyHttpGetRequest, String> PROXY_CACHE = CacheBuilder.newBuilder()
			.maximumSize(500_000) // reasonable value: avoids massive memory usage
			.expireAfterWrite(CACHE_TIME_OUT_SEC, TimeUnit.SECONDS)
			.build(new CacheLoader<ApiProxyHttpGetRequest, String>() {

				@Override
				public String load(ApiProxyHttpGetRequest key) {
					final String remoteUrl = "http://" + key.provider + key.uri;

					String json = HttpClientGetUtil.call(remoteUrl).trim();
					return "null".equals(json) ? "" : json;
				}
			});

	/**
	 * Process secured proxy GET requests. <br>
	 *
	 * Only authenticated SystemUser can access the proxied GET result.
	 */
	public static String process(RoutingContext context, HttpServerRequest req) {

		MultiMap params = req.params();

		String provider = StringUtil.safeString(params.get("provider"));
		String uri = StringUtil.safeString(params.get("uri"));

		// Retrieve session from cookie or header
		MultiMap headers = req.headers();
		String headerSession = StringUtil.safeString(headers.get(BaseWebRouter.HEADER_SESSION));
		String userSession = CookieUserSessionUtil.getUserSession(context, headerSession);

		// Validate if the user is authorized for this uri and request parameters
		SystemUser user = SecuredHttpDataHandler.initSystemUser(userSession, uri, params);

		if (user == null) {
			return JsonErrorPayload.NO_AUTHENTICATION.toString();
		}

		try {
			ApiProxyHttpGetRequest cacheKey = new ApiProxyHttpGetRequest(provider, uri);
			String cachedResult = PROXY_CACHE.get(cacheKey);

			return JsonDataPayload.ok(req.path(), cachedResult, true).toString();

		} catch (Exception e) {
			e.printStackTrace();
			return JsonErrorPayload.ERROR.setException(e).toString();
		}
	}

	/**
	 * Validate API access tokens for EventObserver-based callers. <br>
	 *
	 * Each observer has: <br>
	 * - key = tokenKey (observer ID) <br>
	 * - value = tokenValue (secret) <br>
	 *
	 * Returns: <br>
	 * - EventObserver if valid token <br>
	 * - null if invalid <br>
	 */
	public static EventObserver checkApiTokenAndGetEventObserver(HttpServerRequest req) {

		MultiMap headers = req.headers();

		// Read provided tokens (or fallback to placeholders)
		String tokenKey = StringUtil.safeString(headers.get(HttpParamKey.ACCESS_TOKEN_KEY), "_");
		String tokenValue = StringUtil.safeString(headers.get(HttpParamKey.ACCESS_TOKEN_VALUE), "_");

		EventObserver observer;

		// Default observer shortcut
		if (EventObserver.DEFAULT_ACCESS_KEY.equals(tokenKey)) {
			observer = EventObserverDaoUtil.getDefaultDataObserver();
		} else {
			observer = EventObserverManagement.getById(tokenKey);
		}

		if (observer == null) {
			return null;
		}

		// Validate token match
		String validToken = observer.getAccessTokens().getOrDefault(tokenKey, "");
		return validToken.equals(tokenValue) ? observer : null;
	}

	// Subclasses implement HTTP handlers for JSON body POST and URL-param GET
	public abstract JsonDataPayload handle(HttpServerRequest req, String uri, JsonObject paramJson);

	public abstract JsonDataPayload handle(HttpServerRequest req, String uri, MultiMap urlParams);
}
