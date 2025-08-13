package leotech.cdp.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static leotech.starter.router.ObserverHttpRouter.INVALID;
import static leotech.starter.router.ObserverHttpRouter.OK;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_CONTEXT_SESSION_PROFILE_UPDATE;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_ACTION;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_CONVERSION;
import static leotech.starter.router.ObserverHttpRouter.PREFIX_EVENT_FEEDBACK;
import static leotech.system.common.BaseHttpRouter.DEFAULT_RESPONSE_TEXT;

import com.google.gson.Gson;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.domain.ContextSessionManagement;
import leotech.cdp.model.analytics.ContextSession;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.system.common.BaseHttpHandler;
import leotech.system.common.BaseHttpRouter;
import leotech.system.model.DeviceInfo;
import leotech.system.util.HttpWebParamUtil;
import rfx.core.util.StringUtil;

/**
 * 
 * @author @tantrieuf31
 */
public final class ObserverHttpPostHandler {
	


	/**
	 * HTTP Method Post Handler
	 * 
	 * @param req
	 * @param urlPath
	 * @param params
	 * @param resp
	 * @param outHeaders
	 * @param device
	 * @param origin
	 * @param eventName
	 * @param clientSessionKey
	 * @return
	 */
	public final static boolean process(RoutingContext context, HttpServerRequest req, String urlPath, MultiMap reqHeaders, MultiMap params, HttpServerResponse resp,
			MultiMap outHeaders, DeviceInfo device, String origin) {
		String eventName = StringUtil.safeString(params.get(HttpParamKey.EVENT_METRIC_NAME)).toLowerCase();
		String clientSessionKey = StringUtil.safeString(params.get(HttpParamKey.CTX_SESSION_KEY));
		
		outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);
		BaseHttpRouter.setCorsHeaders(outHeaders, origin);
		
		if (urlPath.equalsIgnoreCase(PREFIX_EVENT_ACTION)) {
			// synch ContextSession with event tracking
			ContextSession ctxSession = ContextSessionManagement.get(clientSessionKey, req, params, device);
			int status = 404;
			String eventId = "", visitorId = "", sessionKey = "";
			if (ctxSession != null) {
				visitorId = ctxSession.getVisitorId();
				sessionKey = ctxSession.getSessionKey();

				// event-conversion(add_to_cart|submit_form|checkout|join,sessionKey,visitorId)
				eventId = EventObserverUtil.recordActionEvent(req, params, device, ctxSession, eventName);
				if(StringUtil.isNotEmpty(eventId)) {
					status = 200;
				}
			}
			else {
				// invalid session info
				resp.end(new Gson().toJson(new ObserverResponse("","", INVALID, status)));
			}
			ObserverResponse.done(resp, status, visitorId, sessionKey, eventId);
			return true;
		}
		// conversion event 
		else if (urlPath.equalsIgnoreCase(PREFIX_EVENT_CONVERSION) ) {
			// synch ContextSession with event tracking
			ContextSession ctxSession = ContextSessionManagement.get(clientSessionKey, req, params, device);
			int status = 404;
			String eventId = "", visitorId = "", sessionKey = "";
			if (ctxSession != null) {
				visitorId = ctxSession.getVisitorId();
				sessionKey = ctxSession.getSessionKey();

				// event-conversion(add_to_cart|submit_form|checkout|join,sessionKey,visitorId)
				eventId = EventObserverUtil.recordConversionEvent(req, params, device, ctxSession, eventName);
				if (StringUtil.isNotEmpty(eventId)) {
					status = 200;
				}
			}
			else {
				// invalid session info
				resp.end(new Gson().toJson(new ObserverResponse("","", INVALID, status)));
			}
			ObserverResponse.done(resp, status, visitorId, sessionKey, eventId);
			return true;
		}

		// collect data from SURVEY and feedback data
		else if (urlPath.equalsIgnoreCase(PREFIX_EVENT_FEEDBACK)) {
			// synch ContextSession with event tracking
			FeedbackEvent feedbackEvent = HttpWebParamUtil.getFeedbackEventFromHttpPost(req, eventName);
			ContextSession currentSession = ContextSessionManagement.get(clientSessionKey, req, params, device);

			int status = 500;
			String eventId = "";
			if (currentSession != null && feedbackEvent != null) {
				// event-feedback submit-[feedbackType]-form
				eventId = EventObserverUtil.recordFeedbackEvent(req, device, currentSession, feedbackEvent);
				if (StringUtil.isNotEmpty(eventId)) {
					status = 200;
				}
			} 
			else {
				//
				resp.end(new Gson().toJson(new ObserverResponse("", "", INVALID, status)));
				return false;
			}
			String visitorId = currentSession.getVisitorId();
			String sessionKey = currentSession.getSessionKey();
			ObserverResponse.done(resp, status, visitorId, sessionKey, eventId);
			return true;
		}

		// collect data from LEO FORM JS
		else if (urlPath.equalsIgnoreCase(PREFIX_CONTEXT_SESSION_PROFILE_UPDATE)) {
			int status = 404;
			// SYNCH ContextSession with request
			ContextSession session = ContextSessionManagement.get(clientSessionKey, req, params, device);

			// UPDATE profile from POST data
			String profileId = session.getProfileId();
			if (StringUtil.isNotEmpty(profileId)) {
				status = ContextSessionManagement.updateProfileData(req, params, session, device);
			} else {
				status = 101;
			}
			DataResponse rs = new ObserverResponse(session.getVisitorId(), session.getSessionKey(), OK, status);
			resp.end(new Gson().toJson(rs));
			return true;
		}		
		
		// no handler found
		else {
			resp.end("CDP Observer_" + DEFAULT_RESPONSE_TEXT);
			return false;
		}
	}
}
