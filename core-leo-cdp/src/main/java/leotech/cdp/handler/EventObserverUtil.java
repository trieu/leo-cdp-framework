package leotech.cdp.handler;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.github.jknack.handlebars.internal.text.StringEscapeUtils;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import leotech.cdp.domain.DeviceManagement;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.model.analytics.ContextSession;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.analytics.OrderTransaction;
import leotech.cdp.model.analytics.OrderedItem;
import leotech.cdp.utils.EventTrackingUtil;
import leotech.cdp.utils.RealtimeTrackingUtil;
import leotech.system.model.DeviceInfo;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.UrlUtil;
import rfx.core.util.StringUtil;

/**
 *  Event Observer Util for API and http handler
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class EventObserverUtil {

	/**
	 * @param req
	 * @param params
	 * @param device
	 * @param ctxSession
	 * @param eventName
	 * @return
	 */
	public final static String recordViewEvent(HttpServerRequest req, MultiMap params, DeviceInfo device, ContextSession ctxSession, String eventName) {
		String sourceIP = HttpWebParamUtil.getRemoteIP(req);

		String srcObserverId = params.get(HttpParamKey.OBSERVER_ID);
		String srcTouchpointName = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_NAME);
		String srcTouchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_URL);
		String refTouchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_REFERRER_URL);
		String touchpointRefDomain = UrlUtil.getHostName(refTouchpointUrl);
		
		String deviceId = DeviceManagement.getDeviceId(params, device);
		String fingerprintId = StringUtil.safeString(params.get(HttpParamKey.FINGERPRINT_ID));
		String environment = StringUtil.safeString(params.get(HttpParamKey.DATA_ENVIRONMENT),HttpParamKey.PRO_ENV);
		Map<String, Object> eventData = HttpWebParamUtil.getEventData(params);

		//System.out.println(new Gson().toJson(eventJsonData));
		Date createdAt = new Date();

		return EventObserverManagement.recordEventFromWeb(createdAt, ctxSession,srcObserverId, environment, fingerprintId, deviceId, sourceIP, device,
				srcTouchpointName, srcTouchpointUrl, refTouchpointUrl, touchpointRefDomain, eventName, eventData);
	}

	/**
	 * @param req
	 * @param params
	 * @param device
	 * @param ctxSession
	 * @param eventName
	 * @return
	 */
	public final static String recordActionEvent(HttpServerRequest req, MultiMap params, DeviceInfo device, ContextSession ctxSession, String eventName) {
		Date createdAt = new Date();
		
		String sourceIP = HttpWebParamUtil.getRemoteIP(req);
		String srcObserverId = params.get(HttpParamKey.OBSERVER_ID);
		
		String srcTouchpointName = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_NAME);
		String srcTouchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_URL);
		
		String refTouchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_REFERRER_URL);
		String touchpointRefDomain = UrlUtil.getHostName(refTouchpointUrl);
		
		String deviceId = DeviceManagement.getDeviceId(params, device);
		String fingerprintId = StringUtil.safeString(params.get(HttpParamKey.FINGERPRINT_ID));
		String environment = StringUtil.safeString(params.get(HttpParamKey.DATA_ENVIRONMENT), HttpParamKey.PRO_ENV);	
		Map<String, Object> eventData = HttpWebParamUtil.getEventData(params);
		
		return EventObserverManagement.recordEventFromWeb(createdAt, ctxSession, srcObserverId, environment, fingerprintId, deviceId, sourceIP, device,
				srcTouchpointName, srcTouchpointUrl, refTouchpointUrl,  touchpointRefDomain, eventName, eventData);
	}

	/**
	 * @param req
	 * @param params
	 * @param deviceInfo
	 * @param ctxSession
	 * @param eventName
	 * @return
	 */
	public final static String recordConversionEvent(HttpServerRequest req, MultiMap params, DeviceInfo deviceInfo, ContextSession ctxSession, String eventName) {
		Date createdAt = new Date();
		
		String sourceIP = HttpWebParamUtil.getRemoteIP(req);
		MultiMap formData = req.formAttributes();
		
		String deviceId = DeviceManagement.getDeviceId(formData, deviceInfo);
		String fingerprintId = StringUtil.safeString(params.get(HttpParamKey.FINGERPRINT_ID));
		String environment = StringUtil.safeString(params.get(HttpParamKey.DATA_ENVIRONMENT), HttpParamKey.PRO_ENV);
		//String srcEventKey = StringUtil.safeString(params.get(TrackingApiParam.SRC_EVENT_KEY));
		
		String srcObserverId = formData.get(HttpParamKey.OBSERVER_ID);
		String srcTouchpointName = HttpWebParamUtil.getString(formData, HttpParamKey.TOUCHPOINT_NAME);
		String srcTouchpointUrl = HttpWebParamUtil.getString(formData, HttpParamKey.TOUCHPOINT_URL);
		String refTouchpointUrl = HttpWebParamUtil.getString(formData, HttpParamKey.TOUCHPOINT_REFERRER_URL);
		String touchpointRefDomain = UrlUtil.getHostName(refTouchpointUrl);
		
		Map<String, Object> eventData = HttpWebParamUtil.getEventData(params);
						
		OrderTransaction transaction = new OrderTransaction(createdAt, formData); 		
		String transactionId = transaction.getTransactionId();
		double totalTransactionValue = transaction.getTotalTransactionValue();
		String currencyCode = transaction.getCurrencyCode();
		Set<OrderedItem> orderedItems = transaction.getOrderedItems();

		return EventObserverManagement.recordConversionFromWeb(createdAt, ctxSession, srcObserverId, environment, fingerprintId, deviceId, sourceIP, deviceInfo, srcTouchpointName,
				srcTouchpointUrl, refTouchpointUrl, touchpointRefDomain, eventName, eventData, transactionId, orderedItems, totalTransactionValue, currencyCode);
	}
	
	/**
	 * @param req
	 * @param device
	 * @param ctxSession
	 * @param eventName
	 * @return
	 */
	public final static String recordFeedbackEvent(HttpServerRequest req, DeviceInfo device, ContextSession ctxSession, FeedbackEvent feedbackEvent) {
		Date createdAt = new Date();
		String sourceIP = HttpWebParamUtil.getRemoteIP(req);
		MultiMap formData = req.formAttributes();
		String srcObserverId = StringUtil.safeString(formData.get(HttpParamKey.OBSERVER_ID));	
		String touchpointName =  HttpWebParamUtil.getString(formData, HttpParamKey.TOUCHPOINT_NAME);
		String touchpointUrl =  HttpWebParamUtil.getString(formData, HttpParamKey.TOUCHPOINT_URL);
		String fingerprintId = StringUtil.safeString(formData.get(HttpParamKey.FINGERPRINT_ID));
		
		touchpointName = StringEscapeUtils.unescapeHtml4(touchpointName);
		touchpointUrl = StringEscapeUtils.unescapeHtml4(touchpointUrl);
		
		String deviceId = DeviceManagement.getDeviceId(formData, device);
		String environment = StringUtil.safeString(formData.get(HttpParamKey.DATA_ENVIRONMENT), HttpParamKey.PRO_ENV);	
		
		return EventObserverManagement.recordFeedbackEvent(createdAt, ctxSession, environment, fingerprintId, deviceId, sourceIP, device, srcObserverId,
				touchpointName, touchpointUrl, feedbackEvent);
	}
	
	public final static void realtimeLogging(String metric, String cid, long loggedTime, int platformId, String uuid,
			String locationId, boolean countUserReach) {
		String kNew = metric + "-" + cid + "-" + platformId + "-" + "-" + locationId;
		String[] events = new String[]{metric, metric + "-" + cid, kNew};
		EventTrackingUtil.updateEvent(loggedTime, events, true);
		if (countUserReach) {
			RealtimeTrackingUtil.trackContentViewFromUser(loggedTime, cid, uuid);
		}
	}

}
