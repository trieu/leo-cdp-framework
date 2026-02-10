package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;

import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.config.AqlTemplate;
import leotech.system.util.LogUtil;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;
import leotech.system.util.database.ArangoDbUtil;
import rfx.core.util.StringUtil;

/**
 * Product Item Database
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class AssetProductItemDaoUtil extends AssetItemDao {

	static final String PRODUCT_ITEM = "product-item";

	static final String AQL_FIND_KEY_AQL = ArangoDbUtil.contentFindKeyAql(ProductItem.COLLECTION_NAME);

	static final String AQL_GET_PRODUCT_ITEMS_IN_GROUP = AqlTemplate.get("AQL_GET_PRODUCT_ITEMS_IN_GROUP");
	static final String AQL_COUNT_PRODUCT_ITEMS_IN_GROUP = AqlTemplate.get("AQL_COUNT_PRODUCT_ITEMS_IN_GROUP");

	static final String AQL_GET_PRODUCT_ITEM_BY_ID = AqlTemplate.get("AQL_GET_PRODUCT_ITEM_BY_ID");
	static final String AQL_GET_PRODUCT_ITEM_BY_FULL_URL = AqlTemplate.get("AQL_GET_PRODUCT_ITEM_BY_FULL_URL");
	static final String AQL_GET_PRODUCT_ITEM_BY_ID_AND_TYPE = AqlTemplate.get("AQL_GET_PRODUCT_ITEM_BY_ID_AND_TYPE");

	static final String AQL_GET_PRODUCT_ITEMS = AqlTemplate.get("AQL_GET_PRODUCT_ITEMS");
	static final String AQL_SEARCH_PRODUCT_ITEMS_BY_KEYWORDS = AqlTemplate.get("AQL_SEARCH_PRODUCT_ITEMS_BY_KEYWORDS");
	static final String AQL_GET_PRODUCT_ITEMS_BY_CAMPAIGN = AqlTemplate.get("AQL_GET_PRODUCT_ITEMS_BY_CAMPAIGN");

	public static String save(ProductItem item) {

		if (!item.dataValidation()) {
			return null;
		}

		ArangoDatabase db = getCdpDatabase();
		ArangoCollection col = item.getDbCollection();

		if (col == null) {
			System.err.println("AssetProductItemDaoUtil save ArangoCollection is NULL");
			return null;
		}

		String id = item.getId();

		// ---- side effects ----
		TargetMediaUnit targetMediaUnit = item.createTargetMediaUnit();
		if (targetMediaUnit != null) {
			String mid = TargetMediaUnitDaoUtil.save(targetMediaUnit);
			if (StringUtil.isNotEmpty(mid)) {
				item.createShortLinkUrlFromTargetMediaUnit(mid);
				item.createQrCodeUrlFromShortLinkUrl(PRODUCT_ITEM);
			}
		}

		item.setUpdatedAt(new Date());

		// ---- REAL UPSERT (silent, atomic) ----
		String aql = "UPSERT { _key: @key } " + "INSERT @doc " + "UPDATE @doc " + "IN @@col "
				+ "OPTIONS { silent: true }";

		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@col", col.name());
		bindVars.put("key", id);
		bindVars.put("doc", item);

		db.query(aql, bindVars, new AqlQueryOptions(), Void.class);

		return id;
	}

	public static ProductItem getById(String id) {
		return getById(id, true);
	}

	public static ProductItem getById(String id, boolean loadCategoryData) {
		if (StringUtil.isNotEmpty(id)) {
			ArangoDatabase db = getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("id", id);
			ProductItem p = new ArangoDbCommand<ProductItem>(db, AQL_GET_PRODUCT_ITEM_BY_ID, bindVars,
					ProductItem.class, new CallbackQuery<ProductItem>() {
						@Override
						public ProductItem apply(ProductItem obj) {
							obj.buildDefaultHeadlineImage();
							if (loadCategoryData) {
								loadGroupsAndCategory(obj);
							}
							return obj;
						}
					}).getSingleResult();
			return p;
		}
		return null;
	}

	public static boolean delete(String id) {
		ArangoCollection col = ProductItem.getCollection();
		if (col != null) {
			col.deleteDocument(id);
			return true;
		}
		return false;
	}

	public static ProductItem getByUrl(String url) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("url", url);
		ProductItem p = new ArangoDbCommand<ProductItem>(db, AQL_GET_PRODUCT_ITEM_BY_FULL_URL, bindVars,
				ProductItem.class).getSingleResult();
		return p;
	}

	public static ProductItem getByProductId(String productId, String productIdType) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("productId", productId);
		bindVars.put("productIdType", productIdType);
		ProductItem p = new ArangoDbCommand<ProductItem>(db, AQL_GET_PRODUCT_ITEM_BY_ID_AND_TYPE, bindVars,
				ProductItem.class).getSingleResult();
		return p;
	}

	public static List<ProductItem> list(int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<ProductItem> list = new ArangoDbCommand<ProductItem>(db, AQL_GET_PRODUCT_ITEMS, bindVars,
				ProductItem.class).getResultsAsList();
		return list;
	}

	/**
	 * @param keywords
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static Map<String, String> searchProductItemsByKeywords(String keywords, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("keywords", "%" + keywords + "%");
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<ProductItem> list = new ArangoDbCommand<ProductItem>(db, AQL_SEARCH_PRODUCT_ITEMS_BY_KEYWORDS, bindVars,
				ProductItem.class).getResultsAsList();
		Map<String, String> map = new HashMap<>(list.size());
		for (ProductItem productItem : list) {
			String title = productItem.getTitle();
			if (title.length() > 100) {
				title = title.substring(0, 100) + "...";
			}
			map.put(productItem.getProductId(), title);
		}
		return map;
	}

	public static List<ProductItem> getByCampaign(String campaignId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("campaignId", campaignId);
		List<ProductItem> list = new ArangoDbCommand<ProductItem>(db, AQL_GET_PRODUCT_ITEMS_BY_CAMPAIGN, bindVars,
				ProductItem.class).getResultsAsList();
		return list;
	}

	public static List<ProductItem> list(String searchValue, String groupId, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(4);
		if (searchValue.length() > 1) {
			bindVars.put("keywords", searchValue);
		} else {
			bindVars.put("keywords", "");
		}
		bindVars.put("groupId", groupId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);

		LogUtil.logInfo(AssetProductItemDaoUtil.class, bindVars);
		LogUtil.logInfo(AssetProductItemDaoUtil.class, AQL_GET_PRODUCT_ITEMS_IN_GROUP);
		List<ProductItem> list = new ArangoDbCommand<ProductItem>(db, AQL_GET_PRODUCT_ITEMS_IN_GROUP, bindVars,
				ProductItem.class).getResultsAsList();
		return list;
	}

	public static int countTotalSizeOfGroup(String searchValue, String groupId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("groupId", groupId);
		if (searchValue.length() > 1) {
			bindVars.put("keywords", "%" + searchValue + "%");
		} else {
			bindVars.put("keywords", "");
		}
		Integer count = new ArangoDbCommand<Integer>(db, AQL_COUNT_PRODUCT_ITEMS_IN_GROUP, bindVars, Integer.class)
				.getSingleResult();
		return count;
	}

	public final static long countTotal() {
		ArangoDatabase db = getCdpDatabase();
		long c = db.collection(ProductItem.COLLECTION_NAME).count().getCount();
		return c;
	}

}
