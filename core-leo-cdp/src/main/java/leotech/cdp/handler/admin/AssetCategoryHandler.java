package leotech.cdp.handler.admin;

import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetCategoryDaoUtil;
import leotech.cdp.domain.AssetCategoryManagement;
import leotech.cdp.model.asset.AssetCategory;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;

/**
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class AssetCategoryHandler extends SecuredHttpDataHandler {
	
	// for Admin CMS, only for ROLE_SUPER_ADMIN
	static final String API_LIST_ALL = "/cdp/asset-category/list-all";
	static final String API_CREATE_NEW = "/cdp/asset-category/create-new";
	static final String API_UPDATE_INFO = "/cdp/asset-category/update-info";
	static final String API_GET_INFO = "/cdp/asset-category/get-info";
	static final String API_DELETE = "/cdp/asset-category/delete";
	
	public AssetCategoryHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}


	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			if (isAuthorized(loginUser, AssetCategory.class)) {
				switch (uri) {
					case API_LIST_ALL : {
						List<AssetCategory> list = AssetCategoryManagement.getAllCategories();
						return JsonDataPayload.ok(uri, list, loginUser, AssetCategory.class);
					}
					case API_GET_INFO : {
						String key = paramJson.getString("key", "");
						if (!key.isEmpty()) {
							AssetCategory category;
							if (key.equals("newcategory")) {
								category = new AssetCategory();
							} else {
								category = AssetCategoryDaoUtil.getById(key);
							}
							return JsonDataPayload.ok(uri, category, loginUser, AssetCategory.class);
						}
					}
					case API_CREATE_NEW : {
						String key = AssetCategoryManagement.save(paramJson, true);
						return JsonDataPayload.ok(uri, key, loginUser, AssetCategory.class);
					}
					case API_UPDATE_INFO : {
						String key = AssetCategoryManagement.save(paramJson, false);
						return JsonDataPayload.ok(uri, key, loginUser, AssetCategory.class);
					}
					case API_DELETE : {
						String key = paramJson.getString("key", "");
						AssetCategoryDaoUtil.deleteById(key);
						return JsonDataPayload.ok(uri, key, true);
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
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			if (isAuthorized(loginUser, AssetCategory.class)) {
				// skip
			} else {
				return JsonErrorPayload.NO_AUTHORIZATION;
			}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}

}
