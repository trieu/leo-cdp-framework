package leotech.cdp.dao;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.asset.AssetCategory;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;

public class AssetCategoryDaoUtil extends AbstractCdpDatabaseUtil {

	private static final String AQL_FIND_KEY_BY_SLUG = AqlTemplate.get("AQL_FIND_KEY_BY_SLUG");
	static final String AQL_GET_CATEGORY_BY_ID = AqlTemplate.get("AQL_GET_CATEGORY_BY_ID");
	static final String AQL_GET_ALL_CATEGORIES_BY_NETWORK = AqlTemplate.get("AQL_GET_ALL_CATEGORIES_BY_NETWORK");
	static final String AQL_GET_ALL_CATEGORIES = AqlTemplate.get("AQL_GET_ALL_CATEGORIES");

	public static String save(AssetCategory category) {
		if (category.dataValidation()) {
			ArangoCollection col = category.getDbCollection();
			if (col != null) {
				String _key = ArangoDbUtil.findKey(AQL_FIND_KEY_BY_SLUG, "slug", category.getSlug());
				if (_key == null) {
					_key = col.insertDocument(category).getKey();
				} else {
					category.setModificationTime(System.currentTimeMillis());
					col.updateDocument(_key, category, getUpdateOptions());
				}
				category.setId(_key);
				return _key;
			}
		}
		return null;
	}

	public static AssetCategory getById(String id) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		AssetCategory cat = new ArangoDbCommand<AssetCategory>(db, AQL_GET_CATEGORY_BY_ID, bindVars, AssetCategory.class).getSingleResult();
		return cat;
	}

	public static String deleteById(String id) {
		ArangoCollection col = new AssetCategory().getDbCollection();
		if (col != null) {
			col.deleteDocument(id);
			return id;
		}
		return "";
	}
	
	public static List<AssetCategory> getAllCategories() {
		ArangoDatabase db = getCdpDatabase();
		List<AssetCategory> list = new ArangoDbCommand<AssetCategory>(db, AQL_GET_ALL_CATEGORIES, AssetCategory.class).getResultsAsList();
		Collections.sort(list, new Comparator<AssetCategory>() {
			@Override
			public int compare(AssetCategory o1, AssetCategory o2) {
				if (o1.getNavigationOrder() < o2.getNavigationOrder()) {
					return 1;
				} else if (o1.getNavigationOrder() > o2.getNavigationOrder()) {
					return -1;
				}
				return 0;
			}
		});
		return list;
	}

	public static List<AssetCategory> listAllByNetwork(long networkId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("networkId", networkId);
		List<AssetCategory> list = new ArangoDbCommand<AssetCategory>(db, AQL_GET_ALL_CATEGORIES_BY_NETWORK, bindVars, AssetCategory.class).getResultsAsList();

		Collections.sort(list, new Comparator<AssetCategory>() {
			@Override
			public int compare(AssetCategory o1, AssetCategory o2) {
				if (o1.getNavigationOrder() > o2.getNavigationOrder()) {
					return 1;
				} else if (o1.getNavigationOrder() < o2.getNavigationOrder()) {
					return -1;
				}
				return 0;
			}
		});
		return list;
	}
}
