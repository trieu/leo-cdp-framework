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
 * Configuration for core system services
 * ArrangoDB collection: system_service
 *
 * @author Trieu Nguyen
 * @since 2024
 */
public class SystemService implements PersistentArangoObject {

    // =========================
    // Constants
    // =========================
    public static final String SERVICE_PROVIDER = "service_provider";
    public static final String SERVICE_API_URL = "service_api_url";
    public static final String SERVICE_API_KEY = "service_api_key";
    public static final String SERVICE_API_TOKEN = "service_api_token";

    public static final String COLLECTION_NAME = "system_service";

    // =========================
    // Static Fields
    // =========================
    private static volatile ArangoCollection instance;

    // =========================
    // Fields (Persisted)
    // =========================
    @Key
    @Expose
    protected String id;

    @Expose
    protected String name;

    @Expose
    protected String description;

    @Expose
    protected String dagId = "";

    @Expose
    protected int index = 0;

    @Expose
    protected Date createdAt = new Date();

    @Expose
    protected Date updatedAt = new Date();

    // Mutable / runtime fields
    @Expose
    protected int status = 0; // 0 = enabled, 1 = ready, -1 = disabled

    @Expose
    protected Map<String, Object> configs = new HashMap<>();

    @Expose
    protected Map<String, AttributeMetaData> coreFieldConfigs = new HashMap<>();

    @Expose
    protected Map<String, AttributeMetaData> extFieldConfigs = new HashMap<>();

    // =========================
    // Constructors
    // =========================
    public SystemService() {
        this("", "", new HashMap<>());
    }

    public SystemService(String id, String name, Map<String, Object> configs) {
        this.id = StringUtil.safeString(id);
        this.name = StringUtil.safeString(name);
        setConfigs(configs);
        validate();
    }

    public SystemService(String id,
                         String name,
                         Map<String, AttributeMetaData> coreFieldConfigs,
                         Map<String, AttributeMetaData> extFieldConfigs) {

        this.id = StringUtil.safeString(id);
        this.name = StringUtil.safeString(name);

        setCoreFieldConfigs(coreFieldConfigs);
        setExtFieldConfigs(extFieldConfigs);

        validate();
    }

    // =========================
    // Persistence
    // =========================
    @Override
    public ArangoCollection getDbCollection() {
        if (instance == null) {
            synchronized (SystemService.class) {
                if (instance == null) {
                    ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
                    instance = db.collection(COLLECTION_NAME);
                }
            }
        }
        return instance;
    }

    @Override
    public boolean dataValidation() {
        return StringUtil.isNotEmpty(id)
                && StringUtil.isNotEmpty(name)
                && configs != null;
    }

    private void validate() {
        if (!dataValidation()) {
            throw new IllegalArgumentException("Invalid SystemService: id, name, configs must be provided");
        }
    }

    // =========================
    // Business Logic
    // =========================
    public Map<String, Object> buildConfParamsAirflowDagForSegment(
            String segmentId,
            Map<String, String> accessTokens) {

        Map<String, Object> params = new HashMap<>(this.configs);

        params.put("segmentid", segmentId);
        params.put("tokenkey", EventObserver.DEFAULT_ACCESS_KEY);
        params.put("tokenvalue",
                accessTokens != null
                        ? accessTokens.getOrDefault(EventObserver.DEFAULT_ACCESS_KEY, "")
                        : "");

        params.put("service_id", this.id);
        params.put("cdp_hostname", SystemMetaData.DOMAIN_CDP_ADMIN);

        return params;
    }

    public boolean isReadyToRun() {
        return configs != null && !configs.isEmpty();
    }

    // =========================
    // Getters / Setters
    // =========================
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = StringUtil.safeString(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = StringUtil.safeString(name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = StringUtil.safeString(description);
    }

    public String getDagId() {
        return StringUtil.safeString(dagId);
    }

    public void setDagId(String dagId) {
        this.dagId = StringUtil.safeString(dagId);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Date getCreatedAt() {
        return new Date(createdAt.getTime());
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt != null ? new Date(createdAt.getTime()) : new Date();
    }

    public Date getUpdatedAt() {
        return new Date(updatedAt.getTime());
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt != null ? new Date(updatedAt.getTime()) : new Date();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, Object> getConfigs() {
        return new HashMap<>(configs);
    }

    public void setConfigs(Map<String, Object> configs) {
        this.configs = (configs != null) ? new HashMap<>(configs) : new HashMap<>();
    }

    public Map<String, AttributeMetaData> getCoreFieldConfigs() {
        return new HashMap<>(coreFieldConfigs);
    }

    public void setCoreFieldConfigs(Map<String, AttributeMetaData> coreFieldConfigs) {
        if (coreFieldConfigs != null) {
            this.coreFieldConfigs = new HashMap<>(coreFieldConfigs);
        }
    }

    public Map<String, AttributeMetaData> getExtFieldConfigs() {
        return new HashMap<>(extFieldConfigs);
    }

    public void setExtFieldConfigs(Map<String, AttributeMetaData> extFieldConfigs) {
        if (extFieldConfigs != null) {
            this.extFieldConfigs = new HashMap<>(extFieldConfigs);
        }
    }

    // =========================
    // Object Overrides
    // =========================
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SystemService)) return false;
        SystemService other = (SystemService) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}