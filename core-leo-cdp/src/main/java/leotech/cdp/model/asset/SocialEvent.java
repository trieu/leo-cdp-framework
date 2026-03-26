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
 * Social Event entity to enrich customer profile understanding. <br>
 * A social event represents real-world or virtual marketing activations such as a product launch, 
 * promotional campaign, webinar, conference, trade show, festival, concert, sports event, movie release, 
 * or software release. <br>
 * 
 * Tracking social events allows the CDP to map offline/online behavioral data to specific campaigns, brands, 
 * products, and services, enabling deep contextual analytics and targeted follow-up marketing. <br><br>
 * 
 * ArangoDB Collection: cdp_socialevent <br>
 * 
 * @author tantrieuf31
 * @since 2020
 */
public final class SocialEvent extends MeasurableItem {

	public static final String COLLECTION_NAME = getCdpCollectionName(SocialEvent.class);
	
	// Volatile for thread-safe lazy DB initialization
	static volatile ArangoCollection collection;

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
			synchronized (SocialEvent.class) {
				if (collection == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);

					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);

					// --------------------------------------------------------------------------------
					// ARANGODB 3.11 INDEX OPTIMIZATION (RocksDB Engine)
					// Removed 10+ standalone indexes that were creating massive I/O bottlenecks. 
					// Replaced with logical composite indexes that ArangoDB's RocksDB engine processes 
					// from Left-to-Right. Array indexes (`[*]`) are kept separate as required by Arango.
					// --------------------------------------------------------------------------------
					
					// Unique Lookups
					col.ensurePersistentIndex(Arrays.asList("slug"), new PersistentIndexOptions().unique(true));
					col.ensurePersistentIndex(Arrays.asList("fullUrl"), new PersistentIndexOptions().unique(true));

					// Core Organizational & Location Filtering
					col.ensurePersistentIndex(Arrays.asList("networkId", "contentClass", "locationCode"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("ownerId", "createdAt"), pIdxOpts);
					
					// Array Relational Mapping 
					col.ensurePersistentIndex(Arrays.asList("productIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("brands[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("groupIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("topicIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("categoryIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("keywords[*]"), pIdxOpts);
					
					// Campaign & Targeting Arrays
					col.ensurePersistentIndex(Arrays.asList("targetGeoLocations[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("targetSegmentIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("targetViewerIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("inCampaigns[*]"), pIdxOpts);
					
					// Full-text Engine for UI searches
					col.ensureFulltextIndex(Arrays.asList("title"), new FulltextIndexOptions().minLength(5));

					collection = col;
				}
			}
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