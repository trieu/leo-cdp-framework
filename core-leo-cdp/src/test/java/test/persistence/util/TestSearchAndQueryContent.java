package test.persistence.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import leotech.cdp.dao.AssetContentDaoPublicUtil;
import leotech.cdp.dao.ContentQueryDaoUtil;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.ContentClassPostQuery;
import leotech.cdp.model.asset.MeasurableItem;
import leotech.system.config.DatabaseConfigs;
import leotech.system.model.JsonDataPayload;
import leotech.system.util.database.ArangoDbUtil;

public class TestSearchAndQueryContent {
	@BeforeAll
	public void setup() {
		System.out.println("setup");
	}

	@AfterAll
	public void clean() {
		System.out.println("clean");
	}

	// @Test(priority = 1)
	public void listPagesByKeywords() {
		String[] keywords = "vietnam, steel market".split(",");
		List<AssetGroup> pages = ContentQueryDaoUtil.listPagesByKeywords(keywords, true, false, true);
		for (AssetGroup p : pages) {
			System.out.println(p.getTitle());
			System.out.println(p.getTitle());
		}
		assertTrue(pages.size() > 0);
	}

	@Test
	@Order(2)
	public void listPostsByCategoriesAndKeywords() {

		String[] defCategoryIds = new String[] { "1329181", "1329376", "1329482" };
		String[] keywords = new String[] {};
		Map<String, List<MeasurableItem>> results = ContentQueryDaoUtil.listPostsByCategoriesAndKeywords(defCategoryIds,
				keywords, true, true, true);

		Set<String> catSlugs = results.keySet();

		for (String catSlug : catSlugs) {
			List<MeasurableItem> posts = results.get(catSlug);
			for (MeasurableItem p : posts) {
				// System.out.println(catSlug + " => " + p.getTitle());
			}
		}

		assertTrue(results.size() > 0);
	}

	protected static void testGetAllKeywords() {
		List<String> list = AssetContentDaoPublicUtil.getAllKeywords();
		List<Map<String, String>> keywords = list.stream().map(e -> {
			Map<String, String> map = new HashMap<>(1);
			map.put("name", e);
			return map;
		}).collect(Collectors.toList());
		JsonDataPayload payload = JsonDataPayload.ok("", keywords, false);
		payload.setReturnOnlyData(true);
		System.out.println(payload.toString());
	}

	public static void main(String[] args) {
		ArangoDbUtil.setDbConfigs(DatabaseConfigs.load("dbConfigsBluescope"));

		List<ContentClassPostQuery> ccpQueries = new ArrayList<>();
		ccpQueries.add(new ContentClassPostQuery("market_news", "news", "1329181"));
		ccpQueries.add(new ContentClassPostQuery("product_list", "product", "1329376"));
		ccpQueries.add(new ContentClassPostQuery("project_list", "project", "1329482"));

		System.out.println(AssetContentDaoPublicUtil.buildContentClassPostQuery(ccpQueries));

		testQueryPostsForHomepage();
	}

	public static void testQueryPostsForHomepage() {

		List<ContentClassPostQuery> ccpQueries = new ArrayList<>();
		ccpQueries.add(new ContentClassPostQuery("market_news", "news", "1329181"));
		ccpQueries.add(new ContentClassPostQuery("product_list", "product", "1329376"));
		ccpQueries.add(new ContentClassPostQuery("project_list", "project", "1329482"));
		// new TestSearchAndQueryContent().listPostsByCategoriesAndKeywords();
		Map<String, Object> map = AssetContentDaoPublicUtil.listForDefaultHomepage(ccpQueries);
		System.out.println(JsonDataPayload.ok("", map));
		rfx.core.util.Utils.exitSystemAfterTimeout(1000);
	}

}
