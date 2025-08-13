package leotech.cdp.model.journey;

import java.util.Arrays;
import java.util.Date;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.GeoIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.openlocationcode.OpenLocationCode;

import leotech.cdp.model.analytics.EventCounter;
import leotech.cdp.model.analytics.ScoreCX;
import leotech.cdp.model.asset.AssetType;
import leotech.system.util.UrlUtil;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 * @since 2020
 * 
 * A touchpoint can be defined as any way a consumer can interact with a business, whether it be person-to-person, through a website, an app or any form of communication
 *
 */
public final class Touchpoint extends TouchpointHub implements Comparable<Touchpoint> {

	public static final String COLLECTION_NAME = getCdpCollectionName(Touchpoint.class);
	static ArangoCollection instance;

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();
			instance = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields for fast lookup
			instance.ensurePersistentIndex(Arrays.asList("url", "name"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("name"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("url"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("hostname"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("collectionId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("type"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("typeAsType"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("observerId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("parentId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("locationCode"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("keywords[*]"),  new PersistentIndexOptions().unique(false));
			instance.ensureGeoIndex(Arrays.asList("latitude", "longitude"), new GeoIndexOptions());
			instance.ensurePersistentIndex(Arrays.asList("isRootNode"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("parentId", "createdAt"),new PersistentIndexOptions().unique(false));
			
			instance.ensurePersistentIndex(Arrays.asList(
					"eventCounter.pageView","eventCounter.itemView","eventCounter.surveyView","eventCounter.clickDetails","eventCounter.madePayment","eventCounter.qrCodeScan"
			), new PersistentIndexOptions().unique(false));
		}
		return instance;
	}

	@Expose
	String collectionId = "";// IAB-17 or category topics ...

	/**
	 * E.g: a store in the shopping mall, a teacher in a school, a sale agent in a
	 * company
	 */
	@Expose
	String parentId = "";//

	/**
	 * linked creative content to owned media touch-point, to show media
	 */
	@Expose
	String assetContentId = "";

	/**
	 * the cost of touchpoint, e.g: cost to have convenient physical location for
	 * customer, cost to show media for customer acquisition
	 */
	@Expose
	double unitCost = 0;

	@Expose
	EventCounter eventCounter = new EventCounter();

	@Expose
	ScoreCX scoreCX = new ScoreCX();

	@Expose
	int assetType = AssetType.UNCLASSIFIED;

	@Expose
	int partitionId;

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.name) && this.type >= 0 && StringUtil.isNotEmpty(this.url) ;
	}

	////////////////

	public Touchpoint() {
		
	}
	
	/**
	 * create a RootNode Node 
	 * 
	 * @param touchpointHub
	 */
	public Touchpoint(TouchpointHub touchpointHub) {
		this.isRootNode = true;
		this.id = "tph_"+touchpointHub.getId();
		this.name = touchpointHub.getName();
		this.type = touchpointHub.getType();
		this.url = touchpointHub.getUrl();
		this.hostname = touchpointHub.getHostname();
		this.firstPartyData = touchpointHub.isFirstPartyData();
		this.journeyMapId = touchpointHub.getJourneyMapId();
		this.observerId = touchpointHub.getObserverId();
		this.locationCode = touchpointHub.getLocationCode();
		this.address = touchpointHub.getAddress();
		this.thumbnailUrl = touchpointHub.getThumbnailUrl();
		this.reachableArea = touchpointHub.getReachableArea();
		this.latitude = touchpointHub.getLatitude();
		this.longitude = touchpointHub.getLongitude();
		this.radius = touchpointHub.getRadius();
		this.keywords = touchpointHub.getKeywords(); 
	}

	/**
	 * for online touch-point from internal API
	 * 
	 * @param name
	 * @param type
	 * @param firstPartyData
	 * @param url
	 * @param collectionId
	 */
	public Touchpoint(String name, int type, String url) {
		super();
		this.createdAt = new Date();
		this.name = name;
		this.type = type;
		this.typeAsType = TouchpointType.getTypeAsText(type);
		this.url = url;
		this.hostname = UrlUtil.getHostName(url);
		if(StringUtil.isEmpty(name) && StringUtil.isNotEmpty(url)) {
			this.name = url;
		}
		buildHashedId();
	}

	/**
	 * for online touch-point from internal API
	 * 
	 * @param name
	 * @param type
	 * @param firstPartyData
	 * @param url
	 * @param firstPartyData
	 */
	public Touchpoint(String name, int type, String url, boolean firstPartyData) {
		super();
		this.createdAt = new Date();
		this.name = name;
		this.type = type;
		this.typeAsType = TouchpointType.getTypeAsText(type);
		this.url = url;
		this.hostname = UrlUtil.getHostName(url);
		this.firstPartyData = firstPartyData;
		buildHashedId();
	}

	/**
	 * for online touch-point from internal API
	 * 
	 * @param name
	 * @param type
	 * @param firstPartyData
	 * @param url
	 * @param firstPartyData
	 */
	public Touchpoint(Date createdAt, String name, int type, String url, boolean firstPartyData) {
		super();
		this.createdAt = createdAt;
		this.name = name;
		this.type = type;
		this.typeAsType = TouchpointType.getTypeAsText(type);
		this.url = url;
		this.hostname = UrlUtil.getHostName(url);
		this.firstPartyData = firstPartyData;
		buildHashedId();
	}

	/**
	 * for online touch-point from public API
	 * 
	 * @param title
	 * @param type
	 * @param firstPartyData
	 * @param url
	 * @param collectionId
	 */
	public Touchpoint(int type, String url) {
		super();
		this.createdAt = new Date();
		this.name = "Web";
		this.type = type;
		this.typeAsType = TouchpointType.getTypeAsText(type);
		this.url = url;
		this.hostname = UrlUtil.getHostName(url);
		buildHashedId();
	}

	/**
	 * for offline touch-point
	 * 
	 * @param name
	 * @param type
	 * @param firstPartyData
	 * @param locationCode
	 * @param address
	 * @param collectionId
	 */
	public Touchpoint(String name, int type, String address, String locationCode) {
		super();
		this.createdAt = new Date();
		this.name = name;
		this.type = type;
		this.typeAsType = TouchpointType.getTypeAsText(type);
		this.address = address;
		this.locationCode = locationCode;
		
		OpenLocationCode.CodeArea decoded = new OpenLocationCode(locationCode).decode();
		this.latitude = decoded.getCenterLatitude();
		this.longitude =  decoded.getCenterLongitude();
		this.url = "https://plus.codes/" + locationCode;
		buildHashedId();
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (StringUtil.isNotEmpty(name) && StringUtil.isNotEmpty(url)) {
			String keyHint = parentId + type + locationCode + name + url + isRootNode;
			this.id = createId(this.id, keyHint);
		} else {
			newIllegalArgumentException("name and url is required");
		}
		return this.id;
	}

	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public double getUnitCost() {
		return unitCost;
	}

	public void setUnitCost(double unitCost) {
		this.unitCost = unitCost;
	}

	public int getAssetType() {
		return assetType;
	}

	public void setAssetType(int assetType) {
		this.assetType = assetType;
	}

	public String getAssetContentId() {
		return assetContentId;
	}

	public void setAssetContentId(String assetContentId) {
		this.assetContentId = assetContentId;
	}

	public int getPartitionId() {
		return partitionId;
	}

	public void setPartitionId(int partitionId) {
		this.partitionId = partitionId;
	}

	public ScoreCX getScoreCX() {
		return scoreCX;
	}

	public void setScoreCX(ScoreCX scoreCX) {
		this.scoreCX = scoreCX;
	}

	public void setCxPositive(int positive) {
		this.scoreCX.setPositive(positive);
	}

	public void setCxNeutral(int neutral) {
		this.scoreCX.setNeutral(neutral);
	}

	public void setCxNegative(int negative) {
		this.scoreCX.setNegative(negative);
	}

	public EventCounter getEventCounter() {
		return eventCounter;
	}

	public void setEventCounter(EventCounter eventCounter) {
		this.eventCounter = eventCounter;
	}

	@Override
	public int compareTo(Touchpoint o) {
		return this.createdAt.compareTo(o.getCreatedAt());
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
