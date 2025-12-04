package leotech.cdp.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import leotech.cdp.dao.ContextSessionDaoUtil;
import leotech.cdp.dao.DeviceDaoUtil;
import leotech.cdp.dao.EventObserverDaoUtil;
import leotech.cdp.model.analytics.ContextSession;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.analytics.OrderTransaction;
import leotech.cdp.model.analytics.OrderedItem;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.analytics.UpdateProfileEvent;
import leotech.cdp.model.customer.BasicContactData;
import leotech.cdp.model.customer.Device;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.model.journey.TouchpointType;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.DeviceInfo;
import leotech.system.util.DeviceInfoUtil;
import rfx.core.util.StringUtil;

/**
 * The event stream observer management
 * 
 * @author Trieu Nguyen (Thomas)
 * @since 2020
 */
public class EventObserverManagement {

	// re-targeting 
	public static final String ADS_RETARGETING = "ads-retargeting";
	public static final String EMAIL_RETARGETING = "email-retargeting";
	public static final String WEB_PUSH_RETARGETING = "web-push-retargeting";
	public static final String APP_PUSH_RETARGETING = "app-push-retargeting";
	public static final String SMS_RETARGETING = "sms-retargeting";
	
	// marketing cross-sales or value-added services to optimize CLV scoring model 
	public static final String EMAIL_MARKETING = "email-marketing";
	public static final String WEB_PUSH_MARKETING = "web-push-marketing";
	public static final String APP_PUSH_MARKETING = "app-push-marketing";
	
	/**
	 * start Pub Sub for all observer, type = input data sources
	 */
	public static void initAllInputDataObservers() {
		 List<EventObserver> eventObservers = EventObserverDaoUtil.getAllInputDataSourceObserver();
		 System.out.println(" AllInputDataSourceObserver.size " + eventObservers.size());
		 for (EventObserver eventObserver : eventObservers) {
			 System.out.println("eventObserver.getDataSourceUrl " + eventObserver.getDataSourceUrl());
			 processInputDataSourceObserver(eventObserver);
		}
	}
	
	/**
	 * @return
	 */
	public static List<EventObserver> listAll() {
		return EventObserverDaoUtil.listAll();
	}
	
	
	/**
	 * @param journeyMapId
	 * @return
	 */
	public static List<EventObserver> listAllByJourneyMap(String journeyMapId, String filterKeywords) {
		return EventObserverDaoUtil.listAllByJourneyMap(journeyMapId, filterKeywords);
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static EventObserver getById(String id) {
		EventObserver o = EventObserverDaoUtil.getById(id);
		return o != null ? o : getDefaultDataObserver();
	}
	
	/**
	 * to validate a true observerId from API
	 * 
	 * @param observerId
	 * @return
	 */
	public static String validateObserverId(String observerId) {
		if(StringUtil.isNotEmpty(observerId)){
			EventObserver o = EventObserverDaoUtil.getById(observerId);
			return o != null ? o.getId() : TouchpointHub.DATA_OBSERVER.getId();
		}
		return TouchpointHub.DATA_OBSERVER.getId();
	}
	
	/**
	 * @return
	 */
	public static EventObserver getDefaultDataObserver() {
		return EventObserverDaoUtil.getDefaultDataObserver();
	}
	
	public static EventObserver getEventObserver(TargetMediaUnit media) {
		EventObserver o = getById(media.getRefObserverId());
		if(o != null) {
			return o;
		}
		return EventObserverDaoUtil.getDefaultDataObserver();
	}

	
	/**
	 * @param touchpointHubId
	 * @return
	 */
	public static EventObserver getByTouchpointHubId(String touchpointHubId) {
		return EventObserverDaoUtil.getByTouchpointHubId(touchpointHubId);
	}
	
	/**
	 * save event from Web: View Event Tracking or Action Event Tracking
	 * 
	 * @param createdAt
	 * @param ctxSession
	 * @param srcObserverId
	 * @param environment
	 * @param deviceId
	 * @param sourceIP
	 * @param deviceInfo
	 * @param srcTouchpointName
	 * @param srcTouchpointUrl
	 * @param refTouchpointUrl
	 * @param touchpointRefDomain
	 * @param eventName
	 * @return
	 */
	public static String recordEventFromWeb(Date createdAt, ContextSession ctxSession, String srcObserverId, String environment,String fingerprintId,
			String deviceId, String sourceIP, DeviceInfo deviceInfo, String srcTouchpointName, String srcTouchpointUrl,
			String refTouchpointUrl, String touchpointRefDomain, String eventName) {
		// saving 
		return saveTrackingEventFromWeb(createdAt, ctxSession, srcObserverId, environment, fingerprintId, deviceId, sourceIP, deviceInfo, srcTouchpointName, 
				srcTouchpointUrl, refTouchpointUrl, touchpointRefDomain, eventName, null, "", null, 0, null, null);
	}
	
	/**
	 * save event from Web: View Event Tracking or Action Event Tracking
	 * 
	 * @param createdAt
	 * @param ctxSession
	 * @param srcObserverId
	 * @param environment
	 * @param deviceId
	 * @param sourceIP
	 * @param deviceInfo
	 * @param srcTouchpointName
	 * @param srcTouchpointUrl
	 * @param refTouchpointUrl
	 * @param touchpointRefDomain
	 * @param eventName
	 * @param eventData
	 * @return
	 */
	public static String recordEventFromWeb(Date createdAt, ContextSession ctxSession, String srcObserverId, String environment,String fingerprintId,
			String deviceId, String sourceIP, DeviceInfo deviceInfo, String srcTouchpointName, String srcTouchpointUrl,
			String refTouchpointUrl, String touchpointRefDomain, String eventName, Map<String, Object> eventData) {
		// saving 
		return saveTrackingEventFromWeb(createdAt, ctxSession, srcObserverId, environment, fingerprintId, deviceId, sourceIP, deviceInfo, srcTouchpointName, 
				srcTouchpointUrl, refTouchpointUrl, touchpointRefDomain, eventName, eventData, "", null, 0, null, null);
	}
	

	
	/**
	 * save event from Web: Conversion Event Tracking
	 * 
	 * @param createdAt
	 * @param ctxSession
	 * @param srcObserverId
	 * @param environment
	 * @param deviceId
	 * @param sourceIP
	 * @param deviceInfo
	 * @param srcTouchpointName
	 * @param srcTouchpointUrl
	 * @param refTouchpointUrl
	 * @param touchpointRefDomain
	 * @param eventName
	 * @param eventData
	 * @param transactionId
	 * @param purchasedItems
	 * @param totalTransactionValue
	 * @param currencyCode
	 * @return
	 */
	public static String recordConversionFromWeb(Date createdAt, ContextSession ctxSession, String srcObserverId, String environment,String fingerprintId,
			String deviceId, String sourceIP, DeviceInfo deviceInfo, String srcTouchpointName, String srcTouchpointUrl,
			String refTouchpointUrl, String touchpointRefDomain, String eventName, Map<String, Object> eventData, 
			String transactionId, Set<OrderedItem> orderedItems, double totalTransactionValue, String currencyCode) {
		// saving 
		return saveTrackingEventFromWeb(createdAt, ctxSession, srcObserverId, environment, fingerprintId, deviceId, sourceIP, deviceInfo, srcTouchpointName, 
				srcTouchpointUrl, refTouchpointUrl, touchpointRefDomain, eventName, eventData, transactionId, orderedItems, totalTransactionValue, currencyCode, null);
	}
	

	/**
	 * @param observerId
	 * @param createdAt
	 * @param profile
	 * @param journeyMapId
	 * @param environment
	 * @param sourceIP
	 * @param touchpointType
	 * @param srcTouchpointName
	 * @param srcTouchpointUrl
	 * @param refTouchpointUrl
	 * @param touchpointRefDomain
	 * @param eventName
	 * @param eventData
	 * @return
	 */
	public static String saveEventFromApi(Profile profile, String observerId,  String fingerprintId, Date createdAt,  String journeyMapId, String environment, 
			String sourceIP, int touchpointType, String srcTouchpointName, String srcTouchpointUrl,
			String refTouchpointUrl, String touchpointRefDomain, String eventName, Map<String, Object> eventData) {
		return saveEventFromApi(profile, observerId, fingerprintId, createdAt,    journeyMapId,  environment, 
				 sourceIP, null,  touchpointType,  srcTouchpointName,  srcTouchpointUrl,
				 refTouchpointUrl,  touchpointRefDomain,  eventName, "",  eventData, null,  null, -1, null, null);
	}
	
	/**
	 * @param observerId
	 * @param createdAt
	 * @param profile
	 * @param journeyMapId
	 * @param environment
	 * @param sourceIP
	 * @param touchpointType
	 * @param srcTouchpointName
	 * @param srcTouchpointUrl
	 * @param refTouchpointUrl
	 * @param touchpointRefDomain
	 * @param eventName
	 * @param message
	 * @param eventData
	 * @return
	 */
	public static String saveEventFromApi(Profile profile,  String observerId,  String fingerprintId, Date createdAt, String journeyMapId, String environment, 
			String sourceIP, int touchpointType, String srcTouchpointName, String srcTouchpointUrl,
			String refTouchpointUrl, String touchpointRefDomain, String eventName, String message, Map<String, Object> eventData) {
		return saveEventFromApi(profile, observerId, fingerprintId,  createdAt,   journeyMapId,  environment, 
				 sourceIP, null,  touchpointType,  srcTouchpointName,  srcTouchpointUrl,
				 refTouchpointUrl,  touchpointRefDomain,  eventName, message, eventData, null,null, -1,  null, null);
	}


	/**
	 * to create event for a profile from CDP API
	 * 
	 * @param observerId
	 * @param createdAt
	 * @param profile
	 * @param journeyMapId
	 * @param environment
	 * @param sourceIP
	 * @param touchpointType
	 * @param srcTouchpointName
	 * @param srcTouchpointUrl
	 * @param refTouchpointUrl
	 * @param touchpointRefDomain
	 * @param eventName
	 * @param message
	 * @param eventData
	 * @param rawJsonData
	 * @param transaction
	 * @param ratingScore
	 * @param imageUrls
	 * @param videoUrls
	 * @return
	 */
	public static String saveEventFromApi(Profile profile, String observerId, String fingerprintId, Date createdAt, String journeyMapId, String environment, 
			String sourceIP, Device userDevice, int touchpointType, String srcTouchpointName, String srcTouchpointUrl,
			String refTouchpointUrl, String touchpointRefDomain, String eventName, String message, 
			Map<String, Object> eventdata, String rawJsonData, OrderTransaction transaction, int ratingScore, 
			String imageUrls, String videoUrls) {
		if(profile == null) {
			throw new InvalidDataException("profile is NULL, can not saveEventFromApi");
		}
		String srcObserverId = validateObserverId(observerId);

		// this could be computed at the Redis
		long eventCount = 1L;

		// touch-point info process
		Touchpoint refTouchPoint = TouchpointManagement.getOrCreate(touchpointRefDomain, touchpointType, refTouchpointUrl, true);
		Touchpoint srcTouchpoint = TouchpointManagement.getOrCreateNew(srcTouchpointName, touchpointType, srcTouchpointUrl);
		String refTouchpointId = refTouchPoint.getId();
		String srcTouchpointId = srcTouchpoint.getId();
		
		
		// profile
		String refProfileId = profile.getId();
		int refProfileType = profile.getType();
		
		// create event model
		TrackingEvent event = new TrackingEvent(journeyMapId, srcObserverId, eventName, eventCount, refProfileId,
				refProfileType, srcTouchpointId, refTouchpointId, sourceIP, createdAt, userDevice);
		event.setMessage(message);
		event.setEnvironment(environment);
		event.setEventData(eventdata);
		event.setImageUrls(imageUrls);
		event.setVideoUrls(videoUrls);
		event.setRawJsonData(rawJsonData);
		event.setFingerprintId(fingerprintId);
		
		// check is conversion event from database
		if(transaction != null) {
			event.setOrderTransaction(true, transaction);
		}
		
		// feedback event
		FeedbackEvent feedbackEvent = null;		
		if(ratingScore >= 0) {
			EventMetric metric = EventMetricManagement.getEventMetricByName(eventName);
			if(metric.isScoreModelForCX()) {
				feedbackEvent = new FeedbackEvent(eventName, createdAt, refProfileId, ratingScore, srcTouchpoint);
				event.setRefFeedbackEvent(feedbackEvent);
			}
			else {
				System.err.println(metric + " is not a event metric for CX or feedback");
			}
		}
		
		// exec command to create tracking event
		UpdateProfileEvent u = new UpdateProfileEvent(refProfileId, srcObserverId, touchpointRefDomain, event, feedbackEvent);
		ProfileDataManagement.updateProfileByEvent(u);
		return event.getId();
	}
	


	/**
	 *  save event from Web
	 * 
	 * @param createdAt
	 * @param ctxSession
	 * @param srcObserverId
	 * @param environment
	 * @param fingerprintId
	 * @param deviceId
	 * @param sourceIP
	 * @param deviceInfo
	 * @param srcTouchpointName
	 * @param srcTouchpointUrl
	 * @param refTouchpointUrl
	 * @param touchpointRefDomain
	 * @param eventName
	 * @param eventData
	 * @param transactionId
	 * @param orderedItems
	 * @param totalTransactionValue
	 * @param currencyCode
	 * @param feedbackEvent
	 * @return
	 */
	protected static String saveTrackingEventFromWeb(Date createdAt, ContextSession ctxSession, String srcObserverId, String environment, String fingerprintId,
			String deviceId, String sourceIP, DeviceInfo deviceInfo, String srcTouchpointName, String srcTouchpointUrl,
			String refTouchpointUrl, String touchpointRefDomain, String eventName, Map<String, Object> eventData, 
			String transactionId, Set<OrderedItem> orderedItems, double totalTransactionValue, String currencyCode, FeedbackEvent feedbackEvent) {
				
		String deviceName = deviceInfo.deviceName;
		String deviceOS = deviceInfo.deviceOs;
		String browserName = deviceInfo.browserName;
		String deviceType = deviceInfo.deviceType;
		Device userDevice = DeviceInfoUtil.getUserDevice(deviceInfo);
		DeviceDaoUtil.save(userDevice);
		
		// owned media has data from itself
		boolean isFromOwnedMedia = ctxSession.getMediaHost().equalsIgnoreCase(touchpointRefDomain);
		int refProfileType = ctxSession.getProfileType();
		String refProfileId = ctxSession.getProfileId();
		String sessionKey = ctxSession.getSessionKey();
		String refVisitorId = ctxSession.getVisitorId();

		// touch-point info process
		TouchpointHub hub = TouchpointHubManagement.getByObserverId(srcObserverId);
		int touchpointType = (hub != null) ? hub.getType() : TouchpointType.WEBSITE;
		Touchpoint refTouchpoint = TouchpointManagement.getOrCreate(touchpointRefDomain, touchpointType, refTouchpointUrl, isFromOwnedMedia);
		Touchpoint srcTouchpoint = TouchpointManagement.getOrCreateNew(srcTouchpointName, touchpointType, srcTouchpointUrl);
		
//		System.out.println("refTouchPoint " + refTouchPoint);
//		System.out.println("srcTouchpoint " + srcTouchpoint);
				
		boolean isConversion = EventMetricManagement.getEventMetricByName(eventName).isScoreModelForCLV();
		long eventCount = 1L; // this could be computed at the Redis

		TrackingEvent trackingEvent = new TrackingEvent(srcObserverId, sessionKey, eventName, eventCount, refProfileId, refVisitorId,
				refProfileType, hub, srcTouchpoint, refTouchpoint, browserName, fingerprintId, deviceId, deviceOS, deviceName,
				deviceType, sourceIP, createdAt);
		
		if(isConversion) {
			// set purchased items 
			trackingEvent.setOrderedItems(orderedItems);
			trackingEvent.setTransactionId(transactionId);
			trackingEvent.setTransactionValue(totalTransactionValue);
			trackingEvent.setTransactionCurrency(currencyCode);
		}
		
		if(feedbackEvent != null) {
			trackingEvent.setRefFeedbackEventId(feedbackEvent.getId());	
			trackingEvent.setMessage(feedbackEvent.getComment());
		}
		trackingEvent.setConversion(isConversion);
		trackingEvent.setEnvironment(environment);
		trackingEvent.setEventData(eventData);
		
		UpdateProfileEvent e = new UpdateProfileEvent(deviceId, sessionKey, refProfileId, srcObserverId, touchpointRefDomain, trackingEvent, feedbackEvent);
		ProfileDataManagement.updateProfileByEvent(e);
		return trackingEvent.getId();
	}

	
	/**
	 * @param createdAt
	 * @param ctxSession
	 * @param srcObserverId
	 * @param environment
	 * @param deviceId
	 * @param sourceIP
	 * @param deviceInfo
	 * @param srcTouchpointName
	 * @param srcTouchpointUrl
	 * @param refTouchpointUrl
	 * @param touchpointRefDomain
	 * @param eventName
	 * @param feedbackEvent
	 * @return
	 */
	public static String recordFeedbackEvent(Date createdAt, ContextSession ctxSession, String environment, String fingerprintId, String deviceId, 
			String lastSeenIp, DeviceInfo device, String srcObserverId, String touchpointName, String touchpointUrl, FeedbackEvent feedbackEvent) {
		
		Touchpoint srcTouchpoint = TouchpointManagement.getOrCreateNew(touchpointName, TouchpointType.FEEDBACK_SURVEY, touchpointUrl);
		BasicContactData contactInfo = feedbackEvent.getBasicContactData();
		
		System.out.println(" OLD ctxSession " + ctxSession);
		// FIXME
		if( contactInfo.hasData() && feedbackEvent.isOnSharedDevices()) {
			fingerprintId = ""; // reset in
			// search database for {profileFirstName , profileLastName, profileEmail, profilePhone} before merge into current session profile
			Profile newProfile = ProfileDataManagement.updateOrCreateFromWebTouchpoint(srcObserverId, srcTouchpoint, lastSeenIp, deviceId, contactInfo);
			ctxSession.resetProfileKeys(newProfile);
			// save into database
			ContextSessionDaoUtil.create(ctxSession);
		}
		
		feedbackEvent.setRefProfileId(ctxSession.getProfileId());
		feedbackEvent.setRefVisitorId(ctxSession.getVisitorId());
		feedbackEvent.setDeviceId(deviceId);
		
		// where is the touchpoint for customer give feedback 	
		feedbackEvent.setTouchpointId( srcTouchpoint.getId());
		feedbackEvent.setTouchpointName(touchpointName);
		feedbackEvent.setTouchpointUrl(touchpointUrl);
		feedbackEvent.setRefTouchpointHubId(srcTouchpoint.getParentId());
	
		// hash and get the feedback event ID
		String fbeId = feedbackEvent.buildHashedId();
		
		if(fbeId != null) {
			String eventName = feedbackEvent.getEventName();
			saveTrackingEventFromWeb(createdAt, ctxSession, srcObserverId, environment, fingerprintId, deviceId, lastSeenIp, device, touchpointName, touchpointUrl, 
					"", "", eventName, null, "", null, 0, null, feedbackEvent);
			return fbeId;
		} 
		
		return null;
	}
	
	
	/**
	 * @param obj
	 */
	private static void processInputDataSourceObserver(EventObserver obj) {
		String dataSourceUrl = obj.getDataSourceUrl();
		String[] toks = dataSourceUrl.split("/");
		if(toks.length == 4) {
			int type = obj.getType();
			// redis
			if(type == TouchpointType.REDIS_DATA_SOURCE) {
				String [] hostAndPort = toks[2].split(":");
				String channelName = toks[3];
				
				if(hostAndPort.length == 2) {
					String host = hostAndPort[0];
					int port = StringUtil.safeParseInt(hostAndPort[1]);
					DataImporterManagement.startProfileImportingJob(host, port, channelName);
				} else {
					System.err.println("hostAndPort is not correct format, e.g: 127.0.0.1:6480 ");
				}
			}
			else {
				System.err.println("protocol is not correct format, e.g: redis:// or kafka:// ");
			}
		}
	}


	/**
	 * to get EventObserver by hostname
	 * 
	 * @param hostname
	 * @return EventObserver
	 */
	public static EventObserver getEventObserverByHostname(String hostname, boolean returnDefault){
		EventObserver obs = EventObserverDaoUtil.getEventObserversByHostname(hostname);
		if(obs != null) {
			return obs;
		}
		else if(returnDefault) {
			return getDefaultDataObserver();
		}
		return null;
	}
	
}
