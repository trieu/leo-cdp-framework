package leotech.cdp.model.graph;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.From;
import com.arangodb.entity.Key;
import com.arangodb.entity.Rev;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.annotations.Expose;

import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * 
 * Profile Graph Edge 
 * 
 * @author tantrieuf31
 *
 */
public abstract class ProfileGraphEdge extends PersistentObject implements Comparable<ProfileGraphEdge> {
	
	/**
	 * sort by eventScore
	 * 
	 * @param graphName
	 * @return
	 */
	public final static String getGraphQuerySortByEventScore(String graphName) {
		StringBuilder aql = new StringBuilder();
		aql.append(" LET edgeList = ( ");
		aql.append(" FOR v, e, p IN 1..1 OUTBOUND @fromProfileId GRAPH ").append(graphName);
		aql.append(" FILTER p.edges[0].eventMetricId == @eventMetricId SORT e.eventScore DESC LIMIT @startIndex,@numberResult RETURN p.edges[0] ) ");
		aql.append(" FOR e IN edgeList RETURN e ");
		return aql.toString();
	}
	

	
	/**
	 * sort by indexScore
	 * 
	 * @param graphName
	 * @return
	 */
	public final static String getGraphQueryRecommendation(String graphName) {
		StringBuilder aql = new StringBuilder();
		aql.append(" LET edgeList = ( ");
		aql.append(" FOR v, e, p IN 1..1 OUTBOUND @fromProfileId GRAPH ").append(graphName);
		aql.append(" FILTER p.edges[0].eventMetricId == '").append(BehavioralEvent.General.RECOMMEND).append("' ");
		aql.append(" SORT e.eventScore DESC, e.indexScore LIMIT @startIndex,@numberResult RETURN p.edges[0] ) ");
		aql.append(" FOR e IN edgeList "); 
		aql.append(" RETURN e ");
		return aql.toString();
	}
	
	/**
	 * @param edgeCollectionName
	 * @return
	 */
	protected static ArangoCollection getArangoCollection(String edgeCollectionName) {
		ArangoDatabase arangoDatabase = getArangoDatabase();
		ArangoCollection instance = arangoDatabase.collection(edgeCollectionName);

		// ensure indexing key fields for fast lookup
		PersistentIndexOptions indexOptions = new PersistentIndexOptions().inBackground(true).unique(false).deduplicate(false);
		instance.ensurePersistentIndex(Arrays.asList("eventMetricId","assetGroupIds[*]","segmentIds[*]"), indexOptions);
		instance.ensurePersistentIndex(Arrays.asList("fromProfileId","eventMetricId","targetMediaUnitId"), indexOptions);
		instance.ensurePersistentIndex(Arrays.asList("fromProfileId","eventMetricId","targetMediaUnitId","indexScore","updatedAt"), indexOptions);
		
		return instance;
	}
	
	@Key
	@Expose
	protected String key;
	
	@Rev
	@Expose
	protected String rev;
	
	@From
	@Expose
	protected String fromProfileId;
	
	@Expose
	protected Set<String> segmentIds = new HashSet<>(10);
	
	@Expose
	protected Set<String> assetGroupIds = new HashSet<>(10);

	@Expose
	protected String eventMetricId = "";
	
	@Expose
	protected Date createdAt;

	@Expose
	protected Date updatedAt;
	
	@Expose
	protected int eventScore = 0;
	
	@Expose
	protected long totalEvent = 0;
	
	@Expose
	protected int indexScore = 0;
	
	@Expose
	String targetMediaUnitId = "";
	
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	@Expose
	protected Profile fromProfile = null;
	
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	@Expose
	TargetMediaUnit targetMediaUnit;
	
	/**
	 * 
	 */
	public ProfileGraphEdge() {
		super();
	}
	
	@Override
	public int compareTo(ProfileGraphEdge o) {
		if(this.eventScore < o.eventScore) {
			return 1;
		}
		else if(this.eventScore > o.eventScore) {
			return -1;
		}
		return this.updatedAt.compareTo(o.getUpdatedAt());
	}
	
	
	public String getKey() {
		if(StringUtil.isEmpty(key)) {
			this.key = buildHashedId();
		}
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public String getRev() {
		return rev;
	}

	public void setRev(String rev) {
		this.rev = rev;
	}

	public String getFromProfileId() {
		return fromProfileId.split("/")[1];
	}
	
	public void setFromProfileId(String fromProfileId) {
		this.fromProfileId = fromProfileId;
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

	public String getEventMetricId() {
		return eventMetricId;
	}

	public void setEventMetricId(String eventMetricId) {
		this.eventMetricId = eventMetricId;
	}
	
	public Profile getFromProfile() {
		return fromProfile;
	}

	public void setFromProfile(Profile profile) {
		this.fromProfile = profile;
	}
	
	public void setEventScore(int eventScore) {
		this.eventScore = eventScore;
	}
	
	public int getEventScore() {
		return eventScore;
	}

	public void updateEventScore(int baseScore) {
		this.eventScore += baseScore;
		this.totalEvent++;
	}
	
	public long getTotalEvent() {
		return totalEvent;
	}

	public void setTotalEvent(long totalEvent) {
		if(totalEvent >= 0) {
			this.totalEvent = totalEvent;
		}
	}
	
	public int getIndexScore() {
		return indexScore;
	}

	public void setIndexScore(int indexScore) {
		this.indexScore = indexScore;
	}
	
	public Set<String> getSegmentIds() {
		return segmentIds;
	}

	public void setSegmentIds(Set<String> segmentIds) {
		this.segmentIds = segmentIds;
	}
	
	public void setSegmentId(String segmentId) {
		this.segmentIds.add(segmentId);
	}
	
	public Set<String> getAssetGroupIds() {
		return assetGroupIds;
	}

	public void setAssetGroupIds(Set<String> groupIds) {
		if(groupIds != null) {
			this.assetGroupIds = groupIds;
		}
	}
	
	public String getTargetMediaUnitId() {
		return targetMediaUnitId;
	}

	public void setTargetMediaUnitId(String targetMediaUnitId) {
		this.targetMediaUnitId = targetMediaUnitId;
	}
	
	public TargetMediaUnit getTargetMediaUnit() {
		return targetMediaUnit;
	}

	public void setTargetMediaUnit(TargetMediaUnit targetMediaUnit) {
		this.targetMediaUnit = targetMediaUnit;
	}
	
}