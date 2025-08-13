package leotech.cdp.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;

/**
 * target media unit Database Access Object
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class TargetMediaUnitDaoUtil extends AbstractCdpDatabaseUtil {
	
	static final String AQL_GET_TARGET_MEDIA_UNIT_BY_ID = AqlTemplate.get("AQL_GET_TARGET_MEDIA_UNIT_BY_ID");
	static final String AQL_GET_TARGET_MEDIA_UNIT_BY_PROFILE_ID = AqlTemplate.get("AQL_GET_TARGET_MEDIA_UNIT_BY_PROFILE_ID");
	
	/**
	 * @param m
	 * @return
	 */
	public static String save(TargetMediaUnit m) {
		if (m.dataValidation()) {
			try {
				ArangoCollection col = m.getDbCollection();
				if (col != null) {
					col.insertDocument(m, insertDocumentOverwriteModeReplace());
				}
			} catch (ArangoDBException e) {
				if(e.getErrorNum() != 1200) {
					System.err.println(e.getErrorNum() + " " + e.getErrorMessage());
				}
			}
			return m.getId();
		}
		throw new IllegalArgumentException("TargetMediaUnitDaoUtil.error, dataValidation is failed: " + m);
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static TargetMediaUnit getTargetMediaUnitById(String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		TargetMediaUnit p = new ArangoDbCommand<TargetMediaUnit>(db, AQL_GET_TARGET_MEDIA_UNIT_BY_ID, bindVars, TargetMediaUnit.class).getSingleResult();
		if(p != null) {
			ProductItem item = AssetProductItemDaoUtil.getById(p.getRefProductItemId());
			p.setProductItem(item);
			
			AssetContent content = AssetContentDaoUtil.getById(p.getRefContentItemId());
			p.setContentItem(content);
		}
		return p;
	}

	/**
	 * @param id
	 * @return
	 */
	public static TargetMediaUnit getByIdForProductRecommendation(String id, boolean withProductInfo) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		TargetMediaUnit p = new ArangoDbCommand<TargetMediaUnit>(db, AQL_GET_TARGET_MEDIA_UNIT_BY_ID, bindVars, TargetMediaUnit.class).getSingleResult();
		if(p != null && withProductInfo) {
			ProductItem product = AssetProductItemDaoUtil.getById(p.getRefProductItemId());
			p.setProductItem(product);
		}
		return p;
	}
	
	public static TargetMediaUnit getByIdForContentRecommendation(String id, boolean withContentInfo) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		TargetMediaUnit p = new ArangoDbCommand<TargetMediaUnit>(db, AQL_GET_TARGET_MEDIA_UNIT_BY_ID, bindVars, TargetMediaUnit.class).getSingleResult();
		if(p != null && withContentInfo) {
			AssetContent item = AssetContentDaoUtil.getById(p.getRefContentItemId());
			p.setContentItem(item);
		}
		return p;
	}

	/**
	 * @param refProfileId
	 * @return
	 */
	public static List<TargetMediaUnit> getByProfileId(String refProfileId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("refProfileId", refProfileId);
		List<TargetMediaUnit> list = new ArangoDbCommand<TargetMediaUnit>(db, AQL_GET_TARGET_MEDIA_UNIT_BY_PROFILE_ID, bindVars, TargetMediaUnit.class).getResultsAsList();
		return list;
	}

}
