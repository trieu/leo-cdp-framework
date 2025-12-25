package leotech.cdp.model.activation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.system.model.SystemUser;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * ActivationRule
 *
 * Pure domain model. No scheduler, no Quartz, no side effects.
 */
public class ActivationRule extends PersistentObject implements Comparable<ActivationRule>, Serializable {



	private static final long serialVersionUID = 1825726230556120351L;

	public static final String SCHEDULING_TASK = "scheduling_task";
	public static final String EVENT_TRIGGER_TASK = "event_trigger_task";

	public static final String COLLECTION_NAME = getCdpCollectionName(ActivationRule.class);

	private static final Gson GSON = new Gson();
	static ArangoCollection dbCollection;

	// =========================
	// Persistent Fields
	// =========================
	@Key
	@Expose
	private String id;
	@Expose
	private Date createdAt;
	@Expose
	private Date updatedAt;

	@Expose
	private String name;
	@Expose
	private String activationType;
	@Expose
	private String description = "";

	@Expose
	private int priority = 1;
	@Expose
	private boolean active = true;

	@Expose
	private String condition = "true";
	@Expose
	private List<String> actions = new ArrayList<>();

	@Expose
	private String purpose = "";

	@Expose
	private String agentId = "";
	@Expose
	private String segmentId = "";

	@Expose
	int assetType = -1;

	@Expose
	String assetTemplateId = "";

	@Expose
	String assetTemplateName = "";

	@Expose
	String agentName = "";

	/**
	 * schedulingTime: < 0 : manual / one-time = 0 : polling loop > 0 : fixed-rate
	 * schedule
	 */
	@Expose
	private int schedulingTime = 0;

	/**
	 * timeUnit: 0 = seconds 1 = minutes 2 = hours (DEFAULT) 4 = days 5 = weeks
	 */
	@Expose
	private int timeUnit = TimeUnitCode.HOUR;

	@Expose
	private String timeToStart = "00:00";
	@Expose
	private String triggerEventName = "";

	@Expose
	private String ownerUsername = SystemUser.SUPER_ADMIN_LOGIN;

	// =========================
	// Persistence
	// =========================
	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			synchronized (ActivationRule.class) {
				if (dbCollection == null) {
					ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
					dbCollection = db.collection(COLLECTION_NAME);

					dbCollection.ensurePersistentIndex(List.of("agentId"), new PersistentIndexOptions().unique(false));

					dbCollection.ensurePersistentIndex(List.of("segmentId"),
							new PersistentIndexOptions().unique(false));
				}
			}
		}
		return dbCollection;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(name) && StringUtil.isNotEmpty(activationType)
				&& StringUtil.isNotEmpty(ownerUsername);
	}

	@Override
	public String buildHashedId() {
		if (!dataValidation()) {
			throw new IllegalArgumentException("Invalid ActivationRule");
		}

		this.id = createHashedId(ownerUsername + name + activationType + segmentId + agentId + timeToStart
				+ schedulingTime + timeUnit + condition + triggerEventName);
		return id;
	}

	// =========================
	// Scheduling metadata (read-only)
	// =========================
	public int getSchedulingTime() {
		return schedulingTime;
	}

	public int getTimeUnit() {
		return timeUnit;
	}

	public String getTimeToStart() {
		return timeToStart;
	}

	public boolean isManualTrigger() {
		return schedulingTime < 0;
	}

	public boolean isPollingLoop() {
		return schedulingTime == 0;
	}

	public boolean isFixedRate() {
		return schedulingTime > 0;
	}

	// =========================
	// Equality
	// =========================
	@Override
	public boolean equals(Object o) {
		return (o instanceof ActivationRule) && Objects.equals(id, ((ActivationRule) o).id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	@Override
	public int compareTo(ActivationRule o) {
		return Integer.compare(o.priority, this.priority);
	}

	@Override
	public String toString() {
		return GSON.toJson(this);
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	// =========================
	// Getter and Setter
	// =========================

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public Date getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getActivationType() {
		return activationType;
	}

	public void setActivationType(String activationType) {
		this.activationType = activationType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public List<String> getActions() {
		return actions;
	}

	public void setActions(List<String> actions) {
		this.actions = actions;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getSegmentId() {
		return segmentId;
	}

	public void setSegmentId(String segmentId) {
		this.segmentId = segmentId;
	}

	public String getTriggerEventName() {
		return triggerEventName;
	}

	public void setTriggerEventName(String triggerEventName) {
		this.triggerEventName = triggerEventName;
	}

	public String getOwnerUsername() {
		return ownerUsername;
	}

	public void setOwnerUsername(String ownerUsername) {
		this.ownerUsername = ownerUsername;
	}

	public static String getSchedulingTask() {
		return SCHEDULING_TASK;
	}

	public void setSchedulingTime(int schedulingTime) {
		this.schedulingTime = schedulingTime;
	}

	public void setTimeUnit(int timeUnit) {
		this.timeUnit = timeUnit;
	}

	public void setTimeToStart(String timeToStart) {
		this.timeToStart = timeToStart;
	}

	public int getAssetType() {
		return assetType;
	}

	public void setAssetType(int assetType) {
		this.assetType = assetType;
	}

	public String getAssetTemplateId() {
		return assetTemplateId;
	}

	public void setAssetTemplateId(String assetTemplateId) {
		this.assetTemplateId = assetTemplateId;
	}

	public String getAssetTemplateName() {
		return assetTemplateName;
	}

	public void setAssetTemplateName(String assetTemplateName) {
		this.assetTemplateName = assetTemplateName;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public static ActivationRule create(String ownerUsername, String purpose, String activationType, String name,
			String description, int priority, String agentId, String segmentId, String timeToStart, int schedulingTime,
			int timeUnit, String triggerEventName) {
		ActivationRule rule = new ActivationRule();

		rule.setOwnerUsername(ownerUsername);
		rule.setPurpose(purpose);
		rule.setActivationType(activationType);
		rule.setName(name);
		rule.setDescription(description);
		rule.setPriority(priority);
		rule.setAgentId(agentId);
		rule.setSegmentId(segmentId);

		rule.setTimeToStart(timeToStart);
		rule.setSchedulingTime(schedulingTime);
		rule.setTimeUnit(timeUnit);

		rule.setTriggerEventName(triggerEventName);

		Date now = new Date();
		rule.setCreatedAt(now);
		rule.setUpdatedAt(now);

		rule.buildHashedId();
		return rule;
	}

}
