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
import leotech.cdp.domain.schema.FunnelMetaData;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * The connection from Profile to TouchpointHub
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class Profile2TouchpointHub extends ProfileGraphEdge {
	
	public static final String GRAPH_NAME =  PersistentObject.CDP_COLLECTION_PREFIX + "touchpointhub_graph";
	public static final String COLLECTION_NAME = PersistentObject.getCdpCollectionName(Profile2TouchpointHub.class);
	
	static ArangoEdgeCollection edgeCollection = null;
	static ArangoCollection dataCollection = null;

	@To
	@Expose
	String toTouchpointHubId;
	
	@Expose
	String journeyMapId;
	
	@Expose
	String funnelStage = FunnelMetaData.STAGE_NEW_VISITOR;
	
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	@Expose
	TouchpointHub touchpointHub = null;

	public Profile2TouchpointHub() {
		// gson
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
	}
	
	public Profile2TouchpointHub(Profile profile, TouchpointHub toTouchpointHub, int score) {
		super();
		this.eventMetricId = BehavioralEvent.STR_PAGE_VIEW;
		this.totalEvent = 1;
		this.eventScore = score;
		
		this.funnelStage = profile.getFunnelStage();
		this.fromProfileId = profile.getDocumentUUID();
		this.toTouchpointHubId = toTouchpointHub.getDocumentUUID();
		this.journeyMapId = toTouchpointHub.getJourneyMapId();
		
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		this.buildHashedId();
	}

	public Profile2TouchpointHub(Profile profile, String tpHubId, String journeyMapId, EventMetric eventMetric) {
		super();
		this.eventMetricId = eventMetric.getId();
		this.eventScore = eventMetric.getScore();
		this.totalEvent = 1;
		
		this.funnelStage = profile.getFunnelStage();
		this.fromProfileId = profile.getDocumentUUID();
		this.toTouchpointHubId = TouchpointHub.getDocumentUUID(tpHubId);
		this.journeyMapId = journeyMapId;
		
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		this.buildHashedId();
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		this.key = PersistentObject.createHashedId(this.eventMetricId + this.fromProfileId + this.toTouchpointHubId + this.journeyMapId);
		return this.key;
	}
	
	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.eventMetricId) && StringUtil.isNotEmpty(this.fromProfileId) && StringUtil.isNotEmpty(this.toTouchpointHubId) ;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + this.key;
	}
	
	public String getTouchpointHubId() {
		return toTouchpointHubId.split("/")[1];
	}

	public TouchpointHub getTouchpointHub() {
		return touchpointHub;
	}

	public void setTouchpointHub(TouchpointHub touchpointHub) {
		this.touchpointHub = touchpointHub;
	}
	
	public final String getFunnelStage() {
		return funnelStage;
	}

	public void setFunnelStage(String funnelStage) {
		if(StringUtil.isNotEmpty(funnelStage)) {
			this.funnelStage = funnelStage;
		}
	}
	
	public String getJourneyMapId() {
		return journeyMapId;
	}

	public void setJourneyMapId(String journeyMapId) {
		this.journeyMapId = journeyMapId;
	}

	public static String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	public synchronized static final ArangoEdgeCollection getArangoEdgeCollection() {
		if(edgeCollection == null) {
			ArangoDatabase db = AbstractCdpDatabaseUtil.getCdpDatabase();
			ArangoGraph graph = db.graph(GRAPH_NAME);
			edgeCollection = graph.edgeCollection(Profile2TouchpointHub.COLLECTION_NAME);
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
		new Profile2TouchpointHub().getDbCollection();
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
