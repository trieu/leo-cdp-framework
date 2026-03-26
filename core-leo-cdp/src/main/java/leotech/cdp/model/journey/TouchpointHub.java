package leotech.cdp.model.journey;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.GeoIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.analytics.TouchpointHubReport;
import leotech.system.util.UrlUtil;
import leotech.system.util.database.PersistentObject;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * Touchpoint Hub is the root grouping entity for Touchpoints that share the same domain URL, location-code, or individual person. <br>
 * 
 * TouchpointHub is utilized to build robust omnichannel Customer Journey Maps. It tracks customer movement, 
 * unifies Cross-Device/Cross-Platform interactions, and feeds data directly into Machine Learning pipelines 
 * to optimize Journey Analytics, Conversion Rates, and Next-Best-Action predictions. <br><br>
 * 
 * ArangoDB collection: cdp_touchpointhub
 * 
 * @author tantrieuf31
 * @since 2020
 */
public class TouchpointHub extends PersistentObject {
	
	public static final String URL_LEO_DATA_OBSERVER = "https://"+SystemMetaData.DOMAIN_CDP_OBSERVER;
	
	// SYSTEM_DATA_OBSERVER
	public static final TouchpointHub DATA_OBSERVER = new TouchpointHub("DATA OBSERVER", TouchpointType.DATA_OBSERVER, true, URL_LEO_DATA_OBSERVER, 5, JourneyMap.DEFAULT_JOURNEY_MAP_ID);
		
	// URL for admin management
	public static final String URL_LEO_ADMIN_VISITORS = "https://"+SystemMetaData.DOMAIN_CDP_ADMIN + "/#calljs-leoCdpRouter('Profile_List_By_Type','visitor')";
	public static final String URL_LEO_ADMIN_LEADS = "https://"+SystemMetaData.DOMAIN_CDP_ADMIN + "/#calljs-leoCdpRouter('Profile_List_By_Type','lead')";
	public static final String URL_LEO_ADMIN_CUSTOMERS = "https://"+SystemMetaData.DOMAIN_CDP_ADMIN + "/#calljs-leoCdpRouter('Profile_List_By_Type','customer')";
	
	
	public static final String COLLECTION_NAME = getCdpCollectionName(TouchpointHub.class);
	
	// Thread-safe lazy database initialization
	static volatile ArangoCollection instance;

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			// Double-checked locking to prevent index race conditions
			synchronized (TouchpointHub.class) {
				if (instance == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);

					PersistentIndexOptions pIdxOpts = createNonUniquePersistentIndex();

					// --------------------------------------------------------------------------------
					// ARANGODB 3.11 INDEX OPTIMIZATION (RocksDB Engine)
					// Removed 10+ standalone redundant indexes (e.g., ["name"], ["url"], ["type"], etc). 
					// Grouped them into highly efficient Composite Indexes tailored for Hierarchical 
					// Journey Lookups and API filtering (JourneyMap -> Observer -> Hub -> Touchpoint).
					// --------------------------------------------------------------------------------

					// Core Journey Mapping & Hierarchy: Instantly fetches all Hubs for a specific Journey
					col.ensurePersistentIndex(Arrays.asList("journeyMapId", "isRootNode"), pIdxOpts);
					
					// Core API Routing & Identification Lookups
					col.ensurePersistentIndex(Arrays.asList("observerId", "type"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("url", "name"), pIdxOpts);
					
					// Core Geographical & Location Filtering
					col.ensurePersistentIndex(Arrays.asList("countryCode", "locationCode"), pIdxOpts);
					col.ensureGeoIndex(Arrays.asList("latitude", "longitude"), new GeoIndexOptions());

					// Array Domain/Host resolution (must remain isolated for Array querying in ArangoDB)
					col.ensurePersistentIndex(Arrays.asList("hostname"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("dataSourceHosts[*]"), pIdxOpts);
					
					// NLP / AI Search Tag Arrays
					col.ensurePersistentIndex(Arrays.asList("keywords[*]"), pIdxOpts);

					instance = col;
				}
			}
		}
		return instance;
	}
	
	public static String getDocumentUUID(String id) {
		return COLLECTION_NAME + "/" + id;
	}

	@Override
	public boolean dataValidation() {
		boolean ok = StringUtil.isNotEmpty(this.id) && StringUtil.isNotEmpty(this.name) && this.type >= 0 
				&& StringUtil.isNotEmpty(this.journeyMapId)
				&& (StringUtil.isNotEmpty(this.url) || StringUtil.isNotEmpty(this.locationCode));
		if(ok) {
			this.name = this.name.trim();
			this.url = this.url.trim();
			this.typeAsType = TouchpointType.getTypeAsText(type);
		}
		return ok;
	}

	@Key
	@Expose
	protected String id;
	
	@Expose
	protected String name;
	
	@Expose
	protected int type = TouchpointType.CDP_API;
	
	@Expose
	protected String typeAsType = TouchpointType.getTypeAsText(type);
	
	@Expose
	protected boolean firstPartyData = true;
	
	@Expose
	protected boolean isRootNode = false;
	
	@Expose
	Date createdAt = new Date();
	
	@Expose
	Date updatedAt = new Date();
	
	@Expose
	protected int status = 1;
	
	@Expose
	protected String url = "";
	
	@Expose
	protected String hostname = "";
	
	@Expose
	private Set<String> dataSourceHosts = new HashSet<>();
	
	@Expose
	protected String thumbnailUrl = "";
	
	@Expose
	protected String countryCode = "";
	
	@Expose
	protected String locationCode = "";
	
	@Expose
	protected String address = "";
	
	@Expose
	protected double latitude = 0;
	
	@Expose
	protected double longitude = 0;
	
	@Expose
	protected double radius = 0;
	
	@Expose
	protected double reachableArea = 0;
	
	@Expose
	protected int journeyLevel = 0;
	
	// default metrics to track
	@Expose
	protected Set<String> eventMetrics = new HashSet<String>(20);
	
	@Expose
	protected Set<String> keywords = new HashSet<>();
	
	@Expose
	protected String observerId = "";
	
	@Expose
	String journeyMapId = "";
	
	@Expose
	long totalProfile = 0;
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	TouchpointHubReport report = null;
	
	public TouchpointHub() {
		super();
	}
	
	public TouchpointHub(String name, int type, boolean firstPartyData, String url) {
		super();
		initTouchpoint(name, type, firstPartyData, url, 1, null);
	}

	public TouchpointHub(String name, int type, boolean firstPartyData, String url, int journeyLevel) {
		super();
		initTouchpoint(name, type, firstPartyData, url, journeyLevel, null);
	}
	
	public TouchpointHub(String name, int type, boolean firstPartyData, String url, int journeyLevel, String journeyMapId) {
		super();
		initTouchpoint(name, type, firstPartyData, url, journeyLevel, journeyMapId);
	}

	public void initTouchpoint(String name, int type, boolean firstPartyData, String url, int journeyLevel, String journeyMapId) {
		this.name = name;
		this.type = type;
		this.typeAsType = TouchpointType.getTypeAsText(type);
		this.firstPartyData = firstPartyData;
		this.url = url;
		this.hostname = UrlUtil.getHostName(url);
		this.dataSourceHosts.add(this.hostname);
		
		this.isRootNode = true;
		this.journeyLevel = journeyLevel;
		
		if(StringUtil.isEmpty(journeyMapId)) {
			this.journeyMapId = JourneyMap.DEFAULT_JOURNEY_MAP_ID;
		}
		else {
			this.journeyMapId = journeyMapId;
		}
		
		this.buildHashedId();
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if(StringUtil.isNotEmpty(name) && journeyLevel > 0) {
			if(this.type == TouchpointType.DATA_OBSERVER) {
				// only one leo data observer can existed !? 
				this.id = EventObserver.DEFAULT_EVENT_OBSERVER_ID;
			}
			else {
				String keyHints;
				if( this.journeyMapId.equals(JourneyMap.DEFAULT_JOURNEY_MAP_ID) ) {
					// to keep existing touchpoint ID
					keyHints = name;
				}
				else {
					keyHints = name + journeyMapId;
				}
				this.id = createId(this.id, keyHints);
			}
		}
		else {
			newIllegalArgumentException("name and journeyLevel is required");
		}
		return this.id;
	}
	
	public boolean isDataObserver() {
		return this.type == TouchpointType.DATA_OBSERVER;
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
		this.typeAsType = TouchpointType.getTypeAsText(type);
	}
	
	public String getTypeAsType() {
		return typeAsType;
	}

	public void setTypeAsType(String typeAsType) {
		this.typeAsType = typeAsType;
		this.type = TouchpointType.getTypeAsInt(typeAsType);
	}

	public boolean isFirstPartyData() {
		return firstPartyData;
	}

	public void setFirstPartyData(boolean firstPartyData) {
		this.firstPartyData = firstPartyData;
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

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getUrl() {
		if(url == null) {
			url = "";
		}
		return url;
	}

	public String getHostname() {
		if(StringUtil.isEmpty(hostname) && StringUtil.isNotEmpty(url)) {
			this.hostname = UrlUtil.getHostName(url);
		}
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setUrl(String url) {
		this.url = url;
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

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getLocationCode() {
		return locationCode;
	}

	public void setLocationCode(String locationCode) {
		this.locationCode = locationCode;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public double getReachableArea() {
		return reachableArea;
	}

	public void setReachableArea(double reachableArea) {
		this.reachableArea = reachableArea;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}

	public boolean isRootNode() {
		return isRootNode;
	}

	public void setRootNode(boolean isRootNode) {
		this.isRootNode = isRootNode;
	}
	
	public int getJourneyLevel() {
		return journeyLevel;
	}

	public void setJourneyLevel(int journeyLevel) {
		this.journeyLevel = journeyLevel;
	}

	public void clearEventMetrics() {
		if(eventMetrics != null) {
			eventMetrics.clear();
		}
	}
	
	public Set<String> getEventMetrics() {
		return eventMetrics;
	}

	public void setEventMetrics(Set<String> eventMetrics) {
		this.eventMetrics = eventMetrics;
	}
	
	public void setEventMetric(String eventName) {
		this.eventMetrics.add(eventName);
	}
	
	public String getJourneyMapId() {
		return journeyMapId;
	}
	
	public void setJourneyMapId(String journeyMapId) {
		if(StringUtil.isNotEmpty(journeyMapId)) {
			this.journeyMapId = journeyMapId;
		}
	}
	
	public void removeJourneyMapId() {
		this.journeyMapId = "";
	}

	public String getObserverId() {
		return observerId;
	}

	public void setObserverId(String observerId) {
		this.observerId = observerId;
	}

	@Override
	public int hashCode() {
		return StringUtil.safeString(id).hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TouchpointHub that = (TouchpointHub) o;
		return Objects.equals(id, that.id);
	}
	
	public long getTotalProfile() {
		return totalProfile;
	}

	public void setTotalProfile(long totalProfile) {
		if(totalProfile > 0) {
			this.totalProfile = totalProfile;	
		}
	}
	
	public TouchpointHubReport getReport() {
		return report;
	}

	public void setReport(TouchpointHubReport report) {
		this.report = report;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
}