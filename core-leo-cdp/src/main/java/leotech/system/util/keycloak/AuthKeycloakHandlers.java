package leotech.system.util.keycloak;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import leotech.system.util.keycloak.KeycloakRouterVerticle.SsoRoutePaths; // Importing the constants
import rfx.core.util.StringUtil;

/**
 * AuthKeycloakHandlers <br>
 *
 * Controller layer for Keycloak SSO integration. <br>
 * - Reads HTTP params from RoutingContext <br>
 * - Delegates persistence to SessionRepository <br>
 * - Delegates token/userinfo retrieval to Keycloak endpoints <br>
 * - Writes JSON or redirects back to the browser <br>
 *
 * Designed to keep logic readable and maintainable. <br>
 * 
 * @author Trieu Nguyen
 * @since 2025
 */
public class AuthKeycloakHandlers {

    private static final Logger logger = LoggerFactory.getLogger(AuthKeycloakHandlers.class);

    // HTTP Header Constants
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_AUTH = "Authorization";
    public static final String MIME_JSON = "application/json";

    // OIDC/Keycloak Parameter Constants
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_CLIENT_SECRET = "client_secret";
    private static final String PARAM_REDIRECT_URI = "redirect_uri";
    private static final String PARAM_RESPONSE_TYPE = "response_type";
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_STATE = "state";
    private static final String PARAM_GRANT_TYPE = "grant_type";
    private static final String PARAM_CODE = "code";
    private static final String PARAM_REFRESH_TOKEN = "refresh_token";
    
    private static final String GRANT_AUTH_CODE = "authorization_code";
    private static final String GRANT_REFRESH = "refresh_token";

    private final KeycloakConfig config;
    private final SessionRepository sessionRepo;
    private final WebClient webClient;

    public AuthKeycloakHandlers(KeycloakConfig config, SessionRepository sessionRepo, WebClient webClient) {
        this.config = config;
        this.sessionRepo = sessionRepo;
        this.webClient = webClient;
    }

    // =================================================================================
    // Public Route Handlers
    // =================================================================================

    /**
     * Handler for simple info/health check
     */
    public void handleInfo(RoutingContext ctx) {
        ctx.response().end("LEO CDP SSO System");
    }

    /**
     * Admin handler: Validates session and returns User Profile.
     */
    public void handleAdmin(RoutingContext ctx) {
        String sessionId = ctx.queryParams().get("sid");
        if (StringUtil.isEmpty(sessionId)) {
            redirectToLogin(ctx);
            return;
        }

        sessionRepo.getSession(sessionId, res -> {
            if (res.failed()) {
                logger.error("handleAdmin: Session read error", res.cause());
                redirectToError(ctx, "session_read_error");
                return;
            }

            String rawSession = res.result();
            if (StringUtil.isEmpty(rawSession)) {
                redirectToLogin(ctx);
                return;
            }

            // Transform and response
            JsonObject userJson = new JsonObject(rawSession).getJsonObject("user");
            UserProfile user = UserProfile.fromJson(userJson);

            JsonObject responseJson = new JsonObject()
                    .put("user", JsonObject.mapFrom(user))
                    .put("session_id", sessionId)
                    .put("timestamp", System.currentTimeMillis() / 1000);

            sendJsonResponse(ctx, responseJson);
        });
    }

    /**
     * Initiates the OAuth2 Authorization Code flow.
     */
    public void handleLogin(RoutingContext ctx) {
        try {
            String encodedRedirect = URLEncoder.encode(config.callbackUrl, StandardCharsets.UTF_8.name());
            String state = UUID.randomUUID().toString(); // Anti-CSRF token

            // Build Keycloak Auth URL
            String authUrl = String.format("%s/realms/%s/protocol/openid-connect/auth?%s=%s&%s=code&%s=openid&%s=%s&%s=%s",
                    config.url, config.realm,
                    PARAM_CLIENT_ID, config.clientId,
                    PARAM_RESPONSE_TYPE,
                    PARAM_SCOPE,
                    PARAM_STATE, state,
                    PARAM_REDIRECT_URI, encodedRedirect
            );

            ctx.response().putHeader(HEADER_LOCATION, authUrl).setStatusCode(302).end();

        } catch (Exception e) {
            logger.error("handleLogin: Failed to build redirect URL", e);
            redirectToError(ctx, "login_build_error");
        }
    }

    /**
     * Handles the callback from Keycloak (Code Exchange).
     */
    public void handleCallback(RoutingContext ctx) {
        String code = ctx.request().getParam(PARAM_CODE);
        String error = ctx.request().getParam("error");
        String logout = ctx.request().getParam("logout");

        // 1. Handle Logout Redirect
        if ("true".equalsIgnoreCase(logout)) {
            // Redirect back to Admin (or Root) after logout
            String destination = SsoRoutePaths.ADMIN + "?_t=" + System.currentTimeMillis();
            ctx.response().putHeader(HEADER_LOCATION, destination).setStatusCode(303).end();
            return;
        }

        // 2. Handle Errors from Identity Provider
        if (error != null) {
            redirectToError(ctx, error);
            return;
        }

        if (code == null) {
            redirectToError(ctx, "missing_code");
            return;
        }

        // 3. Exchange Code for Token
        exchangeCodeForToken(ctx, code);
    }

    /**
     * Refreshes the access token using the refresh token stored in the session.
     */
    public void handleRefreshToken(RoutingContext ctx) {
        String sid = ctx.queryParams().get("sid");
        if (sid == null) {
            ctx.response().setStatusCode(401).end("Missing session id");
            return;
        }

        sessionRepo.getSession(sid, res -> {
            if (res.failed() || res.result() == null) {
                ctx.response().setStatusCode(401).end("Invalid session");
                return;
            }

            JsonObject session = new JsonObject(res.result());
            String refreshToken = session.getJsonObject("token").getString("refresh_token");

            performTokenRefresh(ctx, sid, session, refreshToken);
        });
    }

    /**
     * Logs out the user: deletes local session and redirects to Keycloak logout.
     */
    public void handleLogout(RoutingContext ctx) {
        String sid = ctx.queryParams().get("sid");

        // Fire and forget session deletion
        if (sid != null) {
            sessionRepo.deleteSession(sid, res -> {});
        }

        // Construct Logout URL
        try {
            String redirectParam = config.callbackUrl + "?logout=true";
            String encodedRedirect = URLEncoder.encode(redirectParam, StandardCharsets.UTF_8.name());

            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout?%s=%s&post_logout_redirect_uri=%s",
                    config.url, config.realm,
                    PARAM_CLIENT_ID, config.clientId,
                    encodedRedirect);

            ctx.response().putHeader(HEADER_LOCATION, logoutUrl).setStatusCode(303).end();
        } catch (Exception e) {
            logger.error("Logout URL construction failed", e);
            ctx.response().setStatusCode(500).end();
        }
    }

    /**
     * Protected API example with RBAC (Role Based Access Control).
     */
    public void handleDataApi(RoutingContext ctx) {
        String sid = ctx.queryParams().get("sid");
        if (sid == null) {
            ctx.response().setStatusCode(401).end("Missing sid");
            return;
        }

        sessionRepo.getSession(sid, res -> {
            if (res.failed() || res.result() == null) {
                ctx.response().setStatusCode(401).end("Invalid session");
                return;
            }

            JsonObject session = new JsonObject(res.result());
            String accessToken = session.getJsonObject("token").getString("access_token");

            if (isUserAuthorized(accessToken, "DATA_OPERATOR")) {
                JsonObject data = new JsonObject()
                        .put("message", "Welcome authorized user")
                        .put("records", new JsonObject().put("id", 123).put("value", "example record"));
                
                sendJsonResponse(ctx, data);
            } else {
                ctx.response().setStatusCode(403).end("Forbidden");
            }
        });
    }

    // =================================================================================
    // Private Helper Logic
    // =================================================================================

    private void exchangeCodeForToken(RoutingContext ctx, String code) {
        String tokenUrl = getKeycloakEndpoint("/protocol/openid-connect/token");

        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add(PARAM_GRANT_TYPE, GRANT_AUTH_CODE)
                .add(PARAM_CODE, code)
                .add(PARAM_CLIENT_ID, config.clientId)
                .add(PARAM_REDIRECT_URI, config.callbackUrl);

        if (config.clientSecret != null) {
            form.add(PARAM_CLIENT_SECRET, config.clientSecret);
        }

        webClient.postAbs(tokenUrl).sendForm(form, ar -> {
            if (ar.failed() || ar.result().statusCode() != 200) {
                logger.warn("Token exchange failed. Status: {}", ar.succeeded() ? ar.result().statusCode() : "Client Error");
                redirectToError(ctx, "token_exchange_failed");
                return;
            }

            JsonObject tokenJson = ar.result().bodyAsJsonObject();
            String accessToken = tokenJson.getString("access_token");

            fetchUserInfoAndCreateSession(ctx, accessToken, tokenJson);
        });
    }

    private void fetchUserInfoAndCreateSession(RoutingContext ctx, String accessToken, JsonObject tokenData) {
        String userInfoUrl = getKeycloakEndpoint("/protocol/openid-connect/userinfo");

        webClient.getAbs(userInfoUrl)
                .putHeader(HEADER_AUTH, "Bearer " + accessToken)
                .send(ar -> {
                    if (ar.failed() || ar.result().statusCode() != 200) {
                        redirectToError(ctx, "userinfo_fetch_failed");
                        return;
                    }

                    JsonObject userInfo = ar.result().bodyAsJsonObject();

                    sessionRepo.createSession(userInfo, tokenData, res -> {
                        if (res.succeeded()) {
                            String sid = res.result();
                            // Redirect to Admin Page using Constant
                            String target = SsoRoutePaths.ADMIN + "?sid=" + sid;
                            ctx.response().putHeader(HEADER_LOCATION, target).setStatusCode(303).end();
                        } else {
                            redirectToError(ctx, "session_create_failed");
                        }
                    });
                });
    }

    private void performTokenRefresh(RoutingContext ctx, String sid, JsonObject currentSession, String refreshToken) {
        String tokenUrl = getKeycloakEndpoint("/protocol/openid-connect/token");

        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add(PARAM_GRANT_TYPE, GRANT_REFRESH)
                .add(PARAM_CLIENT_ID, config.clientId)
                .add(PARAM_REFRESH_TOKEN, refreshToken);

        if (config.clientSecret != null) {
            form.add(PARAM_CLIENT_SECRET, config.clientSecret);
        }

        webClient.postAbs(tokenUrl).sendForm(form, tokenResp -> {
            if (tokenResp.failed()) {
                ctx.response().setStatusCode(500).end("Token refresh failed");
                return;
            }

            JsonObject newTokens = tokenResp.result().bodyAsJsonObject();
            currentSession.put("token", newTokens);

            sessionRepo.updateSession(sid, currentSession, updateRes -> {
                sendJsonResponse(ctx, newTokens);
            });
        });
    }

    private boolean isUserAuthorized(String accessToken, String requiredRole) {
        JsonObject payload = decodeJwtWithoutVerify(accessToken);
        try {
            JsonArray roles = payload.getJsonObject("realm_access").getJsonArray("roles");
            logger.debug("Checking roles: {}", roles);
            return roles != null && roles.contains(requiredRole);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Naively decodes a JWT to inspect payload. 
     * Note: This does NOT verify the signature (used only for fast role hints).
     */
    private JsonObject decodeJwtWithoutVerify(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return new JsonObject();
            String payloadJson = new String(java.util.Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return new JsonObject(payloadJson);
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    // =================================================================================
    // Utilities
    // =================================================================================

    private String getKeycloakEndpoint(String suffix) {
        return config.url + "/realms/" + config.realm + suffix;
    }

    private void sendJsonResponse(RoutingContext ctx, JsonObject json) {
        ctx.response()
           .putHeader(HEADER_CONTENT_TYPE, MIME_JSON)
           .end(json.encodePrettily());
    }

    private void redirectToLogin(RoutingContext ctx) {
        // Using SsoRoutePaths constant
        ctx.response().putHeader(HEADER_LOCATION, SsoRoutePaths.LOGIN).setStatusCode(303).end();
    }

    private void redirectToError(RoutingContext ctx, String errorCode) {
        // Using SsoRoutePaths constant
        String url = SsoRoutePaths.ERROR + "?error=" + errorCode;
        ctx.response().putHeader(HEADER_LOCATION, url).setStatusCode(303).end();
    }
}