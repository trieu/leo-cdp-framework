package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
 * Profile Graph to match creative contents and product items
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class ProfileGraphManagement {
	
	private static final String REMOVE_ALL = "remove_all";

	static Logger logger = LoggerFactory.getLogger(ProfileGraphManagement.class);

	private static final int QUERY_CACHE_TIME = 30;

	static CacheLoader<TargetMediaUnitQuery, List<TargetMediaUnit>> cacheRecommendLoader = new CacheLoader<>() {
		@Override
		public List<TargetMediaUnit> load(TargetMediaUnitQuery query) {
			return queryRecommendedProductItems(query);
		}
	};

	static LoadingCache<TargetMediaUnitQuery, List<TargetMediaUnit>> cacheRecommend = CacheBuilder.newBuilder()
			.maximumSize(1000000).expireAfterWrite(QUERY_CACHE_TIME, TimeUnit.SECONDS).build(cacheRecommendLoader);
	
	static CacheLoader<String, String> cacheProfileIdsLoader = new CacheLoader<>() {
		@Override
		public String load(String visitorId) {
			String fromProfileId = ProfileDaoUtil.getProfileIdByVisitorId(visitorId);
			return fromProfileId;
		}
	};

	static LoadingCache<String, String> cacheProfileIds = CacheBuilder.newBuilder()
			.maximumSize(1000000).expireAfterWrite(QUERY_CACHE_TIME, TimeUnit.DAYS).build(cacheProfileIdsLoader);

	public static List<TargetMediaUnit> queryRecommendedProductItems(TargetMediaUnitQuery query) {
		String visitorId = query.getVisitorId();
		
		String fromProfileId = null;
		try {
			fromProfileId = cacheProfileIds.get(visitorId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (fromProfileId != null) {
			int startIndex = query.getStartIndex();
			int numberResult = query.getNumberResult();
			List<TargetMediaUnit> rsList = GraphProfile2Product.getRecommendedProductItemsForUser(fromProfileId, startIndex, numberResult);
			return rsList;
		}
		return new ArrayList<>(0);
	}

	/**
	 * 
	 * for public query
	 * 
	 * @param pmIds
	 * @param visitorId
	 * @param sourceUrl
	 * @return
	 */
	public static List<TargetMediaUnit> queryRecommendedProductItems(String observerId, String visitorId, String touchpointUrl, int startIndex, int numberResult) {
		List<TargetMediaUnit> rsList = null;
		try {
			rsList = cacheRecommend.get(new TargetMediaUnitQuery(visitorId, startIndex, numberResult));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rsList != null ? rsList : new ArrayList<>(0);
	}

	/**
	 * @param groupId
	 * @param segmentId
	 * @return
	 */
	public static int setRecommendationItems(boolean isProductGroup, String groupId, String segmentId) {
		int c = 0;
		if (isProductGroup) {
			c = GraphProfile2Product.setRecommenderDataFromAssetGroup(groupId, segmentId);
		}
		else {
			c = GraphProfile2Content.setRecommenderDataFromAssetGroup(groupId, segmentId);
		}
		return c;
	}

	/**
	 * for admin
	 * 
	 * @param fromProfileId
	 * @return
	 */
	public static List<Profile2Product> getRecommendedProductItemsByIndexScore(String fromProfileId, int startIndex, int numberResult) {
		List<Profile2Product> rsList = GraphProfile2Product.getRecommendedProductItemsForAdmin(fromProfileId, startIndex, numberResult);
		return rsList;
	}

	/**
	 * @param fromProfileId
	 * @return
	 */
	public static List<Profile2Conversion> getPurchasedProductItems(String fromProfileId, int startIndex, int numberResult) {
		List<Profile2Conversion> rsList = GraphProfile2Conversion.getPurchasedProductItems(fromProfileId, startIndex, numberResult);
		return rsList;
	}

	/**
	 * @param fromProfileId
	 * @return
	 */
	public static List<Profile2Content> getRecommendedContentItemsByIndexScore(String fromProfileId, int startIndex, int numberResult) {
		List<Profile2Content> rsList = GraphProfile2Content.getRecommendedContentItems(fromProfileId, startIndex,numberResult);
		return rsList;
	}


	/**
	 * @param observerId
	 * @param visitorId
	 * @param sourceUrl
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<TargetMediaUnit> queryRecommendedContents(String observerId, String visitorId, String sourceUrl, int startIndex, int numberResult) {
		String fromProfileId = null;
		try {
			fromProfileId = cacheProfileIds.get(visitorId);
		} catch (Exception e) {
			System.err.println("No profile is founded with visitorId " + visitorId);
		}
		if (fromProfileId != null) {
			boolean rankedByIndexScore = true;//profile.getRecommendationModel() == 1;
			List<TargetMediaUnit> rsList = GraphProfile2Content.getRecommendedAssetContents(fromProfileId, startIndex, numberResult, rankedByIndexScore);
			return rsList;
		}
		return new ArrayList<>(0);
	}

	/**
	 * @param profile
	 * @param product
	 * @param eventMetric
	 * @param transactionId
	 */
	public final static void updateTransactionalData(Date createdAt, Profile profile, ProductItem product, int quantity, EventMetric eventMetric, String transactionId, double transactionValue, String currency) {
		GraphProfile2Conversion.updateEdgeData(createdAt, profile, product, quantity, eventMetric, transactionId, transactionValue, currency);
		int score = DateTimeUtil.currentUnixTimestamp();
		GraphProfile2Product.batchUpdateEdgeData(createdAt, profile, product, EventMetricManagement.RECOMMENDATION, score);
	}

	/**
	 * @param profile
	 * @param product
	 * @param eventMetric
	 */
	public final static void updateEdgeProductData(Date createdAt, Profile profile, ProductItem product, EventMetric eventMetric) {
		GraphProfile2Product.batchUpdateEdgeData(createdAt, profile, product, eventMetric);
	}
	
	/**
	 * @param fromProfile
	 * @param creative
	 * @param eventMetric
	 */
	public final static void updateEdgeContentData(ProfileIdentity profileIdentity, AssetContent content) {
		int score = -1 * DateTimeUtil.currentUnixTimestamp();
		GraphProfile2Content.updateEdgeData(profileIdentity, content, EventMetricManagement.RECOMMENDATION, score);
	}

	/**
	 * add event to profile graph edge collection and update ranking of item (set indexScore = -1)
	 * 
	 * @param profile
	 * @param product
	 * @param eventMetric
	 * @param score
	 */
	public final static void updateEdgeDataForRecommendation(Date createdAt, Profile profile, ProductItem product, EventMetric eventMetric, int score) {
		GraphProfile2Product.batchUpdateEdgeData(createdAt, profile, product, eventMetric);
		logger.info("profile " + profile.getId() + " product " + product.getId() + " score " + score);
		GraphProfile2Product.batchUpdateEdgeData(createdAt, profile, product, EventMetricManagement.RECOMMENDATION, score);
	}



	/**
	 * @param profileId
	 * @param recommendationModel
	 * @param graphName
	 * @param updateItemMap
	 * @return
	 */
	public final static boolean updateItemRanking(String profileId, int recommendationModel, String graphName, Map<String, Integer> updateItemMap) {
		boolean rs = false;
		// update profile recommendation model
		ProfileSingleView profile = ProfileDaoUtil.getProfileById(profileId);
		if (profile != null) {
			// update recommendationModel
			if (profile.getRecommendationModel() != 1 && recommendationModel == 1) {
				profile.setRecommendationModel(1);
				ProfileDaoUtil.saveProfile(profile);
			}
			// update item ranking for product graph
			if (Profile2Product.GRAPH_NAME.equals(graphName)) {
				updateItemMap.forEach((key, indexScore) -> {
					GraphProfile2Product.updateRanking(key, indexScore);
				});
				rs = true;
			}
			// update item ranking for content graph
			else if (Profile2Content.GRAPH_NAME.equals(graphName)) {
				updateItemMap.forEach((key, indexScore) -> {
					GraphProfile2Content.updateRanking(key, indexScore);
				});
				rs = true;
			}
		}
		return rs;
	}

	/**
	 * @param groupId
	 * @param segmentId
	 * @return
	 */
	public static boolean removeRecommendationItemsForGroup(String groupId) {
		AssetGroup group = AssetGroupManagement.getAssetGroupById(groupId);
		if (group.isProduct()) {
			GraphProfile2Product.removeAllGraphEdgesByGroupId(groupId);
			return true;
		}
		else if (group.isContent()) {
			GraphProfile2Content.removeAllGraphEdgesByGroupId(groupId);
			return true;
		}
		return false;
	}
	
	/**
	 * @param groupId
	 * @param segmentId
	 * @return
	 */
	public static void removeRecommendationItems(boolean isProductGroup, String groupId, String segmentId) {
		if(REMOVE_ALL.equals(groupId)) {
			if (isProductGroup) {
				GraphProfile2Product.removeAllGraphEdgesBySegmentId(segmentId);
			}
			else {
				GraphProfile2Content.removeAllGraphEdgesBySegmentId(segmentId);
			}
		}
		else {
			if (isProductGroup) {
				GraphProfile2Product.removeAllGraphEdgesByGroupIdAndSegmentId(groupId, segmentId);
			}
			else {
				GraphProfile2Content.removeAllGraphEdgesByGroupIdAndSegmentId(groupId, segmentId);
			}
		}
	}
	
	/**
	 * update profile graph data
	 * 
	 * @param sourceProfileId
	 * @param destProfileId
	 */
	public static void updateEdgeDataProfileToProfile(String sourceProfileId, String destProfileId) {
		// Purchased Products
		GraphProfile2Conversion.updateFromOldProfileToNewProfile(sourceProfileId, destProfileId);
		// recommended contents
		GraphProfile2Content.updateFromOldProfileToNewProfile(sourceProfileId, destProfileId);
		// recommended products
		GraphProfile2Product.updateFromOldProfileToNewProfile(sourceProfileId, destProfileId);
		// touchpointhub graph
		GraphProfile2TouchpointHub.updateFromOldProfileToNewProfile(sourceProfileId, destProfileId);
	}

	// TODO add caching with Redis here
}
