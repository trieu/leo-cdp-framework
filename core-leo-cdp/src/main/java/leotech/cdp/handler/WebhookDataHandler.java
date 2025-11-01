package leotech.cdp.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static leotech.system.common.BaseHttpRouter.DEFAULT_RESPONSE_TEXT;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.domain.TouchpointHubManagement;
import leotech.cdp.domain.WebhookDataManagement;
import leotech.cdp.model.analytics.WebhookDataEvent;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.system.common.BaseHttpHandler;
import leotech.system.common.BaseHttpRouter;
import leotech.system.model.DeviceInfo;
import leotech.system.util.HttpWebParamUtil;
import rfx.core.util.StringUtil;

/**
 * Webbook Data Handler
 * 
 * @author @tantrieuf31
 * @since 2025
 */
public final class WebhookDataHandler {

	public static final String PREFIX_WEBHOOK = "/webhook";
	public static final String THE_BODY_IN_HTTP_IS_EMPTY = "The body in HTTP is empty";
	public static final String OK = "ok";
	public static final String INVALID_ACCESS_TOKEN_VALUE = "INVALID_ACCESS_TOKEN_VALUE";
	public static final String DEFAULT_RESULT = "Observer Http Webhook Handler of " + DEFAULT_RESPONSE_TEXT;

	public final static boolean process(RoutingContext context, HttpServerRequest req, String urlPath,
			MultiMap reqHeaders, MultiMap params, HttpServerResponse resp, MultiMap outHeaders, DeviceInfo device,
			String origin) {

		// In LEO CDP, the ACCESS_TOKEN_KEY is observerId
		String observerId = HttpWebParamUtil.getString(params, HttpParamKey.ACCESS_TOKEN_KEY,
				EventObserver.DEFAULT_EVENT_OBSERVER_ID);
		EventObserver observer = EventObserverManagement.getById(observerId);

		// ZALO
		if (urlPath.startsWith(ZaloWebhookHandler.ZALO)) {
			return ZaloWebhookHandler.verify(urlPath, params, resp, outHeaders);
		} else if (urlPath.startsWith(PREFIX_WEBHOOK)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_TEXT);
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);

			String tokenValue = HttpWebParamUtil.getString(params, HttpParamKey.ACCESS_TOKEN_VALUE);
			boolean isValidToken = observer.getAccessTokens().getOrDefault(observerId, "").equals(tokenValue);
			if (isValidToken) {
				String payload = StringUtil.safeString(context.getBodyAsString());
				String source = HttpWebParamUtil.getString(params, HttpParamKey.SOURCE);
				boolean ok = processWebhookEvent(observerId, source, payload);
				if (ok) {
					resp.end(OK);
					return true;
				} else {
					resp.end(THE_BODY_IN_HTTP_IS_EMPTY);
				}
			} else {
				resp.end(INVALID_ACCESS_TOKEN_VALUE);
			}
		}

		return false;
	}

	/**
	 * @param observerId
	 * @param source
	 * @param payload
	 * @return
	 */
	static boolean processWebhookEvent(String observerId, String source, String payload) {
		if (StringUtil.isNotEmpty(payload)) {
			TouchpointHub tphub = TouchpointHubManagement.getByObserverId(observerId);
			if (tphub != null) {
				WebhookDataEvent e = new WebhookDataEvent(source, observerId, tphub.getId(), payload);
				WebhookDataManagement.process(source, e);
				return true;
			}
			return false;
		}
		// The payload in HTTP is empty
		else {
			return false;
		}
	}

}
