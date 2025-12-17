package leotech.system.util.keycloak;

import static leotech.system.util.keycloak.KeycloakConstants.GRANT_AUTH_CODE;
import static leotech.system.util.keycloak.KeycloakConstants.GRANT_REFRESH;
import static leotech.system.util.keycloak.KeycloakConstants.HEADER_AUTH;
import static leotech.system.util.keycloak.KeycloakConstants.PARAM_CLIENT_ID;
import static leotech.system.util.keycloak.KeycloakConstants.PARAM_CLIENT_SECRET;
import static leotech.system.util.keycloak.KeycloakConstants.PARAM_CODE;
import static leotech.system.util.keycloak.KeycloakConstants.PARAM_GRANT_TYPE;
import static leotech.system.util.keycloak.KeycloakConstants.PARAM_REDIRECT_URI;
import static leotech.system.util.keycloak.KeycloakConstants.PARAM_REFRESH_TOKEN;
import static leotech.system.util.keycloak.KeycloakUtils.encodeUrl;
import static leotech.system.util.keycloak.KeycloakUtils.getUserRoles;
import static leotech.system.util.keycloak.KeycloakUtils.hasRole;
import static leotech.system.util.keycloak.KeycloakUtils.kcEndpoint;
import static leotech.system.util.keycloak.KeycloakUtils.makeSsoSessionCookie;
import static leotech.system.util.keycloak.KeycloakUtils.redirect;
import static leotech.system.util.keycloak.KeycloakUtils.sendJson;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import leotech.system.util.keycloak.KeycloakClientSsoRouter.SsoRoutePaths;
import rfx.core.util.StringUtil;

/**
 * Robust Keycloak Handler with Timeout and Error Management
 * * @author Trieu Nguyen
 * @since 2025
 */
public class AuthKeycloakHandlers {

    // --- Constants ---
    public static final String URI_OPENID_CONNECT_TOKEN = "/protocol/openid-connect/token";
    public static final String URI_OPENID_CONNECT_USERINFO = "/protocol/openid-connect/userinfo";

    public static final String ROLE = "role";
    public static final String LOGOUT = "logout";
    public static final String ERROR = "error";
    public static final String PARAM_SESSION_ID = "sid";
    public static final String USER = "user";
    public static final String TOKEN = "token";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String LEO_CDP_SSO = "LEO CDP SSO";

    // 10 Seconds Timeout for Keycloak calls
    private static final long KC_REQUEST_TIMEOUT_MS = 10000L;

    private static final Logger logger = LoggerFactory.getLogger(AuthKeycloakHandlers.class);

    private final SessionRepository sessionRepo;
    private final WebClient webClient;

    public AuthKeycloakHandlers(SessionRepository sessionRepo, WebClient wc) {
        this.sessionRepo = sessionRepo;
        this.webClient = wc;
    }

    // -----------------------------------------------------------------------------
    // Public Handlers
    // -----------------------------------------------------------------------------

    public void handleInfo(RoutingContext ctx) {
        ctx.response().end(LEO_CDP_SSO);
    }

    public void handleLogin(RoutingContext ctx) {
        KeycloakConfig config = KeycloakConfig.getInstance();
        if (!config.isReady()) {
            noValidSsoConfig(ctx);
            return;
        }

        try {
            String redirectEncoded = encodeUrl(config.getCallbackUrl());
            // State should ideally be cryptographically secure and stored in a cookie to prevent CSRF, 
            // but UUID is acceptable for simple implementations.
            String state = UUID.randomUUID().toString(); 

            String authUrl = String.format(
                    "%s/realms/%s/protocol/openid-connect/auth?client_id=%s&response_type=code&scope=openid%%20email%%20profile&state=%s&redirect_uri=%s",
                    config.getUrl(), config.getRealm(), config.getClientId(), state, redirectEncoded);

            logger.info("Redirecting to Keycloak: {}", authUrl);
            redirect(ctx, authUrl);

        } catch (Exception e) {
            logger.error("Login redirect construction failed", e);
            redirect(ctx, SsoRoutePaths.ERROR + "?error=login_build_error");
        }
    }

    public void handleCallback(RoutingContext ctx) {
    	logger.info(">>> ENTER handleCallback <<<");

        String code = getQueryParam(ctx, PARAM_CODE);
        String error = getQueryParam(ctx, ERROR);
        String logoutFlag = getQueryParam(ctx, LOGOUT);

        if ("true".equalsIgnoreCase(logoutFlag)) {
            redirect(ctx, SsoRoutePaths.SESSION + "?_t=" + System.currentTimeMillis());
            return;
        }

        if (error != null) {
            logger.warn("Callback received error from Keycloak: {}", error);
            redirect(ctx, SsoRoutePaths.ERROR + "?error=" + error);
            return;
        }

        if (StringUtil.isEmpty(code)) {
            redirect(ctx, SsoRoutePaths.ERROR + "?error=missing_code");
            return;
        }

        exchangeCode(ctx, code);
    }

    public void handleSession(RoutingContext ctx) {
        String sid = getQueryParam(ctx, PARAM_SESSION_ID);
        if (StringUtil.isEmpty(sid)) {
            redirect(ctx, SsoRoutePaths.LOGIN);
            return;
        }

        KeycloakConfig config = KeycloakConfig.getInstance();
        if (!config.isReady()) {
            noValidSsoConfig(ctx);
            return;
        }

        sessionRepo.getSession(sid, ar -> {
            if (ar.failed() || StringUtil.isEmpty(ar.result())) {
                logger.warn("Session not found or invalid: {}", sid);
                redirect(ctx, SsoRoutePaths.LOGIN);
                return;
            }

            try {
                JsonObject session = new JsonObject(ar.result());
                JsonObject tokenJson = session.getJsonObject(TOKEN);
                
                if (tokenJson == null || !tokenJson.containsKey(ACCESS_TOKEN)) {
                    logger.warn("Session found but corrupt (missing tokens). Sid: {}", sid);
                    redirect(ctx, SsoRoutePaths.LOGIN);
                    return;
                }
                
                String accessToken = tokenJson.getString(ACCESS_TOKEN);
                JsonArray roles = getUserRoles(accessToken);

                // convert JSON to SSO User
                SsoUserProfile user = SsoUserProfile.fromJson(session.getJsonObject(USER), roles);

                logger.info("Active User Session: {}", user.getEmail());

                ctx.addCookie(makeSsoSessionCookie(sid));
                redirect(ctx, SsoRoutePaths.ROOT);
                
            } catch (Exception e) {
                logger.error("Error parsing session data", e);
                redirect(ctx, SsoRoutePaths.ERROR + "?error=session_corrupt");
            }
        });
    }

    public void handleRefreshToken(RoutingContext ctx) {
        String sid = getQueryParam(ctx, PARAM_SESSION_ID);
        if (sid == null) {
            ctx.response().setStatusCode(401).end("Missing sid");
            return;
        }

        sessionRepo.getSession(sid, ar -> {
            if (ar.failed() || ar.result() == null) {
                ctx.response().setStatusCode(401).end("Invalid sid");
                return;
            }

            JsonObject session = new JsonObject(ar.result());
            JsonObject tokenObj = session.getJsonObject(TOKEN);
            String refresh = (tokenObj != null) ? tokenObj.getString(REFRESH_TOKEN) : null;

            if (StringUtil.isEmpty(refresh)) {
                ctx.response().setStatusCode(401).end("No refresh token available");
                return;
            }

            refreshToken(ctx, sid, session, refresh);
        });
    }

    public void handleLogout(RoutingContext ctx) {
        KeycloakConfig config = KeycloakConfig.getInstance();
        if (!config.isReady()) {
            ctx.response().putHeader(KeycloakConstants.HEADER_LOCATION, "/").setStatusCode(303).end();
            return;
        }
        
        String sid = getQueryParam(ctx, PARAM_SESSION_ID);
        if (sid != null) {
            sessionRepo.deleteSession(sid, r -> logger.debug("Session deleted: " + sid));
        }

        try {
            String redirectUri = encodeUrl(config.getCallbackUrl() + "?logout=true&t=" + System.currentTimeMillis());
            String logoutUrl = String.format(
                    "%s/realms/%s/protocol/openid-connect/logout?client_id=%s&post_logout_redirect_uri=%s",
                    config.getUrl(), config.getRealm(), config.getClientId(), redirectUri);

            redirect(ctx, logoutUrl);
        } catch (Exception e) {
            logger.error("Logout URL generation failed", e);
            ctx.response().setStatusCode(500).end("logout_failed");
        }
    }

    public void handleCheckRole(RoutingContext ctx) {
        String sid = getQueryParam(ctx, PARAM_SESSION_ID);
        String role = getQueryParam(ctx, ROLE);
        
        if (sid == null) {
            ctx.response().setStatusCode(401).end("Missing sid");
            return;
        }

        sessionRepo.getSession(sid, ar -> {
            if (ar.failed() || ar.result() == null) {
                ctx.response().setStatusCode(401).end("Invalid sid");
                return;
            }

            JsonObject session = new JsonObject(ar.result());
            JsonObject tokenJson = session.getJsonObject(TOKEN);
            
            if (tokenJson == null) {
                ctx.response().setStatusCode(401).end("Session expired");
                return;
            }
            
            String accessToken = tokenJson.getString(ACCESS_TOKEN);

            if (!hasRole(accessToken, role)) {
                ctx.response().setStatusCode(403).end("Forbidden");
                return;
            }

            JsonObject data = new JsonObject().put("message", "Welcome authorized user");
            sendJson(ctx, data);
        });
    }

    // -----------------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------------

    private void exchangeCode(RoutingContext ctx, String code) {
        KeycloakConfig config = KeycloakConfig.getInstance();
        if (!config.isReady()) {
            noValidSsoConfig(ctx);
            return;
        }

        String tokenUrl = kcEndpoint(config, URI_OPENID_CONNECT_TOKEN);
        
        // --- STEP 1: Set a hard safety timer ---
        long timerId = ctx.vertx().setTimer(KC_REQUEST_TIMEOUT_MS, id -> {
            if (!ctx.response().ended()) {
                logger.error("Hard Timeout reached for ExchangeCode. Keycloak is unreachable.");
                redirect(ctx, SsoRoutePaths.ERROR + "?error=timeout&detail=connection_to_iam_failed");
            }
        });

        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add(PARAM_GRANT_TYPE, GRANT_AUTH_CODE)
                .add(PARAM_CODE, code)
                .add(PARAM_REDIRECT_URI, config.getCallbackUrl())
                .add(PARAM_CLIENT_ID, config.getClientId());

        if (config.getClientSecret() != null) {
            form.add(PARAM_CLIENT_SECRET, config.getClientSecret());
        }

        logger.info("Attempting code exchange: {}", tokenUrl);

        webClient.postAbs(tokenUrl)
                .timeout(KC_REQUEST_TIMEOUT_MS) 
                .sendForm(form, ar -> {
                    // --- STEP 2: Cancel the safety timer as we got a response ---
                    ctx.vertx().cancelTimer(timerId);

                    if (ctx.response().ended()) return; // Already handled by timer

                    if (ar.failed()) {
                        // This will catch "Connection Refused" or "Connect Timeout"
                        handleWebClientFailure(ctx, ar.cause(), "Token Exchange");
                        return;
                    }

                    HttpResponse<Buffer> response = ar.result();
                    if (response.statusCode() != 200) {
                        handleKeycloakError(ctx, response, "Token Exchange");
                        return;
                    }

                    // ... proceed to fetchUserInfo
                    fetchUserInfo(ctx, response.bodyAsJsonObject().getString(ACCESS_TOKEN), response.bodyAsJsonObject());
                });
    }

    private void fetchUserInfo(RoutingContext ctx, String accessToken, JsonObject tokenJson) {
        KeycloakConfig config = KeycloakConfig.getInstance();
        if (!config.isReady()) {
            noValidSsoConfig(ctx);
            return;
        }

        String userinfoUrl = kcEndpoint(config, URI_OPENID_CONNECT_USERINFO);

        webClient.getAbs(userinfoUrl)
                .putHeader(HEADER_AUTH, "Bearer " + accessToken)
                .timeout(KC_REQUEST_TIMEOUT_MS) // <--- CRITICAL: Handle Timeout
                .send(ar -> {
                    if (ar.failed()) {
                        handleWebClientFailure(ctx, ar.cause(), "UserInfo Fetch");
                        return;
                    }

                    HttpResponse<Buffer> response = ar.result();
                    if (response.statusCode() != 200) {
                        // 401 usually means token expired immediately or invalid
                        // 403 means missing Client Scopes in Keycloak (e.g., 'profile' scope)
                        handleKeycloakError(ctx, response, "UserInfo Fetch");
                        return;
                    }

                    try {
                        JsonObject userInfo = response.bodyAsJsonObject();
                        
                        // Check for missing data permissions
                        if (userInfo == null || userInfo.isEmpty() || !userInfo.containsKey("sub")) {
                            logger.error("UserInfo is empty or invalid. Check Keycloak Client Scopes (profile/email). Payload: {}", userInfo);
                            redirect(ctx, SsoRoutePaths.ERROR + "?error=userinfo_missing_claims");
                            return;
                        }

                        sessionRepo.createSession(userInfo, tokenJson, res -> {
                            if (!res.succeeded()) {
                                logger.error("Session creation failed", res.cause());
                                redirect(ctx, SsoRoutePaths.ERROR + "?error=session_create_failed");
                                return;
                            }

                            String sid = res.result();
                            redirect(ctx, SsoRoutePaths.SESSION + "?sid=" + sid);
                        });

                    } catch (Exception e) {
                        logger.error("Failed to parse UserInfo JSON", e);
                        redirect(ctx, SsoRoutePaths.ERROR + "?error=userinfo_parse_error");
                    }
                });
    }

    private void refreshToken(RoutingContext ctx, String sid, JsonObject session, String refreshToken) {
        KeycloakConfig config = KeycloakConfig.getInstance();
        if (!config.isReady()) {
            noValidSsoConfig(ctx);
            return;
        }

        String tokenUrl = kcEndpoint(config, URI_OPENID_CONNECT_TOKEN);

        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add(PARAM_GRANT_TYPE, GRANT_REFRESH)
                .add(PARAM_CLIENT_ID, config.getClientId())
                .add(PARAM_REFRESH_TOKEN, refreshToken);

        if (config.getClientSecret() != null && !config.getClientSecret().isEmpty())
            form.add(PARAM_CLIENT_SECRET, config.getClientSecret());

        webClient.postAbs(tokenUrl)
                .timeout(KC_REQUEST_TIMEOUT_MS)
                .sendForm(form, ar -> {
                    if (ar.failed()) {
                        // For refresh token, we return 500/401 JSON, not redirect
                        logger.error("Refresh Token Network Fail", ar.cause());
                        ctx.response().setStatusCode(502).end(new JsonObject().put("error", "refresh_network_error").encode());
                        return;
                    }
                    
                    if (ar.result().statusCode() != 200) {
                        logger.error("Refresh Token Keycloak Fail: {}", ar.result().bodyAsString());
                        ctx.response().setStatusCode(401).end(new JsonObject().put("error", "refresh_denied").encode());
                        return;
                    }

                    JsonObject newTokens = ar.result().bodyAsJsonObject();
                    session.put(TOKEN, newTokens);

                    sessionRepo.updateSession(sid, session, r -> sendJson(ctx, newTokens));
                });
    }

    // -----------------------------------------------------------------------------
    // Helpers for Robust Error Handling
    // -----------------------------------------------------------------------------

    /**
     * Handles Network/Timeout errors from WebClient
     */
    private void handleWebClientFailure(RoutingContext ctx, Throwable cause, String actionName) {
        String errorMsg = "network_error";
        
        // Log the detailed exception to find out if it's DNS, Connection Refused, or Timeout
        logger.error("Network Failure during {}: {}", actionName, cause.getMessage());

        if (cause instanceof java.net.ConnectException || cause.getMessage().contains("Connection refused")) {
            errorMsg = "connection_refused"; // Server is down or firewall blocked
        } else if (cause instanceof java.util.concurrent.TimeoutException || cause.getMessage().contains("timeout")) {
            errorMsg = "timeout";
        } else if (cause instanceof java.net.UnknownHostException) {
            errorMsg = "dns_error"; // Cannot find iam.ttcgroup.vn
        }

        redirect(ctx, SsoRoutePaths.ERROR + "?error=" + errorMsg + "&action=" + actionName.replace(" ", "_"));
    }

    /**
     * Handles API errors (HTTP 4xx/5xx) from Keycloak
     */
    private void handleKeycloakError(RoutingContext ctx, HttpResponse<Buffer> response, String actionName) {
        int status = response.statusCode();
        String body = response.bodyAsString();
        String errorCode = "api_error";
        
        // Try to parse Keycloak standard error JSON
        try {
            JsonObject errJson = new JsonObject(body);
            String providerError = errJson.getString("error");
            String providerDesc = errJson.getString("error_description");
            logger.error("{} failed [{}]: {} - {}", actionName, status, providerError, providerDesc);
            
            if("invalid_grant".equals(providerError)) errorCode = "invalid_grant"; // Code expired
            else if(status == 403) errorCode = "forbidden"; // Scope issue
            
        } catch (Exception e) {
            // Fallback if not JSON
            logger.error("{} failed [{}]: {}", actionName, status, body);
        }

        redirect(ctx, SsoRoutePaths.ERROR + "?error=" + errorCode + "&status=" + status);
    }

    private void noValidSsoConfig(RoutingContext ctx) {
        logger.error("Keycloak configuration is invalid or missing");
        ctx.response()
            .setStatusCode(500)
            .putHeader(KeycloakConstants.HEADER_CONTENT_TYPE, KeycloakConstants.MIME_JSON)
            .end(new JsonObject().put("error", "Keycloak not configured").encode());
    }

    private String getQueryParam(RoutingContext ctx, String name) {
        try {
            List<String> vals = ctx.queryParam(name);
            if (vals == null || vals.isEmpty()) return null;
            String v = vals.get(0);
            return (v == null || v.isBlank()) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }
}