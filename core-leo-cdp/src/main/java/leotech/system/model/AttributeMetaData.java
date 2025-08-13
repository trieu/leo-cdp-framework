package leotech.system.model;

import com.google.gson.Gson;

import rfx.core.util.StringUtil;

/**
 * for meta-data field config in ProfileModelUtil
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class AttributeMetaData {

	String attributeName = "";
	String label = "";
	String getterMethodName;

	boolean identityResolution = false;
	boolean indexing = false;
	boolean listing = true;
	boolean filtering = false;
	boolean synchronizable = false;

	String type;
	int dataQualityScore = 0;
	String invalidWhenEqual = "";

	public AttributeMetaData(String attributeName, String getterMethodName, String label, boolean identityResolution, boolean indexing,
			boolean listing, boolean filtering, String type, int dataQualityScore, String invalidWhenEqual,
			boolean synchronizable) {
		super();
		this.attributeName = attributeName;
		this.getterMethodName = getterMethodName;
		this.label = label;
		
		this.identityResolution = identityResolution;
		this.indexing = indexing;
		this.listing = listing;
		this.filtering = filtering;
		this.type = type;
		this.dataQualityScore = dataQualityScore;
		this.invalidWhenEqual = invalidWhenEqual;
		this.synchronizable = synchronizable;
	}

	public AttributeMetaData() {
		// gson
	}

	public String getInvalidWhenEqual() {
		return StringUtil.safeString(invalidWhenEqual);
	}

	public void setInvalidWhenEqual(String invalidWhenEqual) {
		this.invalidWhenEqual = invalidWhenEqual;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getGetterMethodName() {
		return getterMethodName;
	}

	public void setGetterMethodName(String getterMethodName) {
		this.getterMethodName = getterMethodName;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isIndexing() {
		return indexing;
	}

	public void setIndexing(boolean indexing) {
		this.indexing = indexing;
	}

	public boolean isListing() {
		return listing;
	}

	public void setListing(boolean listing) {
		this.listing = listing;
	}

	public boolean isFiltering() {
		return filtering;
	}

	public void setFiltering(boolean filtering) {
		this.filtering = filtering;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getDataQualityScore() {
		return dataQualityScore;
	}

	public void setDataQualityScore(int dataQualityScore) {
		this.dataQualityScore = dataQualityScore;
	}

	public boolean isSynchronizable() {
		return synchronizable;
	}

	public void setSynchronizable(boolean synchronizable) {
		this.synchronizable = synchronizable;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}