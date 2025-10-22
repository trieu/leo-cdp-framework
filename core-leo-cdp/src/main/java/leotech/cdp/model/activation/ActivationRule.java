package leotech.cdp.model.activation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.quartz.DateBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.system.model.SystemUser;
import leotech.system.util.LogUtil;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * 
 *   An activation rule is persistent object with condition and actions that
 *   creates an opening for a marketing or sales opportunity. <br>
 *   Sales and marketing automation workflows use trigger events to enable small
 *   organizations to scale customer interactions.
 *   
 *    @author Trieu Nguyen (Thomas)
 *    @since 2021
 *
 */
public class ActivationRule extends PersistentObject implements Comparable<ActivationRule>, Serializable {
	
	private static final int DEFAULT_TIME_INTERVAL = 300; // 300 seconds is 5 minutes

	private static final long serialVersionUID = 1825726230556120351L;

	// 15 minutes
	public static final long TIMEOUT_ACTIVATION = 60000 * 15;
	
	// 20 minutes
	public static final long TIME_FOR_NEXT_LOOP = 60000 * 20;

	public static final String SCHEDULING_TASK = "scheduling_task";
	public static final String EVENT_TRIGGER_TASK = "event_trigger_task";
	
	public static final String COLLECTION_NAME = getCdpCollectionName(ActivationRule.class);
	static ArangoCollection dbCollection;

	@Key
	@Expose
	String id;

	@Expose
	Date createdAt;

	@Expose
	Date updatedAt;

	@Expose
	String name;
	
	@Expose
	String activationType;

	@Expose
	String description = "";

	@Expose
	int priority = 1;
	
	@Expose
	boolean active = true;

	@Expose
	String condition = "true";
	
	@Expose
	List<String> actions = new ArrayList<String>();

	@Expose
	String purpose = "";

	@Expose
	int assetType = -1;
	
	@Expose
	String assetTemplateId = "";
	
	@Expose
	String assetTemplateName = "";
	
	@Expose
	String dataServiceId = "";
	
	@Expose
	String dataServiceName = "";
	
	@Expose
	String segmentId = "";
	
	@Expose
	String campaignId = "";
	
	@Expose
	int schedulingTime = 0; // run every 5 minutes
	
	@Expose
	String timeToStart = "";  // in HH:mm , e.g: 02:16
	
	@Expose
	String triggerEventName = "";
	
	@Expose
	boolean directActivation = false;
	
	/**
	 * who created this rule 
	 */
	@Expose
	String ownerUsername = SystemUser.SUPER_ADMIN_LOGIN;	
	
	
	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			ArangoDatabase arangoDatabase = ArangoDbUtil.getCdpDatabase();
			dbCollection = arangoDatabase.collection(COLLECTION_NAME);
			// ensure indexing key fields
			dbCollection.ensurePersistentIndex(Arrays.asList("dataServiceId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("assetTemplateId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("segmentId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("campaignId"), new PersistentIndexOptions().unique(false));
		}
		return dbCollection;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.name) && StringUtil.isNotEmpty(this.activationType) && StringUtil.isNotEmpty(this.ownerUsername);
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if(dataValidation()) {
			String keyHint = ownerUsername + name + activationType + segmentId + campaignId + dataServiceId + timeToStart + purpose + schedulingTime + condition + triggerEventName;
			this.id = createHashedId(keyHint);
			return this.id;
		} else {
			newIllegalArgumentException("purpose, segmentId and dataServiceId are required to create ActivationRule !");
		}
		return null;
	}

	public ActivationRule() {
		// for JSON
	}
	
	/**
	 * to create a new ActivationRule
	 * 
	 * @param purpose
	 * @param activationType
	 * @param name
	 * @param description
	 * @param priority
	 * @param dataServiceId
	 * @param segmentId
	 * @param delayMinutes
	 * @param schedulingTime
	 * @param triggerEventName
	 * @return
	 */
	public static ActivationRule create(String ownerUsername, String purpose, String activationType, String name, String description, int priority, String dataServiceId, String segmentId, String timeToStart, int schedulingTime,  String triggerEventName) {
		return new ActivationRule(ownerUsername, purpose, activationType, name, description, priority, dataServiceId, segmentId, timeToStart, schedulingTime, triggerEventName);
	}
	
	public static ActivationRule create(String ownerUsername, String purpose, String activationType, String name, String condition) {
		return new ActivationRule(ownerUsername, purpose, activationType, name, condition);
	}

	protected ActivationRule(String ownerUsername, String purpose, String activationType, String name, String description, int priority, String dataServiceId, String segmentId, String timeToStart, int schedulingTime, String triggerEventName) {
		super();
		this.ownerUsername = ownerUsername;
		this.purpose = purpose;
		this.condition = SCHEDULING_TASK;
		
		this.activationType = activationType;
		this.name = name;
		this.description = description;
		this.priority = priority;
		this.dataServiceId = dataServiceId;
		this.segmentId = segmentId;
		
		// if schedulingTime > 0, means is scheduleAtFixedRate, make sure the delay value is positive number
		this.timeToStart = timeToStart;
		this.schedulingTime = schedulingTime;
		this.triggerEventName = triggerEventName;
		
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		
		// build ID
		buildHashedId();
	}
	
	protected ActivationRule(String ownerUsername, String purpose, String activationType, String name, String condition) {
		super();
		this.ownerUsername = ownerUsername;
		this.purpose = purpose;
		this.condition = condition;
		
		this.activationType = activationType;
		this.name = name;
		this.description = "";
		this.priority = 0;
		
		// if schedulingTime > 0, means is scheduleAtFixedRate, make sure the delay value is positive number
		this.timeToStart = "00:00";
		this.schedulingTime = 0;
		this.triggerEventName = "";
		
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		
		// build ID
		buildHashedId();
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getTimeToStart() {
		return timeToStart;
	}
	
	public Date getTodayAt() {		
		String[] toks = timeToStart.split(StringPool.COLON);		
		int hourOfDay = 0;
		int minute = 0;
		int second = 0;
		if(toks.length == 2) {
			hourOfDay = StringUtil.safeParseInt(toks[0]);
			minute = StringUtil.safeParseInt(toks[1]);
			return DateBuilder.todayAt(hourOfDay, minute, second);
		}
		return new Date();
	}

	public void setTimeToStart(String timeToStart) {
		this.timeToStart = timeToStart;
	}

	public String getTriggerEventName() {
		return triggerEventName;
	}

	public void setTriggerEventName(String triggerEventName) {
		this.triggerEventName = triggerEventName;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public void setAssetTemplateId(String assetTemplateId) {
		this.assetTemplateId = assetTemplateId;
	}

	public void setSchedulingTime(int schedulingTime) {
		this.schedulingTime = schedulingTime;
	}

	public String getActivationType() {
		return activationType;
	}

	public void setActivationType(String activationType) {
		this.activationType = activationType;
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

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
	
	public String getOwnerUsername() {
		if(this.ownerUsername == null) {
			return "";
		}
		return ownerUsername;
	}

	public void setOwnerUsername(String ownerUsername) {
		if(StringUtil.isEmpty(this.ownerUsername)) {
			this.ownerUsername = ownerUsername;
		}
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public Date getCreatedAt() {
		return this.createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public Date getUpdatedAt() {
		return this.updatedAt;
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public int getAssetType() {
		return assetType;
	}

	public void setAssetType(int assetType) {
		this.assetType = assetType;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isDirectActivation() {
		return directActivation;
	}

	public void setDirectActivation(boolean directActivation) {
		this.directActivation = directActivation;
	}

	public static String getCollectionName() {
		return COLLECTION_NAME;
	}

	public String getAssetTemplateId() {
		return assetTemplateId;
	}
	
	public String getDataServiceId() {
		return dataServiceId;
	}

	public void setDataServiceId(String dataServiceId) {
		this.dataServiceId = dataServiceId;
	}

	public String getDataServiceName() {
		return dataServiceName;
	}

	public void setDataServiceName(String dataServiceName) {
		this.dataServiceName = dataServiceName;
	}

	public String getAssetTemplateName() {
		return assetTemplateName;
	}

	public void setAssetTemplateName(String assetTemplateName) {
		this.assetTemplateName = assetTemplateName;
	}
	
	public String getSegmentId() {
		return segmentId;
	}

	public void setSegmentId(String segmentId) {
		this.segmentId = segmentId;
	}

	public int getSchedulingTime() {
		return schedulingTime;
	}
	
	
	
	public List<String> getActions() {
		return actions;
	}

	public void setActions(List<String> actions) {
		this.actions = actions;
	}
	
	public void setAction(String action) {
		this.actions.add(action);
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
	@Override
	public boolean equals(Object obj) {
		ActivationRule rule = (ActivationRule) obj;
		return this.id.equals(rule.getId());
	}

	@Override
	public int compareTo(ActivationRule o) {
		if(this.priority > o.priority) {
			return -1;
		}
		else if(this.priority < o.priority) {
			return 1;
		}
		return 0;
	}

	/**
	 * start activation rule instance and schedule it as TimerTask
	 * 
	 * @param service
	 * @param timer
	 */
	public final JobDetail getJobDetail(String activationJobId, Agent dataService) {
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("activationRule", this);
		jobDataMap.put("dataService", dataService);
		jobDataMap.put("activationJobId", activationJobId);
		Class<? extends Job> classForJobDetails = dataService.getClassForJobDetails();
		JobDetail jobDetail = JobBuilder.newJob(classForJobDetails).usingJobData(jobDataMap)
				.withIdentity("activationJobId", activationJobId)
				.build();
		return jobDetail;
	}
	
	/**
	 * @param timer
	 */
	public final Trigger getJobTrigger() {
		LogUtil.logInfo(this.getClass(), "setSchedulerForActivationRule schedulingTime: " + schedulingTime);
		if (schedulingTime >= 0) {
			Date timeToStart = getTodayAt();
			
			SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
			if(schedulingTime == 0) {
				scheduleBuilder.withIntervalInSeconds(DEFAULT_TIME_INTERVAL).repeatForever();
			}
			else {
				scheduleBuilder.withIntervalInHours(schedulingTime).repeatForever();
			}
			
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity("ActivationRule", this.getId())
					.startAt(timeToStart) 
					.withSchedule(scheduleBuilder)
					.build();
			return trigger;
		}
		else {
			System.out.println("===> do manually for one time");
			LogUtil.logInfo(this.getClass(), "setSchedulerForActivationRule do manually for one time: ");
			// do manually for one time
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity("ActivationRule", this.getId())
					.startAt(new Date()) 
					.build();
			return trigger;
		}
	}

}
