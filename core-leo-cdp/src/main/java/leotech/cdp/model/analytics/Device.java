package leotech.cdp.model.analytics;

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
 * Represents a physical or logical device used by a Profile/Visitor in analytics.
 * 
 * ArangoDB collection: cdp_device
 * 
 * @author tantrieuf31
 */
public final class Device extends PersistentObject {

	public static final String CDP_API = "CDP_API";
	public static final String BACKEND_SYSTEM = "Backend_System";

	public static final String COLLECTION_NAME = getCdpCollectionName(Device.class);

	// FIX: Thread-safe lazy initialization
	private static volatile ArangoCollection instance;
	
	public static final Device CDP_API_DEVICE = new Device(CDP_API, BACKEND_SYSTEM);

	@Key
	@Expose
	private String id;

	@Expose
	private String name;

	@Expose
	private String type;

	@Expose
	private String osName;

	@Expose
	private String osVersion;

	@Expose
	private String browserName;

	@Expose
	private Date createdAt;
	
	@Expose
	private Date updatedAt;
	
	public Device() {
		// Default constructor for Gson serialization
	}

	public Device(String name, String type) {
		initApiDevice(name, type);
		initTimestampsAndId(new Date());
	}

	public Device(DeviceInfo dv) {
		this(dv, new Date());
	}

	public Device(DeviceInfo dv, Date createdAt) {
		setDeviceInfo(dv);
		initTimestampsAndId(createdAt);
	}

	public Device(String userAgent) {
		DeviceInfo dv = DeviceInfoUtil.getDeviceInfo(userAgent);
		if (dv == null || dv.isEmpty() || dv.isUnknownDevice()) {
			initApiDevice(CDP_API, BACKEND_SYSTEM);
		} else {
			setDeviceInfo(dv);
		}
		initTimestampsAndId(new Date());
	}

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			// FIX: Double-checked locking to prevent simultaneous index creation
			synchronized (Device.class) {
				if (instance == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);
					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);

					// --------------------------------------------------------------------------------
					// ARANGODB 3.11 INDEX OPTIMIZATION (RocksDB Engine)
					// In Analytics Reporting, marketers usually drill down via: Type -> OS -> Browser.
					// Previously, there were 5 separate redundant indices. 
					// We replaced them with high-efficiency composite indices that perfectly cover 
					// all UI filters using ArangoDB's left-to-right index utilization architecture.
					// --------------------------------------------------------------------------------
					
					// Covers queries by just "type", or "type + osName", or "type + osName + browserName"
					col.ensurePersistentIndex(Arrays.asList("type", "osName", "browserName"), pIdxOpts);
					
					// Covers queries by just "osName", or "osName + osVersion"
					col.ensurePersistentIndex(Arrays.asList("osName", "osVersion", "browserName"), pIdxOpts);
					
					// Covers queries strictly filtering by "browserName" first
					col.ensurePersistentIndex(Arrays.asList("browserName", "type"), pIdxOpts);
					
					// Covers exact full-name matches
					col.ensurePersistentIndex(Arrays.asList("name"), pIdxOpts);

					instance = col;
				}
			}
		}
		return instance;
	}

	/**
	 * Consolidates the initialization of timestamps and ID generation
	 */
	private void initTimestampsAndId(Date timestamp) {
		this.createdAt = timestamp;
		this.updatedAt = timestamp; // Better consistency to initialize updatedAt on creation
		this.buildHashedId();
	}

	private void initApiDevice(String name, String type) {
		this.name = name;
		this.browserName = name;
		this.type = type;
		this.osName = "CDP System";
		this.osVersion = SystemMetaData.BUILD_VERSION;
	}

	private void setDeviceInfo(DeviceInfo dv) {
		this.name = dv.deviceName;
		this.type = dv.deviceType;
		this.osName = dv.deviceOs;
		this.osVersion = dv.deviceOsVersion;
		this.browserName = dv.deviceName; // Originally mapped to deviceName, preserving logic
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		// FIX: Replaced raw null checks with safe StringUtil checks
		if (StringUtil.isNotEmpty(this.name) && StringUtil.isNotEmpty(this.type) 
				&& StringUtil.isNotEmpty(this.osName) && StringUtil.isNotEmpty(this.osVersion) 
				&& StringUtil.isNotEmpty(this.browserName)) {
			
			String keyHint = this.name + this.type + this.osName + this.osVersion + this.browserName;
			this.id = createId(this.id, keyHint);
			return this.id;
		} else {
			// FIX: Throws standard Java exception rather than relying on assumed superclass method
			throw new IllegalArgumentException("name, type, osName, osVersion, and browserName are required to generate a Device ID.");
		}
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(id);
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	public String getId() { return id; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getType() { return type; }
	public void setType(String type) { this.type = type; }

	public String getOsName() { return osName; }
	public void setOsName(String osName) { this.osName = osName; }

	public String getOsVersion() { return osVersion; }
	public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

	public String getBrowserName() { return browserName; }
	public void setBrowserName(String browserName) { this.browserName = browserName; }

	@Override
	public Date getCreatedAt() { return createdAt; }
	@Override
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

	@Override
	public Date getUpdatedAt() { return this.updatedAt; }
	@Override
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
	@Override
	public int hashCode() {
		return StringUtil.isNotEmpty(this.id) ? this.id.hashCode() : 0;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}