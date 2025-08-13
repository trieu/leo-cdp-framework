package leotech.cdp.model.analytics;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.SingleViewAnalyticalObject;
import leotech.system.util.IdGenerator;
import leotech.system.util.database.PersistentObject;

/**
 *  payment data event, the Truth of Universe with money <br>
 *  https://docs.google.com/spreadsheets/d/1fi1kgzn7l0n8Jyk3LTWygrKdtrZrOkaz/edit?usp=sharing&ouid=108357463841498827395&rtpof=true&sd=true
 * 
 * @author Trieu Nguyen
 * @since 2022
 *
 */
public final class FinanceEvent extends PersistentObject implements SingleViewAnalyticalObject {
	
	public static final String COLLECTION_NAME = getCdpCollectionName(FinanceEvent.class);
	static ArangoCollection dbCollection;
	
	@Expose
	@Key
	protected String id;
	
	@Expose
	protected Date createdAt, updatedAt;
	
	@Expose
	protected String refProfileId = "";
	
	@Expose
	protected String paymentType = ""; // what type
	
	@Expose
	protected String observerId = ""; // who see
	
	@Expose
	protected String refTouchpointHubId = ""; // at where (data hub)

	@Expose
	protected String srcTouchpointId= ""; // at where (data source ID)
	
	@Expose
	protected String srcTouchpointName = ""; // at where (data source name) 

	@Expose
	protected String srcTouchpointUrl = ""; // at where (data source URL)

	@Expose
	protected String refTouchpointId= ""; // from where (data source ID)
	
	@Expose
	protected String refTouchpointName = ""; // from where (data source name) 

	@Expose
	protected String refTouchpointUrl = "";  // from where (data source URL)
	
	@Expose
	protected String refApplicationId = ""; // the Application Record ID
	
	@Expose
	protected int period = 0;
	
	@Expose
	protected String periodType = "month";
	
	@Expose
	protected Date dueDate;
	
	@Expose
	protected long dueAmountValue = 0;

	@Expose
	protected long principalValue = 0;
	
	@Expose
	protected long interestValue = 0;
	
	@Expose
	protected boolean paid = false;
	
	@Expose
	protected Map<String, String> extData = new HashMap<>();

	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			dbCollection = arangoDatabase.collection(COLLECTION_NAME);
			
			// ensure indexing key fields for fast lookup
			
			dbCollection.ensurePersistentIndex(Arrays.asList("refProfileId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("paymentType"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("refApplicationId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("refTouchpointHubId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("refTouchpointId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("observerId"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("paid"), new PersistentIndexOptions().unique(false));
		}
		return null;
	}
	
	

	@Override
	public boolean dataValidation() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unifyData() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String buildHashedId() throws IllegalArgumentException {
		String keyHint = refApplicationId + refProfileId + paymentType + srcTouchpointUrl + observerId + createdAt;
		this.id = IdGenerator.createHashedId(keyHint);
		return null;
	}

	@Override
	public String getDocumentUUID() {
		// TODO Auto-generated method stub
		return null;
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

	public String getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}

	public String getObserverId() {
		return observerId;
	}

	public void setObserverId(String observerId) {
		this.observerId = observerId;
	}

	public String getRefTouchpointHubId() {
		return refTouchpointHubId;
	}

	public void setRefTouchpointHubId(String refTouchpointHubId) {
		this.refTouchpointHubId = refTouchpointHubId;
	}

	public String getRefTouchpointId() {
		return refTouchpointId;
	}

	public void setRefTouchpointId(String refTouchpointId) {
		this.refTouchpointId = refTouchpointId;
	}

	public String getRefApplicationId() {
		return refApplicationId;
	}

	public void setRefApplicationId(String refApplicationId) {
		this.refApplicationId = refApplicationId;
	}

	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public String getPeriodType() {
		return periodType;
	}

	public void setPeriodType(String periodType) {
		this.periodType = periodType;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public long getDueAmountValue() {
		return dueAmountValue;
	}

	public void setDueAmountValue(long dueAmountValue) {
		this.dueAmountValue = dueAmountValue;
	}

	public long getPrincipalValue() {
		return principalValue;
	}

	public void setPrincipalValue(long principalValue) {
		this.principalValue = principalValue;
	}

	public long getInterestValue() {
		return interestValue;
	}

	public void setInterestValue(long interestValue) {
		this.interestValue = interestValue;
	}

	public boolean isPaid() {
		return paid;
	}

	public void setPaid(boolean paid) {
		this.paid = paid;
	}

	public Map<String, String> getExtData() {
		return extData;
	}

	public void setExtData(Map<String, String> extData) {
		this.extData = extData;
	}

	public String getRefProfileId() {
		return refProfileId;
	}

	public void setRefProfileId(String refProfileId) {
		this.refProfileId = refProfileId;
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}
	
	@Override
	public int hashCode() {
		if(this.id != null) {
			return this.id.hashCode();
		}
		return 0;
	}
	
	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}

}
