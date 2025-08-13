package leotech.cdp.model.asset;

import java.util.Date;
import java.util.Objects;

/**
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class BusinessDeal  {

	String name;
	
	String description;
	
	String documentUrl;
	
	long offeringPrice;
	
	long dealPrice;
	
	Date createdAt = new Date();
	
	Date updatedAt = new Date();
	
	public BusinessDeal() {
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDocumentUrl() {
		return documentUrl;
	}

	public void setDocumentUrl(String documentUrl) {
		this.documentUrl = documentUrl;
	}

	public long getOfferingPrice() {
		return offeringPrice;
	}

	public void setOfferingPrice(long offeringPrice) {
		this.offeringPrice = offeringPrice;
	}

	public long getDealPrice() {
		return dealPrice;
	}

	public void setDealPrice(long dealPrice) {
		this.dealPrice = dealPrice;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}
	
	@Override
	public boolean equals(Object obj) {
		return hashCode() == obj.hashCode();
	}
}
