package leotech.starter.router;

import java.util.Map;
import java.util.function.Consumer;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.handler.admin.AgentHandler;
import leotech.cdp.handler.admin.Analytics360Handler;
import leotech.cdp.handler.admin.AssetCategoryHandler;
import leotech.cdp.handler.admin.AssetGroupHandler;
import leotech.cdp.handler.admin.AssetItemHandler;
import leotech.cdp.handler.admin.BusinessAccountHandler;
import leotech.cdp.handler.admin.CampaignHandler;
import leotech.cdp.handler.admin.DataFunnelHandler;
import leotech.cdp.handler.admin.EventDataHandler;
import leotech.cdp.handler.admin.EventObserverHandler;
import leotech.cdp.handler.admin.JourneyMapHandler;
import leotech.cdp.handler.admin.ProfileDataHandler;
import leotech.cdp.handler.admin.SegmentDataHandler;
import leotech.cdp.handler.admin.SystemConfigHandler;
import leotech.cdp.handler.admin.SystemControlHandler;
import leotech.cdp.handler.admin.SystemUserLoginHandler;
import leotech.cdp.handler.admin.TouchpointHubHandler;
import leotech.cdp.handler.api.EventApiHandler;
import leotech.cdp.handler.api.ProductApiHandler;
import leotech.cdp.handler.api.ProfileApiHandler;
import leotech.cdp.handler.api.SegmentApiHandler;
import leotech.cdp.handler.api.SystemApiHandler;
import leotech.system.common.BaseWebRouter;
import leotech.system.model.JsonDataPayload;

/**
 * Admin Http Web Router
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class AdminHttpRouter extends BaseWebRouter {

	public static final String CDP_JOURNEY_MAP_PREFIX = "/cdp/journeymap";
	public static final String CDP_PROFILE_PREFIX = "/cdp/profile";
	public static final String CDP_FUNNEL_PREFIX = "/cdp/funnel";
	public static final String CDP_OBSERVER_PREFIX = "/cdp/observer";
	public static final String CDP_EVENT_PREFIX = "/cdp/event";
	public static final String CDP_SEGMENT_PREFIX = "/cdp/segment";
	public static final String CDP_ACCOUNT_PREFIX = "/cdp/account";
	public static final String CDP_TOUCHPOINT_HUB_PREFIX = "/cdp/touchpointhub";
	public static final String CDP_TOUCHPOINT_PREFIX = "/cdp/touchpoint";
	public static final String CDP_ANALYTICS_360_PREFIX = "/cdp/analytics360";
	public static final String CDP_CAMPAIGN_PREFIX = "/cdp/campaign";
	public static final String CDP_AI_AGENT_PREFIX = "/cdp/agent";
	
	// system handler for admin
	public static final String CDP_SYSTEM_CONTROL= "/cdp/system-control";
	public static final String CDP_SYSTEM_CONFIG_PREFIX = "/cdp/system-config";
	
	// digital asset management
	public static final String CDP_ITEM_PREFIX = "/cdp/asset-item";
	public static final String CDP_GROUP_PREFIX = "/cdp/asset-group";
	public static final String CDP_CATEGORY_PREFIX = "/cdp/asset-category";
	public static final String CDP_CONTENT_CRAWLER_PREFIX = "/cdp/content-crawler";
	
	// 5 public API to extend CDP features 
	public static final String API_JOURNEY = "/api/journey";
	public static final String API_PROFILE = "/api/profile";
	public static final String API_SEGMENT = "/api/segment";
	public static final String API_EVENT = "/api/event";
	public static final String API_PRODUCT = "/api/product";
	public static final String API_CONTENT = "/api/content";
	public static final String API_SYSTEM = "/api/system";
	
	public AdminHttpRouter(RoutingContext context, String host, int port) {
		super(context, host, port);
	}

	@Override
	public void process() throws Exception {
		this.handle(this.context);
	}

	@Override
	protected void callHttpPostHandler(HttpServerRequest req, String userSession, String uri, JsonObject paramJson, Consumer<JsonDataPayload> done) {
		
		PROCESSORS.submit(()->{
			JsonDataPayload payload = null;
			try {			
				Map<String, Cookie> cookieMap = req.cookieMap();
				
				if (uri.startsWith(CDP_SYSTEM_CONTROL)) {
					payload = new SystemControlHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//////// Core System Management ///////
				else if (uri.startsWith(SYSTEM_USER_PREFIX)) {
					payload = new SystemUserLoginHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_SYSTEM_CONFIG_PREFIX)) {
					payload = new SystemConfigHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				} 
				
				//////// Customer Data Platform ///////
				//
				else if (uri.startsWith(CDP_ANALYTICS_360_PREFIX)) {
					payload = new Analytics360Handler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_FUNNEL_PREFIX)) {
					payload = new DataFunnelHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if(uri.startsWith(CDP_JOURNEY_MAP_PREFIX)) {
					payload = new JourneyMapHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if(uri.startsWith(CDP_TOUCHPOINT_HUB_PREFIX)) {
					payload = new TouchpointHubHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if(uri.startsWith(CDP_OBSERVER_PREFIX)) {
					payload = new EventObserverHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if(uri.startsWith(CDP_EVENT_PREFIX)) {
					payload = new EventDataHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_TOUCHPOINT_PREFIX)) {
					payload = new ProfileDataHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_PROFILE_PREFIX)) {
					payload = new ProfileDataHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_SEGMENT_PREFIX)) {
					payload = new SegmentDataHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_ACCOUNT_PREFIX)) {
					payload = new BusinessAccountHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_CAMPAIGN_PREFIX)) {
					payload = new CampaignHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_AI_AGENT_PREFIX)) {
					payload = new AgentHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_CATEGORY_PREFIX)) {
					payload = new AssetCategoryHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_GROUP_PREFIX)) {
					payload = new AssetGroupHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_ITEM_PREFIX)) {
					payload = new AssetItemHandler(this).httpPostHandler(userSession, uri, paramJson, cookieMap);
				}
				// to create or update profile
				else if (uri.startsWith(API_PROFILE)) {
					payload = new ProfileApiHandler().handlePost(req, uri, paramJson);
				}
				// to create a tracking event for specific profile
				else if (uri.startsWith(API_EVENT)) {
					payload = new EventApiHandler().handlePost(req, uri, paramJson);
				}
				// to save product item
				else if (uri.startsWith(API_PRODUCT)) {
					payload = new ProductApiHandler().handlePost(req, uri, paramJson);
				}
				// to create or update segment
				else if (uri.startsWith(API_SEGMENT)) {
					payload = new SegmentApiHandler().handlePost(req, uri, paramJson);
				}
				// to create or update system
				else if (uri.startsWith(API_SYSTEM)) {
					payload = new SystemApiHandler().handlePost(req, uri, paramJson);
				}
			} 
			catch (Throwable e) {
				e.printStackTrace();
				if (e instanceof IllegalArgumentException) {
					payload = JsonDataPayload.fail(e.getMessage(), 507);
				} else {
					payload = JsonDataPayload.fail(e.getMessage(), 500);
				}
			} 
			done.accept(payload);
		});		
		
	}

	@Override
	protected void callHttpGetHandler(HttpServerRequest req, String userSession, String uri, MultiMap urlParams, Consumer<JsonDataPayload> done) {
		PROCESSORS.submit(()->{
			JsonDataPayload payload = null;
			try {
				Map<String, Cookie> cookieMap = req.cookieMap();
				//////// Core System Management ///////
				if (uri.startsWith(SYSTEM_USER_PREFIX)) {
					payload = new SystemUserLoginHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_SYSTEM_CONFIG_PREFIX)) {
					payload = new SystemConfigHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				} 
				//////// Customer Data Platform ///////
				//
				else if (uri.startsWith(CDP_ANALYTICS_360_PREFIX)) {
					payload = new Analytics360Handler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_FUNNEL_PREFIX)) {
					payload = new DataFunnelHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if(uri.startsWith(CDP_JOURNEY_MAP_PREFIX)) {
					payload = new JourneyMapHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if(uri.startsWith(CDP_TOUCHPOINT_HUB_PREFIX)) {
					payload = new TouchpointHubHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if(uri.startsWith(CDP_OBSERVER_PREFIX)) {
					payload = new EventObserverHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if(uri.startsWith(CDP_EVENT_PREFIX)) {
					payload = new EventDataHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_TOUCHPOINT_PREFIX)) {
					payload = new ProfileDataHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_PROFILE_PREFIX)) {
					payload = new ProfileDataHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_SEGMENT_PREFIX)) {
					payload = new SegmentDataHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_ACCOUNT_PREFIX)) {
					payload = new BusinessAccountHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_CAMPAIGN_PREFIX)) {
					payload = new CampaignHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_AI_AGENT_PREFIX)) {
					payload = new AgentHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_CATEGORY_PREFIX)) {
					payload = new AssetCategoryHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_GROUP_PREFIX)) {
					payload = new AssetGroupHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				//
				else if (uri.startsWith(CDP_ITEM_PREFIX)) {
					payload = new AssetItemHandler(this).httpGetHandler(userSession, uri, urlParams, cookieMap);
				}
				
				// ---------- API handler ----------
				
				// to create or update profile
				else if (uri.startsWith(API_PROFILE)) {
					payload = new ProfileApiHandler().handleGet(req, uri, urlParams);
				}
				// to create a tracking event for specific profile
				else if (uri.startsWith(API_EVENT)) {
					payload = new EventApiHandler().handleGet(req, uri, urlParams);
				}
				// to list product items
				else if (uri.startsWith(API_PRODUCT)) {
					payload = new ProductApiHandler().handleGet(req, uri, urlParams);
				}
				// to create or update profile
				else if (uri.startsWith(API_SEGMENT)) {
					payload = new SegmentApiHandler().handleGet(req, uri, urlParams);
				}
				// to create or update system
				else if (uri.startsWith(API_SYSTEM)) {
					payload = new SystemApiHandler().handleGet(req, uri, urlParams);
				}
			} 
			catch (Throwable e) {
				e.printStackTrace();
				payload = JsonDataPayload.fail(e.getMessage(), 500);
			}
			done.accept(payload);
		});	
	}
	
}