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

	private final KeycloakConfig config;
	private final SessionRepository sessionRepo;
	private final WebClient webClient;

	public AuthKeycloakHandlers(KeycloakConfig config, SessionRepository sessionRepo, WebClient webClient) {
		this.config = config;
		this.sessionRepo = sessionRepo;
		this.webClient = webClient;
	}

	/**
	 * Admin handler: must receive a valid sid. Reads user & token from Redis
	 * session storage, converts user JSON into typed class, and returns context as
	 * JSON.
	 */
	public void handleAdmin(RoutingContext ctx) {
		String sessionId = ctx.queryParams().get("sid");
		if (sessionId == null) {
			redirectLogin(ctx);
			return;
		}

		sessionRepo.getSession(sessionId, res -> {
			if (res.failed()) {
				logger.error("handleAdmin: failed to fetch session", res.cause());
				redirectError(ctx, "session_read_error");
				return;
			}

			String rawSession = res.result();
			if (!StringUtil.isNotEmpty(rawSession)) {
				redirectLogin(ctx);
				return;
			}

			JsonObject userJson = new JsonObject(rawSession).getJsonObject("user");
			UserProfile user = UserProfile.fromJson(userJson);

			JsonObject responseJson = new JsonObject().put("user", JsonObject.mapFrom(user))
					.put("session_id", sessionId).put("timestamp", System.currentTimeMillis() / 1000);

			ctx.response().putHeader("Content-Type", "application/json").end(responseJson.encodePrettily());
		});
	}

	/**
	 * Redirect user to Keycloak login page.
	 */
	public void handleLogin(RoutingContext ctx) {
		try {
			String encodedRedirect = URLEncoder.encode(config.callbackUrl, StandardCharsets.UTF_8.name());
			String state = UUID.randomUUID().toString(); // CSRF prevention token

			String redirectUrl = config.url + "/realms/" + config.realm + "/protocol/openid-connect/auth"
					+ "?client_id=" + config.clientId + "&response_type=code" + "&scope=openid" + "&state=" + state
					+ "&redirect_uri=" + encodedRedirect;

			ctx.response().putHeader("Location", redirectUrl).setStatusCode(302).end();

		} catch (Exception e) {
			logger.error("handleLogin: failed to build redirect URL", e);
			redirectError(ctx, "login_build_error");
		}
	}

	/**
	 * Keycloak callback handler: - Exchanges authorization_code for tokens -
	 * Fetches userinfo - Creates internal session (sid)
	 */
	public void handleCallback(RoutingContext ctx) {
		String code = ctx.request().getParam("code");
		String error = ctx.request().getParam("error");
		String logout = ctx.request().getParam("logout");

		// If coming from logout flow
		if ("true".equalsIgnoreCase(logout)) {
			ctx.response().putHeader("Location", "/admin?_t=" + System.currentTimeMillis()).setStatusCode(303).end();
			return;
		}

		// Keycloak returned an error
		if (error != null) {
			redirectError(ctx, error);
			return;
		}

		// Missing required auth code
		if (code == null) {
			redirectError(ctx, "missing_code");
			return;
		}

		// Prepare token exchange request
		String tokenUrl = config.url + "/realms/" + config.realm + "/protocol/openid-connect/token";
		MultiMap form = MultiMap.caseInsensitiveMultiMap().add("grant_type", "authorization_code").add("code", code)
				.add("client_id", config.clientId).add("redirect_uri", config.callbackUrl);

		if (config.clientSecret != null) {
			form.add("client_secret", config.clientSecret);
		}

		// Exchange "code" for tokens
		webClient.postAbs(tokenUrl).sendForm(form, ar -> {
			if (ar.failed() || ar.result().statusCode() != 200) {
				redirectError(ctx, "token_exchange_failed");
				return;
			}

			JsonObject tokenJson = ar.result().bodyAsJsonObject();
			String accessToken = tokenJson.getString("access_token");

			fetchUserInfo(ctx, accessToken, tokenJson);
		});
	}

	/**
	 * Fetch /userinfo with access_token and create internal session in Redis.
	 */
	private void fetchUserInfo(RoutingContext ctx, String accessToken, JsonObject tokenData) {
		String userInfoUrl = config.url + "/realms/" + config.realm + "/protocol/openid-connect/userinfo";

		webClient.getAbs(userInfoUrl).putHeader("Authorization", "Bearer " + accessToken).send(ar -> {
			if (ar.failed() || ar.result().statusCode() != 200) {
				redirectError(ctx, "userinfo_fetch_failed");
				return;
			}

			JsonObject userInfo = ar.result().bodyAsJsonObject();

			// Store user + token in Redis session
			sessionRepo.createSession(userInfo, tokenData, res -> {
				if (res.succeeded()) {
					String sid = res.result();
					ctx.response().putHeader("Location", "/admin?sid=" + sid).setStatusCode(303).end();
				} else {
					redirectError(ctx, "session_create_failed");
				}
			});
		});
	}

	/**
	 * Refresh Keycloak token using refresh_token stored in session.
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

			MultiMap form = MultiMap.caseInsensitiveMultiMap().add("grant_type", "refresh_token")
					.add("client_id", config.clientId).add("client_secret", config.clientSecret)
					.add("refresh_token", refreshToken);

			String tokenUrl = config.url + "/realms/" + config.realm + "/protocol/openid-connect/token";

			webClient.postAbs(tokenUrl).sendForm(form, tokenResp -> {
				if (tokenResp.failed()) {
					ctx.response().setStatusCode(500).end("Token refresh failed");
					return;
				}

				JsonObject newTokens = tokenResp.result().bodyAsJsonObject();
				session.put("token", newTokens);

				sessionRepo.updateSession(sid, session, updateRes -> {
					ctx.response().putHeader("Content-Type", "application/json").end(newTokens.encode());
				});
			});
		});
	}

	/**
	 * Logout: remove session + redirect to Keycloak logout endpoint.
	 */
	public void handleLogout(RoutingContext ctx) {
		String sid = ctx.queryParams().get("sid");

		if (sid != null) {
			sessionRepo.deleteSession(sid, res -> {
			});
		}

		String redirect = config.callbackUrl + "?logout=true";

		try {
			redirect = URLEncoder.encode(redirect, StandardCharsets.UTF_8.name());
		} catch (Exception ignored) {
		}

		String logoutUrl = config.url + "/realms/" + config.realm + "/protocol/openid-connect/logout" + "?client_id="
				+ config.clientId + "&post_logout_redirect_uri=" + redirect;

		ctx.response().putHeader("Location", logoutUrl).setStatusCode(303).end();
	}

	/**
	 * Example protected API with role checking.
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

			JsonObject payload = decodeJwtWithoutVerify(accessToken);

			boolean hasRole = false;
			try {
				JsonArray roles = payload.getJsonObject("realm_access").getJsonArray("roles");
				logger.info("sid " + sid + " roles " + roles);
				hasRole = roles.contains("DATA_OPERATOR");
			} catch (Exception ignored) {
			}

			if (!hasRole) {
				ctx.response().setStatusCode(403).end("Forbidden");
				return;
			}

			JsonObject data = new JsonObject().put("message", "Welcome authorized user").put("records",
					new JsonObject().put("id", 123).put("value", "example record"));

			ctx.response().putHeader("Content-Type", "application/json").end(data.encode());
		});
	}

	/**
	 * Decode JWT payload without verifying signature (client-side inspection only).
	 */
	private JsonObject decodeJwtWithoutVerify(String jwt) {
		try {
			String[] parts = jwt.split("\\.");
			String payloadJson = new String(java.util.Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
			return new JsonObject(payloadJson);
		} catch (Exception e) {
			return new JsonObject();
		}
	}

	private void redirectLogin(RoutingContext ctx) {
		ctx.response().putHeader("Location", "/_leocdp/sso/login").setStatusCode(303).end();
	}

	private void redirectError(RoutingContext ctx, String code) {
		ctx.response().putHeader("Location", "/_leocdp/sso/error?error=" + code).setStatusCode(303).end();
	}
}
