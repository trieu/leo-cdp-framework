package leotech.cdp.dao.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoEdgeCollection;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.EdgeUpdateOptions;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.dao.TouchpointHubDaoUtil;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.graph.Profile2TouchpointHub;
import leotech.cdp.model.graph.ProfileGraphEdge;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.query.SegmentQuery;
import leotech.system.util.TaskRunner;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;

/**
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class GraphProfile2TouchpointHub extends AbstractCdpDatabaseUtil {

	private static final String GRAPH_NAME = Profile2TouchpointHub.GRAPH_NAME;
	
	/**
	 * @param db
	 */
	public final static void initGraph(ArangoDatabase db) {
		Collection<EdgeDefinition> edgeDefinitions = new ArrayList<>();
        EdgeDefinition edgeDefinition = new EdgeDefinition().collection(Profile2TouchpointHub.COLLECTION_NAME).from(Profile.COLLECTION_NAME).to(TouchpointHub.COLLECTION_NAME);
        edgeDefinitions.add(edgeDefinition);
        
        // check before create
        if (!db.graph(GRAPH_NAME).exists()) {
        	db.createGraph(GRAPH_NAME, edgeDefinitions, null);
        }
        
        Profile2TouchpointHub.initIndex();
	}
	
	/**
	 * @param profile
	 * @param tpHubId
	 * @param journeyMapId
	 * @param eventMetric
	 * @param eventCount
	 */
	public final static void batchUpdateEdgeData(Profile profile, String tpHubId, String journeyMapId, EventMetric eventMetric, int eventCount) {
		if(profile.getStatus() == Profile.STATUS_ACTIVE) {
			// init the edge
			Profile2TouchpointHub insertedEdge = new Profile2TouchpointHub(profile, tpHubId, journeyMapId, eventMetric);
			
			// get edge collect of graph
			ArangoEdgeCollection arangoEdgeCollection = Profile2TouchpointHub.getArangoEdgeCollection();
			
			// loop from 1 to eventCount
			final String key = insertedEdge.getKey();
			for (int i = 0; i < eventCount; i++) {
				TaskRunner.runInThreadPools(()->{
					try {
						Profile2TouchpointHub updatedEdge = arangoEdgeCollection.getEdge(key, Profile2TouchpointHub.class);
						if(updatedEdge != null) {
							updatedEdge.updateEventScore(eventMetric.getScore());
							updatedEdge.setFunnelStage(profile.getFunnelStage());
							updatedEdge.setUpdatedAt(new Date());
							EdgeUpdateOptions options = new EdgeUpdateOptions();
							options.waitForSync(true);
							arangoEdgeCollection.updateEdge(key, updatedEdge, options );
						}
						else {
							arangoEdgeCollection.insertEdge(insertedEdge);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		}
	}
	
	/**
	 * @param profile
	 * @param tpHubId
	 * @param journeyMapId
	 * @param eventMetric
	 */
	public final static void updateEdgeData(Profile profile, String tpHubId, String journeyMapId, EventMetric eventMetric) {
		batchUpdateEdgeData(profile, tpHubId, journeyMapId, eventMetric, 1);
	}
	
	/**
	 * @param profile
	 * @param tpHub
	 * @param score
	 * @param segmentId
	 */
	public final static void setRecommendedEdgeData(Profile profile, TouchpointHub tpHub, int score, String segmentId) {
		// init the edge
		Profile2TouchpointHub insertedEdge = new Profile2TouchpointHub(profile, tpHub, score);
		
		// get edge collect of graph
		ArangoEdgeCollection arangoEdgeCol = Profile2TouchpointHub.getArangoEdgeCollection();
		
		// add connection to edge collection 
		String key = insertedEdge.getKey();
		Profile2TouchpointHub updatedEdge = arangoEdgeCol.getEdge(key, Profile2TouchpointHub.class);
		if(updatedEdge != null) {
			updatedEdge.updateEventScore(score);
			updatedEdge.setUpdatedAt(new Date());
			arangoEdgeCol.updateEdge(key, updatedEdge);
		}
		else {
			arangoEdgeCol.insertEdge(insertedEdge);
		}
	}
	
	/**
	 * @param profile
	 * @param startIndex
	 * @param numberResult
	 * @param eventMetric
	 * @return
	 */
	public final static List<Profile2TouchpointHub> query(Profile profile, int startIndex, int numberResult, EventMetric eventMetric) {
		ArangoDatabase db = getCdpDatabase();
		
		String aql = ProfileGraphEdge.getGraphQuerySortByEventScore(GRAPH_NAME);
		
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("fromProfileId", profile.getDocumentUUID());
		bindVars.put("eventMetricId", eventMetric.getId());
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		CallbackQuery<Profile2TouchpointHub> callback = new CallbackQuery<Profile2TouchpointHub>() {
			@Override
			public Profile2TouchpointHub apply(Profile2TouchpointHub obj) {
				obj.setFromProfile(profile);
				
				String touchpointHubId = obj.getTouchpointHubId();
				TouchpointHub touchpointHub = TouchpointHubDaoUtil.getById(touchpointHubId);
				obj.setTouchpointHub(touchpointHub);
				return obj;
			}
		};
		List<Profile2TouchpointHub> rs = new ArangoDbCommand<Profile2TouchpointHub>(db, aql, bindVars, Profile2TouchpointHub.class, callback).getResultsAsList();
		return rs;
	}
	
	/**
	 * @param fromProfileId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Profile2TouchpointHub> getRecommendation(String fromProfileId, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		String aql = ProfileGraphEdge.getGraphQueryRecommendation(GRAPH_NAME);
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("fromProfileId", Profile.COLLECTION_NAME + "/" + fromProfileId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		CallbackQuery<Profile2TouchpointHub> callback = new CallbackQuery<Profile2TouchpointHub>() {
			@Override
			public Profile2TouchpointHub apply(Profile2TouchpointHub obj) {
				String touchpointHubId = obj.getTouchpointHubId();
				TouchpointHub hub = TouchpointHubDaoUtil.getById(touchpointHubId);
				obj.setTouchpointHub(hub);
				return obj;
			}
		};
		List<Profile2TouchpointHub> rs = new ArangoDbCommand<Profile2TouchpointHub>(db, aql, bindVars, Profile2TouchpointHub.class, callback).getResultsAsList();
		return rs;
	}
	
	/**
	 * @param fromProfileId
	 * @param startIndex
	 * @param numberResult
	 * @param rankedByIndexScore
	 * @return
	 */
	public static List<TouchpointHub> getRecommendedTouchpointHubs(String fromProfileId, int startIndex, int numberResult, boolean rankedByIndexScore) {
		ArangoDatabase db = getCdpDatabase();
		String aql = ProfileGraphEdge.getGraphQueryRecommendation(GRAPH_NAME);
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("fromProfileId", Profile.COLLECTION_NAME + "/" + fromProfileId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		List<TouchpointHub> list = new ArrayList<TouchpointHub>(numberResult);
		CallbackQuery<Profile2TouchpointHub> callback = new CallbackQuery<Profile2TouchpointHub>() {
			@Override
			public Profile2TouchpointHub apply(Profile2TouchpointHub obj) {
				String touchpointHubId = obj.getTouchpointHubId();
				TouchpointHub content = TouchpointHubDaoUtil.getById(touchpointHubId);
				list.add(content);
				return obj;
			}
		};
		new ArangoDbCommand<Profile2TouchpointHub>(db, aql, bindVars, Profile2TouchpointHub.class, callback).applyCallback();
		return list;
	}
	
	
	/**
	 * @param TouchpointHub
	 * @return
	 */
	public final static int setRecommendation(TouchpointHub item, String segmentId) {
		int count = 0;
		//FIXME improve performance for big database
		Segment segment = SegmentDaoUtil.getSegmentById(segmentId);
		if(segment != null) {
			int startIndex = 0;
			int numberResult = 100;
			SegmentQuery profileQuery = segment.buildQuery(startIndex, numberResult, true);
			List<Profile> profiles;
			if(profileQuery.isQueryReadyToRun()) {
				profiles = ProfileDaoUtil.getProfilesBySegmentQuery(profileQuery);
			}
			else {
				profiles = new ArrayList<>(0);
			}
			
			while ( ! profiles.isEmpty() ) {
				for (Profile profile : profiles) {
					setRecommendedEdgeData(profile, item, 1, segmentId);
					count ++;
				}
				//loop to the end of segment
				startIndex = startIndex + numberResult;
				profileQuery = segment.buildQuery(startIndex, numberResult, true);
				if(profileQuery.isQueryReadyToRun()) {
					profiles = ProfileDaoUtil.getProfilesBySegmentQuery(profileQuery);
				}
				else {
					profiles = new ArrayList<>(0);
				}
			}
		}
		return count;
	}
	
	/**
	 * @param key
	 * @param indexScore
	 * @return
	 */
	public final static boolean updateRanking(String key, int indexScore) {
		ArangoEdgeCollection arangoEdgeCollection = Profile2TouchpointHub.getArangoEdgeCollection();
		Profile2TouchpointHub updatedEdge = arangoEdgeCollection.getEdge(key, Profile2TouchpointHub.class);
		if(updatedEdge != null) {
			updatedEdge.setIndexScore(indexScore);
			updatedEdge.setUpdatedAt(new Date());
			arangoEdgeCollection.updateEdge(key, updatedEdge);
			return true;
		}
		return false;
	}
	
	/**
	 * @param oldProfileId
	 * @param newProfileId
	 * @return
	 */
	public static int updateFromOldProfileToNewProfile(String oldProfileId, String newProfileId) {
		ArangoEdgeCollection arangoEdgeCollection = Profile2TouchpointHub.getArangoEdgeCollection();
		int numberResult = 200;
		int startIndex = 0;
		int c = 0;
		int maxLoop = 10;
		List<Profile2TouchpointHub> list = getRecommendation(oldProfileId, startIndex, numberResult);
		while (!list.isEmpty()) {
			for (Profile2TouchpointHub updatedEdge : list) {
				String key = updatedEdge.getKey();
				
				String updatedProfileId = Profile.getDocumentUUID(newProfileId);
				
				if( ! updatedProfileId.equals(updatedEdge.getFromProfileId()) ) {
					updatedEdge.setFromProfileId(updatedProfileId);
					updatedEdge.setUpdatedAt(new Date());
					try {
						arangoEdgeCollection.updateEdge(key, updatedEdge);
						c++;
					} catch (ArangoDBException e) {
						e.printStackTrace();
					}
				}
			}
			startIndex = startIndex + numberResult;
			list = getRecommendation(oldProfileId, startIndex, numberResult);
			maxLoop--;
			if(maxLoop == 0) {
				break;
			}
		}
		return c;
	}
	
}
