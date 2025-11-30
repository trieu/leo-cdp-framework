package leotech.system.util.keycloak;

import leotech.system.model.AppMetadata;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * Thread-safe, immutable, lazy-loaded Keycloak configuration.
 * No locking, no race conditions, safe under heavy concurrency.
 *
 * @author Trieu
 * @since 2025
 */
public final class KeycloakConfig {

    public final boolean enabled;
    public final boolean verifySSL;
    public final String url;
    public final String realm;
    public final String clientId;
    public final String clientSecret;
    public final String callbackUrl;

    // Constructor is private and immutable by design
    private KeycloakConfig() {
        this.enabled = SystemMetaData.SSO_LOGIN;
        this.url = SystemMetaData.SSO_LOGIN_URL;

        this.verifySSL = SystemMetaData.getBoolean("keycloakVerifySSL", true);
        this.realm = SystemMetaData.getString("keycloakRealm", "");
        this.clientId = SystemMetaData.getString("keycloakClientId", "");
        this.clientSecret = SystemMetaData.getString("keycloakClientSecret", "");
        this.callbackUrl = SystemMetaData.getString("keycloakCallbackUrl", "");

        if (enabled) {
            validate();
        }
    }

    // Bill Pugh Singleton — the gold standard
    private static class Holder {
        private static final KeycloakConfig INSTANCE = new KeycloakConfig();
    }

    public static KeycloakConfig getInstance() {
        return Holder.INSTANCE;
    }

    public String ssoSource() {
        return url + " Realm:" + realm + " Client:" + clientId;
    }

    public long getHashedId() {
        return AppMetadata.hashToNetworkId(ssoSource());
    }

    private void validate() {
        if (StringUtil.isEmpty(url) ||
            StringUtil.isEmpty(realm) ||
            StringUtil.isEmpty(clientId) ||
            StringUtil.isEmpty(callbackUrl)) {

            throw new RuntimeException("Missing required Keycloak environment variables.");
        }
    }
}
