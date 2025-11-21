package leotech.system.util.keycloak;

import leotech.system.version.SystemMetaData;
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
		this.enabled = SystemMetaData.SSO_LOGIN;
		this.verifySSL = SystemMetaData.getBoolean("keycloakVerifySSL", true);
		this.url = SystemMetaData.getString("keycloakUrl", "");
		this.realm = SystemMetaData.getString("keycloakRealm", "");
		this.clientId = SystemMetaData.getString("keycloakClientId", "");
		this.clientSecret = SystemMetaData.getString("keycloakClientSecret", "");
		this.callbackUrl = SystemMetaData.getString("keycloakCallbackUrl", "");

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

}