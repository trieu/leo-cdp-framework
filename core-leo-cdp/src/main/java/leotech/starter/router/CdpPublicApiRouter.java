package leotech.starter.router;

import java.util.Map;
import java.util.function.Consumer;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.handler.api.BaseApiHandler;
import leotech.cdp.handler.api.EventApiHandler;
import leotech.cdp.handler.api.ProfileApiHandler;
import leotech.cdp.handler.api.SegmentApiHandler;
import leotech.system.common.BaseWebRouter;
import leotech.system.model.JsonDataPayload;

/**
 * LEO CDP public API router for mobile, Flutter, and third-party
 * integrations.<br>
 *
 * Improves readability and performance by: <br>
 * - Removing long if/else chains <br>
 * - Reusing handler instances (no new objects per request) <br>
 * - Centralizing routing logic in static lookup tables <br>
 * 
 * @author tantrieuf31
 * @since 2025
 */
public class CdpPublicApiRouter extends BaseWebRouter {

	// ---- API ROOT PREFIXES ----
	
	public static final String PREFIX_API = "/api/";

	public static final String API_PROFILE = PREFIX_API + "profile";
	public static final String API_SEGMENT = PREFIX_API +  "segment";
	public static final String API_EVENT = PREFIX_API +  "event";


	// ---- Shared Handler Instances (stateless => safe) ----
	private static final ProfileApiHandler PROFILE = new ProfileApiHandler();
	private static final EventApiHandler EVENT = new EventApiHandler();
	private static final SegmentApiHandler SEGMENT = new SegmentApiHandler();

	/**
	 * Routing table for POST handlers. <br>
	 *
	 * Key: API prefix Value: handler instance
	 */
	private static final Map<String, BaseApiHandler> POST_ROUTES = Map.of(API_PROFILE, PROFILE, API_EVENT, EVENT, API_SEGMENT, SEGMENT);

	/**
	 * Same routing table reused for GET handlers.
	 */
	private static final Map<String, BaseApiHandler> GET_ROUTES = POST_ROUTES;

	public CdpPublicApiRouter(RoutingContext context, String host, int port) {
		super(context, host, port);
	}

	@Override
	public void process() throws Exception {
		this.handle(this.context);
	}

	// ========================================================================
	// POST
	// ========================================================================

	@Override
	protected void callHttpPostHandler(HttpServerRequest req, String userSession, String uri, JsonObject paramJson,
			Consumer<JsonDataPayload> done) {

		PROCESSORS.submit(() -> {
			JsonDataPayload payload;
			try {
				BaseApiHandler handler = matchRoute(uri, POST_ROUTES);

				if (handler != null) {
					payload = handler.handlePost(req, uri, paramJson);
				} else {
					payload = JsonDataPayload.fail("No handler for API " + uri);
				}

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				payload = JsonDataPayload.fail(e.getMessage(), 507);
			} catch (Throwable e) {
				e.printStackTrace();
				payload = JsonDataPayload.fail(e.getMessage(), 500);
			}

			done.accept(payload);
		});
	}

	// ========================================================================
	// GET
	// ========================================================================

	@Override
	protected void callHttpGetHandler(HttpServerRequest req, String userSession, String uri, MultiMap urlParams,
			Consumer<JsonDataPayload> done) {

		PROCESSORS.submit(() -> {
			JsonDataPayload payload;

			try {
				BaseApiHandler handler = matchRoute(uri, GET_ROUTES);

				if (handler != null) {
					payload = handler.handleGet(req, uri, urlParams);
				} else {
					payload = JsonDataPayload.fail("No handler for API " + uri);
				}

			} catch (Throwable e) {
				e.printStackTrace();
				payload = JsonDataPayload.fail(e.getMessage(), 500);
			}

			done.accept(payload);
		});
	}

	// ========================================================================
	// Helper: Route Matching
	// ========================================================================

	/**
	 * Matches a URI to the correct handler based on prefix. <br>
	 * Using prefix-matching keeps compatibility with nested APIs:
	 * /api/profile/list, /api/profile/update, etc.
	 */
	private static BaseApiHandler matchRoute(String uri, Map<String, BaseApiHandler> routes) {
		for (Map.Entry<String, BaseApiHandler> e : routes.entrySet()) {
			if (uri.startsWith(e.getKey())) {
				return e.getValue();
			}
		}
		return null;
	}
}
