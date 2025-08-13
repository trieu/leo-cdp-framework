package leotech.cdp.dao;

import java.util.List;
import java.util.Map;

import leotech.cdp.domain.AssetCategoryManagement;
import leotech.cdp.model.asset.AssetCategory;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.AssetItem;

public class AssetItemDao extends AbstractCdpDatabaseUtil {

	public static void loadGroupsAndCategory(AssetItem item) {
		List<String> groupIds = item.getGroupIds();
		for (String groupId : groupIds) {
			AssetGroup group = AssetGroupDaoUtil.getById(groupId);
			if(group != null) {
				item.setAssetGroup(groupId, group );
			}
		}
		List<String> cateIds = item.getCategoryIds();
		Map<String, AssetCategory> catMap = AssetCategoryManagement.getAssetCategoryMap();
		for (String cateId : cateIds) {
			AssetCategory cate = catMap.get(cateId);
			if(cate != null) {
				item.setAssetCategory(cateId, cate );
			}
		}
	}
}
