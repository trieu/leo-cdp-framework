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
import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.dao.TargetMediaUnitDaoUtil;
import leotech.cdp.domain.AssetItemManagement;
import leotech.cdp.domain.SegmentQueryManagement;
import leotech.cdp.domain.TargetMediaUnitManagement;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.graph.Profile2Content;
import leotech.cdp.model.graph.Profile2Product;
import leotech.cdp.model.graph.ProfileGraphEdge;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.domain.SystemConfigsManagement;
import leotech.system.util.TaskRunner;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;

/**
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class GraphProfile2Product extends AbstractCdpDatabaseUtil {

	
	private static final String G_PROFILE2PRODUCT = Profile2Product.GRAPH_NAME;
	
	/**
	 * init graph data schema
	 */
	public final static void initGraph(ArangoDatabase db) {
		Collection<EdgeDefinition> edgeDefinitions = new ArrayList<>();
        EdgeDefinition edgeDefinition = new EdgeDefinition().collection(Profile2Product.COLLECTION_NAME).from(Profile.COLLECTION_NAME).to(ProductItem.COLLECTION_NAME);
        edgeDefinitions.add(edgeDefinition);
        
        // check before create
        if (!db.graph(G_PROFILE2PRODUCT).exists()) {
        	db.createGraph(G_PROFILE2PRODUCT, edgeDefinitions, null);
        }
        
        // set index
        Profile2Product.initIndex();
	}
	
	/**
	 * @param profile
	 * @param product
	 * @param eventMetric
	 */
	public final static void batchUpdateEdgeData(Date createdAt, Profile profile, ProductItem product, EventMetric eventMetric) {
		batchUpdateEdgeData(createdAt, profile, product,  eventMetric, 1, 0);
	}
	

	/**
	 * to update the ranking of item
	 * 
	 * @param createdAt
	 * @param profile
	 * @param product
	 * @param eventMetric
	 * @param indexScore
	 */
	public final static void batchUpdateEdgeData(Date createdAt, Profile profile, ProductItem product, EventMetric eventMetric, int indexScore) {
		batchUpdateEdgeData(createdAt, profile, product,  eventMetric, 1, indexScore);
	}
	

	/**
	 * @param profile
	 * @param product
	 * @param quantity
	 * @param eventMetric
	 * @param eventCount
	 * @param transactionId
	 * @param transactionValue
	 * @param currencyCode
	 */
	public final static void batchUpdateEdgeData(Date createdAt, Profile profile, ProductItem product, EventMetric eventMetric, int eventCount, int indexScore) {
		if(profile.getStatus() == Profile.STATUS_ACTIVE) {
			// init the edge
			Profile2Product insertedEdge = new Profile2Product(createdAt, profile, product, eventMetric);
			
			// get edge collect of graph
			ArangoEdgeCollection arangoEdgeCollection = Profile2Product.getArangoEdgeCollection();
			
			// loop from 1 to eventCount
			String key = insertedEdge.getKey();
			for (int i = 0; i < eventCount; i++) {
				Profile2Product updatedEdge = arangoEdgeCollection.getEdge(key, Profile2Product.class);
				if(updatedEdge != null) {
					updatedEdge.updateEventScore(eventMetric.getScore());
					updatedEdge.setUpdatedAt(new Date());
					
					// update for recommendation
					if(indexScore != 0) {
						updatedEdge.setIndexScore(indexScore);
					}
					
					arangoEdgeCollection.updateEdge(key, updatedEdge);
					System.out.println("batchUpdateEdgeData.updateEdge key: " + key);
				}
				else {
					// update for recommendation
					if(indexScore != 0) {
						insertedEdge.setIndexScore(indexScore);
					}
					arangoEdgeCollection.insertEdge(insertedEdge);
					System.out.println("batchUpdateEdgeData.insertEdge key: " + key);
				}
			}
		}
	}
	
	/**
	 * @param key
	 * @param indexScore
	 * @return
	 */
	public final static boolean updateRanking(String key, int indexScore) {
		ArangoEdgeCollection arangoEdgeCollection = Profile2Product.getArangoEdgeCollection();
		Profile2Product updatedEdge = arangoEdgeCollection.getEdge(key, Profile2Product.class);
		if(updatedEdge != null) {
			updatedEdge.setIndexScore(indexScore);
			updatedEdge.setUpdatedAt(new Date());
			arangoEdgeCollection.updateEdge(key, updatedEdge);
			return true;
		}
		return false;
	}
	
	
	/**
	 * @param groupId
	 */
	public final static void removeAllGraphEdgesByGroupId(String groupId) {
		ArangoDatabase db = getCdpDatabase();
		String aql = Profile2Product.AQL_REMOVE_EDGES_BY_GROUP;
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("groupId", groupId );
		db.query(aql, bindVars, Void.class);
		System.out.println("removeAllGraphEdgesByGroupId "+aql);
	}
	
	/**
	 * @param groupId
	 * @param segmentId
	 */
	public final static void removeAllGraphEdgesByGroupIdAndSegmentId(String groupId, String segmentId) {
		ArangoDatabase db = getCdpDatabase();
		String aql = Profile2Product.AQL_REMOVE_EDGES_BY_GROUP_AND_SEGMENT;
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("groupId", groupId );
		bindVars.put("segmentId", segmentId );
		db.query(aql, bindVars, Void.class);
		System.out.println("removeAllGraphEdgesByGroupIdAndSegmentId "+aql);
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
			String aql = Profile2Product.AQL_REMOVE_EDGES_BY_PROFILE;
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("fromProfileId", profileIdentity.getDocumentUUID() );
			db.query(aql, bindVars, Void.class);
			System.out.println("removeAllGraphEdgesBySegmentId "+aql);
		};
		return SegmentQueryManagement.applyConsumerForAllProfilesInSegment(segmentId, applyRemoveEdgeData);
	}
	
	/**
	 * @param profile
	 * @param product
	 * @param eventMetric
	 */
	public final static void updateEdgeData(Date createdAt, Profile profile, ProductItem product, EventMetric eventMetric) {
		batchUpdateEdgeData(createdAt, profile, product, eventMetric, 1);
	}
	
	/**
	 * @param fromProfile
	 * @param product
	 * @param score
	 */
	public final static void createRecommendedEdgeData(ProfileIdentity profileIdentity, ProductItem product, int score, String segmentId) {
		// run async
		TaskRunner.runInThreadPools(new Runnable() {
			@Override
			public void run() {
				try {
					String clickableMediaId = createTargetMediaUnitFromProduct(profileIdentity, product);
					
					// init the edge					
					Profile2Product insertedEdge = new Profile2Product(new Date(), profileIdentity, product, score, clickableMediaId, segmentId);
					
					// get edge collect of graph
					ArangoEdgeCollection arangoEdgeCol = Profile2Product.getArangoEdgeCollection();
					
					// add connection to edge collection 
					String key = insertedEdge.getKey();
					System.out.println("createRecommendedEdgeData insertedEdge.key " + key);
					
					Profile2Product updatedEdge = arangoEdgeCol.getEdge(key, Profile2Product.class);
					if(updatedEdge != null) {
						updatedEdge.setSegmentId(segmentId);
						updatedEdge.updateEventScore(score);
						updatedEdge.setTargetMediaUnitId(clickableMediaId);
						updatedEdge.setUpdatedAt(new Date());
						arangoEdgeCol.updateEdge(key, updatedEdge);
					}
					else {
						arangoEdgeCol.insertEdge(insertedEdge );
					}
				} catch (ArangoDBException e) {
					if(e.getErrorNum() != 1200) {
						System.err.println(e.getErrorNum() + " " + e.getErrorMessage());
					}
				}
			}
		});
	}

	static String createTargetMediaUnitFromProduct(ProfileIdentity profileIdentity, ProductItem product) {
		String landingPageUrl = product.getFullUrl();
		String landingPageName = product.getTitle();
		String productItemId = product.getId();
		String headlineImageUrl = product.getHeadlineImageUrl();
		String headlineVideoUrl = product.getHeadlineVideoUrl();
		String profileId = profileIdentity.getId();
		String visitorId = profileIdentity.getVisitorId();
		TargetMediaUnit targetMediaUnit = TargetMediaUnit.fromProduct(productItemId, headlineImageUrl, headlineVideoUrl, profileId , visitorId , landingPageUrl, landingPageName);
		String ok = TargetMediaUnitManagement.save(targetMediaUnit);
		return ok;
	}
	
	/**
	 * @param fromProfileId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Profile2Product> getRecommendedProductItemsForAdmin(String fromProfileId, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("fromProfileId", Profile.getDocumentUUID(fromProfileId));
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		CallbackQuery<Profile2Product> callback = new CallbackQuery<Profile2Product>() {
			@Override
			public Profile2Product apply(Profile2Product obj) {
				if(obj != null) {
					TargetMediaUnit media = TargetMediaUnitDaoUtil.getByIdForProductRecommendation(obj.getTargetMediaUnitId(), true);
					
					if(media != null) {
						obj.setTargetMediaUnit(media);
						ProductItem product = media.getProductItem();
						if(product != null) {
							obj.setProduct(product);
							return obj;
						}
					}
				}
				return null;
			}
		};
		String aql = ProfileGraphEdge.getGraphQueryRecommendation(G_PROFILE2PRODUCT);
		System.out.println(" getRecommendedProductItemsForAdmin \n "+aql);
		List<Profile2Product> rs = new ArangoDbCommand<Profile2Product>(db, aql, bindVars, Profile2Product.class, callback).getResultsAsList();
		return rs;
	}
	
	public static List<TargetMediaUnit> getRecommendedProductItemsForUser(String fromProfileId, int startIndex, int numberResult) {
		return getRecommendedProductItemsForUser(fromProfileId, startIndex, numberResult, false);
	}
	
	/**
	 * public get recommended items
	 * 
	 * @param fromProfileId
	 * @return
	 */
	public static List<TargetMediaUnit> getRecommendedProductItemsForUser(String fromProfileId, int startIndex, int numberResult, boolean withProductItem) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("fromProfileId", Profile.getDocumentUUID(fromProfileId));
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		List<TargetMediaUnit> targetMediaUnits = new ArrayList<TargetMediaUnit>();
		CallbackQuery<Profile2Product> callback = new CallbackQuery<Profile2Product>() {
			@Override
			public Profile2Product apply(Profile2Product obj) {
				String targetMediaUnitId = obj.getTargetMediaUnitId();
				TargetMediaUnit media = TargetMediaUnitDaoUtil.getByIdForProductRecommendation(targetMediaUnitId, false);
				if(media != null) {
					if(withProductItem) {
						ProductItem item = AssetItemManagement.getProductItemById(media.getRefProductItemId());
						media.setProductItem(item);
					}
					else {
						media.clearPrivateData();
					}
					targetMediaUnits.add(media);
				} 
				else {
					System.err.println("TargetMediaUnit is null, targetMediaUnitId " + targetMediaUnitId + " Profile2Product " + obj);
				}
				return null;
			}
		};
		
		String aql = ProfileGraphEdge.getGraphQueryRecommendation(G_PROFILE2PRODUCT);
		System.out.println(" getRecommendedTargetMediaUnits \n "+aql);
		new ArangoDbCommand<Profile2Product>(db, aql, bindVars, Profile2Product.class, callback).applyCallback();
		return targetMediaUnits;
	}
	
	/**
	 * @param profile
	 * @param eventMetric
	 * @return
	 */
	public static List<Profile2Product> query(Profile profile, int startIndex, int numberResult, EventMetric eventMetric) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("fromProfileId", profile.getDocumentUUID());
		bindVars.put("eventMetricId", eventMetric.getId());
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		CallbackQuery<Profile2Product> callback = new CallbackQuery<Profile2Product>() {
			@Override
			public Profile2Product apply(Profile2Product obj) {
				obj.setFromProfile(profile);
				
				String toProductId = obj.getToProductId();
				
				//System.out.println(toProductId);
				
				ProductItem product = AssetProductItemDaoUtil.getById(toProductId);
				obj.setProduct(product);
				return obj;
			}
		};
		
		String aql = ProfileGraphEdge.getGraphQuerySortByEventScore(G_PROFILE2PRODUCT);
		List<Profile2Product> rs = new ArangoDbCommand<Profile2Product>(db, aql, bindVars, Profile2Product.class, callback).getResultsAsList();
		return rs;
	}
	

	/**
	 * @param groupId
	 * @return
	 */
	public final static int setRecommenderDataFromAssetGroup(String groupId, String segmentId) {
		int countProfile = 0;
		int limit = SystemConfigsManagement.DEFAULT_ITEM_FOR_PROFILE;
		List<ProductItem> productItems = AssetProductItemDaoUtil.list("", groupId, 0, limit);
		for (ProductItem product : productItems) {
			int c = updateRecommendedEdgeForProduct(product, segmentId);
			System.out.println(" updateRecommendedEdgeForProduct " + product.getTitle() + " countProfile " + c) ;
			countProfile += c;
		}
		return countProfile;
	}

	/**
	 * @param product
	 * @return
	 */
	public final static int updateRecommendedEdgeForProduct(ProductItem product, String segmentId) {
		Consumer<? super ProfileIdentity> applyRecommendedEdgeData = profileIdentity -> {
			createRecommendedEdgeData(profileIdentity, product, 1, segmentId);
		};
		return SegmentQueryManagement.applyConsumerForAllProfilesInSegment(segmentId, applyRecommendedEdgeData);
	}

	/**
	 * @param profileId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Profile2Product> getProductEdges(String profileId, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("fromProfileId", Profile.getDocumentUUID(profileId) );
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		String aql = ProfileGraphEdge.getGraphQueryRecommendation(G_PROFILE2PRODUCT);
		List<Profile2Product> rs = new ArangoDbCommand<Profile2Product>(db, aql, bindVars, Profile2Product.class).getResultsAsList();
		return rs;
	}

	/**
	 * @param oldProfileId
	 * @param newProfileId
	 * @return
	 */
	public final static int updateFromOldProfileToNewProfile(String oldProfileId, String newProfileId) {
		ArangoEdgeCollection arangoEdgeCollection = Profile2Content.getArangoEdgeCollection();
		int numberResult = 200;
		int startIndex = 0;
		int c = 0;
		int maxLoop = 10;
		List<Profile2Product> list = getProductEdges(oldProfileId, startIndex, numberResult);
		while (!list.isEmpty()) {
			for (Profile2Product updatedEdge : list) {
				String key = updatedEdge.getKey();
				
				String updatedProfileId = Profile.getDocumentUUID(newProfileId);
				
				if( ! updatedProfileId.equals(updatedEdge.getFromProfileId()) ) {
					updatedEdge.setFromProfileId(updatedProfileId);
					updatedEdge.setUpdatedAt(new Date());
					try {
						arangoEdgeCollection.updateEdge(key, updatedEdge);
						c++;
					} catch (ArangoDBException e) {
						//e.printStackTrace();
						System.err.println(e.getErrorMessage());
					}
				}
			}
			
			startIndex = startIndex + numberResult;
			list = getProductEdges(oldProfileId, startIndex, numberResult);
			
			maxLoop--;
			if(maxLoop == 0) {
				break;
			}
		}
		return c;
	}
	
}
