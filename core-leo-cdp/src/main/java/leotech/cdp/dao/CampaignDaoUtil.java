package leotech.cdp.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.marketing.Campaign;
import leotech.cdp.query.filters.DataFilter;
import leotech.system.config.AqlTemplate;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;

/**
 * Campaign Database
 * 
 * @author Trieu Nguyen
 * @since 2022
 *
 */
public class CampaignDaoUtil extends AbstractCdpDatabaseUtil {

	static final String AQL_COUNT_TOTAL_ACTIVE_CAMPAIGN = "RETURN LENGTH( FOR s in " + Campaign.COLLECTION_NAME + " FILTER s.status >= 0 RETURN s._key)";
	static final String AQL_GET_CAMPAIGNS_BY_PAGINATION = AqlTemplate.get("AQL_GET_CAMPAIGNS_BY_PAGINATION");
	static final String AQL_GET_CAMPAIGN_BY_ID = AqlTemplate.get("AQL_GET_CAMPAIGN_BY_ID");
	static final String AQL_GET_CAMPAIGNS_TO_DELETE_FOREVER = AqlTemplate.get("AQL_GET_CAMPAIGNS_TO_DELETE_FOREVER");
	
	/**
	 * @param campaign
	 * @return
	 */
	public static String saveCampaign(Campaign campaign) {
		if (campaign.dataValidation() ) {
			ArangoCollection col = campaign.getDbCollection();
			if (col != null) {
				String id = campaign.getId();
				col.insertDocument(campaign, optionToUpsertInSilent());
				return id;
			}
		}
		return null;
	}
	
	/**
	 * @param cam
	 * @return
	 */
	public static Campaign loadFullCampaignData(Campaign cam) {
		String campaignId = cam.getId();
		List<ProductItem> items = AssetProductItemDaoUtil.getByCampaign(campaignId);
//		cam.setProductItems(items);
//		
//		cam.loadTemplateSelector();
		return cam;
	}

	/**
	 * @param id
	 * @return
	 */
	public static Campaign getCampaignById(String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		CallbackQuery<Campaign> callback = new CallbackQuery<Campaign>() {
			@Override
			public Campaign apply(Campaign obj) {
				return loadFullCampaignData(obj);
			}
		};
		Campaign s = new ArangoDbCommand<Campaign>(db, AQL_GET_CAMPAIGN_BY_ID, bindVars, Campaign.class, callback ).getSingleResult();
		return s;
	}
	
	/**
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Campaign> listCampaigns(int startIndex, int numberResult) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<Campaign> list = new ArangoDbCommand<Campaign>(db, AQL_GET_CAMPAIGNS_BY_PAGINATION, bindVars, Campaign.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @return
	 */
	public static List<Campaign> getCampaignsToDeleteForever() {
		ArangoDatabase db = getCdpDatabase();
		List<Campaign> list = new ArangoDbCommand<Campaign>(db, AQL_GET_CAMPAIGNS_TO_DELETE_FOREVER, Campaign.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param campaign
	 * @return
	 */
	public static String delete(Campaign campaign) {
		ArangoCollection col = campaign.getDbCollection();
		if (col != null) {
			return col.deleteDocument(campaign.getId()).getKey();
		}
		return null;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload filterCampaigns(DataFilter filter){
		int draw = filter.getDraw();
		List<Campaign> list = runFilterQuery(filter);
		long recordsTotal = countTotalOfCampaigns();
		long recordsFiltered = getTotalRecordsFiltered(filter);
		JsonDataTablePayload payload = JsonDataTablePayload.data(filter.getUri(), list, recordsTotal, recordsFiltered, draw);
		return payload;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	private static List<Campaign> runFilterQuery(DataFilter filter) {
		ArangoDatabase db = getCdpDatabase();
		//TODO dynamic query builder for filtering data
		List<Campaign> list = getCampaignsByPagination(filter, db);
		return list;
	}

	/**
	 * @param filter
	 * @param db
	 * @return
	 */
	private static List<Campaign> getCampaignsByPagination(DataFilter filter, ArangoDatabase db) {
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("startIndex", filter.getStart());
		bindVars.put("numberResult", filter.getLength());
		
		ArangoDbCommand<Campaign> q = new ArangoDbCommand<Campaign>(db, AQL_GET_CAMPAIGNS_BY_PAGINATION, bindVars, Campaign.class );
		List<Campaign> list = q.getResultsAsList();
		return list;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static long getTotalRecordsFiltered(DataFilter filter) {
		//TODO
		return countTotalOfCampaigns();
	}
	
	/**
	 * @return
	 */
	public static long countTotalOfCampaigns() {
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, AQL_COUNT_TOTAL_ACTIVE_CAMPAIGN, Long.class).getSingleResult();
		return c;
	}
}
