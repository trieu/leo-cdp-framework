package leotech.system.util.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import redis.clients.jedis.JedisPool;
import rfx.core.nosql.jedis.RedisClientFactory;

/**
 * KeycloakRouterFactory responsible for handling SSO routing and
 * initialization.
 * 
 * @author Trieu Nguyen
 * @since 2025
 */
public class KeycloakClientSsoRouter {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakClientSsoRouter.class);

	private static final JedisPool jedisPool = RedisClientFactory.buildRedisPool("clusterInfoRedis");
	private final KeycloakConfig config;
	private final AuthKeycloakHandlers handlers;

	public KeycloakClientSsoRouter( KeycloakConfig config, AuthKeycloakHandlers handlers) {
		this.config = config;
		this.handlers = handlers;
	}

	public Router configureRouter(Router router) {
		configureBaseRoutes(router);
		if (config.isEnabled()) {
			configureKeycloakRoutes(router);
		} else {
			configureFallbackRoutes(router);
		}
		return router;
	}


	private void configureBaseRoutes(Router router) {
		router.get(SsoRoutePaths.IS_ENABLED).handler(ctx -> {
			JsonObject res = new JsonObject().put("ok", config.isEnabled());
			ctx.response().putHeader(KeycloakConstants.HEADER_CONTENT_TYPE, KeycloakConstants.MIME_JSON)
					.end(res.encode());
		});
	}

	private void configureKeycloakRoutes(Router router) {
		router.get(SsoRoutePaths.ROOT_PREFIX).handler(handlers::handleInfo);
		router.get(SsoRoutePaths.ME).handler(handlers::handleInfo);
		
		router.get(SsoRoutePaths.LOGIN).handler(handlers::handleLogin);
		router.get(SsoRoutePaths.REFRESH).handler(handlers::handleRefreshToken);
		router.get(SsoRoutePaths.CALLBACK).handler(handlers::handleCallback);
		router.get(SsoRoutePaths.LOGOUT).handler(handlers::handleLogout);

		router.get(SsoRoutePaths.SESSION).handler(handlers::handleSession);
		router.get(SsoRoutePaths.CHECK_ROLE).handler(handlers::handleCheckRole);

		router.get(SsoRoutePaths.ERROR).handler(this::handleErrorRoute);
	}

	private void configureFallbackRoutes(Router router) {
		router.get(SsoRoutePaths.SESSION)
				.handler(ctx -> ctx.response().setStatusCode(503).end("Keycloak not enabled or failed to initialize."));

		router.get(SsoRoutePaths.LOGIN)
				.handler(ctx -> ctx.response().setStatusCode(500)
						.putHeader(KeycloakConstants.HEADER_CONTENT_TYPE, KeycloakConstants.MIME_JSON)
						.end(new JsonObject().put("error", "Keycloak not configured").encode()));

		router.get(SsoRoutePaths.LOGOUT).handler(
				ctx -> ctx.response().putHeader(KeycloakConstants.HEADER_LOCATION, "/").setStatusCode(303).end());
	}

	private void handleErrorRoute(RoutingContext ctx) {
		String error = ctx.queryParam("error").stream().findFirst().orElse(null);
		String desc = ctx.queryParam("description").stream().findFirst().orElse(null);

		JsonObject payload = new JsonObject().put("error", error).put("message", "SSO Error: " + error)
				.put("description", desc).put("timestamp", System.currentTimeMillis() / 1000);

		ctx.response().setStatusCode(400).putHeader(KeycloakConstants.HEADER_CONTENT_TYPE, KeycloakConstants.MIME_JSON)
				.end(payload.encode());
	}

	/**
	 * Route constants stay here.
	 */
	public static final class SsoRoutePaths {
		public static final String PREFIX = "/_ssocdp";

		public static final String IS_ENABLED = PREFIX + "/is-sso-enabled";
		public static final String LOGIN = PREFIX + "/login";
		public static final String REFRESH = PREFIX + "/refresh";
		public static final String CALLBACK = PREFIX + "/callback";
		public static final String LOGOUT = PREFIX + "/logout";
		public static final String SESSION = PREFIX + "/session";
		public static final String CHECK_ROLE = PREFIX + "/checkrole";
		public static final String ERROR = PREFIX + "/error";
		public static final String ME = PREFIX + "/me";
		public static final String ROOT_PREFIX = PREFIX + "/";
		
		public static final String ROOT = "/";
	}
	
	
	public static Router startKeyCloakRouter(Vertx vertxInstance , Router router) {
		// Load config from database
		KeycloakConfig config = KeycloakConfig.getInstance();
		logger.info("KEYCLOAK Settings -> URL: [{}], ClientId: [{}], Callback: [{}]", config.getUrl(), config.getClientId(),config.getCallbackUrl());

		// WebClient
		WebClient webClient = createWebClient(vertxInstance, config);

		// Session repo
		SessionRepository sessionRepo = new SessionRepository(vertxInstance, jedisPool);

		// Handler wrapper
		AuthKeycloakHandlers handlers = new AuthKeycloakHandlers(config, sessionRepo, webClient);

		// Router factory builds Router
		KeycloakClientSsoRouter routerFactory = new KeycloakClientSsoRouter(config, handlers);
		routerFactory.configureRouter(router);
		return router;
	}

	public static WebClient createWebClient(Vertx vertxInstance, KeycloakConfig config) {
		boolean isHttps = config.getUrl() != null && config.getUrl().toLowerCase().startsWith("https");
		WebClientOptions opt = new WebClientOptions().setSsl(isHttps).setTrustAll(!config.isVerifySSL())
				.setVerifyHost(config.isVerifySSL());

		return WebClient.create(vertxInstance, opt);
	}
}
