package test.cdp;


import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;

public class KeycloakTestLauncher {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakTestLauncher.class);

    public static void main(String[] args) {
    	 String configPath = "./configs/log4j.xml";  // your custom path
         
         System.out.println("Loading Log4j2 config from: " + configPath);
         Configurator.initialize(null, configPath);
    	
        Vertx vertx = Vertx.vertx();

        // Deploy the admin Verticle
        vertx.deployVerticle(new KeycloakRouterVerticle(), res -> {
        	System.out.println("Vertx.deployVerticle OK");
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
