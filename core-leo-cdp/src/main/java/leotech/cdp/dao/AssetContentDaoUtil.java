package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.cdp.query.filters.DataFilter;
import leotech.starter.router.ObserverHttpRouter;
import leotech.system.config.AqlTemplate;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;
import leotech.system.util.database.ArangoDbUtil;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 * @since 2020
 *
 */
public class AssetContentDaoUtil extends AssetContentDaoPublicUtil {

	static final String AQL_FIND_KEY_AQL = ArangoDbUtil.contentFindKeyAql(AssetContent.COLLECTION_NAME);
	static final String AQL_GET_ASSET_CONTENT_BY_ID = AqlTemplate.get("AQL_GET_ASSET_CONTENT_BY_ID");
	static final String AQL_GET_ASSET_CONTENT_BY_FULL_URL = AqlTemplate.get("AQL_GET_ASSET_CONTENT_BY_FULL_URL");
	static final String AQL_GET_ASSET_CONTENT_BY_FILTER = AqlTemplate.get("AQL_GET_ASSET_CONTENT_BY_FILTER");
	static final String AQL_GET_ASSET_CONTENT_BY_CAMPAIGN = AqlTemplate.get("AQL_GET_ASSET_CONTENT_BY_CAMPAIGN");

	/**
	 * @param item
	 * @return
	 */
	public static String save(AssetContent item) {
		if (item.dataValidation()) {
			ArangoCollection col = item.getDbCollection();
			if (col != null) {
				String id = item.getId();

				if (item.isShortUrlLink()) {
					TargetMediaUnit mediaUnit = item.createTargetMediaUnit();
					if (mediaUnit != null) {
						String mid = TargetMediaUnitDaoUtil.save(mediaUnit);
						if (StringUtil.isNotEmpty(mid)) {
							item.createShortLinkUrlFromTargetMediaUnit(mid);
							item.createQrCodeUrlFromShortLinkUrl(AssetContent.SHORT_LINK_FOLDER_NAME);
						}
					}
				} else if (item.isPresentation()) {
					item.createShortLinkUrlFromSlug(ObserverHttpRouter.PREFIX_PRESENTATION);
					item.createQrCodeUrlFromShortLinkUrl(AssetContent.PRESENTATION_FOLDER_NAME);
				} else {
					item.createShortLinkUrlFromSlug(ObserverHttpRouter.PREFIX_CONTENT);
					item.createQrCodeUrlFromShortLinkUrl(AssetContent.CONTENT_FOLDER_NAME);
				}
				item.setUpdatedAt(new Date());
				col.insertDocument(item, optionToUpsertInSilent());
				return id;
			}
		}
		return "";
	}

	/**
	 * @param id
	 * @param headlineOnly
	 * @return
	 */
	public static AssetContent getById(String id, boolean headlineOnly) {
		if (StringUtil.isNotEmpty(id)) {
			ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("id", id);
			AssetContent p = new ArangoDbCommand<AssetContent>(db, AQL_GET_ASSET_CONTENT_BY_ID, bindVars,
					AssetContent.class, new CallbackQuery<AssetContent>() {
						@Override
						public AssetContent apply(AssetContent obj) {
							obj.compactDataForList(headlineOnly);
							if (headlineOnly == false) {
								loadGroupsAndCategory(obj);
							}
							obj.buildDefaultHeadlineImage();
							return obj;
						}
					}).getSingleResult();
			return p;
		}
		return null;
	}

	/**
	 * @param id
	 * @return
	 */
	public static AssetContent getById(String id) {
		return getById(id, false);
	}

	/**
	 * @param id
	 * @return
	 */
	public static boolean delete(String id) {
		ArangoCollection col = AssetContent.theCollection();
		if (col != null) {
			col.deleteDocument(id);
			return true;
		}
		return false;
	}

	/**
	 * @param fullUrl
	 * @return
	 */
	public static AssetContent getByUrl(String fullUrl) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("fullUrl", fullUrl);
		AssetContent p = new ArangoDbCommand<AssetContent>(db, AQL_GET_ASSET_CONTENT_BY_FULL_URL, bindVars,
				AssetContent.class).getSingleResult();
		return p;
	}

	/**
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<AssetContent> list(int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<AssetContent> list = new ArangoDbCommand<AssetContent>(db, AQL_GET_ASSET_CONTENT_BY_FILTER, bindVars,
				AssetContent.class).getResultsAsList();
		return list;
	}

	/**
	 * @param campaignId
	 * @return
	 */
	public static List<AssetContent> getByCampaign(String campaignId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("campaignId", campaignId);
		List<AssetContent> list = new ArangoDbCommand<AssetContent>(db, AQL_GET_ASSET_CONTENT_BY_CAMPAIGN, bindVars,
				AssetContent.class).getResultsAsList();
		return list;
	}

	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload filter(DataFilter filter) {
		ArangoDatabase db = getCdpDatabase();

		// TODO dynamic query builder for filtering data
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("startIndex", filter.getStart());
		bindVars.put("numberResult", filter.getLength());

		List<AssetContent> list = new ArangoDbCommand<AssetContent>(db, AQL_GET_ASSET_CONTENT_BY_FILTER, bindVars,
				AssetContent.class).getResultsAsList();

		long recordsTotal = countTotal();
		int recordsFiltered = list.size();
		int draw = filter.getDraw();
		JsonDataTablePayload payload = JsonDataTablePayload.data(filter.getUri(), list, recordsTotal, recordsFiltered,
				draw);
		return payload;
	}

}
