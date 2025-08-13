package leotech.cdp.model.asset;

import com.arangodb.ArangoCollection;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonObject;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.customer.FeedbackType;
import leotech.system.util.QrCodeUtil;
import leotech.system.version.SystemMetaData;

/**
 *  template is used for building landing page and feedback form
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class AssetTemplate extends AssetItem {
	
	public static final String COLLECTION_NAME = getCdpCollectionName(AssetTemplate.class);
	static ArangoCollection collection;
	
	public static final String ASSET_TEMPLATE_ITEM = "asset-template-item";
	public static final String WEB_TEMPLATE_HTML_URI = "/webhtml?" + HttpParamKey.TEMPLATE_ID + "=";
	public static final String WEBFORM_SURVEY_URI = "/webform?" + HttpParamKey.TEMPLATE_ID + "=";

	public static final String DEFAULT_SELECTOR_KEY = "default";
	public static final String SELECT_BY_WEIGHT = "selectByWeight";
	
	public static final String BASIC_EMAIL_CONTENT = "basic_email_content";
	public static final String PRODUCT_RECOMMENDATION = "product_recommendation";
	public static final String CONTENT_RECOMMENDATION = "content_recommendation";
	public static final String FEEDBACK_SURVEY = "feedback_survey";
	
	@Expose
	private String selectorKey = DEFAULT_SELECTOR_KEY;
	
	@Expose
	private String activationName = "";
	
	@Expose
	private String jsonMetadata = "";
	
	@Expose
	private String headCode = "";
	
	@Expose
	private String bodyCode = "";
	
	@Expose
	private int templateType = FeedbackType.SURVEY;
	
	@Expose
	private double selectorWeight = 0.31F;

	public AssetTemplate() {
		// for JSON
		this.type = ContentType.TEMPLATE;
		this.assetType = AssetType.WEB_HTML_CONTENT;
	}
	
	public AssetTemplate(String categoryId, String groupId, String selectorKey, String activationName, int assetType, String ownerId, String title, String mediaInfo) {
		super();
		this.selectorKey = selectorKey;
		this.activationName = activationName;
		this.assetType = assetType;
		this.type = ContentType.TEMPLATE;
		initNewItem(categoryId, groupId, title, mediaInfo, assetType, ownerId);
	}
	
	public AssetTemplate(String categoryId, String groupId, String activationName, int assetType, String ownerId, String title, String mediaInfo) {
		super();
		this.selectorKey = DEFAULT_SELECTOR_KEY;
		this.activationName = activationName;
		this.assetType = assetType;
		this.type = ContentType.TEMPLATE;
		initNewItem(categoryId, groupId, title, mediaInfo,  assetType, ownerId);
	}
	
	public void initTemplateData(String categoryId, String groupId, String ownerId, String title, String mediaInfo, int assetType) {
		if(categoryId != null) {
			this.categoryIds.add(categoryId);
		}
		
		if(groupId != null) {
			this.groupIds.add(groupId);
		}
		this.ownerId = ownerId;
		this.title = title;
		this.mediaInfo = mediaInfo;
		this.type = ContentType.TEMPLATE;
		this.assetType = assetType;
		this.buildHashedId();
	}
	
	public static ArangoCollection getCollectionInstance() {
		return getCollection(collection, COLLECTION_NAME);
	}

	@Override
	public ArangoCollection getDbCollection() {
		return getCollection(collection, COLLECTION_NAME);
	}

	public String getSelectorKey() {
		return selectorKey;
	}

	public void setSelectorKey(String selectorKey) {
		this.selectorKey = selectorKey;
	}

	public String getActivationName() {
		return activationName;
	}

	public void setActivationName(String activationName) {
		this.activationName = activationName;
	}

	public double getSelectorWeight() {
		return selectorWeight;
	}

	public void setSelectorWeight(double selectorWeight) {
		this.selectorWeight = selectorWeight;
	}

	public String getMapKey() {
		return buildMapKey(activationName, selectorKey);
	}
	

	public int getTemplateType() {
		return templateType;
	}

	public void setTemplateType(int templateType) {
		this.templateType = templateType;
	}

	public String getJsonMetadata() {
		return jsonMetadata;
	}

	public void setJsonMetadata(String jsonMetadata) {
		this.jsonMetadata = jsonMetadata;
	}
	
	public String getHeadCode() {
		return headCode;
	}

	public void setHeadCode(String headCode) {
		this.headCode = headCode;
	}

	public String getBodyCode() {
		return bodyCode;
	}

	public void setBodyCode(String bodyCode) {
		this.bodyCode = bodyCode;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	public static final String buildMapKey(String activationName, String selectorKey) {
		return activationName + "_" + selectorKey;
	}
	
	@Override
	public void initItemDataFromJson(JsonObject paramJson) {
		setItemDataFromJson(this, paramJson);
		
		this.headCode = paramJson.getString("headCode","");
		this.jsonMetadata = paramJson.getString("jsonMetadata","{}");
		this.templateType = paramJson.getInteger("templateType", FeedbackType.SURVEY);
		
		if(this.templateType != FeedbackType.SURVEY) {
			// only survey need parser, else is just HTML code 
			this.bodyCode = paramJson.getString("bodyCode","");
		}
	}
	
	public void generateQrCodeForFeedback(String observerId) {
		this.shortLinkUrl = "https://" + SystemMetaData.DOMAIN_CDP_OBSERVER + WEBFORM_SURVEY_URI  + this.id + "&"+HttpParamKey.OBSERVER_ID+"=" + observerId;
		this.setQrCodeUrl(QrCodeUtil.generate(ASSET_TEMPLATE_ITEM, this.shortLinkUrl));
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		buildItemId(this);
		return this.id;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
}
