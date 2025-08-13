package leotech.cdp.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.domain.AssetCategoryManagement;
import leotech.cdp.model.asset.AssetCategory;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.AssetType;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;
import leotech.system.util.database.ArangoDbUtil;

/**
 * @author tantrieuf31
 * @since 2021
 *
 */
public class AssetGroupDaoUtil extends AbstractCdpDatabaseUtil{

	static final String AQL_FIND_KEY_AQL = ArangoDbUtil.contentFindKeyAql(AssetGroup.COLLECTION_NAME);
	static final String AQL_GET_ASSET_GROUP_BY_ID = AqlTemplate.get("AQL_GET_ASSET_GROUP_BY_ID");
	static final String AQL_GET_ASSET_GROUP_BY_SLUG = AqlTemplate.get("AQL_GET_ASSET_GROUP_BY_SLUG");

	static final String AQL_GET_ASSET_GROUPS = AqlTemplate.get("AQL_GET_ASSET_GROUPS");
	static final String AQL_GET_ALL_ASSET_GROUPS_BY_CATEGORY = AqlTemplate.get("AQL_GET_ALL_ASSET_GROUPS_BY_CATEGORY");
	static final String AQL_GET_PUBLIC_ASSET_GROUPS_BY_CATEGORY = AqlTemplate.get("AQL_GET_PUBLIC_ASSET_GROUPS_BY_CATEGORY");
	static final String AQL_GET_ASSET_GROUPS_FOR_SEGMENTATION = AqlTemplate.get("AQL_GET_ASSET_GROUPS_FOR_SEGMENTATION");
	
	static final String AQL_GET_DEFAULT_ASSET_GROUP_FOR_ASSET_TYPE = AqlTemplate.get("AQL_GET_DEFAULT_ASSET_GROUP_FOR_ASSET_TYPE");
	

	public static String save(AssetGroup group) {
		if (group.dataValidation()) {
			ArangoCollection col = group.getDbCollection();
			if (col != null) {
				String id = group.getId();
				String _key = ArangoDbUtil.findKey(AQL_FIND_KEY_AQL, "id", id);
				if (_key == null) {
					col.insertDocument(group);
				} else {
					group.setModificationTime(System.currentTimeMillis());
					col.updateDocument(_key, group, getUpdateOptions());
				}
				return id;
			}
		}
		return null;
	}

	public static String delete(String assetGroupId) {
		AssetGroup page = getById(assetGroupId);
		ArangoCollection col = page.getDbCollection();
		if (col != null) {
			String _key = ArangoDbUtil.findKey(AQL_FIND_KEY_AQL, "id", assetGroupId);
			col.deleteDocument(_key);
			return _key;
		}
		return null;
	}

	public static String saveMediaContent(String assetGroupId, int type, String mediaContent) {
		AssetGroup page = getById(assetGroupId);
		page.setType(type);
		page.setMediaInfo(mediaContent);
		return assetGroupId;
	}

	public static AssetGroup getById(String id) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		CallbackQuery<AssetGroup> callback = new CallbackQuery<AssetGroup>() {
			@Override
			public AssetGroup apply(AssetGroup obj) {
				List<String> cateIds = obj.getCategoryIds();
				Map<String, AssetCategory> catMap = AssetCategoryManagement.getAssetCategoryMap();
				for (String cateId : cateIds) {
					AssetCategory cate = catMap.get(cateId);
					if(cate != null) {
						obj.setAssetCategory(cateId, cate );
					}
				}
				return obj;
			}
		};
		AssetGroup p = new ArangoDbCommand<AssetGroup>(db, AQL_GET_ASSET_GROUP_BY_ID, bindVars, AssetGroup.class, callback ).getSingleResult();
		return p;
	}

	public static AssetGroup getBySlug(String slug) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("slug", slug);
		AssetGroup p = new ArangoDbCommand<AssetGroup>(db, AQL_GET_ASSET_GROUP_BY_SLUG, bindVars, AssetGroup.class).getSingleResult();
		return p;
	}

	public static List<AssetGroup> list(int startIndex, int numberResult) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<AssetGroup> list = new ArangoDbCommand<AssetGroup>(db, AQL_GET_ASSET_GROUPS, bindVars, AssetGroup.class)
				.getResultsAsList();
		return list;
	}

	public static List<AssetGroup> listByCategory(String categoryId) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("categoryId", categoryId);
		List<AssetGroup> list = new ArangoDbCommand<AssetGroup>(db, AQL_GET_ALL_ASSET_GROUPS_BY_CATEGORY, bindVars, AssetGroup.class).getResultsAsList();
		return list;
	}

	public static List<AssetGroup> listByCategoryWithPublicPrivacy(String categoryId) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("categoryId", categoryId);
		List<AssetGroup> list = new ArangoDbCommand<AssetGroup>(db, AQL_GET_PUBLIC_ASSET_GROUPS_BY_CATEGORY, bindVars, AssetGroup.class).getResultsAsList();
		return list;
	}
	
	public static List<AssetGroup> getAllAssetGroupsForSegmentation() {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		List<AssetGroup> list = new ArangoDbCommand<AssetGroup>(db, AQL_GET_ASSET_GROUPS_FOR_SEGMENTATION, new HashMap<>(0), AssetGroup.class).getResultsAsList();
		return list;
	}
	
	public static AssetGroup getDefaultGroupForProduct() {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("assetType", AssetType.PRODUCT_ITEM_CATALOG);
		return new ArangoDbCommand<>(db, AQL_GET_DEFAULT_ASSET_GROUP_FOR_ASSET_TYPE, bindVars, AssetGroup.class).getSingleResult();
	}
	
	public static AssetGroup getDefaultGroupForContent() {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("assetType", AssetType.CONTENT_ITEM_CATALOG);
		return new ArangoDbCommand<>(db, AQL_GET_DEFAULT_ASSET_GROUP_FOR_ASSET_TYPE, bindVars, AssetGroup.class).getSingleResult();
	}
	
	public static AssetGroup getDefaultGroupForPresentation() {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("assetType", AssetType.PRESENTATION_ITEM_CATALOG);
		return new ArangoDbCommand<>(db, AQL_GET_DEFAULT_ASSET_GROUP_FOR_ASSET_TYPE, bindVars, AssetGroup.class).getSingleResult();
	}

}
