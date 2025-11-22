package leotech.cdp.handler.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetGroupDaoUtil;
import leotech.cdp.domain.AssetCategoryManagement;
import leotech.cdp.domain.AssetGroupManagement;
import leotech.cdp.domain.ProfileGraphManagement;
import leotech.cdp.model.asset.AssetCategory;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.AssetType;
import leotech.cdp.model.asset.ContentType;
import leotech.cdp.model.customer.Segment;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 *
 */
public final class AssetGroupHandler extends SecuredHttpDataHandler {
	
	// for Admin CMS, only for ROLE_DATA_ADMIN and ROLE_SUPER_ADMIN
	static final String LIST_BY_CATEGORY = "/cdp/asset-group/list-by-category";
	static final String LIST_ASSET_GROUPS = "/cdp/asset-group/list-by-pagination";
	static final String SAVE_ASSET_GROUP = "/cdp/asset-group/save";

	static final String GET_ASSET_GROUP_GET_INFO = "/cdp/asset-group/get-info";
	static final String GET_ASSET_GROUP_DEFAULT = "/cdp/asset-group/get-default";
	static final String DELETE_ASSET_GROUP = "/cdp/asset-group/delete";
	
	static final String LIST_BY_ASSET_TYPE = "/cdp/asset-group/list-by-asset-type";
	static final String LIST_BY_ASSET_TYPES = "/cdp/asset-group/list-by-asset-types";
	static final String REMOVE_RECOMMMENDED_ITEMS = "/cdp/asset-group/remove-recommended-items";
	
	public AssetGroupHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}

	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			if (isAuthorized(loginUser, AssetGroup.class)) {
				switch (uri) {
					case LIST_ASSET_GROUPS : {
						int startIndex = paramJson.getInteger("startIndex", 0);
						int numberResult = paramJson.getInteger("numberResult", 1000);
						List<AssetGroup> list = AssetGroupManagement.getAssetGroups(startIndex, numberResult);
						return JsonDataPayload.ok(uri, list, loginUser, AssetGroup.class);
					}
					case LIST_BY_CATEGORY : {
						String categoryId = paramJson.getString("categoryId", "");
						if (!categoryId.isEmpty()) {
							List<AssetGroup> list = AssetGroupManagement.getAssetGroupsByCategory(categoryId);
							return JsonDataPayload.ok(uri, list, loginUser, AssetGroup.class);
						} else {
							return JsonDataPayload.fail("Missing categoryId", 500);
						}
					}
					case SAVE_ASSET_GROUP : {
						String key = null;
						try {
							key = AssetGroupManagement.save(paramJson, loginUser);
						} catch (Throwable e) {
							return JsonDataPayload.fail(e, 500);
						}
						if (key != null) {
							return JsonDataPayload.ok(uri, key, loginUser, AssetGroup.class);
						} else {
							return JsonErrorPayload.UNKNOWN_EXCEPTION;
						}
					}
					case GET_ASSET_GROUP_GET_INFO : {
						String groupId = paramJson.getString("groupId", "");
						String categoryId = paramJson.getString("categoryId", "");
						
						AssetGroup group;
						if (groupId.isBlank()) {
							group = new AssetGroup();
							
							AssetCategory cate = AssetCategoryManagement.getCategory(categoryId);
							if(cate != null) {
								group.setAssetCategory(categoryId, cate);
								int assetType = cate.getAssetType();
								group.setAssetType(assetType);
								
								if(assetType == AssetType.EMAIL_CONTENT || assetType == AssetType.TEXT_MESSAGE_CONTENT) {
									group.setType(ContentType.TEMPLATE);
								} else {
									group.setType(ContentType.HTML_TEXT);
								}
								
							} else {
								return JsonDataPayload.fail("Invalid categoryId " + categoryId, 500);
							}
						} else {
							group = AssetGroupDaoUtil.getById(groupId);
						}
						return JsonDataPayload.ok(uri, group, loginUser, AssetGroup.class);
					}
					case DELETE_ASSET_GROUP : {
						String groupId = paramJson.getString("groupId", "");
						if (!groupId.isEmpty()) {
							boolean ok = AssetGroupManagement.deleteAssetGroup(groupId);
							return JsonDataPayload.ok(uri, ok, loginUser, AssetGroup.class);
						} else {
							return JsonDataPayload.fail("Missing groupId", 500);
						}
					}
					
					case REMOVE_RECOMMMENDED_ITEMS : {
						if(loginUser.hasOperationRole()) {
							String assetGroupId = paramJson.getString("assetGroupId", "");
							if( ! assetGroupId.isBlank()  ) {
								boolean rs = ProfileGraphManagement.removeRecommendationItemsForGroup(assetGroupId);
								return JsonDataPayload.ok(uri, rs, loginUser, Segment.class);
							}
							else {
								return JsonDataPayload.fail("assetGroupId must not be empty", 500);
							}
						}
						else {
							return JsonErrorPayload.NO_AUTHORIZATION;
						}
					}
					
					default : {
						return JsonErrorPayload.NO_HANDLER_FOUND;
					}
				}

			}
			return JsonErrorPayload.NO_AUTHORIZATION;

		} else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession);
		if (loginUser != null) {
			if (isAuthorized(loginUser, AssetGroup.class)) {
				System.out.println("uri " + uri);
				
				switch (uri) {
				case LIST_BY_ASSET_TYPE : {
					int assetType =  HttpWebParamUtil.getInteger(params,"assetType", -1);
					if (assetType > 0) {
						List<AssetGroup> list = AssetGroupManagement.getAllGroupsByAssetType(assetType);
						return JsonDataPayload.ok(uri, list, loginUser, AssetGroup.class);
					}
					else {
						return JsonDataPayload.fail("Missing categoryId", 500);
					}
				}
				case LIST_BY_ASSET_TYPES : {
					String[] assetTypes = HttpWebParamUtil.getString(params,"assetTypes", "").split(",");
					int length = assetTypes.length;
					if(length > 0) {
						Map<Integer, List<AssetGroup>> map = new HashMap<Integer, List<AssetGroup>>(length);
						for (String assetTypeStr : assetTypes) {
							int assetType = StringUtil.safeParseInt(assetTypeStr);
							List<AssetGroup> list = AssetGroupManagement.getAllGroupsByAssetType(assetType);
							map.put(assetType, list);
						}
						return JsonDataPayload.ok(uri, map, loginUser, AssetGroup.class);
					}
					else {
						return JsonDataPayload.fail("Missing categoryId", 500);
					}
				}
				case GET_ASSET_GROUP_DEFAULT : {
					String context = HttpWebParamUtil.getString(params, "context");
					System.out.println("context " + context);
					AssetGroup group = null;
					if(context.equalsIgnoreCase("content")) {
						group = AssetGroupManagement.getDefaultGroupForContent();
					}
					else if(context.equalsIgnoreCase("product")) {
						group = AssetGroupManagement.getDefaultGroupForProduct();
					}
					else if(context.equalsIgnoreCase("presentation")) {
						group = AssetGroupManagement.getDefaultGroupForPresentation();
					}
					if(group != null) {
						return JsonDataPayload.ok(uri, group, loginUser, AssetGroup.class);
					}
					return JsonDataPayload.fail(context + " is not valid context, it should be content, product or presentation", 500);
				}
				
				default:
					break;
				}
			} else {
				return JsonErrorPayload.NO_AUTHORIZATION;
			}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}

}
