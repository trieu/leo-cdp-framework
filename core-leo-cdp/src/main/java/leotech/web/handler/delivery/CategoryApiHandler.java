package leotech.web.handler.delivery;

import java.util.List;
import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetCategoryDaoUtil;
import leotech.cdp.model.asset.AssetCategory;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.AppMetadata;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;

public class CategoryApiHandler extends SecuredHttpDataHandler {

	private static final String CATEGORY_LIST = "/category/list";
	
	public CategoryApiHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}

	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap) throws Exception {
		return JsonErrorPayload.NO_HANDLER_FOUND;
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser == null) {
			return JsonErrorPayload.NO_AUTHENTICATION;
		} else {
			if (uri.equalsIgnoreCase(CATEGORY_LIST)) {
				List<AssetCategory> cats = AssetCategoryDaoUtil.listAllByNetwork(AppMetadata.DEFAULT_ID);
				return JsonDataPayload.ok(uri, cats);
			}
			return JsonErrorPayload.NO_HANDLER_FOUND;
		}
	}

}
