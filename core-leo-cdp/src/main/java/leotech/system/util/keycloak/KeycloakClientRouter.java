package leotech.system.util.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * KeycloakRouterFactory responsible for handling SSO routing and
 * initialization.
 * 
 * @author Trieu Nguyen
 * @since 2025
 */
public class KeycloakClientRouter {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakClientRouter.class);

    private final Vertx vertx;
    private final KeycloakConfig config;
    private final AuthKeycloakHandlers handlers;

    public KeycloakClientRouter(Vertx vertx, KeycloakConfig config, AuthKeycloakHandlers handlers) {
        this.vertx = vertx;
        this.config = config;
        this.handlers = handlers;
    }

    public Router buildRouter() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        configureBaseRoutes(router);

        if (config.enabled) {
            configureKeycloakRoutes(router);
        } else {
            configureFallbackRoutes(router);
        }

        return router;
    }

    private void configureBaseRoutes(Router router) {
        router.get(SsoRoutePaths.IS_ENABLED)
                .handler(ctx -> {
                    JsonObject res = new JsonObject().put("ok", config.enabled);
                    ctx.response()
                            .putHeader(KeycloakConstants.HEADER_CONTENT_TYPE, KeycloakConstants.MIME_JSON)
                            .end(res.encode());
                });
    }

    private void configureKeycloakRoutes(Router router) {
        router.get(SsoRoutePaths.LOGIN).handler(handlers::handleLogin);
        router.get(SsoRoutePaths.REFRESH).handler(handlers::handleRefreshToken);
        router.get(SsoRoutePaths.CALLBACK).handler(handlers::handleCallback);
        router.get(SsoRoutePaths.LOGOUT).handler(handlers::handleLogout);

        router.get(SsoRoutePaths.SESSION).handler(handlers::handleSession);
        router.get(SsoRoutePaths.CHECK_ROLE).handler(handlers::handleCheckRole);

        router.get(SsoRoutePaths.ME).handler(handlers::handleSession);
        router.get(SsoRoutePaths.ROOT_PREFIX).handler(handlers::handleInfo);
        router.get(SsoRoutePaths.ROOT).handler(handlers::handleInfo);

        router.get(SsoRoutePaths.ERROR).handler(this::handleErrorRoute);
    }

    private void configureFallbackRoutes(Router router) {
        router.get(SsoRoutePaths.SESSION)
                .handler(ctx -> ctx.response().setStatusCode(503)
                        .end("Keycloak not enabled or failed to initialize."));

        router.get(SsoRoutePaths.LOGIN)
                .handler(ctx -> ctx.response()
                        .setStatusCode(500)
                        .putHeader(KeycloakConstants.HEADER_CONTENT_TYPE, KeycloakConstants.MIME_JSON)
                        .end(new JsonObject().put("error", "Keycloak not configured").encode()));

        router.get(SsoRoutePaths.LOGOUT)
                .handler(ctx -> ctx.response()
                        .putHeader(KeycloakConstants.HEADER_LOCATION, "/")
                        .setStatusCode(303)
                        .end());
    }

    private void handleErrorRoute(RoutingContext ctx) {
        String error = ctx.queryParam("error").stream().findFirst().orElse(null);
        String desc = ctx.queryParam("description").stream().findFirst().orElse(null);

        JsonObject payload = new JsonObject()
                .put("error", error)
                .put("message", "SSO Error: " + error)
                .put("description", desc)
                .put("timestamp", System.currentTimeMillis() / 1000);

        ctx.response()
                .setStatusCode(400)
                .putHeader(KeycloakConstants.HEADER_CONTENT_TYPE, KeycloakConstants.MIME_JSON)
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
}
