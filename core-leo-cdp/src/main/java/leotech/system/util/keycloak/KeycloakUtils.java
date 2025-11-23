package leotech.system.util.keycloak;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.RoutingContext;

import java.util.Base64;

import static leotech.system.util.keycloak.KeycloakConstants.*;

/**
 *  Util for local Keycloak client in LEO CDP
 * 
 * @author Trieu Nguyen
 * @since 2025
 */
public final class KeycloakUtils {

    private KeycloakUtils() {}

    // -----------------------------------------------------------------------------
    // JWT Utilities
    // -----------------------------------------------------------------------------

    public static JsonObject decodeJwtWithoutVerify(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return new JsonObject();
            String payloadJson = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return new JsonObject(payloadJson);
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public static JsonArray getUserRoles(String accessToken) {
        try {
            JsonObject payload = decodeJwtWithoutVerify(accessToken);
            return payload.getJsonObject("realm_access").getJsonArray("roles");
        } catch (Exception e) {
            return new JsonArray();
        }
    }

    public static boolean hasRole(String accessToken, String requiredRole) {
        JsonArray roles = getUserRoles(accessToken);
        return roles != null && roles.contains(requiredRole);
    }

    // -----------------------------------------------------------------------------
    // Response Utilities
    // -----------------------------------------------------------------------------

    public static void sendJson(RoutingContext ctx, JsonObject json) {
        ctx.response()
           .putHeader(HEADER_CONTENT_TYPE, MIME_JSON)
           .end(json.encodePrettily());
    }

    public static void redirect(RoutingContext ctx, String url) {
        ctx.response()
            .putHeader(HEADER_LOCATION, url)
            .setStatusCode(303)
            .end();
    }

    /**
     * COOKIE_SESSION_ID 
     * 
     * @param sid
     * @return
     */
    public static Cookie makeSsoSessionCookie(String ssosid) {
        return Cookie.cookie(COOKIE_SSO_SESSION_ID, ssosid)
                .setHttpOnly(true)
                .setSecure(true)
                .setPath("/")
                .setMaxAge(8);
    }

    public static String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return url;
        }
    }

    // -----------------------------------------------------------------------------
    // Keycloak Endpoint Builder
    // -----------------------------------------------------------------------------

    public static String kcEndpoint(KeycloakConfig cfg, String suffix) {
        return cfg.url + "/realms/" + cfg.realm + suffix;
    }
}
