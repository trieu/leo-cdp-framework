package leotech.cdp.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.asset.AssetCategory;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.MeasurableItem;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;

public class ContentQueryDaoUtil {

	//////// Data Query for Post ///////////////////////////////////
	///////////////////////////////////////////////////////////////////

	public static Map<String, List<MeasurableItem>> listPostsByContentClassAndKeywords(String contentClass, String[] keywords,
			boolean includeProtected, boolean includePrivate, boolean headlineOnly) {
		if (keywords.length == 0) {
			return new HashMap<>(0);
		}

		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, List<MeasurableItem>> results = new HashMap<>(keywords.length);

		for (String keyword : keywords) {
			keyword = keyword.trim();
			StringBuilder aql = new StringBuilder();
			aql.append("FOR p in ").append(AssetContent.COLLECTION_NAME).append(" FILTER ");
			
			Map<String, Object> bindVars = new HashMap<>(2);
			bindVars.put("keyword", keyword);
			if (!contentClass.isEmpty()) {
				bindVars.put("contentClass", contentClass);
				aql.append(" p.contentClass == @contentClass AND ");
			}
			bindVars.put("keyword", keyword);

			aql.append(" @keyword IN p.keywords[*] AND (p.privacyStatus == 0 ");

			if (includeProtected) {
				aql.append(" OR p.privacyStatus == 1 ");
			}
			if (includePrivate) {
				aql.append(" OR p.privacyStatus == -1 ");
			}
			aql.append(" ) SORT p.modificationTime DESC RETURN p");
			ArangoDbCommand.CallbackQuery<MeasurableItem> callback = new ArangoDbCommand.CallbackQuery<MeasurableItem>() {
				@Override
				public MeasurableItem apply(MeasurableItem obj) {
					obj.compactDataForList(headlineOnly);
					return obj;
				}

			};
			List<MeasurableItem> list = new ArangoDbCommand<MeasurableItem>(db, aql.toString(), bindVars, MeasurableItem.class, callback)
					.getResultsAsList();
			results.put(keyword, list);
		}

		// System.out.println(aql.toString());

		return results;
	}

	public static Map<String, List<MeasurableItem>> listPostsByCategoriesAndKeywords(String[] categoryIds, String[] keywords,
			boolean includeProtected, boolean includePrivate, boolean headlineOnly) {
		Map<String, List<MeasurableItem>> results = new HashMap<>(categoryIds.length);
		if (categoryIds.length == 0) {
			return results;
		}
		ArangoCursor<MeasurableItem> cursor = null;
		try {
			StringBuilder aql = new StringBuilder();
			aql.append("FOR p in ").append(AssetContent.COLLECTION_NAME).append(" FILTER ");
			ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(categoryIds.length);

			// category filter
			Map<String, String> mapCatSlug = new HashMap<>(categoryIds.length);
			int c = 0;
			aql.append("( ");
			for (String categoryId : categoryIds) {
				categoryId = categoryId.trim();
				if (!categoryId.isEmpty() || !categoryId.contains("@")) {

					// FIXME use cache here
					AssetCategory cat = AssetCategoryDaoUtil.getById(categoryId);
					mapCatSlug.put(categoryId, cat.getSlug().toLowerCase());

					bindVars.put("categoryId" + c, categoryId);
					aql.append("@categoryId").append(c).append(" IN p.categoryIds[*] ");
					c++;
					if (c + 1 <= categoryIds.length) {
						aql.append(" OR ");
					}
				}
			}
			aql.append(") ");

			// keyword filter
			if (keywords.length > 0) {
				c = 0;
				aql.append(" AND ( ");
				for (String k : keywords) {
					String keyword = k.trim();
					if (!keyword.isEmpty() || !keyword.contains("@")) {
						System.out.println(" keyword: " + keyword);
						bindVars.put("keyword" + c, keyword);
						aql.append("@keyword").append(c).append(" IN p.keywords[*] ");
						c++;
						if (c + 1 <= keywords.length) {
							aql.append(" OR ");
						}
					}
				}
				aql.append(") ");
			}

			// filter only public as default
			aql.append(" AND (p.privacyStatus == 0 ");

			if (includeProtected) {
				aql.append(" OR p.privacyStatus == 1 ");
			}
			if (includePrivate) {
				aql.append(" OR p.privacyStatus == -1 ");
			}
			aql.append(" ) SORT p.modificationTime DESC RETURN p");

			System.out.println(aql.toString());
			System.out.println(bindVars);
			cursor = db.query(aql.toString(), bindVars, null, MeasurableItem.class);
			while (cursor.hasNext()) {
				MeasurableItem p = cursor.next();

				// static url update
				p.compactDataForList(headlineOnly);

				String catId = p.getCategoryIds().size() > 0 ? p.getCategoryIds().get(0) : "";
				String catSlug = mapCatSlug.get(catId);

				if (catSlug != null && !catId.isEmpty()) {
					List<MeasurableItem> posts = results.get(catSlug);
					if (posts == null) {
						posts = new ArrayList<>();
						results.put(catSlug, posts);
					}
					posts.add(p);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (cursor != null) {
					cursor.close();
				}
			} catch (IOException e) {
			}
		}
		return results;
	}

	public static List<MeasurableItem> searchPost(String[] keywords, boolean includeProtected, boolean includePrivate,
			boolean headlineOnly) {
		List<MeasurableItem> list;
		if (keywords.length == 0) {
			return new ArrayList<>(0);
		}

		StringBuilder query = new StringBuilder();
		int c = 0;
		for (String keyword : keywords) {
			String k = keyword.trim();
			if (!k.isEmpty()) {
				query.append(k);
				c++;
				if (c + 1 <= keywords.length) {
					query.append(",|");
				}
			}

		}

		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);

		StringBuilder aql = new StringBuilder("FOR p IN FULLTEXT(post, \"title\", @query) ");

		// filter only public as default
		aql.append(" FILTER (p.privacyStatus == 0 ");
		if (includeProtected) {
			aql.append(" OR p.privacyStatus == 1 ");
		}
		if (includePrivate) {
			aql.append(" OR p.privacyStatus == -1 ");
		}
		aql.append(" ) SORT p.modificationTime DESC RETURN p");

		System.out.println(query + " ==> searchPost " + aql.toString());
		bindVars.put("query", query.toString());

		ArangoDbCommand.CallbackQuery<MeasurableItem> callback = new ArangoDbCommand.CallbackQuery<MeasurableItem>() {
			@Override
			public MeasurableItem apply(MeasurableItem obj) {
				obj.compactDataForList(headlineOnly);
				return obj;
			}
		};
		list = new ArangoDbCommand<MeasurableItem>(db, aql.toString(), bindVars, MeasurableItem.class, callback).getResultsAsList();
		return list;
	}

	//////// Data Query for Page ///////////////////////////////////
	///////////////////////////////////////////////////////////////////

	public static List<AssetGroup> listPagesByKeywords(String[] keywords, boolean includeProtected,
			boolean includePrivate, boolean headlineOnly) {

		if (keywords.length == 0) {
			return new ArrayList<>(0);
		}

		StringBuilder aql = new StringBuilder("FOR p in ").append(AssetGroup.COLLECTION_NAME).append(" FILTER ( ");
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(keywords.length);

		int c = 0;
		for (String keyword : keywords) {
			keyword = keyword.trim();
			if (!keyword.isEmpty() || !keyword.contains("@")) {
				bindVars.put("keyword" + c, keyword);
				aql.append("@keyword").append(c).append(" IN p.keywords[*] ");
				c++;
				if (c + 1 <= keywords.length) {
					aql.append(" OR ");
				}
			}
		}
		// filter only public as default
		aql.append(" ) AND (p.privacyStatus == 0 ");

		if (includeProtected) {
			aql.append(" OR p.privacyStatus == 1 ");
		}
		if (includePrivate) {
			aql.append(" OR p.privacyStatus == -1 ");
		}
		aql.append(" ) SORT p.modificationTime DESC  LIMIT @startIndex,@numberResult RETURN p");

		System.out.println(aql.toString());
		ArangoDbCommand.CallbackQuery<AssetGroup> callback = new ArangoDbCommand.CallbackQuery<AssetGroup>() {
			@Override
			public AssetGroup apply(AssetGroup obj) {
				obj.compactDataForList(headlineOnly);
				return obj;
			}
		};
		List<AssetGroup> list = new ArangoDbCommand<AssetGroup>(db, aql.toString(), bindVars, AssetGroup.class, callback)
				.getResultsAsList();
		return list;
	}
}
