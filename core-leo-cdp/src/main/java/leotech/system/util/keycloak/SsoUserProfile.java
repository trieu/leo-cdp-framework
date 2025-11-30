package leotech.system.util.keycloak;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import rfx.core.util.StringUtil;

/**
 * UserProfile: the user profile from Keycloak callback
 * 
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public class SsoUserProfile {

	private String sub;
	private boolean emailVerified;
	private String name;
	private String preferredUsername;
	private String givenName;
	private String familyName;
	private String email;

	private Set<String> roles;


	public static SsoUserProfile fromJson(JsonObject json, JsonArray rolesObj) {
		SsoUserProfile u = new SsoUserProfile();
		u.setSub(json.getString("sub"));
		u.setEmailVerified(json.getBoolean("email_verified", false));
		u.setName(json.getString("name"));
		u.setPreferredUsername(json.getString("preferred_username"));
		u.setGivenName(json.getString("given_name"));
		u.setFamilyName(json.getString("family_name"));
		u.setEmail(json.getString("email"));

		Set<String> roles = new HashSet<String>();
		rolesObj.forEach(o -> {
			String r = StringUtil.safeString(o, "");
			if (!r.isBlank())
				roles.add(r);
		});
		u.setRoles(roles);
		return u;
	}

	public SsoUserProfile() {
	}

	public String getSub() {
		return StringUtil.safeString(sub);
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
		return StringUtil.safeString(name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPreferredUsername() {
		return StringUtil.safeString(preferredUsername);
	}

	public void setPreferredUsername(String preferredUsername) {
		this.preferredUsername = preferredUsername;
	}

	public String getGivenName() {
		return StringUtil.safeString(givenName);
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getFamilyName() {
		return StringUtil.safeString(familyName);
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getEmail() {
		return StringUtil.safeString(email);
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Set<String> getRoles() {
		return roles != null ? roles : new HashSet<String>(0);
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}


	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
