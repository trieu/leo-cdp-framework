package leotech.cdp.dao.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoEdgeCollection;
import com.arangodb.entity.EdgeDefinition;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.dao.AssetContentDaoUtil;
import leotech.cdp.dao.TargetMediaUnitDaoUtil;
import leotech.cdp.domain.SegmentQueryManagement;
import leotech.cdp.domain.TargetMediaUnitManagement;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.graph.Profile2Content;
import leotech.cdp.model.graph.ProfileGraphEdge;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.domain.SystemConfigsManagement;
import leotech.system.exception.InvalidDataException;
import leotech.system.util.TaskRunner;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;

/**
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class GraphProfile2Content extends AbstractCdpDatabaseUtil {

	private static final String GRAPH_NAME = Profile2Content.GRAPH_NAME;
	
	public final static void initGraph(ArangoDatabase db) {
		Collection<EdgeDefinition> edgeDefinitions = new ArrayList<>();
        EdgeDefinition edgeDefinition = new EdgeDefinition().collection(Profile2Content.COLLECTION_NAME).from(Profile.COLLECTION_NAME).to(AssetContent.COLLECTION_NAME);
        edgeDefinitions.add(edgeDefinition);
        
        // check before create
        if (!db.graph(GRAPH_NAME).exists()) {
        	db.createGraph(GRAPH_NAME, edgeDefinitions, null);
        }
        
        Profile2Content.initIndex();
	}
	
	public final static void batchUpdateEdgeData(ProfileIdentity profileIdentity, AssetContent content, EventMetric eventMetric, int eventCount, int indexScore) {
		if(profileIdentity != null) {
			// init the edge
			Profile2Content insertedEdge = new Profile2Content(profileIdentity, content, eventMetric);
			String key = insertedEdge.getKey();
			
			// get edge collect of graph
			ArangoEdgeCollection arangoEdgeCollection = Profile2Content.getArangoEdgeCollection();
			
			// loop from 1 to eventCount
			for (int i = 0; i < eventCount; i++) {
				Profile2Content updatedEdge = arangoEdgeCollection.getEdge(key, Profile2Content.class);
				if(updatedEdge != null) {
					updatedEdge.updateEventScore(eventMetric.getScore());
					updatedEdge.setUpdatedAt(new Date());
					
					// update for recommendation
					if(indexScore != 0) {
						updatedEdge.setIndexScore(indexScore);
					}
					
					arangoEdgeCollection.updateEdge(key, updatedEdge);
				}
				else {
					arangoEdgeCollection.insertEdge(insertedEdge);
				}
				
				
			}
		}
		else {
			throw new InvalidDataException("profileIdentity is NULL, can not batchUpdateEdgeData");
		}
	}
	
	/**
	 * @param profileIdentity
	 * @param creative
	 * @param eventMetric
	 */
	public final static void updateEdgeData(ProfileIdentity profileIdentity, AssetContent creative, EventMetric eventMetric) {
		batchUpdateEdgeData(profileIdentity, creative, eventMetric, 1, 0);
	}
	
	/**
	 * @param profileIdentity
	 * @param creative
	 * @param eventMetric
	 * @param indexScore
	 */
	public final static void updateEdgeData(ProfileIdentity profileIdentity, AssetContent creative, EventMetric eventMetric, int indexScore) {
		batchUpdateEdgeData(profileIdentity, creative, eventMetric, 1, indexScore);
	}
	
	public final static List<Profile2Content> query(Profile profile, int startIndex, int numberResult, EventMetric eventMetric) {
		ArangoDatabase db = getCdpDatabase();		
		String aql = ProfileGraphEdge.getGraphQuerySortByEventScore(GRAPH_NAME);
		
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("fromProfileId", profile.getDocumentUUID());
		bindVars.put("eventMetricId", eventMetric.getId());
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		CallbackQuery<Profile2Content> callback = new CallbackQuery<Profile2Content>() {
			@Override
			public Profile2Content apply(Profile2Content obj) {
				obj.setFromProfile(profile);
				
				String toContentId = obj.getToContentId();
				AssetContent content = AssetContentDaoUtil.getById(toContentId);
				obj.setContent(content);
				return obj;
			}
		};
		return new ArangoDbCommand<Profile2Content>(db, aql, bindVars, Profile2Content.class, callback).getResultsAsList();
	}
	
	/**
	 * @param fromProfileId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Profile2Content> getRecommendedContentItems(String fromProfileId, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		String aql = ProfileGraphEdge.getGraphQueryRecommendation(GRAPH_NAME);
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("fromProfileId", Profile.getDocumentUUID(fromProfileId));
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		CallbackQuery<Profile2Content> callback = new CallbackQuery<Profile2Content>() {
			@Override
			public Profile2Content apply(Profile2Content obj) {
				String toContentId = obj.getToContentId();
				AssetContent content = AssetContentDaoUtil.getById(toContentId);
				TargetMediaUnit media = TargetMediaUnitDaoUtil.getByIdForContentRecommendation(obj.getTargetMediaUnitId(), true);
			
				if(content != null && media != null) {
					obj.setTargetMediaUnit(media);
					obj.setContent(content);
					return obj;
				}
				return null;
			}
		};
		return new ArangoDbCommand<Profile2Content>(db, aql, bindVars, Profile2Content.class, callback).getResultsAsList();
	}
	
	
	
	public static List<TargetMediaUnit> getRecommendedAssetContents(String fromProfileId, int startIndex, int numberResult, boolean rankedByIndexScore) {
		ArangoDatabase db = getCdpDatabase();
		String aql = ProfileGraphEdge.getGraphQueryRecommendation(GRAPH_NAME);
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("fromProfileId", Profile.COLLECTION_NAME + "/" + fromProfileId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		List<TargetMediaUnit> list = new ArrayList<>(numberResult);
		CallbackQuery<Profile2Content> callback = new CallbackQuery<Profile2Content>() {
			@Override
			public Profile2Content apply(Profile2Content obj) {
				
				String targetMediaUnitId = obj.getTargetMediaUnitId();
				TargetMediaUnit media = TargetMediaUnitDaoUtil.getByIdForContentRecommendation(targetMediaUnitId, false);
			
				if(media != null) {
					list.add(media);
					return obj;
				}
				return null;
			}
		};
		new ArangoDbCommand<Profile2Content>(db, aql, bindVars, Profile2Content.class, callback).applyCallback();
		return list;
	}
	
	/**
	 * @param groupId
	 * @return
	 */
	public final static int setRecommenderDataFromAssetGroup(String groupId, String segmentId) {
		int count = 0;
		int startIndex1 = 0;
		int numberResult1 = SystemConfigsManagement.DEFAULT_ITEM_FOR_PROFILE;
		List<AssetContent> items = AssetContentDaoUtil.list("", groupId, startIndex1, numberResult1);
		while ( ! items.isEmpty() ) {
			for (AssetContent item : items) {
				count += setRecommendationForContent(item, segmentId);
			}
			startIndex1 = startIndex1 + numberResult1;
			items = AssetContentDaoUtil.list("", groupId, startIndex1, numberResult1);
		}
		return count;
	}
	

	
	/**
	 * @param profileIdentity
	 * @param content
	 * @param score
	 * @param segmentId
	 */
	public final static void setRecommendedEdgeData(ProfileIdentity profileIdentity, AssetContent content, int score, String segmentId) {
		// run async
		TaskRunner.runInThreadPools(new Runnable() {
			@Override
			public void run() {
				try {
					String clickableMediaId = createTargetMediaUnitFromContent(profileIdentity, content);
					
					// init the edge
					Profile2Content insertedEdge = new Profile2Content(profileIdentity, content, score, clickableMediaId, segmentId);
					
					// get edge collect of graph
					ArangoEdgeCollection arangoEdgeCol = Profile2Content.getArangoEdgeCollection();
					
					// add connection to edge collection 
					String key = insertedEdge.getKey();
					Profile2Content updatedEdge = arangoEdgeCol.getEdge(key, Profile2Content.class);
					if(updatedEdge != null) {
						updatedEdge.setSegmentId(segmentId);
						updatedEdge.updateEventScore(score);
						updatedEdge.setTargetMediaUnitId(clickableMediaId);
						updatedEdge.setUpdatedAt(new Date());
						arangoEdgeCol.updateEdge(key, updatedEdge);
					}
					else {
						insertedEdge.setSegmentId(segmentId);
						arangoEdgeCol.insertEdge(insertedEdge);
					}
				} catch (ArangoDBException e) {
					if(e.getErrorNum() != 1200) {
						System.err.println(e.getErrorNum() + " " + e.getErrorMessage());
					}
				}
			}
		});
	}
	
	static String createTargetMediaUnitFromContent(ProfileIdentity profileIdentity, AssetContent content) {
		String landingPageUrl = content.getLandingPageUrl();
		String landingPageName = content.getTitle();
		String contentItemId = content.getId();
		String headlineImageUrl = content.getHeadlineImageUrl();
		String headlineVideoUrl = content.getHeadlineVideoUrl();
		String profileId = profileIdentity.getId();
		String visitorId = profileIdentity.getVisitorId();
		TargetMediaUnit targetMediaUnit = TargetMediaUnit.fromContent(contentItemId, headlineImageUrl, headlineVideoUrl, profileId , visitorId , landingPageUrl, landingPageName);
		String ok = TargetMediaUnitManagement.save(targetMediaUnit);
		return ok;
	}
	
	/**
	 * @param AssetContent
	 * @return
	 */
	public final static int setRecommendationForContent(AssetContent item, String segmentId) {
		Consumer<? super ProfileIdentity> consumer = profileIdentity->{
			setRecommendedEdgeData(profileIdentity, item, 1, segmentId);
		};
		return SegmentQueryManagement.applyConsumerForAllProfilesInSegment(segmentId, consumer);
	}
	
	/**
	 * @param key
	 * @param indexScore
	 * @return
	 */
	public final static boolean updateRanking(String key, int indexScore) {
		ArangoEdgeCollection arangoEdgeCollection = Profile2Content.getArangoEdgeCollection();
		Profile2Content updatedEdge = arangoEdgeCollection.getEdge(key, Profile2Content.class);
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
		ArangoEdgeCollection arangoEdgeCollection = Profile2Content.getArangoEdgeCollection();
		int numberResult = 200;
		int startIndex = 0;
		int c = 0;
		int maxLoop = 10;
		List<Profile2Content> list = getRecommendedContentItems(oldProfileId, startIndex, numberResult);
		while (!list.isEmpty()) {
			for (Profile2Content updatedEdge : list) {
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
			list = getRecommendedContentItems(oldProfileId, startIndex, numberResult);
			
			maxLoop--;
			if(maxLoop == 0) {
				break;
			}
		}
		return c;
	}
	
	/**
	 * @param groupId
	 */
	public final static void removeAllGraphEdgesByGroupId(String groupId) {
		ArangoDatabase db = getCdpDatabase();
		String aql = Profile2Content.AQL_REMOVE_EDGES_BY_GROUP;
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("groupId", groupId );
		db.query(aql, bindVars, Void.class);
	}
	
	/**
	 * @param groupId
	 * @param segmentId
	 */
	public final static void removeAllGraphEdgesByGroupIdAndSegmentId(String groupId, String segmentId) {
		ArangoDatabase db = getCdpDatabase();
		String aql = Profile2Content.AQL_REMOVE_EDGES_BY_GROUP_AND_SEGMENT;
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("groupId", groupId );
		bindVars.put("segmentId", segmentId );
		db.query(aql, bindVars, Void.class);
	}
	
	
	/**
	 * remove All Graph Profile Edges By SegmentId
	 * 
	 * @param segmentId
	 * @return the total of profiles is removed from Profile2Product
	 */
	public final static int removeAllGraphEdgesBySegmentId(String segmentId) {
		Consumer<? super ProfileIdentity> applyRemoveEdgeData = profileIdentity -> {
			ArangoDatabase db = getCdpDatabase();
			String aql = Profile2Content.AQL_REMOVE_EDGES_BY_PROFILE;
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("fromProfileId", profileIdentity.getDocumentUUID() );
			db.query(aql, bindVars, Void.class);
		};
		return SegmentQueryManagement.applyConsumerForAllProfilesInSegment(segmentId, applyRemoveEdgeData);
	}
	
}
