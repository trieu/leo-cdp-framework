package leotech.cdp.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.core.MultiMap;
import leotech.cdp.dao.AssetContentDaoPublicUtil;
import leotech.cdp.domain.AssetCategoryManagement;
import leotech.cdp.model.asset.AssetCategory;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.customer.ProfileType;
import leotech.query.util.SearchEngineUtil;
import leotech.system.domain.SystemConfigsManagement;
import leotech.system.model.AppMetadata;
import leotech.system.util.AppMetadataUtil;
import leotech.web.handler.delivery.AssetGroupApiHandler;
import leotech.web.handler.delivery.CreativeContentWebHandler;
import leotech.web.model.CategoryDataModel;
import leotech.web.model.MediaNetworkDataModel;
import leotech.web.model.PostDataModel;
import leotech.web.model.WebData;
import rfx.core.util.StringUtil;

/**
 * The main HTTP Router Util
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public class MainHttpRouterUtil {

	// URL prefix for common
	static final String HOME = "/";
	static final String HTML_USER = "/html/user/";
	static final String HTML_GROUP = "/html/group/";
	static final String HTML_NETWORK = "/html/network/";
	static final String HTML_CATEGORY = "/html/category/";
	static final String HTML_TOPIC = "/html/topic/";

	// handler for listing content by speacial filters
	static final String HTML_SEARCH = "/html/search";
	static final String HTML_MY_FAVORITES_LIST = "/html/my-favorites-list";
	static final String HTML_MOST_TRENDING = "/html/most-trending";
	static final String HTML_TOP_POSTS_BY_KEYWORDS = "/html/top-posts-by-keywords";

	// template name
	static final String LIST_NETWORK = "list-network";
	static final String SINGLE_NETWORK = "single-network";
	static final String SINGLE_CATEGORY = "single-category";
	static final String LIST_CATEGORY = "list-category";

	static final String LIST_POST = "list-post";
	static final String SEARCH_POST = "search-post";

	static final int NUMBER_RESULTS = 6;

	public static WebData build(String path, String networkDomain, MultiMap params, String userSession) {
		AppMetadata network = AppMetadataUtil.getContentNetwork(networkDomain);
		List<String> publicContentClasses = network.getPublicContentClassList();
		String contentCategoryId = network.getContentCategoryId();
		String webTemplateFolder = network.getWebTemplateFolder();

		String objectId;
		WebData model = null;

		// media network
		if (path.startsWith(HTML_NETWORK)) {
			objectId = path.replace(HTML_NETWORK, "");
			model = buildMediaNetworkDataModel(network, objectId);
		}
		// media category:
		// https://www.iab.com/guidelines/iab-quality-assurance-guidelines-qag-taxonomy/
		else if (path.startsWith(HTML_CATEGORY)) {
			objectId = path.replace(HTML_CATEGORY, "");
			model = buildCategoryDataModel(network, objectId);
		}
		// playlist, master node of sorted posts by specific ranking algorithms
		else if (path.startsWith(AssetGroupApiHandler.HTML_GROUP)) {
			objectId = path.replace(AssetGroupApiHandler.HTML_GROUP, "");
			int startIndex = StringUtil.safeParseInt(params.get("startIndex"), 0);
			int numberResult = StringUtil.safeParseInt(params.get("numberResult"), NUMBER_RESULTS);
			model = AssetGroupApiHandler.buildPageDataModel(path, network, objectId, startIndex, numberResult);
		}
		// post: render content for end-user (video, text, slide, images,...)
		else if (path.startsWith(CreativeContentWebHandler.HTML_POST)) {
			String slug = path.replace(CreativeContentWebHandler.HTML_POST, "");
			int startIndex = StringUtil.safeParseInt(params.get("startIndex"), 0);
			int numberResult = StringUtil.safeParseInt(params.get("numberResult"), NUMBER_RESULTS);
			model = CreativeContentWebHandler.buildPostDataModel(userSession, network, slug, startIndex, numberResult);
		}
		// listing posts by filtering keywords
		else if (path.startsWith(HTML_TOP_POSTS_BY_KEYWORDS)) {

			int startIndex = StringUtil.safeParseInt(params.get("startIndex"), 0);
			int numberResult = StringUtil.safeParseInt(params.get("numberResult"), NUMBER_RESULTS);
			String keywordsStr = StringUtil.safeString(params.get("keywords"), "");
			String[] keywords;
			if (!keywordsStr.isEmpty()) {
				keywords = keywordsStr.split(",");
			} else {
				keywords = new String[]{};
			}
			List<AssetContent> posts = AssetContentDaoPublicUtil.listPublicContentsByKeywords(new String[]{contentCategoryId},
					publicContentClasses, keywords, startIndex, numberResult);
			String title = " Top posts about \"" + keywordsStr + "\"";
			if (posts != null) {
				model = new PostDataModel(networkDomain, webTemplateFolder, LIST_POST, title, posts);
				int prevStartIndex = startIndex - numberResult;
				int nextStartIndex = startIndex + numberResult;
				if (prevStartIndex < 0) {
					prevStartIndex = 0;
				}
				if (posts.size() < numberResult) {
					nextStartIndex = 0;
				}
				model.setCustomData("prevStartIndex", prevStartIndex);
				model.setCustomData("nextStartIndex", nextStartIndex);
				model.setPageKeywords(keywordsStr);
			}
		}
		// searching posts by keywords
		else if (path.startsWith(HTML_SEARCH)) {

			int startIndex = StringUtil.safeParseInt(params.get("startIndex"), 0);
			int numberResult = StringUtil.safeParseInt(params.get("numberResult"), NUMBER_RESULTS);
			String keywordsStr = StringUtil.safeString(params.get("keywords"), "");
			String[] keywords;
			if (!keywordsStr.isEmpty()) {
				keywords = keywordsStr.split(",");
			} else {
				keywords = new String[]{};
			}
			List<AssetContent> posts = SearchEngineUtil.searchPublicPost(keywords, startIndex, numberResult);
			String title = keywordsStr + " - Content Search";
			if (posts != null) {
				model = new PostDataModel(networkDomain, webTemplateFolder, SEARCH_POST, title, posts);
				int prevStartIndex = startIndex - numberResult;
				int nextStartIndex = startIndex + numberResult;
				if (prevStartIndex < 0) {
					prevStartIndex = 0;
				}
				if (posts.size() < numberResult) {
					nextStartIndex = 0;
				}
				model.setCustomData("prevStartIndex", prevStartIndex);
				model.setCustomData("nextStartIndex", nextStartIndex);
				model.setPageKeywords(keywordsStr);
			}
		}

		// not found 404
		if (model == null) {
			model = WebData.page404(networkDomain, webTemplateFolder);
		}

		// set data for Top Page
		AssetGroupApiHandler.setPageNavigators(model, contentCategoryId);

		return model;
	}

	public static WebData buildMediaNetworkDataModel(AppMetadata network, String objectId) {
		WebData model;
		String networkDomain = network.getDomain();
		String templateFolder = network.getWebTemplateFolder();
		if (StringUtil.isNotEmpty(objectId)) {
			model = new MediaNetworkDataModel(networkDomain, templateFolder, SINGLE_NETWORK, network.getName());
		} else {
			model = new MediaNetworkDataModel(networkDomain, templateFolder, LIST_NETWORK, "All networks");
		}
		return model;
	}

	public static WebData buildCategoryDataModel(AppMetadata network, String objectId) {
		WebData model;
		String networkDomain = network.getDomain();
		String templateFolder = network.getWebTemplateFolder();
		if (StringUtil.isNotEmpty(objectId)) {
			AssetCategory category = AssetCategoryManagement.getCategory(objectId);
			if (category != null) {
				String title = network.getName() + "-" + category.getName();
				model = new CategoryDataModel(networkDomain, templateFolder, SINGLE_CATEGORY, title,
						Arrays.asList(category));
			} else {
				model = WebData.page404(networkDomain, templateFolder);
			}
		} else {
			String title = network.getName() + "- All categories";
			List<AssetCategory> cats = AssetCategoryManagement.getCategoriesByNetwork(network.getAppId());
			model = new CategoryDataModel(networkDomain, templateFolder, LIST_CATEGORY, title, cats);
		}
		return model;
	}

	public static WebData buildModel(String host, String tplFolderName, String tplName, MultiMap params) {
		AppMetadata network = AppMetadataUtil.getContentNetwork(host);
		String categoryId = network.getContentCategoryId();
	

		WebData model = new WebData(host, tplFolderName, tplName);
		AssetGroupApiHandler.setPageNavigators(model, categoryId);
		
		model.setCategoryNavigators(new ArrayList<>(0));
		
		model.setSystemConfigJson(SystemConfigsManagement.getPublicSystemConfigMapAsJson());
		model.setContactTypeJson(ProfileType.getMapValueToNameAsJson());
		
		model.setContentMediaBoxs(new ArrayList<>());

		int startIndex = StringUtil.safeParseInt(params.get("startIndex"), 0);
		int numberResult = StringUtil.safeParseInt(params.get("numberResult"), NUMBER_RESULTS);

		List<AssetContent> headlines = AssetContentDaoPublicUtil.listByMediaNetwork(network, false, false, startIndex, numberResult);
		int prevStartIndex = startIndex - numberResult;
		int nextStartIndex = startIndex + numberResult;
		if (prevStartIndex < 0) {
			prevStartIndex = 0;
		}
		if (headlines.size() < numberResult) {
			nextStartIndex = 0;
		}
		model.setCustomData("prevStartIndex", prevStartIndex);
		model.setCustomData("nextStartIndex", nextStartIndex);
		model.setHeadlines(headlines);
		
		return model;
	}

}
