package leotech.starter.router;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
// Assumes all handlers are here
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
import leotech.cdp.handler.admin.TouchpointHandler;
import leotech.cdp.handler.admin.TouchpointHubHandler;
import leotech.system.common.BaseWebRouter;
import leotech.system.model.JsonDataPayload;

/**
 * Admin Http Web Router
 * Refactored for Leo CDP (Java 11)
 * * @author tantrieuf31
 * @since 2020
 */
public final class AdminHttpRouter extends BaseWebRouter {

	static Logger logger = LoggerFactory.getLogger(AdminHttpRouter.class);

    // --- Route Constants ---
    public static final String CDP_JOURNEY_MAP_PREFIX     = "/cdp/journeymap";
    public static final String CDP_PROFILE_PREFIX         = "/cdp/profile";
    public static final String CDP_FUNNEL_PREFIX          = "/cdp/funnel";
    public static final String CDP_OBSERVER_PREFIX        = "/cdp/observer";
    public static final String CDP_EVENT_PREFIX           = "/cdp/event";
    public static final String CDP_SEGMENT_PREFIX         = "/cdp/segment";
    public static final String CDP_ACCOUNT_PREFIX         = "/cdp/account";
    public static final String CDP_TOUCHPOINT_HUB_PREFIX  = "/cdp/touchpointhub";
    public static final String CDP_TOUCHPOINT_PREFIX      = "/cdp/touchpoint";
    public static final String CDP_ANALYTICS_360_PREFIX   = "/cdp/analytics360";
    public static final String CDP_CAMPAIGN_PREFIX        = "/cdp/campaign";
    public static final String CDP_AI_AGENT_PREFIX        = "/cdp/agent";
    public static final String CDP_SYSTEM_CONTROL         = "/cdp/system-control";
    public static final String CDP_SYSTEM_CONFIG_PREFIX   = "/cdp/system-config";
    public static final String CDP_ITEM_PREFIX            = "/cdp/asset-item";
    public static final String CDP_GROUP_PREFIX           = "/cdp/asset-group";
    public static final String CDP_CATEGORY_PREFIX        = "/cdp/asset-category";
    public static final String CDP_CONTENT_CRAWLER_PREFIX = "/cdp/content-crawler";

    /**
     * Functional interface factory to instantiate handlers with the router context
     */
    private static final Map<String, Function<BaseWebRouter, ?>> ROUTE_REGISTRY = new LinkedHashMap<>();

    static {
        // System & Core
        ROUTE_REGISTRY.put(CDP_SYSTEM_CONTROL,       SystemControlHandler::new);
        ROUTE_REGISTRY.put(CDP_SYSTEM_USER_PREFIX,   SystemUserLoginHandler::new);
        ROUTE_REGISTRY.put(CDP_SYSTEM_CONFIG_PREFIX, SystemConfigHandler::new);

        // CDP Core
        ROUTE_REGISTRY.put(CDP_ANALYTICS_360_PREFIX, Analytics360Handler::new);
        ROUTE_REGISTRY.put(CDP_FUNNEL_PREFIX,        DataFunnelHandler::new);
        ROUTE_REGISTRY.put(CDP_JOURNEY_MAP_PREFIX,   JourneyMapHandler::new);
        ROUTE_REGISTRY.put(CDP_TOUCHPOINT_HUB_PREFIX,TouchpointHubHandler::new);
        ROUTE_REGISTRY.put(CDP_TOUCHPOINT_PREFIX,    TouchpointHandler::new);
        ROUTE_REGISTRY.put(CDP_OBSERVER_PREFIX,      EventObserverHandler::new);
        ROUTE_REGISTRY.put(CDP_EVENT_PREFIX,         EventDataHandler::new);
        ROUTE_REGISTRY.put(CDP_PROFILE_PREFIX,       ProfileDataHandler::new);
        ROUTE_REGISTRY.put(CDP_SEGMENT_PREFIX,       SegmentDataHandler::new);
        ROUTE_REGISTRY.put(CDP_ACCOUNT_PREFIX,       BusinessAccountHandler::new);
        ROUTE_REGISTRY.put(CDP_CAMPAIGN_PREFIX,      CampaignHandler::new);
        ROUTE_REGISTRY.put(CDP_AI_AGENT_PREFIX,      AgentHandler::new);

        // Assets
        ROUTE_REGISTRY.put(CDP_CATEGORY_PREFIX,      AssetCategoryHandler::new);
        ROUTE_REGISTRY.put(CDP_GROUP_PREFIX,         AssetGroupHandler::new);
        ROUTE_REGISTRY.put(CDP_ITEM_PREFIX,          AssetItemHandler::new);
    }

    public AdminHttpRouter(RoutingContext context, String host, int port) {
        super(context, host, port);
    }

    @Override
    public void process() throws Exception {
        this.handle(this.context);
    }

    @Override
    protected void callHttpPostHandler(HttpServerRequest req, String userSession, String uri, JsonObject paramJson, Consumer<JsonDataPayload> done) {
    	System.out.println("AdminHttpRouter paramJson " + paramJson);
        handleRequest(req, uri, done, (handler, cookies) -> {
            // reflection or casting required if no common interface exists. 
            // Assuming common methods are available via reflection or BaseHandler interface.
            return invokeHandlerMethod(handler, "httpPostHandler", 
                    new Object[]{userSession, uri, paramJson, cookies}, 
                    new Class[]{String.class, String.class, JsonObject.class, Map.class});
        });
    }

    @Override
    protected void callHttpGetHandler(HttpServerRequest req, String userSession, String uri, MultiMap urlParams, Consumer<JsonDataPayload> done) {
        handleRequest(req, uri, done, (handler, cookies) -> {
            return invokeHandlerMethod(handler, "httpGetHandler", 
                    new Object[]{userSession, uri, urlParams, cookies},
                    new Class[]{String.class, String.class, MultiMap.class, Map.class});
        });
    }

    /**
     * Unified request handling logic to reduce duplication between GET and POST.
     */
    private void handleRequest(HttpServerRequest req, String uri, Consumer<JsonDataPayload> done, 
                               BiFunction<Object, Map<String, Cookie>, JsonDataPayload> action) {
        PROCESSORS.submit(() -> {
            JsonDataPayload payload;
            try {
                Map<String, Cookie> cookieMap = req.cookieMap();
                var handler = resolveHandler(uri);

                if (handler != null) {
                    payload = action.apply(handler, cookieMap);
                } else {
                    // Fallback if no route matches (though BaseWebRouter likely handles routing before this)
                    payload = JsonDataPayload.fail("No handler found for URI: " + uri, 404);
                }
            } catch (Throwable e) {
                logger.error("handleRequest is failed by "+e.getMessage(), e);
                int code = (e instanceof IllegalArgumentException) ? 507 : 500;
                payload = JsonDataPayload.fail(e.getMessage(), code);
            }
            done.accept(payload);
        });
    }

    /**
     * Resolves the correct handler instance based on the URI prefix.
     */
    private Object resolveHandler(String uri) {
        // Iterate over registry to find matching prefix. 
        // LinkedHashMap ensures we check in insertion order if order matters.
        for (var entry : ROUTE_REGISTRY.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue().apply(this);
            }
        }
        return null;
    }

    /**
     * Helper to invoke handler methods via Reflection if a common interface is missing.
     * Note: Ideally, all your Handlers should implement a common interface (e.g. AdminRequestHandler)
     * so you can cast 'handler' and call methods directly without reflection.
     */
    @SuppressWarnings("unchecked")
    private JsonDataPayload invokeHandlerMethod(Object handler, String methodName, Object[] args, Class<?>[] paramTypes) {
        try {
            var method = handler.getClass().getMethod(methodName, paramTypes);
            return (JsonDataPayload) method.invoke(handler, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke " + methodName + " on " + handler.getClass().getSimpleName(), e);
        }
    }
}