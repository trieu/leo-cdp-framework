package leotech.web.handler.delivery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetContentDaoPublicUtil;
import leotech.cdp.domain.ContentItemManagement;
import leotech.cdp.model.asset.AssetContent;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.domain.SystemEventManagement;
import leotech.system.model.AppMetadata;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.web.model.PostDataModel;
import leotech.web.model.WebData;
import rfx.core.util.StringUtil;

public class CreativeContentWebHandler extends SecuredHttpDataHandler {

	static final String URI_GET_POSTS_BY_CONTENT_CLASS = "/post/by-content-class";
	static final String URI_GET_RECENT_POSTS_BY_GROUP = "/post/filter-by-group";
	static final String URI_GET_POST_INFO = "/post/get-info";
	static final String URI_GET_POSTS_LIST = "/post/get-list";
	static final String URI_GET_POSTS_LIST_BY = "/post/get-list-by";

	public static final String HTML_POST = "/html/post/";
	public static final String SINGLE_POST = "single-post";
	
	public CreativeContentWebHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}

	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson)
			throws Exception {
		return JsonErrorPayload.NO_HANDLER_FOUND;
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params) throws Exception {
		boolean includeProtected = false;
		boolean includePrivate = false;
		boolean allowPublicAccess = true;

		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			includeProtected = true;
			if (loginUser.hasAdminRole()) {
				includePrivate = true;
			}
		}
		System.out.println("##PostApiHandler.loginUser " + loginUser);
		if (allowPublicAccess || includeProtected) {
			if (uri.equalsIgnoreCase(URI_GET_POSTS_LIST)) {
				String categoryId = StringUtil.safeString(params.get("categoryId"));
				String groupId = StringUtil.safeString(params.get("groupId"));
				List<AssetContent> list = AssetContentDaoPublicUtil.listByCategoryOrGroup(categoryId, groupId);
				return JsonDataPayload.ok(uri, list, true);
			}

			else if (uri.equalsIgnoreCase(URI_GET_POSTS_LIST_BY)) {
				String ids = StringUtil.safeString(params.get("ids"));
				String[] list;
				if (!ids.isEmpty()) {
					list = ids.split(",");
				} else {
					list = new String[]{};
				}
				List<AssetContent> posts = new ArrayList<>(list.length);
				for (String id : list) {
					AssetContent post = ContentItemManagement.getById(id);
					post.compactDataForList(true);
					posts.add(post);
				}
				return JsonDataPayload.ok(uri, posts, true);
			}

			else if (uri.equalsIgnoreCase(URI_GET_POSTS_BY_CONTENT_CLASS)) {
				String contentClass = StringUtil.safeString(params.get("q"));
				List<String> contentClasses = Arrays.asList(contentClass);
				
				String keywordStr = StringUtil.safeString(params.get("keywords"), "");

				// filtering all posts must contain all keywords from query
				boolean operatorAnd = StringUtil.safeString(params.get("operator"), "or").equals("and");
				String[] keywords;
				if (!keywordStr.isEmpty()) {
					keywords = keywordStr.split(",");
				} else {
					keywords = new String[]{};
				}
				List<AssetContent> list = AssetContentDaoPublicUtil.listByContentClassAndKeywords(null, contentClasses, keywords,
						includeProtected, includePrivate, operatorAnd, 0, 1000);

				// TODO tracking
				// tracking with Google Analytics
				String userIp = StringUtil.safeString(params.get("__userIp"));
				String userAgent = StringUtil.safeString(params.get("__userAgent"));
				String trackingTitle = "landing-page: " + contentClass + "-" + keywordStr;
				SystemEventManagement.contentView(trackingTitle, uri, loginUser.getUserLogin(), userIp, userAgent);
				

				return JsonDataPayload.ok(uri, list, true);
			}

			else if (uri.equalsIgnoreCase(URI_GET_RECENT_POSTS_BY_GROUP)) {
				String groupId = StringUtil.safeString(params.get("q"));
				List<AssetContent> list = AssetContentDaoPublicUtil.listByGroup("", groupId);
				return JsonDataPayload.ok(uri, list, true);
			}

			else if (uri.equalsIgnoreCase(URI_GET_POST_INFO)) {
				String itemId = StringUtil.safeString(params.get("itemId"));
				String slug = StringUtil.safeString(params.get("slug"));
				AssetContent post = null;
				if (!itemId.isEmpty() || !slug.isEmpty()) {
					if (itemId.isEmpty()) {
						post = AssetContentDaoPublicUtil.getBySlug(slug);
					} else {
						post = ContentItemManagement.getById(itemId);
					}

					if (post != null) {

						// tracking with Google Analytics
						String userIp = StringUtil.safeString(params.get("__userIp"));
						String userAgent = StringUtil.safeString(params.get("__userAgent"));
						String trackingTitle = "content-post: " + post.getContentClass() + "-" + post.getTitle();
						SystemEventManagement.contentView(trackingTitle, uri, loginUser.getUserLogin(), userIp,
								userAgent);
						

						return JsonDataPayload.ok(uri, Arrays.asList(post), true);
					} else {
						// TODO tracking

						return JsonDataPayload.fail("Not found post", 404);
					}
				} else {
					return JsonDataPayload.fail("missing param itemId or slug", 500);
				}
			}
		} else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
		return JsonErrorPayload.NO_HANDLER_FOUND;
	}

	public static WebData buildPostDataModel(String userSession, AppMetadata network, String slug, int startIndex, int numberResult) {
		PostDataModel model = null;
		String networkDomain = network.getDomain();
		String templateFolder = network.getWebTemplateFolder();
		if (StringUtil.isNotEmpty(slug)) {
			// CrawledYouTubeVideo video =
			// CrawledYouTubeVideoDaoUtil.getByVideoID(objectId);
			// AssetContent post = new AssetContent(video.getTitle(), video.getUrl(), 0, "");
			AssetContent post = ContentItemManagement.getBySlug(slug);
			if (post != null) {

				List<String> contextGroupIds = post.getGroupIds();

				String itemId = post.getId();
				String title = network.getDomain() + " - " + post.getTitle();
				String des = post.getDescription();
				String pageImage = post.getHeadlineImageUrl();
				String siteName = network.getName();

				// build model from database object
				model = new PostDataModel(networkDomain, templateFolder, SINGLE_POST, title, post);
				model.setBaseStaticUrl(network.getBaseStaticUrl());
				model.setPageDescription(des);
				model.setPageImage(pageImage);
				model.setPageName(siteName);
				model.setPageKeywords(post.getKeywords());
				String pageUrl = model.getBaseStaticUrl() + HTML_POST + slug;
				model.setPageUrl(pageUrl);
				model.setContextPageId(contextGroupIds.size() > 0 ? contextGroupIds.get(0) : "");

				// TODO implement recommendation engine here
				List<AssetContent> simlilarPosts = ContentItemManagement.getSimilarPosts(contextGroupIds, itemId);
				model.setRecommendedPosts(simlilarPosts);
				SystemUser loginUser = initSystemUser(userSession);
				if (loginUser != null) {
					model.setAdminRole(SecuredHttpDataHandler.isAdminRole(loginUser));
					model.setSessionUserId(loginUser.getKey());
				}
			} else {
				return WebData.page404(networkDomain, templateFolder);
			}
		}
		return model;
	}

}
