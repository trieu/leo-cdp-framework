package leotech.cdp.model.customer;

import java.util.Date;

import com.arangodb.ArangoCollection;
import com.arangodb.entity.Key;
import com.google.gson.annotations.Expose;

import leotech.system.util.database.PersistentObject;

/**
 * customer support cases or consulting cases
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class BusinessCase extends PersistentObject {

	public static final String COLLECTION_NAME = getCdpCollectionName(BusinessCase.class);
	static ArangoCollection dbCol;
	
	@Key
	@Expose
	String id;

	@Expose
	protected Date createdAt = new Date();

	@Expose
	protected Date updatedAt = new Date();

	@Override
	public ArangoCollection getDbCollection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean dataValidation() {
		// TODO Auto-generated method stub
		return false;
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
	public String buildHashedId() throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return this.id;
	}

	@Override
	public String getDocumentUUID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}
}
