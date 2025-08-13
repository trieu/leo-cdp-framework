package leotech.system.model;

import java.util.Arrays;
import java.util.Date;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.system.util.IdGenerator;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.database.PersistentArangoObject;
import rfx.core.util.StringUtil;

/**
 * System Tracked Event, to monitor data scheduled jobs and admin system
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public final class SystemEvent implements PersistentArangoObject {
	
	public static final String COLLECTION_NAME = "system_event";
	static ArangoCollection instance;
	
	@Key
	@Expose
	String id;
	
	@Expose
	String loginUsername;
	
	@Expose
	Date createdAt;
	
	@Expose
	String objectName;
	
	@Expose
	String objectId;
	
	@Expose
	String action;
	
	@Expose
	String data;
	
	@Expose
	String accessIp;
	
	@Expose
	String userAgent;

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			ArangoDatabase arangoDatabase = ArangoDbUtil.getCdpDatabase();
			instance = arangoDatabase.collection(COLLECTION_NAME);
			// ensure indexing key fields
			instance.ensurePersistentIndex(Arrays.asList("createdAt", "userLogin"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("createdAt", "userLogin", "objectName"), new PersistentIndexOptions().unique(false));		
			instance.ensurePersistentIndex(Arrays.asList("createdAt", "userLogin", "objectName", "objectId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("createdAt", "objectName", "objectId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("createdAt", "userLogin", "objectName", "objectId", "action"), new PersistentIndexOptions().unique(true));
		}
		return instance;
	}

	@Override
	public boolean dataValidation() {
		return this.createdAt != null && StringUtil.isNotEmpty(objectName) && StringUtil.isNotEmpty(action) && StringUtil.isNotEmpty(loginUsername);
	}
	
	public String buildHashedId() throws IllegalArgumentException {
		if(StringUtil.isEmpty(this.id) && this.createdAt != null) {
			String keyHint = this.createdAt.getTime() + this.loginUsername + this.objectName + this.objectId + this.action + this.accessIp + this.userAgent + this.data;
			this.id = IdGenerator.createHashedId(keyHint);
		}
		return this.id;
	}
	
	public String getId() {
		return id;
	}
	
	public SystemEvent() {
		// for Gson
	}

	public SystemEvent(String userLogin, String objectName, String action) {
		super();
		this.loginUsername = userLogin;
		this.objectName = objectName;
		this.action = action;
		this.data = "";
		this.createdAt = new Date();
		this.buildHashedId();
	}
	
	public SystemEvent(String userLogin, Class<?> clazz, String action) {
		super();
		this.loginUsername = userLogin;
		this.objectName = clazz.getSimpleName();
		this.action = action;
		this.data = "";
		this.createdAt = new Date();
		this.buildHashedId();
	}
	
	public SystemEvent(String userLogin, String objectName, String action, String data) {
		super();
		this.loginUsername = userLogin;
		this.objectName = objectName;
		this.objectId = "";
		this.action = action;
		this.data = data;
		this.createdAt = new Date();
		this.buildHashedId();
	}

	public SystemEvent(String userLogin, String objectName, String objectId, String action, String data) {
		super();
		this.loginUsername = userLogin;		
		this.objectName = objectName;
		this.objectId = objectId;
		this.action = action;
		this.data = data;
		this.createdAt = new Date();
		this.buildHashedId();
	}
	
	public SystemEvent(String userLogin, Class<?> clazz, String objectId, String action, String data) {
		super();
		this.loginUsername = userLogin;		
		this.objectName = clazz.getSimpleName();
		this.objectId = objectId;
		this.action = action;
		this.data = data;
		this.createdAt = new Date();
		this.buildHashedId();
	}

	public SystemEvent(String userLogin, String objectName, String objectId, String action, String data, String accessIp, String userAgent) {
		super();
		this.loginUsername = userLogin;
		this.objectName = objectName;
		this.objectId = objectId;
		this.action = action;
		this.data = data;
		this.accessIp = accessIp;
		this.userAgent = userAgent;
		this.createdAt = new Date();
		this.buildHashedId();
	}



	public String getLoginUsername() {
		return loginUsername;
	}

	public void setLoginUsername(String loginUsername) {
		this.loginUsername = loginUsername;
	}

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getAccessIp() {
		return accessIp;
	}

	public void setAccessIp(String accessIp) {
		this.accessIp = accessIp;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
