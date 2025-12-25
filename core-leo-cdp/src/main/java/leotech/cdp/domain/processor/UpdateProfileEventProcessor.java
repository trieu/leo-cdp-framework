package leotech.cdp.domain.processor;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.dao.AssetContentDaoUtil;
import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.dao.FeedbackDataDao;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.TrackingEventDao;
import leotech.cdp.dao.graph.GraphProfile2TouchpointHub;
import leotech.cdp.domain.AssetItemManagement;
import leotech.cdp.domain.EventDataManagement;
import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.domain.FeedbackDataManagement;
import leotech.cdp.domain.JourneyMapManagement;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.ProfileGraphManagement;
import leotech.cdp.domain.TouchpointHubManagement;
import leotech.cdp.domain.TouchpointManagement;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.domain.schema.FunnelMetaData;
import leotech.cdp.domain.scoring.ProcessorScoreCX;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.analytics.FeedbackSurveyReport;
import leotech.cdp.model.analytics.OrderedItem;
import leotech.cdp.model.analytics.ScoreCX;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.analytics.UpdateProfileEvent;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.BasicContactData;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.model.journey.DataFlowStage;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.JourneyMapRefKey;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.model.journey.TouchpointType;
import leotech.system.util.LogUtil;
import leotech.system.util.TaskRunner;
import leotech.system.util.XssFilterUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.StringUtil;

/**
 * the Processor for UpdateProfileEvent 
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
/**
 * @author mac
 *
 */
public final class UpdateProfileEventProcessor {
	
	static Logger logger = LoggerFactory.getLogger(UpdateProfileEventProcessor.class);
	
	// const to update profile from event stream
	static final String mergeStrategy = SystemMetaData.PROFILE_MERGE_STRATEGY;
	static final String FIRST_NAME = "firstName";
	static final String LAST_NAME = "lastName";
	static final String EMAIL = "email";
	static final String PHONE = "phone";
	static final String ECOMMERCE_USER_ID = "ecommerceUserId";
	static final String DATA_LABELS = "dataLabels";
	
	public static int processEvent(UpdateProfileEvent updateEvent) {
		return processEvents(Arrays.asList(updateEvent));
	}

	/**
	 * @param updateEvent
	 */
	public static int processEvents(List<UpdateProfileEvent> updateEvents) {
		int result = 0;
		for (UpdateProfileEvent updateEvent : updateEvents) {
			boolean ok = false;
			
			// who see the event data
			String observerId = updateEvent.getObserverId();
			EventObserver eventObserver = EventObserverManagement.getById(observerId);
			TouchpointHub tpHub = TouchpointHubManagement.getByObserverId(observerId);
			
			// TrackingEvent
			TrackingEvent trackingEvent = updateEvent.getEvent();
			if(trackingEvent == null) {
				logger.error("trackingEvent is NULL ");
				return 0;
			}
			
			// EventMetric
			String metricName = trackingEvent.getMetricName();
			EventMetric eventMetric = EventMetricManagement.getEventMetricByName(metricName);
			if(eventMetric == null) {
				logger.error("eventMetric is NULL ");
				return 0;
			}
			
			String srcTouchpointId = trackingEvent.getSrcTouchpointId();
			String profileId = trackingEvent.getRefProfileId();
			String refVisitorId = trackingEvent.getRefVisitorId();
			String sourceIP = trackingEvent.getSourceIP();
			String fingerprintId = trackingEvent.getFingerprintId();
			String deviceId = trackingEvent.getDeviceId();
			
			// init Touchpoint that profile is tracked
			Touchpoint srcTouchpoint = TouchpointManagement.getById(srcTouchpointId);
			if (srcTouchpoint == null) {
				srcTouchpoint = TouchpointManagement.getOrCreate(trackingEvent.getSrcTouchpointName(), TouchpointType.WEB_APP, trackingEvent.getSrcTouchpointUrl(), true);
			}
	
			// init ProfileSingleView
			ProfileSingleView profile = ProfileDaoUtil.getProfileById(profileId);
			if (profile != null) {
				// add web session key
				String sessionKey = updateEvent.getSessionKey();
				if(StringUtil.isNotEmpty(sessionKey)) {
					profile.setContextSessionKey(sessionKey, trackingEvent.getCreatedAt());
				}
				
				// update profile
				profile.engageAtTouchpoint(srcTouchpoint, true);
				profile.setFingerprintId(fingerprintId);
				profile.setLastUsedDeviceId(deviceId);
				profile.setLastSeenIp(sourceIP);
				profile.updateReferrerChannel(updateEvent.getTouchpointRefDomain());
				profile.setObserverAndTouchpointHub(observerId, tpHub);
				
				
				logger.info("UpdateProfileEventProcessor for profileId: " + profileId );
			}
			else {		
				// create profile
				profile = ProfileSingleView.newAnonymousProfile(observerId, srcTouchpoint, sourceIP, refVisitorId, deviceId, fingerprintId);
				profile.updateReferrerChannel(updateEvent.getTouchpointRefDomain());
				profile.setObserverAndTouchpointHub(observerId, tpHub);
				
				// add web session key
				String sessionKey = updateEvent.getSessionKey();
				if(StringUtil.isNotEmpty(sessionKey)) {
					profile.setContextSessionKey(sessionKey, trackingEvent.getCreatedAt());
				}
				
				// commit into database
				ProfileDaoUtil.insert(profile);
				logger.info("create new ProfileSingleView for profileId: " + profile.getId() );
				return 0;
			}
			
			// Journey Map
			String journeyId = trackingEvent.getRefJourneyId();
			if(StringUtil.isEmpty(journeyId)) {
				journeyId = JourneyMapManagement.getIdFromObserverOrTouchpoint(eventObserver, srcTouchpoint);
			}
			
			DataFlowStage funnelStage = eventMetric.getFunnelStage();
			// feedback event
			if(updateEvent.isFeedbackEvent()) {		
				FeedbackEvent feedbackEvent = updateEvent.getFeedbackEvent();
				
				BasicContactData contact = feedbackEvent.getBasicContactData();
				if(contact.hasData()) {
					profile.setFirstName(contact.firstName);
					profile.setLastName(contact.lastName);
					profile.setEmail(contact.email);
					profile.setPhone(contact.phone);
				}				
				
				ok = processFeedbackEvent(eventObserver, feedbackEvent, trackingEvent, metricName, eventMetric, funnelStage, profile, journeyId); 	
			}
			else {
				ok = processTrackingEvent(eventObserver, trackingEvent, eventMetric, funnelStage, profile, journeyId);
			}
			
			if(profile != null) {
				ProfileSingleView updateProfile = profile;
				TaskRunner.run(()->{
					// update Profile and tracking event into database
					ProfileDataManagement.updateProfileFromEvents(updateProfile, trackingEvent);
				});
			}
	
			if(ok) {
				result++;
			}
		}
		return result;
	}

	/**
	 * process a single tracking event. E.g: page-view, purchase, click-details,...
	 * 
	 * @param eventObserver
	 * @param trackingEvent
	 * @param eventMetric
	 * @param funnelStage
	 * @param profile
	 * @param journeyId
	 * @return
	 */
	private static boolean processTrackingEvent(EventObserver eventObserver, TrackingEvent trackingEvent,
			EventMetric eventMetric, DataFlowStage funnelStage, ProfileSingleView profile, String journeyId) {
		// save tracking event
		trackingEvent.setRefJourneyId(journeyId);
		trackingEvent.setJourneyStage(eventMetric.getJourneyStage());
		
		// commit to database 
		boolean saveEventOk = TrackingEventDao.save(trackingEvent);
		
		// updating attributes from event
		if(SystemMetaData.isAutoMergeProfileData() && profile.isMergeableProfile()) {
			profile = mergeEventDataToProfile(profile, trackingEvent);
		}

		// update Behavioral Graph 
		updateBehavioralGraph(profile, trackingEvent, eventMetric);
		
		// inJourneyMap
		saveJourneyMapAndTouchpointHub(eventObserver, profile, journeyId, eventMetric, funnelStage, null);
		return saveEventOk;
	}


	/**
	 * @param feedbackEvent
	 * @param result
	 * @param trackingEvent
	 * @param metricName
	 * @param eventMetric
	 * @param funnelStage
	 * @param profile
	 * @param journeyId
	 * @return
	 */
	protected static boolean processFeedbackEvent(EventObserver eventObserver, FeedbackEvent feedbackEvent, TrackingEvent trackingEvent, String metricName, EventMetric eventMetric, DataFlowStage funnelStage, ProfileSingleView profile, String journeyId) {
	
		int feedbackScore = feedbackEvent.getFeedbackScoreInteger();
		
		Map<String, Object> submitProfileAttributes = feedbackEvent.getUpdatingProfileAttributes();
		
		ScoreCX cxScore = null;
		Map<String, Object> eventData = new HashMap<>();
		
		if (BehavioralEvent.Feedback.SUBMIT_FEEDBACK_FORM.equals(metricName)) {
			// Survey Templates: handle code when profile submit a survey form
			FeedbackSurveyReport report = FeedbackDataManagement.buildSurveyFeedbackReport(feedbackEvent);
			if(report != null) {						
				System.out.println("STR_SUBMIT_FEEDBACK_FORM. score " + cxScore);
				feedbackEvent.setFeedbackScore(report.getAvgFeedbackScore());
				
				eventData.put("Header", feedbackEvent.getHeader());
				eventData.put("Group", feedbackEvent.getGroup());
				eventData.put("Feedback Type", feedbackEvent.getFeedbackType());
				
				cxScore = computeAndUpdateCX(profile, journeyId, feedbackEvent, false);
				
				if(cxScore != null) {
					eventData.put("CX_Score_Sentiment", cxScore.getSentimentScore()+"");
					eventData.put("CX_Score_Positive", cxScore.getPositive()+"");
					eventData.put("CX_Score_Neutral", cxScore.getNeutral()+"");
					eventData.put("CX_Score_Negative", cxScore.getNegative()+"");
				}	
			}
		}
		else if(BehavioralEvent.General.SUBMIT_CONTACT.equals(metricName)) {
			submitProfileAttributes.forEach((k,v)->{
				String value = StringUtil.safeString(v, "");
				if( ! value.isBlank() ) {
					eventData.put(k, value);
				}
			});
		}
		else {
			// Rating HTML Forms: update profile with feedback data, get score
			cxScore = computeAndUpdateCX(profile, journeyId, feedbackEvent, false);
			
			if(cxScore != null) {
				eventData.put("CX_Score_Sentiment", cxScore.getSentimentScore()+"");
				eventData.put("CX_Score_Positive", cxScore.getPositive()+"");
				eventData.put("CX_Score_Neutral", cxScore.getNeutral()+"");
				eventData.put("CX_Score_Negative", cxScore.getNegative()+"");
				eventData.put("FeedbackEventID", feedbackEvent.getId());
				eventData.put("Header", feedbackEvent.getHeader());
				eventData.put("Group", feedbackEvent.getGroup());
				eventData.put("Feedback Score", feedbackScore+"");
				eventData.put("Feedback Type", feedbackEvent.getFeedbackType());
			}
		}
		
		// save feedbackEvent
		if(cxScore != null) {
			if(cxScore.hasScoreData()) {
				// inJourneyMap
				saveJourneyMapAndTouchpointHub(eventObserver, profile, journeyId, eventMetric, funnelStage, cxScore);
				feedbackEvent.setScoreCX(cxScore);
				String savedId = FeedbackDataDao.save(feedbackEvent);
				eventData.put("FeedbackEventID", savedId);
			}
		}
		
		// save tracking event
		trackingEvent.setMetricValue(feedbackScore);
		trackingEvent.setEventData(eventData);
		trackingEvent.setRefJourneyId(journeyId);
		trackingEvent.setJourneyStage(eventMetric.getJourneyStage());
		
		// commit to database
		boolean ok = TrackingEventDao.save(trackingEvent);
		
		// update data fields of profile
		if(profile.isMergeableProfile()) {
			profile.updateDataWithAttributeMap(submitProfileAttributes);
		}		
		return ok;
	}

	
	/**
	 * @param eventObserver
	 * @param profile
	 * @param updateJourneyId
	 * @param eventMetric
	 * @param funnelStage
	 * @param scoreCX
	 */
	public static void saveJourneyMapAndTouchpointHub(EventObserver eventObserver, ProfileSingleView profile, String updateJourneyId, EventMetric eventMetric, DataFlowStage funnelStage, ScoreCX scoreCX) {
		try {
			JourneyMap journeyMap = JourneyMapManagement.getById(updateJourneyId, false, true);
			
			// System.out.println(updateJourneyId + " updateJourneyMapForProfile.journeyMap " + journeyMap);
			
			JourneyMapRefKey journeyRef = profile.getOrCreateJourneyMapRefKey(journeyMap);
			int funnelIndex = profile.classifyFunnelStageIndex(funnelStage, journeyRef, scoreCX);
			//System.out.println(" classifyFunnelStageIndex = " + funnelIndex);
			
			// update current journey map
			int journeyStage = eventMetric.getJourneyStage();
			profile.setInJourneyMap(journeyMap, journeyStage, funnelIndex, scoreCX);
			logger.info("[setInJourneyMap]" + journeyMap.getName() + " journeyStage " + journeyStage + " eventMetric " + eventMetric.getEventName());
			
			// update touchpoint hub graph
			
			String tpHubId = eventObserver.getTouchpointHubId();
			GraphProfile2TouchpointHub.updateEdgeData(profile, tpHubId, updateJourneyId , eventMetric);
			
			// System.out.println("profile.getInJourneyMaps "+profile.getInJourneyMaps());
		} catch (Exception e) {
			// deleted journey map ? 
			profile.removeJourneyMap(updateJourneyId);
			e.printStackTrace();
		}
	}

	
	/**
	 * @param profile
	 * @param trackingEvent
	 * @return
	 */
	static ProfileSingleView mergeEventDataToProfile(ProfileSingleView profile, TrackingEvent trackingEvent) {
		Map<String, Object> map = trackingEvent.getEventData();
		String value = StringUtil.safeString(map.get(FIRST_NAME),"");
		if( StringUtil.isNotEmpty(value) ) {
			profile.setFirstName(XssFilterUtil.clean(value));
		}
		value = StringUtil.safeString(map.get(LAST_NAME),"");
		if( StringUtil.isNotEmpty(value) ) {
			profile.setLastName(XssFilterUtil.clean(value));
		}
		value = StringUtil.safeString(map.get(PHONE),"");
		if( StringUtil.isNotEmpty(value) ) {
			profile.setPhone(XssFilterUtil.clean(value));
		}
		value = StringUtil.safeString(map.get(EMAIL),"");
		if( StringUtil.isNotEmpty(value) ) {
			profile.setEmail(XssFilterUtil.clean(value));
		}
		value = StringUtil.safeString(map.get(ECOMMERCE_USER_ID),"");
		if( StringUtil.isNotEmpty(value) ) {
			value = ECOMMERCE_USER_ID + ":" + XssFilterUtil.clean(value);
			profile.setIdentities(value);
		}
		value = StringUtil.safeString(map.get(DATA_LABELS),"");
		if( StringUtil.isNotEmpty(value) ) {
			profile.setDataLabels(XssFilterUtil.clean(value), false);
		}
		return profile;
	}

	/**
	 * every profile has a customer graph, storing everything 
	 * 
	 * @param profile
	 * @param event
	 */
	static void updateBehavioralGraph(ProfileSingleView profile, TrackingEvent event, EventMetric eventMetric) {
		
		// product or service impression
		if(eventMetric.isLeadMetric()) {
			itemViewEventMetricHandler(profile, event, eventMetric);
		}
		// has an intent to do a transaction
		else if(eventMetric.isProspectiveMetric()) {
			prospectMetricHandler(profile, event, eventMetric);
		}
		// purchased or made-payment or subscribe
		else if(eventMetric.isConversion()) {
			customerMetricHandler(profile, event, eventMetric);
		}  
		else {
			System.out.println(" ==> Skip updateProfileCustomerGraph for event " + event.getMetricName());
		}
		// end
	}



	/**
	 * customer metric handler
	 * 
	 * @param profile
	 * @param event
	 * @param eventMetric
	 */
	protected static void customerMetricHandler(ProfileSingleView profile, TrackingEvent event, EventMetric eventMetric) {
		String transactionId = event.getTransactionId();
		Date createdAt = event.getCreatedAt();
		
		Set<OrderedItem> orderedItems = enrichShoppingItemsForTransactionalEvent(event);
		
		int updateCount = 0;
		for (OrderedItem orderedItem : orderedItems) {
			if(orderedItem.isTransactionalItem()) {				
				ProductItem pItem = AssetItemManagement.createOrUpdateProductItem(orderedItem);
				if(pItem != null) {
					double salePrice = pItem.getSalePrice();
					int quantity = orderedItem.getQuantity();
					String currency = orderedItem.getCurrency();
					double transactionValue = quantity * salePrice;
					
					profile.addTotalTransactionValue(transactionValue);
					profile.addPurchasedItem(orderedItem);
					
					// update conversion graph
					commitTransactionDataToDb(profile, eventMetric, transactionId, pItem, createdAt, quantity, currency, transactionValue);
					updateCount++;
				}
			}
		}
		
		
	}

	private static void commitTransactionDataToDb(ProfileSingleView profile, EventMetric eventMetric,
			String transactionId, ProductItem pItem, Date createdAt, int quantity, String currency,
			double transactionValue) {
		TaskRunner.run(()->{
			// save product item to default group
			AssetProductItemDaoUtil.save(pItem);
			// save profile graph
			ProfileGraphManagement.updateTransactionalData(createdAt, profile, pItem, quantity , eventMetric, transactionId, transactionValue, currency);
			LogUtil.logInfo(UpdateProfileEvent.class, "ProfileGraphManagement.updateTransactionalData itemId: " + pItem.getId() + " createdAt " + createdAt);
		});
	}
	

	/**
	 *  prospect event metric
	 * 
	 * @param profile
	 * @param event
	 * @param eventMetric
	 */
	protected static void prospectMetricHandler(ProfileSingleView profile, TrackingEvent event, EventMetric eventMetric) {
		String sessionKey = event.getSessionKey();
		Set<OrderedItem> cartItems = event.getOrderedItems();
		
		// loop to all shopping items in cart
		for (OrderedItem shoppingItem : cartItems) {
			String itemId = shoppingItem.getItemId();
			String itemIdType = shoppingItem.getIdType();
			
			if(StringUtil.isNotEmpty(itemId) && shoppingItem.isTransactionalItem() && StringUtil.isNotEmpty(itemIdType) ) {
				// check from internal database first
				ProductItem pItem = AssetProductItemDaoUtil.getByProductId(itemId, itemIdType);
				System.out.println(itemId + " getByProductId " + pItem);
				if(pItem != null) {
					System.out.println("marketingEventMetric for "+shoppingItem);
					
					// data enrichment
					shoppingItem.setSessionKey(sessionKey);
					shoppingItem.setFullUrl(pItem.getFullUrl());
					shoppingItem.setOriginalPrice(pItem.getOriginalPrice());
					shoppingItem.setSalePrice(pItem.getSalePrice());
					shoppingItem.setStoreId(pItem.getStoreIds());
					shoppingItem.setName(pItem.getTitle());
					shoppingItem.setImageUrl(pItem.getHeadlineImageUrl());
					shoppingItem.setVideoUrl(pItem.getHeadlineVideoUrl());
					shoppingItem.setCurrency(pItem.getPriceCurrency());
					
					// add event to profile graph edge collection and update indexScore of item 
					int score = -1 * DateTimeUtil.currentUnixTimestamp();
					ProfileGraphManagement.updateEdgeDataForRecommendation(event.getCreatedAt(), profile, pItem, eventMetric, score);
					
					profile.addShoppingItem(shoppingItem);
				}
			}
		}
		
	}
	

	/**
	 * view event metric
	 * 
	 * @param profile
	 * @param event
	 * @param eventMetric
	 */
	protected static void itemViewEventMetricHandler(ProfileSingleView profile, TrackingEvent event, EventMetric eventMetric) {
		
		Map<String, Object> eventData = event.getEventData();
		String contentId = StringUtil.safeString(eventData.get(TrackingEvent.CONTENT_ID),"");
		String productIds = StringUtil.safeString(eventData.get(TrackingEvent.PRODUCT_IDS),"");
		String itemIdType = StringUtil.safeString(eventData.get(TrackingEvent.ID_TYPE),"");

		if(StringUtil.isNotEmpty(productIds) && StringUtil.isNotEmpty(itemIdType)) {
			// check from internal database first
			ProductItem pdItem = AssetProductItemDaoUtil.getByProductId(productIds, itemIdType);
			System.out.println(pdItem);
			if(pdItem != null) {
				Date createdAt = event.getCreatedAt();
				profile.addShoppingItem(createdAt, pdItem, 1);
				
				// data enrichment
				String sessionKey = event.getSessionKey();
				String name = pdItem.getTitle();
				OrderedItem cartItem = new OrderedItem(createdAt, sessionKey, name, productIds, itemIdType, 1);
				cartItem.setFullUrl(pdItem.getFullUrl());
				cartItem.setOriginalPrice(pdItem.getOriginalPrice());
				cartItem.setSalePrice(pdItem.getSalePrice());
				cartItem.setStoreId(pdItem.getStoreIds());
				cartItem.setImageUrl(pdItem.getHeadlineImageUrl());
				cartItem.setVideoUrl(pdItem.getHeadlineVideoUrl());
				cartItem.setCurrency(pdItem.getPriceCurrency());
				
				// add event to profile graph edge collection and update indexScore of item
				int score = -1 * DateTimeUtil.currentUnixTimestamp() + 100000;
				ProfileGraphManagement.updateEdgeDataForRecommendation(createdAt, profile, pdItem, eventMetric, score);
				
				Set<OrderedItem> cartItems = new HashSet<>(1);
				cartItems.add(cartItem);
				profile.setShoppingItems(cartItems);
			}
		}
		else if(StringUtil.isNotEmpty(contentId)) {
			AssetContent content = AssetItemManagement.getContentItemById(contentId);
			if(content != null) {
				ProfileGraphManagement.updateEdgeContentData(profile.toProfileIdentity(), content);
			}
		}
	}
	
	/**
	 * @param profile
	 * @param event
	 * @param eventMetric
	 */
	protected static void shortLinkClickHandler(ProfileSingleView profile, TrackingEvent event) {
		// FIXME
		// recording an intent to read content
		String srcTouchpointId = event.getSrcTouchpointId();
		String contentId = null;
		Touchpoint tp = TouchpointManagement.getById(srcTouchpointId);
		if(tp != null) {
			contentId = tp.getAssetContentId();
			if(StringUtil.isEmpty(contentId)) {
				contentId = StringUtil.safeString(event.getEventData().get("contentId"),"");
			}
			AssetContent content = AssetContentDaoUtil.getById(contentId, true);
			if(content != null) {
				ProfileGraphManagement.updateEdgeContentData(profile.toProfileIdentity(), content);
			}
		}
	}


	
	/**
	 * @param profile
	 * @param event
	 * @return
	 */
	protected static Set<OrderedItem> enrichShoppingItemsForTransactionalEvent(TrackingEvent event) {
		double totalValue = event.getTransactionValue();
		
		Set<OrderedItem> orderedItems = event.getOrderedItems();
		Set<OrderedItem> purchasedItems = new HashSet<OrderedItem>(orderedItems.size());
		
		String sessionKey = event.getSessionKey();
		String currencyCode = event.getTransactionCurrency();
		
		for (OrderedItem shoppingItem : orderedItems) {
			if(shoppingItem.getSessionKey().equals(sessionKey)) {
				purchasedItems.add(shoppingItem);
				currencyCode = shoppingItem.getCurrency();
			}
		}
		event.setTransactionValue(totalValue);
		event.setTransactionCurrency(currencyCode);
		event.setOrderedItems(purchasedItems);
		return orderedItems;
	}
	
	
	/**
	 * update feedback scores (CFS,CES,CSAT,NPS) for profile
	 * 
	 * @param profile
	 * @param eventName
	 * @param fbEvent
	 * @param resetData
	 * @return
	 */
	final static ScoreCX computeAndUpdateCX(ProfileSingleView profile, String journeyId, FeedbackEvent fbEvent, boolean resetData) {
	
		ScoreCX score = null;
		int scoreNPS = 0;
		int totalCSAT = 0;
		int totalCFS = 0;
		int totalCES = 0;

		String eventName = fbEvent.getEventName(); 
		String dateKey = fbEvent.buildDateKey();

		double fbScoreDouble = fbEvent.getFeedbackScore();
		int fbScoreInt = fbEvent.getFeedbackScoreInteger();

		if (!resetData) {
			// CX
			totalCFS = profile.getTotalCFS();
			totalCES = profile.getTotalCES();
			totalCSAT = profile.getTotalCSAT();
			scoreNPS = profile.getTotalNPS();
		}

		EventMetric eventMetric = EventMetricManagement.getEventMetricByName(eventName);
		int scoreModel = eventMetric.getScoreModel();
		
		// CFS
		if (scoreModel == EventMetric.SCORING_FEEDBACK_METRIC) {
			int positiveCFS = profile.getPositiveCFS();
			int neutralCFS = profile.getNeutralCFS();
			int negativeCFS = profile.getNegativeCFS();
			
			// compute
			if(BehavioralEvent.Feedback.SUBMIT_FEEDBACK_FORM.equalsIgnoreCase(eventName)) {
				score = ProcessorScoreCX.computeScale5double(fbScoreDouble, positiveCFS, neutralCFS, negativeCFS);
			}
			else {
				score = ProcessorScoreCX.computeScale5integer(fbScoreInt, positiveCFS, neutralCFS, negativeCFS);
			}
			
			if(score != null) {
				profile.setNegativeCFS(score.getNegative());
				profile.setNeutralCFS(score.getNeutral());
				profile.setPositiveCFS(score.getPositive());
				profile.addTimeseriesCFS(dateKey, score);
				totalCFS += score.getSentimentScore();
			}
		} 
		// CES
		else if (scoreModel == EventMetric.SCORING_EFFORT_METRIC) {
			int positiveCES = profile.getPositiveCES();
			int neutralCES = profile.getNeutralCES();
			int negativeCES = profile.getNegativeCES();
			// compute
			score = ProcessorScoreCX.computeScale5integer(fbScoreInt, positiveCES, neutralCES, negativeCES);

			if(score != null) {
				profile.setNegativeCES(score.getNegative());
				profile.setNeutralCES(score.getNeutral());
				profile.setPositiveCES(score.getPositive());
				profile.addTimeseriesCES(dateKey, score);
				totalCES += score.getSentimentScore();
			}
		} 
		// CSAT
		else if (scoreModel == EventMetric.SCORING_SATISFACTION_METRIC) {

			int positiveCSAT = profile.getPositiveCSAT();
			int neutralCSAT = profile.getNeutralCSAT();
			int negativeCSAT = profile.getNegativeCSAT();
			// compute
			score = ProcessorScoreCX.computeScale5integer(fbScoreInt, positiveCSAT, neutralCSAT, negativeCSAT);
			
			if(score != null) {
				profile.setNegativeCSAT(score.getNegative());
				profile.setNeutralCSAT(score.getNeutral());
				profile.setPositiveCSAT(score.getPositive());
				profile.addTimeseriesCSAT(dateKey, score);
				totalCSAT += score.getSentimentScore();
			}
		}
		// NPS
		else if (scoreModel == EventMetric.SCORING_PROMOTER_METRIC) {
			int positiveNPS = profile.getPositiveNPS();
			int neutralNPS = profile.getNeutralNPS();
			int negativeNPS = profile.getNegativeNPS();
			// compute
			score = ProcessorScoreCX.computeScale10(fbScoreInt, positiveNPS, neutralNPS, negativeNPS);
			
			if(score != null) {
				profile.setNegativeNPS(score.getNegative());
				profile.setNeutralNPS(score.getNeutral());
				profile.setPositiveNPS(score.getPositive());
				profile.addDataTimeseriesNPS(dateKey, score);
				scoreNPS += score.getSentimentScore();
			}
		}

		// update database
		if (score != null) {
			// CX score
			profile.setTotalCFS(totalCFS);
			profile.setTotalCES(totalCES);
			profile.setTotalCSAT(totalCSAT);
			profile.setTotalNPS(scoreNPS);

			boolean isHappy = score.isHappy();

			// check for positive or negative CX
			EventMetric cxMetric = null;
			if (isHappy) {
				cxMetric = EventMetricManagement.getEventMetricByName(BehavioralEvent.Feedback.POSITIVE_FEEDBACK);
			} else {
				cxMetric = EventMetricManagement.getEventMetricByName(BehavioralEvent.Feedback.NEGATIVE_FEEDBACK);
			}

			// only customer, has contact  can send feedback at the happy or unhappy stage
			if (cxMetric != null && profile.hasContactData()) {
				// set CX stage for profile

				if (isHappy) {
					profile.setFunnelStage(FunnelMetaData.STAGE_HAPPY_CUSTOMER);
					profile.setJourneyScore(70);
				} else {
					profile.setFunnelStage(FunnelMetaData.STAGE_UNHAPPY_CUSTOMER);
					profile.setJourneyScore(90);
				}
			}
		}
		return score;
	}
	
	/**
	 * @param finalProfile
	 * @return
	 */
	public static ProfileSingleView recomputationFromEventStream(ProfileSingleView finalProfile) {
		int startIndex = 0;
		int numberResult = 200;
		String profileId = finalProfile.getId();
		List<TrackingEvent> events = EventDataManagement.getTrackingEventsOfProfile(profileId, "", startIndex, numberResult);
		
		// make sure it clear as empty map
		finalProfile.clearInJourneyMaps();
		
		while (!events.isEmpty()) {
			for (TrackingEvent event : events) {
				String eventName = event.getMetricName();
				// get metadata of event
				EventMetric eventMetric = EventMetricManagement.getEventMetricByName(eventName);
				
				// only event in defined funnel is processed
				if (eventMetric != null) {
					
					DataFlowStage funnelStage = eventMetric.getFunnelStage();
					
					String observerId = event.getObserverId();
					EventObserver eventObserver = EventObserverManagement.getById(observerId);
					
					String srcTouchpointId = event.getSrcTouchpointId();
					Touchpoint srcTouchpoint = TouchpointManagement.getById(srcTouchpointId);
					String journeyId = JourneyMapManagement.getIdFromObserverOrTouchpoint(eventObserver, srcTouchpoint);
					
					// save tracking event
					event.setRefJourneyId(journeyId);
					event.setJourneyStage(eventMetric.getJourneyStage());
					TrackingEventDao.save(event);
					
					saveJourneyMapAndTouchpointHub(eventObserver, finalProfile, journeyId, eventMetric, funnelStage, new ScoreCX(event));
				}
			}
			
			// loop to the end to reach all events of profile in database
			startIndex = startIndex + numberResult;

			// go to next page of events
			events = EventDataManagement.getTrackingEventsOfProfile(profileId, "", startIndex, numberResult);
		}
		return finalProfile;
	}
	
	

}
