package leotech.system.util.keycloak;

import io.vertx.core.json.JsonObject;

/**
 * UserProfile: the user profile from Keycloak callback
 * 
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public class UserProfile {

	private String sub;
	private boolean emailVerified;
	private String name;
	private String preferredUsername;
	private String givenName;
	private String familyName;
	private String email;

	public static UserProfile fromJson(JsonObject json) {
		System.out.println("json " + json);
		UserProfile u = new UserProfile();
		u.setSub(json.getString("sub"));
		u.setEmailVerified(json.getBoolean("email_verified", false));
		u.setName(json.getString("name"));
		u.setPreferredUsername(json.getString("preferred_username"));
		u.setGivenName(json.getString("given_name"));
		u.setFamilyName(json.getString("family_name"));
		u.setEmail(json.getString("email"));
		return u;
	}

	public UserProfile() {
	}

	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPreferredUsername() {
		return preferredUsername;
	}

	public void setPreferredUsername(String preferredUsername) {
		this.preferredUsername = preferredUsername;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
