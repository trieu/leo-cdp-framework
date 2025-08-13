package leotech.cdp.model.asset;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.FulltextIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonObject;
import rfx.core.util.StringUtil;

/**
 * Social Event to get more information about customer profile
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class SocialEvent extends MeasurableItem {

	public static final String COLLECTION_NAME = getCdpCollectionName(SocialEvent.class);
	static ArangoCollection collection;

	@Expose
	Set<String> brands = new HashSet<>();
	
	// external product ID
	@Expose
	Set<String> productIds = new HashSet<>();
	
	@Expose
	Set<String> serviceIds = new HashSet<>();
	
	@Expose
	Set<String> inCampaigns = new HashSet<>(50);

	@Expose
	String fullUrl;
	
	@Expose
	String videoUrl;

	@Expose
	Date beginDate;
	
	@Expose
	Date endDate;

	@Expose
	String locationName;
	
	@Expose
	String locationCode;
	
	@Expose
	String locationAddress;

	@Override
	public ArangoCollection getDbCollection() {
		if (collection == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			collection = arangoDatabase.collection(COLLECTION_NAME);
			
			// index data for this class
			// productId is required for fast-lookup and fullUrl must be unique URL in this collection
			collection.ensurePersistentIndex(Arrays.asList("productId"), new PersistentIndexOptions().unique(true));
			collection.ensurePersistentIndex(Arrays.asList("fullUrl"), new PersistentIndexOptions().unique(true));
			collection.ensurePersistentIndex(Arrays.asList("brand"), new PersistentIndexOptions().unique(false));

			// ensure indexing key fields
			collection.ensurePersistentIndex(Arrays.asList("slug"), new PersistentIndexOptions().unique(true));
			collection.ensurePersistentIndex(Arrays.asList("ownerId"), new PersistentIndexOptions().unique(false));
			collection.ensureFulltextIndex(Arrays.asList("title"), new FulltextIndexOptions().minLength(5));
			collection.ensurePersistentIndex(Arrays.asList("createdAt"),new PersistentIndexOptions().unique(false));
			
			collection.ensurePersistentIndex(Arrays.asList("networkId", "contentClass"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("contentClass", "groupIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("contentClass", "categoryIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("networkId", "topicIds[*]"), new PersistentIndexOptions().unique(false));

			// array fields
			collection.ensurePersistentIndex(Arrays.asList("groupIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("topicIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("categoryIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("keywords[*]"), new PersistentIndexOptions().unique(false));

			// for marketing marketing with targeting
			collection.ensurePersistentIndex(Arrays.asList("targetGeoLocations[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("targetSegmentIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("targetViewerIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("inCampaigns[*]"), new PersistentIndexOptions().unique(false));
		}
		return collection;
	}

	public SocialEvent() {
		this.fullUrl = "";
	}
	
	public SocialEvent(String fullUrl, String title, String siteDomain, String extId) {
		this.fullUrl = fullUrl;
		this.title = title;
		this.productIds.add(extId);

		buildHashedId();
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.title) && StringUtil.isNotEmpty(this.fullUrl) && StringUtil.isNotEmpty(this.id);
	}

	public String getFullUrl() {
		return fullUrl;
	}

	public void setFullUrl(String fullUrl) {
		this.fullUrl = fullUrl;
	}
	
	public boolean isEmpty() {
		return StringUtil.isEmpty(this.id);
	}
	
	public Set<String> getInCampaigns() {
		return inCampaigns;
	}

	public void setInCampaigns(Set<String> inCampaigns) {
		this.inCampaigns = inCampaigns;
	}

	public void setActiveCampaign(String campaignId) {
		this.inCampaigns.add(campaignId);
	}
	
	public void unsetActiveCampaign(String campaignId) {
		this.inCampaigns.remove(campaignId);
	}

	public Set<String> getBrands() {
		return brands;
	}

	public void setBrands(Set<String> brands) {
		this.brands = brands;
	}

	public Set<String> getProductIds() {
		return productIds;
	}

	public void setProductIds(Set<String> productIds) {
		this.productIds = productIds;
	}

	public Set<String> getServiceIds() {
		return serviceIds;
	}

	public void setServiceIds(Set<String> serviceIds) {
		this.serviceIds = serviceIds;
	}

	public Date getBeginDate() {
		return beginDate;
	}

	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
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

	public String getVideoUrl() {
		return videoUrl;
	}

	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public String getLocationAddress() {
		return locationAddress;
	}

	public void setLocationAddress(String locationAddress) {
		this.locationAddress = locationAddress;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	
	@Override
	public void initItemDataFromJson(JsonObject paramJson) {
		setItemDataFromJson(this, paramJson);
		String fullUrl = paramJson.getString("fullUrl","");
		String productId = paramJson.getString("productId","");
		if(StringUtil.isNotEmpty(fullUrl) && StringUtil.isNotEmpty(productId)) {
			this.fullUrl = fullUrl;
			this.productIds.add(productId);
		}
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (StringUtil.isNotEmpty(fullUrl) && StringUtil.isNotEmpty(title) ) {
			String keyHint = fullUrl + "-" + title;
			this.id = createId(this.id, keyHint);
			
			this.slug = new Slugify().slugify(title);
			this.createdAt = new Date();
			this.updatedAt = this.createdAt;
			this.creationTime = this.createdAt.getTime();
			this.modificationTime = this.creationTime;
		} else {
			newIllegalArgumentException("title, productId and fullUrl SKU are required!");
		}
		return this.id; 
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

}
