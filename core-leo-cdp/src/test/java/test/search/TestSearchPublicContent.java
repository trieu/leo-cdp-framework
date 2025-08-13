package test.search;

import java.util.List;

import leotech.cdp.model.asset.AssetContent;
import leotech.query.util.SearchEngineUtil;

public class TestSearchPublicContent {

	public static void main(String[] args) {
		testSearch();
		
//		List<AssetContent> posts = PostDaoUtil.listByNetwork(MediaNetwork.DEFAULT_ID, 0, Integer.MAX_VALUE);
//		LuceneSearchPostUtil.insertPostIndex(posts);

	}

	private static void testSearch() {
		String[] keywords = new String[]{" Hero"};
		List<AssetContent> results = SearchEngineUtil.searchPublicPost(keywords, 1, 5);
		for (AssetContent result : results) {
			System.out.println(result.getTitle());
		}
	}
}
