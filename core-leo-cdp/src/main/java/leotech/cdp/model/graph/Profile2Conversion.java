package leotech.cdp.model.graph;

import java.util.Date;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoEdgeCollection;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.To;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.EventMetric;
import leotech.system.exception.InvalidDataException;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * The connection from Profile to Purchased Item
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class Profile2Conversion extends ProfileGraphEdge {

	public static final String GRAPH_NAME =  PersistentObject.CDP_COLLECTION_PREFIX + "conversion_graph";
	public static final String COLLECTION_NAME = PersistentObject.getCdpCollectionName(Profile2Conversion.class);
	
	public static final String AQL_REMOVE_EDGES_BY_GROUP = "FOR e IN "+COLLECTION_NAME+" FILTER @groupId IN e.groupIds[*] REMOVE e IN " + COLLECTION_NAME;
	
	public static final String getGraphQueryForPurchasedProducts() {
		StringBuilder aql = new StringBuilder();
		aql.append(" LET edgeList = ( ");
		aql.append(" FOR v, e, p IN 1..1 OUTBOUND @fromProfileId GRAPH ").append(GRAPH_NAME);
		aql.append(" FILTER LENGTH(p.edges[0].transactionId) > 0 LIMIT @startIndex,@numberResult ");
		aql.append(" RETURN p.edges[0]) FOR e IN edgeList SORT e.createdAt DESC RETURN e ");
		return aql.toString();
	}
	
	static ArangoEdgeCollection edgeCollection = null;
	static ArangoCollection dataCollection = null;

	@To
	@Expose
	String toProductId;
	
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	@Expose
	ProductItem product = null;
	
	@Expose
	int quantity = 0;
	
	@Expose
	String transactionId = "";
	
	@Expose
	double transactionValue = 0;
	
	@Expose
	String currencyCode;
	

	public Profile2Conversion() {
		// gson
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
	}
	
	public Profile2Conversion(Date createdAt, Profile fromProfile, ProductItem toProduct, int quantity, EventMetric eventMetric, String transactionId, double transactionValue, String currencyCode) {
		super();
		this.eventMetricId = eventMetric.getId();
		this.eventScore = eventMetric.getScore();
		
		this.fromProfileId = fromProfile.getDocumentUUID();
		this.toProductId = toProduct.getDocumentUUID();
		this.quantity = quantity;
		this.assetGroupIds.addAll(toProduct.getGroupIds());
		this.transactionId = transactionId;
		this.transactionValue = transactionValue;
		this.currencyCode = currencyCode;
		
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
		this.buildHashedId();
	}

	@Override
	public String buildHashedId() {
		if(this.createdAt != null) {
			String keyHint = this.transactionValue + this.eventMetricId + this.fromProfileId + this.toProductId + this.transactionId + this.createdAt.getTime();
			this.key = PersistentObject.createHashedId(keyHint);
			return this.key;	
		}
		else {
			throw new InvalidDataException("Profile2Conversion.createdAt is NULL");
		}
	}
	
	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.eventMetricId) && StringUtil.isNotEmpty(this.fromProfileId) && StringUtil.isNotEmpty(this.toProductId) && this.createdAt != null;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + this.key;
	}
	
	public static String getCollectionName() {
		return COLLECTION_NAME;
	}

	public String getToProductId() {
		return toProductId.split("/")[1];
	}
	
	public ProductItem getProduct() {
		return product;
	}

	public void setProduct(ProductItem product) {
		this.product = product;
	}
	
	public String getTransactionId() {
		if(transactionId == null) {
			transactionId = "";
		}
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public double getTransactionValue() {
		return transactionValue;
	}

	public void setTransactionValue(double transactionValue) {
		this.transactionValue = transactionValue;
	}

	public synchronized static ArangoEdgeCollection getArangoEdgeCollection() {
		if(edgeCollection == null) {
			ArangoDatabase db = AbstractCdpDatabaseUtil.getCdpDatabase();
			ArangoGraph graph = db.graph(GRAPH_NAME);
			edgeCollection = graph.edgeCollection(Profile2Conversion.COLLECTION_NAME);
		}
		return edgeCollection;
	}

	@Override
	public ArangoCollection getDbCollection() {
		if(dataCollection == null) {
			dataCollection = getArangoCollection(COLLECTION_NAME);
		}
		return dataCollection;
	}
	
	public static void initIndex() {
		new Profile2Conversion().getDbCollection();
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}
}
