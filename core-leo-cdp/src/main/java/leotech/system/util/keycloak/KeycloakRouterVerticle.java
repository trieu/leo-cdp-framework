package leotech.system.util.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import redis.clients.jedis.JedisPool;
import rfx.core.nosql.jedis.RedisClientFactory;

/**
 * Verticle responsible for handling SSO routing and initialization.
 * Refactored for cleaner OOP structure and maintainability.
 * 
 * @author Trieu Nguyen
 * @since 2025
 */
public class KeycloakRouterVerticle extends AbstractVerticle {


	private static final Logger logger = LoggerFactory.getLogger("leobot-admin");

	private String host = "0.0.0.0";
	private int port = 9079;
	
	public KeycloakRouterVerticle() {
		// default
		logger.info("event=router_init status=configured host={} port={}", host, port);
	}

	public KeycloakRouterVerticle(String host, int port) {
		super();
		this.host = host;
		this.port = port;
		logger.info("event=router_init status=configured host={} port={}", host, port);
	}

	// Shared Resource (Consider dependency injection in the future)
    private static final JedisPool jedisPool = RedisClientFactory.buildRedisPool("clusterInfoRedis");

    /**
     * Inner static class to encapsulate Route Constants.
     * Prevents hardcoding URLs throughout the application.
     */
    public static final class SsoRoutePaths {
        // The requested constant prefix
        public static final String PREFIX = "/_ssocdp";

        // Sub-paths
        public static final String IS_ENABLED = PREFIX + "/is-sso-enabled";
        public static final String LOGIN = PREFIX + "/login";
        public static final String REFRESH = PREFIX + "/refresh";
        public static final String CALLBACK = PREFIX + "/callback";
        public static final String LOGOUT = PREFIX + "/logout";
        public static final String ADMIN = PREFIX + "/admin";
        public static final String API_DATA = PREFIX + "/api/data";
        public static final String ERROR = PREFIX + "/error";
        public static final String ME = PREFIX + "/me";
        public static final String ROOT_PREFIX = PREFIX + "/";
        public static final String ROOT = "/";
    }

    @Override
    public void start(Promise<Void> startPromise) {
        try {
            // 1. Initialize Configuration
            KeycloakConfig config = new KeycloakConfig();
            logConfig(config);

            // 2. Initialize Services
            WebClient webClient = createWebClient(config);
            SessionRepository sessionRepo = new SessionRepository(vertx, jedisPool);
            AuthKeycloakHandlers authHandlers = new AuthKeycloakHandlers(config, sessionRepo, webClient);

            // 3. Build Router
            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());

            // 4. Register Routes
            configureBaseRoutes(router, config);

            if (config.enabled) {
                configureKeycloakRoutes(router, authHandlers);
            } else {
                configureFallbackRoutes(router);
            }

            // 5. Start HTTP Server
            startHttpServer(router, startPromise);

        } catch (Exception e) {
            logger.error("❌ Failed to start KeycloakRouterVerticle", e);
            startPromise.fail(e);
        }
    }

    private void logConfig(KeycloakConfig config) {
        logger.info("KEYCLOAK Settings -> URL: [{}], ClientId: [{}], Callback: [{}]", 
            config.url, config.clientId, config.callbackUrl);
    }

    private WebClient createWebClient(KeycloakConfig config) {
        boolean isHttps = config.url != null && config.url.toLowerCase().startsWith("https");
        WebClientOptions options = new WebClientOptions()
                .setSsl(isHttps)
                .setTrustAll(!config.verifySSL) // dev mode: trust all
                .setVerifyHost(config.verifySSL); // dev mode: disable hostname verification

        return WebClient.create(vertx, options);
    }

    private void configureBaseRoutes(Router router, KeycloakConfig config) {
        // Public check for frontend to know if SSO is on
        router.get(SsoRoutePaths.IS_ENABLED)
                .handler(ctx -> {
                    JsonObject response = new JsonObject().put("ok", config.enabled);
                    ctx.response()
                       .putHeader(AuthKeycloakHandlers.HEADER_CONTENT_TYPE, AuthKeycloakHandlers.MIME_JSON)
                       .end(response.encode());
                });
    }

    private void configureKeycloakRoutes(Router router, AuthKeycloakHandlers handlers) {
        // Authentication Flow
        router.get(SsoRoutePaths.LOGIN).handler(handlers::handleLogin);
        router.get(SsoRoutePaths.REFRESH).handler(handlers::handleRefreshToken);
        router.get(SsoRoutePaths.CALLBACK).handler(handlers::handleCallback);
        router.get(SsoRoutePaths.LOGOUT).handler(handlers::handleLogout);

        // Protected Areas
        router.get(SsoRoutePaths.ADMIN).handler(handlers::handleAdmin);
        router.get(SsoRoutePaths.API_DATA).handler(handlers::handleDataApi);
        
        // Aliases / Info
        router.get(SsoRoutePaths.ME).handler(handlers::handleAdmin); // Legacy alias
        router.get(SsoRoutePaths.ROOT_PREFIX).handler(handlers::handleInfo);
        router.get(SsoRoutePaths.ROOT).handler(handlers::handleInfo);

        // Error Handling
        router.get(SsoRoutePaths.ERROR).handler(this::handleErrorRoute);
    }

    private void configureFallbackRoutes(Router router) {
        router.get(SsoRoutePaths.ADMIN)
                .handler(ctx -> ctx.response()
                        .setStatusCode(503)
                        .end("Keycloak not enabled or failed to initialize."));

        router.get(SsoRoutePaths.LOGIN)
                .handler(ctx -> ctx.response()
                        .setStatusCode(500)
                        .putHeader("Content-Type", AuthKeycloakHandlers.MIME_JSON)
                        .end(new JsonObject().put("error", "Keycloak not configured").encode()));

        router.get(SsoRoutePaths.LOGOUT)
                .handler(ctx -> ctx.response()
                        .putHeader(AuthKeycloakHandlers.HEADER_LOCATION, "/")
                        .setStatusCode(303)
                        .end());
    }

    /**
     * Extracted error handler logic to keep route configuration clean.
     */
    private void handleErrorRoute(RoutingContext ctx) {
        String error = ctx.request().getParam("error");
        String desc = ctx.request().getParam("description");
        
        JsonObject payload = new JsonObject()
                .put("error", error)
                .put("message", "SSO Error: " + error)
                .put("description", desc)
                .put("timestamp", System.currentTimeMillis() / 1000);

        ctx.response()
                .setStatusCode(400)
                .putHeader(AuthKeycloakHandlers.HEADER_CONTENT_TYPE, AuthKeycloakHandlers.MIME_JSON)
                .end(payload.encode());
    }

    private void startHttpServer(Router router, Promise<Void> startPromise) {
        vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true))
                .requestHandler(router)
                .listen(port, host, ar -> {
                    if (ar.succeeded()) {
                        logger.info("✅ Admin router started on {}:{}", host, ar.result().actualPort());
                        startPromise.complete();
                    } else {
                        logger.error("❌ Failed to start HTTP server on port {}", port, ar.cause());
                        startPromise.fail(ar.cause());
                    }
                });
    }
}