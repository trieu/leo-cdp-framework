package leotech.cdp.model.marketing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.activation.TimeUnitCode;
import leotech.system.model.SystemUser;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * Campaign is defined as a series of organized actions which are done for one
 * purpose <br>
 * <br>
 * 
 * It can target directly into all profiles in a customer segment in
 * omni-channel. It can send directly reports to marketer via email or
 * notification <br>
 * <br>
 * 
 * It contains 4 financial metrics for ROI calculation: estimated cost, real
 * cost, estimated revenue and real revenue <br>
 * <br>
 * 
 * A campaign can have products or/and services to offer customer directly via
 * email or push notification <br>
 * <br>
 * 
 * Also need a set of activation rules (stored in automatedFlowJson) to evaluate
 * real-time profile data and fire reactive actions: sending email or message to
 * offer a product <br>
 * <br>
 * 
 * <b> Final Goal: Increasing the average customer lifetime value </b> <br>
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class Campaign extends PersistentObject implements Comparable<Campaign> {

	// https://docs.mautic.org/en/5.x/campaigns/campaign_builder.html

	// campaign status
	public static final int STATUS_DRAFT = 0;
	public static final int STATUS_PLANNED = 1;
	public static final int STATUS_IN_PROGRESS = 2;
	public static final int STATUS_COMPLETED = 3;
	public static final int STATUS_CANCELED = 4;
	public static final int STATUS_REMOVED = -1;

	public static final String COLLECTION_NAME = getCdpCollectionName(Campaign.class);
	static ArangoCollection dbCollection;

	@Key
	@Expose
	String id = null;

	@Expose
	String name;

	@Expose
	String description;

	@Expose
	List<String> keywords = new ArrayList<>();

	@Expose
	int type = CampaignType.USER_DEFINED;

	@Expose
	String automatedFlowJson = "";

	@Expose
	int status = STATUS_DRAFT;
	
	
	@Expose
	String agentPersona = ""; // Agent Persona
	
	@Expose
	List<Scenario> scenarios = new ArrayList<Scenario>();
	
	@Expose
	String targetSegmentId = ""; // what is the targeted segment of campaign

	@Expose
	@com.arangodb.velocypack.annotations.Expose(serialize = false, deserialize = true)
	String targetSegmentName = ""; // will be load from AQL join

	@Expose
	String ownerUsername = SystemUser.SUPER_ADMIN_LOGIN; // the owner or creator of campaign

	@Expose
	List<String> reportedUsernames = Arrays.asList(SystemUser.SUPER_ADMIN_LOGIN);// whos can be received reports via email

	@Expose
	double maxBudget = 0; // the estimated maximum budget to run marketing campaign

	@Expose
	double spentBudget = 0; // the real spending budget to run marketing campaign

	@Expose
	int communicationFrequency = 1; // default is daily communication via Email or Chatbot

	@Expose
	int timeUnit = TimeUnitCode.DAY;

	@Expose
	Date createdAt = null;

	@Expose
	Date updatedAt = null;

	// the time will set when run a campaign

	@Expose
	Date startDate = null; // if == null, means run immediately after status == STATUS_IN_PROGRESS

	@Expose
	Date lastRunAt = null;

	@Expose
	Date endDate = null; // if == null, means run forever likes email marketing
	
	@Expose
	String tagName;

	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();
			dbCollection = arangoDatabase.collection(COLLECTION_NAME);
			// ensure indexing key fields for fast lookup
			dbCollection.ensurePersistentIndex(Arrays.asList("tagName"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("ownerUsername"),
					new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("reportedUsernames[*]"),
					new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("type"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("status"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("name"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("keywords[*]"),
					new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("targetSegmentId"),
					new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("createdAt"), new PersistentIndexOptions().unique(false));
		}
		return dbCollection;
	}

	@Override
	public boolean dataValidation() {
		boolean ok = StringUtil.isNotEmpty(name) && StringUtil.isNotEmpty(ownerUsername)
				&& StringUtil.isNotEmpty(automatedFlowJson) && StringUtil.isNotEmpty(targetSegmentId);
		return ok;
	}

	public Campaign() {
		// gson
	}

	/**
	 * @param targetedSegmentId
	 * @param name
	 * @param tagName
	 * @param type
	 * @param automatedFlowJson
	 */
	public Campaign(String targetedSegmentId, String name, String tagName, int type, String automatedFlowJson) {
		super();
		setTargetSegmentId(targetedSegmentId);
		initData(name, tagName, type, automatedFlowJson);
	}

	/**
	 * @param targetedSegmentId
	 * @param name
	 * @param tagName
	 * @param automatedFlowJson
	 */
	public Campaign(String targetedSegmentId, String name, String tagName, String automatedFlowJson) {
		super();
		setTargetSegmentId(targetedSegmentId);
		initData(name, tagName, CampaignType.USER_DEFINED, automatedFlowJson);
	}

	/**
	 * @param targetedSegmentId
	 * @param name
	 * @param automatedFlowJson
	 */
	public Campaign(String targetedSegmentId, String name, String automatedFlowJson) {
		super();
		setTargetSegmentId(targetedSegmentId);
		initData(name, "", CampaignType.USER_DEFINED, automatedFlowJson);
	}

	/**
	 * @param name
	 * @param tagName
	 * @param type
	 * @param automatedFlowJson
	 */
	void initData(String name, String tagName, int type, String automatedFlowJson) {
		this.name = name;
		this.type = type;
		if (StringUtil.isNotEmpty(tagName)) {
			this.tagName = tagName;
		} else {
			// if tagName is empty, then slugify the name as tagName
			this.tagName = new Slugify().slugify(name).replace("-", "_").replace(" ", "_");
		}
		this.createdAt = new Date();
		this.updatedAt = new Date();
		this.automatedFlowJson = automatedFlowJson;
		this.buildHashedId();
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (dataValidation()) {
			String keyHint = this.name + this.type + this.createdAt + this.communicationFrequency + this.maxBudget
					+ this.automatedFlowJson + this.targetSegmentId;
			this.id = createId(this.id, keyHint);
		} else {
			newIllegalArgumentException("name and tagName are required ");
		}
		return this.id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getTargetSegmentId() {
		return targetSegmentId;
	}

	public void setTargetSegmentId(String targetSegmentId) {
		this.targetSegmentId = targetSegmentId;
	}

	public String getTargetSegmentName() {
		return targetSegmentName;
	}

	public void setTargetSegmentName(String targetedSegmentName) {
		this.targetSegmentName = targetedSegmentName;
	}

	@Override
	public int compareTo(Campaign o) {
		if (this.startDate != null && o.getStartDate() != null) {
			return this.startDate.compareTo(o.getStartDate());
		}
		return -1;
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
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}
//

	public String getOwnerUsername() {
		return ownerUsername;
	}

	public void setOwnerUsername(String ownerUsername) {
		this.ownerUsername = ownerUsername;
	}

	public String getAutomatedFlowJson() {
		return automatedFlowJson;
	}

	public void setAutomatedFlowJson(String automatedFlowJson) {
		this.automatedFlowJson = automatedFlowJson;
	}

	public List<String> getReportedUsernames() {
		return reportedUsernames;
	}

	public void setReportedUsernames(List<String> reportedUsernames) {
		this.reportedUsernames = reportedUsernames;
	}

	public double getMaxBudget() {
		return maxBudget;
	}

	public void setMaxBudget(double maxBudget) {
		this.maxBudget = maxBudget;
	}

	public double getSpentBudget() {
		return spentBudget;
	}

	public void setSpentBudget(double spentBudget) {
		this.spentBudget = spentBudget;
	}



	public String getAgentPersona() {
		return agentPersona;
	}

	public void setAgentPersona(String agentPersona) {
		this.agentPersona = agentPersona;
	}

	public int getCommunicationFrequency() {
		return communicationFrequency;
	}

	public void setCommunicationFrequency(int communicationFrequency) {
		this.communicationFrequency = communicationFrequency;
	}

	public int getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(int timeUnit) {
		this.timeUnit = timeUnit;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Date getLastRunAt() {
		return lastRunAt;
	}

	public void setLastRunAt(Date lastRunAt) {
		this.lastRunAt = lastRunAt;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

}
