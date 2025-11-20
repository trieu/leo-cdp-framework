package test.cdp;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;

public class KeycloakRouterVerticle extends AbstractVerticle {
	private static final String HTTP_HOST = "0.0.0.0";
	private static final int HTTP_PORT = 8888;

	private static final Logger logger = LoggerFactory.getLogger("leobot-admin");

	private WebClient webClient;
	static JedisPool jedisPool = RedisClientFactory.buildRedisPool("clusterInfoRedis");
	private boolean keycloakEnabled;
	private boolean verifySSL = false;

	private String keycloakUrl;
	private String keycloakRealm;
	private String keycloakClientId;
	private String keycloakClientSecret;
	private String keycloakCallbackUrl;

	@Override
	public void start(Future<Void> startFuture) {
		try {
			// Load config from environment
			keycloakEnabled = getEnvBool("KEYCLOAK_ENABLED", true);
			verifySSL = getEnvBool("KEYCLOAK_VERIFY_SSL", true);

			keycloakUrl = System.getenv("KEYCLOAK_URL");
			keycloakRealm = System.getenv("KEYCLOAK_REALM");
			keycloakClientId = System.getenv("KEYCLOAK_CLIENT_ID");
			keycloakClientSecret = System.getenv("KEYCLOAK_CLIENT_SECRET");
			keycloakCallbackUrl = System.getenv("KEYCLOAK_CALLBACK_URL");

			logger.info("KEYCLOAK_CLIENT_ID " + keycloakClientId);
			logger.info("KEYCLOAK_CALLBACK_URL " + keycloakCallbackUrl);
			logger.info("KEYCLOAK_URL " + keycloakUrl);

			if (StringUtil.isEmpty(keycloakUrl) || StringUtil.isEmpty(keycloakRealm)
					|| StringUtil.isEmpty(keycloakClientId) || StringUtil.isEmpty(keycloakCallbackUrl)) {
				throw new RuntimeException("Missing required Keycloak env vars (URL/REALM/CLIENT_ID/CALLBACK_URL).");
			}

			if (keycloakClientSecret == null || keycloakClientSecret.isEmpty()) {
				throw new RuntimeException("KEYCLOAK_CLIENT_SECRET is missing.");
			}

			// Setup Vert.x WebClient with SSL options
			boolean isHttps = keycloakUrl.toLowerCase().startsWith("https");
			WebClientOptions options = new WebClientOptions().setSsl(isHttps)
					// when verifySSL=false (DEV): trustAll + no host verify
					.setTrustAll(!verifySSL).setVerifyHost(verifySSL);

			webClient = WebClient.create(vertx, options);

			Router router = Router.router(vertx);
			router.route().handler(BodyHandler.create());

			// Simple health check
			router.get("/_leocdp/is-admin-ready").handler(this::handleReady);

			if (keycloakEnabled) {
				registerKeycloakRoutes(router);
			} else {
				registerFallbackRoutes(router);
			}

			// Start HTTP server
			vertx.createHttpServer().requestHandler(router).listen(HTTP_PORT, HTTP_HOST, server -> {
				if (server.succeeded()) {
					logger.info("✅ Admin router started on {}:{}", HTTP_HOST, server.result().actualPort());
					startFuture.complete();
				} else {
					logger.error("❌ Failed to start HTTP server", server.cause());
					startFuture.fail(server.cause());
				}
			});

		} catch (Exception e) {
			logger.error("❌ Failed to start KeycloakRouterVerticle", e);
			startFuture.fail(e);
		}
	}

	private void handleReady(RoutingContext ctx) {
		ctx.response().putHeader("Content-Type", "application/json")
				.end(new JsonObject().put("ok", keycloakEnabled).encode());
	}

	// ------------------------------------------------------------------------------
	// Routes when Keycloak is enabled
	// ------------------------------------------------------------------------------
	private void registerKeycloakRoutes(Router router) {
		router.get("/_leocdp/sso/login").handler(this::handleLogin);
		router.get("/_leocdp/sso/refresh").handler(this::handleRefreshToken);
		router.get("/_leocdp/sso/error").handler(this::handleError);
		router.get("/_leocdp/sso/callback").handler(this::handleCallback);
		router.get("/_leocdp/sso/me").handler(this::handleGetMe);
		router.get("/_leocdp/sso/logout").handler(this::handleLogout);
		router.get("/admin").handler(this::handleAdmin);
		router.get("/api/data").handler(this::handleDataApi);
	}

	// ------------------------------------------------------------------------------
	// LOGIN: redirect to Keycloak auth endpoint
	// ------------------------------------------------------------------------------
	private void handleLogin(RoutingContext ctx) {
		try {
			String encodedRedirect = URLEncoder.encode(keycloakCallbackUrl, StandardCharsets.UTF_8.name());

			// Optional CSRF state (not persisted here, minimal implementation)
			String state = UUID.randomUUID().toString();

			String redirectUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/auth"
					+ "?client_id=" + keycloakClientId + "&response_type=code" + "&scope=openid" + "&state=" + state
					+ "&redirect_uri=" + encodedRedirect;

			logger.info("🔑 Redirecting to Keycloak login: {}", redirectUrl);

			ctx.response().putHeader("Location", redirectUrl).setStatusCode(302).end();
		} catch (Exception e) {
			logger.error("handleLogin: failed to build redirect URL", e);
			redirectError(ctx, "login_build_error");
		}
	}
	
	private void handleRefreshToken(RoutingContext ctx) {
	    String sid = ctx.queryParams().get("sid");
	    if (sid == null) {
	        ctx.response().setStatusCode(401).end("Missing session id");
	        return;
	    }

	    // Step 1: read session from Redis
	    vertx.executeBlocking(future -> {
	        RedisCommand<String> cmd = new RedisCommand<String>(jedisPool) {
	            @Override
	            protected String build(Jedis jedis) {
	                return jedis.get(sid);
	            }
	        };
	        future.complete(cmd.execute());
	    }, ar -> {
	        if (!ar.succeeded() || ar.result() == null) {
	            ctx.response().setStatusCode(401).end("Invalid session");
	            return;
	        }

	        JsonObject session = new JsonObject(ar.result().toString());
	        String refreshToken = session.getJsonObject("token").getString("refresh_token");

	        // Step 2: call Keycloak /token endpoint
	        MultiMap form = MultiMap.caseInsensitiveMultiMap();
	        form.add("grant_type", "refresh_token");
	        form.add("client_id", keycloakClientId);
	        form.add("client_secret", keycloakClientSecret);
	        form.add("refresh_token", refreshToken);

	        String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm +
	                "/protocol/openid-connect/token";

	        webClient.postAbs(tokenUrl).sendForm(form, tokenResp -> {
	            if (tokenResp.failed()) {
	                ctx.response().setStatusCode(500).end("Token refresh failed");
	                return;
	            }

	            JsonObject newTokens = tokenResp.result().bodyAsJsonObject();
	            session.put("token", newTokens);

	            // Step 3: write back to Redis
	            vertx.executeBlocking(f2 -> {
	                RedisCommand<Boolean> cmd2 = new RedisCommand<Boolean>(jedisPool) {
	                    @Override
	                    protected Boolean build(Jedis jedis) {
	                        jedis.setex(sid, 3600, session.toString());
	                        return true;
	                    }
	                };
	                f2.complete(cmd2.execute());
	            }, r2 -> {
	                ctx.response()
	                   .putHeader("Content-Type", "application/json")
	                   .end(newTokens.encode());
	            });
	        });
	    });
	}


	private void handleError(RoutingContext ctx) {
		String error = ctx.queryParams().get("error");
		String description = ctx.queryParams().get("description");

		JsonObject payload = new JsonObject().put("error", error).put("message", "SSO Error: " + error)
				.put("description", description).put("timestamp", System.currentTimeMillis() / 1000);

		ctx.response().setStatusCode(400).putHeader("Content-Type", "application/json").end(payload.encode());
	}

	// ------------------------------------------------------------------------------
	// CALLBACK: handle code → tokens → userinfo
	// ------------------------------------------------------------------------------
	private void handleCallback(RoutingContext ctx) {

		final String code = ctx.request().getParam("code");
		final String error = ctx.request().getParam("error");
		final String logout = ctx.request().getParam("logout");
		final String sessionState = ctx.request().getParam("session_state");

		logger.info("🔁 handleCallback: code={}, error={}, logout={}, session_state={}", code, error, logout,
				sessionState);

		// Handle logout flow redirected back here
		if ("true".equalsIgnoreCase(logout)) {
			ctx.response().putHeader("Location", "/admin?_t=" + System.currentTimeMillis()).setStatusCode(303).end();
			return;
		}

		// Keycloak returned error
		if (error != null) {
			logger.warn("handleCallback: Keycloak returned error={}", error);
			redirectError(ctx, error);
			return;
		}

		// Missing required auth code
		if (code == null) {
			logger.warn("handleCallback: missing authorization code");
			redirectError(ctx, "missing_code");
			return;
		}

		// redirect_uri MUST match exactly the configured value in Keycloak client
		String redirectUri = keycloakCallbackUrl;

		String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";

		MultiMap form = MultiMap.caseInsensitiveMultiMap();
		form.add("grant_type", "authorization_code");
		form.add("code", code);
		form.add("client_id", keycloakClientId);

		// Confidential client uses client_secret
		if (keycloakClientSecret != null && !keycloakClientSecret.isEmpty()) {
			form.add("client_secret", keycloakClientSecret);
		}

		form.add("redirect_uri", redirectUri);

		logger.info("🔑 Exchanging code for token at {}", tokenUrl);

		webClient.postAbs(tokenUrl).sendForm(form, ar -> {
			if (ar.failed()) {
				logger.error("Token request failed", ar.cause());
				redirectError(ctx, "token_request_error");
				return;
			}

			HttpResponse<Buffer> resp = ar.result();

			if (resp.statusCode() != 200) {
				logger.error("Token exchange rejected [{}]: {}", resp.statusCode(), resp.bodyAsString());
				redirectError(ctx, "token_rejected");
				return;
			}

			JsonObject token = resp.bodyAsJsonObject();
			String accessToken = token.getString("access_token");

			if (accessToken == null) {
				logger.error("Token JSON missing access_token: {}", token.encode());
				redirectError(ctx, "invalid_token");
				return;
			}

			logger.info("✅ Token exchange OK, fetching userinfo");
			fetchUserInfo(accessToken, ctx, token);
		});
	}

	// ------------------------------------------------------------------------------
	// USERINFO & SESSION
	// ------------------------------------------------------------------------------
	private void fetchUserInfo(String accessToken, RoutingContext ctx, JsonObject tokenData) {
		String userInfoUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/userinfo";

		webClient.getAbs(userInfoUrl).putHeader("Authorization", "Bearer " + accessToken).send(ar -> {
			if (ar.succeeded()) {
				HttpResponse<Buffer> resp = ar.result();
				if (resp.statusCode() == 200) {
					JsonObject userInfo = resp.bodyAsJsonObject();
					logger.info("✅ userinfo fetched: {}", userInfo.encode());

					createRedisSession(ctx, userInfo, tokenData, sessionId -> {
						ctx.response().putHeader("Location", "/admin?sid=" + sessionId).setStatusCode(303).end();
					});
				} else {
					logger.error("userinfo_fetch_failed [{}]: {}", resp.statusCode(), resp.bodyAsString());
					redirectError(ctx, "userinfo_fetch_failed");
				}
			} else {
				logger.error("Failed to fetch user info", ar.cause());
				redirectError(ctx, "server_error");
			}
		});
	}

	void createRedisSession(RoutingContext ctx, JsonObject userInfo, JsonObject tokenData,
			Handler<String> successCallback) {

		String sessionId = "sid:" + UUID.randomUUID();
		JsonObject data = new JsonObject().put("user", userInfo).put("token", tokenData).put("timestamp",
				System.currentTimeMillis() / 1000);

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
				future.complete(sessionId);
			} else {
				future.fail("Failed to set session in Redis");
			}
		}, (AsyncResult<String> res) -> {
			if (res.succeeded()) {
				logger.info("✅ createRedisSession OK: sessionId {}", res.result());
				successCallback.handle(res.result());
			} else {
				logger.error("❌ createRedisSession Failed", res.cause());
				redirectError(ctx, "session_create_failed");
			}
		});
	}

	// ------------------------------------------------------------------------------
	// /admin – return JSON only
	// ------------------------------------------------------------------------------
	private void handleAdmin(RoutingContext ctx) {
		String sessionId = ctx.queryParams().get("sid");
		if (sessionId == null) {
			redirectLogin(ctx);
			return;
		}

		vertx.executeBlocking(future -> {
			RedisCommand<String> cmd = new RedisCommand<String>(jedisPool) {
				@Override
				protected String build(Jedis jedis) throws JedisException {
					return jedis.get(sessionId);
				}
			};
			String rs = cmd.execute();
			future.complete(rs);
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
						.put("user", user).put("session_id", sessionId)
						.put("timestamp", System.currentTimeMillis() / 1000);

				ctx.response().putHeader("Content-Type", "application/json").end(context.encodePrettily());

			} else {
				redirectLogin(ctx);
			}
		});
	}

	// ------------------------------------------------------------------------------
	// /_leocdp/sso/me – return user JSON from session
	// ------------------------------------------------------------------------------
	private void handleGetMe(RoutingContext ctx) {
		String sid = ctx.queryParams().get("sid");
		if (sid == null) {
			ctx.response().setStatusCode(401).end("Missing session ID");
			return;
		}

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
				ctx.response().putHeader("Content-Type", "application/json").end(
						new JsonObject().put("user", session.getJsonObject("user")).put("session_id", sid).encode());
			} else {
				ctx.response().setStatusCode(401).end("Invalid or expired session");
			}
		});
	}
	
	private void handleDataApi(RoutingContext ctx) {

	    String sid = ctx.queryParams().get("sid");
	    if (sid == null) {
	        ctx.response().setStatusCode(401).end("Missing sid");
	        return;
	    }

	    // Step 1: fetch Redis session
	    vertx.executeBlocking(f -> {
	        RedisCommand<String> cmd = new RedisCommand<String>(jedisPool) {
	            @Override
	            protected String build(Jedis jedis) {
	                return jedis.get(sid);
	            }
	        };
	        f.complete(cmd.execute());
	    }, ar -> {
	        if (ar.result() == null) {
	            ctx.response().setStatusCode(401).end("Invalid session");
	            return;
	        }

	        JsonObject session = new JsonObject(ar.result().toString());
	        JsonObject token = session.getJsonObject("token");
	        String accessToken = token.getString("access_token");

	        // Step 2: decode JWT roles
	        JsonObject payload = decodeJwtWithoutVerify(accessToken);
	        boolean hasRole = payload
	            .getJsonObject("realm_access")
	            .getJsonArray("roles")
	            .contains("data_reader");

	        if (!hasRole) {
	            ctx.response().setStatusCode(403).end("Forbidden");
	            return;
	        }

	        // Step 3: return example data
	        JsonObject data = new JsonObject()
	            .put("message", "Welcome authorized user")
	            .put("records", new JsonObject()
	                .put("id", 123)
	                .put("value", "example record"));

	        ctx.response()
	           .putHeader("Content-Type", "application/json")
	           .end(data.encode());
	    });
	}
	
	private JsonObject decodeJwtWithoutVerify(String jwt) {
	    String[] parts = jwt.split("\\.");
	    String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
	    return new JsonObject(payload);
	}


	// ------------------------------------------------------------------------------
	// /_leocdp/sso/logout – kill Redis session + Keycloak logout
	// ------------------------------------------------------------------------------
	private void handleLogout(RoutingContext ctx) {
		String sid = ctx.queryParams().get("sid");

		// Build post_logout_redirect_uri=<callback>?logout=true (URL encoded)
		String redirect = keycloakCallbackUrl + "?logout=true";
		String encodedRedirect;
		try {
			encodedRedirect = URLEncoder.encode(redirect, StandardCharsets.UTF_8.name());
		} catch (Exception e) {
			logger.error("handleLogout: failed to encode redirect", e);
			encodedRedirect = redirect;
		}

		String logoutUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/logout" + "?client_id="
				+ keycloakClientId + "&post_logout_redirect_uri=" + encodedRedirect;

		if (sid != null) {
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

		ctx.response().putHeader("Location", logoutUrl).setStatusCode(303).end();
	}

	// ------------------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------------------
	void redirectLogin(RoutingContext ctx) {
		ctx.response().putHeader("Location", "/_leocdp/sso/login").setStatusCode(303).end();
	}

	void redirectError(RoutingContext ctx, String code) {
		ctx.response().putHeader("Location", "/_leocdp/sso/error?error=" + code).setStatusCode(303).end();
	}

	private void registerFallbackRoutes(Router router) {
		router.get("/admin")
				.handler(ctx -> ctx.response().setStatusCode(503).end("Keycloak not enabled or failed to initialize."));
		router.get("/_leocdp/sso/login")
				.handler(ctx -> ctx.response().setStatusCode(500).end("{\"error\": \"Keycloak not configured\"}"));
		router.get("/_leocdp/sso/logout")
				.handler(ctx -> ctx.response().putHeader("Location", "/").setStatusCode(303).end());
	}

	private boolean getEnvBool(String key, boolean def) {
		String val = System.getenv(key);
		if (val == null)
			return def;
		return val.equalsIgnoreCase("true") || val.equals("1") || val.equalsIgnoreCase("yes");
	}
}
