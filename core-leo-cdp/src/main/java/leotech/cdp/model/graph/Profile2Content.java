package leotech.cdp.model.graph;

import java.util.Date;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoEdgeCollection;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.To;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.journey.EventMetric;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * The connection from Profile to AssetContent
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class Profile2Content extends ProfileGraphEdge {
	
	public static final String GRAPH_NAME =  PersistentObject.CDP_COLLECTION_PREFIX + "content_graph";
	public static final String COLLECTION_NAME = PersistentObject.getCdpCollectionName(Profile2Content.class);
	
	public static final String AQL_REMOVE_EDGES_BY_GROUP = "FOR e IN "+COLLECTION_NAME
			+" FILTER e.eventMetricId == 'recommend' AND @groupId IN e.assetGroupIds[*] REMOVE e IN " + COLLECTION_NAME;
	public static final String AQL_REMOVE_EDGES_BY_GROUP_AND_SEGMENT = "FOR e IN "+COLLECTION_NAME 
			+" FILTER e.eventMetricId == 'recommend' AND @groupId IN e.assetGroupIds[*] AND @segmentId IN e.segmentIds[*] REMOVE e IN " + COLLECTION_NAME;
	public static final String AQL_REMOVE_EDGES_BY_PROFILE = "FOR e IN "+COLLECTION_NAME 
			+" FILTER e.eventMetricId == 'recommend' AND @fromProfileId == e._from REMOVE e IN " + COLLECTION_NAME;
	
	static ArangoEdgeCollection edgeCollection = null;
	static ArangoCollection dataCollection = null;

	@To
	@Expose
	private String toContentId;
	
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	@Expose
	private AssetContent content = null;
	
	public Profile2Content() {
		// gson
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
	}
	
	public Profile2Content(ProfileIdentity fromProfile, AssetContent toContent, int score) {
		super();
		this.eventMetricId = BehavioralEvent.STR_RECOMMEND;
		this.eventScore = score;
		this.totalEvent = 1;
		
		this.fromProfileId = fromProfile.getDocumentUUID();
		this.toContentId = toContent.getDocumentUUID();
		this.assetGroupIds.addAll(toContent.getGroupIds());
		
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		this.buildHashedId();
	}

	public Profile2Content(ProfileIdentity fromProfile, AssetContent toContent, EventMetric eventMetric) {
		super();
		this.eventMetricId = eventMetric.getId();
		this.eventScore = eventMetric.getScore();
		this.totalEvent = 1;
		
		this.fromProfileId = fromProfile.getDocumentUUID();
		this.toContentId = toContent.getDocumentUUID();
		this.assetGroupIds.addAll(toContent.getGroupIds());
		
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		this.buildHashedId();
	}
	
	public Profile2Content(ProfileIdentity fromProfile, AssetContent toContent, int score, String targetMediaUnitId, String segmentId) {
		super();
		this.eventMetricId = BehavioralEvent.STR_RECOMMEND;
		this.eventScore = score;
		
		this.fromProfileId = fromProfile.getDocumentUUID();
		this.toContentId = toContent.getDocumentUUID();
		this.targetMediaUnitId = targetMediaUnitId;
		this.assetGroupIds.addAll(toContent.getGroupIds());
		this.segmentIds.add(segmentId);
		
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		this.buildHashedId();
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		this.key = PersistentObject.createHashedId(this.eventMetricId + this.fromProfileId + this.toContentId);
		return this.key;
	}
	
	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.eventMetricId) && StringUtil.isNotEmpty(this.fromProfileId) && StringUtil.isNotEmpty(this.toContentId) ;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + this.key;
	}
	
	public String getToContentId() {
		return toContentId.split("/")[1];
	}

	public AssetContent getContent() {
		return content;
	}

	public void setContent(AssetContent content) {
		this.content = content;
	}
	
	public long getTotalScore() {
		return eventScore;
	}

	public static String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	public synchronized static final ArangoEdgeCollection getArangoEdgeCollection() {
		if(edgeCollection == null) {
			ArangoDatabase db = AbstractCdpDatabaseUtil.getCdpDatabase();
			ArangoGraph graph = db.graph(GRAPH_NAME);
			edgeCollection = graph.edgeCollection(Profile2Content.COLLECTION_NAME);
		}
		return edgeCollection;
	}
	
	@Override
	public ArangoCollection getDbCollection() {
		if(dataCollection == null) {
			dataCollection = getArangoCollection(COLLECTION_NAME);
		}
		return dataCollection;
	}
	
	public static void initIndex() {
		new Profile2Content().getDbCollection();
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
