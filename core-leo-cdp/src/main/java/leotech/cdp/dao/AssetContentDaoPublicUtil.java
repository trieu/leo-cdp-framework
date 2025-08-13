package leotech.cdp.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;

import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.ContentClassPostQuery;
import leotech.system.config.AqlTemplate;
import leotech.system.model.AppMetadata;
import leotech.system.model.DataPrivacy;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;
import leotech.system.util.database.ArangoDbUtil;
import rfx.core.util.StringUtil;

public class AssetContentDaoPublicUtil extends AssetItemDao {

	static final String AQL_FIND_KEY_AQL = ArangoDbUtil.contentFindKeyAql(AssetContent.COLLECTION_NAME);
	static final String AQL_GET_ASSET_CONTENT_BY_NETWORK = AqlTemplate.get("AQL_GET_ASSET_CONTENT_BY_NETWORK");
	static final String AQL_GET_ASSET_CONTENT_BY_GROUP = AqlTemplate.get("AQL_GET_ASSET_CONTENT_BY_GROUP");
	static final String AQL_GET_VIEWABLE_ASSET_CONTENT_BY_GROUP = AqlTemplate.get("AQL_GET_VIEWABLE_ASSET_CONTENT_BY_GROUP");
	static final String AQL_GET_ASSET_CONTENT_BY_ID = AqlTemplate.get("AQL_GET_ASSET_CONTENT_BY_ID");
	static final String AQL_GET_ASSET_CONTENT_BY_SLUG = AqlTemplate.get("AQL_GET_ASSET_CONTENT_BY_SLUG");
	static final String AQL_GET_ALL_ASSET_CONTENT_BY_GROUP = AqlTemplate.get("AQL_GET_ALL_ASSET_CONTENT_BY_GROUP");
	static final String AQL_GET_ALL_ASSET_CONTENT_BY_CATEGORY_OR_GROUP = AqlTemplate.get("AQL_GET_ALL_ASSET_CONTENT_BY_CATEGORY_OR_GROUP");
	static final String AQL_GET_KEYWORDS_OF_ALL_ASSET_CONTENT = AqlTemplate.get("AQL_GET_KEYWORDS_OF_ALL_ASSET_CONTENT");


	public static AssetContent getBySlug(String slug) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("slug", slug);
		System.out.println("AssetContent.getBySlug " + slug);
		AssetContent p = new ArangoDbCommand<AssetContent>(db, AQL_GET_ASSET_CONTENT_BY_SLUG, bindVars, AssetContent.class, new CallbackQuery<AssetContent>() {
			@Override
			public AssetContent apply(AssetContent obj) {
				obj.buildDefaultHeadlineImage();
				loadGroupsAndCategory(obj);
				return obj;
			}
		}).getSingleResult();
		return p;
	}

	public static List<AssetContent> listByNetwork(long networkId, int startIndex, int numberResult) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("networkId", networkId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<AssetContent> list = new ArangoDbCommand<AssetContent>(db, AQL_GET_ASSET_CONTENT_BY_NETWORK, bindVars, AssetContent.class,
				new CallbackQuery<AssetContent>() {
					@Override
					public AssetContent apply(AssetContent obj) {
						obj.compactDataForList(true);
						loadGroupsAndCategory(obj);
						return obj;
					}
				}).getResultsAsList();
		return list;
	}

	public static List<AssetContent> list(String searchValue, String groupId, int startIndex, int numberResult) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(4);
		if(searchValue.length()>1) {
			bindVars.put("keywords", "%" + searchValue + "%");
		}
		else {
			bindVars.put("keywords", "");
		}
		bindVars.put("groupId", groupId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<AssetContent> list = new ArangoDbCommand<AssetContent>(db, AQL_GET_ASSET_CONTENT_BY_GROUP, bindVars, AssetContent.class,
				new CallbackQuery<AssetContent>() {
					@Override
					public AssetContent apply(AssetContent obj) {
						obj.compactDataForList(true);
						loadGroupsAndCategory(obj);
						return obj;
					}
				}).getResultsAsList();
		return list;
	}

	public static List<AssetContent> listByMediaNetwork(AppMetadata app, boolean includeProtected,
			boolean includePrivate, int startIndex, int numberResult) {
		return listByContentClassAndKeywords(app.getContentCategoryId(),
				app.getPublicContentClassList(), new String[]{}, includeProtected, includePrivate, false,
				startIndex, numberResult);
	}

	public static List<AssetContent> listByContentClass(String contentCategoryId, List<String> contentClasses,
			boolean includeProtected, boolean includePrivate, int startIndex, int numberResult) {
		return listByContentClassAndKeywords(contentCategoryId, contentClasses, new String[]{}, includeProtected,
				includePrivate, false, startIndex, numberResult);
	}

	public static List<AssetContent> listByContentClassAndKeywords(String contentCategoryId, List<String> publicContentClasses,
			String[] keywords, boolean includeProtected, boolean includePrivate, boolean joinResultByAnd,
			int startIndex, int numberResult) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		
		StringBuilder aql = new StringBuilder();
		aql.append("FOR p in ").append(AssetContent.COLLECTION_NAME).append(" FILTER ");
		aql.append(" p.privacyStatus == ").append(DataPrivacy.PUBLIC);
		
		Map<String, Object> bindVars = new HashMap<>();
		
		// content class
		int c = 0;
		int sizeContentClasses = publicContentClasses.size();
		if(sizeContentClasses > 0) {
			aql.append(" AND ( ");
			for (String contentClass : publicContentClasses) {
				if (!contentClass.isEmpty()) {
					bindVars.put("contentClass"+c, contentClass);
					aql.append(" p.contentClass == @contentClass").append(c).append(" ");
					c++;
					if (c + 1 <= sizeContentClasses) {
						aql.append(" OR ");
					}
				}
			}
			aql.append(" ) ");
		}

		if (StringUtil.isNotEmpty(contentCategoryId)) {
			aql.append(" AND @contentCategoryId IN p.categoryIds[*] ");
			bindVars.put("contentCategoryId", contentCategoryId);
		}

		// keyword filter
		if (keywords.length > 0) {
			c = 0;
			aql.append(" AND ( ");

			String keywordLogicOperator;
			if (joinResultByAnd) {
				keywordLogicOperator = " AND ";
			} else {
				keywordLogicOperator = " OR ";
			}

			for (String k : keywords) {
				String keyword = k.trim();
				if (!keyword.isEmpty() || !keyword.contains("@")) {
					System.out.println(" keyword: " + keyword);
					bindVars.put("keyword" + c, keyword);
					aql.append("@keyword").append(c).append(" IN p.keywords[*] ");
					c++;
					if (c + 1 <= keywords.length) {
						aql.append(keywordLogicOperator);
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
		aql.append(" ) SORT p.modificationTime DESC LIMIT @startIndex,@numberResult RETURN p");
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);

		List<AssetContent> list = new ArangoDbCommand<AssetContent>(db, aql.toString(), bindVars, AssetContent.class,
				new CallbackQuery<AssetContent>() {
					@Override
					public AssetContent apply(AssetContent obj) {
						obj.compactDataForList(true);
						loadGroupsAndCategory(obj);
						return obj;
					}
				}).getResultsAsList();
		return list;
	}

	public static List<AssetContent> listByGroup(String searchValue, String groupId) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("groupId", groupId);
		if(searchValue.length()>1) {
			bindVars.put("keywords", "%" + searchValue + "%");
		}
		else {
			bindVars.put("keywords", "");
		}
		List<AssetContent> list = new ArangoDbCommand<AssetContent>(db, AQL_GET_ALL_ASSET_CONTENT_BY_GROUP, bindVars, AssetContent.class,
			new CallbackQuery<AssetContent>() {
				@Override
				public AssetContent apply(AssetContent obj) {
					obj.compactDataForList(true);
					loadGroupsAndCategory(obj);
					return obj;
				}
			}).getResultsAsList();
		return list;
	}

	public static List<AssetContent> listByCategoryOrGroup(String categoryId, String groupId) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("categoryId", categoryId);
		bindVars.put("groupId", groupId);
		List<AssetContent> list = new ArangoDbCommand<AssetContent>(db, AQL_GET_ALL_ASSET_CONTENT_BY_CATEGORY_OR_GROUP, bindVars, AssetContent.class,
				new CallbackQuery<AssetContent>() {
					@Override
					public AssetContent apply(AssetContent obj) {
						obj.compactDataForList(true);
						loadGroupsAndCategory(obj);
						return obj;
					}
				}).getResultsAsList();
		return list;
	}

	public static List<AssetContent> listViewableContentByGroup(String groupId, long ownerId, int startIndex,
			int numberResult) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("groupId", groupId);
		bindVars.put("ownerId", ownerId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<AssetContent> list = new ArangoDbCommand<AssetContent>(db, AQL_GET_VIEWABLE_ASSET_CONTENT_BY_GROUP, bindVars, AssetContent.class,
				new CallbackQuery<AssetContent>() {
					@Override
					public AssetContent apply(AssetContent obj) {
						obj.compactDataForList(true);
						loadGroupsAndCategory(obj);
						return obj;
					}
				}).getResultsAsList();
		return list;
	}

	public static List<AssetContent> list(long networkId, int startIndex, int numberResult, int privacyStatus,
			String groupId, int status) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("networkId", networkId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<AssetContent> list = new ArangoDbCommand<AssetContent>(db, AQL_GET_ASSET_CONTENT_BY_NETWORK, bindVars, AssetContent.class,
				new CallbackQuery<AssetContent>() {
					@Override
					public AssetContent apply(AssetContent obj) {
						obj.compactDataForList(true);
						loadGroupsAndCategory(obj);
						return obj;
					}
				}).getResultsAsList();
		return list;
	}

	public static String buildContentClassPostQuery(List<ContentClassPostQuery> ccpQueries) {
		StringBuilder aql = new StringBuilder();
		String returnSingle = " RETURN {title: p.title, headlineImageUrl: p.headlineImageUrl, id: p._key, description: p.description, contentClass: p.contentClass, creationTime: p.creationTime, modificationTime : p.modificationTime, headlineImages: p.headlineImages , slug : p.slug }  ) ";
		List<String> keys = new ArrayList<>(ccpQueries.size());
		for (ContentClassPostQuery query : ccpQueries) {
			String ccpQueryKey = query.getKey();
			keys.add(ccpQueryKey);
			String categoryId = query.getCategoryId();
			int startIndex = query.getStartIndex();
			int limit = query.getLimit();
			aql.append("LET ").append(ccpQueryKey).append(" = (FOR p in ").append(AssetContent.COLLECTION_NAME)
					.append(" FILTER ( \"").append(categoryId)
					.append("\" IN p.categoryIds[*]) AND (p.privacyStatus == 0 OR p.privacyStatus == 1 ) ")
					.append(" AND (p.contentClass == \"news\")").append("SORT p.modificationTime DESC LIMIT  ")
					.append(startIndex).append(",").append(limit).append(returnSingle).append("\n");

		}
		aql.append(" RETURN {").append(StringUtil.joinFromList(",", keys)).append("}");
		return aql.toString();
	}

	public static Map<String, Object> listForDefaultHomepage(List<ContentClassPostQuery> ccpQueries) {
		Map<String, Object> map = new HashMap<>();
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		ArangoCursor<BaseDocument> cursor = null;
		String aql = buildContentClassPostQuery(ccpQueries);
		cursor = db.query(aql, new HashMap<>(0), null, BaseDocument.class);
		while (cursor.hasNext()) {
			BaseDocument doc = cursor.next();
			map = doc.getProperties();
			break;
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	public static List<String> getAllKeywords() {
		List<String> list = null;
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		ArangoCursor<BaseDocument> cursor = db.query(AQL_GET_KEYWORDS_OF_ALL_ASSET_CONTENT, new HashMap<>(0), null,
				BaseDocument.class);
		while (cursor.hasNext()) {
			BaseDocument doc = cursor.next();
			System.out.println(doc.getProperties());
			list = (List<String>) doc.getProperties().getOrDefault("keywords", new ArrayList<String>(0));
			list = list.stream().distinct().filter(s -> {
				return !s.isEmpty();
			}).collect(Collectors.toList());
			break;
		}
		return list == null ? new ArrayList<>(0) : list;
	}

	public static List<AssetContent> listPublicContentsByKeywords(String[] categoryIds, List<String> publicContentClasses,
			String[] keywords, int startIndex, int numberResult) {
		if (keywords.length == 0 || categoryIds.length == 0) {
			return new ArrayList<>(0);
		}

		List<AssetContent> list = new ArrayList<>();
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>();

		StringBuilder aql = new StringBuilder();
		aql.append("FOR p in ").append(AssetContent.COLLECTION_NAME).append(" FILTER ");

		// content class
		int c = 0;
		int sizeContentClasses = publicContentClasses.size();
		if(sizeContentClasses > 0) {
			aql.append(" ( ");
			for (String contentClass : publicContentClasses) {
				if (!contentClass.isEmpty()) {
					bindVars.put("contentClass"+c, contentClass);
					aql.append(" p.contentClass == @contentClass").append(c).append(" ");
					c++;
					if (c + 1 <= sizeContentClasses) {
						aql.append(" OR ");
					}
				}
			}
			aql.append(" ) ");
		}
		

		// category filter
		aql.append(" AND ( ");
		c = 0;
		for (String categoryId : categoryIds) {
			categoryId = categoryId.trim();
			if (!categoryId.isEmpty()) {

				bindVars.put("categoryId" + c, categoryId);
				aql.append("@categoryId").append(c).append(" IN p.categoryIds[*] ");
				c++;
				if (c + 1 <= categoryIds.length) {
					aql.append(" OR ");
				}
			}
		}
		aql.append(") AND ( ");

		// keywords filter
		int i = 0;
		for (String keyword : keywords) {
			keyword = keyword.trim();
			if (!keyword.isEmpty()) {
				String key = "keyword" + i;
				bindVars.put(key, keyword);
				aql.append("@").append(key).append(" IN p.keywords[*] ");
				i++;
			}
			int lastIndex = keywords.length;
			if (i < lastIndex) {
				aql.append(" OR ");
			} else {
				aql.append(" ) ");
			}
		}

		aql.append(" AND p.privacyStatus == 0 SORT p.modificationTime DESC LIMIT @startIndex,@numberResult RETURN p ");
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);

		ArangoDbCommand.CallbackQuery<AssetContent> callback = new ArangoDbCommand.CallbackQuery<AssetContent>() {
			@Override
			public AssetContent apply(AssetContent obj) {
				obj.compactDataForList(true);
				loadGroupsAndCategory(obj);
				return obj;
			}
		};
		System.out.println("listPublicPostsByKeywords " + aql.toString());
		list.addAll(new ArangoDbCommand<AssetContent>(db, aql.toString(), bindVars, AssetContent.class, callback).getResultsAsList());

		// System.out.println(aql.toString());
		return list;
	}

	public static long countTotal() {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		long c = db.collection(AssetContent.COLLECTION_NAME).count().getCount();
		return c;
	}

}
