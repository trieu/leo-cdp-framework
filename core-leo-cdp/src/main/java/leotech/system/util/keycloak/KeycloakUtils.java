package leotech.system.util.keycloak;

import static leotech.system.util.keycloak.KeycloakConstants.COOKIE_SSO_SESSION_ID;
import static leotech.system.util.keycloak.KeycloakConstants.HEADER_CONTENT_TYPE;
import static leotech.system.util.keycloak.KeycloakConstants.HEADER_LOCATION;
import static leotech.system.util.keycloak.KeycloakConstants.MIME_JSON;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import rfx.core.util.StringUtil;

/**
 * Util for local Keycloak client in LEO CDP
 * 
 * @author Trieu Nguyen
 * @since 2025
 */
public final class KeycloakUtils {

	private static final int MAX_AGE_SSO_COOKIE = 30; // in seconds
	private static final int HTTP_CODE_SEE_OTHER_303 = 303;

	private KeycloakUtils() {
	}

	// -----------------------------------------------------------------------------
	// JWT Utilities
	// -----------------------------------------------------------------------------

	public static JsonObject decodeJwtWithoutVerify(String jwt) {
		try {
			String[] parts = jwt.split("\\.");
			if (parts.length < 2)
				return new JsonObject();
			String payloadJson = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
			return new JsonObject(payloadJson);
		} catch (Exception e) {
			return new JsonObject();
		}
	}

	/**
	 * @param accessToken
	 * @return
	 */
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

	/**
	 * @param ctx
	 * @param json
	 */
	public static void sendJson(RoutingContext ctx, JsonObject json) {
		ctx.response().putHeader(HEADER_CONTENT_TYPE, MIME_JSON).end(json.encodePrettily());
	}

	/**
	 * redirect 303
	 * 
	 * @param ctx
	 * @param url
	 */
	public static void redirect(RoutingContext ctx, String url) {
		ctx.response().putHeader(HEADER_LOCATION, url).setStatusCode(HTTP_CODE_SEE_OTHER_303).end();
	}

	/**
	 * COOKIE_SESSION_ID
	 * 
	 * @param sid
	 * @return
	 */
	public static Cookie makeSsoSessionCookie(String ssosid) {
		return Cookie.cookie(COOKIE_SSO_SESSION_ID, ssosid).setHttpOnly(true).setSecure(true).setPath("/").setMaxAge(MAX_AGE_SSO_COOKIE);
	}
	
	/**
	 * @param cookieMap
	 * @param name
	 * @return
	 */
	public static String getValueOfCookie(Map<String, Cookie> cookieMap, String name) {
		Cookie cookie = cookieMap.get( name);
		return cookie != null ? StringUtil.safeString(cookie.getValue()) : "";
	}

	/**
	 * @param url
	 * @return
	 */
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

	/**
	 * @param cfg
	 * @param suffix
	 * @return
	 */
	public static String kcEndpoint(KeycloakConfig cfg, String suffix) {
		return cfg.getUrl() + "/realms/" + cfg.getRealm() + suffix;
	}
}
