package leotech.cdp.handler;

import static leotech.starter.router.ObserverHttpRouter.FAILED;
import static leotech.starter.router.ObserverHttpRouter.INVALID;
import static leotech.starter.router.ObserverHttpRouter.OK;

import com.google.gson.Gson;

import io.vertx.core.http.HttpServerResponse;

/**
 * Observer Response
 * 
 * @author @tantrieuf31
 */
public final class ObserverResponse extends DataResponse {
	
	public final String sessionKey;
	public final String visitorId;
	
	public final int eventCount;
	
	public ObserverResponse(String visitorId, String sessionKey, String message, int status, int eventCount) {
		super();
		this.visitorId = visitorId;
		this.sessionKey = sessionKey;
		this.message = message;
		this.status = status;
		this.eventCount = eventCount;
	}
	
	public ObserverResponse(String visitorId, String sessionKey, String message, int status) {
		super();
		this.visitorId = visitorId;
		this.sessionKey = sessionKey;
		this.message = message;
		this.status = status;
		this.eventCount = 0;
	}
	
	/**
	 * @param resp
	 * @param status
	 * @param visitorId
	 * @param sessionKey
	 * @param eventId
	 */
	final public static void done(HttpServerResponse resp, int status, String visitorId, String sessionKey, int eventCount) {
		if (status >= 200 && status < 300) {
			ObserverResponse rs = new ObserverResponse(visitorId, sessionKey, OK, status, eventCount);
			resp.end(new Gson().toJson(rs));
		} else if (status == 500) {
			ObserverResponse rs = new ObserverResponse(visitorId, sessionKey, FAILED, status);
			resp.end(new Gson().toJson(rs));
		} else if (status == 102) {
			ObserverResponse rs = new ObserverResponse(visitorId, sessionKey, OK, status, eventCount);
			resp.end(new Gson().toJson(rs));
		} else {
			ObserverResponse rs = new ObserverResponse(visitorId, sessionKey, INVALID, status);
			resp.end(new Gson().toJson(rs));
		}
	}
}
