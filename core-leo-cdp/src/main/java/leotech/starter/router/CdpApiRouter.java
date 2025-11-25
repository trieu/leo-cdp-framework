package leotech.starter.router;

import java.util.function.Consumer;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.handler.api.EventApiHandler;
import leotech.cdp.handler.api.ProductApiHandler;
import leotech.cdp.handler.api.ProfileApiHandler;
import leotech.cdp.handler.api.SegmentApiHandler;
import leotech.cdp.handler.api.SystemApiHandler;
import leotech.system.common.BaseWebRouter;
import leotech.system.model.JsonDataPayload;

/**
 * LEO CDP public API for mobile tracking and third-party platform
 * 
 * @author tantrieuf31
 * @since 2025
 *
 */
public class CdpApiRouter extends BaseWebRouter {

	public CdpApiRouter(RoutingContext context, String host, int port) {
		super(context, host, port);
	}

	// public API to extend CDP features
	public static final String API_JOURNEY = "/api/journey";
	public static final String API_PROFILE = "/api/profile";
	public static final String API_SEGMENT = "/api/segment";
	public static final String API_EVENT = "/api/event";
	public static final String API_PRODUCT = "/api/product";
	public static final String API_CONTENT = "/api/content";
	public static final String API_SYSTEM = "/api/system";

	@Override
	public void process() throws Exception {
		this.handle(this.context);
	}

	@Override
	protected void callHttpPostHandler(HttpServerRequest req, String userSession, String uri, JsonObject paramJson,
			Consumer<JsonDataPayload> done) {
		{
			PROCESSORS.submit(() -> {
				JsonDataPayload payload = null;
				try {

					// to create or update profile
					if (uri.startsWith(API_PROFILE)) {
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
					else {
						payload = JsonDataPayload.fail("No handler for API " + uri);
					}
				} catch (Throwable e) {
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

	}

	@Override
	protected void callHttpGetHandler(HttpServerRequest req, String userSession, String uri, MultiMap urlParams,
			Consumer<JsonDataPayload> done) {
		// ---------- API handler ----------
		PROCESSORS.submit(() -> {
			JsonDataPayload payload = null;
			try {
				// to create or update profile
				if (uri.startsWith(API_PROFILE)) {
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
				else {
					payload = JsonDataPayload.fail("No handler for API " + uri);
				}
			} catch (Throwable e) {
				e.printStackTrace();
				payload = JsonDataPayload.fail(e.getMessage(), 500);
			}
			done.accept(payload);
		});
	}
}
