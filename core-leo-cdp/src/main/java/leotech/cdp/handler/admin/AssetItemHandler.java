package leotech.cdp.handler.admin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.api.client.util.Maps;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetContentDaoPublicUtil;
import leotech.cdp.domain.AssetCategoryManagement;
import leotech.cdp.domain.AssetGroupManagement;
import leotech.cdp.domain.AssetItemManagement;
import leotech.cdp.domain.ProductItemManagement;
import leotech.cdp.model.asset.AssetCategory;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.AssetItem;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.MeasurableItem;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.FeedbackType;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.query.filters.AssetItemFilter;
import leotech.query.util.SearchEngineUtil;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.ImportingResult;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class AssetItemHandler extends SecuredHttpDataHandler {

	// for Admin CMS, only for ROLE_DATA_ADMIN and ROLE_SUPER_ADMIN
	
	static final String ITEM_KEYWORDS_FOR_SEARCH = "/cdp/asset-item/keywords-for-search";
	static final String ITEM_SEARCH = "/cdp/asset-item/search";
	
	static final String URI_LIST_ITEMS_BY_GROUP = "/cdp/asset-item/filter-by-group";
	static final String URI_SAVE_ITEM = "/cdp/asset-item/save";
	
	static final String URI_SAVE_WEB_FORM = "/cdp/asset-item/save";
	
	static final String URI_GET_ITEM = "/cdp/asset-item/get";
	static final String URI_GET_BY_CONTENT_CLASS_AND_KEYWORDS = "/cdp/asset-item/by-content-class-and-keywords";
	static final String URI_DELETE_ITEM = "/cdp/asset-item/delete";
	
	static final String URI_IMPORT_PRODUCT = "/cdp/asset-item/import-product";
	static final String URI_IMPORT_PRODUCT_PREVIEW = "/cdp/asset-item/import-product-preview";
	static final String URI_ASSET_ITEM_TEMPLATES = "/cdp/asset-item/asset-templates";
	
	static final String URI_ASSET_ITEM_TEMPLATES_FOR_SURVEY = "/cdp/asset-item/asset-templates-for-survey";
	static final String URI_ASSET_ITEM_TEMPLATES_FOR_EMAIL = "/cdp/asset-item/asset-templates-for-email";
	static final String URI_ASSET_ITEM_TEMPLATES_FOR_TEXT_MESSAGE = "/cdp/asset-item/asset-templates-for-text-message";

	public AssetItemHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson)
			throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			if (isAuthorized(loginUser, AssetItem.class)) {
				if (uri.equalsIgnoreCase(URI_LIST_ITEMS_BY_GROUP)) {
					AssetItemFilter filter = new AssetItemFilter(loginUser, uri, paramJson);
					JsonDataTablePayload payload = AssetItemManagement.getListByFilter(filter);
					payload.setUserLoginPermission(loginUser, AssetItem.class);
					return payload;
				}
				else if (uri.equalsIgnoreCase(URI_SAVE_ITEM)) {
					String key = null;
					try {
						key = AssetItemManagement.saveAssetItem(paramJson, loginUser.getKey());
					} catch (Throwable e) {
						return JsonDataPayload.fail(e, 500);
					}
					if (key != null) {
						return JsonDataPayload.ok(uri, key, loginUser, AssetItem.class);
					} else {
						return JsonErrorPayload.UNKNOWN_EXCEPTION;
					}
				} 
				else if (uri.equalsIgnoreCase(URI_GET_ITEM)) {
					String itemId = paramJson.getString("itemId", "");
					String categoryId = paramJson.getString("categoryId", "");
					String groupId = paramJson.getString("groupId", "");
					AssetItem item = null;;
					
					AssetCategory cate = AssetCategoryManagement.getCategory(categoryId);
					AssetGroup group = AssetGroupManagement.getAssetGroupById(groupId);
					if(cate == null ) {
						return JsonDataPayload.fail("Invalid categoryId " + categoryId, 500);
						
					} 
					else if(group == null ) {
						return JsonDataPayload.fail("Invalid groupId " + groupId, 500);
					}
					else {
						item = AssetItemManagement.getAssetItem(cate, group, itemId);
						return JsonDataPayload.ok(uri, item, loginUser, AssetItem.class);
					}
				} 
				else if (uri.equalsIgnoreCase(URI_DELETE_ITEM)) {
					String itemId = paramJson.getString("itemId", "");
					String groupId = paramJson.getString("groupId");
					if (!itemId.isEmpty()) {
						boolean ok = AssetItemManagement.delete(itemId, groupId);
						return JsonDataPayload.ok(uri, ok, loginUser, AssetItem.class);
					} else {
						return JsonDataPayload.fail("Missing itemId", 500);
					}
				}
				else if (uri.equalsIgnoreCase(URI_IMPORT_PRODUCT)) {
					if(isAuthorizedToUpdateData(loginUser, Profile.class)) {
						String groupId = paramJson.getString("groupId", "");
						String importFileUrl = paramJson.getString("importFileUrl", "");
						if(StringUtil.isNotEmpty(importFileUrl)) {
							ImportingResult rs = ProductItemManagement.importCsvProductItems(groupId, importFileUrl);
							return JsonDataPayload.ok(uri, rs, loginUser, AssetItem.class);
						}
						return JsonDataPayload.fail("importFileName must not be empty", 500);
					} else {
						return JsonDataPayload.fail("No Authorization To update Data", 500);
					}
				}
			}
			return JsonErrorPayload.NO_AUTHORIZATION;

		} else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			if (uri.equalsIgnoreCase(URI_IMPORT_PRODUCT_PREVIEW)) {
				if(isAuthorizedToUpdateData(loginUser, AssetItem.class)) {
					String groupId = StringUtil.safeString(params.get("groupId"), "");
					String importFileUrl = HttpWebParamUtil.getString(params,"importFileUrl", "");
					if(StringUtil.isNotEmpty(importFileUrl)) {
						List<ProductItem> items = ProductItemManagement.parseImportedCsvFile(groupId, importFileUrl, true);
						
						JsonDataTablePayload payload = JsonDataTablePayload.data(uri, items , 20);
						return payload;
					}
					return JsonDataPayload.fail("importFileUrl must not be empty", 500);
				} else {
					return JsonDataPayload.fail("No Authorization To Update Data", 500);
				}
			}
			else if (uri.equalsIgnoreCase(URI_ASSET_ITEM_TEMPLATES)) {
				String observerId = StringUtil.safeString(params.get("obsid"), "");
				List<AssetTemplate> cxFeedbackTemplates = AssetItemManagement.getFeedbackTemplates(observerId);
				List<AssetTemplate> landingPageTemplates = AssetItemManagement.getLandingPageTemplates();
				Map<String, List<AssetTemplate>> results = Maps.newHashMap();
				results.put("cxFeedbackTemplates", cxFeedbackTemplates);
				results.put("landingPageTemplates", landingPageTemplates);
				return JsonDataPayload.ok(uri, results, loginUser, AssetItem.class);
			}
			else if (uri.equalsIgnoreCase(URI_ASSET_ITEM_TEMPLATES_FOR_SURVEY)) {
				List<AssetTemplate> templates = AssetItemManagement.getFeedbackTemplatesByTemplateType(FeedbackType.SURVEY);
				return JsonDataPayload.ok(uri, templates, loginUser, AssetItem.class);
			}
			else if (uri.equalsIgnoreCase(URI_ASSET_ITEM_TEMPLATES_FOR_EMAIL)) {
				List<AssetTemplate> templates = AssetItemManagement.getAllEmailTemplates();
				return JsonDataPayload.ok(uri, templates, loginUser, AssetItem.class);
			}
			else if (uri.equalsIgnoreCase(URI_ASSET_ITEM_TEMPLATES_FOR_TEXT_MESSAGE)) {
				List<AssetTemplate> templates = AssetItemManagement.getAllTextMessageTemplates();
				return JsonDataPayload.ok(uri, templates, loginUser, AssetItem.class);
			}
			
			else if (uri.equalsIgnoreCase(URI_GET_BY_CONTENT_CLASS_AND_KEYWORDS)) {
				String contentClass = StringUtil.safeString(params.get("contentClass"));
				String k = StringUtil.safeString(params.get("keywords"), "");
				String[] keywords;
				if (!k.isEmpty()) {
					keywords = k.split(",");
				} else {
					keywords = new String[]{};
				}
				// FIXME
				List<String> contentClasses = Arrays.asList(contentClass);
				List<AssetContent> list = AssetContentDaoPublicUtil.listByContentClassAndKeywords(null, contentClasses, keywords, true, false, false, 0, 1000);
				return JsonDataPayload.ok(uri, list, loginUser, AssetItem.class);
			} 
			else if (uri.equalsIgnoreCase(ITEM_SEARCH)) {
				String k = StringUtil.safeString(params.get("keywords"), "");
				String[] keywords;
				if (!k.isEmpty()) {
					keywords = k.split(",");
				} else {
					keywords = new String[]{};
				}
				List<MeasurableItem> results = SearchEngineUtil.search(keywords, 1, 100);
				return JsonDataPayload.ok(uri, results, loginUser, AssetItem.class);
			}
		} else {
			if (uri.equalsIgnoreCase(ITEM_KEYWORDS_FOR_SEARCH)) {
				List<String> list = AssetContentDaoPublicUtil.getAllKeywords();
				List<Map<String, String>> keywords = list.stream().map(e -> {
					Map<String, String> map = new HashMap<>(1);
					map.put("name", e);
					return map;
				}).collect(Collectors.toList());
				JsonDataPayload payload = JsonDataPayload.ok(uri, keywords, loginUser, AssetItem.class);
				payload.setReturnOnlyData(true);
				return payload;
			}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}

}
