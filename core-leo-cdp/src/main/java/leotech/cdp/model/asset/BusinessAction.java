package leotech.cdp.model.asset;

import java.util.Date;
import java.util.Objects;

/**
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class BusinessAction {
	
	String name;
	
	String description;
	
	Date createdAt = new Date();
	
	public BusinessAction() {
		// TODO Auto-generated constructor stub
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

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
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

