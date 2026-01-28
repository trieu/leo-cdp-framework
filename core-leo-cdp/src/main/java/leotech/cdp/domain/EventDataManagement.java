package leotech.cdp.domain;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.dao.FeedbackDataDao;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.TrackingEventDao;
import leotech.cdp.domain.processor.UpdateProfileEventProcessor;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.analytics.ImportingEventCallback;
import leotech.cdp.model.analytics.OrderTransaction;
import leotech.cdp.model.analytics.OrderedItem;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.analytics.TrackingEventCsvData;
import leotech.cdp.model.analytics.UpdateProfileEvent;
import leotech.cdp.model.customer.Device;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.query.filters.DataFilter;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.CsvDataParser;
import leotech.system.model.DeviceInfo;
import leotech.system.model.ImportingResult;
import leotech.system.util.DeviceInfoUtil;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.LogUtil;
import leotech.system.util.UrlUtil;
import rfx.core.util.StringUtil;

/**
 * Tracking Event Data Management
 * 
 * @author tantrieuf31
 * @since 2022
 */
public class EventDataManagement {

	static Logger logger = LoggerFactory.getLogger(EventDataManagement.class);
	
	/**
	 * parse the imported CSV File
	 * 
	 * @param importFileUrl
	 * @param previewTop30
	 * @return
	 */
	public static List<TrackingEventCsvData> parseToPreviewTrackingEvents(String importFileUrl) {
		CsvDataParser<TrackingEventCsvData> parser = new CsvDataParser<TrackingEventCsvData>(importFileUrl, true) {
			@Override
			public TrackingEventCsvData createObjectFromCsvRow(String[] headers, String[] csvDataRow) {
				return new TrackingEventCsvData(csvDataRow);
			}
		};
		return parser.parseImportedCsvFile();
	}
	
	/**
	 * parse the imported CSV File
	 * 
	 * @param importFileUrl
	 * @param previewTop30
	 * @return List<TrackingEvent>
	 */
	public static List<TrackingEvent> parseImportedEventData(String importFileUrl) {
		CsvDataParser<TrackingEvent> parser = new CsvDataParser<TrackingEvent>(importFileUrl, false) {
			@Override
			public TrackingEvent createObjectFromCsvRow(String[] headers, String[] csvDataRow) {
				return createEventFromCsvData(headers, csvDataRow);
			}
		};
		return parser.parseImportedCsvFile();
	}
	
	/**
	 * @param importFileUrl
	 * @return ImportingResult
	 */
	public static ImportingResult importFromCsvAndSaveEvents(String importFileUrl) {
		return importFromCsvAndSaveEvents(importFileUrl, null);
	}


	/**
	 * @param importFileUrl
	 * @param callback
	 * @return ImportingResult
	 */
	public static ImportingResult importFromCsvAndSaveEvents(String importFileUrl, ImportingEventCallback callback) {
		int okCount = 0;
		int failCount = 0;
		logger.info(" importFromCsvAndSaveEvents importFileUrl= " + importFileUrl);
		if (StringUtil.isNotEmpty(importFileUrl)) {
			// parse CSV file into profile list
			List<TrackingEvent> parsedEvents = parseImportedEventData(importFileUrl);
			Function<TrackingEvent, UpdateProfileEvent> mapper = e -> {
				if(e != null) {
					TrackingEvent event;
					if(callback != null) {
						callback.setUpdateProfileEvent(e);
						event = callback.apply();
					}
					else {
						event = e;
					}
					FeedbackEvent fbe = null;
					if(event.isExperience()) {
						fbe = new FeedbackEvent(event);
						event.setRefFeedbackEventId(fbe.getId());
					}								
					return new UpdateProfileEvent(event.getDeviceId(), event, fbe);
				}
				return null;
			};
			List<UpdateProfileEvent> updateEvents = parsedEvents.stream().map(mapper).filter( e -> { 
				return e != null; 
			}).collect(Collectors.toList());
			
			// update profile and save events
			okCount = UpdateProfileEventProcessor.processEvents(updateEvents);
			if(okCount > 0) {
				failCount = parsedEvents.size() - okCount;
			}
			else {
				failCount = parsedEvents.size();
			}
			
		}
		else {
			throw new InvalidDataException("Not found any file to import events at " + importFileUrl);
		}
		return new ImportingResult(okCount, failCount);
	}
	
	/**
	 * to convert CSV data to TrackingEvent 
	 * 
	 * @param headers
	 * @param dataRow
	 * @return
	 */
	public static TrackingEvent createEventFromCsvData(String[] headers, String[] dataRow) {
		int csvDataRowLen = dataRow.length;
		if(csvDataRowLen == TrackingEventCsvData.EVENT_CSV_DATA_LENGHT) {
			String profileId = StringUtil.safeString(dataRow[0], "_");
			String crmRefId = StringUtil.safeString(dataRow[1], "_");
			String primaryEmail = StringUtil.safeString(dataRow[2], "_");
			String primaryPhone = StringUtil.safeString(dataRow[3], "_");
			
			String observerId = StringUtil.safeString(dataRow[4]);
			String journeyMapId =  StringUtil.safeString(dataRow[5], JourneyMap.DEFAULT_JOURNEY_MAP_ID);
			int fraudScore =  StringUtil.safeParseInt(dataRow[6]);
			String eventTime = dataRow[7];
			String eventName = dataRow[8];
			long eventValue = StringUtil.safeParseLong(dataRow[9]);
			String message = dataRow[10];
			
			int timespent = StringUtil.safeParseInt(dataRow[11]);
			String transactionID = dataRow[12];
			double transactionValue = StringUtil.safeParseDouble(dataRow[13]);
			String transactionCurrency = dataRow[14];
			String locationName = dataRow[15];
			String locationCode = dataRow[16];
			
			String sourceTouchpointURL = dataRow[17];
			String sourceTouchpointName = dataRow[18];
			int sourceTouchpointType = StringUtil.safeParseInt(dataRow[19]);
			
			String referralTouchpointURL = dataRow[20];
			String referralTouchpointName = dataRow[21];
			int referralTouchpointType = StringUtil.safeParseInt(dataRow[22]);
			
			String deviceUserAgent = dataRow[23];
			String sourceIP = dataRow[24];
			String userSession = dataRow[25];
			
			String referralCampaignID = dataRow[26];
			String referralContentID = dataRow[27];
			String referralItemID = dataRow[28];
			String refDataSource = dataRow[29];
			String referralTicketID = dataRow[30];
			
			String imageUrls = dataRow[31];
			String videoUrls = dataRow[32];
			String eventDataJson = dataRow[33];
			String orderedItemsJson = dataRow[34];
			
			String dbProfileId = ProfileDaoUtil.getProfileIdByPrimaryKeys(profileId, crmRefId, primaryEmail, primaryPhone);
						
			LogUtil.logInfo(EventDataManagement.class, "getProfileIdByPrimaryKeys = "+ dbProfileId);
			
			if(StringUtil.isNotEmpty(dbProfileId)) {				
				TrackingEvent e = toTrackingEvent(observerId, journeyMapId, fraudScore, eventTime, eventName,
						eventValue, message, timespent, transactionID, transactionValue, transactionCurrency,
						locationName, locationCode, sourceTouchpointURL, sourceTouchpointName, sourceTouchpointType,
						referralTouchpointURL, referralTouchpointName, referralTouchpointType, deviceUserAgent,
						sourceIP, userSession, referralCampaignID, referralContentID, referralItemID, refDataSource,
						referralTicketID, imageUrls, videoUrls, eventDataJson, orderedItemsJson, dbProfileId);
				return e;
			}
		}
		return null;
	}

	static TrackingEvent toTrackingEvent(String eventObserverId,String journeyMapId, int fraudScore, String eventTime, String eventName, 
			long eventValue, String message, int timespent, String transactionID,
			double transactionValue, String transactionCurrency, String locationName, String locationCode,
			String sourceTouchpointURL, String sourceTouchpointName, int sourceTouchpointType,
			String referralTouchpointURL, String referralTouchpointName, int referralTouchpointType,
			String deviceUserAgent, String sourceIP, String userSession, String referralCampaignID,
			String referralContentID, String referralItemID, String refDataSource, String referralTicketID,
			String imageUrls, String videoUrls, String eventDataJson, String orderedItemsJson,
			String dbProfileId) {
		
		EventMetric eventMetric = EventMetricManagement.getEventMetricByName(eventName);
		// must be a valid event with metadata
		if(eventMetric != null) {
			int journeyStage = eventMetric.getJourneyStage();
			
			Date createdAt = Date.from(Instant.parse(eventTime));
			boolean isConversionEvent = eventMetric.isScoringForCLV();
			boolean isExperienceEvent = eventMetric.isScoreModelForCX();
			
			Touchpoint sourceTouchpoint = TouchpointManagement.importTouchpoint(sourceTouchpointURL, sourceTouchpointName, sourceTouchpointType);
			Touchpoint referralTouchpoint = TouchpointManagement.importTouchpoint(referralTouchpointURL, referralTouchpointName, referralTouchpointType);
			
			String srcTouchpointHubId = "", refTouchpointHubId = "";
			// make sure data is correct
			if(StringUtil.isEmpty(eventObserverId) && StringUtil.isNotEmpty(sourceTouchpointURL)) {
				String hostname = UrlUtil.getHostName(sourceTouchpointURL);
				EventObserver observer = EventObserverManagement.getEventObserverByHostname(hostname, true);
				srcTouchpointHubId = observer.getTouchpointHubId();
				eventObserverId = observer.getId();
				journeyMapId = observer.getJourneyMapId();
			}
			else {
				EventObserver observer = EventObserverManagement.getDefaultDataObserver();
				srcTouchpointHubId = observer.getTouchpointHubId();
				eventObserverId = observer.getId();
			}
			
			// parse JSON 		
			Map<String, Object> eventData = HttpWebParamUtil.parseJsonToGetHashMap(eventDataJson);
			Set<OrderedItem> orderedItems = OrderTransaction.parseJsonToGetOrderedItems(orderedItemsJson);
			
			DeviceInfo deviceInfo = DeviceInfoUtil.getDeviceInfo(deviceUserAgent);
			Device userDevice = DeviceInfoUtil.getUserDevice(deviceInfo);
			
			// create event model
			TrackingEvent e = new TrackingEvent(journeyMapId, journeyStage, eventObserverId, dbProfileId, eventName, eventValue, createdAt);
			e.setConversion(isConversionEvent);
			e.setExperience(isExperienceEvent);
			
			// action data
			e.setMessage(message);
			e.setTimeSpent(timespent);
			e.setSourceIP(sourceIP);
			e.setSessionKey(userSession);
			e.setFraudScore(fraudScore);
			
			// source touchpoint
			e.setSrcTouchpointHubId(srcTouchpointHubId);
			e.setSrcTouchpointId(sourceTouchpoint.getId());
			e.setSrcTouchpointUrl(sourceTouchpoint.getUrl());
			e.setSrcTouchpointName(sourceTouchpoint.getName());
			
			// referrer touchpoint
			e.setRefTouchpointHubId(refTouchpointHubId);
			e.setRefTouchpointId(referralTouchpoint.getId());
			e.setRefTouchpointUrl(referralTouchpoint.getUrl());
			e.setRefTouchpointName(referralTouchpoint.getName());
			
			// location
			e.setLocationCode(locationCode);
			e.setLocationName(locationName);
			
			// transaction 
			e.setTransactionId(transactionID);
			e.setTransactionValue(transactionValue);
			e.setTransactionCurrency(transactionCurrency);
			
			// device
			e.setDeviceId(userDevice.getId());
			e.setDeviceName(deviceInfo.deviceName);
			e.setDeviceOS(deviceInfo.deviceOs);
			e.setDeviceType(deviceInfo.deviceType);
			e.setBrowserName(deviceInfo.browserName);
			
			// meta ref keys
			e.setRefCampaignId(referralCampaignID);
			e.setRefContentId(referralContentID);
			e.setRefDataSource(refDataSource);
			e.setRefItemId(referralItemID);
			e.setRefTicketId(referralTicketID);
			
			// media
			e.setImageUrls(imageUrls);
			e.setVideoUrls(videoUrls);
			
			// transaction
			e.setOrderedItems(orderedItems);
			
			// ext attributes
			e.setEventData(eventData);
			return e;
		}
		return null;
	}
	
	/**
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<TrackingEvent> getTrackingEventsByPagination(int startIndex, int numberResult) {
		return TrackingEventDao.getTrackingEventsByPagination(startIndex, numberResult);
	}
	
	/**
	 * Feedback Event Activities
	 * 
	 * @param profileId
	 * @param startIndex
	 * @param numberResults
	 * @return
	 */
	public static List<FeedbackEvent> getFeedbackEventFlowOfProfile(String profileId, int startIndex,int numberResults) { 
		List<FeedbackEvent> eventActivities = FeedbackDataDao.getFeedbackEventsByProfileId(profileId,startIndex, numberResults);
		return eventActivities;
	}
	
	
	/**
	 * Unprocessed Event Data for single-view profile analytics
	 * 
	 * @param profileId
	 * @param startIndex
	 * @param numberResults
	 * @return
	 */
	public static List<TrackingEvent> getUnprocessedEventsOfProfile(String profileId, int startIndex,int numberResults) {
		List<TrackingEvent> eventActivities = TrackingEventDao.getUnprocessedEventsByProfileId(profileId,new DataFilter(startIndex, numberResults));
		return eventActivities;
	}


	/**
	 * @param profileId
	 * @param searchValue
	 * @param startIndex
	 * @param numberResults
	 * @return
	 */
	public static List<TrackingEvent> getTrackingEventsOfProfile(String profileId, String journeyMapId, String searchValue, int startIndex,int numberResults) {
		DataFilter filter = new DataFilter(journeyMapId, searchValue, startIndex, numberResults);
		List<TrackingEvent> eventActivities = TrackingEventDao.getEventsByProfileIdAndDataFilter(profileId, filter);
		return eventActivities;
	}
	
	/**
	 * @param profileId
	 * @param startIndex
	 * @param numberResults
	 * @return
	 */
	public static List<TrackingEvent> getTrackingEventsOfProfile(String profileId, String journeyMapId, int startIndex,int numberResults) {
		DataFilter filter = new DataFilter(journeyMapId, "", startIndex, numberResults);
		List<TrackingEvent> eventActivities = TrackingEventDao.getEventsByProfileIdAndDataFilter(profileId, filter);
		return eventActivities;
	}
	
	/**
	 * @param profileId
	 * @param startIndex
	 * @param numberResults
	 * @return
	 */
	public static List<TrackingEvent> getProductViewActivityFlowOfProfile(String profileId, int startIndex,int numberResults) {
		// Product Event Activities
		DataFilter filter = new DataFilter(startIndex, numberResults);
		List<TrackingEvent> eventActivities = TrackingEventDao.getEventsByProfileIdAndMetricName(profileId,BehavioralEvent.General.ITEM_VIEW,filter);
		return eventActivities;
	}
}