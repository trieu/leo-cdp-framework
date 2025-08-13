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

public abstract class SecuredApiHandler extends BaseHttpHandler {

	static final class ApiProxyHttpGetRequest {

		String provider;
		String uri;

		public ApiProxyHttpGetRequest(String provider, String uri) {
			super();

			this.provider = provider;
			this.uri = uri;
		}

		public String getProvider() {
			return provider;
		}

		public void setProvider(String provider) {
			this.provider = provider;
		}

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}

		@Override
		public String toString() {
			return StringUtil.join(":", provider, uri);
		}

	}

	public static final int CACHE_TIME_OUT = 30;// seconds
	static final LoadingCache<ApiProxyHttpGetRequest, String> queriedCache = CacheBuilder.newBuilder()
			.maximumSize(9999999).expireAfterWrite(CACHE_TIME_OUT, TimeUnit.SECONDS)
			.build(new CacheLoader<ApiProxyHttpGetRequest, String>() {
				@Override
				public String load(ApiProxyHttpGetRequest q) {
					String url = "http://" + q.getProvider() + q.getUri();
					String json =  HttpClientGetUtil.call(url).trim();
					if ("null".equals(json)) {
						return "";
					}
					return json;
				}
			});

	public static String process(RoutingContext context, HttpServerRequest req) {
		MultiMap params = req.params();
		String json = "{}";
		String provider = StringUtil.safeString(params.get("provider"));
		String uri = StringUtil.safeString(params.get("uri"));
		MultiMap reqHeaders = req.headers();
		String headerSession = StringUtil.safeString(reqHeaders.get(BaseWebRouter.HEADER_SESSION));
		String userSession = CookieUserSessionUtil.getUserSession(context,headerSession);
		
		SystemUser loginUser = SecuredHttpDataHandler.initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			try {
				String rs = queriedCache.get(new ApiProxyHttpGetRequest(provider, uri));
				json = JsonDataPayload.ok(req.path(), rs, true).toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			json = JsonErrorPayload.NO_AUTHENTICATION.toString();
		}

		return json;
	}
	
	/**
	 * to check API token
	 * 
	 * @param req
	 * @return
	 */
	public final static EventObserver checkApiTokenAndGetEventObserver(HttpServerRequest req) {
		MultiMap headers = req.headers();
		String tokenKey = StringUtil.safeString(headers.get(HttpParamKey.ACCESS_TOKEN_KEY),"_");
		String tokenValue = StringUtil.safeString(headers.get(HttpParamKey.ACCESS_TOKEN_VALUE),"_");

		EventObserver observer;
		if(EventObserver.DEFAULT_ACCESS_KEY.equals(tokenKey)) {
			observer = EventObserverDaoUtil.getDefaultDataObserver();
		}
		else {
			observer = EventObserverManagement.getById(tokenKey);
		}
		// every observer has an access token
		if(observer != null) {
			String validToken = observer.getAccessTokens().getOrDefault(tokenKey, "");
			if(validToken.equals(tokenValue)) {
				return observer;
			}
		}
		return null;
	}
	
	//
	
	abstract public JsonDataPayload handle(HttpServerRequest req, String uri, JsonObject paramJson);
	
	abstract public JsonDataPayload handle(HttpServerRequest req, String uri, MultiMap urlParams);
}
