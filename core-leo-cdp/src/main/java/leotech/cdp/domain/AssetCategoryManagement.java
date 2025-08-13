package leotech.cdp.domain;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetCategoryDaoUtil;
import leotech.cdp.dao.AssetContentDaoUtil;
import leotech.cdp.dao.AssetGroupDaoUtil;
import leotech.cdp.dao.AssetTemplateDaoUtil;
import leotech.cdp.model.asset.AssetCategory;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.AssetType;
import leotech.cdp.model.asset.ContentType;
import leotech.system.model.AppMetadata;
import leotech.system.model.SystemUser;
import leotech.system.util.JsonFileImporter;
import rfx.core.util.FileUtils;

/**
 * Asset Category Management
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class AssetCategoryManagement {
	
	private static final String CONFIGS_INIT_FEEDBACK_FORMS_JSON = "./resources/data-for-new-setup/init-feedback-forms.json";
	
	static Map<String, AssetCategory> assetCategoryMap = new HashMap<String, AssetCategory>(20);
	static Map<Integer, AssetCategory> categoryMapByAssetType = new HashMap<Integer, AssetCategory>(20);

	public static void initDefaultSystemData(SystemUser rootUser) throws IOException {
		if(rootUser == null) {
			System.err.println("rootUser is NULL");
			return;
		}
		String rootUserId = rootUser.getKey();
		String catDesc;
		
		// 1 --- Short URL Links ---
		catDesc = "Short URL links to collect lead profile data and click stream data";
		String categoryId = AssetCategoryDaoUtil.save(new AssetCategory("Short URL Links", AssetType.SHORT_URL_LINK, catDesc,1));
		if (categoryId != null) {
			int assetType = AssetType.SHORT_URL_LINK;
			
			AssetGroup group = new AssetGroup("CDP links", categoryId, assetType, rootUserId);
			group.setSystemAsset(true);
			group.setDefaultGroup(true);
			String groupId = AssetGroupDaoUtil.save(group);
			
			String fullUrl = "https://drive.google.com/file/d/1zHE31kmvTM74gtJ8ztdDDaU49fJUVkWv/view";
			AssetContent handbook = new AssetContent();
			handbook.initNewShortUrlLink(categoryId, groupId, "[CDP] CUSTOMER DATA PLATFORM - THE ULTIMATE HANDBOOK", fullUrl, rootUserId);
			handbook.setHeadlineImageUrl("https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhTsp9-cgwlpEno2me07w3BVrh8MFZAtkPyt3-p2BY-TQKfYR57iYlRRgP8E_i51V9gK81_-ZNFDxuv4Y2UTGeSZYOXW9_rI9DF8veUoK_ElBkdLXQnoVhrWzfuIz76CuV6fvJPbLPfwbP6xYvg42nO-MWxQvtzlshqYM-TKAkRDun_b78cIqJuCiDj/s672/345469666_801242254848505_1149504252855023564_n.jpeg");
			
			AssetContentDaoUtil.save(handbook);
		}
		
		// 2 --- Content Hub ---
		catDesc = "All content catalogs for Personalization Engine and Direct Marketing";
		categoryId = AssetCategoryDaoUtil.save(new AssetCategory("Web Content Catalogs", AssetType.CONTENT_ITEM_CATALOG, catDesc,2));
		if (categoryId != null) {
			int assetType = AssetType.CONTENT_ITEM_CATALOG;
			int type = ContentType.HTML_TEXT;
			
			AssetGroup group = new AssetGroup("CDP content", categoryId, assetType, rootUserId);
			group.setSystemAsset(true);
			group.setDefaultGroup(true);
			String groupId = AssetGroupDaoUtil.save(group);
			
			String mediaHtml = FileUtils.readFileAsString("./resources/content-templates/introduction-to-leocdp.html");
			AssetContent content = new AssetContent();
			content.initNewItem(categoryId, groupId, "Introduction to CDP", mediaHtml, type, rootUserId);
			AssetContentDaoUtil.save(content);
		}
		
		// 3 --- Product Item Catalogs ---
		catDesc = "All product calalogs for Personalization Engine and Direct Marketing";
		categoryId = AssetCategoryDaoUtil.save(new AssetCategory("Product Item Catalogs", AssetType.PRODUCT_ITEM_CATALOG, catDesc,3));
		if (categoryId != null) {
			int assetType = AssetType.PRODUCT_ITEM_CATALOG;
			int type = ContentType.HTML_TEXT;
			
			AssetGroup group = new AssetGroup("Default Product Items", categoryId, assetType, rootUserId);
			group.setSystemAsset(true);
			group.setDefaultGroup(true);
			AssetGroupDaoUtil.save(group);
		}
		
		
		// 4 --- CX Feedback Forms --- 
		catDesc = "All rating forms and survey forms to collect CX feedback data";
		categoryId = AssetCategoryDaoUtil.save(new AssetCategory("Feedback Form Catalogs", AssetType.FEEDBACK_FORM, catDesc,4));
		if (categoryId != null) {
			int assetType = AssetType.FEEDBACK_FORM;
			
			AssetGroup group1 = new AssetGroup("Survey Templates", categoryId, assetType, rootUserId, "");
			group1.setSystemAsset(true);
			group1.setDefaultGroup(true);
			AssetGroupDaoUtil.save(group1);
			
			AssetGroup group2 = new AssetGroup("Rating HTML Forms", categoryId, assetType, rootUserId, "");
			group2.setSystemAsset(true);
			group2.setDefaultGroup(true);
			AssetGroupDaoUtil.save(group2);
			
			// init template data
			JsonFileImporter<AssetTemplate> importer = new JsonFileImporter<AssetTemplate>(CONFIGS_INIT_FEEDBACK_FORMS_JSON , AssetTemplate[].class);
			
			List<AssetTemplate> feedbackTemplates = importer.getDataAsList();
			for (AssetTemplate assetTemplate : feedbackTemplates) {
				List<String> cateIds = Arrays.asList(categoryId);
				assetTemplate.setCategoryIds(cateIds);
				assetTemplate.setCreatedAt(new Date());
				
				String title = assetTemplate.getTitle();
				if(title.contains("[Survey]")) {
					assetTemplate.setGroupIds( Arrays.asList(group1.getId()));
					assetTemplate.buildHashedId();
					AssetTemplateDaoUtil.save(assetTemplate);
				}
				else if(title.contains("[Rating]")) {
					assetTemplate.setGroupIds(Arrays.asList(group2.getId()));
					assetTemplate.buildHashedId();
					AssetTemplateDaoUtil.save(assetTemplate);
				}
			}
		}

		// 5 Presentation Slides
		catDesc = "Visually appealing HTML presentation slides";
		categoryId = AssetCategoryDaoUtil.save(new AssetCategory("Presentation Slides", AssetType.PRESENTATION_ITEM_CATALOG, catDesc,5));
		if (categoryId != null) {
			int assetType = AssetType.PRESENTATION_ITEM_CATALOG;
			
			AssetGroup group1 = new AssetGroup("Default Presentation Group", categoryId, assetType, rootUserId, "");
			group1.setSystemAsset(true);
			group1.setDefaultGroup(true);
			AssetGroupDaoUtil.save(group1);
		}
		
		// 6 --- Email marketing and templates ---
		catDesc = "All HTML email templates for direct customer communication";
		categoryId = AssetCategoryDaoUtil.save(new AssetCategory("HTML Email Templates", AssetType.EMAIL_CONTENT, catDesc,6));
		if (categoryId != null) {
			int assetType = AssetType.EMAIL_CONTENT;
			int type = ContentType.TEMPLATE;
			
			String mediaHtml = "All email templates";
			AssetGroup group = new AssetGroup("Default Email Templates", categoryId, assetType,  rootUserId, mediaHtml);
			group.setSystemAsset(true);
			group.setDefaultGroup(true);
			group.setMergeDataIntoTemplates(true);
			String groupId = AssetGroupDaoUtil.save(group);
			
			String tpl1 = FileUtils.readFileAsString("./resources/content-templates/email-thanks-for-submit-info.html");
			AssetTemplate template1  = new AssetTemplate(categoryId, groupId, "email-thanks", assetType,  rootUserId, "Email Template - Thank you for your information", tpl1);
			template1.setSystemAsset(true);
			template1.setAssetType(assetType);
			template1.setType(type);
			template1.setContentClass(AssetTemplate.BASIC_EMAIL_CONTENT);
			AssetTemplateDaoUtil.save(template1);
			
			String tpl2 = FileUtils.readFileAsString("./resources/content-templates/email-product-recommendation.html");
			AssetTemplate template2  = new AssetTemplate(categoryId, groupId, "email-product-recommendation", assetType, rootUserId, "Email Template - Product Recommendation", tpl2);
			template2.setSystemAsset(true);
			template2.setAssetType(assetType);
			template2.setType(type);
			template1.setContentClass(AssetTemplate.PRODUCT_RECOMMENDATION);
			AssetTemplateDaoUtil.save(template2);
		}
		
		// 7 --- Notification and templates ---
		catDesc = "A template library for SMS, Zalo, and push notification messages";
		categoryId = AssetCategoryDaoUtil.save(new AssetCategory("Text Message Templates", AssetType.TEXT_MESSAGE_CONTENT, catDesc,7));
		if (categoryId != null) {
			int assetType = AssetType.TEXT_MESSAGE_CONTENT;
			int type = ContentType.TEMPLATE;
			
			AssetGroup group = new AssetGroup("Web Push templates", categoryId, assetType, rootUserId, "All push message templates");
			group.setSystemAsset(true);
			group.setDefaultGroup(true);
			group.setMergeDataIntoTemplates(true);
			AssetGroupDaoUtil.save(group);
			
			String mediaInfo = " Hi {{profile.firstName}}, you may like this item {{item.title}} at {{targetMediaUnit.trackingLinkUrl}}";
			String groupId = group.getId();
			AssetTemplate template2  = new AssetTemplate(categoryId, groupId, "webpush-product-recommendation", assetType, rootUserId, "Web Push template for Product Recommendation", mediaInfo);
			template2.setSystemAsset(true);
			template2.setAssetType(assetType);
			template2.setType(type);
			AssetTemplateDaoUtil.save(template2);
			
			AssetGroup group2 = new AssetGroup("SMS templates", categoryId, assetType, rootUserId, "All SMS message templates");
			group2.setSystemAsset(true);
			group2.setDefaultGroup(true);
			group2.setMergeDataIntoTemplates(true);
			AssetGroupDaoUtil.save(group2);
			
			String mediaInfo2 = " Hi {{profile.firstName}}, you may like this item {{item.title}} at {{targetMediaUnit.trackingLinkUrl}}";
			String groupId2 = group2.getId();
			AssetTemplate template3  = new AssetTemplate(categoryId, groupId2, "sms-product-recommendation", assetType, rootUserId, "SMS template for Product Recommendation", mediaInfo2);
			template3.setSystemAsset(true);
			template3.setAssetType(assetType);
			template3.setType(type);
			AssetTemplateDaoUtil.save(template3);
		}
		
		// 8 --- Web Landing Pages ---
		catDesc = "All landing pages and web forms are designed to collect customer data directly";
		categoryId = AssetCategoryDaoUtil.save(new AssetCategory("Web Landing Pages", AssetType.WEB_HTML_CONTENT, catDesc,8));
		if (categoryId != null) {
			int assetType = AssetType.WEB_HTML_CONTENT;			
			AssetGroup group = new AssetGroup("Default Web Pages", categoryId, assetType, rootUserId);
			group.setSystemAsset(true);
			group.setDefaultGroup(true);
			AssetGroupDaoUtil.save(group);
		}
	
		
	}

	public static String save(JsonObject paramJson, boolean createNew) {
		String catKey = paramJson.getString("key", "");
		String name = paramJson.getString("name", "");
		int assetType = paramJson.getInteger("type", AssetType.CONTENT_ITEM_CATALOG);

		// customData
		JsonObject jsonCustomData = paramJson.getJsonObject("customData", new JsonObject());
		Map<String, String> customData = new HashMap<>(jsonCustomData.size());
		jsonCustomData.forEach(e -> {
			String key = e.getKey();
			String val = e.getValue().toString();
			if (!key.isEmpty()) {
				customData.put(key, val);
			}
		});

		if (createNew) {
			catKey = AssetCategoryDaoUtil.save(new AssetCategory(name, AppMetadata.DEFAULT_ID, assetType));
		} else {
			AssetCategory c = AssetCategoryDaoUtil.getById(catKey);
			c.setName(name);
			c.setAssetType(assetType);
			catKey = AssetCategoryDaoUtil.save(c);
		}

		return catKey;
	}

	public static AssetCategory getCategory(String id) {
		 Map<String, AssetCategory> cache = getAssetCategoryMap();
		return cache.get(id);
	}

	public static List<AssetCategory> getCategoriesByNetwork(long networkId) {
		return AssetCategoryDaoUtil.listAllByNetwork(networkId);
	}

	public static List<AssetCategory> getAllCategories() {
		return AssetCategoryDaoUtil.getAllCategories();
	}
	
	public static Map<String, AssetCategory> getAssetCategoryMap() {
		if(assetCategoryMap.isEmpty()) {
			 List<AssetCategory> cates = AssetCategoryDaoUtil.getAllCategories();
			 for (AssetCategory assetCategory : cates) {
				 assetCategoryMap.put(assetCategory.getId(), assetCategory);
			}
		}
		return assetCategoryMap;
	}

	public static Map<Integer, AssetCategory> getCategoryMapByAssetType() {
		if(categoryMapByAssetType.isEmpty()) {
			 List<AssetCategory> cates = AssetCategoryDaoUtil.getAllCategories();
			 for (AssetCategory assetCategory : cates) {
				 categoryMapByAssetType.put(assetCategory.getAssetType(), assetCategory);
			}
		}
		return categoryMapByAssetType;
	}
	
	
}
