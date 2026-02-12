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
import leotech.system.model.DeviceInfo;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.UrlUtil;
import rfx.core.util.StringUtil;

/**
 * Event Observer Util for API and HTTP handlers
 *
 * @author tantrieuf31
 * @since 2020
 */
public final class EventObserverUtil {

	/**
	 * Unified method for VIEW + ACTION events
	 *
	 * @param req
	 * @param params
	 * @param device
	 * @param ctxSession
	 * @param eventName
	 * @return eventId
	 */
	public static String recordBehavioralEvent(
			HttpServerRequest req,
			MultiMap params,
			DeviceInfo device,
			ContextSession ctxSession,
			String eventName
	) {
		final Date createdAt = new Date();

		final String sourceIP = HttpWebParamUtil.getRemoteIP(req);
		final String srcObserverId = params.get(HttpParamKey.OBSERVER_ID);

		final String srcTouchpointName = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_NAME);
		final String srcTouchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_URL);
		final String refTouchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_REFERRER_URL);
		final String touchpointRefDomain = UrlUtil.getHostName(refTouchpointUrl);

		final String deviceId = DeviceManagement.getDeviceId(params, device);
		final String fingerprintId = StringUtil.safeString(params.get(HttpParamKey.FINGERPRINT_ID));
		final String environment = StringUtil.safeString(
				params.get(HttpParamKey.DATA_ENVIRONMENT),
				HttpParamKey.PRO_ENV
		);

		final Map<String, Object> eventData = HttpWebParamUtil.getEventData(params);

		return EventObserverManagement.recordEventFromWeb(
				createdAt,
				ctxSession,
				srcObserverId,
				environment,
				fingerprintId,
				deviceId,
				sourceIP,
				device,
				srcTouchpointName,
				srcTouchpointUrl,
				refTouchpointUrl,
				touchpointRefDomain,
				eventName,
				eventData
		);
	}

	/**
	 * Conversion / Transaction event
	 */
	public static String recordConversionEvent(
			HttpServerRequest req,
			MultiMap params,
			DeviceInfo deviceInfo,
			ContextSession ctxSession,
			String eventName
	) {
		final Date createdAt = new Date();
		final String sourceIP = HttpWebParamUtil.getRemoteIP(req);
		

		final String deviceId = DeviceManagement.getDeviceId(params, deviceInfo);
		final String fingerprintId = StringUtil.safeString(params.get(HttpParamKey.FINGERPRINT_ID));
		final String environment = StringUtil.safeString(
				params.get(HttpParamKey.DATA_ENVIRONMENT),
				HttpParamKey.PRO_ENV
		);


		final String srcObserverId = params.get(HttpParamKey.OBSERVER_ID);
		final String srcTouchpointName = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_NAME);
		final String srcTouchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_URL);
		final String refTouchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_REFERRER_URL);
		final String touchpointRefDomain = UrlUtil.getHostName(refTouchpointUrl);

		final Map<String, Object> eventData = HttpWebParamUtil.getEventData(params);

		final OrderTransaction transaction = new OrderTransaction(createdAt, params);
		final String transactionId = transaction.getTransactionId();
		final double totalTransactionValue = transaction.getTotalTransactionValue();
		final String currencyCode = transaction.getCurrencyCode();
		final Set<OrderedItem> orderedItems = transaction.getOrderedItems();

		return EventObserverManagement.recordConversionFromWeb(
				createdAt,
				ctxSession,
				srcObserverId,
				environment,
				fingerprintId,
				deviceId,
				sourceIP,
				deviceInfo,
				srcTouchpointName,
				srcTouchpointUrl,
				refTouchpointUrl,
				touchpointRefDomain,
				eventName,
				eventData,
				transactionId,
				orderedItems,
				totalTransactionValue,
				currencyCode
		);
	}

	/**
	 * Feedback / Survey event
	 */
	public static String recordFeedbackEvent(
			HttpServerRequest req,
			DeviceInfo device,
			ContextSession ctxSession,
			FeedbackEvent feedbackEvent
	) {
		final Date createdAt = new Date();
		final String sourceIP = HttpWebParamUtil.getRemoteIP(req);
		final MultiMap formData = req.formAttributes();

		final String srcObserverId = StringUtil.safeString(formData.get(HttpParamKey.OBSERVER_ID));
		String touchpointName = HttpWebParamUtil.getString(formData, HttpParamKey.TOUCHPOINT_NAME);
		String touchpointUrl = HttpWebParamUtil.getString(formData, HttpParamKey.TOUCHPOINT_URL);

		final String fingerprintId = StringUtil.safeString(formData.get(HttpParamKey.FINGERPRINT_ID));
		final String deviceId = DeviceManagement.getDeviceId(formData, device);
		final String environment = StringUtil.safeString(
				formData.get(HttpParamKey.DATA_ENVIRONMENT),
				HttpParamKey.PRO_ENV
		);

		touchpointName = StringEscapeUtils.unescapeHtml4(touchpointName);
		touchpointUrl = StringEscapeUtils.unescapeHtml4(touchpointUrl);

		return EventObserverManagement.recordFeedbackEvent(
				createdAt,
				ctxSession,
				environment,
				fingerprintId,
				deviceId,
				sourceIP,
				device,
				srcObserverId,
				touchpointName,
				touchpointUrl,
				feedbackEvent
		);
	}
}
