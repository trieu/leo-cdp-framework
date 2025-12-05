package leotech.system.util.keycloak;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.system.domain.SystemConfigsManagement;
import leotech.system.model.AppMetadata;
import leotech.system.model.SystemService;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * Thread-safe, immutable, lazy-loaded Keycloak configuration.
 *
 * @author Trieu
 * @since 2025
 */
public final class KeycloakConfig {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakConfig.class);

	
	private boolean validated = false;
	private boolean enabled = false;
	private boolean verifySSL = false;
	
	private String url = "";
	private String realm = "";
	private String clientId = "";
	private String clientSecret = "";
	private String callbackUrl = "";

	// Constructor is private and immutable by design
	private KeycloakConfig(Map<String, Object> configs) {

		// load existing KeycloakConfig in database
		boolean ssoLoginEnabled = Boolean.valueOf(configs.getOrDefault("ssoLogin", "false").toString());
		String ssoLoginUrl = configs.getOrDefault("ssoLoginUrl", "").toString();
		if (StringUtil.isNotEmpty(ssoLoginUrl) && ssoLoginEnabled) {
			this.enabled = ssoLoginEnabled;
			this.url = ssoLoginUrl;

			this.verifySSL = Boolean.valueOf(configs.getOrDefault("keycloakVerifySSL", "false").toString());
			this.realm = configs.getOrDefault("keycloakRealm", "").toString();
			this.clientId = configs.getOrDefault("keycloakClientId", "").toString();
			this.clientSecret = configs.getOrDefault("keycloakClientSecret", "").toString();
			this.callbackUrl = configs.getOrDefault("keycloakCallbackUrl", "").toString();

			if (this.enabled) {
				validate();
			}
			logger.info("From CDP database, loaded KeycloakConfig.clientId: " + this.clientId);
		}	

	}

	// Bill Pugh Singleton — the gold standard
	private static class Holder {

		private static Map<String, Object> loadFromSystemService() {
			SystemService s = SystemConfigsManagement.loadLeoCdpMetadata();
			return s.getConfigs();
		}

		// the instance of Keycloak must load from database first, if
		// empty then try load from leocdp-metadata.properties as second option
		private static final KeycloakConfig load() {
			return new KeycloakConfig(loadFromSystemService());
		}
	}

	public static KeycloakConfig getInstance() {
		return Holder.load();
	}

	public String ssoSource() {
		return url + "#" + realm + "#" + clientId;
	}

	public long getHashedId() {
		return AppMetadata.hashToNetworkId(ssoSource());
	}

	private void validate() {
		if (StringUtil.isEmpty(url) || StringUtil.isEmpty(realm) || StringUtil.isEmpty(clientId)
				|| StringUtil.isEmpty(callbackUrl)) {
			this.validated = false;
			throw new RuntimeException("Missing required Keycloak environment variables.");
		}
		else {
			this.validated = true;
		}
	}

	///////
	
	public boolean isReady() {
		return validated && enabled ;
	}
	
	public boolean isValidated() {
		return validated;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isVerifySSL() {
		return verifySSL;
	}

	public void setVerifySSL(boolean verifySSL) {
		this.verifySSL = verifySSL;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getCallbackUrl() {
		return callbackUrl;
	}

	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}

}
