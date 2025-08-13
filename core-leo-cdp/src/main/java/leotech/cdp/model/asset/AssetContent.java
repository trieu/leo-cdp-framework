package leotech.cdp.model.asset;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonObject;
import rfx.core.util.StringUtil;

/**
 * data entity for ad-banner, web-form, video, web-game, slide
 * assetType == AssetType.AD_BANNER || assetType == AssetType.AD_VIDEO || assetType == AssetType.MULTIMEDIA_CATALOG || assetType == AssetType.GAMIFICATION_CONTENT
 * 
 * @author mac
 *
 */
public class AssetContent extends MeasurableItem {
	
	public static final String PRESENTATION_FOLDER_NAME = "presentation-item" ;
	public static final String CONTENT_FOLDER_NAME = "content-item" ;
	public static final String SHORT_LINK_FOLDER_NAME = "short-link-item";
	
	public static final String COLLECTION_NAME = getCdpCollectionName(AssetContent.class);
	static ArangoCollection collectionInstance;
	
	@Expose
	protected String campaignId;
	
	public static ArangoCollection theCollection() throws ArangoDBException {
		return getCollection(collectionInstance, COLLECTION_NAME);
	}
	@Override
	public ArangoCollection getDbCollection() {
		return theCollection();
	}
	
	public AssetContent() {
		this.type = ContentType.HTML_TEXT;
		this.assetType = AssetType.KNOWLEDGE_HUB;
		this.contentClass = "creative";
	}
	
	public AssetContent(int type, int assetType) {
		this.type = type;
		this.assetType = assetType;
		this.contentClass = "knowledge";
	}
	
	public AssetContent(String fullUrl, String campaignId) {
		super();
		this.fullUrl = fullUrl;
		this.campaignId = campaignId;
		this.isAdItem = true;
	}
	
	public void initNewShortUrlLink(String categoryId, String groupId, String title, String fullUrl, String ownerId) {
		initRequiredData(categoryId, groupId, title, "", ContentType.WEB_URL, ownerId);
		this.fullUrl = fullUrl;
		this.assetType = AssetType.SHORT_URL_LINK;
		this.contentClass = "ShortUrlLink";
	}
	
	public void setContentFromJsonObject(JsonObject paramJson) {
		setItemDataFromJson(this, paramJson);
		
		String campaignId = paramJson.getString("campaignId", "");
		this.setCampaignId(campaignId);
		
		String fullUrl = paramJson.getString("fullUrl", "");
		this.setFullUrl(fullUrl);
	}
	
	@Override
	public void initItemDataFromJson(JsonObject paramJson) {
		setItemDataFromJson(this, paramJson);
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		buildItemId(this);
		return this.id;
	}

	public String getFullUrl() {
		return fullUrl;
	}

	public void setFullUrl(String fullUrl) {
		this.fullUrl = fullUrl;
	}

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
	
	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	public String getLandingPageUrl() {
		if(StringUtil.isEmpty(this.fullUrl)) {
			return this.shortLinkUrl;
		}
		return this.fullUrl;
	}
}
