package leotech.cdp.model.journey;

import java.util.Arrays;
import java.util.Date;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * data flow stage is metadata of Data Journey Funnel
 * 
 * @author tantrieuf31
 * @since
 *
 */
public class DataFlowStage extends PersistentObject {

	public static final String COLLECTION_NAME = getCdpCollectionName(DataFlowStage.class);
	static ArangoCollection dbCollection;

	@Key
	@Expose
	protected String id;

	@Expose
	int orderIndex;

	@Expose
	String name;

	@Expose
	String type;
	
	@Expose
	String flowName;
	
	@Expose
	Date createdAt;

	@Expose
	Date updatedAt;
	
	@Expose
	int flowType = FlowType.SYSTEM_METRIC; // 0 for system, 1 for marketing, 2 for sales, 3 for customer service

	public DataFlowStage() {
		// for gson
	}

	public DataFlowStage(int orderIndex, String name, String type, String flowName, int flowType) {
		super();
		this.orderIndex = orderIndex;
		this.name = name;
		this.type = type;
		this.flowName = flowName;
		this.createdAt = new Date();
		this.updatedAt = new Date();
		this.flowType = flowType;
		// hashed ID
		this.buildHashedId();
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if(StringUtil.isNotEmpty(this.name) ) {
			this.id = new Slugify().slugify(this.name);
		}
		return this.id;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.id) && StringUtil.isNotEmpty(this.name) && StringUtil.isNotEmpty(this.flowName)  && StringUtil.isNotEmpty(this.type);
	}

	public int getOrderIndex() {
		return orderIndex;
	}
	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}
	
	public int getFlowType() {
		return flowType;
	}

	public void setFlowType(int flowType) {
		this.flowType = flowType;
	}

	@Override
	public int hashCode() {
		if(StringUtil.isNotEmpty(id)) {
			return this.id.hashCode();
		}
		return 0;
	}

	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();
			dbCollection = arangoDatabase.collection(COLLECTION_NAME);
			// ensure indexing key fields for fast lookup
			dbCollection.ensurePersistentIndex(Arrays.asList("type"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("name"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("type","orderIndex"), new PersistentIndexOptions().unique(false));
		}
		return dbCollection;
	}


	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public Date getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}
	
	public final boolean isCustomerMetric() {
		return EventMetric.NEW_CUSTOMER.equalsIgnoreCase(this.id) || EventMetric.ENGAGED_CUSTOMER.equalsIgnoreCase(this.id);
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
