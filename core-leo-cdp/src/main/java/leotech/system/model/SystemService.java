package leotech.system.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.journey.EventObserver;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.database.PersistentArangoObject;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * Configuration for core system services <br>
 * ArangoDB collection: system_service
 * @author tantrieuf31
 * 
 * @since 2021
 */
public class SystemService implements PersistentArangoObject {

	public static final String COLLECTION_NAME = "system_service";

	// Status Constants
	public static final int STATUS_ENABLED = 0;
	public static final int STATUS_READY = 1;
	public static final int STATUS_DISABLED = -1;

	// Config Keys
	public static final String SERVICE_PROVIDER = "service_provider";
	public static final String SERVICE_API_URL = "service_api_url";
	public static final String SERVICE_API_KEY = "service_api_key";
	public static final String SERVICE_API_TOKEN = "service_api_token";

	private static volatile ArangoCollection collectionInstance;

	@Key
	@Expose
	protected String id = "";

	@Expose
	protected String name = "";

	@Expose
	protected String description = "";

	@Expose
	protected String dagId = ""; // DAG (Directed Acyclic Graph) ID in Airflow

	@Expose
	protected int index = 0;

	@Expose
	protected Date createdAt = new Date();

	@Expose
	protected Date updatedAt = new Date();

	@Expose
	protected int status = STATUS_ENABLED;

	@Expose
	protected Map<String, Object> configs = new HashMap<>();

	@Expose
	protected Map<String, AttributeMetaData> coreFieldConfigs = new HashMap<>();

	@Expose
	protected Map<String, AttributeMetaData> extFieldConfigs = new HashMap<>();

	// --- Constructors ---

	public SystemService() {
	}

	public SystemService(String id, String name, Map<String, Object> configs) {
		this.id = id;
		this.name = name;
		if (configs == null) {
			throw new IllegalArgumentException("configs must not be null");
		}
		this.configs = configs;
	}

	public SystemService(String id, String name, Map<String, AttributeMetaData> coreFieldConfigs,
			Map<String, AttributeMetaData> extFieldConfigs) {
		this.id = id;
		this.name = name;
		setCoreFieldConfigs(coreFieldConfigs);
		setExtFieldConfigs(extFieldConfigs);
	}

	// --- Persistence Logic ---

	@Override
	public ArangoCollection getDbCollection() {
		if (collectionInstance == null) {
			synchronized (SystemService.class) {
				if (collectionInstance == null) {
					ArangoDatabase arangoDatabase = ArangoDbUtil.getCdpDatabase();
					collectionInstance = arangoDatabase.collection(COLLECTION_NAME);
				}
			}
		}
		return collectionInstance;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(id) && StringUtil.isNotEmpty(name) && configs != null;
	}

	// --- Business Logic ---

	/**
	 * Build parameters for Airflow DAG based on segment and access tokens
	 */
	public Map<String, Object> buildConfParamsAirflowDagForSegment(String segmentId, Map<String, String> accessTokens) {
		Map<String, Object> params = new HashMap<>(this.configs);
		params.put("segmentid", segmentId);
		params.put("tokenkey", EventObserver.DEFAULT_ACCESS_KEY);
		params.put("tokenvalue",
				accessTokens != null ? accessTokens.getOrDefault(EventObserver.DEFAULT_ACCESS_KEY, "") : "");
		params.put("service_id", this.id);
		params.put("cdp_hostname", SystemMetaData.DOMAIN_CDP_ADMIN);
		return params;
	}

	public boolean isReadyToRun() {
		return this.configs != null && !this.configs.isEmpty();
	}

	// --- Getters and Setters ---

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, Object> getConfigs() {
		return configs;
	}

	public void setConfigs(Map<String, Object> configs) {
		if (configs != null) {
			this.configs = configs;
		}
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Map<String, AttributeMetaData> getCoreFieldConfigs() {
		return coreFieldConfigs;
	}

	public void setCoreFieldConfigs(Map<String, AttributeMetaData> coreFieldConfigs) {
		if (coreFieldConfigs != null) {
			this.coreFieldConfigs.clear();
			this.coreFieldConfigs.putAll(coreFieldConfigs);
		}
	}

	public Map<String, AttributeMetaData> getExtFieldConfigs() {
		return extFieldConfigs;
	}

	public void setExtFieldConfigs(Map<String, AttributeMetaData> extFieldConfigs) {
		if (extFieldConfigs != null) {
			this.extFieldConfigs.clear();
			this.extFieldConfigs.putAll(extFieldConfigs);
		}
	}

	public String getDagId() {
		return StringUtil.safeString(dagId);
	}

	public void setDagId(String dagId) {
		this.dagId = dagId;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	// --- Overrides ---

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SystemService that = (SystemService) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		// Using a fresh Gson instance for debugging; in production, consider a static
		// constant for performance
		return new Gson().toJson(this);
	}
}