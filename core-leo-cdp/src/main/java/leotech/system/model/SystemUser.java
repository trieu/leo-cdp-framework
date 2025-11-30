package leotech.system.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.activation.Agent;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.AssetItem;
import leotech.cdp.model.customer.AbstractProfile;
import leotech.cdp.model.customer.BusinessAccount;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.utils.ProfileDataValidator;
import leotech.system.util.EncryptorAES;
import leotech.system.util.PasswordGenerator;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.database.PersistentArangoObject;
import leotech.system.util.keycloak.KeycloakConfig;
import leotech.system.util.keycloak.SsoUserProfile;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * the data entity to store system user model for system administration
 * 
 * @author tantrieuf31
 * @since 2019
 */
public final class SystemUser implements PersistentArangoObject {
	
	private static ArangoCollection instance;
	
	public static final String SUPER_ADMIN_LOGIN = "superadmin";
	public static final String SUPER_ADMIN_NAME = "Super Admin";
	public static final String COLLECTION_NAME = "system_user";
	
	
	public static final int STATUS_PENDING = 0;
	public static final int STATUS_ACTIVE = 1;
	public static final int STATUS_DISABLED = 2;
	public static final int STATUS_EXPIRED = 3;
	
	private static final String SSO_PREFIX = "sso_";
    private static final int PASSWORD_LEN = 16;

    // Map SSO role → Internal SystemUserRole 
    private static final Map<String, Integer> ROLE_MAPPING = Map.of(
        "LEOCDP_SUPER_SYSTEM_ADMIN", SystemUserRole.SUPER_SYSTEM_ADMIN,
        "LEOCDP_DATA_ADMIN", SystemUserRole.DATA_ADMIN,
        "LEOCDP_DATA_OPERATOR", SystemUserRole.DATA_OPERATOR,
        "LEOCDP_CUSTOMER_DATA_EDITOR", SystemUserRole.CUSTOMER_DATA_EDITOR,
        "LEOCDP_REPORT_VIEWER", SystemUserRole.REPORT_VIEWER
    );

	@Key
	private String key;

	@Expose
	private String userLogin;
	
	@Expose
	private String profileVisitorId = StringPool.BLANK;
	
	@Expose
	private String displayName;
	
	@Expose
	private int role = SystemUserRole.STANDARD_USER;

	private String userEmail;
	
	private int status = STATUS_PENDING;

	private String avatarUrl = "";

	private long creationTime;

	private long modificationTime;

	private long registeredTime = 0;

	private String businessUnit = ""; // Company
	
	private Set<String> inGroups = new HashSet<String>(); // Departments

	private boolean isOnline;

	private long networkId;

	private Map<String, String> customData = new HashMap<>();
	
	/**
	 * A list of access profile fields: to control access to fields of a profile
	 */
	private List<String> accessProfileFields = new ArrayList<String>();
	
	// high sensitive data
	private transient String encryptionKey;
	
	// hashed password
	private String userPass;
	
	// key to activate the login account and set new password
	private String activationKey;
	
	private String ssoSource = "";
	
	@Expose
	private List<Notification> notifications = new ArrayList<>();
	
	@Expose
	private List<String> viewableJourneyMapIds = new ArrayList<>();
	
	@Expose
	private List<String> editableJourneyMapIds = new ArrayList<>();
	
	public SystemUser() {

	}
	
	/**
	 * to create virtual temporary user with the role as DATA_ADMIN, from Event Observer for CDP API 
	 * 
	 * @param observer
	 */
	public SystemUser(EventObserver observer) {
		this.userLogin = "EventObserver-"+observer.getId();
		this.displayName = observer.getName();
		setStatus(STATUS_ACTIVE);
		setRole(SystemUserRole.DATA_ADMIN);
	}


    /**
     * constructor for local update
     * 
     * @param ssoUser
     */
	public SystemUser(String userLogin, String userPass, String displayName, String userEmail, long networkId) {
		super();
		this.userLogin = userLogin;
		this.userPass = EncryptorAES.passwordHash(userLogin, userPass);
		this.displayName = displayName;
		this.userEmail = userEmail;
		this.networkId = networkId;
	}
	
    /**
     * constructor for SSO login
     * 
     * @param ssoUser
     */
    public SystemUser(SsoUserProfile ssoUser, KeycloakConfig config) {
    	this.status = STATUS_ACTIVE;
        this.userEmail = ssoUser.getEmail();
        this.userLogin = SSO_PREFIX + userEmail;
        
        String username = ProfileDataValidator.extractUsernameFromEmail(userEmail);
        this.displayName = ssoUser.getName().length() > 1 ? ssoUser.getName() : username;
        
        // SSO source
        this.networkId = config.getHashedId();
        this.ssoSource = config.ssoSource();

        // set Authorization
        Set<String> ssoUserRoles = ssoUser.getRoles();
        // Pick the first matching role from stream, fallback to DATA_OPERATOR
        int finalRole =
            ssoUserRoles.stream()
                .map(ROLE_MAPPING::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(SystemUserRole.DATA_OPERATOR);
        this.setRole(finalRole);
        
        // set random pass
        this.userPass = EncryptorAES.passwordHash(userLogin, PasswordGenerator.generate(PASSWORD_LEN));
    }

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			ArangoDatabase arangoDatabase = ArangoDbUtil.getCdpDatabase();

			instance = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields
			instance.ensurePersistentIndex(Arrays.asList("userEmail"), new PersistentIndexOptions().unique(true));
			instance.ensurePersistentIndex(Arrays.asList("userLogin"), new PersistentIndexOptions().unique(true));
			instance.ensurePersistentIndex(Arrays.asList("networkId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("customData[*]"), new PersistentIndexOptions().unique(false));
		}
		return instance;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(userEmail) && StringUtil.isNotEmpty(userLogin)
				&& StringUtil.isNotEmpty(userPass) && StringUtil.isNotEmpty(this.displayName);
	}

	public String getUserLogin() {
		return userLogin;
	}

	public void setUserLogin(String userLogin) {
		this.userLogin = userLogin;
	}

	public String getUserPass() {
		return userPass;
	}

	public void setUserPass(String userPass) {
		this.userPass = EncryptorAES.passwordHash(userLogin, userPass);
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public long getRegisteredTime() {
		return registeredTime;
	}

	public void setRegisteredTime(long registeredTime) {
		this.registeredTime = registeredTime;
	}

	public String getKey() {
		return key;
	}
	
	public String getId() {
		return key;
	}

	public String getProfileVisitorId() {
		profileVisitorId = StringUtil.safeString(profileVisitorId);
		String[] toks = profileVisitorId.split(" ");
		if(toks.length > 0) {
			return toks[0];
		}
		return profileVisitorId;
	}

	public void setProfileVisitorId(String profileVisitorId) {
		this.profileVisitorId = profileVisitorId;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public long getModificationTime() {
		return modificationTime;
	}

	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}

	public String getActivationKey() {
		return activationKey;
	}

	public void setActivationKey(String activationKey) {
		this.activationKey = activationKey;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getRole() {
		return role;
	}

	public void setRole(int role) {
		this.role = role;
	}
	
	public String getBusinessUnit() {
		return businessUnit;
	}

	public void setBusinessUnit(String businessUnit) {
		this.businessUnit = businessUnit;
	}

	public boolean isOnline() {
		return isOnline;
	}
	
	
	public String getSsoSource() {
		return ssoSource;
	}

	public void setSsoSource(String ssoSource) {
		this.ssoSource = ssoSource;
	}

	public boolean hasAdminRole() {
		return this.role == SystemUserRole.SUPER_SYSTEM_ADMIN || this.role == SystemUserRole.DATA_ADMIN;
	}
	
	public boolean hasSuperAdminRole() {
		System.out.println("role " + role + " userEmail " + this.userEmail);
		return this.role == SystemUserRole.SUPER_SYSTEM_ADMIN;
	}
	
	public boolean hasOperationRole() {
		return this.role == SystemUserRole.DATA_ADMIN || this.role == SystemUserRole.SUPER_SYSTEM_ADMIN 
				|| this.role == SystemUserRole.CUSTOMER_DATA_EDITOR || this.role == SystemUserRole.DATA_OPERATOR;
	}

	public void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
	}

	public Set<String> getInGroups() {
		return inGroups;
	}

	public void setInGroups(Set<String> inGroups) {
		if(inGroups != null) {
			this.inGroups = inGroups;
		}
	}
	
	public void setInGroup(String inGroup) {
		if(inGroup != null) {
			this.inGroups.add(inGroup);
		}
	}

	public Map<String, String> getCustomData() {
		return customData;
	}

	public void setCustomData(Map<String, String> customData) {
		this.customData = customData;
	}

	public void addCustomData(String key, String value) {
		this.customData.put(key, value);
	}

	public List<String> getAccessProfileFields() {
		return accessProfileFields;
	}

	public void setAccessProfileFields(List<String> accessProfileFields) {
		if(accessProfileFields != null) {
			this.accessProfileFields.addAll(accessProfileFields);
		}
	}

	public long getNetworkId() {
		return networkId;
	}

	public void setNetworkId(long networkId) {
		this.networkId = networkId;
	}

	public String getEncryptionKey() {
		if (encryptionKey == null) {
			encryptionKey = "";
		}
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}
	
	public boolean canViewSurveyReport() {
		return this.role >= SystemUserRole.STANDARD_USER;
	}
	
	/**
	 * @param clazz
	 * @return
	 */
	public boolean canInsertData(Class<?> clazz) {
		if(this.role == SystemUserRole.SUPER_SYSTEM_ADMIN || this.role == SystemUserRole.DATA_ADMIN) {
			return true;
		}
		else if(this.role == SystemUserRole.DATA_OPERATOR || this.role == SystemUserRole.CUSTOMER_DATA_EDITOR ) {
			return (clazz.equals(Segment.class) || clazz.equals(Profile.class) || clazz.equals(AssetGroup.class) 
					|| clazz.equals(AssetItem.class) || clazz.equals(Agent.class) );
		}
		else if(this.role == SystemUserRole.STANDARD_USER ) {
			return ( clazz.equals(AssetGroup.class) || clazz.equals(AssetItem.class));
		}
		else if(this.role == SystemUserRole.REPORT_VIEWER || this.role == SystemUserRole.CUSTOMER_DATA_VIEWER) {
			return (clazz.equals(Segment.class) || clazz.equals(Profile.class));
		}
		return false;
	}
	
	/**
	 * @param clazz
	 * @return
	 */
	public boolean canEditData(Class<?> clazz) {
		if(this.role == SystemUserRole.SUPER_SYSTEM_ADMIN || this.role == SystemUserRole.DATA_ADMIN) {
			return true;
		}
		else if(this.role == SystemUserRole.DATA_OPERATOR || this.role == SystemUserRole.CUSTOMER_DATA_EDITOR ) {
			return (clazz.equals(Segment.class) || clazz.equals(Profile.class) || clazz.equals(AssetGroup.class) || clazz.equals(AssetItem.class) || clazz.equals(Agent.class) );
		}
		else if(this.role == SystemUserRole.STANDARD_USER ) {
			return ( clazz.equals(AssetGroup.class) || clazz.equals(AssetItem.class));
		}
		else if(this.role == SystemUserRole.REPORT_VIEWER || this.role == SystemUserRole.CUSTOMER_DATA_VIEWER) {
			return (clazz.equals(Segment.class) || clazz.equals(Profile.class));
		}
		return false;
	}
	
	/**
	 * @param clazz
	 * @return
	 */
	public boolean canDeleteData(Class<?> clazz) {
		if(this.role == SystemUserRole.SUPER_SYSTEM_ADMIN || this.role == SystemUserRole.DATA_ADMIN) {
			return true;
		}
		else if(this.role == SystemUserRole.DATA_OPERATOR ) {
			return clazz.equals(Segment.class) || clazz.equals(AssetGroup.class) || clazz.equals(AssetItem.class);
		}
		return false;
	}
	
	/**
	 * @param profile
	 * @return
	 */
	public boolean checkToEditProfile(AbstractProfile profile) {
		boolean check = false;
		if(profile != null) {
			Set<String> authorizedEditors = profile.getAuthorizedEditors();
			check = this.hasAdminRole() || ( StringUtil.isNotEmpty(this.userLogin) && authorizedEditors.contains(this.userLogin));
		}
		return check;
	}
	

	/**
	 * @param segment
	 * @return
	 */
	public boolean checkToEditAccount(BusinessAccount account) {
		if(account != null) {
			return this.hasAdminRole() || account.getAuthorizedEditors().contains(this.userLogin);
		}
		return false;
	}

	public final void removeSensitiveData() {
		this.setUserPass(null);
	}


	public List<Notification> getNotifications() {
		return notifications;
	}
	
	public boolean shouldCheckNotifications() {
		return this.notifications.size() > 0;
	}
	


	public void setNotifications(List<Notification> notifications) {
		this.notifications = notifications;
	}
	
	public void setNotification(Notification notification) {
		if( ! notification.notifyPercentage() ) {
			this.notifications.add(notification);
		}
	}


	public List<String> getViewableJourneyMapIds() {
		return viewableJourneyMapIds;
	}

	public void setViewableJourneyMapIds(List<String> viewableJourneyMapIds) {
		this.viewableJourneyMapIds = viewableJourneyMapIds;
	}

	public List<String> getEditableJourneyMapIds() {
		return editableJourneyMapIds;
	}

	public void setEditableJourneyMapIds(List<String> editableJourneyMapIds) {
		this.editableJourneyMapIds = editableJourneyMapIds;
	}

	public void clearAllNotifications() {
		this.notifications.clear();
	}

	@Override
	public String toString() {
		return this.getUserLogin() + " " + this.getKey();
	}
}
