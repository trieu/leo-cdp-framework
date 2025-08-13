package leotech.cdp.model.marketing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.annotations.Expose;

import leotech.system.model.SystemUser;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * Campaign is defined as a series of organized actions which are done for one purpose <br><br>
 * 
 * It can target directly into all profiles in a customer segment in omni-channel. 
 * It can send directly reports to marketer via email or notification <br><br>
 * 
 * It contains 4 financial metrics for ROI calculation: estimated cost, real
 * cost, estimated revenue and real revenue <br><br>
 * 
 * A campaign can have products or/and services to offer customer
 * directly via email or push notification <br><br>
 * 
 * Also need a set of activation rules (stored in automatedFlowJson) to evaluate real-time profile data and
 * fire reactive actions: sending email or message to offer a product <br><br>
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
	String tagName;

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
	Set<String> targetedSegmentIds = new HashSet<>(); // what are the targeted segments of campaign
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(serialize = false, deserialize = true)
	List<String> targetedSegmentNames = new ArrayList<>(); // will be load from query

	@Expose
	String ownerUsername  =SystemUser.SUPER_ADMIN_LOGIN; // the owner or creator of campaign

	@Expose
	List<String> reportedUsernames = Arrays.asList("superadmin");// whose (in the organization) can be received campaign report via email

	@Expose
	double estimatedCost = 0; // the estimated cost to run marketing campaign

	@Expose
	double realCost = 0;  // the real cost to run marketing campaign

	@Expose
	double estimatedRevenue = 0;

	@Expose
	double realRevenue = 0;

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
	

	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();
			dbCollection = arangoDatabase.collection(COLLECTION_NAME);
			// ensure indexing key fields for fast lookup
			dbCollection.ensurePersistentIndex(Arrays.asList("tagName"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("ownerUsername"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("reportedUsernames[*]"),new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("type"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("status"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("name"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("keywords[*]"),new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("targetedSegmentIds[*]"),new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("createdAt"), new PersistentIndexOptions().unique(false));
		}
		return dbCollection;
	}

	@Override
	public boolean dataValidation() {
		boolean ok = StringUtil.isNotEmpty(name) && StringUtil.isNotEmpty(ownerUsername) 
				&& StringUtil.isNotEmpty(automatedFlowJson) && this.targetedSegmentIds.size() > 0;
		return ok;
	}

	public Campaign() {
		// gson
	}

	/**
	 * @param targetedSegmentIds
	 * @param name
	 * @param tagName
	 * @param type
	 * @param automatedFlowJson
	 */
	public Campaign(List<String> targetedSegmentIds, String name, String tagName, int type, String automatedFlowJson) {
		super();
		setTargetedSegmentIds(targetedSegmentIds);
		initData(name, tagName, type, automatedFlowJson);
	}
	

	/**
	 * @param targetedSegmentIds
	 * @param name
	 * @param tagName
	 * @param automatedFlowJson
	 */
	public Campaign(List<String> targetedSegmentIds, String name, String tagName, String automatedFlowJson) {
		super();
		setTargetedSegmentIds(targetedSegmentIds);
		initData(name, tagName, CampaignType.USER_DEFINED, automatedFlowJson);
	}
	
	/**
	 * @param targetedSegmentIds
	 * @param name
	 * @param automatedFlowJson
	 */
	public Campaign(List<String> targetedSegmentIds, String name, String automatedFlowJson) {
		super();
		setTargetedSegmentIds(targetedSegmentIds);
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
		if(StringUtil.isNotEmpty(tagName)) {
			this.tagName = tagName;
		}
		else {
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
			String keyHint = this.name + this.type + this.createdAt + this.automatedFlowJson + this.targetedSegmentIds.toString();
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
	
	public double getEstimatedRevenue() {
		return estimatedRevenue;
	}

	public void setEstimatedRevenue(double estimatedRevenue) {
		this.estimatedRevenue = estimatedRevenue;
	}

	public double getRealRevenue() {
		return realRevenue;
	}

	public void setRealRevenue(double realRevenue) {
		this.realRevenue = realRevenue;
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
	public Set<String> getTargetedSegmentIds() {
		return targetedSegmentIds;
	}

	public void setTargetedSegmentIds(Set<String> targetedSegmentIds) {
		if(targetedSegmentIds != null) {
			if(targetedSegmentIds.size() > 0) {
				this.targetedSegmentIds = targetedSegmentIds;
			}
		}
	}
	
	public void setTargetedSegmentIds(List<String> targetedSegmentIds) {
		if(targetedSegmentIds != null) {
			targetedSegmentIds.forEach(e->{
				this.targetedSegmentIds.add(e);
			});
		}
	}

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

	public double getEstimatedCost() {
		return estimatedCost;
	}

	public void setEstimatedCost(double estimatedCost) {
		this.estimatedCost = estimatedCost;
	}

	public double getRealCost() {
		return realCost;
	}

	public void setRealCost(double realCost) {
		this.realCost = realCost;
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
	
	public double getRatioRevenuePerCost() {
		if (realCost > 0 && realRevenue > 0) {
			double ratio = realRevenue / realCost;
			return ratio;
		}
		return -1;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

}
