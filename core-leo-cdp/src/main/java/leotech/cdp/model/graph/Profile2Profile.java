package leotech.cdp.model.graph;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoEdgeCollection;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.To;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * The connection from Profile to Profile
 * 
 * @author tantrieuf31
 * @since 2023
 *
 */
public final class Profile2Profile extends ProfileGraphEdge {
	
	/**
	 * Relationship Type between 2 profiles
	 */
	public static class RelationshipType {
		public static final int ONE_PERSON = 1;
		public static final int FAMILY = 2;
		public static final int FRIENDSHIP = 3;
		public static final int ROMANTIC = 4;
		public static final int ACQUAINTANCE = 5;
		public static final int PROFESSIONAL = 6;
		public static final int MENTORSHIP = 7;
		public static final int NEIGHBORLY = 8;
		public static final int COMPETITOR = 9;
		
		int type;
		int score;

		public RelationshipType(int type, int score) {
			super();
			this.type = type;
			this.score = score;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public int getScore() {
			return score;
		}

		public void setScore(int score) {
			this.score = score;
		}
	}
	
	public static final String GRAPH_NAME =  PersistentObject.CDP_COLLECTION_PREFIX + "profile_graph";
	public static final String COLLECTION_NAME = PersistentObject.getCdpCollectionName(Profile2Profile.class);
	
	static ArangoEdgeCollection edgeCollection = null;
	static ArangoCollection dataCollection = null;
	
	public final static String getGraphQueryForSingleView(int singleViewScoreLimit) {
		StringBuilder aql = new StringBuilder();
		aql.append(" LET edgeList = ( ");
		aql.append(" FOR v, e, p IN 1..1 OUTBOUND @fromProfileId GRAPH ").append(GRAPH_NAME);
		aql.append(" FILTER p.edges[0].singleViewScore >= @singleViewScoreLimit LIMIT @startIndex,@numberResult RETURN p.edges[0] ) ");
		aql.append(" FOR e IN edgeList SORT e.singleViewScore DESC RETURN e ");
		return aql.toString();
	}

	@To
	@Expose
	String toProfileId;
	
	@Expose
	int singleViewScore = 0;
	
	@Expose
	Map<Integer, Integer> relationshipTypes = new HashMap<>(10);
	
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	@Expose
	protected Profile toProfile = null;


	public Profile2Profile() {
		// gson
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
	}
	
	public Profile2Profile(ProfileIdentity fromProfileIdentity, ProfileIdentity toProfileIdentity, int singleViewScore) {
		super();
		this.eventMetricId = BehavioralEvent.General.PAGE_VIEW;
		this.totalEvent = 1;
		this.singleViewScore = singleViewScore;
		this.relationshipTypes.put(RelationshipType.ONE_PERSON, singleViewScore);
		

		this.fromProfileId = fromProfileIdentity.getDocumentUUID();
		this.toProfileId = toProfileIdentity.getDocumentUUID();
		
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		this.buildHashedId();
	}
	
	public Profile2Profile(Profile fromProfile, Profile toProfile, RelationshipType relationshipType) {
		super();
		this.relationshipTypes.put(relationshipType.getType(), relationshipType.getScore());
		
		this.fromProfileId = fromProfile.getDocumentUUID();
		this.toProfileId = toProfile.getDocumentUUID();
		
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		this.buildHashedId();
	}


	@Override
	public String buildHashedId() throws IllegalArgumentException {
		this.key = PersistentObject.createHashedId(this.fromProfileId + this.toProfileId + this.singleViewScore + relationshipTypes.toString());
		return this.key;
	}
	
	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.fromProfileId) && StringUtil.isNotEmpty(this.toProfileId) ;
	}
	
	

	public String getToProfileId() {
		return toProfileId;
	}

	public void setToProfileId(String toProfileId) {
		this.toProfileId = toProfileId;
	}
	
	

	public Profile getToProfile() {
		return toProfile;
	}

	public void setToProfile(Profile toProfile) {
		this.toProfile = toProfile;
	}

	public int getSingleViewScore() {
		return singleViewScore;
	}

	public void setSingleViewScore(int singleViewScore) {
		this.singleViewScore = singleViewScore;
	}

	public Map<Integer, Integer> getRelationshipTypes() {
		return relationshipTypes;
	}

	public void setRelationshipTypes(Map<Integer, Integer> relationshipTypes) {
		this.relationshipTypes = relationshipTypes;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + this.key;
	}
	
	public static String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	public synchronized static final ArangoEdgeCollection getArangoEdgeCollection() {
		if(edgeCollection == null) {
			ArangoDatabase db = AbstractCdpDatabaseUtil.getCdpDatabase();
			ArangoGraph graph = db.graph(GRAPH_NAME);
			edgeCollection = graph.edgeCollection(Profile2Profile.COLLECTION_NAME);
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
		new Profile2Profile().getDbCollection();
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
