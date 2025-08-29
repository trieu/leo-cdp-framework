package leotech.cdp.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetContentDaoPublicUtil;
import leotech.cdp.dao.AssetContentDaoUtil;
import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.dao.AssetTemplateDaoUtil;
import leotech.cdp.model.analytics.OrderedItem;
import leotech.cdp.model.asset.AssetCategory;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.AssetItem;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.AssetType;
import leotech.cdp.model.asset.ContentType;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.journey.TouchpointType;
import leotech.cdp.query.filters.AssetItemFilter;
import leotech.query.util.SearchEngineUtil;
import leotech.system.model.JsonDataTablePayload;
import rfx.core.util.StringUtil;

/**
 * digital item asset management
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class AssetItemManagement {

	static Logger logger = LoggerFactory.getLogger(AssetItemManagement.class);
	
	// TODO add shared redis cache here
	private static final int QUERY_CACHE_TIME = 30;

	static CacheLoader<String, AssetContent> cacheContentLoaderById = new CacheLoader<>() {
		@Override
		public AssetContent load(String id) {
			return AssetContentDaoUtil.getById(id);
		}
	};
	static LoadingCache<String, AssetContent> cacheContentById = CacheBuilder.newBuilder()
			.maximumSize(1000000).expireAfterWrite(QUERY_CACHE_TIME, TimeUnit.SECONDS).build(cacheContentLoaderById);
	
	static CacheLoader<String, ProductItem> cacheProductLoaderById = new CacheLoader<>() {
		@Override
		public ProductItem load(String id) {
			return AssetProductItemDaoUtil.getById(id);
		}
	};
	static LoadingCache<String, ProductItem> cacheProductById = CacheBuilder.newBuilder()
			.maximumSize(1000000).expireAfterWrite(QUERY_CACHE_TIME, TimeUnit.SECONDS).build(cacheProductLoaderById);

	public static AssetContent getContentItemById(String id) {
		AssetContent item = null;
		try {
			item = cacheContentById.get(id);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return item;
	}
	
	public static ProductItem getProductItemById(String id) {
		ProductItem item = null;
		try {
			item = cacheProductById.get(id);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return item;
	}
	
	public static AssetContent getContentItemBySlug(String slug) {
		return AssetContentDaoUtil.getBySlug(slug);
	}

	public static int rebuildSearchIndex() throws IOException {
		int s = SearchEngineUtil.indexing();
		return s;
	}
	
	public static JsonDataTablePayload getListByFilter(AssetItemFilter filter) {
		String groupId = filter.getGroupId();
		String searchValue = filter.getSearchValue();
		int startIndex = filter.getStart();
		int numberResult = filter.getLength();
		String uri = filter.getUri();
		int draw = filter.getDraw();
		AssetGroup group = AssetGroupManagement.getAssetGroupById(groupId);
		if(group != null) {
			if(group.isContent() || group.isPresentation()) {
				long recordsTotal = AssetContentDaoPublicUtil.countTotal();
				int recordsFiltered = AssetContentDaoPublicUtil.listByGroup(searchValue, groupId).size();
				List<AssetContent> list = AssetContentDaoPublicUtil.list(searchValue, groupId, startIndex, numberResult);
				JsonDataTablePayload payload = JsonDataTablePayload.data(uri, list, recordsTotal, recordsFiltered , draw);
				return payload;
			}
			else if(group.isProduct()) {
				long recordsTotal = AssetProductItemDaoUtil.countTotal();
				int recordsFiltered = AssetProductItemDaoUtil.countTotalSizeOfGroup(searchValue, groupId);
				List<ProductItem> list = AssetProductItemDaoUtil.list(searchValue, groupId, startIndex, numberResult);
				JsonDataTablePayload payload = JsonDataTablePayload.data(uri, list, recordsTotal, recordsFiltered , draw);
				return payload;
			}
			else if(group.isTemplate()) {
				long recordsTotal = AssetTemplateDaoUtil.countTotal();
				int recordsFiltered = AssetTemplateDaoUtil.countTotalSizeOfGroup(searchValue, groupId);
				List<AssetTemplate> list = AssetTemplateDaoUtil.list(searchValue, groupId, startIndex, numberResult);
				JsonDataTablePayload payload = JsonDataTablePayload.data(uri, list, recordsTotal, recordsFiltered , draw);
				return payload;
			}
		}
		return JsonDataTablePayload.data(uri, new ArrayList<>(0), 0, 0, draw);
	}
	
	public static AssetItem getAssetItem(AssetCategory cate, AssetGroup group, String itemId) {
		AssetItem item = null;
		
		if (itemId.isEmpty()) {
			 // as new object schema for create
			if(group.isProduct()) {
				item = new ProductItem();
				System.out.println("new Product Item ");
			} 
			else if(group.isTemplate()) {
				item = new AssetTemplate();
				System.out.println("new Template Item ");
			}
			else if(group.isPresentation()) {
				item = new AssetContent(ContentType.MARKDOWN, AssetType.PRESENTATION_ITEM_CATALOG);
				System.out.println("new Presentation Item ");
			}
			else {
				// default everything is content
				item = new AssetContent();
				System.out.println("new Content Item ");
			}
			
			item.setAssetCategory(cate.getId(), cate);
			item.setAssetGroup(group.getId(), group);
			item.setAssetType(group.getAssetType());
			item.setPrivacyStatus(group.getPrivacyStatus());
		} else {
			if(group.isProduct()) {
				item = AssetProductItemDaoUtil.getById(itemId);
			} 
			else if(group.isTemplate()) {
				item = AssetTemplateDaoUtil.getById(itemId);
			}
			else if(group.isContent() || group.isPresentation()) {
				item = AssetContentDaoUtil.getById(itemId);
			}
		}
		return item;
	}
	/**
	 * delete an asset item
	 * 
	 * @param itemId
	 * @param groupId
	 * @return
	 */
	public static boolean delete(String itemId, String groupId) {
		boolean ok = false;
		AssetGroup group = AssetGroupManagement.getAssetGroupById(groupId);
		if(group != null) {
			
			System.out.println("itemId " + itemId);
			System.out.println(new Gson().toJson(group));
			System.out.println("isProductItem " + group.isProduct());
			System.out.println("isTemplate " + group.isTemplate());
			System.out.println("isContentItem " + group.isContent());
			
			if(group.isProduct()) {
				ok = AssetProductItemDaoUtil.delete(itemId);
			} 
			else if(group.isTemplate()) {
				ok = AssetTemplateDaoUtil.delete(itemId);
			}
			else if(group.isContent()) {
				ok = AssetContentDaoUtil.delete(itemId);
			}
			if (ok) {
				SearchEngineUtil.deleteItemIndex(itemId);
			}
		}
		return ok;
	}
	

	/**
	 * @param paramJson
	 * @param loginUser
	 * @return
	 */
	public static String saveAssetItem(JsonObject paramJson, String ownerId) {
		String groupId = paramJson.getString("groupId", "");
		String itemId = paramJson.getString("itemId", "");

		// TODO need validate and check security data
		AssetItem item = null;
		boolean updateData = false;
		AssetGroup group = AssetGroupManagement.getAssetGroupById(groupId);
		if(group != null) {
			if(group.isProduct()) {
				ProductItem productItem;
				if (itemId.isEmpty()) {
					// create new 
					productItem = new ProductItem();
					productItem.setOwnerId(ownerId);
					System.out.println("save new ProductItem ....");
				} 
				else {
					// update existing 
					productItem = AssetProductItemDaoUtil.getById(itemId);
					updateData = true;
					System.out.println("update existing ProductItem ....");
				}
				// raise exception
				if(productItem == null) {
					throw new IllegalArgumentException("Invalid ProductItem, item is null, itemId = " + itemId);
				}
				productItem.initItemDataFromJson(paramJson);
				item = productItem;
			} 
			else if(group.isTemplate()) {
				AssetTemplate templateItem;
				if (itemId.isEmpty()) {
					// create new 
					templateItem = new AssetTemplate();
					templateItem.setOwnerId(ownerId);
					System.out.println("save new AssetTemplate ....");
				} else {
					// update existing 
					templateItem = AssetTemplateDaoUtil.getById(itemId);
					updateData = true;
					System.out.println("update existing AssetTemplate ....");
				}
				if(templateItem == null) {
					throw new IllegalArgumentException("Invalid AssetTemplate, item is null, itemId = " + itemId);
				}
				templateItem.initItemDataFromJson(paramJson);
				item = templateItem;
			}
			else if( group.isContent() || group.isPresentation() ) {
				AssetContent contentItem;
				if (itemId.isEmpty()) {
					// create new 
					contentItem = new AssetContent();
					contentItem.setOwnerId(ownerId);
					System.out.println("save new AssetContent ....");
				} else {
					// update existing 
					contentItem = AssetContentDaoUtil.getById(itemId);
					updateData = true;
					System.out.println("update existing AssetContent ....");
				}
				if(contentItem == null) {
					throw new IllegalArgumentException("Invalid AssetContent, item is null, itemId = " + itemId);
				}
				contentItem.setContentFromJsonObject(paramJson);
				item = contentItem;
			}
		} else {
			throw new IllegalArgumentException("Invalid or not found any asset group with ID " + groupId);
		}
		
		if(item == null) {
			throw new IllegalArgumentException("Invalid item init, item is null due to invalid group or itemId");
		}
		
		boolean editable = item.isEditable(ownerId) || group.isEditable(ownerId);
		if (!editable) {
			throw new IllegalAccessError("You do not have permission to edit the content");
		} 
		
		// set basic information
		item.setAssetType(group.getAssetType());
		item.setGroupIds(Arrays.asList(groupId));
		item.buildHashedId();
		
		// ready to save into database
		String saveId = null;
		if(item.isProduct()) {
			ProductItem productItem = (ProductItem) item;
			saveId = AssetProductItemDaoUtil.save(productItem);
		} 
		else if(item.isTemplate()) {
			AssetTemplate templateItem = (AssetTemplate) item;
			saveId = AssetTemplateDaoUtil.save(templateItem);
		}
		else if(item.isContent() || item.isPresentation()  ) {
			AssetContent content = (AssetContent) item;
			saveId = AssetContentDaoUtil.save(content);
			
			// create or update to touchpoint
			if(saveId != null && content.getType() == ContentType.WEB_URL) {
				String srcTouchpointUrl = content.getFullUrl();
				int channelType = TouchpointType.WEBSITE;
				String srcTouchpointName = content.getTitle();
				TouchpointManagement.getOrCreateNew(srcTouchpointName , channelType , srcTouchpointUrl);
			}
		}
		else {
			throw new IllegalArgumentException("Unknown AssetType " + item.getAssetType());
		}
		
		System.out.println("ContentClass: "+item.getContentClass() + " saved OK for ID " + saveId);
		
		if (StringUtil.isNotEmpty(saveId)) {
			if (updateData) {
				SearchEngineUtil.updateIndexedItem(item);
			} else {
				SearchEngineUtil.insertItemIndex(item);
			}
		}
		return saveId;
	}
	
	/**
	 * get template items to build embedded code in web touchpoints, collect CX feedback data
	 * 
	 * @return List<AssetTemplate>
	 */
	public static List<AssetTemplate> getFeedbackTemplates(String observerId) {
		AssetCategory cat = AssetCategoryManagement.getCategoryMapByAssetType().get(AssetType.FEEDBACK_FORM);
		if(cat != null) {
			List<AssetTemplate> list = AssetTemplateDaoUtil.listByFilter(AssetType.FEEDBACK_FORM, cat.getId());
			if(StringUtil.isNotEmpty(observerId)) {
				for (AssetTemplate assetTemplate : list) {
					assetTemplate.generateQrCodeForFeedback(observerId);
				}
			}
			return list;
		}
		return new ArrayList<>(0);
	}
	
	/**
	 * @param templateType
	 * @return
	 */
	public static List<AssetTemplate> getFeedbackTemplatesByTemplateType(int templateType) {
		AssetCategory cat = AssetCategoryManagement.getCategoryMapByAssetType().get(AssetType.FEEDBACK_FORM);
		if(cat != null) {
			return AssetTemplateDaoUtil.listByFilter(templateType, AssetType.FEEDBACK_FORM, cat.getId());
		}
		return new ArrayList<>(0);
	}
	
	/**
	 * @return
	 */
	public static List<AssetTemplate> getAllEmailTemplates() {
		AssetCategory cat = AssetCategoryManagement.getCategoryMapByAssetType().get(AssetType.EMAIL_CONTENT);
		if(cat != null) {
			return AssetTemplateDaoUtil.listByFilter(0, AssetType.EMAIL_CONTENT, cat.getId());
		}
		return new ArrayList<>(0);
	}
	
	/**
	 * @return
	 */
	public static List<AssetTemplate> getAllTextMessageTemplates() {
		AssetCategory cat = AssetCategoryManagement.getCategoryMapByAssetType().get(AssetType.TEXT_MESSAGE_CONTENT);
		if(cat != null) {
			return AssetTemplateDaoUtil.listByFilter(0, AssetType.TEXT_MESSAGE_CONTENT, cat.getId());
		}
		return new ArrayList<>(0);
	}
	
	/**
	 * @return
	 */
	public static Map<String, Map<String,String>> searchProductItemsByKeywords(String keywords, int startIndex, int numberResult) {
		Map<String,String> allProductItems = AssetProductItemDaoUtil.searchProductItemsByKeywords(keywords, startIndex, numberResult);
		Map<String, Map<String,String>> map = new HashMap<String, Map<String,String>>();
		map.put("allProductItems", allProductItems);
		return map;
	}
	
	/**
	 * get landing page items to build embedded code in web touchpoints, collect profile lead data
	 * 
	 * @return List<AssetTemplate>
	 */
	public static List<AssetTemplate> getLandingPageTemplates() {
		AssetCategory cat1 = AssetCategoryManagement.getCategoryMapByAssetType().get(AssetType.WEB_HTML_CONTENT);
		AssetCategory cat2 = AssetCategoryManagement.getCategoryMapByAssetType().get(AssetType.HTML_LANDING_PAGE);
		List<AssetTemplate> list = new ArrayList<>();
		if(cat1 != null) {
			list.addAll(AssetTemplateDaoUtil.listByFilter(AssetType.WEB_HTML_CONTENT, cat1.getId()));
		}
		if(cat2 != null) {
			list.addAll(AssetTemplateDaoUtil.listByFilter(AssetType.HTML_LANDING_PAGE, cat2.getId()));
		}
		return list;
	}
	
	static ProductItem getOrCreate(String itemId, String itemIdType, OrderedItem orderedItem) {
		ProductItem pItem = AssetProductItemDaoUtil.getByProductId(itemId, itemIdType);
		if(pItem == null) {
			AssetGroup group = AssetGroupManagement.getDefaultGroupForProduct();
			// create product item and add into default group
			if(group != null) {
				pItem = new ProductItem(orderedItem, group.getId());
			}
		} 
		else {
			// update from external event
			pItem.updateData(orderedItem);
		}
		return pItem;
	}
	
	/**
	 * create or update ProductItem (in system) from OrderedItem (external data)
	 * 
	 * @param itemId
	 * @param itemIdType
	 * @param orderedItem
	 * @return ProductItem
	 */
	public static ProductItem createOrUpdateProductItem(OrderedItem orderedItem) {
		String itemId = orderedItem.getItemId();
		String itemIdType = orderedItem.getIdType();
		// check from internal database first
		ProductItem pItem = getOrCreate(itemId, itemIdType, orderedItem);
		return pItem;
	}
	
}
