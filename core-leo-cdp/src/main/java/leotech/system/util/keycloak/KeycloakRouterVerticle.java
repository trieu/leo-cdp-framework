package leotech.system.util.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import redis.clients.jedis.JedisPool;
import rfx.core.nosql.jedis.RedisClientFactory;

public class KeycloakRouterVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger("leobot-admin");
	private static final String HTTP_HOST = "0.0.0.0";
	private static final int HTTP_PORT = 8888;

	// Shared pool resource
	static JedisPool jedisPool = RedisClientFactory.buildRedisPool("clusterInfoRedis");

	// https://chatgpt.com/c/692059a5-b9c0-8324-acec-74b641295962
	
	@Override
	public void start(Promise<Void> startPromise) {
		try {
			// 1. Initialize Configuration
			KeycloakConfig config = new KeycloakConfig();

			logger.info("KEYCLOAK_URL: {}", config.url);
			logger.info("KEYCLOAK_CLIENT_ID: {}", config.clientId);

			// 2. Setup WebClient
			WebClientOptions options = new WebClientOptions()
					.setSsl(config.url != null && config.url.toLowerCase().startsWith("https"))
					.setTrustAll(!config.verifySSL) // dev mode
					.setVerifyHost(config.verifySSL); // disable hostname verify in dev

			WebClient webClient = WebClient.create(vertx, options);

			// 3. Repositories
			SessionRepository sessionRepo = new SessionRepository(vertx, jedisPool);

			// 4. Handlers
			AuthKeycloakHandlers authHandlers = new AuthKeycloakHandlers(config, sessionRepo, webClient);

			// 5. Router
			Router router = Router.router(vertx);
			router.route().handler(BodyHandler.create());

			router.get("/_leocdp/is-admin-ready")
					.handler(ctx -> ctx.response().putHeader("Content-Type", "application/json")
							.end(new JsonObject().put("ok", config.enabled).encode()));

			if (config.enabled) {
				registerKeycloakRoutes(router, authHandlers);
			} else {
				registerFallbackRoutes(router);
			}

			// 6. HTTP Server
			vertx.createHttpServer().requestHandler(router).listen(HTTP_PORT, HTTP_HOST, ar -> {
				if (ar.succeeded()) {
					logger.info("✅ Admin router started on {}:{}", HTTP_HOST, ar.result().actualPort());
					startPromise.complete();
				} else {
					logger.error("❌ Failed to start HTTP server", ar.cause());
					startPromise.fail(ar.cause());
				}
			});

		} catch (Exception e) {
			logger.error("❌ Failed to start KeycloakRouterVerticle", e);
			startPromise.fail(e);
		}
	}

	private void registerKeycloakRoutes(Router router, AuthKeycloakHandlers handlers) {
		router.get("/_leocdp/sso/login").handler(handlers::handleLogin);
		router.get("/_leocdp/sso/refresh").handler(handlers::handleRefreshToken);
		router.get("/_leocdp/sso/callback").handler(handlers::handleCallback);
		router.get("/_leocdp/sso/logout").handler(handlers::handleLogout);

		// Protected routes
		router.get("/admin").handler(handlers::handleAdmin);
		router.get("/api/data").handler(handlers::handleDataApi);

		// Error route
		router.get("/_leocdp/sso/error").handler(ctx -> {
			String error = ctx.request().getParam("error");
			String desc = ctx.request().getParam("description");
			JsonObject payload = new JsonObject().put("error", error).put("message", "SSO Error: " + error)
					.put("description", desc).put("timestamp", System.currentTimeMillis() / 1000);

			ctx.response().setStatusCode(400).putHeader("Content-Type", "application/json").end(payload.encode());
		});

		// Legacy/Alias route for "me"
		router.get("/_leocdp/sso/me").handler(handlers::handleAdmin);
	}

	private void registerFallbackRoutes(Router router) {
		router.get("/admin")
				.handler(ctx -> ctx.response().setStatusCode(503).end("Keycloak not enabled or failed to initialize."));
		router.get("/_leocdp/sso/login")
				.handler(ctx -> ctx.response().setStatusCode(500).end("{\"error\": \"Keycloak not configured\"}"));
		router.get("/_leocdp/sso/logout")
				.handler(ctx -> ctx.response().putHeader("Location", "/").setStatusCode(303).end());
	}
}