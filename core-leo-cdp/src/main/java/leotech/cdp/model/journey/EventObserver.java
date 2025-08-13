package leotech.cdp.model.journey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * Event Observer is the object to collect and track all event data stream of Touchpoint Hub
 * 
 * @author Trieu Nguyen
 * @since 2020
 */
public final class EventObserver extends PersistentObject {
	
	public static final String DEFAULT_ACCESS_KEY = "default_access_key";
	public static final String DEFAULT_EVENT_OBSERVER_ID = "leo_data_observer";
	
	public static final String COLLECTION_NAME = getCdpCollectionName(EventObserver.class);
	static ArangoCollection instance;

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();
			instance = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields for fast lookup
			instance.ensurePersistentIndex(Arrays.asList("name"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("slug"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("touchpointHubId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("deviceId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("mobileAppId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("journeyMapId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("dataSourceHosts[*]"), new PersistentIndexOptions().unique(false));
		}
		return instance;
	}

	@Key
	@Expose
	String id;
	
	@Expose
	boolean collectDirectly = false;
	
	@Expose
	boolean firstPartyData = false;

	@Expose
	String name;
	
	@Expose
	String slug = "";
	
	@Expose
	String dataSourceUrl = "";
	
	@Expose
	Set<String> dataSourceHosts = new HashSet<>();
	
	@Expose
	String thumbnailUrl = "";
	
	@Expose
	QrCodeData qrCodeData = null;

	@Expose
	int type = TouchpointType.WEBSITE;// observer for website

	@Expose
	int status = 1;

	@Expose
	long estimatedTotalEvent = 0;

	@Expose
	String observerUri;

	@Expose
	String touchpointHubId;
	
	@Expose
	int journeyLevel = 0;

	@Expose
	String deviceId;

	@Expose
	String securityAccessIp;

	@Expose
	String securityCode = "";

	@Expose
	List<String> javascriptTags = new ArrayList<String>(2);

	@Expose
	List<String> webApiHooks;

	@Expose
	String mobileAppId;

	@Expose
	Date createdAt;

	@Expose
	Date updatedAt;
	
	@Expose
	String journeyMapId = "";
	
	@Expose
	Map<String, String> accessTokens = new HashMap<String, String>(10);
	
	public EventObserver() {
		// gson
	}

	public EventObserver(TouchpointHub touchpointHub) {
		super();
		this.createdAt = new Date();
		update(touchpointHub);
		this.buildHashedId();
	}
	
	/**
	 * to update event observer metadata
	 * 
	 * @param touchpointHub
	 */
	public void update(TouchpointHub touchpointHub) {
		this.name = touchpointHub.getName();
		this.slug = new Slugify().slugify(this.name);
		this.type = touchpointHub.getType();
		this.touchpointHubId = touchpointHub.getId();
		this.firstPartyData =  touchpointHub.isFirstPartyData();
		this.dataSourceHosts = touchpointHub.getDataSourceHosts();
		this.thumbnailUrl = touchpointHub.getThumbnailUrl();
		this.journeyMapId = touchpointHub.getJourneyMapId();
		this.journeyLevel = touchpointHub.getJourneyLevel();
		this.dataSourceUrl = touchpointHub.getUrl();
		this.securityCode = TokenUtil.getRandomToken(dataSourceUrl + name);	
		
		// only data from online channels can be collected directly
		this.collectDirectly = this.firstPartyData && 
								(this.type > TouchpointType.SEARCH_ENGINE && this.type < TouchpointType.RETAIL_STORE 
								|| this.type == TouchpointType.CUSTOMER_SERVICE  || this.type == TouchpointType.FEEDBACK_SURVEY 
								|| this.type == TouchpointType.DATA_OBSERVER || this.type == TouchpointType.DIGITAL_DOCUMENT );
	}
	
	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(id);
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if(this.name != null && this.touchpointHubId != null) {
			if(this.type == TouchpointType.DATA_OBSERVER) {
				// only one leo data observer can existed !? 
				this.id = DEFAULT_EVENT_OBSERVER_ID;
			}
			else {
				String keyHint = this.name + this.type + this.touchpointHubId;
				this.id = createId(this.id, keyHint);
			}
		}
		return this.id;
	}
	
	/**
	 * is Default Event Observer
	 * @return
	 */
	public final boolean isDefaultEventObserver() {
		return this.id.equals(DEFAULT_EVENT_OBSERVER_ID);
	}
	
	/**
	 * @param landingPageUrl
	 */
	public final void generateQrCodeImage(String landingPageUrl) {
		TargetMediaUnit targetMediaUnit = new TargetMediaUnit(this.touchpointHubId, this.id, landingPageUrl, this.name);
		String trackingUrl = targetMediaUnit.getTrackingLinkUrl();
		if(StringUtil.isNotEmpty(trackingUrl)) {
			String shortUrl = targetMediaUnit.getShortUrl();
			this.qrCodeData = new QrCodeData(trackingUrl, shortUrl , landingPageUrl, this.slug);
			TargetMediaUnitDaoUtil.save(targetMediaUnit);
		}
	}
	
	/**
	 * @param tokenName
	 * @param domain
	 */
	public final String generateAccessToken(String tokenName, String domain, boolean reset) {
		if(reset) {
			this.accessTokens.remove(tokenName);
		}
		if(!this.accessTokens.containsKey(tokenName)) {
			String value = TokenUtil.getRandomToken(tokenName +  domain);
			this.accessTokens.put(tokenName, value);
			return value;
		}
		return "";
	}
	
	public final void removeAccessToken(String tokenName) {
		this.accessTokens.remove(tokenName);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	

	public Set<String> getDataSourceHosts() {
		return dataSourceHosts;
	}

	public void setDataSourceHosts(Set<String> dataSourceHosts) {
		this.dataSourceHosts = dataSourceHosts;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public long getEstimatedTotalEvent() {
		return estimatedTotalEvent;
	}

	public void setEstimatedTotalEvent(long estimatedTotalEvent) {
		this.estimatedTotalEvent = estimatedTotalEvent;
	}

	public String getObserverUri() {
		return observerUri;
	}

	public void setObserverUri(String observerUri) {
		this.observerUri = observerUri;
	}

	public String getTouchpointHubId() {
		return touchpointHubId;
	}

	public void setTouchpointHubId(String touchpointHubId) {
		this.touchpointHubId = touchpointHubId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getSecurityAccessIp() {
		return securityAccessIp;
	}

	public void setSecurityAccessIp(String securityAccessIp) {
		this.securityAccessIp = securityAccessIp;
	}

	public String getSecurityCode() {
		return securityCode;
	}

	public void setSecurityCode(String securityCode) {
		this.securityCode = securityCode;
	}

	public Map<String, String> getAccessTokens() {
		return accessTokens;
	}

	public void setAccessTokens(Map<String, String> accessTokens) {
		this.accessTokens = accessTokens;
	}

	public List<String> getJavascriptTags() {
		return javascriptTags;
	}

	public void setJavascriptTags(List<String> javascriptTags) {
		this.javascriptTags = javascriptTags;
	}
	
	public void setJavascriptTags(String javascriptTag) {
		if(this.javascriptTags == null) {
			this.javascriptTags = new ArrayList<String>();
		}
		this.javascriptTags.add(javascriptTag);
	}

	public List<String> getWebApiHooks() {
		return webApiHooks;
	}

	public void setWebApiHooks(List<String> webApiHooks) {
		this.webApiHooks = webApiHooks;
	}

	public String getMobileAppId() {
		return mobileAppId;
	}

	public void setMobileAppId(String mobileAppId) {
		this.mobileAppId = mobileAppId;
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
		return updatedAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	public boolean isCollectDirectly() {
		return collectDirectly;
	}

	public void setCollectDirectly(boolean collectDirectly) {
		this.collectDirectly = collectDirectly;
	}

	public boolean isFirstPartyData() {
		return firstPartyData;
	}

	public void setFirstPartyData(boolean firstPartyData) {
		this.firstPartyData = firstPartyData;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}


	public QrCodeData getQrCodeData() {
		return qrCodeData;
	}

	public void setQrCodeData(QrCodeData qrCodeData) {
		this.qrCodeData = qrCodeData;
	}

	public String getDataSourceUrl() {
		return dataSourceUrl;
	}

	public void setDataSourceUrl(String dataSourceUrl) {
		this.dataSourceUrl = dataSourceUrl;
	}

	public String getJourneyMapId() {
		return journeyMapId;
	}

	public void setJourneyMapId(String journeyMapId) {
		this.journeyMapId = journeyMapId;
	}
	

	public int getJourneyLevel() {
		return journeyLevel;
	}

	public void setJourneyLevel(int journeyLevel) {
		this.journeyLevel = journeyLevel;
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
