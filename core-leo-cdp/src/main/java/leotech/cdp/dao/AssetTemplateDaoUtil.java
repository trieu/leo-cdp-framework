package leotech.cdp.dao;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.AssetType;
import leotech.system.config.AqlTemplate;
import leotech.system.util.QrCodeUtil;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

public class AssetTemplateDaoUtil extends AssetItemDao {


	
	static final String AQL_FIND_KEY_AQL = ArangoDbUtil.contentFindKeyAql(AssetTemplate.COLLECTION_NAME);
	static final String AQL_GET_TEMPLATE_BY_ID = AqlTemplate.get("AQL_GET_TEMPLATE_BY_ID");
	static final String AQL_GET_TEMPLATE_BY_SLUG = AqlTemplate.get("AQL_GET_TEMPLATE_BY_SLUG");
	
	static final String AQL_GET_TEMPLATES_IN_GROUP = AqlTemplate.get("AQL_GET_TEMPLATES_IN_GROUP");
	static final String AQL_GET_TEMPLATES = AqlTemplate.get("AQL_GET_TEMPLATES");
	static final String AQL_COUNT_TEMPLATES_IN_GROUP = AqlTemplate.get("AQL_COUNT_TEMPLATES_IN_GROUP");
	
	static final String AQL_GET_TEMPLATES_BY_ASSET_TYPE_AND_CATEGORY = AqlTemplate.get("AQL_GET_TEMPLATES_BY_ASSET_TYPE_AND_CATEGORY");
	static final String AQL_GET_TEMPLATES_BY_ASSET_TYPE_AND_TEMPLATE_TYPE_AND_CATEGORY = AqlTemplate.get("AQL_GET_TEMPLATES_BY_ASSET_TYPE_AND_TEMPLATE_TYPE_AND_CATEGORY");


	/**
	 * @param item
	 * @return
	 */
	public final static String save(AssetTemplate item) {
		if (item.dataValidation()) {
			ArangoDatabase db = getCdpDatabase();
			ArangoCollection col = item.getDbCollection();
			if (col != null) {
				String itemTplId = item.getId();
				
				int assetType = item.getAssetType();
				String uri = "";
				if(assetType == AssetType.FEEDBACK_FORM) {
					uri =  AssetTemplate.WEBFORM_SURVEY_URI;
				}
				else if(assetType == AssetType.WEB_HTML_CONTENT) {
					uri =  AssetTemplate.WEB_TEMPLATE_HTML_URI;
				}
				
				if(StringUtil.isNotEmpty(uri) ) {
					// check to QR code image file
					String qrCodeUrl = item.getQrCodeUrl();
					File file = new File(qrCodeUrl);
					if( !file.isFile() ) {
						String landingPageUrl = "https://" + SystemMetaData.DOMAIN_CDP_OBSERVER + uri  + itemTplId;
						String imageQrCodeUrl = QrCodeUtil.generate(AssetTemplate.ASSET_TEMPLATE_ITEM, landingPageUrl);
						item.setQrCodeUrl(imageQrCodeUrl);
					}
				}
				
				boolean isExisted = ArangoDbUtil.isExistedDocument(db, AssetTemplate.COLLECTION_NAME, itemTplId);
				if (isExisted) {
					item.setUpdatedAt(new Date());
					col.updateDocument(itemTplId, item, getUpdateOptions());
				} else {
					col.insertDocument(item);
				}
				return itemTplId;
			}
		}
		return "";
	}

	/**
	 * @param id
	 * @return
	 */
	public final static AssetTemplate getById(String id) {
		AssetTemplate p = null;
		if(StringUtil.isNotEmpty(id)) {
			ArangoDatabase db = getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("id", id);
			p = new ArangoDbCommand<AssetTemplate>(db, AQL_GET_TEMPLATE_BY_ID, bindVars, AssetTemplate.class, new CallbackQuery<AssetTemplate>() {
				@Override
				public AssetTemplate apply(AssetTemplate obj) {
					obj.buildDefaultHeadlineImage();
					loadGroupsAndCategory(obj);
					return obj;
				}
			}).getSingleResult();
		}
		return p;
	}
	
	/**
	 * @param slug
	 * @return
	 */
	public final static AssetTemplate getBySlug(String slug) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("slug", slug);
		AssetTemplate p = new ArangoDbCommand<AssetTemplate>(db, AQL_GET_TEMPLATE_BY_SLUG, bindVars, AssetTemplate.class, new CallbackQuery<AssetTemplate>() {
			@Override
			public AssetTemplate apply(AssetTemplate obj) {
				obj.buildDefaultHeadlineImage();
				loadGroupsAndCategory(obj);
				return obj;
			}
		}).getSingleResult();
		return p;
	}
	
	/**
	 * @param id
	 * @return
	 */
	public final static boolean delete(String id) {
		ArangoCollection col = AssetTemplate.getCollectionInstance();
		if (col != null) {
			col.deleteDocument(id);
			System.out.println("Deleted template id " + id);
			return true;
		}
		return false;
	}

	/**
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<AssetTemplate> list(int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<AssetTemplate> list = new ArangoDbCommand<AssetTemplate>(db, AQL_GET_TEMPLATES, bindVars, AssetTemplate.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param assetType
	 * @param categoryId
	 * @return
	 */
	public static List<AssetTemplate> listByFilter(int assetType, String categoryId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("assetType", assetType);
		bindVars.put("categoryId", categoryId);
		List<AssetTemplate> list = new ArangoDbCommand<AssetTemplate>(db, AQL_GET_TEMPLATES_BY_ASSET_TYPE_AND_CATEGORY, bindVars, AssetTemplate.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param templateType
	 * @param assetType
	 * @param categoryId
	 * @return
	 */
	public static List<AssetTemplate> listByFilter(int templateType, int assetType, String categoryId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("templateType", templateType);
		bindVars.put("categoryId", categoryId);
		bindVars.put("assetType", assetType);
		String aql = AQL_GET_TEMPLATES_BY_ASSET_TYPE_AND_TEMPLATE_TYPE_AND_CATEGORY;
		List<AssetTemplate> list = new ArangoDbCommand<AssetTemplate>(db, aql, bindVars, AssetTemplate.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param groupId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<AssetTemplate> list(String searchValue, String groupId, int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();

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

		List<AssetTemplate> list = new ArangoDbCommand<AssetTemplate>(db, AQL_GET_TEMPLATES_IN_GROUP, bindVars, AssetTemplate.class).getResultsAsList();
		return list;
	}


	/**
	 * @param groupId
	 * @return
	 */
	public static int countTotalSizeOfGroup(String searchValue, String groupId) {
		ArangoDatabase db = getCdpDatabase();		
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("groupId", groupId);
		if(searchValue.length()>1) {
			bindVars.put("keywords", "%" + searchValue + "%");
		}
		else {
			bindVars.put("keywords", "");
		}
		Integer count = new ArangoDbCommand<Integer>(db, AQL_COUNT_TEMPLATES_IN_GROUP, bindVars, Integer.class).getSingleResult();
		return count;
	}

	/**
	 * @return
	 */
	public final static long countTotal() {
		ArangoDatabase db = getCdpDatabase();
		long c = db.collection(AssetTemplate.COLLECTION_NAME).count().getCount();
		return c;
	}

}
