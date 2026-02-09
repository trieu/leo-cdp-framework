package leotech.cdp.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static leotech.starter.router.ObserverHttpRouter.FAILED;
import static leotech.starter.router.ObserverHttpRouter.OK;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_CONTENT;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_CONTEXT_SESSION_PROFILE_INIT;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_ACTION;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_VIEW;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_FEEDBACK_SCORE;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_HTML_RECOMMENDER_CONTENTS;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_HTML_RECOMMENDER_PRODUCTS;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_JSON_RECOMMENDER_CONTENTS;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_JSON_RECOMMENDER_PRODUCTS;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_PRESENTATION;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_TARGET_MEDIA_CLICK_TRACKING;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_TARGET_MEDIA_QR_CODE_TRACKING;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_TOUCHPOINT;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_WEBFORM;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_WEB_TEMPLATE_HTML;
import static leotech.system.common.BaseHttpRouter.PONG;
import static leotech.system.common.BaseHttpRouter.URI_ERROR_404;
import static leotech.system.common.BaseHttpRouter.URI_FAVICON_ICO;
import static leotech.system.common.BaseHttpRouter.URI_GEOIP;
import static leotech.system.common.BaseHttpRouter.URI_PING;
import static leotech.system.common.BaseHttpRouter.URI_SYSINFO;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.domain.ContextSessionManagement;
import leotech.cdp.domain.FeedbackDataManagement;
import leotech.cdp.domain.ProfileGraphManagement;
import leotech.cdp.model.analytics.ContextSession;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.common.BaseHttpHandler;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.PublicFileHttpRouter;
import leotech.system.domain.SystemSnapshot;
import leotech.system.model.DeviceInfo;
import leotech.system.model.GeoLocation;
import leotech.system.util.GeoLocationUtil;
import leotech.system.util.HttpWebParamUtil;
import leotech.web.model.WebData;
import rfx.core.util.StringUtil;

/**
 * Handles HTTP GET requests for the CDP Observer. * @author @tantrieuf31
 */
public final class ObserverHttpGetHandler {

	private static final String GOOGLE_COM_SEARCH_URL = "https://google.com/search?q=";

	// Optimize performance: Create Gson instance once
	private static final Gson GSON = new Gson();
	private static final Gson GSON_EXPOSE = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	public static void process(RoutingContext context, HttpServerRequest req, String urlPath, MultiMap reqHeaders,
			MultiMap params, HttpServerResponse resp, MultiMap outHeaders, DeviceInfo device, String origin,
			String nodeInfo) {

		// 1. Session & Event Tracking
		if (urlPath.equalsIgnoreCase(PREFIX_CONTEXT_SESSION_PROFILE_INIT)) {
			handleSessionInit(req, params, resp, outHeaders, device, origin);
		} else if (isEventTrackingRequest(params)) {
			handleEventTracking(req, urlPath, params, resp, outHeaders, device, origin);
		}

		// 2. Recommendations & Products
		else if (urlPath.startsWith(PREFIX_JSON_RECOMMENDER_PRODUCTS)) {
			handleJsonRecommender(params, resp, outHeaders, origin, true);
		} else if (urlPath.startsWith(PREFIX_JSON_RECOMMENDER_CONTENTS)) {
			handleJsonRecommender(params, resp, outHeaders, origin, false);
		} else if (urlPath.startsWith(PREFIX_HTML_RECOMMENDER_PRODUCTS)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
			ObserverWedHandler.processHtmlRecommender(req, urlPath, reqHeaders, params, resp, true);
		} else if (urlPath.startsWith(PREFIX_HTML_RECOMMENDER_CONTENTS)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
			ObserverWedHandler.processHtmlRecommender(req, urlPath, reqHeaders, params, resp, false);
		}

		// 3. Content Presentation & Web Forms
		else if (urlPath.startsWith(PREFIX_WEBFORM)) {
			handleWebForm(reqHeaders, params, resp, outHeaders);
		} else if (urlPath.startsWith(PREFIX_CONTENT)) {
			handleContentPresentation(urlPath, PREFIX_CONTENT, reqHeaders, params, resp, outHeaders);
		} else if (urlPath.startsWith(PREFIX_PRESENTATION)) {
			handleContentPresentation(urlPath, PREFIX_PRESENTATION, reqHeaders, params, resp, outHeaders);
		} else if (urlPath.startsWith(PREFIX_WEB_TEMPLATE_HTML)) {
			handleWebTemplate(reqHeaders, params, resp, outHeaders);
		}

		// 4. Feedback & Touchpoints
		else if (urlPath.startsWith(PREFIX_FEEDBACK_SCORE)) {
			handleFeedbackScore(params, resp, outHeaders, origin);
		} else if (urlPath.startsWith(PREFIX_TOUCHPOINT)) {
			handleTouchpointRedirect(params, resp);
		}

		// 5. Tracking Links (QR, Short Links)
		else if (urlPath.startsWith(PREFIX_TARGET_MEDIA_CLICK_TRACKING)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
			ObserverWedHandler.processShortLinkClick(req, urlPath, reqHeaders, params, resp, device);
		} else if (urlPath.startsWith(PREFIX_TARGET_MEDIA_QR_CODE_TRACKING)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
			ObserverWedHandler.processQrCodeScan(req, urlPath, reqHeaders, params, resp, device);
		}

		// 6. System & Utilities
		else if (urlPath.startsWith(PublicFileHttpRouter.PUBLIC_FILE_ROUTER)) {
			PublicFileHttpRouter.handle(resp, outHeaders, urlPath, params);
		} else if (urlPath.equalsIgnoreCase(URI_GEOIP)) {
			GeoLocation loc = GeoLocationUtil.processGeoLocation(req, resp);
			resp.end(loc.toString());
		} else if (urlPath.equalsIgnoreCase(URI_PING)) {
			sendHtml(resp, outHeaders, PONG);
		} else if (urlPath.equalsIgnoreCase(URI_SYSINFO)) {
			SystemSnapshot snapshot = new SystemSnapshot(nodeInfo);
			resp.end(snapshot.toString());
		} else if (urlPath.equalsIgnoreCase(URI_FAVICON_ICO)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_IMAGE_ICON);
			resp.end();
		} else if (urlPath.equalsIgnoreCase(URI_ERROR_404)) {
			resp.setStatusCode(404);
			sendText(resp, outHeaders, "404 - Not found error");
		}

		// 7. Fallback
		else {
			resp.end("CDP OBSERVER: " + nodeInfo);
		}
	}

	// =================================================================================
	// Logic Handlers
	// =================================================================================

	private static void handleSessionInit(HttpServerRequest req, MultiMap params, HttpServerResponse resp,
			MultiMap outHeaders, DeviceInfo device, String origin) {

		enableCors(outHeaders, origin);

		ContextSession session = ContextSessionManagement.checkAndCreate(req, params, device);
		if (session != null) {
			ObserverResponse rs = new ObserverResponse(session.getVisitorId(), session.getSessionKey(), OK, 101);
			sendJson(resp, outHeaders, GSON.toJson(rs));
		} else {
			sendJson(resp, outHeaders, GSON.toJson(new ObserverResponse("", "", FAILED, -101)));
		}
	}

	private static boolean isEventTrackingRequest(MultiMap params) {
		String eventName = params.get(HttpParamKey.EVENT_METRIC_NAME);
		String ctxSessionKey = params.get(HttpParamKey.CTX_SESSION_KEY);
		return StringUtil.isNotEmpty(eventName) && StringUtil.isNotEmpty(ctxSessionKey);
	}

	private static void handleEventTracking(HttpServerRequest req, String urlPath, MultiMap params,
			HttpServerResponse resp, MultiMap outHeaders, DeviceInfo device, String origin) {

		enableCors(outHeaders, origin);
		String eventName = StringUtil.safeString(params.get(HttpParamKey.EVENT_METRIC_NAME)).toLowerCase();
		String ctxSessionKey = StringUtil.safeString(params.get(HttpParamKey.CTX_SESSION_KEY));
		String ip = HttpWebParamUtil.getRemoteIP(req);

		ContextSession ctxSession = ContextSessionManagement.get(ctxSessionKey, ip, params, device);

		int status = 404;
		String visitorId = "";
		String sessionKey = "";

		if (ctxSession != null) {
			visitorId = ctxSession.getVisitorId();
			sessionKey = ctxSession.getSessionKey();

			if (urlPath.equalsIgnoreCase(PREFIX_EVENT_VIEW) || urlPath.equalsIgnoreCase(PREFIX_EVENT_ACTION)) {
				String eventId = EventObserverUtil.recordBehavioralEvent(req, params, device, ctxSession, eventName);
				if (StringUtil.isNotEmpty(eventId)) {
					status = 200;
				}
			}
		}
		ObserverResponse.done(resp, status, visitorId, sessionKey, 1);
	}

	private static void handleJsonRecommender(MultiMap params, HttpServerResponse resp, MultiMap outHeaders,
			String origin, boolean isProduct) {
		enableCors(outHeaders, origin);
		resp.setStatusCode(200);

		String observerId = HttpWebParamUtil.getString(params, HttpParamKey.OBSERVER_ID, "");
		String visid = HttpWebParamUtil.getString(params, HttpParamKey.VISITOR_ID, "");
		String touchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_URL, "");
		int startIndex = HttpWebParamUtil.getInteger(params, HttpParamKey.START_INDEX, 0);
		int numberResult = HttpWebParamUtil.getInteger(params, HttpParamKey.NUMBER_RESULT, 10);

		List<TargetMediaUnit> items;
		if (isProduct) {
			items = ProfileGraphManagement.queryRecommendedProductItems(observerId, visid, touchpointUrl, startIndex,
					numberResult);
		} else {
			items = ProfileGraphManagement.queryRecommendedContents(observerId, visid, touchpointUrl, startIndex,
					numberResult);
		}

		sendJson(resp, outHeaders, GSON_EXPOSE.toJson(items));
	}

	private static void handleWebForm(MultiMap reqHeaders, MultiMap params, HttpServerResponse resp,
			MultiMap outHeaders) {
		WebData model = ObserverWebModel.buildWebFormDataModel(reqHeaders, params);
		renderWebData(resp, outHeaders, model);
	}

	private static void handleContentPresentation(String urlPath, String prefix, MultiMap reqHeaders, MultiMap params,
			HttpServerResponse resp, MultiMap outHeaders) {
		String slug = urlPath.replace(prefix, "");
		WebData model = ObserverWebModel.buildContentDataModel(reqHeaders, params, slug);
		if (model != null) {
			renderWebData(resp, outHeaders, model);
		} else {
			resp.setStatusCode(404).end("404");
		}
	}

	private static void handleWebTemplate(MultiMap reqHeaders, MultiMap params, HttpServerResponse resp,
			MultiMap outHeaders) {
		String html = ObserverWebModel.getTemplateHtml(reqHeaders, params);
		if (StringUtil.isNotEmpty(html)) {
			resp.setStatusCode(200);
			sendHtml(resp, outHeaders, html);
		} else {
			resp.setStatusCode(404).end("404");
		}
	}

	private static void handleFeedbackScore(MultiMap params, HttpServerResponse resp, MultiMap outHeaders,
			String origin) {
		enableCors(outHeaders, origin);

		String templateId = StringUtil.safeString(params.get(HttpParamKey.TEMPLATE_ID));
		String refVisitorId = StringUtil.safeString(params.get(HttpParamKey.VISITOR_ID));
		String srcTouchpointUrl = StringUtil.safeString(params.get(HttpParamKey.TOUCHPOINT_URL));

		int previousFeedbackScore = FeedbackDataManagement.loadLatestFeedbackScore(templateId, refVisitorId,
				srcTouchpointUrl);

		resp.setStatusCode(200);
		sendText(resp, outHeaders, String.valueOf(previousFeedbackScore));
	}

	private static void handleTouchpointRedirect(MultiMap params, HttpServerResponse resp) {
		String q = HttpWebParamUtil.getString(params, "q");
		resp.setStatusCode(302);
		resp.putHeader("Location", GOOGLE_COM_SEARCH_URL + q);
		resp.end();
	}

	// =================================================================================
	// Helper Methods
	// =================================================================================

	private static void enableCors(MultiMap headers, String origin) {
		BaseHttpRouter.setCorsHeaders(headers, origin);
	}

	private static void renderWebData(HttpServerResponse resp, MultiMap outHeaders, WebData model) {
		String html = WebData.renderHtml(model);
		resp.setStatusCode(model.getHttpStatusCode());
		sendHtml(resp, outHeaders, html);
	}

	private static void sendJson(HttpServerResponse resp, MultiMap headers, String jsonBody) {
		headers.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);
		resp.end(jsonBody);
	}

	private static void sendHtml(HttpServerResponse resp, MultiMap headers, String htmlBody) {
		headers.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
		resp.end(htmlBody);
	}

	private static void sendText(HttpServerResponse resp, MultiMap headers, String textBody) {
		headers.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_TEXT);
		resp.end(textBody);
	}
}