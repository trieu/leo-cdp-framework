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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import leotech.system.util.keycloak.KeycloakClientSsoRouter.SsoRoutePaths;
import rfx.core.util.StringUtil;

/**
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public class AuthKeycloakHandlers {

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

	private static final Logger log = LoggerFactory.getLogger(AuthKeycloakHandlers.class);

	private final SessionRepository sessionRepo;
	private final WebClient webClient;

	public AuthKeycloakHandlers(SessionRepository sessionRepo, WebClient wc) {
		this.sessionRepo = sessionRepo;
		this.webClient = wc;
	}

	/**
	 * Safe query param extractor <br>
	 * Extracts param safely from RoutingContext. Vert.x → queryParam(name) returns
	 * List<String>. This returns: - first value - or null if missing
	 */
	private String getQueryParam(RoutingContext ctx, String name) {
		try {
			List<String> vals = ctx.queryParam(name);
			if (vals == null || vals.isEmpty())
				return null;
			String v = vals.get(0);
			if (v == null || v.isBlank())
				return null;
			return v;
		} catch (Exception e) {
			return null;
		}
	}

	// -----------------------------------------------------------------------------
	// Public Handlers
	// -----------------------------------------------------------------------------

	public void handleInfo(RoutingContext ctx) {
		ctx.response().end(LEO_CDP_SSO);
	}

	public void handleSession(RoutingContext ctx) {
		String sid = getQueryParam(ctx, PARAM_SESSION_ID);
		if (StringUtil.isEmpty(sid)) {
			redirect(ctx, SsoRoutePaths.LOGIN);
			return;
		}

		KeycloakConfig config = KeycloakConfig.getInstance();
		if (config.isReady()) {
			sessionRepo.getSession(sid, ar -> {
				if (ar.failed() || StringUtil.isEmpty(ar.result())) {
					redirect(ctx, SsoRoutePaths.LOGIN);
					return;
				}

				JsonObject session = new JsonObject(ar.result());
				String accessToken = session.getJsonObject(TOKEN).getString(ACCESS_TOKEN);
				JsonArray roles = getUserRoles(accessToken);

				// convert JSON to SSO User
				SsoUserProfile user = SsoUserProfile.fromJson(session.getJsonObject(USER), roles);

				log.info("Admin user: {}", user);

				ctx.addCookie(makeSsoSessionCookie(sid));

				redirect(ctx, SsoRoutePaths.ROOT);
			});
		} else {
			noValidSsoConfig(ctx);
		}

	}

	public void handleLogin(RoutingContext ctx) {

		KeycloakConfig config = KeycloakConfig.getInstance();
		if (config.isReady()) {
			try {
				String redirectEncoded = encodeUrl(config.getCallbackUrl());
				String state = UUID.randomUUID().toString();

				String authUrl = String.format(
						"%s/realms/%s/protocol/openid-connect/auth?client_id=%s&response_type=code&scope=openid&state=%s&redirect_uri=%s",
						config.getUrl(), config.getRealm(), config.getClientId(), state, redirectEncoded);

				redirect(ctx, authUrl);

			} catch (Exception e) {
				log.error("Login redirect error", e);
				redirect(ctx, SsoRoutePaths.ERROR + "?error=login_build_error");
			}
		} else {
			noValidSsoConfig(ctx);
		}

	}

	public void handleCallback(RoutingContext ctx) {
		String code = getQueryParam(ctx, PARAM_CODE);
		String error = getQueryParam(ctx, ERROR);
		String logoutFlag = getQueryParam(ctx, LOGOUT);

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
			String refresh = tokenObj != null ? tokenObj.getString(REFRESH_TOKEN) : "";

			refreshToken(ctx, sid, session, refresh);
		});
	}

	public void handleLogout(RoutingContext ctx) {
		KeycloakConfig config = KeycloakConfig.getInstance();
		if (config.isReady()) {
			String sid = getQueryParam(ctx, PARAM_SESSION_ID);

			if (sid != null) {
				sessionRepo.deleteSession(sid, r -> {
				});
			}

			try {
				String redirectUri = encodeUrl(
						config.getCallbackUrl() + "?logout=true&t=" + System.currentTimeMillis());

				String logoutUrl = String.format(
						"%s/realms/%s/protocol/openid-connect/logout?client_id=%s&post_logout_redirect_uri=%s",
						config.getUrl(), config.getRealm(), config.getClientId(), redirectUri);

				redirect(ctx, logoutUrl);

			} catch (Exception e) {
				ctx.response().setStatusCode(500).end("logout_failed");
			}
		} else {
			ctx.response().putHeader(KeycloakConstants.HEADER_LOCATION, "/").setStatusCode(303).end();
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
			String accessToken = session.getJsonObject(TOKEN).getString(ACCESS_TOKEN);

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

		if (config.isReady()) {
			String tokenUrl = kcEndpoint(config, URI_OPENID_CONNECT_TOKEN);

			MultiMap form = MultiMap.caseInsensitiveMultiMap().add(PARAM_GRANT_TYPE, GRANT_AUTH_CODE)
					.add(PARAM_CODE, code).add(PARAM_REDIRECT_URI, config.getCallbackUrl())
					.add(PARAM_CLIENT_ID, config.getClientId());

			if (config.getClientSecret() != null) {
				form.add(PARAM_CLIENT_SECRET, config.getClientSecret());
			}

			webClient.postAbs(tokenUrl).sendForm(form, ar -> {
				if (ar.failed() || ar.result().statusCode() != 200) {
					redirect(ctx, SsoRoutePaths.ERROR + "?error=token_exchange_failed");
					return;
				}

				JsonObject tokenJson = ar.result().bodyAsJsonObject();
				String accessToken = tokenJson.getString(ACCESS_TOKEN);

				fetchUserInfo(ctx, accessToken, tokenJson);
			});
		} else {
			noValidSsoConfig(ctx);
		}

	}

	private void fetchUserInfo(RoutingContext ctx, String accessToken, JsonObject tokenJson) {
		KeycloakConfig config = KeycloakConfig.getInstance();
		if (config.isReady()) {
			String userinfoUrl = kcEndpoint(config, URI_OPENID_CONNECT_USERINFO);

			webClient.getAbs(userinfoUrl).putHeader(HEADER_AUTH, "Bearer " + accessToken).send(ar -> {
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
		} else {
			noValidSsoConfig(ctx);
		}

	}

	private void refreshToken(RoutingContext ctx, String sid, JsonObject session, String refreshToken) {
		KeycloakConfig config = KeycloakConfig.getInstance();
		if (config.isReady()) {
			String tokenUrl = kcEndpoint(config, URI_OPENID_CONNECT_TOKEN);

			MultiMap form = MultiMap.caseInsensitiveMultiMap().add(PARAM_GRANT_TYPE, GRANT_REFRESH)
					.add(PARAM_CLIENT_ID, config.getClientId()).add(PARAM_REFRESH_TOKEN, refreshToken);

			if (!config.getClientSecret().isEmpty())
				form.add(PARAM_CLIENT_SECRET, config.getClientSecret());

			webClient.postAbs(tokenUrl).sendForm(form, ar -> {
				if (ar.failed()) {
					ctx.response().setStatusCode(500).end("refresh_failed");
					return;
				}

				JsonObject newTokens = ar.result().bodyAsJsonObject();
				session.put(TOKEN, newTokens);

				sessionRepo.updateSession(sid, session, r -> sendJson(ctx, newTokens));
			});
		} else {
			noValidSsoConfig(ctx);
		}

	}

	private void noValidSsoConfig(RoutingContext ctx) {
		ctx.response().setStatusCode(500).putHeader(KeycloakConstants.HEADER_CONTENT_TYPE, KeycloakConstants.MIME_JSON)
				.end(new JsonObject().put("error", "Keycloak not configured").encode());
	}
}
