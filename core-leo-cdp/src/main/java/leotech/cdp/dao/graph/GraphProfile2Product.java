package leotech.cdp.dao.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoEdgeCollection;
import com.arangodb.entity.EdgeDefinition;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.dao.TargetMediaUnitDaoUtil;
import leotech.cdp.domain.SegmentQueryManagement;
import leotech.cdp.domain.TargetMediaUnitManagement;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.graph.ProductRecommendation;
import leotech.cdp.model.graph.Profile2Product;
import leotech.cdp.model.graph.ProfileGraphEdge;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.config.AqlTemplate;
import leotech.system.domain.SystemConfigsManagement;
import leotech.system.util.TaskRunner;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;

/**
 * Data Access Object for managing Graph Edges between Profiles and Products.
 * Refactored for performance and maintainability. 
 * 
 * @author tantrieuf31
 * @since 2021
 */
public final class GraphProfile2Product extends AbstractCdpDatabaseUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphProfile2Product.class);

	private static final String GRAPH_NAME = Profile2Product.GRAPH_NAME;
	private static final String COLLECTION_NAME = Profile2Product.COLLECTION_NAME;

	// AQL Constants
	private static final String PARAM_FROM_PROFILE_ID = "fromProfileId";
	private static final String PARAM_GROUP_ID = "groupId";
	private static final String PARAM_SEGMENT_ID = "segmentId";
	private static final String PARAM_START_INDEX = "startIndex";
	private static final String PARAM_NUMBER_RESULT = "numberResult";

	// This allows us to insert or update in a single DB round trip, atomic and fast.
	private static final String AQL_UPSERT_EDGE = "UPSERT { _from: @fromId, _to: @toId } "
			+ "INSERT { _from: @fromId, _to: @toId, eventScore: @score, indexScore: @indexScore, createdAt: @now, updatedAt: @now } "
			+ "UPDATE { eventScore: OLD.eventScore + @score, indexScore: (@indexScore > 0 ? @indexScore : OLD.indexScore), updatedAt: @now } "
			+ "IN " + COLLECTION_NAME;
	
	static final String AQL_GET_RECOMMENDED_PRODUCTS_FOR_PROFILE = AqlTemplate.get("AQL_GET_RECOMMENDED_PRODUCTS_FOR_PROFILE");

	private GraphProfile2Product() {
		// Utility class
	}

	/**
	 * Initialize graph data schema and indices.
	 */
	public static void initGraph(ArangoDatabase db) {
		try {
			if (!db.graph(GRAPH_NAME).exists()) {
				EdgeDefinition edgeDefinition = new EdgeDefinition().collection(COLLECTION_NAME)
						.from(Profile.COLLECTION_NAME).to(ProductItem.COLLECTION_NAME);

				Collection<EdgeDefinition> edgeDefinitions = new ArrayList<>();
				edgeDefinitions.add(edgeDefinition);

				db.createGraph(GRAPH_NAME, edgeDefinitions, null);
				LOGGER.info("Graph {} created successfully.", GRAPH_NAME);
			}
			Profile2Product.initIndex();
		} catch (ArangoDBException e) {
			LOGGER.error("Failed to init graph {}", GRAPH_NAME, e);
		}
	}

	// =================================================================================
	// Update / Insert Operations
	// =================================================================================

	public static void batchUpdateEdgeData(Date createdAt, Profile profile, ProductItem product,
			EventMetric eventMetric) {
		batchUpdateEdgeData(createdAt, profile, product, eventMetric, 1, 0);
	}

	public static void batchUpdateEdgeData(Date createdAt, Profile profile, ProductItem product,
			EventMetric eventMetric, int indexScore) {
		batchUpdateEdgeData(createdAt, profile, product, eventMetric, 1, indexScore);
	}

	/**
	 * Optimized batch update. Instead of looping N times performing N DB reads and
	 * N DB writes, we calculate the total score and perform a single AQL UPSERT.
	 */
	public static void batchUpdateEdgeData(Date createdAt, Profile profile, ProductItem product,
			EventMetric eventMetric, int eventCount, int indexScore) {
		if (profile.getStatus() != Profile.STATUS_ACTIVE) {
			return;
		}

		try {
			ArangoDatabase db = getCdpDatabase();

			// Calculate total score to add
			double totalScoreToAdd = eventMetric.getScore() * eventCount;

			Map<String, Object> bindVars = new HashMap<>();
			bindVars.put("fromId", profile.getDocumentUUID());
			bindVars.put("toId", product.getDocumentUUID());
			bindVars.put("score", totalScoreToAdd);
			bindVars.put("indexScore", indexScore);
			bindVars.put("now", new Date());

			db.query(AQL_UPSERT_EDGE, bindVars, Void.class);
			LOGGER.debug("Upserted edge: Profile {} -> Product {}, Added Score: {}", profile.getId(), product.getId(),
					totalScoreToAdd);

		} catch (ArangoDBException e) {
			LOGGER.error("Error in batchUpdateEdgeData for profile {}", profile.getId(), e);
		}
	}

	public static void updateEdgeData(Date createdAt, Profile profile, ProductItem product, EventMetric eventMetric) {
		batchUpdateEdgeData(createdAt, profile, product, eventMetric, 1, 0);
	}

	public static boolean updateRanking(String key, int indexScore) {
		try {
			ArangoEdgeCollection edgeCollection = Profile2Product.getArangoEdgeCollection();
			Profile2Product edge = edgeCollection.getEdge(key, Profile2Product.class);

			if (edge != null) {
				edge.setIndexScore(indexScore);
				edge.setUpdatedAt(new Date());
				edgeCollection.updateEdge(key, edge);
				return true;
			}
		} catch (ArangoDBException e) {
			LOGGER.error("Failed to update ranking for key {}", key, e);
		}
		return false;
	}

	/**
	 * Async creation of recommended edge data.
	 */
	public static void createRecommendedEdgeData(ProfileIdentity profileIdentity, ProductItem product, int score,
			String segmentId) {
		TaskRunner.runInThreadPools(() -> {
			try {
				String clickableMediaId = createTargetMediaUnitFromProduct(profileIdentity, product);

				// Use the entity helper to generate the key deterministically if needed,
				// or rely on AQL Upsert logic similar to batchUpdate.
				// For now, keeping Java logic but cleaning it up.

				Profile2Product edgePrototype = new Profile2Product(new Date(), profileIdentity, product, score,
						clickableMediaId, segmentId);
				String key = edgePrototype.getKey();

				ArangoEdgeCollection col = Profile2Product.getArangoEdgeCollection();
				Profile2Product existingEdge = col.getEdge(key, Profile2Product.class);

				if (existingEdge != null) {
					existingEdge.setSegmentId(segmentId);
					existingEdge.updateEventScore(score);
					existingEdge.setTargetMediaUnitId(clickableMediaId);
					existingEdge.setUpdatedAt(new Date());
					col.updateEdge(key, existingEdge);
				} else {
					col.insertEdge(edgePrototype);
				}

				LOGGER.debug("Recommended Edge processed. Key: {}", key);

			} catch (ArangoDBException e) {
				// Ignore duplicate key errors (1200) or race conditions gracefully
				if (e.getErrorNum() != 1200) {
					LOGGER.error("Error creating recommended edge data", e);
				}
			} catch (Exception e) {
				LOGGER.error("Unexpected error in createRecommendedEdgeData", e);
			}
		});
	}

	private static String createTargetMediaUnitFromProduct(ProfileIdentity profileIdentity, ProductItem product) {
		TargetMediaUnit targetMediaUnit = TargetMediaUnit.fromProduct(product.getId(), product.getHeadlineImageUrl(),
				product.getHeadlineVideoUrl(), profileIdentity.getId(), profileIdentity.getVisitorId(),
				product.getFullUrl(), product.getTitle());
		return TargetMediaUnitManagement.save(targetMediaUnit);
	}

	// =================================================================================
	// Read / Query Operations
	// =================================================================================

	/**
	 * @param fromProfileId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Profile2Product> getRecommendedProductItemsForAdmin(String fromProfileId, int startIndex,
			int numberResult) {
		ArangoDatabase db = getCdpDatabase();

		// Java 11: Map.of is more concise for small maps
		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put(PARAM_FROM_PROFILE_ID, Profile.getDocumentUUID(fromProfileId));
		bindVars.put(PARAM_START_INDEX, startIndex);
		bindVars.put(PARAM_NUMBER_RESULT, numberResult);

		String aql = ProfileGraphEdge.getGraphQueryRecommendation(GRAPH_NAME);

		CallbackQuery<Profile2Product> callback = new CallbackQuery<Profile2Product>() {
			@Override
			public Profile2Product apply(Profile2Product edge) {
				if (edge != null && edge.getTargetMediaUnitId() != null) {
					TargetMediaUnit media = TargetMediaUnitDaoUtil
							.getByIdForProductRecommendation(edge.getTargetMediaUnitId(), true);
					if (media != null && media.getProductItem() != null) {
						edge.setTargetMediaUnit(media);
						edge.setProduct(media.getProductItem());
						return edge;
					}
				}
				return null;
			}
		};

		return new ArangoDbCommand<>(db, aql, bindVars, Profile2Product.class, callback).getResultsAsList();
	}


	
	

	/**
	 * @param fromProfileId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<TargetMediaUnit> getRecommendedProductItemsForProfile(String fromProfileId, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();

		// Using HashMap for compatibility with the rest of your DAO pattern
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put(PARAM_FROM_PROFILE_ID, Profile.getDocumentUUID(fromProfileId));
		bindVars.put(PARAM_START_INDEX, startIndex);
		bindVars.put(PARAM_NUMBER_RESULT, numberResult);

		String aql = AQL_GET_RECOMMENDED_PRODUCTS_FOR_PROFILE;
		List<ProductRecommendation> resultList = new ArangoDbCommand<>(db, aql, bindVars, ProductRecommendation.class).getResultsAsList();

		// mapping from ProductRecommendation to TargetMediaUnit
		return resultList.stream().map(r->{return r.getTargetMediaUnit();}).filter(e->{ return e != null;}).collect(Collectors.toList());
	}

	/**
	 * @param profile
	 * @param startIndex
	 * @param numberResult
	 * @param eventMetric
	 * @return
	 */
	public static List<Profile2Product> queryProfile2Products(Profile profile, int startIndex, int numberResult, EventMetric eventMetric) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put(PARAM_FROM_PROFILE_ID, profile.getDocumentUUID());
		bindVars.put("eventMetricId", eventMetric.getId());
		bindVars.put(PARAM_START_INDEX, startIndex);
		bindVars.put(PARAM_NUMBER_RESULT, numberResult);

		CallbackQuery<Profile2Product> callback = new CallbackQuery<Profile2Product>() {
			@Override
			public Profile2Product apply(Profile2Product edge) {
				if (edge != null) {
					edge.setFromProfile(profile);
					String toProductId = edge.getToProductId();
					ProductItem product = AssetProductItemDaoUtil.getById(toProductId);
					edge.setProduct(product);
					return edge;
				}
				return null;
			}
		};

		String aql = ProfileGraphEdge.getGraphQuerySortByEventScore(GRAPH_NAME);
		return new ArangoDbCommand<>(db, aql, bindVars, Profile2Product.class, callback).getResultsAsList();
	}

	/**
	 * @param profileId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Profile2Product> getProductEdges(String profileId, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put(PARAM_FROM_PROFILE_ID, Profile.getDocumentUUID(profileId));
		bindVars.put(PARAM_START_INDEX, startIndex);
		bindVars.put(PARAM_NUMBER_RESULT, numberResult);

		String aql = ProfileGraphEdge.getGraphQueryRecommendation(GRAPH_NAME);
		return new ArangoDbCommand<>(db, aql, bindVars, Profile2Product.class).getResultsAsList();
	}

	// =================================================================================
	// Deletion / Maintenance Operations
	// =================================================================================

	/**
	 * @param groupId
	 */
	public static void removeAllGraphEdgesByGroupId(String groupId) {
		executeDeleteQuery(Profile2Product.AQL_REMOVE_EDGES_BY_GROUP, Map.of(PARAM_GROUP_ID, groupId));
	}

	public static void removeAllGraphEdgesByGroupIdAndSegmentId(String groupId, String segmentId) {
		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put(PARAM_GROUP_ID, groupId);
		bindVars.put(PARAM_SEGMENT_ID, segmentId);
		executeDeleteQuery(Profile2Product.AQL_REMOVE_EDGES_BY_GROUP_AND_SEGMENT, bindVars);
	}

	/**
	 * @param segmentId
	 * @return
	 */
	public static int removeAllGraphEdgesBySegmentId(String segmentId) {
		Consumer<? super ProfileIdentity> removeAction = profileIdentity -> {
			executeDeleteQuery(Profile2Product.AQL_REMOVE_EDGES_BY_PROFILE,
					Map.of(PARAM_FROM_PROFILE_ID, profileIdentity.getDocumentUUID()));
		};
		return SegmentQueryManagement.applyConsumerForAllProfilesInSegment(segmentId, removeAction);
	}

	/**
	 * @param aql
	 * @param bindVars
	 */
	private static void executeDeleteQuery(String aql, Map<String, Object> bindVars) {
		ArangoDatabase db = getCdpDatabase();
		db.query(aql, bindVars, Void.class);
		LOGGER.info("Executed Delete AQL: {} Params: {}", aql, bindVars);
	}

	/**
	 * @param groupId
	 * @param segmentId
	 * @return
	 */
	public static int setRecommenderDataFromAssetGroup(String groupId, String segmentId) {
		int countProfile = 0;
		int limit = SystemConfigsManagement.DEFAULT_ITEM_FOR_PROFILE;
		List<ProductItem> productItems = AssetProductItemDaoUtil.list("", groupId, 0, limit);

		for (ProductItem product : productItems) {
			int c = updateRecommendedEdgeForProduct(product, segmentId);
			LOGGER.debug("Updated recommended edge for product: {}. Count: {}", product.getTitle(), c);
			countProfile += c;
		}
		return countProfile;
	}

	/**
	 * @param product
	 * @param segmentId
	 * @return
	 */
	public static int updateRecommendedEdgeForProduct(ProductItem product, String segmentId) {
		Consumer<? super ProfileIdentity> action = profileIdentity -> createRecommendedEdgeData(profileIdentity,
				product, 1, segmentId);
		return SegmentQueryManagement.applyConsumerForAllProfilesInSegment(segmentId, action);
	}

	/**
	 * Migrates edges from an old profile to a new profile (e.g., after profile merging). 
	 * 
	 * @param oldProfileId The source profile ID.
	 * @param newProfileId The destination profile ID.
	 * @return Number of edges updated.
	 */
	public static int updateFromOldProfileToNewProfile(String oldProfileId, String newProfileId) {
		ArangoEdgeCollection edgeCollection = Profile2Product.getArangoEdgeCollection();
		String newProfileUuid = Profile.getDocumentUUID(newProfileId);

		int batchSize = 200;
		int updatedCount = 0;
		int maxLoopSafety = 1000; // Prevent infinite loops

		// IMPORTANT: Always query startIndex 0.
		// As we move edges to the new profile, they disappear from the "oldProfileId"
		// query results.
		// Pagination logic (increasing startIndex) would skip records.
		List<Profile2Product> edges = getProductEdges(oldProfileId, 0, batchSize);

		while (!edges.isEmpty() && maxLoopSafety > 0) {
			for (Profile2Product edge : edges) {
				if (!newProfileUuid.equals(edge.getFromProfileId())) {
					try {
						edge.setFromProfileId(newProfileUuid);
						edge.setUpdatedAt(new Date());
						edgeCollection.updateEdge(edge.getKey(), edge);
						updatedCount++;
					} catch (ArangoDBException e) {
						LOGGER.error("Failed to migrate edge {}", edge.getKey(), e);
					}
				}
			}

			// Re-fetch the next batch (which is effectively the "first" batch remaining for
			// the old profile)
			edges = getProductEdges(oldProfileId, 0, batchSize);
			maxLoopSafety--;
		}

		if (maxLoopSafety == 0) {
			LOGGER.warn("Migration terminated due to safety limit. Profile: {} -> {}", oldProfileId, newProfileId);
		}

		return updatedCount;
	}
}