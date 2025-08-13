package leotech.cdp.dao.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoEdgeCollection;
import com.arangodb.entity.EdgeDefinition;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.graph.Profile2Conversion;
import leotech.cdp.model.journey.EventMetric;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;

/**
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class GraphProfile2Conversion extends AbstractCdpDatabaseUtil {

	private static final String GRAPH_NAME = Profile2Conversion.GRAPH_NAME;
	
	/**
	 * init graph data schema
	 */
	public final static void initGraph(ArangoDatabase db) {
		Collection<EdgeDefinition> edgeDefinitions = new ArrayList<>();
        EdgeDefinition edgeDefinition = new EdgeDefinition().collection(Profile2Conversion.COLLECTION_NAME).from(Profile.COLLECTION_NAME).to(ProductItem.COLLECTION_NAME);
        edgeDefinitions.add(edgeDefinition);
        
        // check before create
        if (!db.graph(GRAPH_NAME).exists()) {
        	db.createGraph(GRAPH_NAME, edgeDefinitions, null);
        }
        
        Profile2Conversion.initIndex();
	}
	

	/**
	 * @param profile
	 * @param product
	 * @param quantity
	 * @param eventMetric
	 * @param eventCount
	 * @param transactionId
	 * @param transactionValue
	 * @param currencyCode
	 */
	public final static void updateEdgeData(Date createdAt, Profile profile, ProductItem toProduct, int quantity, EventMetric eventMetric, String transactionId, double transactionValue, String currencyCode) {
		if(profile.getStatus() == Profile.STATUS_ACTIVE) {
			// init the edge
			Profile2Conversion insertedEdge = new Profile2Conversion(createdAt, profile, toProduct, quantity, eventMetric, transactionId, transactionValue, currencyCode);
			
			// get edge collect of graph
			ArangoEdgeCollection arangoEdgeCollection = Profile2Conversion.getArangoEdgeCollection();
			
			// loop from 1 to eventCount
			String key = insertedEdge.getKey();
			
			Profile2Conversion updatedEdge = arangoEdgeCollection.getEdge(key, Profile2Conversion.class);
			if(updatedEdge != null) {
				updatedEdge.updateEventScore(eventMetric.getScore());
				updatedEdge.setTransactionValue(transactionValue);
				updatedEdge.setQuantity(quantity);
				updatedEdge.setCurrencyCode(currencyCode);
				updatedEdge.setUpdatedAt(new Date());
				arangoEdgeCollection.updateEdge(key, updatedEdge);
			}
			else {
				arangoEdgeCollection.insertEdge(insertedEdge);
			}
		}
	}

	/**
	 * @param fromProfileId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<Profile2Conversion> getPurchasedProductItems(String fromProfileId, int startIndex, int numberResult) {
		return getPurchasedProductItems(fromProfileId, startIndex, numberResult, true);
	}

	/**
	 * @param oldProfileId
	 * @param newProfileId
	 * @return
	 */
	public static int updateFromOldProfileToNewProfile(String oldProfileId, String newProfileId) {
		ArangoEdgeCollection arangoEdgeCollection = Profile2Conversion.getArangoEdgeCollection();
		int numberResult = 200;
		int startIndex = 0;
		int c = 0;
		int maxLoop = 10;
		List<Profile2Conversion> list = getPurchasedProductItems(oldProfileId, startIndex, numberResult, false);
		while (!list.isEmpty() ) {
			for (Profile2Conversion updatedEdge : list) {
				String key = updatedEdge.getKey();
				
				String updatedProfileId = Profile.getDocumentUUID(newProfileId);
				if( ! updatedProfileId.equals(updatedEdge.getFromProfileId()) ) {
					updatedEdge.setFromProfileId(updatedProfileId);
					updatedEdge.setUpdatedAt(new Date());
					try {
						arangoEdgeCollection.updateEdge(key, updatedEdge);
						c++;
					} catch (ArangoDBException e) {
						e.printStackTrace();
					}
				}
				
			}
			startIndex = startIndex + numberResult;
			list = getPurchasedProductItems(oldProfileId, startIndex, numberResult, false);
			
			maxLoop--;
			if(maxLoop == 0) {
				break;
			}
		}
		return c;
	}
	
	/**
	 * @param fromProfileId
	 * @param startIndex
	 * @param numberResult
	 * @param withProduct
	 * @return
	 */
	static List<Profile2Conversion> getPurchasedProductItems(String fromProfileId, int startIndex, int numberResult, boolean withProduct) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("fromProfileId", Profile.getDocumentUUID(fromProfileId));
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		CallbackQuery<Profile2Conversion> callback = null;
		
		if(withProduct) {
			callback = new CallbackQuery<Profile2Conversion>() {
				@Override
				public Profile2Conversion apply(Profile2Conversion obj) {
					String toProductId = obj.getToProductId();
					ProductItem product = AssetProductItemDaoUtil.getById(toProductId);
					obj.setProduct(product);
					return obj;
				}
			};
		}
		
		String aql = Profile2Conversion.getGraphQueryForPurchasedProducts();
		List<Profile2Conversion> rs = new ArangoDbCommand<Profile2Conversion>(db, aql, bindVars, Profile2Conversion.class, callback).getResultsAsList();
		return rs;
	}
	
}
