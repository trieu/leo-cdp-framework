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
import leotech.starter.router.ObserverHttpRouter;
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
 * 
 * @author @tantrieuf31
 */
public final class ObserverHttpGetHandler {

	private static final String GOOGLE_COM_SEARCH_URL = "https://google.com/search?q=";

	public final static void process(RoutingContext context, HttpServerRequest req, String urlPath, MultiMap reqHeaders,
			MultiMap params, HttpServerResponse resp, MultiMap outHeaders, DeviceInfo device, String origin, String nodeInfo) {
		String eventName = StringUtil.safeString(params.get(HttpParamKey.EVENT_METRIC_NAME)).toLowerCase();
		String ctxSessionKey = StringUtil.safeString(params.get(HttpParamKey.CTX_SESSION_KEY));

		// init web session
		if (urlPath.equalsIgnoreCase(PREFIX_CONTEXT_SESSION_PROFILE_INIT)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);

			// synchronize session (when) with user's device (how), touchpoint's context
			// (where) and profile (who)
			// into one object for analytics (understand why)

			ContextSession session = ContextSessionManagement.checkAndCreate(req, params, device);
			
			if (session != null) {
				ObserverResponse rs = new ObserverResponse(session.getVisitorId(), session.getSessionKey(), OK, 101);
				resp.end(new Gson().toJson(rs));
			} else {
				resp.end(new Gson().toJson(new ObserverResponse("", "", FAILED, -101)));
			}

		}
		// tracking event in real-time
		else if (StringUtil.isNotEmpty(eventName) && StringUtil.isNotEmpty(ctxSessionKey)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);

			// synch ContextSession with event tracking
			ContextSession ctxSession = ContextSessionManagement.get(ctxSessionKey, req, params, device);

			int status = 404;
			String eventId = "", visitorId = "", sessionKey = "";
			if (ctxSession != null) {
				visitorId = ctxSession.getVisitorId();
				sessionKey = ctxSession.getSessionKey();
				// event-view(pageview|screenview|storeview|trueview|placeview,contentId,sessionKey,visitorId)
				if (urlPath.equalsIgnoreCase(PREFIX_EVENT_VIEW)) {
					eventId = EventObserverUtil.recordViewEvent(req, params, device, ctxSession, eventName);
					if (StringUtil.isNotEmpty(eventId)) {
						status = 200;
					}
				}
				// event-action(click|play|touch|contact|watch|test,sessionKey,visitorId)
				else if (urlPath.equalsIgnoreCase(PREFIX_EVENT_ACTION)) {
					eventId = EventObserverUtil.recordActionEvent(req, params, device, ctxSession, eventName);
					if (StringUtil.isNotEmpty(eventId)) {
						status = 200;
					}
				}
			}

			ObserverResponse.done(resp, status, visitorId, sessionKey, eventId);
		}

		// TOUCHPOINT
		else if (urlPath.startsWith(ObserverHttpRouter.PREFIX_TOUCHPOINT)) {
			resp.setStatusCode(302);
			String q = HttpWebParamUtil.getString(params, "q");
			// Add the Location header to redirect to Google
			resp.putHeader("Location", GOOGLE_COM_SEARCH_URL + q);
			// End the response
			resp.end();
		}

		// SHORT LINK CLICK, redirect for O2O synchronization E.g:
		// https://demotrack.leocdp.net/ct/294HGH6ZYaYyOBqdTnjhG3
		else if (urlPath.startsWith(PREFIX_TARGET_MEDIA_CLICK_TRACKING)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
			ObserverWedHandler.processShortLinkClick(req, urlPath, reqHeaders, params, resp, device);
		}
		// feedback score
		else if (urlPath.startsWith(PREFIX_FEEDBACK_SCORE)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_TEXT);
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);

			String templateId = StringUtil.safeString(params.get(HttpParamKey.TEMPLATE_ID));
			String refVisitorId = StringUtil.safeString(params.get(HttpParamKey.VISITOR_ID));
			String srcTouchpointUrl = StringUtil.safeString(params.get(HttpParamKey.TOUCHPOINT_URL));
			int previousFeedbackScore = FeedbackDataManagement.loadLatestFeedbackScore(templateId, refVisitorId,
					srcTouchpointUrl);

			resp.setStatusCode(200);
			resp.end(String.valueOf(previousFeedbackScore));

		}
		// get and render the HTML Web Form for profile data collection
		else if (urlPath.startsWith(PREFIX_WEBFORM)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
			WebData model = ObserverWebModel.buildWebFormDataModel(reqHeaders, params);
			String html = WebData.renderHtml(model);
			resp.setStatusCode(model.getHttpStatusCode());
			resp.end(html);
		}
		// get and render the HTML Web Form for profile data collection
		else if (urlPath.startsWith(PREFIX_CONTENT)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);

			String slug = urlPath.replace(PREFIX_CONTENT, "");
			WebData model = ObserverWebModel.buildContentDataModel(reqHeaders, params, slug);
			if (model != null) {
				String html = WebData.renderHtml(model);
				resp.setStatusCode(model.getHttpStatusCode());
				resp.end(html);
			} else {
				resp.setStatusCode(404);
				resp.end("404");
			}
		}
		// get and render the HTML
		else if (urlPath.startsWith(PREFIX_PRESENTATION)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);

			String slug = urlPath.replace(PREFIX_PRESENTATION, "");
			WebData model = ObserverWebModel.buildContentDataModel(reqHeaders, params, slug);
			if (model != null) {
				String html = WebData.renderHtml(model);
				resp.setStatusCode(model.getHttpStatusCode());
				resp.end(html);
			} else {
				resp.setStatusCode(404);
				resp.end("404");
			}
		}
		// get and render the template as html
		else if (urlPath.startsWith(PREFIX_WEB_TEMPLATE_HTML)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
			String html = ObserverWebModel.getTemplateHtml(reqHeaders, params);
			if (StringUtil.isNotEmpty(html)) {
				resp.setStatusCode(200);
				resp.end(html);
			} else {
				resp.setStatusCode(404);
				resp.end("404");
			}
		}

		//
		else if (urlPath.startsWith(PREFIX_HTML_RECOMMENDER_PRODUCTS)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
			ObserverWedHandler.processHtmlRecommender(req, urlPath, reqHeaders, params, resp, true);
		}
		//
		else if (urlPath.startsWith(PREFIX_JSON_RECOMMENDER_PRODUCTS)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);
			resp.setStatusCode(200);

			String observerId = HttpWebParamUtil.getString(params, HttpParamKey.OBSERVER_ID, "");
			String visid = HttpWebParamUtil.getString(params, HttpParamKey.VISITOR_ID, "");
			String touchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_URL, "");

			int startIndex = HttpWebParamUtil.getInteger(params, HttpParamKey.START_INDEX, 0);
			int numberResult = HttpWebParamUtil.getInteger(params, HttpParamKey.NUMBER_RESULT, 10);

			List<TargetMediaUnit> items = ProfileGraphManagement.queryRecommendedProductItems(observerId, visid,
					touchpointUrl, startIndex, numberResult);
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			resp.end(gson.toJson(items));
		}
		//
		else if (urlPath.startsWith(PREFIX_HTML_RECOMMENDER_CONTENTS)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
			ObserverWedHandler.processHtmlRecommender(req, urlPath, reqHeaders, params, resp, false);
		}
		//
		else if (urlPath.startsWith(PREFIX_JSON_RECOMMENDER_CONTENTS)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);
			resp.setStatusCode(200);

			String observerId = HttpWebParamUtil.getString(params, HttpParamKey.OBSERVER_ID, "");
			String visid = HttpWebParamUtil.getString(params, HttpParamKey.VISITOR_ID, "");
			String touchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_URL, "");

			int startIndex = HttpWebParamUtil.getInteger(params, HttpParamKey.START_INDEX, 0);
			int numberResult = HttpWebParamUtil.getInteger(params, HttpParamKey.NUMBER_RESULT, 10);

			List<TargetMediaUnit> items = ProfileGraphManagement.queryRecommendedContents(observerId, visid,
					touchpointUrl, startIndex, numberResult);
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			resp.end(gson.toJson(items));
		}
		//
		else if (urlPath.startsWith(PREFIX_TARGET_MEDIA_QR_CODE_TRACKING)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_HTML);
			ObserverWedHandler.processQrCodeScan(req, urlPath, reqHeaders, params, resp, device);

		}
		// Files handler with CDN (raw HTML, Images, CSS, JS, JSON,...)
		else if (urlPath.startsWith(PublicFileHttpRouter.PUBLIC_FILE_ROUTER)) {
			PublicFileHttpRouter.handle(resp, outHeaders, urlPath, params);
		}
		//
		else if (urlPath.equalsIgnoreCase(URI_GEOIP)) {
			GeoLocation loc = GeoLocationUtil.processGeoLocation(req, resp);
			resp.end(loc.toString());
		}
		//
		else if (urlPath.equalsIgnoreCase(URI_PING)) {
			outHeaders.set(CONTENT_TYPE, "text/html");
			resp.end(PONG);
		}
		//
		else if (urlPath.equalsIgnoreCase(URI_SYSINFO)) {
			SystemSnapshot SystemSnapshot = new SystemSnapshot(nodeInfo);
			resp.end(SystemSnapshot.toString());
		}
		//
		else if (urlPath.equalsIgnoreCase(URI_FAVICON_ICO)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_IMAGE_ICON);
			resp.end();
		}
		//
		else if (urlPath.equalsIgnoreCase(URI_ERROR_404)) {
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_TEXT);
			resp.setStatusCode(404);
			resp.end("404 - Not found error");
		}
		// no handler found
		else {
			resp.end("CDP OBSERVER: " + nodeInfo);

		}
	}
}
