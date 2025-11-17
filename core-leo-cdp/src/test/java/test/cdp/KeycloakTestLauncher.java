package test.cdp;


import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeycloakTestLauncher {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakTestLauncher.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // Deploy the admin Verticle
        vertx.deployVerticle(new KeycloakRouterVerticle(), res -> {
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
