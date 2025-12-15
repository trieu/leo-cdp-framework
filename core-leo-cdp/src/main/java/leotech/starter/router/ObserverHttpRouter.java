package leotech.starter.router;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.handler.ObserverHttpGetHandler;
import leotech.cdp.handler.ObserverHttpPostHandler;
import leotech.cdp.handler.WebhookDataHandler;
import leotech.system.common.BaseHttpHandler;
import leotech.system.common.BaseHttpRouter;
import leotech.system.model.DeviceInfo;
import leotech.system.template.HandlebarsTemplateUtil;
import leotech.system.util.DeviceInfoUtil;
import leotech.system.util.HttpTrackingUtil;
import leotech.system.version.SystemMetaData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;

/**
 * HTTP POST Event Data handler
 * 
 * diagram: leo-cdp-event-observer-data-flow.md
 * 
 * @author @tantrieuf31
 * @since 2021
 */
public final class ObserverHttpRouter extends BaseHttpRouter {

	// ------------------------------------------------------------
	// CONSTANTS
	// ------------------------------------------------------------

	private static final String SCREEN = "screen";
	public static final String INVALID = "invalid";
	public static final String FAILED = "failed";
	public static final String OK = "ok";

	public static final String PREFIX_TARGET_MEDIA_CLICK_TRACKING = "/ct/";
	public static final String PREFIX_TARGET_MEDIA_QR_CODE_TRACKING = "/qrct/";

	public static final String PREFIX_CONTEXT_SESSION_PROFILE_INIT = "/cxs-pf-init";
	public static final String PREFIX_CONTEXT_SESSION_PROFILE_UPDATE = "/cxs-pf-update";

	// HTML render
	public static final String PREFIX_CONTENT = "/content/";
	public static final String PREFIX_PRESENTATION = "/presentation/";
	public static final String PREFIX_WEBFORM = "/webform";
	public static final String PREFIX_FEEDBACK_SCORE = "/feedback-score";
	public static final String PREFIX_WEB_TEMPLATE_HTML = "/webhtml";

	// Recommender JSON
	public static final String PREFIX_JSON_RECOMMENDER_PRODUCTS = "/ris/json/products";
	public static final String PREFIX_JSON_RECOMMENDER_CONTENTS = "/ris/json/contents";

	// Recommender HTML
	public static final String PREFIX_HTML_RECOMMENDER_PRODUCTS = "/ris/html/products";
	public static final String PREFIX_HTML_RECOMMENDER_CONTENTS = "/ris/html/contents";

	public static final String PREFIX_STANDARD_EVENT_PREFIX = "/et";
	public static final String PREFIX_EVENT_VIEW = "/etv";
	public static final String PREFIX_EVENT_ACTION = "/eta";
	public static final String PREFIX_EVENT_CONVERSION = "/etc";
	public static final String PREFIX_EVENT_FEEDBACK = "/efb";

	public static final String PREFIX_TOUCHPOINT = "/touchpoint";

	public static final String BASE_URL_TARGET_MEDIA_CLICK_TRACKING = "https://" + SystemMetaData.DOMAIN_CDP_OBSERVER
			+ PREFIX_TARGET_MEDIA_CLICK_TRACKING;

	public static final String LOG_CLASSNAME = ObserverHttpRouter.class.getName();
	private static final JedisPool jedisPool = RedisClientFactory.buildRedisPool("realtimeDataStats");

	// ------------------------------------------------------------

	public ObserverHttpRouter(RoutingContext context, String host, Integer port) {
		super(context, host, port);
		setupTemplateCache();
	}

	private void setupTemplateCache() {
		if (SystemMetaData.isEnableCachingViewTemplates()) {
			HandlebarsTemplateUtil.enableUsedCache();
		} else {
			HandlebarsTemplateUtil.disableUsedCache();
		}
	}

	@Override
	public void process() {
		incrementRequestAsync();

		HttpServerRequest req = context.request();
		HttpServerResponse resp = context.response();

		String urlPath = safe(req.path());
		String method = req.rawMethod();
		MultiMap headers = req.headers();
		MultiMap params = req.params();

		prepareDefaultHeaders(resp);

		String origin = safe(headers.get(BaseHttpHandler.ORIGIN), "*");
		String screenSize = safe(params.get(SCREEN),"");
		String userAgent = safe(req.getHeader(BaseHttpHandler.USER_AGENT),"");
		DeviceInfo device = DeviceInfoUtil.getDeviceInfo(userAgent, screenSize);

		System.out.println("contentType=" + headers.get(BaseHttpHandler.CONTENT_TYPE) + " urlPath=" + urlPath);

		try {
			// CDP public API
			if (urlPath.startsWith(CdpPublicApiRouter.PREFIX_API)) {
				new CdpPublicApiRouter(context, host, port).process();
				return;
			}

			// Webhook
			boolean handled = WebhookDataHandler.process(context, req, urlPath, headers, params, resp, origin, device);

			if (!handled) {
				dispatchHandler(method, req, urlPath, headers, params, resp, origin, device);
				return;
			}

			resp.end("CDP Observer_" + nodeInfo);

		} catch (Exception e) {
			System.err.println("urlPath=" + urlPath + " error=" + e.getMessage());
			e.printStackTrace();
		}
	}

	// ------------------------------------------------------------
	// HELPERS
	// ------------------------------------------------------------

	private void incrementRequestAsync() {
		new RedisCommand<Void>(jedisPool) {
			@Override
			protected Void build(Jedis jedis) throws JedisException {
				jedis.hincrBy(LOG_CLASSNAME, nodeId, 1);
				return null;
			}
		}.executeAsync();
	}

	private void prepareDefaultHeaders(HttpServerResponse resp) {
		MultiMap out = resp.headers();
		out.set(CONNECTION, HttpTrackingUtil.HEADER_CONNECTION_CLOSE);
		out.set(POWERED_BY, SERVER_VERSION);
	}

	private void dispatchHandler(String method, HttpServerRequest req, String urlPath, MultiMap headers,
			MultiMap params, HttpServerResponse resp, String origin, DeviceInfo device) {

		PROCESSORS.submit(() -> {
			MultiMap out = resp.headers();

			if ("GET".equalsIgnoreCase(method)) {
				ObserverHttpGetHandler.process(context, req, urlPath, headers, params, resp, out, device, origin,
						nodeInfo);
			} else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
				ObserverHttpPostHandler.process(context, req, urlPath, headers, params, resp, out, device, origin,
						nodeInfo);
			} else {
				resp.end("NO handler found");
			}
		});
	}

	private static String safe(String s) {
		return StringUtil.safeString(s);
	}

	private static String safe(String s, String def) {
		return StringUtil.safeString(s, def);
	}
}
