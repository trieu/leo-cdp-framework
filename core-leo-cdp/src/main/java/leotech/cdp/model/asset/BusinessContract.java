package leotech.cdp.model.asset;

import java.util.Objects;

/**
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class BusinessContract {

	String contractId = "";

	String name = "";

	String description = "";

	String documentUrl = "";

	public BusinessContract() {

	}

	public String getContractId() {
		return contractId;
	}

	public void setContractId(String contractId) {
		this.contractId = contractId;
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

	@Override
	public int hashCode() {
		return Objects.hashCode(documentUrl + contractId);
	}

	@Override
	public boolean equals(Object obj) {
		return hashCode() == obj.hashCode();
	}

}
