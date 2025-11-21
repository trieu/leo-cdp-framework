package leotech.system.util.keycloak;

import rfx.core.util.StringUtil;

/**
 * KeycloakConfig: Encapsulates environment variables and validation logic.
 * 
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public class KeycloakConfig {
	public final boolean enabled;
	public final boolean verifySSL;
	public final String url;
	public final String realm;
	public final String clientId;
	public final String clientSecret;
	public final String callbackUrl;

	public KeycloakConfig() {
		this.enabled = getEnvBool("KEYCLOAK_ENABLED", true);
		this.verifySSL = getEnvBool("KEYCLOAK_VERIFY_SSL", true);
		this.url = System.getenv("KEYCLOAK_URL");
		this.realm = System.getenv("KEYCLOAK_REALM");
		this.clientId = System.getenv("KEYCLOAK_CLIENT_ID");
		this.clientSecret = System.getenv("KEYCLOAK_CLIENT_SECRET");
		this.callbackUrl = System.getenv("KEYCLOAK_CALLBACK_URL");

		if (enabled) {
			validate();
		}

	}

	private void validate() {
		if (StringUtil.isEmpty(url) || StringUtil.isEmpty(realm) || StringUtil.isEmpty(clientId)
				|| StringUtil.isEmpty(callbackUrl)) {
			throw new RuntimeException("Missing required Keycloak env vars.");
		}
	}

	private boolean getEnvBool(String key, boolean def) {
		String val = System.getenv(key);
		if (val == null)
			return def;
		return val.equalsIgnoreCase("true") || val.equals("1");
	}
}