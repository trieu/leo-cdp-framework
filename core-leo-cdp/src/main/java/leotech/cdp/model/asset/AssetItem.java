package leotech.cdp.model.asset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import leotech.starter.router.UploaderHttpRouter;
import leotech.system.model.DataPrivacy;
import leotech.system.util.QrCodeUtil;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.database.PersistentObject;
import leotech.system.version.SystemMetaData;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.StringUtil;

/**
 * core abstract class for asset item data persistence
 * 
 * @author tantrieuf31
 * @since 2018 
 */
public abstract class AssetItem extends PersistentObject implements Comparable<AssetItem> {

	public static final int MAX_LENGTH_OF_SLUG = 72;

	@Key
	@Expose
	protected String id = null;
	
	@Expose
	protected List<String> groupIds = new ArrayList<>(50);

	@Expose
	protected String title;
	
	@Expose
	protected String description = ""; // maximum 500 characters
	
	@Expose
	protected int type = ContentType.META_DATA;
	
	@Expose
	protected int assetType = AssetType.UNCLASSIFIED;

	@Expose
	protected String contentClass = ""; // class name. E.g: project, product, item, youtube-video, facebook-video,..

	@Expose
	protected String mediaInfo = "";
	
	@Expose
	List<MediaInfoUnit> mediaInfoUnits = new ArrayList<>(); // a collection of images or videos or multimedia data

	@Expose
	protected int status = AssetItemStatus.DRAFTED;

	@Expose
	protected long creationTime = DateTimeUtil.currentUnixTimestampInLong();

	@Expose
	protected long modificationTime;

	@Expose
	protected long publishingTime;
	
	@Expose
	protected Date createdAt = new Date();

	@Expose
	protected Date updatedAt = new Date();

	@Expose
	protected long destroyedTime;
	
	// for SEO friendly, maximum 255 characters
	@Expose
	protected String slug = ""; 
	
	// for mobile QR code scan
	@Expose
	protected String qrCodeUrl = ""; 
	
	@Expose
	protected String shortLinkUrl = ""; 

	// default headline image of media node
	@Expose
	protected String headlineImageUrl = "";

	// default headline short video about content
	@Expose
	protected String headlineVideoUrl = "";

	// KEY is Image URL, VALUE is the label of image
	@Expose
	protected Map<String, String> headlineImages = new HashMap<>(20);

	// is defined by user-generated
	@Expose
	protected List<String> topicIds = new ArrayList<>(20);

	// is defined by Administrator or authorized users
	@Expose
	protected List<String> categoryIds = new ArrayList<>(10);

	@Expose
	protected Set<String> keywords = new HashSet<>(100);

	@Expose
	protected int networkId = 0;

	// the userId or botId
	@Expose
	protected String ownerId = "";
	
	@Expose
	protected boolean systemAsset = false;
	
	@Expose
	boolean isAdItem;

	@Expose
	protected int privacyStatus = DataPrivacy.PUBLIC;

	// for the order in ranking
	@Expose
	protected long rankingScore = 0;

	@Expose
	protected List<String> targetGeoLocations = new ArrayList<>();

	@Expose
	protected List<String> targetSegmentIds = new ArrayList<>();

	@Expose
	protected List<String> targetViewerIds = new ArrayList<>();
	
	// custom field for extending data
	@Expose
	protected Map<String, String> customData = new HashMap<>(100);
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected Map<String, AssetCategory> assetCategories = new HashMap<>();
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected Map<String, AssetGroup> assetGroups = new HashMap<>();

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected AssetItem() {
		// json
	}

	public void initNewItem(String categoryId, String groupId, String title, String mediaInfo,  int type, String ownerId) {
		initRequiredData(categoryId, groupId, title, mediaInfo, type, ownerId);
	}
	
	public void initNewGroup(String categoryId, String title, String mediaInfo, int type, String ownerId) {
		initRequiredData(categoryId, null, title, mediaInfo, type, ownerId);
	}
	
	public void initRequiredData(String categoryId, String groupId, String title, String mediaInfo, int type, String ownerId) {
		if(categoryId != null) {
			this.categoryIds.add(categoryId);
		}
		
		if(groupId != null) {
			this.groupIds.add(groupId);
		}
		
		setTitle(title);
		
		this.createdAt = new Date();
		this.updatedAt = new Date();
		this.creationTime = this.createdAt.getTime();
		this.modificationTime = this.updatedAt.getTime();
		
		this.mediaInfo = mediaInfo;
		this.type = type;
		this.ownerId = ownerId;
		
		this.buildHashedId();
	}
	
	@Override
	public abstract String buildHashedId() throws IllegalArgumentException;

	@Override
	public boolean dataValidation() {
		boolean ok = StringUtil.isNotEmpty(this.id) && StringUtil.isNotEmpty(this.title);
		if (ok) {
			return true;
		}
		throw new IllegalArgumentException("The data is not ready, title is empty ");
	}

	protected static ArangoCollection getCollection(ArangoCollection collection, String colName) throws ArangoDBException {
		if (collection == null) {
			ArangoDatabase arangoDatabase = ArangoDbUtil.getCdpDatabase();

			collection = arangoDatabase.collection(colName);

			// ensure indexing key fields
			collection.ensurePersistentIndex(Arrays.asList("slug"), new PersistentIndexOptions().unique(true));
			collection.ensurePersistentIndex(Arrays.asList("ownerId"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("assetType"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("createdAt"),new PersistentIndexOptions().unique(false));
			
			collection.ensurePersistentIndex(Arrays.asList("networkId", "contentClass"), new PersistentIndexOptions().unique(false));
			
			collection.ensurePersistentIndex(Arrays.asList("assetType", "categoryIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("contentClass", "categoryIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("networkId", "topicIds[*]"), new PersistentIndexOptions().unique(false));

			// array fields
			collection.ensurePersistentIndex(Arrays.asList("topicIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("categoryIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("keywords[*]"), new PersistentIndexOptions().unique(false));

			// for micro-targeting media
			collection.ensurePersistentIndex(Arrays.asList("targetGeoLocations[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("targetSegmentIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("targetViewerIds[*]"), new PersistentIndexOptions().unique(false));
			
			collection.ensurePersistentIndex(Arrays.asList("contentClass", "groupIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("groupIds[*]"), new PersistentIndexOptions().unique(false));
		}
		return collection;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	public boolean isContent() {
		boolean c =  this.isAdItem || assetType == AssetType.AD_BANNER || assetType == AssetType.AD_VIDEO || assetType == AssetType.SOCIAL_EVENT || assetType == AssetType.SHORT_URL_LINK;
		if(c) {
			return true;
		}
		return assetType == AssetType.HTML_LANDING_PAGE || assetType == AssetType.CONTENT_ITEM_CATALOG || assetType == AssetType.SOCIAL_MEDIA_CONTENT || assetType == AssetType.VIDEO_CATALOG;
	}

	public boolean isProduct() {
		return assetType == AssetType.PRODUCT_ITEM_CATALOG || assetType == AssetType.SERVICE_ITEM_CATALOG;
	}
	
	public boolean isTemplate() {
		return assetType == AssetType.WEB_HTML_CONTENT || assetType == AssetType.EMAIL_CONTENT || assetType == AssetType.TEXT_MESSAGE_CONTENT || assetType == AssetType.FEEDBACK_FORM || assetType == AssetType.GAMIFICATION;
	}
	
	public boolean isPresentation() {
		return assetType == AssetType.PRESENTATION_ITEM_CATALOG;
	}
		
	public boolean isShortUrlLink() {
		return assetType == AssetType.SHORT_URL_LINK || type == ContentType.WEB_URL;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public List<String> getGroupIds() {
		return groupIds;
	}

	public void setGroupIds(List<String> groupIds) {
		this.groupIds = groupIds;
	}

	public void setGroupId(String groupId) {
		this.groupIds.add(groupId);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMediaInfo() {
		if(mediaInfo == null) {
			return "";
		}
		return mediaInfo;
	}

	public void setMediaInfo(String mediaInfo) {
		this.mediaInfo = mediaInfo;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public long getModificationTime() {
		return modificationTime;
	}

	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}

	public long getPublishingTime() {
		return publishingTime;
	}

	public void setPublishingTime(long publishingTime) {
		this.publishingTime = publishingTime;
	}

	public long getDestroyedTime() {
		return destroyedTime;
	}

	public void setDestroyedTime(long destroyedTime) {
		this.destroyedTime = destroyedTime;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public int getPrivacyStatus() {
		return privacyStatus;
	}

	public void setPrivacyStatus(int privacyStatus) {
		this.privacyStatus = privacyStatus;
	}

	public Map<String, String> getCustomData() {
		return customData;
	}

	public void setCustomData(Map<String, String> customData) {
		if(customData != null) {
			this.customData = customData;
		}
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getQrCodeUrl() {
		return qrCodeUrl;
	}

	public void setQrCodeUrl(String qrCodeUrl) {
		this.qrCodeUrl = qrCodeUrl;
	}
	
	public void createQrCodeUrlFromShortLinkUrl(String prefix) {
		if(StringUtil.isNotEmpty(this.shortLinkUrl)) {
			String qrCodeUrl = QrCodeUtil.generate(prefix, this.shortLinkUrl);
			this.qrCodeUrl = qrCodeUrl;
		} else {
			throw new IllegalArgumentException("shortLinkUrl is NULL");
		}
	}
	

	public String getShortLinkUrl() {
		return shortLinkUrl;
	}

	public void setShortLinkUrl(String shortLinkUrl) {
		this.shortLinkUrl = shortLinkUrl;
	}

	public void createShortLinkUrlFromTargetMediaUnit(String targetMediaUnitId) {
		this.shortLinkUrl =  "https://"+ SystemMetaData.DOMAIN_CDP_OBSERVER +"/ct/" + targetMediaUnitId;
	}
	
	public void createShortLinkUrlFromSlug(String prefix) {
		this.shortLinkUrl =  "https://"+ SystemMetaData.DOMAIN_CDP_OBSERVER + prefix + this.slug;
	}

	public String getHeadlineImageUrl() {
		if (headlineImages.size() > 0 && StringUtil.isEmpty(headlineImageUrl)) {
			Object firstImageKey = 0;
			headlineImageUrl = headlineImages.get(firstImageKey);
		}
		if (headlineImageUrl.startsWith(UploaderHttpRouter.UPLOADED_FILES_LOCATION)) {
			headlineImageUrl = UploaderHttpRouter.STATIC_BASE_URL + headlineImageUrl;
		}
		return headlineImageUrl;
	}
	
	public void clearKeywords() {
		if(keywords != null) {
			keywords.clear();
		}
	}

	public Set<String> getKeywords() {
		return keywords;
	}
	
	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}

	public void setKeyword(String keyword) {
		this.keywords.add(keyword);
	}

	///////////////////////////////////////////////////


	public int getNetworkId() {
		return networkId;
	}

	public void setNetworkId(int networkId) {
		this.networkId = networkId;
	}

	public void setAssetCategories(Map<String, AssetCategory> assetCategories) {
		this.assetCategories = assetCategories;
	}

	public void setAssetGroups(Map<String, AssetGroup> assetGroups) {
		this.assetGroups = assetGroups;
	}

	public List<String> getCategoryIds() {
		return categoryIds;
	}

	public void setCategoryIds(List<String> categoryIds) {
		this.categoryIds = categoryIds;
	}

	public void setCategoryId(String categoryId) {
		this.categoryIds.add(categoryId);
	}

	public List<String> getTopicIds() {
		return topicIds;
	}

	public void setTopicIds(List<String> topicIds) {
		this.topicIds = topicIds;
	}

	public void setTopicId(String topicId) {
		this.topicIds.add(topicId);
	}

	public long getRankingScore() {
		return rankingScore;
	}

	public void setRankingScore(long rankingScore) {
		this.rankingScore = rankingScore;
	}

	public List<String> getTargetGeoLocations() {
		return targetGeoLocations;
	}

	public void setTargetGeoLocations(List<String> targetGeoLocations) {
		this.targetGeoLocations = targetGeoLocations;
	}

	public String getContentClass() {
		return contentClass;
	}

	public void setContentClass(String contentClass) {
		this.contentClass = contentClass;
	}

	public Map<String, String> getHeadlineImages() {
		return headlineImages;
	}

	public void setHeadlineImages(Map<String, String> headlineImages) {
		this.headlineImages.clear();
		this.headlineImages = headlineImages;
	}

	public List<MediaInfoUnit> getMediaInfoUnits() {
		return mediaInfoUnits;
	}

	public void setMediaInfoUnits(List<MediaInfoUnit> mediaInfoUnits) {
		this.mediaInfoUnits = mediaInfoUnits;
	}

	public void setMediaInfoUnit(MediaInfoUnit mediaInfoUnit) {
		this.mediaInfoUnits.add(mediaInfoUnit);
	}

	public String getHeadlineVideoUrl() {
		return headlineVideoUrl;
	}

	public void setHeadlineVideoUrl(String headlineVideoUrl) {
		this.headlineVideoUrl = headlineVideoUrl;
	}

	public void setHeadlineImageUrl(String imageUrl) {
		if(StringUtil.isNotEmpty(imageUrl)) {
			this.headlineImageUrl = imageUrl;
			if (!headlineImages.containsKey(imageUrl)) {
				headlineImages.put(imageUrl, "");
			}
		}
	}

	public void buildDefaultHeadlineImage() {
		if (headlineImages.size() > 0 && StringUtil.isEmpty(this.headlineImageUrl) ) {
			String url = null;
			Set<String> keys = this.headlineImages.keySet();
			for (String imgUri : keys) {
				String caption = this.headlineImages.get(imgUri).trim();
				if (caption.equalsIgnoreCase("headline")) {
					url = imgUri;
					break;
				}
			}
			if (url == null) {
				url = headlineImages.keySet().iterator().next();
			}
			this.headlineImageUrl = url;
		}
	}

	/**
	 * this method is only for Content Delivery to client
	 * 
	 * @param headlineOnly
	 */
	public void compactDataForList(boolean headlineOnly) {
		buildDefaultHeadlineImage();
		if (headlineOnly) {
			this.mediaInfo = "";
			if (mediaInfoUnits != null) {
				this.mediaInfoUnits.clear();
			}
		}
	}

	public boolean isEditable(String viewerId) {
		// if it is not private, authenticated system user can edit OR the owner of content
		return this.privacyStatus != DataPrivacy.PRIVATE || this.ownerId.equals(viewerId);
	}
	
	public int getAssetType() {
		return assetType;
	}

	public void setAssetType(int assetType) {
		this.assetType = assetType;
	}

	@Override
	public int compareTo(AssetItem o2) {
		AssetItem o1 = this;
		if (o1.getModificationTime() > o2.getModificationTime()) {
			return 1;
		} else if (o1.getModificationTime() < o2.getModificationTime()) {
			return -1;
		}
		return 0;
	}
	

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
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
		this.modificationTime = updatedAt.getTime();
	}

	public List<String> getTargetSegmentIds() {
		return targetSegmentIds;
	}

	public void setTargetSegmentIds(List<String> targetSegmentIds) {
		this.targetSegmentIds = targetSegmentIds;
	}

	public List<String> getTargetViewerIds() {
		return targetViewerIds;
	}

	public void setTargetViewerIds(List<String> targetViewerIds) {
		this.targetViewerIds = targetViewerIds;
	}

	public boolean isSystemAsset() {
		return systemAsset;
	}

	public void setSystemAsset(boolean systemAsset) {
		this.systemAsset = systemAsset;
	}

	public Map<String, AssetCategory> getAssetCategories() {
		return assetCategories;
	}
	
	public void setAssetCategory(String catId, AssetCategory cat) {
		this.assetCategories.put(catId, cat);
	}

	public Map<String, AssetGroup> getAssetGroups() {
		return assetGroups;
	}

	public void setAssetGroup(String groupId, AssetGroup group) {
		this.assetGroups.put(groupId, group);
	}
	
	public boolean isAdItem() {
		return isAdItem;
	}

	public void setAdItem(boolean isAdItem) {
		this.isAdItem = isAdItem;
	}

	abstract public void initItemDataFromJson(JsonObject paramJson);
	
	public static void setItemDataFromJson(AssetItem item, JsonObject paramJson) {
		String categoryId = paramJson.getString("categoryId", "");
		item.setCategoryIds(Arrays.asList(categoryId));
		
		String title = paramJson.getString("title", "");
		item.setTitle(title);
		
		String mediaInfo = paramJson.getString("mediaInfo", "");
		item.setMediaInfo(mediaInfo);
		
		int type = paramJson.getInteger("type", ContentType.HTML_TEXT);
		item.setType(type);
		
		int assetType = paramJson.getInteger("assetType", AssetType.HTML_LANDING_PAGE);
		item.setAssetType(assetType);
		
		// update slug Content URI for SEO or user-friendly URL
		String slug = paramJson.getString("slug", "");
		item.setSlug(slug);

		// privacyStatus for authorization check
		int privacyStatus = paramJson.getInteger("privacyStatus", 0);
		item.setPrivacyStatus(privacyStatus);

		// contentClass for JavaScript OOP
		String contentClass = paramJson.getString("contentClass", "");
		item.setContentClass(contentClass);

		// description default for SEO
		String description = paramJson.getString("description", "");
		item.setDescription(description);

		// headline image default for social media feed
		String headlineVideoUrl = paramJson.getString("headlineVideoUrl", "");
		item.setHeadlineVideoUrl(headlineVideoUrl);
		
		// headline images
		JsonObject jsonHeadlineImages = paramJson.getJsonObject("headlineImages", new JsonObject());
		Map<String, String> headlineImages = new HashMap<>(jsonHeadlineImages.size());
		jsonHeadlineImages.forEach(e -> {
			String key = e.getKey();
			String val = e.getValue().toString();
			if (!key.isEmpty()) {
				headlineImages.put(key, val);
			}
		});
		item.setHeadlineImages(headlineImages);
		
		String headlineImageUrl = paramJson.getString("headlineImageUrl", "");
		if(StringUtil.isEmpty(headlineImageUrl) && headlineImages.size() > 0) {
			headlineImageUrl = headlineImages.keySet().iterator().next();
		} 
		item.setHeadlineImageUrl(headlineImageUrl);

		// keywords
		item.clearKeywords();
		JsonArray jsonKeywords = paramJson.getJsonArray("keywords", new JsonArray());
		for (Object e : jsonKeywords) {
			String keyword = e.toString().trim();
			if (!keyword.isEmpty()) {
				item.setKeyword(keyword);
			}
		}

		// custom data
		JsonObject jsonCustomData = paramJson.getJsonObject("customData", new JsonObject());
		Map<String, String> customData = new HashMap<>(jsonCustomData.size());
		jsonCustomData.forEach(e -> {
			String key = e.getKey();
			String val = e.getValue().toString();
			if (!key.isEmpty()) {
				customData.put(key, val);
			}
		});
		item.setCustomData(customData);

		// mediaInfoUnits
		JsonArray jsonMediaInfoUnits = paramJson.getJsonArray("mediaInfoUnits", new JsonArray());
		List<MediaInfoUnit> mediaInfoUnits = new ArrayList<>(jsonMediaInfoUnits.size());
		int size = jsonMediaInfoUnits.size();
		for (int i = 0; i < size; i++) {
			JsonObject obj = jsonMediaInfoUnits.getJsonObject(i);
			MediaInfoUnit infoUnit = new MediaInfoUnit(obj.getString("headline"), obj.getString("content"));
			mediaInfoUnits.add(infoUnit);
		}
		item.setMediaInfoUnits(mediaInfoUnits);
	}
	
	public static void buildItemId(AssetItem item) {
		if(StringUtil.isEmpty(item.id)) {
			boolean check = StringUtil.isNotEmpty(item.title) && StringUtil.isNotEmpty(item.ownerId) && item.assetType >= 0;
			check = check && item.categoryIds != null || item.groupIds != null;
		
			if(check) {
				String keyHint = item.type + item.title + item.ownerId + item.assetType + item.categoryIds.toString() + item.groupIds.toString();
				item.id = createHashedId(keyHint);
				
				String s = new Slugify().slugify(item.title);
				item.slug = item.id + "-" + s.substring(0, Math.min(s.length(), MAX_LENGTH_OF_SLUG));
			}
			else {
				newIllegalArgumentException("Need: title, ownerId, assetType, categoryIds.size > 0, groupIds.size > 0 ");
			}
		}
	}
}
