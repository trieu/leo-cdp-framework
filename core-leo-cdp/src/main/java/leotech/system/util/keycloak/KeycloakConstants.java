package leotech.system.util.keycloak;

/**
 *  Constants for local Keycloak client in LEO CDP
 * 
 * @author Trieu Nguyen
 * @since 2025
 */
public final class KeycloakConstants {

    private KeycloakConstants() {}

    // HTTP Headers
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_AUTH = "Authorization";

    // MIME types
    public static final String MIME_JSON = "application/json";

    // OIDC Params
    public static final String PARAM_CLIENT_ID = "client_id";
    public static final String PARAM_CLIENT_SECRET = "client_secret";
    public static final String PARAM_REDIRECT_URI = "redirect_uri";
    public static final String PARAM_RESPONSE_TYPE = "response_type";
    public static final String PARAM_SCOPE = "scope";
    public static final String PARAM_STATE = "state";
    public static final String PARAM_GRANT_TYPE = "grant_type";
    public static final String PARAM_CODE = "code";
    public static final String PARAM_REFRESH_TOKEN = "refresh_token";

    // OIDC Grant Types
    public static final String GRANT_AUTH_CODE = "authorization_code";
    public static final String GRANT_REFRESH = "refresh_token";

    // Session Cookie
    public static final String COOKIE_SESSION_ID = "sid";
}
