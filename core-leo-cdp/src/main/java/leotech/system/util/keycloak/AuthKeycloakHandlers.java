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
import static leotech.system.util.keycloak.KeycloakUtils.makeSessionCookie;
import static leotech.system.util.keycloak.KeycloakUtils.redirect;
import static leotech.system.util.keycloak.KeycloakUtils.sendJson;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import leotech.system.util.keycloak.KeycloakClientRouter.SsoRoutePaths;
import rfx.core.util.StringUtil;

public class AuthKeycloakHandlers {

    private static final Logger log = LoggerFactory.getLogger(AuthKeycloakHandlers.class);

    private final KeycloakConfig config;
    private final SessionRepository sessionRepo;
    private final WebClient webClient;

    public AuthKeycloakHandlers(KeycloakConfig config, SessionRepository sessionRepo, WebClient wc) {
        this.config = config;
        this.sessionRepo = sessionRepo;
        this.webClient = wc;
    }

    // -----------------------------------------------------------------------------
    // NEW: Safe query param extractor
    // -----------------------------------------------------------------------------

    /**
     * Extracts param safely from RoutingContext.
     * Vert.x → queryParam(name) returns List<String>.
     * This returns:
     * - first value
     * - or null if missing
     */
    private String getQueryParam(RoutingContext ctx, String name) {
        try {
            List<String> vals = ctx.queryParam(name);
            if (vals == null || vals.isEmpty()) return null;
            String v = vals.get(0);
            if (v == null || v.isBlank()) return null;
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    // -----------------------------------------------------------------------------
    // Public Handlers
    // -----------------------------------------------------------------------------

    public void handleInfo(RoutingContext ctx) {
        ctx.response().end("LEO CDP SSO System");
    }

    public void handleSession(RoutingContext ctx) {
        String sid = getQueryParam(ctx, "sid");
        if (StringUtil.isEmpty(sid)) {
            redirect(ctx, SsoRoutePaths.LOGIN);
            return;
        }

        sessionRepo.getSession(sid, ar -> {
            if (ar.failed() || StringUtil.isEmpty(ar.result())) {
                redirect(ctx, SsoRoutePaths.LOGIN);
                return;
            }

            JsonObject session = new JsonObject(ar.result());
            String accessToken = session.getJsonObject("token").getString("access_token");
            JsonArray roles = getUserRoles(accessToken);

            UserProfile user = UserProfile.fromJson(session.getJsonObject("user"), roles);

            log.info("Admin user: {}", user);

            Cookie cookie = makeSessionCookie(sid);
            ctx.addCookie(cookie);

            redirect(ctx, SsoRoutePaths.ROOT);
        });
    }

    public void handleLogin(RoutingContext ctx) {
        try {
            String redirectEncoded = encodeUrl(config.callbackUrl);
            String state = UUID.randomUUID().toString();

            String authUrl = String.format(
                "%s/realms/%s/protocol/openid-connect/auth?client_id=%s&response_type=code&scope=openid&state=%s&redirect_uri=%s",
                config.url, config.realm, config.clientId, state, redirectEncoded
            );

            redirect(ctx, authUrl);

        } catch (Exception e) {
            log.error("Login redirect error", e);
            redirect(ctx, SsoRoutePaths.ERROR + "?error=login_build_error");
        }
    }

    public void handleCallback(RoutingContext ctx) {
        String code = getQueryParam(ctx, PARAM_CODE);
        String error = getQueryParam(ctx, "error");
        String logoutFlag = getQueryParam(ctx, "logout");

        if ("true".equalsIgnoreCase(logoutFlag)) {
            redirect(ctx, SsoRoutePaths.SESSION + "?_t=" + System.currentTimeMillis());
            return;
        }

        if (error != null) {
            redirect(ctx, SsoRoutePaths.ERROR + "?error=" + error);
            return;
        }

        if (code == null) {
            redirect(ctx, SsoRoutePaths.ERROR + "?error=missing_code");
            return;
        }

        exchangeCode(ctx, code);
    }

    public void handleRefreshToken(RoutingContext ctx) {
        String sid = getQueryParam(ctx, "sid");
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
            String refresh = session
                    .getJsonObject("token")
                    .getString("refresh_token");

            refreshToken(ctx, sid, session, refresh);
        });
    }

    public void handleLogout(RoutingContext ctx) {
        String sid = getQueryParam(ctx, "sid");

        if (sid != null)
            sessionRepo.deleteSession(sid, r -> {});

        try {
            String redirectUri = encodeUrl(config.callbackUrl + "?logout=true");

            String logoutUrl = String.format(
                "%s/realms/%s/protocol/openid-connect/logout?client_id=%s&post_logout_redirect_uri=%s",
                config.url, config.realm, config.clientId, redirectUri
            );

            redirect(ctx, logoutUrl);

        } catch (Exception e) {
            ctx.response().setStatusCode(500).end("logout_failed");
        }
    }

    public void handleCheckRole(RoutingContext ctx) {
        String sid = getQueryParam(ctx, "sid");
        String role = getQueryParam(ctx, "role");
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
            String accessToken = session.getJsonObject("token").getString("access_token");

            if (!hasRole(accessToken, role)) {
                ctx.response().setStatusCode(403).end("Forbidden");
                return;
            }

            JsonObject data = new JsonObject()
                .put("message", "Welcome authorized user")
                .put("records", new JsonObject().put("id", 123).put("value", "example"));

            sendJson(ctx, data);
        });
    }

    // -----------------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------------

    private void exchangeCode(RoutingContext ctx, String code) {

        String tokenUrl = kcEndpoint(config, "/protocol/openid-connect/token");

        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add(PARAM_GRANT_TYPE, GRANT_AUTH_CODE)
                .add(PARAM_CODE, code)
                .add(PARAM_REDIRECT_URI, config.callbackUrl)
                .add(PARAM_CLIENT_ID, config.clientId);

        if (config.clientSecret != null)
            form.add(PARAM_CLIENT_SECRET, config.clientSecret);

        webClient.postAbs(tokenUrl).sendForm(form, ar -> {
            if (ar.failed() || ar.result().statusCode() != 200) {
                redirect(ctx, SsoRoutePaths.ERROR + "?error=token_exchange_failed");
                return;
            }

            JsonObject tokenJson = ar.result().bodyAsJsonObject();
            String accessToken = tokenJson.getString("access_token");

            fetchUserInfo(ctx, accessToken, tokenJson);
        });
    }

    private void fetchUserInfo(RoutingContext ctx, String accessToken, JsonObject tokenJson) {
        String userinfoUrl = kcEndpoint(config, "/protocol/openid-connect/userinfo");

        webClient.getAbs(userinfoUrl)
            .putHeader(HEADER_AUTH, "Bearer " + accessToken)
            .send(ar -> {
                if (ar.failed() || ar.result().statusCode() != 200) {
                    redirect(ctx, SsoRoutePaths.ERROR + "?error=userinfo_failed");
                    return;
                }

                JsonObject userInfo = ar.result().bodyAsJsonObject();

                sessionRepo.createSession(userInfo, tokenJson, res -> {
                    if (!res.succeeded()) {
                        redirect(ctx, SsoRoutePaths.ERROR + "?error=session_create_failed");
                        return;
                    }

                    String sid = res.result();
                    redirect(ctx, SsoRoutePaths.SESSION + "?sid=" + sid);
                });
            });
    }

    private void refreshToken(RoutingContext ctx, String sid, JsonObject session, String refreshToken) {
        String tokenUrl = kcEndpoint(config, "/protocol/openid-connect/token");

        MultiMap form = MultiMap.caseInsensitiveMultiMap()
                .add(PARAM_GRANT_TYPE, GRANT_REFRESH)
                .add(PARAM_CLIENT_ID, config.clientId)
                .add(PARAM_REFRESH_TOKEN, refreshToken);

        if (config.clientSecret != null)
            form.add(PARAM_CLIENT_SECRET, config.clientSecret);

        webClient.postAbs(tokenUrl).sendForm(form, ar -> {
            if (ar.failed()) {
                ctx.response().setStatusCode(500).end("refresh_failed");
                return;
            }

            JsonObject newTokens = ar.result().bodyAsJsonObject();
            session.put("token", newTokens);

            sessionRepo.updateSession(sid, session, r -> sendJson(ctx, newTokens));
        });
    }
}
