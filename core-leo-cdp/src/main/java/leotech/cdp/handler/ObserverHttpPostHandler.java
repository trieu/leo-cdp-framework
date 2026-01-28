package leotech.cdp.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static leotech.starter.router.ObserverHttpRouter.INVALID;
import static leotech.starter.router.ObserverHttpRouter.OK;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_CONTEXT_SESSION_PROFILE_UPDATE;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_ACTION;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_FEEDBACK;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_VIEW;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_STANDARD_EVENT_PREFIX;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.domain.ContextSessionManagement;
import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.model.analytics.ContextSession;
import leotech.cdp.model.analytics.EventPayload;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.journey.EventMetric;
import leotech.system.common.BaseHttpHandler;
import leotech.system.common.BaseHttpRouter;
import leotech.system.model.DeviceInfo;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.TaskRunner;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * HTTP POST Event Data handler
 * 
 * @author tantrieuf31
 */
public final class ObserverHttpPostHandler {

	private static final String CDP_OBSERVER_API = "CDP Observer API";

	// Thread-safe singleton for Gson
	private static final Gson gson = new Gson();

	// Constants
	private static final String PARAM_EVENTS = "events";
	private static final String PARAM_EVENT_COUNT = "evc";
	private static final int DEFAULT_RESPONSE = 1;

	private static final Type EVENT_LIST_TYPE = new TypeToken<ArrayList<EventPayload>>() {}.getType();

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
			// batch tracking 
			processBatchEvents(req, params, resp, device, ctxSessionKey);
		} else {
			// legacy tracking
			processSingleEvent(req, urlPath, params, resp, device, ctxSessionKey);
		}
	}

	/**
	 * Handles Batch Events (Queue flushing)
	 */
	private static void processBatchEvents(HttpServerRequest req, MultiMap globalParams, HttpServerResponse resp,
			DeviceInfo device, String ctxSessionKey) {

		String eventsStr = globalParams.get(PARAM_EVENTS);

		// Handle form attribute fallback if not in query params
		if (StringUtil.isEmpty(eventsStr)) {
			eventsStr = req.getFormAttribute(PARAM_EVENTS);
		}

		if (StringUtil.isEmpty(eventsStr)) {
			ObserverResponse.done(resp, 400, "events in HTTP Body is empty!", ctxSessionKey, 0);
			return;
		}

		List<EventPayload> events = convertJsonToEvents(resp, ctxSessionKey, eventsStr);
		if(events == null) {
			ObserverResponse.done(resp, 500, "Processing batch events failed, can not parse JSON to EventPayload", ctxSessionKey, 0);
			return;
		}

		System.out.println("Processing Batch: " + events.size() + " events.");

		String visitorId;
		// 2. Resolve Session (Try to reuse session if possible, but safe to fetch per
		// event)
		// If payload has visid, we might prioritize it
		if (events.size() >0) {
			EventPayload event = events.get(0);
			visitorId = StringUtil.safeString(event.getVisId());
		}
		else {
			visitorId = "";
		}
		
		int successCount = events.size();
		ObserverResponse.done(resp, 200, visitorId, ctxSessionKey, successCount);
		
		
		TaskRunner.runInThreadPools(()->{
			// Resolve IP once for the batch
			String ip = HttpWebParamUtil.getRemoteIP(req);
			String sessionKey = ctxSessionKey;

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

				eventParams.set(HttpParamKey.VISITOR_ID, visitorId);
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

		});

	}
	
	private static void saveEvent(HttpServerRequest req, DeviceInfo device, ContextSession ctxSession, MultiMap eventParams, String metricName) {
		if (ctxSession != null) {
			EventMetric metric = EventMetricManagement.getEventMetricByName(metricName);
			if(metric.isConversion()) {
				EventObserverUtil.recordConversionEvent(req, eventParams, device, ctxSession, metricName);
			}
			else {				
				EventObserverUtil.recordBehavioralEvent(req, eventParams, device, ctxSession, metricName);
			}
		}
	}

	private static List<EventPayload> convertJsonToEvents(HttpServerResponse resp, String ctxSessionKey,
			String eventsStr) {
		List<EventPayload> events = null;
		try {
			events = gson.fromJson(eventsStr, EVENT_LIST_TYPE);
		} catch (Exception e) {
			System.err.println("JSON Parsing Error: " + e.getMessage());
			return events;
		}
		return events;
	}



	/**
	 * Handles Single Event
	 */
	static void processSingleEvent(HttpServerRequest req, String urlPath, MultiMap params, HttpServerResponse resp,
			DeviceInfo device, String ctxSessionKey) {

		String ip = HttpWebParamUtil.getRemoteIP(req);

		// 1. Standard Event
		if (urlPath.startsWith(PREFIX_STANDARD_EVENT_PREFIX)) {
			handleStandardEvent(req, params, resp, device, ctxSessionKey, ip, urlPath);
		}
		// 2. Feedback / Survey
		else if (urlPath.equalsIgnoreCase(PREFIX_EVENT_FEEDBACK)) {
			handleFeedbackEvent(req, params, resp, device, ctxSessionKey, ip);
		}
		// 3. Profile Update (Leo Form)
		else if (urlPath.equalsIgnoreCase(PREFIX_CONTEXT_SESSION_PROFILE_UPDATE)) {
			handleProfileUpdate(req, params, resp, device, ctxSessionKey, ip);
		}
		// 4. 404
		else {
			resp.end(CDP_OBSERVER_API);
		}
	}

	/**
	 *  Helper to reduce duplication between Action and Conversion events
	 * 
	 * @param req
	 * @param params
	 * @param resp
	 * @param device
	 * @param ctxSessionKey
	 * @param ip
	 * @param urlPath
	 */
	private static void handleStandardEvent(HttpServerRequest req, MultiMap params, HttpServerResponse resp,
			DeviceInfo device, String ctxSessionKey, String ip, String urlPath) {

		ContextSession ctxSession = ContextSessionManagement.get(ctxSessionKey, ip, params, device);
		int status = 404;
		String visitorId = "";
		String sessionKey = "";

		if (ctxSession != null) {
			visitorId = ctxSession.getVisitorId();
			sessionKey = ctxSession.getSessionKey();

			String eventName = StringUtil.safeString(params.get(HttpParamKey.EVENT_METRIC_NAME)).toLowerCase();
			String eventId = StringPool.BLANK;

			if (urlPath.equalsIgnoreCase(PREFIX_EVENT_ACTION) || urlPath.equalsIgnoreCase(PREFIX_EVENT_VIEW)) {
				eventId = EventObserverUtil.recordBehavioralEvent(req, params, device, ctxSession, eventName);
			}
			else {
				eventId = EventObserverUtil.recordConversionEvent(req, params, device, ctxSession, eventName);
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

	/**
	 * @param req
	 * @param params
	 * @param resp
	 * @param device
	 * @param ctxSessionKey
	 * @param ip
	 */
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
			ObserverResponse.done(resp, status, ctxSession.getVisitorId(), ctxSession.getSessionKey(), DEFAULT_RESPONSE);
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