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
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.journey.EventMetric;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * The connection from Profile to ProductItem
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class Profile2Product extends ProfileGraphEdge {

	public static final String GRAPH_NAME =  PersistentObject.CDP_COLLECTION_PREFIX + "product_graph";
	public static final String COLLECTION_NAME = PersistentObject.getCdpCollectionName(Profile2Product.class);
	
	public static final String AQL_REMOVE_EDGES_BY_GROUP = "FOR e IN "+COLLECTION_NAME
			+" FILTER e.eventMetricId == 'recommend' AND @groupId IN e.assetGroupIds[*] REMOVE e IN " + COLLECTION_NAME;
	public static final String AQL_REMOVE_EDGES_BY_GROUP_AND_SEGMENT = "FOR e IN "+COLLECTION_NAME 
			+" FILTER e.eventMetricId == 'recommend' AND @groupId IN e.assetGroupIds[*] AND @segmentId IN e.segmentIds[*] REMOVE e IN " + COLLECTION_NAME;
	public static final String AQL_REMOVE_EDGES_BY_PROFILE = "FOR e IN "+COLLECTION_NAME 
			+" FILTER e.eventMetricId == 'recommend' AND @fromProfileId == e._from REMOVE e IN " + COLLECTION_NAME;
	
	static ArangoEdgeCollection edgeCollection = null;
	static ArangoCollection dataCollection = null;

	@To
	@Expose
	String toProductId;
	
	@Expose
	float discount = 0F;
	
	@Expose
	String discountCode = "";
	
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	@Expose
	ProductItem product = null;

	public Profile2Product() {
		// gson
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
	}
	
	public Profile2Product(Date createdAt, ProfileIdentity fromProfile, ProductItem toProduct, int score, String targetMediaUnitId, String segmentId) {
		super();
		this.eventMetricId = BehavioralEvent.General.RECOMMEND;
		this.eventScore = score;
		
		this.fromProfileId = fromProfile.getDocumentUUID();
		this.toProductId = toProduct.getDocumentUUID();
		this.targetMediaUnitId = targetMediaUnitId;
		this.assetGroupIds.addAll(toProduct.getGroupIds());
		this.segmentIds.add(segmentId);
		
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
		this.buildHashedId();
	}
	

	public Profile2Product(Date createdAt, Profile fromProfile, ProductItem toProduct, EventMetric eventMetric) {
		super();
		this.eventMetricId = eventMetric.getId();
		this.eventScore = eventMetric.getScore();
		
		this.fromProfileId = fromProfile.getDocumentUUID();
		this.toProductId = toProduct.getDocumentUUID();
		this.assetGroupIds.addAll(toProduct.getGroupIds());
		
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
		this.buildHashedId();
	}

	@Override
	public String buildHashedId() {
		String keyHint = this.eventMetricId + this.fromProfileId + this.toProductId + this.discountCode;
		this.key = PersistentObject.createHashedId(keyHint);
		return this.key;
	}
	
	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.eventMetricId) && StringUtil.isNotEmpty(this.fromProfileId) && StringUtil.isNotEmpty(this.toProductId);
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
	
	

	public float getDiscount() {
		return discount;
	}

	public void setDiscount(float discount) {
		this.discount = discount;
	}

	public String getDiscountCode() {
		return discountCode;
	}

	public void setDiscountCode(String discountCode) {
		this.discountCode = discountCode;
	}

	public synchronized static ArangoEdgeCollection getArangoEdgeCollection() {
		if(edgeCollection == null) {
			ArangoDatabase db = AbstractCdpDatabaseUtil.getCdpDatabase();
			ArangoGraph graph = db.graph(GRAPH_NAME);
			edgeCollection = graph.edgeCollection(COLLECTION_NAME);
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
		new Profile2Product().getDbCollection();
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
