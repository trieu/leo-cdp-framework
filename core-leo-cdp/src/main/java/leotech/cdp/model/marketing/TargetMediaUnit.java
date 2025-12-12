package leotech.cdp.model.marketing;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.starter.router.ObserverHttpRouter;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * the data object, contains all metadata of Profile2Product and Profile2Content
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class TargetMediaUnit extends PersistentObject implements Comparable<TargetMediaUnit> {
	
	public static final String SHORT_LINK_CLICK = BehavioralEvent.General.SHORT_LINK_CLICK;
	public static final String QR_CODE_SCAN = BehavioralEvent.General.QR_CODE_SCAN;

	public static final String COLLECTION_NAME = getCdpCollectionName(TargetMediaUnit.class);
	static ArangoCollection dbCollection;
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (StringUtil.isNotEmpty(landingPageUrl) && StringUtil.isNotEmpty(eventMetaDataId) ) {
			String keyHint = refCampaignId +  refSegmentId + refProfileId + landingPageUrl + eventMetaDataId + refProductItemId;
			this.id = createId(this.id, keyHint);
		} else {
			System.err.println(new Gson().toJson(this));
			newIllegalArgumentException("landingPageUrl and eventMetaDataId are required!");
		}
		return this.id;
	}

	@Key
	@Expose
	String id;
	
	@Expose
	protected Date createdAt;
	
	@Expose
	protected Date updatedAt;
	
	@Expose
	protected Date expiredAt;
	
	@Expose
	protected String name = "";

	@Expose
	protected int status = 0; // 0 is not read, 1 is clicked , 2 is read, 3 is got feedback from targeted profile
	
	@Expose
	protected String refCampaignId= "";
	
	@Expose
	protected String refProductItemId= "";
	
	@Expose
	protected String refContentItemId= "";
	
	@Expose
	protected String refProfileId = "";
	
	@Expose
	protected String refSegmentId = "";
	
	@Expose
	protected String refVisitorId = "";
	
	@Expose
	protected String refSocialEventId = "";
	
	@Expose
	protected String refTouchpointId = "";
	
	@Expose
	protected String refTouchpointHubId = "";
	
	@Expose
	protected String refObserverId = "";
	
	@Expose
	protected String refAffiliateId = "";
	
	@Expose
	protected String landingPageName = "";
	
	@Expose
	protected String landingPageUrl = "";
	
	@Expose
	protected String imageUrl = "";
	
	@Expose
	protected String videoUrl = "";
	
	@Expose
	protected String trackingLinkUrl = "";
	
	@Expose
	protected String eventMetaDataId = "";
	
	@Expose
	protected double ratingScore = 0;
	
	@Expose
	protected Map<String,String> customData = new HashMap<>();
	
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	@Expose
	private ProductItem productItem;
	
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	@Expose
	private AssetContent contentItem;
	
	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.id) && StringUtil.isNotEmpty(this.landingPageUrl) && StringUtil.isNotEmpty(eventMetaDataId);
	}
	
	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			dbCollection = arangoDatabase.collection(COLLECTION_NAME);
			dbCollection.ensurePersistentIndex(Arrays.asList("refProfileId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("refProductItemId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("refCampaignId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("landingPageUrl"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("eventMetaDataId"), new PersistentIndexOptions().unique(false));
		}
		return dbCollection;
	}
	
	public TargetMediaUnit() {
		// gson and default
	}

	public TargetMediaUnit(String name, double ratingScore) {
		super();
		this.name = name;
		this.ratingScore = ratingScore;
	}

	/**
	 * for targeted media unit in specific campaign 
	 * 
	 * @param campaignId
	 * @param productItemId
	 * @param imageUrl
	 * @param videoUrl
	 * @param refProfileId
	 * @param landingPageUrl
	 * @param landingPageName
	 * @param eventMetaDataId
	 */
	public TargetMediaUnit(String campaignId, String productItemId, String imageUrl, String videoUrl, String refProfileId, String landingPageUrl,  String landingPageName, String eventMetaDataId) {
		super();
		this.createdAt = new Date();
		
		this.refCampaignId = campaignId;
		this.refProductItemId = productItemId;
		this.imageUrl = imageUrl;
		this.videoUrl = videoUrl;
		this.refProfileId = refProfileId;
		this.landingPageUrl = landingPageUrl;
		
		this.landingPageName = landingPageName;
		this.eventMetaDataId = eventMetaDataId;
		
		this.buildHashedId();
		this.buildTrackingLinkUrl();
	}
	
	/**
	 * SHORT_LINK_CLICK targeted media unit
	 * 
	 * @param productItemId
	 * @param imageUrl
	 * @param videoUrl
	 * @param refProfileId
	 * @param landingPageUrl
	 * @param landingPageName
	 */
	public static TargetMediaUnit fromProduct(String productItemId, String imageUrl, String videoUrl, String refProfileId, String refVisitorId, String landingPageUrl, String landingPageName) {
		TargetMediaUnit t = new TargetMediaUnit();
		t.createdAt = new Date();
		
		t.refProductItemId = productItemId;
		t.imageUrl = imageUrl;
		t.videoUrl = videoUrl;
		t.refProfileId = refProfileId;
		t.refVisitorId = refVisitorId;
		t.landingPageUrl = landingPageUrl;
		
		t.landingPageName = landingPageName;
		t.eventMetaDataId = SHORT_LINK_CLICK;
		
		t.buildHashedId();
		t.buildTrackingLinkUrl();
		return t;
	}
	
	public static TargetMediaUnit fromContent(String contentItemId, String imageUrl, String videoUrl, String refProfileId, String refVisitorId, String landingPageUrl, String landingPageName) {
		TargetMediaUnit t = new TargetMediaUnit();
		t.createdAt = new Date();
		
		t.refContentItemId = contentItemId;
		t.imageUrl = imageUrl;
		t.videoUrl = videoUrl;
		t.refProfileId = refProfileId;
		t.refVisitorId = refVisitorId;
		t.landingPageUrl = landingPageUrl;
		
		t.landingPageName = landingPageName;
		t.eventMetaDataId = SHORT_LINK_CLICK;
		
		t.buildHashedId();
		t.buildTrackingLinkUrl();
		return t;
	}
	
	/**
	 *  targeted media unit constructor for QR Code
	 * 
	 * @param landingPageUrl
	 * @param landingPageName
	 */
	public TargetMediaUnit(String touchpointHubId, String observerId, String landingPageUrl, String landingPageName) {
		super();
		this.createdAt = new Date();
		this.refTouchpointHubId = touchpointHubId;
		this.refObserverId = observerId;
		this.landingPageUrl = landingPageUrl;
		
		if(StringUtil.isEmpty(landingPageName)) {
			this.landingPageName = "Target Media for the observer "+observerId;
		} else {
			this.landingPageName = landingPageName;
		}
		
		this.eventMetaDataId = QR_CODE_SCAN;
		this.buildHashedId();
		this.buildTrackingLinkUrl();
	}
	

	
	void buildTrackingLinkUrl() {
		if(StringUtil.isNotEmpty(this.id)) {
			if(SHORT_LINK_CLICK.equals(this.eventMetaDataId)) {
				StringBuilder s = new StringBuilder();
				s.append(ObserverHttpRouter.BASE_URL_TARGET_MEDIA_CLICK_TRACKING).append(this.id);
				this.trackingLinkUrl = s.toString();
			}
			else if(QR_CODE_SCAN.equals(this.eventMetaDataId)) {
				StringBuilder s = new StringBuilder(TouchpointHub.URL_LEO_DATA_OBSERVER);
				s.append(ObserverHttpRouter.PREFIX_TARGET_MEDIA_QR_CODE_TRACKING).append(this.id);
				this.trackingLinkUrl = s.toString();
			}
		}
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRefProductItemId() {
		return refProductItemId;
	}

	public void setRefProductItemId(String refProductItemId) {
		this.refProductItemId = refProductItemId;
	}

	public String getRefProfileId() {
		return refProfileId;
	}

	public void setRefProfileId(String refProfileId) {
		this.refProfileId = refProfileId;
	}

	public String getRefTouchpointId() {
		return refTouchpointId;
	}

	public void setRefTouchpointId(String refTouchpointId) {
		this.refTouchpointId = refTouchpointId;
	}

	public String getRefTouchpointHubId() {
		return refTouchpointHubId;
	}

	public void setRefTouchpointHubId(String refTouchpointHubId) {
		this.refTouchpointHubId = refTouchpointHubId;
	}

	public String getRefObserverId() {
		return refObserverId;
	}

	public void setRefObserverId(String refObserverId) {
		this.refObserverId = refObserverId;
	}

	public String getLandingPageUrl() {
		return landingPageUrl;
	}

	public void setLandingPageUrl(String landingPageUrl) {
		this.landingPageUrl = landingPageUrl;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getLandingPageName() {
		return landingPageName;
	}

	public void setLandingPageName(String landingPageName) {
		this.landingPageName = landingPageName;
	}
	
	

	public String getRefAffiliateId() {
		return refAffiliateId;
	}

	public void setRefAffiliateId(String refAffiliateId) {
		this.refAffiliateId = refAffiliateId;
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
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getRefCampaignId() {
		return refCampaignId;
	}

	public void setRefCampaignId(String refCampaignId) {
		this.refCampaignId = refCampaignId;
	}

	public String getRefVisitorId() {
		if(refVisitorId == null) {
			refVisitorId = "";
		}
		return refVisitorId;
	}

	public void setRefVisitorId(String refVisitorId) {
		this.refVisitorId = refVisitorId;
	}

	public String getTrackingLinkUrl() {
		return trackingLinkUrl;
	}
	
	public String getShortUrl() {
		StringBuilder s = new StringBuilder(TouchpointHub.URL_LEO_DATA_OBSERVER);
		s.append(ObserverHttpRouter.PREFIX_TARGET_MEDIA_CLICK_TRACKING).append(this.id);
		return s.toString();
	}

	public void setTrackingLinkUrl(String trackingLinkUrl) {
		this.trackingLinkUrl = trackingLinkUrl;
	}

	public Date getExpiredAt() {
		return expiredAt;
	}

	public void setExpiredAt(Date expiredAt) {
		this.expiredAt = expiredAt;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getVideoUrl() {
		return videoUrl;
	}

	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	public String getEventMetaDataId() {
		return eventMetaDataId;
	}

	public void setEventMetaDataId(String eventMetaDataId) {
		this.eventMetaDataId = eventMetaDataId;
	}

	public double getRatingScore() {
		return ratingScore;
	}

	public void setRatingScore(double ratingScore) {
		this.ratingScore = ratingScore;
	}

	public String getRefContentItemId() {
		return refContentItemId;
	}

	public void setRefContentItemId(String refContentItemId) {
		if(refContentItemId != null) {
			this.refContentItemId = refContentItemId;
		}
	}

	public String getRefSocialEventId() {
		return refSocialEventId;
	}

	public void setRefSocialEventId(String refSocialEventId) {
		if(refSocialEventId != null) {
			this.refSocialEventId = refSocialEventId;
		}
	}
	
	public String getRefSegmentId() {
		return refSegmentId;
	}

	public void setRefSegmentId(String refSegmentId) {
		if(refSegmentId != null) {
			this.refSegmentId = refSegmentId;
		}
	}

	public ProductItem getProductItem() {
		return productItem;
	}

	public void setProductItem(ProductItem productItem) {
		if(productItem != null) {
			this.productItem = productItem;
		}
	}

	public AssetContent getContentItem() {
		return contentItem;
	}

	public void setContentItem(AssetContent contentItem) {
		if(contentItem != null) {
			this.contentItem = contentItem;
		}
	}

	public Map<String, String> getCustomData() {
		return customData;
	}

	public void setCustomData(Map<String, String> customData) {
		if(customData != null) {
			this.customData = customData;
		}
	}
	
	public void setCustomData(String key, String value) {
		if(StringUtil.isNotEmpty(key) && StringUtil.isNotEmpty(value)) {
			this.customData.put(key, value);
		}
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public int compareTo(TargetMediaUnit o) {
		if(this.getRatingScore() < o.getRatingScore()) {
			return 1;
		}
		else if(this.getRatingScore() > o.getRatingScore()) {
			return -1;
		}
		return 0;
	}
	
	public void clearPrivateData() {
		this.createdAt = null;
		this.refProfileId = null;
		this.refProductItemId = null;
		this.refContentItemId = null;
		this.refObserverId= null;
		this.refSegmentId = null;
		this.refTouchpointHubId = null;
		this.refTouchpointId = null;
		this.refSocialEventId = null;
		this.refVisitorId = null;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
}
