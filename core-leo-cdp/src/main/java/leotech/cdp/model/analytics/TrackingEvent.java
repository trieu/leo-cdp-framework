package leotech.cdp.model.analytics;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.entity.Key;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.dao.EventObserverDaoUtil;
import leotech.cdp.domain.TouchpointManagement;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.SingleViewAnalyticalObject;
import leotech.cdp.model.customer.Device;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.GeoLocation;
import leotech.system.model.OsmGeoLocation;
import leotech.system.util.GeoLocationUtil;
import leotech.system.util.UrlUtil;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.RandomUtil;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 *  tracking data event, is collected by Leo Observer, the Truth of Universe <br>
 *  Record, Analysis, Update state
 * 
 * @author Trieu Nguyen
 * @since 2020
 *
 */
public final class TrackingEvent extends PersistentObject implements SingleViewAnalyticalObject {

	public static final String EMPTY_TRANSACTION = "empty-transaction";
	public static final String SEMICOLON = ";";
	public static final String PRODUCT_IDS = "productids";
	public static final String ID_TYPE = "idtype";
	public static final String CONTENT_ID = "contentid";
	

	public static final String COLLECTION_NAME = getCdpCollectionName(TrackingEvent.class);
	static ArangoCollection dbCollection;

	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			initCollectionAndIndex();
		}
		return dbCollection;
	}

	public static void initCollectionAndIndex() {
		dbCollection = getArangoDatabase().collection(COLLECTION_NAME);

		// ensure indexing key fields for fast lookup

		// journey report
		dbCollection.ensurePersistentIndex(Arrays.asList("refJourneyId","journeyStage", "timestamp"),createNonUniquePersistentIndex());
		
		// AQL_GET_TOUCHPOINT_REPORT
		dbCollection.ensurePersistentIndex(Arrays.asList("srcTouchpointName"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("srcTouchpointUrl"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("srcTouchpointName","srcTouchpointUrl","metricName"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refJourneyId","createdAt"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refJourneyId","srcTouchpointId", "createdAt"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refTouchpointHubId","srcTouchpointHubId", "createdAt"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refJourneyId","srcTouchpointId", "refProfileId", "createdAt"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refTouchpointHubId","srcTouchpointHubId", "refProfileId", "createdAt"),createNonUniquePersistentIndex());
		
		// AQL_GET_OBSERVER_REPORT
		dbCollection.ensurePersistentIndex(Arrays.asList("refJourneyId","observerId", "createdAt"),createNonUniquePersistentIndex());
		
		// AQL_GET_OBSERVER_REPORT_FOR_PROFILE
		dbCollection.ensurePersistentIndex(Arrays.asList("refJourneyId","observerId", "createdAt", "refProfileId"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refJourneyId","observerId", "refProfileId"),createNonUniquePersistentIndex());
		
		// AQL_GET_JOURNEY_EVENT_STATISTICS
		dbCollection.ensurePersistentIndex(Arrays.asList("refJourneyId","journeyStage", "refProfileId"),createNonUniquePersistentIndex());
		
		dbCollection.ensurePersistentIndex(Arrays.asList("refProfileId", "refJourneyId", "createdAt"), createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refProfileId", "createdAt"), createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refProfileId"), createNonUniquePersistentIndex());
		
		dbCollection.ensurePersistentIndex(Arrays.asList("sessionKey", "timestamp"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("observerId", "timestamp"),createNonUniquePersistentIndex());

		dbCollection.ensurePersistentIndex(Arrays.asList("srcTouchpointId", "timestamp"),
				createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refTouchpointId", "timestamp"),
				createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refMessageId", "timestamp"),
				createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refItemId", "timestamp"),
				createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refEventId", "timestamp"),
				createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refCampaignId", "timestamp"),
				createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("refProfileId", "timestamp"),
				createNonUniquePersistentIndex());
		
		dbCollection.ensurePersistentIndex(Arrays.asList("eventData[*]"),createNonUniquePersistentIndex());
		
		// transaction
		dbCollection.ensurePersistentIndex(Arrays.asList("transactionId"), createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("transactionCode"), createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("transactionCurrency"), createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("transactionStatus"), createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("transactionPayment"), createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("transactionValue"), createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("transactionDiscount"), createNonUniquePersistentIndex());
		
		dbCollection.ensurePersistentIndex(Arrays.asList("orderedItems[*].itemId"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("orderedItems[*].productCode"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("orderedItems[*].fullUrl"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("orderedItems[*].name"),createNonUniquePersistentIndex());
		
		dbCollection.ensurePersistentIndex(Arrays.asList("serviceItems[*].itemId"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("serviceItems[*].code"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("serviceItems[*].fullUrl"),createNonUniquePersistentIndex());
		dbCollection.ensurePersistentIndex(Arrays.asList("serviceItems[*].name"),createNonUniquePersistentIndex());
	}

	@Key
	@Expose
	protected String id;

	@Expose
	protected Date createdAt;
	
	@Expose
	protected Date updatedAt;

	@Expose
	protected int timestamp;

	@Expose
	protected String sessionKey;

	@Expose
	protected boolean isActiveTracked = true;
	
	@Expose
	protected boolean isConversion = false;
	
	@Expose
	protected boolean isExperience = false;
	
	@Expose
	protected String observerId = "";

	@Expose
	protected String metricName = "";

	@Expose
	protected long metricValue = 0;
	
	@Expose
	protected String message= "";
	
	@Expose
	protected String refProfileId = "";
	
	@Expose
	protected String refVisitorId = "";
	
	@Expose
	protected String refJourneyId = "";
	
	@Expose
	protected int journeyStage = 0;

	@Expose
	protected String srcTouchpointId= "";
	
	@Expose
	protected String srcTouchpointHubId = "";
	
	@Expose
	protected String srcTouchpointName = "";

	@Expose
	protected String srcTouchpointUrl = "";
	
	@Expose
	protected String srcTouchpointHost = "";

	@Expose
	protected String refTouchpointId = "";
	
	@Expose
	protected String refTouchpointHubId = "";
	
	@Expose
	protected String refTouchpointName = "";

	@Expose
	protected String refTouchpointUrl = "";
	
	@Expose
	protected String refTouchpointHost = "";

	@Expose
	protected String refCampaignId= "";

	@Expose
	protected String refContentId= "";

	@Expose
	protected String refItemId = "";
	
	@Expose
	protected String refTicketId = "";
	
	@Expose
	protected String refDataSource = "";
	
	@Expose
	protected String refFeedbackEventId = "";

	@Expose
	protected String browserName = "";
	
	@Expose
	protected String fingerprintId = "";

	@Expose
	protected String deviceId = "";

	@Expose
	protected String deviceOS = "";

	@Expose
	protected String deviceType = "";

	@Expose
	protected String deviceName = "";

	@Expose
	protected String sourceIP = "";
	
	@Expose
	protected String locationCode = "";
	
	@Expose
	protected String locationName = ""; 
	
	@Expose
	protected String locationAddress = ""; // shipping hay address of home
	
	// attr for ConversionEvent 
	@Expose
	protected int timeSpent = 1;

	@Expose
	protected String srcEventKey = "";

	// ---------------- BEGIN: transaction ----------------
	@Expose
	protected String transactionId = "";
	
	@Expose
	protected double transactionValue = 0;
	
	@Expose
	protected double  transactionShippingValue = 0; 
	
	@Expose
	protected String transactionStatus = "";
	
	@Expose
	protected int transactionCode = 0;
	
	@Expose
	protected double transactionDiscount = 0;
	
	@Expose
	protected String transactionCurrency = "";
	
	@Expose
	protected String transactionPayment = "";
	
	@Expose
	protected double  transactionTax = 0; 
	
	@Expose
	protected Map<String, Object>  transactionShippingInfo = new HashMap<String, Object>();
	
	@Expose
	protected Set<OrderedItem> orderedItems = new HashSet<>();
	
	@Expose
	protected Set<ServiceItem> serviceItems = new HashSet<>();
	
	@Expose
	protected Set<TradingItem> tradingItems = new HashSet<>();
	
	// ---------------- END: transaction ----------------

	@Expose
	protected int fraudScore = 0;
	
	@Expose
	protected String environment = "pro";
	
	@Expose
	protected Set<String> imageUrls = new HashSet<>();
	
	@Expose
	protected Set<String> videoUrls = new HashSet<>();
	
	@Expose
	protected Map<String, Object> eventData = new HashMap<>();
	
	@Expose
	protected int partitionId = 0;
	
	@Expose
	private int state = TrackingEventState.STATE_RAW_DATA; 
	
	// names of class or actor for processing data
	@Expose
	protected Set<ComputableField> computableFields;
	
	@Expose
	protected String rawJsonData = "";
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	EventObserver observer; 
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	Touchpoint refTouchpoint;
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	Touchpoint srcTouchpoint;

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.id) && StringUtil.isNotEmpty(this.metricName) && StringUtil.isNotEmpty(this.observerId)
				&& StringUtil.isNotEmpty(this.srcTouchpointId) && this.timestamp > 0;
	}
	
	@Override
	public String buildHashedId() {
	    validateRequiredFields();

	    StringBuilder keyHint = new StringBuilder()
	            .append(createdAt)
	            .append(transactionId)
	            .append(transactionStatus)
	            .append(deviceId)
	            .append(observerId)
	            .append(metricName)
	            .append(metricValue)
	            .append(srcTouchpointId)
	            .append(isConversion)
	            .append(eventData)
	            .append(refJourneyId)
	            .append(refProfileId)
	            .append(refDataSource)
	            .append(refItemId)
	            .append(refTicketId)
	            .append(refTouchpointHubId)
	            .append(srcTouchpointUrl);

	    if (Boolean.TRUE.equals(isActiveTracked)) {
	        keyHint.insert(0, RandomUtil.getRandom(1_000_000))
	               .append(System.currentTimeMillis());
	    }

	    this.id = createHashedId(keyHint.toString());
	    return this.id;
	}

	private void validateRequiredFields() {
	    if (observerId == null) {
	        throw new IllegalArgumentException("observerId is required");
	    }
	    if (metricName == null) {
	        throw new IllegalArgumentException("metricName is required");
	    }
	    if (srcTouchpointId == null) {
	        throw new IllegalArgumentException("srcTouchpointId is required");
	    }
	    if (createdAt == null) {
	        throw new IllegalArgumentException("createdAt is required");
	    }
	}


	public TrackingEvent() {
		// default constructor for single-view data report
		this.observerId = "";
		this.browserName = "";
		this.deviceName = "";
		this.deviceOS = "";
		this.deviceId = "";
		
		this.timestamp = DateTimeUtil.currentUnixTimestamp();
		this.createdAt = new Date(1000L * this.timestamp);
	}
	


	/**
	 * to create a tracking event from CDP API
	 * 
	 * @param observerId
	 * @param journeyStage
	 * @param dbProfileId
	 * @param metricName
	 * @param metricValue
	 * @param eventTime
	 * @param srcTouchpointId
	 * @param srcTouchpointName
	 * @param srcTouchpointUrl
	 */
	public TrackingEvent(String journeyMapId, int journeyStage, String observerId, String dbProfileId, String eventName, long eventValue, Date eventTime) {
		super();
		if(StringUtil.isEmpty(dbProfileId) || StringUtil.isEmpty(observerId) || StringUtil.isEmpty(journeyMapId) || StringUtil.isEmpty(eventName) || eventTime == null) {
			throw new InvalidDataException("dbProfileId, observerId, journeyMapId, eventName and eventTime must not be empty and have a valid value!");
		}
		this.isActiveTracked = false;
		this.sourceIP = "127.0.0.1";
		
		this.observerId =  observerId ;
		this.refJourneyId = journeyMapId;
		this.journeyStage = (journeyStage > 0 && journeyStage <= 5) ? journeyStage : 1;
		this.refProfileId = dbProfileId;
		
		this.metricName = eventName;
		this.metricValue = eventValue;	
		
		this.createdAt = eventTime;
		long createdTime = eventTime.getTime();
		this.timestamp = (int) (createdTime / 1000L);
		
		Device leoCdpApiDevice = Device.CDP_API_DEVICE;
		this.deviceId = leoCdpApiDevice.getId();
		this.browserName = leoCdpApiDevice.getBrowserName();
		this.deviceOS = leoCdpApiDevice.getOsName();
		this.deviceType = leoCdpApiDevice.getType();
		
		this.buildHashedId();
	}
	
	/**
	 * to create new import-data event for profile
	 * 
	 * @param profile
	 * @return
	 */
	public static TrackingEvent newImportEventForProfile(Profile profile, TouchpointHub tpHub) {
		String journeyMapId = JourneyMap.DEFAULT_JOURNEY_MAP_ID;
		int journeyStage = profile.hasContactData() ? 2 : 1;
		String observerId = EventObserver.DEFAULT_EVENT_OBSERVER_ID;
		String eventName = BehavioralEvent.General.DATA_IMPORT;
		long eventValue = 1;
		Date eventTime = profile.getUpdatedAt();
		String profileId = profile.getId();
		TrackingEvent e = new TrackingEvent(journeyMapId, journeyStage, observerId, profileId, eventName, eventValue, eventTime);
		e.setSrcTouchpointHub(tpHub);
		e.setSrcTouchpoint(TouchpointManagement.LEO_CDP_IMPORTER);
		return e;
	}
	
	public static TrackingEvent newPageViewEventForProfile(Profile profile, TouchpointHub tpHub, Touchpoint tp) {
		String journeyMapId = JourneyMap.DEFAULT_JOURNEY_MAP_ID;
		int journeyStage = profile.hasContactData() ? 2 : 1;
		String observerId = EventObserver.DEFAULT_EVENT_OBSERVER_ID;
		String eventName = BehavioralEvent.General.PAGE_VIEW;
		long eventValue = 1;
		Date eventTime = profile.getUpdatedAt();
		String profileId = profile.getId();
		TrackingEvent e = new TrackingEvent(journeyMapId, journeyStage, observerId, profileId, eventName, eventValue, eventTime);
		e.setSrcTouchpointHub(tpHub);
		e.setSrcTouchpoint(tp);
		return e;
	}

	/**
	 * passive tracking by a data crawler from social media 
	 * (e.g: Facebook, YouTube,...)
	 * 
	 * @param observerId
	 * @param metricName
	 * @param metricValue
	 * @param srcTouchpointId
	 */
	public TrackingEvent(String observerId, String metricName, long metricValue, String srcTouchpointId) {
		super();
		
		this.observerId = observerId;
		this.metricName = metricName;
		this.metricValue = metricValue;
		this.srcTouchpointId = srcTouchpointId;
		this.isActiveTracked = false;
		
		this.timestamp = DateTimeUtil.currentUnixTimestamp();
		this.createdAt = new Date(1000L * this.timestamp);
		
		this.buildHashedId();
	}
	

	/**
	 * 
	 * direct tracking from JavaScript or Mobile SDK
	 * 
	 * @param observerId
	 * @param sessionKey
	 * @param metricName
	 * @param metricValue
	 * @param refProfileId
	 * @param refProfileType
	 * @param srcTouchpointId
	 * @param refTouchpointId
	 * @param browserName
	 * @param webCookies
	 * @param deviceId
	 * @param deviceOS
	 * @param deviceName
	 * @param sourceIP
	 */
	public TrackingEvent(String observerId, String sessionKey, String metricName, long metricValue,
			String refProfileId, String refVisitorId, int refProfileType,TouchpointHub hub, Touchpoint srcTouchpoint, Touchpoint refTouchpoint,
			String browserName, String fingerprintId, String deviceId, String deviceOS, String deviceName, String deviceType,
			String sourceIP, Date createdAt) {
		super();
		this.observerId = observerId;
		this.sessionKey = sessionKey;
		this.metricName = metricName;
		this.metricValue = metricValue;
		this.refProfileId = refProfileId;
		this.refVisitorId = refVisitorId;
		this.browserName = browserName;
		this.fingerprintId = fingerprintId;
		this.deviceId = deviceId;
		this.deviceOS = deviceOS;
		this.deviceName = deviceName;
		this.deviceType = deviceType;
		this.sourceIP = sourceIP;
		this.createdAt = createdAt;
		this.timestamp = (int) (createdAt.getTime()/1000L);
		this.updateTouchpointData(hub, refTouchpoint, srcTouchpoint);
		this.buildHashedId();
	}	
	
	

	/**
	 * @param journeyMapId
	 * @param observerId
	 * @param metricName
	 * @param metricValue
	 * @param refProfileId
	 * @param refProfileType
	 * @param srcTouchpointId
	 * @param refTouchpointId
	 * @param sourceIP
	 * @param createdAt
	 * @param device
	 */
	public TrackingEvent(String journeyMapId, String observerId,  String metricName, long metricValue, String refProfileId, int refProfileType, String srcTouchpointId,
			String refTouchpointId,String sourceIP, Date createdAt, Device device) {
		super();
		this.isActiveTracked = false;
		this.refJourneyId = journeyMapId;
		this.observerId = observerId;

		this.metricName = metricName;
		this.metricValue = metricValue;
		this.refProfileId = refProfileId;
		this.srcTouchpointId = srcTouchpointId;
		this.refTouchpointId = refTouchpointId;
		
		this.sourceIP = sourceIP;
		this.createdAt = createdAt;
		this.timestamp = (int) (createdAt.getTime()/1000L);
		
		device = (device == null) ? Device.CDP_API_DEVICE : device;
		this.deviceId = device.getId();
		this.browserName = device.getBrowserName();
		this.deviceOS = device.getOsName();
		this.deviceType = device.getType();
		
		this.buildHashedId();
	}

	/////////////////////
	
	public boolean isActiveTracked() {
		return isActiveTracked;
	}

	public void setActiveTracked(boolean isActiveTracked) {
		this.isActiveTracked = isActiveTracked;
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	
	@Override
	public Date getUpdatedAt() {
		return this.updatedAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	public String getObserverId() {
		return observerId;
	}

	public void setObserverId(String observerId) {
		this.observerId = observerId;
	}

	public String getSrcTouchpointName() {
		return srcTouchpointName;
	}
	
	
	
	public void setSrcTouchpoint(Touchpoint srcTouchpoint) {
		this.srcTouchpointId = srcTouchpoint.getId();
		this.srcTouchpointName = srcTouchpoint.getName();
		this.srcTouchpointUrl = srcTouchpoint.getUrl();
	}

	public void setSrcTouchpointName(String srcTouchpointName) {
		this.srcTouchpointName = srcTouchpointName;
	}

	public String getSrcTouchpointUrl() {
		return srcTouchpointUrl;
	}

	public void setSrcTouchpointUrl(String srcTouchpointUrl) {
		this.srcTouchpointUrl = srcTouchpointUrl;
	}

	public String getRefTouchpointName() {
		return refTouchpointName;
	}

	public void setRefTouchpointName(String refTouchpointName) {
		this.refTouchpointName = refTouchpointName;
	}

	public String getRefTouchpointUrl() {
		return refTouchpointUrl;
	}

	public void setRefTouchpointUrl(String refTouchpointUrl) {
		this.refTouchpointUrl = refTouchpointUrl;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public long getMetricValue() {
		return metricValue;
	}

	public void setMetricValue(long metricValue) {
		this.metricValue = metricValue;
	}

	public String getRefProfileId() {
		return refProfileId;
	}

	public void setRefProfileId(String refProfileId) {
		this.refProfileId = refProfileId;
	}
	
	
	
	public String getRefVisitorId() {
		return refVisitorId;
	}

	public void setRefVisitorId(String refVisitorId) {
		this.refVisitorId = refVisitorId;
	}

	public String getSrcTouchpointId() {
		return srcTouchpointId;
	}

	public void setSrcTouchpointId(String srcTouchpointId) {
		this.srcTouchpointId = srcTouchpointId;
	}

	public String getRefTouchpointId() {
		return refTouchpointId;
	}

	public void setRefTouchpointId(String refTouchpointId) {
		this.refTouchpointId = refTouchpointId;
	}

	public String getRefCampaignId() {
		return refCampaignId;
	}

	public void setRefCampaignId(String refCampaignId) {
		this.refCampaignId = refCampaignId;
	}


	public String getRefContentId() {
		return refContentId;
	}

	public void setRefContentId(String refContentId) {
		this.refContentId = refContentId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		if(StringUtil.isNotEmpty(message)) {
			this.message = message;
		}
	}

	public String getRefItemId() {
		return refItemId;
	}

	public void setRefItemId(String refItemId) {
		this.refItemId = refItemId;
	}
	
	

	public String getRefDataSource() {
		return refDataSource;
	}

	public void setRefDataSource(String refDataSource) {
		this.refDataSource = refDataSource;
	}

	public String getRefTicketId() {
		return refTicketId;
	}

	public void setRefTicketId(String refTicketId) {
		this.refTicketId = refTicketId;
	}

	public String getRefJourneyId() {
		return refJourneyId;
	}

	public void setRefJourneyId(String refJourneyId) {
		this.refJourneyId = refJourneyId;
	}

	public int getJourneyStage() {
		return journeyStage;
	}

	public void setJourneyStage(int journeyStage) {
		this.journeyStage = journeyStage;
	}

	public String getRefFeedbackEventId() {
		return refFeedbackEventId;
	}

	public void setRefFeedbackEventId(String refFeedbackEventId) {
		if(refFeedbackEventId != null) {
			this.refFeedbackEventId = refFeedbackEventId;
		}
	}
	
	public void setRefFeedbackEvent(FeedbackEvent feedbackEvent) {
		if(feedbackEvent != null) {
			this.refFeedbackEventId = feedbackEvent.getId();
		}
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public String getBrowserName() {
		return browserName;
	}

	public void setBrowserName(String browserName) {
		this.browserName = browserName;
	}
	

	public String getFingerprintId() {
		return fingerprintId;
	}

	public void setFingerprintId(String fingerprintId) {
		this.fingerprintId = fingerprintId;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getDeviceOS() {
		return deviceOS;
	}

	public void setDeviceOS(String deviceOS) {
		this.deviceOS = deviceOS;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getSourceIP() {
		return sourceIP;
	}

	public void setSourceIP(String sourceIP) {
		this.sourceIP = sourceIP;
	}
	
	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public String getLocationCode() {
		return locationCode;
	}

	public void setLocationCode(String locationCode) {
		this.locationCode = locationCode;
	}

	public int getPartitionId() {
		return partitionId;
	}

	public void setPartitionId(int partitionId) {
		this.partitionId = partitionId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		if(StringUtil.isNotEmpty(environment)) {
			this.environment = environment;
		}
	}

	public Map<String, Object> getEventData() {
		if (eventData == null) {
			eventData = new HashMap<String, Object>(0);
		}
		return eventData;
	}

	public void setEventData(Map<String, Object> eventData) {
		if(eventData != null) {
			this.eventData = eventData;
		}
	}
	
	public void setEventData(String k, String v) {
		if(eventData != null) {
			this.eventData.put(k, v);
		}
	}
	
	public int getTimeSpent() {
		return timeSpent;
	}

	public void setTimeSpent(int timeSpent) {
		this.timeSpent = timeSpent;
	}

	public String getTransactionId() {
		if(StringUtil.isEmpty(transactionId)) {
			transactionId = EMPTY_TRANSACTION;
		}
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		if(StringUtil.isNotEmpty(transactionId)) {
			this.transactionId = transactionId;
		}
	}

	public String getTransactionStatus() {
		return transactionStatus;
	}

	public void setTransactionStatus(String transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

	public double getTransactionValue() {
		return transactionValue;
	}

	public void setTransactionValue(double transactionValue) {
		this.transactionValue = transactionValue;
	}

	public Set<OrderedItem> getOrderedItems() {
		if(orderedItems == null) {
			orderedItems = new HashSet<>(0);
		}
		return orderedItems;
	}

	public void setOrderedItems(Set<OrderedItem> orderedItems) {
		if(orderedItems != null) {
			this.orderedItems = orderedItems;
		}
	}
	
	public Set<ServiceItem> getServiceItems() {
		if(serviceItems == null) {
			serviceItems = new HashSet<>(0);
		}
		return serviceItems;
	}

	public void setServiceItems(Set<ServiceItem> serviceItems) {
		if(serviceItems != null) {
			this.serviceItems = serviceItems;
		}
	}
	
	public Set<TradingItem> getTradingItems() {
		if(tradingItems == null) {
			tradingItems = new HashSet<>(0);
		}
		return tradingItems;
	}

	public void setTradingItems(Set<TradingItem> tradingItems) {
		if(tradingItems != null) {
			this.tradingItems = tradingItems;
		}
	}

	public Set<String> getImageUrls() {
		return imageUrls;
	}

	public void setImageUrls(Set<String> imageUrls) {
		this.imageUrls = imageUrls;
	}
	
	public void setImageUrl(String imageUrl) {
		if(StringUtil.isValidUrl(imageUrl)) {
			this.imageUrls.add(imageUrl);
		}
	}
	
	public void setImageUrls(String imageUrls) {
		if(StringUtil.isNotEmpty(imageUrls)) {
			String[] urls = imageUrls.split(StringPool.SEMICOLON);
			for (String url : urls) {
				url = url.trim();
				if(StringUtil.isValidUrl(url)) {
					this.imageUrls.add(url);
				}
			}
		}
		
	}

	public Set<String> getVideoUrls() {
		return videoUrls;
	}

	public void setVideoUrls(Set<String> videoUrls) {
		this.videoUrls = videoUrls;
	}
	
	public void setVideoUrl(String videoUrl) {
		if(StringUtil.isValidUrl(videoUrl)) {
			this.videoUrls.add(videoUrl);
		}
	}
	
	public void setVideoUrls(String videoUrls) {
		if(StringUtil.isNotEmpty(videoUrls)) {
			String[] urls = videoUrls.split(StringPool.SEMICOLON);
			for (String url : urls) {
				url = url.trim();
				if(StringUtil.isValidUrl(url)) {
					this.videoUrls.add(url);
				}
			}
		}
	}

	public String getTransactionCurrency() {
		return transactionCurrency;
	}

	public void setTransactionCurrency(String currency) {
		if(StringUtil.isNotEmpty(currency)) {
			this.transactionCurrency = currency;
		}
	}

	public String getSrcEventKey() {
		return srcEventKey;
	}

	public void setSrcEventKey(String srcEventKey) {
		this.srcEventKey = srcEventKey;
	}


	public int getFraudScore() {
		return fraudScore;
	}

	public void setFraudScore(int fraudScore) {
		this.fraudScore = fraudScore;
	}

	public boolean isConversion() {
		return isConversion;
	}

	public void setConversion(boolean isConversion) {
		this.isConversion = isConversion;
	}
	
	/**
	 * set conversion information with OrderTransaction
	 * 
	 * @param isConversion
	 * @param transaction
	 */
	public void setOrderTransaction(boolean isConversion, OrderTransaction transaction) {
		this.isConversion = isConversion;
		
		// in transaction of retail, the shopping items must be called as ordered items
		Set<OrderedItem> orderedItems = transaction.getOrderedItems();	
		this.setOrderedItems(orderedItems);
		
		// in transaction of service, the service items 
		Set<ServiceItem> serviceItems = transaction.getServiceItems();
		this.setServiceItems(serviceItems);
		
		
		
		String transactionId = transaction.getTransactionId();
		String transactionStatus = transaction.getTransactionStatus();
		double transactionValue = transaction.getTotalTransactionValue();
		double transactionDiscount = transaction.getTransactionDiscount();
		String currencyCode = transaction.getCurrencyCode();
		String paymentInfo = transaction.getPaymentInfo();
		
		this.setTransactionId(transactionId);
		this.setTransactionStatus(transactionStatus);
		this.setTransactionValue(transactionValue);
		this.setTransactionCurrency(currencyCode);
		this.setTransactionDiscount(transactionDiscount);
		this.setTransactionPayment(paymentInfo);
		
		this.setTransactionShippingValue(transaction.getShippingValue());
		this.setTransactionShippingInfo(transaction.getShippingInfo());
		this.setTransactionTax(transaction.getTax());
	}

	public boolean isExperience() {
		return isExperience;
	}

	public void setExperience(boolean isExperience) {
		this.isExperience = isExperience;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}


	public String getLocationAddress() {
		return locationAddress;
	}

	public void setLocationAddress(String locationAddress) {
		if(locationAddress != null) {
			this.locationAddress = locationAddress;
		}
	}

	public double getTransactionDiscount() {
		return transactionDiscount;
	}

	public void setTransactionDiscount(double transactionDiscount) {
		this.transactionDiscount = transactionDiscount;
	}

	public String getTransactionPayment() {
		return transactionPayment;
	}

	public void setTransactionPayment(String transactionPayment) {
		if(StringUtil.isNotEmpty(transactionPayment)) {
			this.transactionPayment = transactionPayment;
		}
	}
	
	

	public double getTransactionTax() {
		return transactionTax;
	}

	public void setTransactionTax(double transactionTax) {
		this.transactionTax = transactionTax;
	}

	public Map<String, Object> getTransactionShippingInfo() {
		return transactionShippingInfo;
	}
	
	

	public double getTransactionShippingValue() {
		return transactionShippingValue;
	}

	public void setTransactionShippingValue(double transactionShippingValue) {
		this.transactionShippingValue = transactionShippingValue;
	}

	public void setTransactionShippingInfo(Map<String, Object> transactionShippingInfo) {
		this.transactionShippingInfo = transactionShippingInfo;
	}

	public String getRawJsonData() {
		return rawJsonData;
	}

	public void setRawJsonData(String rawJsonData) {
		if(rawJsonData != null) {
			this.rawJsonData = rawJsonData;
		}
	}

	public Set<ComputableField> getComputableFields() {
		if(this.computableFields == null) {
			this.computableFields = new HashSet<>(0);
		}
		return computableFields;
	}
	
	public boolean deleteComputableField(String field) {
		if(this.computableFields != null) {
			return this.computableFields.removeIf( s -> s.equals(field) );
		}
		return false;
	}

	public void setDataAgents(Set<ComputableField> fields) {
		if(fields != null) {
			this.computableFields = fields;
		}
	}
	
	public void setComputableFields(ComputableField field) {
		if(this.computableFields == null) {
			this.computableFields = new HashSet<>();
		}
		this.computableFields.add(field);
	}
	
	public Set<String> getViewedProductIds() {
		String strProductIds = StringUtil.safeString(this.getEventData().get(PRODUCT_IDS),"") ;
		if(StringUtil.isNotEmpty(strProductIds)) {
			Set<String> productIds = new HashSet<String>(Arrays.asList(strProductIds.split(SEMICOLON)));
			return productIds;
		}
		return new HashSet<>(0);
	}
	

	public String getSrcTouchpointHubId() {
		return srcTouchpointHubId;
	}

	public void setSrcTouchpointHubId(String srcTouchpointHubId) {
		if(StringUtil.isNotEmpty(srcTouchpointHubId)) {
			this.srcTouchpointHubId = srcTouchpointHubId;
		}
	}
	
	public void setSrcTouchpointHub(TouchpointHub srcTouchpointHub) {
		if(srcTouchpointHub != null) {
			this.srcTouchpointHubId = srcTouchpointHub.getId();;
			this.observerId = srcTouchpointHub.getObserverId();
			String importerName = StringUtil.safeString(srcTouchpointName) + " " + srcTouchpointHub.getName();
			setSrcTouchpointName(importerName.trim());
		}
	}

	public String getRefTouchpointHubId() {
		return refTouchpointHubId;
	}

	public void setRefTouchpointHubId(String refTouchpointHubId) {
		if(StringUtil.isNotEmpty(refTouchpointHubId)) {
			this.refTouchpointHubId = refTouchpointHubId;
		}
	}
	
	public String getSrcTouchpointHost() {
		return srcTouchpointHost;
	}

	public void setSrcTouchpointHost(String srcTouchpointHost) {
		this.srcTouchpointHost = srcTouchpointHost;
	}

	public String getRefTouchpointHost() {
		return refTouchpointHost;
	}

	public void setRefTouchpointHost(String refTouchpointHost) {
		this.refTouchpointHost = refTouchpointHost;
	}

	@Override
	public void unifyData() {
		this.observer = EventObserverDaoUtil.getById(this.observerId);
		this.refTouchpoint = TouchpointManagement.getById(this.refTouchpointId);
		this.srcTouchpoint = TouchpointManagement.getById(this.srcTouchpointId);
	}

	public Touchpoint getRefTouchpoint() {
		if(refTouchpoint == null && refTouchpointId != null) {
			this.refTouchpoint = TouchpointManagement.getById(this.refTouchpointId);
		}
		return refTouchpoint;
	}
	
	public Touchpoint getSrcTouchpoint() {
		if(srcTouchpoint == null && srcTouchpointId != null) {
			this.srcTouchpoint = TouchpointManagement.getById(this.srcTouchpointId);
		}
		return srcTouchpoint;
	}

	public EventObserver getObserver() {
		if(observer == null && observerId != null) {
			observer = EventObserverDaoUtil.getById(observerId);
		}
		return observer;
	}
	
	public void updateTouchpointData(TouchpointHub hub, Touchpoint dbRefTouchpoint, Touchpoint dbSrcTouchpoint) {
		if(hub != null) {
			this.refTouchpointHubId = hub.getId();
		}
		if(dbRefTouchpoint != null) {
			this.refTouchpointId = dbRefTouchpoint.getId();
			this.refTouchpointName = dbRefTouchpoint.getName();
			this.refTouchpointUrl = dbRefTouchpoint.getUrl();
		}
		this.refTouchpointHost = UrlUtil.getHostName(this.refTouchpointUrl);
		
		if(dbSrcTouchpoint != null) {
			this.srcTouchpointId = dbSrcTouchpoint.getId();
			this.srcTouchpointName = dbSrcTouchpoint.getName();
			this.srcTouchpointUrl = dbSrcTouchpoint.getUrl();
		}
		this.srcTouchpointHost = UrlUtil.getHostName(this.srcTouchpointUrl);
	}
	
	/**
	 * to run data enrichment jobs for computable fields
	 */
	public void processComputableFields() {
		// GeoLocation process
		GeoLocation loc = GeoLocationUtil.getGeoLocation(this.sourceIP);
		double latitude = loc.getLatitude();
		double longitude = loc.getLongitude();
		OsmGeoLocation openStreetMapObj = GeoLocationUtil.getLocationName(latitude, longitude);
		this.setLocationCode(loc.getLocationCode());
		this.setLocationName(openStreetMapObj.getLocationName());
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
}
