package leotech.system.util.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import redis.clients.jedis.JedisPool;
import rfx.core.nosql.jedis.RedisClientFactory;

/**
 * KeycloakVerticle responsible for handling SSO routing and initialization.
 * 
 * @author Trieu Nguyen
 * @since 2025
 */
public class KeycloakVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakVerticle.class);

	private String host = "0.0.0.0";
	private int port = 9079;

	private static final JedisPool jedisPool = RedisClientFactory.buildRedisPool("clusterInfoRedis");

	public KeycloakVerticle() {
		logger.info("event=router_init status=configured host={} port={}", host, port);
	}

	public KeycloakVerticle(String host, int port) {
		this.host = host;
		this.port = port;
		logger.info("event=router_init status=configured host={} port={}", host, port);
	}
	
	private void startHttpServer(Router router, Promise<Void> startPromise) {
		vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true)).requestHandler(router)
				.listen(port, host, ar -> {
					if (ar.succeeded()) {
						logger.info("✅ Keycloak HTTP server started on {}:{}", host, ar.result().actualPort());
						startPromise.complete();
					} else {
						logger.error("❌ Failed to start server on port {}", port, ar.cause());
						startPromise.fail(ar.cause());
					}
				});
	}

	@Override
	public void start(Promise<Void> startPromise) {
		try {
			// Load config
			KeycloakConfig config = new KeycloakConfig();
			logger.info("KEYCLOAK Settings -> URL: [{}], ClientId: [{}], Callback: [{}]", config.url, config.clientId,
					config.callbackUrl);

			// WebClient
			WebClient webClient = createWebClient(config);

			// Session repo
			SessionRepository sessionRepo = new SessionRepository(vertx, jedisPool);

			// Handler wrapper
			AuthKeycloakHandlers handlers = new AuthKeycloakHandlers(config, sessionRepo, webClient);

			// Router factory builds Router
			KeycloakClientRouter routerFactory = new KeycloakClientRouter(vertx, config, handlers);
			Router router = routerFactory.buildRouter();

			// TODO
			
			startHttpServer(router, startPromise);

		} catch (Exception e) {
			logger.error("❌ Failed to start KeycloakVerticle", e);
			startPromise.fail(e);
		}
	}

	private WebClient createWebClient(KeycloakConfig config) {
		boolean isHttps = config.url != null && config.url.toLowerCase().startsWith("https");
		WebClientOptions opt = new WebClientOptions().setSsl(isHttps).setTrustAll(!config.verifySSL)
				.setVerifyHost(config.verifySSL);

		return WebClient.create(vertx, opt);
	}


}
