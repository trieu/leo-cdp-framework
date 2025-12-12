package leotech.cdp.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static leotech.starter.router.ObserverHttpRouter.INVALID;
import static leotech.starter.router.ObserverHttpRouter.OK;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_CONTEXT_SESSION_PROFILE_UPDATE;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_ACTION;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_CONVERSION;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_FEEDBACK;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.domain.ContextSessionManagement;
import leotech.cdp.model.analytics.ContextSession;
import leotech.cdp.model.analytics.EventPayload;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.system.common.BaseHttpHandler;
import leotech.system.common.BaseHttpRouter;
import leotech.system.model.DeviceInfo;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.TaskRunner;
import rfx.core.util.StringUtil;

/**
 * HTTP POST Event Data handler
 * 
 * @author tantrieuf31
 */
public final class ObserverHttpPostHandler {

	// Thread-safe singleton for Gson
	private static final Gson gson = new Gson();

	// Constants
	private static final String PARAM_EVENTS = "events";
	private static final String PARAM_EVENT_COUNT = "eventcount";
	private static final int DEFAULT_RESPONSE = 1;

	private static final Type EVENT_LIST_TYPE = new TypeToken<ArrayList<EventPayload>>() {
	}.getType();

	/**
	 * Main Processing Entry Point
	 */
	public static void process(RoutingContext context, HttpServerRequest req, String urlPath, MultiMap reqHeaders,
			MultiMap params, HttpServerResponse resp, MultiMap outHeaders, DeviceInfo device, String origin,
			String serverInfo) {

		outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);
		BaseHttpRouter.setCorsHeaders(outHeaders, origin);

		String ctxSessionKey = StringUtil.safeString(params.get(HttpParamKey.CTX_SESSION_KEY));
		int eventCount = StringUtil.safeParseInt(params.get(PARAM_EVENT_COUNT));

		if (eventCount > 0) {
			processBatchEvents(req, params, resp, device, ctxSessionKey, serverInfo);
		} else {
			processSingleEvent(req, urlPath, params, resp, device, ctxSessionKey, serverInfo);
		}
	}

	/**
	 * Handles Batch Events (Queue flushing)
	 */
	private static void processBatchEvents(HttpServerRequest req, MultiMap globalParams, HttpServerResponse resp,
			DeviceInfo device, String ctxSessionKey, String serverInfo) {

		String eventsStr = globalParams.get(PARAM_EVENTS);

		// Handle form attribute fallback if not in query params
		if (StringUtil.isEmpty(eventsStr)) {
			eventsStr = req.getFormAttribute(PARAM_EVENTS);
		}

		if (StringUtil.isEmpty(eventsStr)) {
			ObserverResponse.done(resp, 400, "", ctxSessionKey, 0);
			return;
		}

		List<EventPayload> events;
		try {
			events = gson.fromJson(eventsStr, EVENT_LIST_TYPE);
		} catch (JsonSyntaxException e) {
			System.err.println("JSON Parsing Error: " + e.getMessage());
			ObserverResponse.done(resp, 400, "", ctxSessionKey, 0);
			return;
		}

		System.out.println("Processing Batch: " + events.size() + " events.");

		String lastVisitorId = "";
		String sessionKey = ctxSessionKey;
		int successCount = events.size();

		// Resolve IP once for the batch
		String ip = HttpWebParamUtil.getRemoteIP(req);

		ContextSession ctxSession = null;
		for (EventPayload event : events) {
			// 1. Prepare Params for this specific event
			// We create a copy of global params and inject specific event data
			MultiMap eventParams = MultiMap.caseInsensitiveMultiMap();
			eventParams.addAll(globalParams);

			// Map EventPayload fields to HttpParamKey expected by EventObserverUtil
			String metric = event.getMetric();
			eventParams.set(HttpParamKey.EVENT_METRIC_NAME, metric);

			// If the payload has encoded JSON data, pass it so Util can read it
			if (StringUtil.isNotEmpty(event.getRawEventData())) {
				// Assuming the Util looks for specific keys, or generic 'eventdata'
				// You might need to adjust the key based on what EventObserverUtil expects
				eventParams.set(HttpParamKey.EVENT_DATA, event.getDecodedEventData());
			}

			// 2. Resolve Session (Try to reuse session if possible, but safe to fetch per
			// event)
			// If payload has visid, we might prioritize it
			if (StringUtil.isNotEmpty(event.getVisId())) {
				lastVisitorId = event.getVisId();
				eventParams.set(HttpParamKey.VISITOR_ID, lastVisitorId);
			}

			eventParams.set(HttpParamKey.OBSERVER_ID, event.getObsId());
			eventParams.set(HttpParamKey.FINGERPRINT_ID, event.getFgp());
			eventParams.set(HttpParamKey.TOUCHPOINT_URL, event.getDecodedTpUrl());
			eventParams.set(HttpParamKey.TOUCHPOINT_NAME, event.getDecodedTpName());
			eventParams.set(HttpParamKey.TOUCHPOINT_REFERRER_URL, event.getTpRefUrl());
			eventParams.set(HttpParamKey.TOUCHPOINT_REFERRER_DOMAIN, event.getTpRefDomain());
			eventParams.set(HttpParamKey.MEDIA_HOST, event.getMediaHost());

			if (ctxSession == null || StringUtil.isEmpty(sessionKey)) {
				ctxSession = ContextSessionManagement.get(sessionKey, ip, eventParams, device);
				sessionKey = ctxSession.getSessionKey();
			}

			// 3. Record Event
			// Note: We default to ACTION logic for batch, unless metric implies otherwise.
			// You could add logic here: if(metric.startsWith("buy")) recordConversion...
			saveEvent(req, device, ctxSession, eventParams, metric);

		}

		ObserverResponse.done(resp, 200, lastVisitorId, sessionKey, successCount);
	}

	private static void saveEvent(HttpServerRequest req, DeviceInfo device, ContextSession ctxSession,
			MultiMap eventParams, String metric) {
		if (ctxSession != null) {
			TaskRunner.runInThreadPools(() -> {
				EventObserverUtil.recordActionEvent(req, eventParams, device, ctxSession, metric);
			});
		}
	}

	/**
	 * Handles Single Event
	 */
	static void processSingleEvent(HttpServerRequest req, String urlPath, MultiMap params, HttpServerResponse resp,
			DeviceInfo device, String ctxSessionKey, String serverInfo) {

		String ip = HttpWebParamUtil.getRemoteIP(req);

		// 1. Standard Action Event
		if (urlPath.equalsIgnoreCase(PREFIX_EVENT_ACTION)) {
			handleStandardEvent(req, params, resp, device, ctxSessionKey, ip, false);
		}
		// 2. Conversion Event
		else if (urlPath.equalsIgnoreCase(PREFIX_EVENT_CONVERSION)) {
			handleStandardEvent(req, params, resp, device, ctxSessionKey, ip, true);
		}
		// 3. Feedback / Survey
		else if (urlPath.equalsIgnoreCase(PREFIX_EVENT_FEEDBACK)) {
			handleFeedbackEvent(req, params, resp, device, ctxSessionKey, ip);
		}
		// 4. Profile Update (Leo Form)
		else if (urlPath.equalsIgnoreCase(PREFIX_CONTEXT_SESSION_PROFILE_UPDATE)) {
			handleProfileUpdate(req, params, resp, device, ctxSessionKey, ip);
		}
		// 5. 404
		else {
			resp.end("CDP Observer_" + serverInfo);
		}
	}

	/**
	 * Helper to reduce duplication between Action and Conversion events
	 */
	private static void handleStandardEvent(HttpServerRequest req, MultiMap params, HttpServerResponse resp,
			DeviceInfo device, String ctxSessionKey, String ip, boolean isConversion) {

		ContextSession ctxSession = ContextSessionManagement.get(ctxSessionKey, ip, params, device);
		int status = 404;
		String visitorId = "";
		String sessionKey = "";

		if (ctxSession != null) {
			visitorId = ctxSession.getVisitorId();
			sessionKey = ctxSession.getSessionKey();

			String eventName = StringUtil.safeString(params.get(HttpParamKey.EVENT_METRIC_NAME)).toLowerCase();
			String eventId;

			if (isConversion) {
				eventId = EventObserverUtil.recordConversionEvent(req, params, device, ctxSession, eventName);
			} else {
				eventId = EventObserverUtil.recordActionEvent(req, params, device, ctxSession, eventName);
			}

			if (StringUtil.isNotEmpty(eventId)) {
				status = 200;
			}
		} else {
			sendInvalidSessionError(resp, status);
			return; // Exit early if response is sent
		}

		ObserverResponse.done(resp, status, visitorId, sessionKey, DEFAULT_RESPONSE);
	}

	private static void handleFeedbackEvent(HttpServerRequest req, MultiMap params, HttpServerResponse resp,
			DeviceInfo device, String ctxSessionKey, String ip) {

		String eventName = StringUtil.safeString(params.get(HttpParamKey.EVENT_METRIC_NAME)).toLowerCase();
		FeedbackEvent feedbackEvent = HttpWebParamUtil.getFeedbackEventFromHttpPost(req, eventName);
		ContextSession ctxSession = ContextSessionManagement.get(ctxSessionKey, ip, params, device);

		int status = 500;

		if (ctxSession != null && feedbackEvent != null) {
			String eventId = EventObserverUtil.recordFeedbackEvent(req, device, ctxSession, feedbackEvent);
			if (StringUtil.isNotEmpty(eventId)) {
				status = 200;
			}
			ObserverResponse.done(resp, status, ctxSession.getVisitorId(), ctxSession.getSessionKey(),
					DEFAULT_RESPONSE);
		} else {
			sendInvalidSessionError(resp, status);
		}
	}

	private static void handleProfileUpdate(HttpServerRequest req, MultiMap params, HttpServerResponse resp,
			DeviceInfo device, String ctxSessionKey, String ip) {

		ContextSession session = ContextSessionManagement.get(ctxSessionKey, ip, params, device);
		int status = 404;

		if (session != null) {
			String profileId = session.getProfileId();
			if (StringUtil.isNotEmpty(profileId)) {
				status = ContextSessionManagement.updateProfileData(req, params, session, device);
			} else {
				status = 101; // Profile ID missing
			}

			var responseData = new ObserverResponse(session.getVisitorId(), session.getSessionKey(), OK, status);
			resp.end(gson.toJson(responseData));
		} else {
			sendInvalidSessionError(resp, status);
		}
	}

	private static void sendInvalidSessionError(HttpServerResponse resp, int status) {
		var errorResponse = new ObserverResponse("", "", INVALID, status);
		resp.end(gson.toJson(errorResponse));
	}
}