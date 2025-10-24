package leotech.cdp.model.customer;

import java.util.Arrays;
import java.util.Date;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.system.model.DeviceInfo;
import leotech.system.util.DeviceInfoUtil;
import leotech.system.util.database.PersistentObject;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 *
 */
public final class Device extends PersistentObject {

	public static final String CDP_API = "CDP_API";
	public static final String BACKEND_SYSTEM = "Backend_System";
	

	public static final String COLLECTION_NAME = getCdpCollectionName(Device.class);

	static ArangoCollection instance;
	public static final Device CDP_API_DEVICE = new Device(CDP_API, BACKEND_SYSTEM);

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			instance = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields for fast lookup
			instance.ensurePersistentIndex(Arrays.asList("name"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("type"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("osName"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("osName","osVersion"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("browserName"),new PersistentIndexOptions().unique(false));
		}
		return instance;
	}

	@Key
	@Expose
	String id;

	@Expose
	String name;

	@Expose
	String type;

	@Expose
	String osName;

	@Expose
	String osVersion;

	@Expose
	String browserName;

	@Expose
	Date createdAt;
	
	@Expose
	Date updatedAt ;
	
	public Device() {
		// gson
	}

	public Device(String name, String type) {
		super();
		initApiDevice(name, type);
		this.createdAt = new Date();
		// create ID
		this.buildHashedId();
	}

	private void initApiDevice(String name, String type) {
		this.name = name;
		this.browserName = name;
		this.type = type;
		this.osName = "CDP System";
		this.osVersion = SystemMetaData.BUILD_VERSION;
	}


	public Device(DeviceInfo dv, Date createdAt) {
		setDeviceInfo(dv);
		this.createdAt = createdAt;
		// create ID
		this.buildHashedId();
	}

	public Device(DeviceInfo dv) {
		setDeviceInfo(dv);
		this.createdAt = new Date();
		// create ID
		this.buildHashedId();
	}

	private void setDeviceInfo(DeviceInfo dv) {
		this.name = dv.deviceName;
		this.type = dv.deviceType;
		this.osName = dv.deviceOs;
		this.osVersion = dv.deviceOsVersion;
		this.browserName = dv.deviceName;
	}
	
	public Device(String userAgent) {
		DeviceInfo dv = DeviceInfoUtil.getDeviceInfo(userAgent);
		if(dv.isEmpty() || dv.isUnknownDevice()) {
			initApiDevice(CDP_API, BACKEND_SYSTEM);
		}
		else {
			setDeviceInfo(dv);
		}
		this.createdAt = new Date();
		// create ID
		this.buildHashedId();
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if(this.name != null && this.type != null && this.osName != null && this.osVersion != null && this.browserName != null) {
			String keyHint = this.name + this.type + this.osName + this.osVersion + this.browserName;
			this.id = createId(this.id, keyHint);	
		}
		else {
			newIllegalArgumentException("name, type, osName, osVersion, browserName is required");
		}
		return this.id; 
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public String getBrowserName() {
		return browserName;
	}

	public void setBrowserName(String browserName) {
		this.browserName = browserName;
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(id);
	}

	@Override
	public Date getUpdatedAt() {
		return this.updatedAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
	@Override
	public int hashCode() {
		if(StringUtil.isNotEmpty(this.id)) {
			return this.id.hashCode();
		}
		return 0;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}
}