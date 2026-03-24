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
 * Data Flow Stage is metadata of Data Journey Funnel. <br>
 * Data Journey Funnel is the process of data collection, data processing, data analysis, 
 * data activation, etc. that customer data goes through in the customer journey. <br>
 * Data Flow Stage is used to track, analyze, and optimize the customer journey funnel. <br><br>
 * 
 * ArangoDB collection: cdp_dataflowstage
 * 
 * @author tantrieuf31
 * @since 2020
 */
public class DataFlowStage extends PersistentObject {

	public static final String COLLECTION_NAME = getCdpCollectionName(DataFlowStage.class);
	
	// Volatile for thread-safe lazy initialization
	private static volatile ArangoCollection dbCollection;
	
	// Cache Slugify instance to prevent object thrashing and regex recompilation on every save
	private static final Slugify SLUGIFY = new Slugify();

	@Key
	@Expose
	protected String id;

	@Expose
	private int orderIndex;

	@Expose
	private String name;

	@Expose
	private String type;
	
	@Expose
	private String flowName;
	
	@Expose
	private Date createdAt;

	@Expose
	private Date updatedAt;
	
	@Expose
	private int flowType = FlowType.SYSTEM_METRIC; // 0 for system, 1 for marketing, 2 for sales, 3 for customer service

	public DataFlowStage() {
		// Default constructor for Gson serialization
	}

	public DataFlowStage(int orderIndex, String name, String type, String flowName, int flowType) {
		this.orderIndex = orderIndex;
		this.name = name;
		this.type = type;
		this.flowName = flowName;
		this.flowType = flowType;
		
		Date now = new Date();
		this.createdAt = now;
		this.updatedAt = now;
		
		this.buildHashedId();
	}
	
	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			// Double-checked locking to prevent race conditions during DB initialization
			synchronized (DataFlowStage.class) {
				if (dbCollection == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);
					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);
					
					col.ensurePersistentIndex(Arrays.asList("type", "orderIndex"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("flowType", "orderIndex"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("flowName", "orderIndex"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("name"), pIdxOpts);
					
					dbCollection = col;
				}
			}
		}
		return dbCollection;
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (StringUtil.isNotEmpty(this.name)) {
			this.id = SLUGIFY.slugify(this.name);
		}
		return this.id;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.id) 
				&& StringUtil.isNotEmpty(this.name) 
				&& StringUtil.isNotEmpty(this.flowName)  
				&& StringUtil.isNotEmpty(this.type);
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public int getOrderIndex() { return orderIndex; }
	public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	
	public String getFlowName() { return flowName; }
	public void setFlowName(String flowName) { this.flowName = flowName; }
	
	public int getFlowType() { return flowType; }
	public void setFlowType(int flowType) { this.flowType = flowType; }

	@Override
	public Date getCreatedAt() { return createdAt; }
	@Override
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

	@Override
	public Date getUpdatedAt() { return updatedAt; }
	@Override
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
	
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
	public int hashCode() {
		return StringUtil.isNotEmpty(id) ? this.id.hashCode() : 0;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}