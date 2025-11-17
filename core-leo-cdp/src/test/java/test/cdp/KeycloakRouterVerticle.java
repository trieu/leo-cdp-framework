package test.cdp;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future; // Vert.x 3 uses Future in start
import io.vertx.core.Handler;
import io.vertx.core.MultiMap; // For sendForm
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;

// https://documentation.cloud-iam.com/resources/recaptcha.html
public class KeycloakRouterVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger("leobot-admin");

    private WebClient webClient;
    static JedisPool jedisPool = RedisClientFactory.buildRedisPool("clusterInfoRedis");
    private boolean keycloakEnabled;
    private boolean verifySSL;

    private String keycloakUrl;
    private String keycloakRealm;
    private String keycloakClientId;
    private String keycloakClientSecret;
    private String keycloakCallbackUrl;

    @Override
    public void start(Future<Void> startFuture) { // Vert.x 3 API
        try {
            // Load config from environment
            keycloakEnabled = getEnvBool("KEYCLOAK_ENABLED", true);
            verifySSL = getEnvBool("KEYCLOAK_VERIFY_SSL", true);

            keycloakUrl = System.getenv("KEYCLOAK_URL");
            keycloakRealm = System.getenv("KEYCLOAK_REALM");
            keycloakClientId = System.getenv("KEYCLOAK_CLIENT_ID");
            keycloakClientSecret = System.getenv("KEYCLOAK_CLIENT_SECRET");
            keycloakCallbackUrl = System.getenv("KEYCLOAK_CALLBACK_URL");

            if (keycloakClientSecret == null || keycloakClientSecret.isEmpty()) {
                throw new RuntimeException("KEYCLOAK_CLIENT_SECRET is missing.");
            }

            // Setup Vert.x tools
            webClient = WebClient.create(vertx);

            Router router = Router.router(vertx);

            router.route().handler(BodyHandler.create());

            // Simple health check
            router.get("/_leoai/is-admin-ready").handler(this::handleReady);

            if (keycloakEnabled) {
                registerKeycloakRoutes(router);
            } else {
                registerFallbackRoutes(router);
            }

            // Start HTTP server
            vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(8600, server -> { // Vert.x 3 listen signature
                        if (server.succeeded()) {
                            logger.info("✅ Admin router started on port {}", server.result().actualPort());
                            startFuture.complete(); // Vert.x 3 API
                        } else {
                            startFuture.fail(server.cause()); // Vert.x 3 API
                        }
                    });

        } catch (Exception e) {
            logger.error("❌ Failed to start AdminRouterVerticle", e);
            startFuture.fail(e); // Vert.x 3 API
        }
    }

    private void handleReady(RoutingContext ctx) {
        // Vert.x 3 API for JSON response
        ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("ok", keycloakEnabled).encode());
    }

    // ------------------------------------------------------------------------------
    // Routes when Keycloak is enabled
    // ------------------------------------------------------------------------------
    private void registerKeycloakRoutes(Router router) {
        router.get("/_leoai/sso/login").handler(this::handleLogin);
        router.get("/_leoai/sso/error").handler(this::handleError);
        router.get("/_leoai/sso/callback").handler(this::handleCallback);
        router.get("/_leoai/sso/me").handler(this::handleGetMe);
        router.get("/_leoai/sso/logout").handler(this::handleLogout);
        router.get("/admin").handler(this::handleAdmin);
    }

    private void handleLogin(RoutingContext ctx) {
        String redirectUrl = keycloakUrl + "/realms/" + keycloakRealm +
                "/protocol/openid-connect/auth?client_id=" + keycloakClientId +
                "&response_type=code&scope=openid&redirect_uri=" + keycloakCallbackUrl;
        ctx.response()
                .putHeader("Location", redirectUrl)
                .setStatusCode(302)
                .end();
    }

    private void handleError(RoutingContext ctx) {
        String error = ctx.queryParams().get("error");
        String description = ctx.queryParams().get("description");
        JsonObject payload = new JsonObject()
                .put("error", error)
                .put("message", "SSO Error: " + error)
                .put("description", description)
                .put("timestamp", System.currentTimeMillis() / 1000);
        ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(payload.encode());
    }

    private void handleCallback(RoutingContext ctx) {
        String code = ctx.queryParams().get("code");
        String sessionState = ctx.queryParams().get("session_state");
        String error = ctx.queryParams().get("error");
        String logout = ctx.queryParams().get("logout");

        if ("true".equals(logout)) {
            ctx.response()
                    .putHeader("Location", "/admin?_t=" + System.currentTimeMillis())
                    .setStatusCode(303)
                    .end();
            return;
        }

        if (error != null) {
            ctx.response()
                    .putHeader("Location", "/_leoai/sso/error?error=" + error)
                    .setStatusCode(303)
                    .end();
            return;
        }

        if (code == null || sessionState == null) {
            ctx.response()
                    .putHeader("Location", "/_leoai/sso/error?error=invalid_grant")
                    .setStatusCode(303)
                    .end();
            return;
        }

        // Exchange authorization code for token
        String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";

        // OAuth token endpoints require application/x-www-form-urlencoded
        MultiMap formBody = MultiMap.caseInsensitiveMultiMap();
        formBody.add("grant_type", "authorization_code");
        formBody.add("code", code);
        formBody.add("client_id", keycloakClientId);
        formBody.add("client_secret", keycloakClientSecret);
        formBody.add("redirect_uri", keycloakCallbackUrl);

        // Vert.x 3 WebClient API (callback-based)
        webClient.postAbs(tokenUrl)
                .sendForm(formBody, ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> resp = ar.result();
                        if (resp.statusCode() == 200) {
                            JsonObject tokenData = resp.bodyAsJsonObject();
                            String accessToken = tokenData.getString("access_token");
                            fetchUserInfo(accessToken, ctx, tokenData);
                        } else {
                            logger.error("Token exchange failed: {}", resp.bodyAsString());
                            redirectError(ctx, "invalid_grant");
                        }
                    } else {
                        logger.error("Token exchange error", ar.cause());
                        redirectError(ctx, "server_error");
                    }
                });
    }

    private void fetchUserInfo(String accessToken, RoutingContext ctx, JsonObject tokenData) {
        String userInfoUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/userinfo";

        // Vert.x 3 WebClient API (callback-based)
        webClient.getAbs(userInfoUrl)
                .putHeader("Authorization", "Bearer " + accessToken)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> resp = ar.result();
                        if (resp.statusCode() == 200) {
                            JsonObject userInfo = resp.bodyAsJsonObject();
                            createRedisSession(ctx, userInfo, tokenData, sessionId -> {
                                ctx.response()
                                        .putHeader("Location", "/admin?sid=" + sessionId)
                                        .setStatusCode(303)
                                        .end();
                            });
                        } else {
                            redirectError(ctx, "userinfo_fetch_failed");
                        }
                    } else {
                        logger.error("Failed to fetch user info", ar.cause());
                        redirectError(ctx, "server_error");
                    }
                });
    }

    void createRedisSession(RoutingContext ctx, JsonObject userInfo, JsonObject tokenData, Handler<String> successCallback) {
        String sessionId = "sid:" + UUID.randomUUID();
        JsonObject data = new JsonObject()
                .put("user", userInfo)
                .put("token", tokenData)
                .put("timestamp", System.currentTimeMillis() / 1000);

        // Vert.x 3 executeBlocking API
        vertx.executeBlocking(future -> {
            RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
                @Override
                protected Boolean build(Jedis jedis) throws JedisException {
                    String ok = jedis.setex(sessionId, 3600L, data.toString()); // 1 hour expiry
                    return StringUtil.isNotEmpty(ok);
                }
            };
            boolean rs = cmd.execute();
            if (rs) {
                future.complete(sessionId); // Vert.x 3 API
            } else {
                future.fail("Failed to set session in Redis"); // Vert.x 3 API
            }
        }, (AsyncResult<String> res) -> {
            // This runs back on the event loop
            if (res.succeeded()) {
                logger.info("✅ createRedisSession OK: sessionId {}", res.result());
                successCallback.handle(res.result());
            } else {
                logger.error("❌ createRedisSession Failed", res.cause());
                redirectError(ctx, "session_create_failed");
            }
        });
    }

    /**
     * Fulfills user request to only return JSON.
     * No template engine is used.
     */
    private void handleAdmin(RoutingContext ctx) {
        String sessionId = ctx.queryParams().get("sid");
        if (sessionId == null) {
            redirectLogin(ctx);
            return;
        }

        // Vert.x 3 executeBlocking API
        vertx.executeBlocking(future -> {
            RedisCommand<String> cmd = new RedisCommand<String>(jedisPool) {
                @Override
                protected String build(Jedis jedis) throws JedisException {
                    return jedis.get(sessionId);
                }
            };
            String rs = cmd.execute();
            future.complete(rs); // complete with null if not found
        }, (AsyncResult<String> res) -> {
            if (res.failed()) {
                logger.error("handleAdmin: Failed to fetch session", res.cause());
                redirectError(ctx, "session_read_error");
                return;
            }

            String rs = res.result();

            if (StringUtil.isNotEmpty(rs)) {
                JsonObject session = new JsonObject(rs);
                JsonObject user = session.getJsonObject("user");
                JsonObject context = new JsonObject()
                        .put("HOSTNAME", System.getenv().getOrDefault("HOSTNAME", "localhost"))
                        .put("LEOBOT_DEV_MODE", System.getenv().getOrDefault("LEOBOT_DEV_MODE", "false"))
                        .put("user", user)
                        .put("session_id", sessionId)
                        .put("timestamp", System.currentTimeMillis() / 1000);

                // FULFILL USER REQUEST: Just return JSON
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(context.encodePrettily());

            } else {
                // Session ID provided but not found in Redis (or empty)
                redirectLogin(ctx);
            }
        });
    }

    private void handleGetMe(RoutingContext ctx) {
        String sid = ctx.queryParams().get("sid");
        if (sid == null) {
            ctx.response().setStatusCode(401).end("Missing session ID");
            return;
        }

        // Vert.x 3 executeBlocking API
        vertx.executeBlocking(future -> {
            RedisCommand<String> cmd = new RedisCommand<String>(jedisPool) {
                @Override
                protected String build(Jedis jedis) throws JedisException {
                    return jedis.get(sid);
                }
            };
            future.complete(cmd.execute());
        }, (AsyncResult<String> res) -> {
            if (res.failed()) {
                logger.error("handleGetMe: Failed to fetch session", res.cause());
                ctx.response().setStatusCode(500).end("Server error");
                return;
            }

            String sessionStr = res.result();

            if (StringUtil.isNotEmpty(sessionStr)) {
                JsonObject session = new JsonObject(sessionStr);
                // Vert.x 3 API for JSON response
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonObject()
                                .put("user", session.getJsonObject("user"))
                                .put("session_id", sid)
                                .encode());
            } else {
                ctx.response().setStatusCode(401).end("Invalid or expired session");
            }
        });
    }

    private void handleLogout(RoutingContext ctx) {
        String sid = ctx.queryParams().get("sid");
        String logoutUrl = keycloakUrl + "/realms/" + keycloakRealm
                + "/protocol/openid-connect/logout?client_id=" + keycloakClientId
                + "&post_logout_redirect_uri=" + keycloakCallbackUrl + "?logout=true";

        if (sid != null) {
            // Vert.x 3 executeBlocking API
            String finalSid = sid;
            vertx.executeBlocking(future -> {
                RedisCommand<Long> cmd = new RedisCommand<Long>(jedisPool) {
                    @Override
                    protected Long build(Jedis jedis) throws JedisException {
                        return jedis.del(finalSid);
                    }
                };
                future.complete(cmd.execute());
            }, res -> {
                if (res.succeeded()) {
                    logger.info("🗑️ Deleted session {}. Result: {}", finalSid, res.result());
                } else {
                    logger.warn("Failed to delete session {}", finalSid, res.cause());
                }
            });
        }

        ctx.response()
                .putHeader("Location", logoutUrl)
                .setStatusCode(303)
                .end();
    }

    void redirectLogin(RoutingContext ctx) {
        ctx.response()
                .putHeader("Location", "/_leoai/sso/login")
                .setStatusCode(303)
                .end();
    }

    void redirectError(RoutingContext ctx, String code) {
        ctx.response()
                .putHeader("Location", "/_leoai/sso/error?error=" + code)
                .setStatusCode(303)
                .end();
    }

    private void registerFallbackRoutes(Router router) {
        router.get("/admin").handler(ctx ->
                ctx.response().setStatusCode(503)
                        .end("Keycloak not enabled or failed to initialize."));
        router.get("/_leoai/sso/login").handler(ctx ->
                ctx.response().setStatusCode(500)
                        .end("{\"error\": \"Keycloak not configured\"}"));
        router.get("/_leoai/sso/logout").handler(ctx ->
                ctx.response().putHeader("Location", "/")
                        .setStatusCode(303)
                        .end());
    }

    private boolean getEnvBool(String key, boolean def) {
        String val = System.getenv(key);
        if (val == null) return def;
        return val.equalsIgnoreCase("true") || val.equals("1") || val.equalsIgnoreCase("yes");
    }
}