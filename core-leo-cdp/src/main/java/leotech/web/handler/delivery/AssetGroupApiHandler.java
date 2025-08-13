package leotech.web.handler.delivery;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetGroupDaoUtil;
import leotech.cdp.domain.AssetGroupManagement;
import leotech.cdp.model.asset.AssetGroup;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.AppMetadata;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.web.model.PageDataModel;
import leotech.web.model.PageNavigator;
import leotech.web.model.WebData;
import rfx.core.util.StringUtil;

public class AssetGroupApiHandler extends SecuredHttpDataHandler {

	static final String API_PAGE_LIST_BY_CATEGORY = "/api/asset-group/list-by-category";
	static final String API_PAGE_LIST_BY_KEYWORD = "/api/asset-group/list-by-keyword";

	public static final String HTML_GROUP = "/html/group/";
	public static final String SINGLE_GROUP = "single-group";
	public static final String LIST_GROUP = "list-group";
	
	public AssetGroupApiHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}

	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson)
			throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser == null) {
			return JsonErrorPayload.NO_AUTHENTICATION;
		} else {
			if (uri.equalsIgnoreCase(API_PAGE_LIST_BY_CATEGORY)) {
				String catId = paramJson.getString("categoryId", "");
				if (catId.isEmpty()) {
					return JsonDataPayload.fail("categoryId is empty", 500);
				} else {
					List<AssetGroup> pages = AssetGroupDaoUtil.listByCategory(catId);
					return JsonDataPayload.ok(uri, pages);
				}
			}
			return JsonErrorPayload.NO_HANDLER_FOUND;
		}

	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser == null) {
			return JsonErrorPayload.NO_AUTHENTICATION;
		} else {
			if (uri.equalsIgnoreCase(API_PAGE_LIST_BY_CATEGORY)) {
				String catId = StringUtil.safeString(params.get("categoryId"), "");
				if (catId.isEmpty()) {
					return JsonDataPayload.fail("categoryId is empty", 500);
				} else {
					List<AssetGroup> pages = AssetGroupDaoUtil.listByCategory(catId);
					return JsonDataPayload.ok(uri, pages);
				}
			}
			return JsonErrorPayload.NO_HANDLER_FOUND;
		}
	}

	public static WebData buildPageDataModel(String path, AppMetadata network, String objectId, int startIndex,
			int numberResult) {
		WebData model;
		String networkDomain = network.getDomain();
		String templateFolder = network.getWebTemplateFolder();
		if (StringUtil.isNotEmpty(objectId)) {
			AssetGroup group = AssetGroupManagement.getGroupWithContents(objectId, startIndex, numberResult);
			if (group != null) {
				String title = network.getDomain() + " - " + group.getTitle();
				int size = group.getItemsOfGroup().size();
				System.out.println(size + "=>>>>>>>>>>>>> ### getPageWithPosts " + numberResult);

				model = new PageDataModel(networkDomain, templateFolder, SINGLE_GROUP, title, group);
				model.setBaseStaticUrl(network.getBaseStaticUrl());
				model.setPageDescription(group.getDescription());
				model.setPageKeywords(group.getKeywords());

				int nextStartIndex = startIndex + numberResult;
				if (size < numberResult) {
					nextStartIndex = 0;
				}
				model.setCustomData("nextStartIndex", nextStartIndex);
				model.setCustomData("currentPath", path);

			} else {
				model = WebData.page404(networkDomain, templateFolder);
			}

		} else {
			String title = network.getName() + " - Top Pages";
			List<AssetGroup> pages = AssetGroupManagement.getAssetGroups(startIndex, numberResult);
			model = new PageDataModel(networkDomain, templateFolder, LIST_GROUP, title, pages);
			model.setBaseStaticUrl(network.getBaseStaticUrl());

			int nextStartIndex = startIndex + numberResult;
			if (pages.size() < numberResult) {
				nextStartIndex = 0;
			}
			model.setCustomData("nextStartIndex", nextStartIndex);
		}
		return model;
	}

	public static void setPageNavigators(WebData model, String category) {
		List<PageNavigator> pageNavigators = new ArrayList<>();
		List<AssetGroup> topPages = AssetGroupManagement.getAssetGroupsByCategoryWithPublicPrivacy(category);
		for (AssetGroup page : topPages) {
			String id = page.getId();
			String uri = HTML_GROUP + page.getSlug();
			// TODO ranking by use profile here
			long rankingScore = page.getRankingScore();
			pageNavigators.add(new PageNavigator(id, uri, page.getTitle(), rankingScore));
		}
		model.setTopPageNavigators(pageNavigators);
	}

}
