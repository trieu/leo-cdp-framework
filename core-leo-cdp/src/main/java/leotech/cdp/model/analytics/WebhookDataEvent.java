package leotech.cdp.model.analytics;

import java.util.Arrays;
import java.util.Date;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.annotations.Expose;

import leotech.system.util.IdGenerator;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * payment data event, the Truth of Universe with money <br>
 * https://docs.google.com/spreadsheets/d/1fi1kgzn7l0n8Jyk3LTWygrKdtrZrOkaz/edit?usp=sharing&ouid=108357463841498827395&rtpof=true&sd=true
 * 
 * @author Trieu Nguyen
 * @since 2022
 *
 */
public final class WebhookDataEvent extends PersistentObject {

	public static final String COLLECTION_NAME = getCdpCollectionName(WebhookDataEvent.class);
	static ArangoCollection dbCol;
	
	@Key
	@Expose
	protected String id;

	@Expose
	Date createdAt, updatedAt;

	@Expose
	String observerId = ""; // who see

	@Expose
	String refTouchpointHubId = ""; // at where (data hub)

	@Expose
	boolean processed = false;

	@Expose
	String payload;
	
	@Expose
	String source = "";

	@Override
	public ArangoCollection getDbCollection() {
		if (dbCol == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			dbCol = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields for fast lookup
			dbCol.ensurePersistentIndex(Arrays.asList("refTouchpointHubId"),new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("observerId"), new PersistentIndexOptions().unique(false));
		}
		if(dbCol != null) {
			return dbCol;
		}
		throw new NullPointerException("dbCol is NULL, can not update " + COLLECTION_NAME);
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(observerId) && StringUtil.isNotEmpty(refTouchpointHubId)
				&& StringUtil.isNotEmpty(payload) && this.createdAt != null;
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (dataValidation()) {
			this.id = IdGenerator.createHashedId(source + observerId + payload);
		}
		return this.id;
	}
	
	public WebhookDataEvent() {
		// gson
	}
	

	public WebhookDataEvent(String source, String observerId, String refTouchpointHubId, String payload) {
		super();
		this.source = source;
		this.observerId = observerId;
		this.refTouchpointHubId = refTouchpointHubId;
		this.payload = payload;
		this.createdAt = new Date();
		this.updatedAt = new Date();
		this.buildHashedId();
	}
	
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
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
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
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

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

	public String getPayload() {
		return payload != null ? payload : "";
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public final String getId() {
		return id;
	}

	@Override
	public String getDocumentUUID() {
		return getDocumentUUID(COLLECTION_NAME, this.id);
	}

}
