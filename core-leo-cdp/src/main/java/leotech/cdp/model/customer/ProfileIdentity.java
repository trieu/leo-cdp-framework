package leotech.cdp.model.customer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.arangodb.entity.Key;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.json.JsonObject;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.RefKey;
import leotech.cdp.model.social.ZaloUserDetail;
import leotech.cdp.utils.ProfileDataValidator;
import leotech.system.util.XssFilterUtil;
import rfx.core.util.StringUtil;

/**
 * 
 * ProfileIdentity is the data entity for Customer/Human Identity Resolution
 * 
 * @author Trieu Nguyen (Thomas)
 * @since 2022
 *
 */
public final class ProfileIdentity {
	
	private static final String PREFIX_JSON_ARRAY_STR = "[";
	static final Type HASHSET_TYPE = new TypeToken<HashSet<String>>(){}.getType();
	static final Type HASHMAP_TYPE = new TypeToken<Map<String, String>>(){}.getType();
	
	public static final String ID_PREFIX_PERSONA = "persona:";
	public static final String ID_PREFIX_FIRST_NAME = "fname:";
	public static final String ID_PREFIX_MIDDLE_NAME = "mname:";
	public static final String ID_PREFIX_LAST_NAME = "lname:";
	
	public static final String ID_PREFIX_CITIZEN = "citizen:";
	public static final String ID_PREFIX_USERNAME = "username:";
	public static final String ID_PREFIX_PHONE = "phone:";
	public static final String ID_PREFIX_EMAIL = "email:";
	
	public static final String ID_PREFIX_VISITOR = "visitor:";
	public static final String ID_PREFIX_FINGERPRINT = "fgp:";
	public static final String ID_PREFIX_APPLICATION = "app:";
	public static final String ID_PREFIX_WEB = "web:";
	
	public static final String ID_PREFIX_DEVICE = "device:";
	public static final String ID_PREFIX_DATABASE = "db:";
	public static final String ID_PREFIX_CRM = "crm:";
	public static final String ID_PREFIX_FINTECH = "fintech:";
	public static final String ID_PREFIX_LOYALTY = "loyalty:";
	
	
	public final static String buildIdPrefix(String provider, String id) {
		return provider + id;
	}

	@Key
	@Expose
	String id;

	@Expose
	String visitorId;

	@Expose
	String crmRefId;

	@Expose
	String primaryEmail;

	@Expose
	String primaryPhone;

	@Expose
	String personaUri;

	@Expose
	String primaryUsername;

	@Expose
	String firstName;

	@Expose
	String lastName;
	
	@Expose
	String fingerprintId;
	
	@Expose
	protected Map<String, String> socialMediaProfiles = new HashMap<>(10);
	
	@Expose
	protected Set<String> applicationIDs = new HashSet<>(10);
	
	@Expose
	protected Set<String> governmentIssuedIDs = new HashSet<>(10);
	
	@Expose
	protected Set<String> loyaltyIDs = new HashSet<>(10);
	
	@Expose
	protected Set<String> fintechSystemIDs = new HashSet<>(10);
	
	@Expose
	Set<RefKey> inSegments = new HashSet<RefKey>(100);
	
	/**
	 *  lets you automatically merge duplicate records 
	 */
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	String updateByKey = ""; // "": insert new, "primaryPhone": update using phone, "primaryEmail": update using email,..
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	boolean deduplicate = false;
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	boolean overwriteData = false;
	


	public ProfileIdentity() {
		// default
	}
	
	public ProfileIdentity(Profile p) {
		this.id = p.getId();
		this.visitorId = p.getVisitorId();
		this.crmRefId = p.getCrmRefId();
		this.primaryEmail = p.getPrimaryEmail();
		this.primaryPhone = p.getPrimaryPhone();
		this.personaUri = p.getPersonaUri();
		this.primaryUsername = p.getPrimaryUsername();
		this.firstName = p.getFirstName();
		this.lastName = p.getLastName();
		this.fingerprintId = p.getFingerprintId();
	}
	
	/**
	 * build ProfileIdentity from JsonObject to save profile from API
	 * 
	 * @param jsonObjData
	 */
	public ProfileIdentity(JsonObject jsonObjData) {
		
		// flag to check save mode
		this.updateByKey = jsonObjData.getString(HttpParamKey.UPDATE_BY_KEY, "");
		jsonObjData.remove(HttpParamKey.UPDATE_BY_KEY);
		
		this.deduplicate = jsonObjData.getBoolean(HttpParamKey.DEDUPLICATE, false);
		jsonObjData.remove(HttpParamKey.DEDUPLICATE);
		
		this.overwriteData = jsonObjData.getBoolean(HttpParamKey.OVERWRITE_DATA, false);
		jsonObjData.remove(HttpParamKey.OVERWRITE_DATA);
		
		this.primaryPhone = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.PRIMARY_PHONE);
		this.primaryEmail = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.PRIMARY_EMAIL);
		this.crmRefId = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.CRM_REF_ID);
		
		String socialProfilesStr = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.SOCIAL_MEDIA_PROFILES);
		setSocialMediaProfiles(socialProfilesStr);
		jsonObjData.remove(HttpParamKey.SOCIAL_MEDIA_PROFILES);
		
		String applicationIDsStr = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.APPLICATION_IDS);
		setApplicationIDs(applicationIDsStr);
		jsonObjData.remove(HttpParamKey.APPLICATION_IDS);
		
		String governmentIssuedIDsStr = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.GOVERNMENT_ISSUED_IDS);
		setGovernmentIssuedIDs(governmentIssuedIDsStr);
		jsonObjData.remove(HttpParamKey.GOVERNMENT_ISSUED_IDS);
		
		String loyaltyIDsStr = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.LOYALTY_IDS);
		setLoyaltyIDs(loyaltyIDsStr);
		jsonObjData.remove(HttpParamKey.LOYALTY_IDS);
		
		String fintechSystemIDsStr = XssFilterUtil.safeGet(jsonObjData, HttpParamKey.FINTECH_SYSTEM_IDS);
		setFintechSystemIDs(fintechSystemIDsStr);
		jsonObjData.remove(HttpParamKey.FINTECH_SYSTEM_IDS);
	}

	private void setSocialMediaProfiles(String socialProfilesStr) {
		if(StringUtil.isNotEmpty(socialProfilesStr)) {
			try {
				this.socialMediaProfiles.putAll(new Gson().fromJson(socialProfilesStr, HASHMAP_TYPE));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void setApplicationIDs(String applicationIDsStr) {
		if(StringUtil.isNotEmpty(applicationIDsStr)) {
			try {
				if(applicationIDsStr.startsWith(PREFIX_JSON_ARRAY_STR)) {
					this.applicationIDs.addAll(new Gson().fromJson(applicationIDsStr, HASHSET_TYPE));
				}
				else {
					String[]toks = applicationIDsStr.split(",");
					for (String tok : toks) {
						this.applicationIDs.add(tok);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void setGovernmentIssuedIDs(String governmentIssuedIDsStr) {
		if(StringUtil.isNotEmpty(governmentIssuedIDsStr)) {
			try {
				if(governmentIssuedIDsStr.startsWith(PREFIX_JSON_ARRAY_STR)) {
					this.governmentIssuedIDs.addAll(new Gson().fromJson(governmentIssuedIDsStr, HASHSET_TYPE));
				}
				else {
					String[]toks = governmentIssuedIDsStr.split(",");
					for (String tok : toks) {
						this.governmentIssuedIDs.add(tok);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void setLoyaltyIDs(String loyaltyIDsStr) {
		if(StringUtil.isNotEmpty(loyaltyIDsStr)) {
			try {
				if(loyaltyIDsStr.startsWith(PREFIX_JSON_ARRAY_STR)) {
					this.loyaltyIDs.addAll(new Gson().fromJson(loyaltyIDsStr, HASHSET_TYPE));
				}
				else {
					String[]toks = loyaltyIDsStr.split(",");
					for (String tok : toks) {
						this.loyaltyIDs.add(tok);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void setFintechSystemIDs(String fintechSystemIDsStr) {
		if(StringUtil.isNotEmpty(fintechSystemIDsStr)) {
			try {
				if(fintechSystemIDsStr.startsWith(PREFIX_JSON_ARRAY_STR)) {
					this.fintechSystemIDs.addAll(new Gson().fromJson(fintechSystemIDsStr, HASHSET_TYPE));
				}
				else {
					String[]toks = fintechSystemIDsStr.split(",");
					for (String tok : toks) {
						this.fintechSystemIDs.add(tok);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public ProfileIdentity(ZaloUserDetail u) {
		this.crmRefId = "zalo-" + u.getUserId();
		this.socialMediaProfiles.put("zalo", u.getUserId());
	}

	public ProfileIdentity(String crmRefId, String primaryEmail, String primaryPhone) {
		super();
		this.crmRefId = crmRefId;
		this.primaryEmail = primaryEmail;
		this.primaryPhone = primaryPhone;
	}

	public String getDocumentUUID() {
		return Profile.getDocumentUUID(this.id);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVisitorId() {
		return visitorId;
	}

	public void setVisitorId(String visitorId) {
		this.visitorId = visitorId;
	}

	public String getCrmRefId() {
		return crmRefId;
	}

	public void setCrmRefId(String crmRefId) {
		this.crmRefId = crmRefId;
	}

	public String getPrimaryEmail() {
		return primaryEmail;
	}

	public void setPrimaryEmail(String primaryEmail) {
		this.primaryEmail = primaryEmail;
	}

	public String getPrimaryPhone() {
		return primaryPhone;
	}

	public void setPrimaryPhone(String primaryPhone) {
		this.primaryPhone = primaryPhone;
	}

	public String getPersonaUri() {
		return personaUri;
	}

	public void setPersonaUri(String personaUri) {
		this.personaUri = personaUri;
	}

	public String getPrimaryUsername() {
		return primaryUsername;
	}

	public void setPrimaryUsername(String primaryUsername) {
		this.primaryUsername = primaryUsername;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFingerprintId() {
		return fingerprintId;
	}

	public void setFingerprintId(String fingerprintId) {
		this.fingerprintId = fingerprintId;
	}

	public Map<String, String> getSocialMediaProfiles() {
		return socialMediaProfiles;
	}

	public void setSocialMediaProfiles(Map<String, String> socialMediaProfiles) {
		this.socialMediaProfiles = socialMediaProfiles;
	}
	
	public void setSocialMediaId(String socialId) {
		if(StringUtil.isNotEmpty(socialId)) {
			String [] toks = socialId.split(":");
			if(toks.length == 2) {
				String socialPlatform = toks[0];
				String userId = toks[1];
		 		this.socialMediaProfiles.put(socialPlatform, userId);
			}
		}
	}

	public Set<String> getApplicationIDs() {
		return applicationIDs != null ? applicationIDs : new HashSet<String>(0);
	}
	
	public String getFirstApplicationID() {
		Iterator<String> iterator = applicationIDs.iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}

	public void setApplicationIDs(Set<String> applicationIDs) {
		this.applicationIDs = applicationIDs;
	}

	public Set<String> getGovernmentIssuedIDs() {
		return governmentIssuedIDs;
	}
	
	public String getFirstGovernmentIssuedID() {
		Iterator<String> iterator = governmentIssuedIDs.iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}

	public void setGovernmentIssuedIDs(Set<String> governmentIssuedIDs) {
		this.governmentIssuedIDs = governmentIssuedIDs;
	}

	public Set<String> getLoyaltyIDs() {
		return loyaltyIDs;
	}
	
	public String getFirstLoyaltyID() {
		Iterator<String> iterator = loyaltyIDs.iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}

	public void setLoyaltyIDs(Set<String> loyaltyIDs) {
		this.loyaltyIDs = loyaltyIDs;
	}

	public Set<String> getFintechSystemIDs() {
		return fintechSystemIDs;
	}
	
	public String getFirstFintechSystemID() {
		Iterator<String> iterator = fintechSystemIDs.iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}

	public void setFintechSystemIDs(Set<String> fintechSystemIDs) {
		this.fintechSystemIDs = fintechSystemIDs;
	}
	
	public boolean hasExternalIds() {
		return this.loyaltyIDs.size() > 0 || this.applicationIDs.size() > 0 || this.fintechSystemIDs.size() > 0 
				|| this.governmentIssuedIDs.size() > 0 || this.socialMediaProfiles.size() > 0;
	}

	public String getUpdateByKey() {
		return StringUtil.safeString(updateByKey);
	}

	public void setUpdateByKey(String updateByKey) {
		this.updateByKey = updateByKey;
	}

	public boolean isDeduplicate() {
		return deduplicate;
	}

	public void setDeduplicate(boolean deduplicate) {
		this.deduplicate = deduplicate;
	}
	
	public boolean shouldUpdateByEmail() {
		boolean isEmail =  ProfileDataValidator.isValidEmail(this.primaryEmail);
		return isEmail && HttpParamKey.PRIMARY_EMAIL.equalsIgnoreCase(this.updateByKey);
	}
	
	public boolean shouldUpdateByPhone() {
		return StringUtil.isNotEmpty(primaryPhone) && HttpParamKey.PRIMARY_PHONE.equalsIgnoreCase(this.updateByKey);
	}
	
	public boolean shouldUpdateByCrmRefId() {
		return StringUtil.isNotEmpty(crmRefId) && HttpParamKey.CRM_REF_ID.equalsIgnoreCase(this.updateByKey);
	}

	public boolean shouldUpdateByApplicationIDs() {
		boolean hasApplicationIDs = this.applicationIDs != null ? this.applicationIDs.size() > 0 : false;
		return hasApplicationIDs && HttpParamKey.APPLICATION_IDS.equalsIgnoreCase(this.updateByKey);
	}
	
	public boolean shouldUpdateByLoyaltyIDs() {
		boolean hasLoyaltyIDs = this.loyaltyIDs != null ? this.loyaltyIDs.size() > 0 : false;
		return hasLoyaltyIDs && HttpParamKey.LOYALTY_IDS.equalsIgnoreCase(this.updateByKey);
	}
	
	public boolean isOverwriteData() {
		return overwriteData;
	}

	public void setOverwriteData(boolean overwriteData) {
		this.overwriteData = overwriteData;
	}
	
	

	public Set<RefKey> getInSegments() {
		return inSegments;
	}

	public void setInSegments(Set<RefKey> inSegments) {
		this.inSegments = inSegments;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	public static void main(String[] args) {
		ProfileIdentity p = new ProfileIdentity();
		p.setApplicationIDs("[\"zaloid_by_app: 5866747742526567508\", \"zaloid_by_brand: 7443055895821075130\"]");
		System.out.println(p.getApplicationIDs());
	}
}
