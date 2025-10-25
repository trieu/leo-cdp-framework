package leotech.starter.router;

import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.domain.ProfileGraphManagement;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.common.BaseWebRouter;
import leotech.system.domain.SystemInfo;
import leotech.system.model.JsonDataPayload;
import leotech.system.template.HandlebarsTemplateUtil;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.version.SystemMetaData;
import leotech.web.handler.delivery.AssetGroupApiHandler;
import leotech.web.handler.delivery.CategoryApiHandler;
import leotech.web.handler.delivery.ContentQueryApiHandler;
import leotech.web.handler.delivery.CreativeContentWebHandler;
import leotech.web.handler.delivery.UserApiHandler;

/**
 * Data Delivery Router for Ads, Content Hub, Page, Post, Category and User Login
 * 
 * @author tantrieuf31
 * @since 2020        
 *
 */
public final class DeliveryHttpRouter extends BaseWebRouter {

	public static final String COMMENT_PREFIX = "/comment";
	public static final String BOOKMARK_PREFIX = "/bookmark";

	public static final String CMS_TOPIC_PREFIX = "/topic";
	public static final String CMS_KEYWORD_PREFIX = "/keyword";
	public static final String CMS_POST_PREFIX = "/post";
	public static final String CMS_GROUP_PREFIX = "/group";
	public static final String CMS_CATEGORY_PREFIX = "/category";

	// Leo Ads System
	public static final String ADS_QUERY = "/ads/query";

	// Recommender
	public static final String RECOMMENDER_PRODUCTS = "/ris/products";
	public static final String RECOMMENDER_CONTENTS = "/ris/contents";
	
	
	public static final String PLACEMENT_PARAM = "pms";

	public DeliveryHttpRouter(RoutingContext context) {
		super(context);
		// caching or not caching templates in resources
		boolean enableCaching = SystemMetaData.isEnableCachingViewTemplates();
		if (enableCaching) {
			HandlebarsTemplateUtil.enableUsedCache();
		} else {
			HandlebarsTemplateUtil.disableUsedCache();
		}
	}

	@Override
	public boolean handle() throws Exception {
		return this.handle(this.context);
	}

	@Override
	protected JsonDataPayload callHttpPostHandler(HttpServerRequest req, String userSession, String uri, JsonObject paramJson) {
		JsonDataPayload payload = null;
		try {

			if (uri.startsWith(CMS_POST_PREFIX)) {
				payload = new CreativeContentWebHandler(this).httpPostHandler(userSession, uri, paramJson);
			}

			else if (uri.equalsIgnoreCase(CMS_GROUP_PREFIX)) {
				payload = new AssetGroupApiHandler(this).httpPostHandler(userSession, uri, paramJson);
			}

			else if (uri.startsWith(CMS_CATEGORY_PREFIX)) {
				payload = new CategoryApiHandler(this).httpPostHandler(userSession, uri, paramJson);
			}

			else if (uri.startsWith(SYSTEM_USER_PREFIX)) {
				payload = new UserApiHandler(this).httpPostHandler(userSession, uri, paramJson);
			}

			else if (uri.startsWith(CMS_TOPIC_PREFIX)) {
				// TODO
			}

			else if (uri.startsWith(CMS_KEYWORD_PREFIX)) {
				// TODO
			}

			else if (uri.startsWith(COMMENT_PREFIX)) {
				// TODO
			}

			else if (uri.startsWith(BOOKMARK_PREFIX)) {
				// TODO
			}

		} catch (Throwable e) {
			e.printStackTrace();
			payload = JsonDataPayload.fail(e.getMessage(), 500);
		}
		if (payload == null) {
			payload = JsonDataPayload.fail("Not handler found for uri:" + uri, 404);
		}
		return payload;
	}

	@Override
	protected JsonDataPayload callHttpGetHandler(HttpServerRequest req, String userSession, String uri, MultiMap params) {
		JsonDataPayload payload = null;

		try {
			
			//////// Recommender for contents ///////
			if (uri.startsWith(RECOMMENDER_CONTENTS)) {

				String observerId = HttpWebParamUtil.getString(params, HttpParamKey.OBSERVER_ID, "");
				String visid = HttpWebParamUtil.getString(params, HttpParamKey.VISITOR_ID, "");
				String touchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_URL, "");
				
				int startIndex =   HttpWebParamUtil.getInteger(params,HttpParamKey.START_INDEX, 0);
				int numberResult = HttpWebParamUtil.getInteger(params,HttpParamKey.NUMBER_RESULT, 10);

				List<?> items = ProfileGraphManagement.queryRecommendedContents(observerId, visid, touchpointUrl, startIndex, numberResult);
				payload = JsonDataPayload.ok(uri, items);
				payload.setReturnOnlyData(true);
			}
			//////// Recommender for products ///////
			else if (uri.startsWith(RECOMMENDER_PRODUCTS)) {

				String observerId = HttpWebParamUtil.getString(params, HttpParamKey.OBSERVER_ID, "");
				String visid = HttpWebParamUtil.getString(params, HttpParamKey.VISITOR_ID, "");
				String touchpointUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_URL, "");
				
				int startIndex =   HttpWebParamUtil.getInteger(params,HttpParamKey.START_INDEX, 0);
				int numberResult = HttpWebParamUtil.getInteger(params,HttpParamKey.NUMBER_RESULT, 10);
				
				List<TargetMediaUnit> items = ProfileGraphManagement.queryRecommendedProductItems(observerId, visid, touchpointUrl, startIndex, numberResult);
				payload = JsonDataPayload.ok(uri, items);
				payload.setReturnOnlyData(true);
			}

			else if (uri.startsWith(QUERY_PREFIX) || uri.startsWith(SEARCH_PREFIX)) {
				payload = new ContentQueryApiHandler(this).httpGetHandler(userSession, uri, params);
			} 
			
			else if (uri.startsWith(CMS_POST_PREFIX)) {
				payload = new CreativeContentWebHandler(this).httpGetHandler(userSession, uri, params);
			}

			else if (uri.startsWith(CMS_GROUP_PREFIX)) {
				payload = new AssetGroupApiHandler(this).httpGetHandler(userSession, uri, params);
			}

			else if (uri.startsWith(CMS_CATEGORY_PREFIX)) {
				payload = new CategoryApiHandler(this).httpGetHandler(userSession, uri, params);
			}

			else if (uri.startsWith(SYSTEM_USER_PREFIX)) {
				payload = new UserApiHandler(this).httpGetHandler(userSession, uri, params);
			}

			else if (uri.equalsIgnoreCase(URI_PING)) {
				payload = JsonDataPayload.ok(uri, PONG);
			}
			
			else if (uri.equalsIgnoreCase(URI_SYSINFO)) {
				SystemInfo systemInfo = new SystemInfo();
				payload = JsonDataPayload.ok(uri, systemInfo);
			} 

			else if (uri.equalsIgnoreCase(START_DATE)) {
				payload = JsonDataPayload.ok(uri, START_DATE);
			}

		} catch (Throwable e) {
			e.printStackTrace();
			payload = JsonDataPayload.fail(e.getMessage(), 500);
		}

		if (payload == null) {
			payload = JsonDataPayload.fail("Not handler found for uri:" + uri, 404);
		}
		return payload;
	}
}