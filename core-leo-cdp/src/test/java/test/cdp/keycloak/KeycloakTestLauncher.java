package test.cdp.keycloak;

import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import leotech.system.util.keycloak.KeycloakRouterVerticle;

public class KeycloakTestLauncher {
	private static final Logger logger = LoggerFactory.getLogger(KeycloakTestLauncher.class);
	
    // Server Constants
    private static final String HTTP_HOST = "0.0.0.0";
    private static final int HTTP_PORT = 9079;

	public static void main(String[] args) {
		String configPath = "./configs/log4j.xml"; // your custom path

		System.out.println("Loading Log4j2 config from: " + configPath);
		Configurator.initialize(null, configPath);

		Vertx vertx = Vertx.vertx();

		// Deploy the admin Verticle
		vertx.deployVerticle(new KeycloakRouterVerticle(HTTP_HOST, HTTP_PORT), res -> {
			if (res.succeeded()) {
				logger.info("🚀 AdminRouterVerticle deployed successfully! ID = {}", res.result());
			} else {
				logger.error("❌ Deployment failed", res.cause());
			}
		});

		// Optional: graceful shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("🛑 Shutting down Vert.x...");
			vertx.close();
		}));
	}
}
