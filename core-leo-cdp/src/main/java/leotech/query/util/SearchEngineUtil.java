package leotech.query.util;

import java.util.ArrayList;
import java.util.List;

import leotech.cdp.dao.AssetContentDaoPublicUtil;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetItem;
import leotech.cdp.model.asset.MeasurableItem;
import leotech.system.model.AppMetadata;

public class SearchEngineUtil {

	public static List<MeasurableItem> search(String[] keywords, int startIndex, int num) {
		// TODO
		return new ArrayList<MeasurableItem>(0);
	}

	public static List<MeasurableItem> searchPost(String[] keywords, boolean includeProtected, boolean includePrivate,
			int startIndex, int num) {
		// TODO
		return new ArrayList<MeasurableItem>(0);
	}

	public static List<AssetContent> searchPublicPost(String[] keywords, int startIndex, int num) {
		// TODO
		return new ArrayList<AssetContent>(0);
	}

	public static void doPostIndexing(MeasurableItem item) {
		// TODO
	}

	public static int indexing() {
		// TODO
		List<AssetContent> items = AssetContentDaoPublicUtil.listByNetwork(AppMetadata.DEFAULT_ID, 0, Integer.MAX_VALUE);
		return items.size();
	}

	public static boolean deleteItemIndex(String id) {
		return true;
	}
	public static boolean updateIndexedItem(AssetItem item) {
		return true;
	}

	public static boolean insertItemIndex(AssetItem item) {
		return true;
	}

}
