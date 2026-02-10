package leotech.cdp.domain;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.graph.GraphProfile2Content;
import leotech.cdp.dao.graph.GraphProfile2Conversion;
import leotech.cdp.dao.graph.GraphProfile2Product;
import leotech.cdp.dao.graph.GraphProfile2TouchpointHub;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.model.graph.Profile2Content;
import leotech.cdp.model.graph.Profile2Conversion;
import leotech.cdp.model.graph.Profile2Product;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.cdp.query.TargetMediaUnitQuery;
import rfx.core.util.DateTimeUtil;

/**
 * Manages the Graph relationships between Profiles and Assets (Products,
 * Content, Conversions). Acting as a Service layer over Graph DAOs with caching
 * support. 
 * 
 * @author tantrieuf31
 * @since 2021
 */
public final class ProfileGraphManagement {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileGraphManagement.class);

	private static final String CMD_REMOVE_ALL = "remove_all";
	private static final int CACHE_DURATION_SECONDS = 30;
	private static final int CACHE_DURATION_DAYS = 30;
	private static final int MAX_CACHE_SIZE = 1_000_000;

	// =================================================================================
	// Caches
	// =================================================================================

	// Cache for Profile ID lookup based on Visitor ID
	private static final LoadingCache<String, String> PROFILE_ID_CACHE = CacheBuilder.newBuilder()
			.maximumSize(MAX_CACHE_SIZE).expireAfterWrite(CACHE_DURATION_DAYS, TimeUnit.DAYS)
			.build(new CacheLoader<>() {
				@Override
				public String load(String visitorId) {
					return ProfileDaoUtil.getProfileIdByVisitorId(visitorId);
				}
			});

	// Cache for Product Recommendations
	private static final LoadingCache<TargetMediaUnitQuery, List<TargetMediaUnit>> RECOMMENDATION_CACHE = CacheBuilder
			.newBuilder().maximumSize(MAX_CACHE_SIZE).expireAfterWrite(CACHE_DURATION_SECONDS, TimeUnit.SECONDS)
			.build(new CacheLoader<>() {
				@Override
				public List<TargetMediaUnit> load(TargetMediaUnitQuery query) {
					return fetchRecommendedProductsFromDb(query);
				}
			});

	private ProfileGraphManagement() {
		// Prevent instantiation
	}

	// =================================================================================
	// Read Operations (Recommendations & Queries)
	// =================================================================================

	/**
	 * Public API to get cached product recommendations.
	 */
	public static List<TargetMediaUnit> queryRecommendedProductItems(String observerId, String visitorId, String touchpointUrl, int startIndex, int numberResult) {
		try {
			TargetMediaUnitQuery query = new TargetMediaUnitQuery(visitorId, startIndex, numberResult);
			return RECOMMENDATION_CACHE.get(query);
		} catch (ExecutionException e) {
			LOGGER.error("Error fetching recommended products for visitorId: {}", visitorId, e);
			return Collections.emptyList();
		}
	}

	/**
	 * Internal method used by CacheLoader to fetch data from DB.
	 */
	private static List<TargetMediaUnit> fetchRecommendedProductsFromDb(TargetMediaUnitQuery query) {
		String profileId = getProfileId(query.getVisitorId());
		if (profileId != null) {
			return GraphProfile2Product.getRecommendedProductItemsForProfile(profileId, query.getStartIndex(),query.getNumberResult());
		}
		return Collections.emptyList();
	}

	public static List<TargetMediaUnit> queryRecommendedContents(String observerId, String visitorId, String sourceUrl,
			int startIndex, int numberResult) {
		String profileId = getProfileId(visitorId);
		if (profileId != null) {
			// TODO: Refactor to allow dynamic configuration of ranking strategy
			boolean rankedByIndexScore = true;
			return GraphProfile2Content.getRecommendedAssetContents(profileId, startIndex, numberResult,
					rankedByIndexScore);
		}
		return Collections.emptyList();
	}

	public static List<Profile2Product> getRecommendedProductItemsByIndexScore(String fromProfileId, int startIndex,
			int numberResult) {
		List<Profile2Product> list = GraphProfile2Product.getRecommendedProductItemsForAdmin(fromProfileId, startIndex,
				numberResult);
		return list != null ? list : Collections.emptyList();
	}

	public static List<Profile2Content> getRecommendedContentItemsByIndexScore(String fromProfileId, int startIndex,
			int numberResult) {
		List<Profile2Content> list = GraphProfile2Content.getRecommendedContentItems(fromProfileId, startIndex,
				numberResult);
		return list != null ? list : Collections.emptyList();
	}

	public static List<Profile2Conversion> getPurchasedProductItems(String fromProfileId, int startIndex,
			int numberResult) {
		List<Profile2Conversion> list = GraphProfile2Conversion.getPurchasedProductItems(fromProfileId, startIndex,
				numberResult);
		return list != null ? list : Collections.emptyList();
	}

	// =================================================================================
	// Write Operations (Updates)
	// =================================================================================

	public static void updateTransactionalData(Date createdAt, Profile profile, ProductItem product, int quantity,
			EventMetric eventMetric, String transactionId, double transactionValue, String currency) {

		GraphProfile2Conversion.updateEdgeData(createdAt, profile, product, quantity, eventMetric, transactionId,
				transactionValue, currency);

		// Post-purchase: Update recommendation score
		int score = DateTimeUtil.currentUnixTimestamp();
		GraphProfile2Product.batchUpdateEdgeData(createdAt, profile, product, EventMetricManagement.RECOMMENDATION,
				score);
	}

	public static void updateEdgeProductData(Date createdAt, Profile profile, ProductItem product,
			EventMetric eventMetric) {
		GraphProfile2Product.batchUpdateEdgeData(createdAt, profile, product, eventMetric);
	}

	public static void updateEdgeContentData(ProfileIdentity profileIdentity, AssetContent content) {
		// Default recommendation score is negative timestamp to reverse sort order (newest first)
		int score = -1 * DateTimeUtil.currentUnixTimestamp();
		GraphProfile2Content.updateEdgeData(profileIdentity, content, EventMetricManagement.RECOMMENDATION, score);
	}

	public static void updateEdgeDataForRecommendation(Date createdAt, Profile profile, ProductItem product, EventMetric eventMetric, int score) {
		LOGGER.info("Update recommendation: profile={}, product={}, score={}", profile.getId(), product.getId(),score);

		GraphProfile2Product.batchUpdateEdgeData(createdAt, profile, product, eventMetric);
		GraphProfile2Product.batchUpdateEdgeData(createdAt, profile, product, EventMetricManagement.RECOMMENDATION, score);
	}

	public static boolean updateItemRanking(String profileId, int recommendationModel, String graphName,
			Map<String, Integer> updateItemMap) {
		ProfileSingleView profile = ProfileDaoUtil.getProfileById(profileId);
		if (profile == null) {
			return false;
		}

		// 1. Update Profile Model if changed
		if (profile.getRecommendationModel() != 1 && recommendationModel == 1) {
			profile.setRecommendationModel(1);
			ProfileDaoUtil.saveProfile(profile);
		}

		// 2. Update Graph Rankings
		if (Profile2Product.GRAPH_NAME.equals(graphName)) {
			updateItemMap.forEach(GraphProfile2Product::updateRanking);
			return true;
		} else if (Profile2Content.GRAPH_NAME.equals(graphName)) {
			updateItemMap.forEach(GraphProfile2Content::updateRanking);
			return true;
		}

		return false;
	}

	public static void updateEdgeDataProfileToProfile(String sourceProfileId, String destProfileId) {
		LOGGER.info("Merging graph data from profile {} to {}", sourceProfileId, destProfileId);
		GraphProfile2Conversion.updateFromOldProfileToNewProfile(sourceProfileId, destProfileId);
		GraphProfile2Content.updateFromOldProfileToNewProfile(sourceProfileId, destProfileId);
		GraphProfile2Product.updateFromOldProfileToNewProfile(sourceProfileId, destProfileId);
		GraphProfile2TouchpointHub.updateFromOldProfileToNewProfile(sourceProfileId, destProfileId);
	}

	// =================================================================================
	// Management Operations (Set/Remove)
	// =================================================================================

	public static int setRecommendationItems(boolean isProductGroup, String groupId, String segmentId) {
		if (isProductGroup) {
			return GraphProfile2Product.setRecommenderDataFromAssetGroup(groupId, segmentId);
		} else {
			return GraphProfile2Content.setRecommenderDataFromAssetGroup(groupId, segmentId);
		}
	}

	public static boolean removeRecommendationItemsForGroup(String groupId) {
		AssetGroup group = AssetGroupManagement.getAssetGroupById(groupId);
		if (group == null) {
			return false;
		}

		if (group.isProduct()) {
			GraphProfile2Product.removeAllGraphEdgesByGroupId(groupId);
			return true;
		} else if (group.isContent()) {
			GraphProfile2Content.removeAllGraphEdgesByGroupId(groupId);
			return true;
		}
		return false;
	}

	public static void removeRecommendationItems(boolean isProductGroup, String groupId, String segmentId) {
		boolean removeAll = CMD_REMOVE_ALL.equals(groupId);

		if (isProductGroup) {
			if (removeAll) {
				GraphProfile2Product.removeAllGraphEdgesBySegmentId(segmentId);
			} else {
				GraphProfile2Product.removeAllGraphEdgesByGroupIdAndSegmentId(groupId, segmentId);
			}
		} else {
			// Content Group
			if (removeAll) {
				GraphProfile2Content.removeAllGraphEdgesBySegmentId(segmentId);
			} else {
				GraphProfile2Content.removeAllGraphEdgesByGroupIdAndSegmentId(groupId, segmentId);
			}
		}
	}

	// =================================================================================
	// Helper Methods
	// =================================================================================

	private static String getProfileId(String visitorId) {
		try {
			return PROFILE_ID_CACHE.get(visitorId);
		} catch (ExecutionException e) {
			LOGGER.warn("Failed to retrieve profileId for visitorId: {}", visitorId);
			return null;
		} catch (Exception e) {
			LOGGER.error("Unexpected error in profile cache lookup", e);
			return null;
		}
	}
}