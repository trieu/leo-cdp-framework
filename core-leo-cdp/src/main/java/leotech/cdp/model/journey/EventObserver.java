package leotech.cdp.model.journey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.dao.TargetMediaUnitDaoUtil;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.util.TokenUtil;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * Event Observer is the object to collect and track all event data streams of Touchpoint Hub. <br>
 * An Event Observer can be associated with a specific Touchpoint Hub, and can be used to collect 
 * event data from the touchpoint hub, and send the event data to the CDP for processing and analytics. <br><br>
 * 
 * ArangoDB Collection: cdp_eventobserver <br>
 * 
 * @author Trieu Nguyen
 * @since 2020
 */
public final class EventObserver extends PersistentObject {
	
	public static final String DEFAULT_ACCESS_KEY = "default_access_key";
	public static final String DEFAULT_EVENT_OBSERVER_ID = "leo_data_observer";
	
	public static final String COLLECTION_NAME = getCdpCollectionName(EventObserver.class);
	
	// FIX: Volatile for thread-safe lazy initialization
	private static volatile ArangoCollection instance;
	
	// FIX: Cached Slugify to prevent object thrashing and heavy regex recompilation on updates
	private static final Slugify SLUGIFY = new Slugify();

	@Key
	@Expose
	private String id;
	
	@Expose
	private boolean collectDirectly = false;
	
	@Expose
	private boolean firstPartyData = false;

	@Expose
	private String name;
	
	@Expose
	private String slug = "";
	
	@Expose
	private String dataSourceUrl = "";
	
	@Expose
	private Set<String> dataSourceHosts = new HashSet<>();
	
	@Expose
	private String thumbnailUrl = "";
	
	@Expose
	private QrCodeData qrCodeData = null;

	@Expose
	private int type = TouchpointType.WEBSITE; // observer for website

	@Expose
	private int status = 1;

	@Expose
	private long estimatedTotalEvent = 0;

	@Expose
	private String observerUri;

	@Expose
	private String touchpointHubId;
	
	@Expose
	private int journeyLevel = 0;

	@Expose
	private String deviceId;

	@Expose
	private String securityAccessIp;

	@Expose
	private String securityCode = "";

	@Expose
	private List<String> javascriptTags = new ArrayList<>(5);

	@Expose
	private List<String> webApiHooks;

	@Expose
	private String mobileAppId;

	@Expose
	private Date createdAt;

	@Expose
	private Date updatedAt;
	
	@Expose
	private String journeyMapId = "";
	
	// FIX: Swapped to ConcurrentHashMap to prevent thread-safety issues during token generation
	@Expose
	private Map<String, String> accessTokens = new ConcurrentHashMap<>(10);
	
	public EventObserver() {
		// Default constructor for Gson
	}

	public EventObserver(TouchpointHub touchpointHub) {
		Date now = new Date();
		this.createdAt = now;
		this.updatedAt = now;
		this.update(touchpointHub);
		this.buildHashedId();
	}

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			// FIX: Double-checked locking to prevent index race conditions
			synchronized (EventObserver.class) {
				if (instance == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);
					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);

					// --------------------------------------------------------------------------------
					// ARANGODB 3.11 INDEX OPTIMIZATION (RocksDB Engine)
					// Consolidated individual indices to support hierarchical routing and filtering.
					// Array indices (like dataSourceHosts[*]) must remain separate to work efficiently.
					// --------------------------------------------------------------------------------
					
					// Core routing lookup
					col.ensurePersistentIndex(Arrays.asList("slug"), pIdxOpts);
					
					// Core UI & hierarchy queries. 
					// Covers queries by ["journeyMapId"] and ["journeyMapId", "touchpointHubId"]
					col.ensurePersistentIndex(Arrays.asList("journeyMapId", "touchpointHubId"), pIdxOpts);
					
					// Lookup filters
					col.ensurePersistentIndex(Arrays.asList("status", "type"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("deviceId"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("mobileAppId"), pIdxOpts);
					
					// ArangoDB Array Index for wild-card domain matching
					col.ensurePersistentIndex(Arrays.asList("dataSourceHosts[*]"), pIdxOpts);

					instance = col;
				}
			}
		}
		return instance;
	}
	
	/**
	 * Updates event observer metadata using the parent TouchpointHub.
	 * 
	 * @param touchpointHub the TouchpointHub parent
	 */
	public void update(TouchpointHub touchpointHub) {
		this.name = touchpointHub.getName();
		
		// FIX: Use cached static SLUGIFY
		this.slug = SLUGIFY.slugify(this.name);
		this.type = touchpointHub.getType();
		this.touchpointHubId = touchpointHub.getId();
		this.firstPartyData = touchpointHub.isFirstPartyData();
		this.dataSourceHosts = touchpointHub.getDataSourceHosts();
		this.thumbnailUrl = touchpointHub.getThumbnailUrl();
		this.journeyMapId = touchpointHub.getJourneyMapId();
		this.journeyLevel = touchpointHub.getJourneyLevel();
		this.dataSourceUrl = touchpointHub.getUrl();
		this.securityCode = TokenUtil.getRandomToken(this.dataSourceUrl + this.name);	
		
		// Only data from online channels can be collected directly
		this.collectDirectly = this.firstPartyData && 
			((this.type > TouchpointType.SEARCH_ENGINE && this.type < TouchpointType.RETAIL_STORE) 
				|| this.type == TouchpointType.CUSTOMER_SERVICE  
				|| this.type == TouchpointType.FEEDBACK_SURVEY 
				|| this.type == TouchpointType.DATA_OBSERVER 
				|| this.type == TouchpointType.DIGITAL_DOCUMENT);
	}
	
	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(id);
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (StringUtil.isNotEmpty(this.name) && StringUtil.isNotEmpty(this.touchpointHubId)) {
			if (this.type == TouchpointType.DATA_OBSERVER) {
				// Only one leo data observer can exist
				this.id = DEFAULT_EVENT_OBSERVER_ID;
			} else {
				String keyHint = this.name + this.type + this.touchpointHubId;
				this.id = createId(this.id, keyHint);
			}
			return this.id;
		}
		
		// FIX: Correctly throw the error with a meaningful message
		throw new IllegalArgumentException("name and touchpointHubId are required to build an EventObserver ID");
	}
	
	/**
	 * Is Default Event Observer
	 * @return boolean
	 */
	public final boolean isDefaultEventObserver() {
		return DEFAULT_EVENT_OBSERVER_ID.equals(this.id);
	}
	
	/**
	 * Generates QR code image data for the landing page.
	 * @param landingPageUrl the URL of the landing page
	 */
	public final void generateQrCodeImage(String landingPageUrl) {
		TargetMediaUnit targetMediaUnit = new TargetMediaUnit(this.touchpointHubId, this.id, landingPageUrl, this.name);
		String trackingUrl = targetMediaUnit.getTrackingLinkUrl();
		
		if (StringUtil.isNotEmpty(trackingUrl)) {
			String shortUrl = targetMediaUnit.getShortUrl();
			this.qrCodeData = new QrCodeData(trackingUrl, shortUrl, landingPageUrl, this.slug);
			TargetMediaUnitDaoUtil.save(targetMediaUnit);
		}
	}
	
	/**
	 * Generates or retrieves an access token for the given domain.
	 * 
	 * @param tokenName the token name
	 * @param domain the domain
	 * @param reset whether to reset the existing token
	 * @return the new token, or an empty string if it already existed and wasn't reset
	 */
	public final String generateAccessToken(String tokenName, String domain, boolean reset) {
		if (reset) {
			this.accessTokens.remove(tokenName);
		}
		if (!this.accessTokens.containsKey(tokenName)) {
			String value = TokenUtil.getRandomToken(tokenName + domain);
			this.accessTokens.put(tokenName, value);
			return value;
		}
		return "";
	}
	
	public final void removeAccessToken(String tokenName) {
		if (this.accessTokens != null) {
			this.accessTokens.remove(tokenName);
		}
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public int getType() { return type; }
	public void setType(int type) { this.type = type; }

	public int getStatus() { return status; }
	public void setStatus(int status) { this.status = status; }

	public Set<String> getDataSourceHosts() { return dataSourceHosts; }
	public void setDataSourceHosts(Set<String> dataSourceHosts) { this.dataSourceHosts = dataSourceHosts; }

	public String getThumbnailUrl() { return thumbnailUrl; }
	public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

	public long getEstimatedTotalEvent() { return estimatedTotalEvent; }
	public void setEstimatedTotalEvent(long estimatedTotalEvent) { this.estimatedTotalEvent = estimatedTotalEvent; }

	public String getObserverUri() { return observerUri; }
	public void setObserverUri(String observerUri) { this.observerUri = observerUri; }

	public String getTouchpointHubId() { return touchpointHubId; }
	public void setTouchpointHubId(String touchpointHubId) { this.touchpointHubId = touchpointHubId; }

	public String getDeviceId() { return deviceId; }
	public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

	public String getSecurityAccessIp() { return securityAccessIp; }
	public void setSecurityAccessIp(String securityAccessIp) { this.securityAccessIp = securityAccessIp; }

	public String getSecurityCode() { return securityCode; }
	public void setSecurityCode(String securityCode) { this.securityCode = securityCode; }

	public Map<String, String> getAccessTokens() { return accessTokens; }
	public void setAccessTokens(Map<String, String> accessTokens) { this.accessTokens = accessTokens; }

	public List<String> getJavascriptTags() { return javascriptTags; }
	public void setJavascriptTags(List<String> javascriptTags) { this.javascriptTags = javascriptTags; }
	
	public void setJavascriptTags(String javascriptTag) {
		if (this.javascriptTags == null) {
			this.javascriptTags = new ArrayList<>();
		}
		this.javascriptTags.add(javascriptTag);
	}

	public List<String> getWebApiHooks() { return webApiHooks; }
	public void setWebApiHooks(List<String> webApiHooks) { this.webApiHooks = webApiHooks; }

	public String getMobileAppId() { return mobileAppId; }
	public void setMobileAppId(String mobileAppId) { this.mobileAppId = mobileAppId; }

	@Override
	public Date getCreatedAt() { return createdAt; }
	@Override
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

	@Override
	public Date getUpdatedAt() { return updatedAt; }
	@Override
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	public boolean isCollectDirectly() { return collectDirectly; }
	public void setCollectDirectly(boolean collectDirectly) { this.collectDirectly = collectDirectly; }

	public boolean isFirstPartyData() { return firstPartyData; }
	public void setFirstPartyData(boolean firstPartyData) { this.firstPartyData = firstPartyData; }

	public String getSlug() { return slug; }
	public void setSlug(String slug) { this.slug = slug; }

	public QrCodeData getQrCodeData() { return qrCodeData; }
	public void setQrCodeData(QrCodeData qrCodeData) { this.qrCodeData = qrCodeData; }

	public String getDataSourceUrl() { return dataSourceUrl; }
	public void setDataSourceUrl(String dataSourceUrl) { this.dataSourceUrl = dataSourceUrl; }

	public String getJourneyMapId() { return journeyMapId; }
	public void setJourneyMapId(String journeyMapId) { this.journeyMapId = journeyMapId; }
	
	public int getJourneyLevel() { return journeyLevel; }
	public void setJourneyLevel(int journeyLevel) { this.journeyLevel = journeyLevel; }

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}