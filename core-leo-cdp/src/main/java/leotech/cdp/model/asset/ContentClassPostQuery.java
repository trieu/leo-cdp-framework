package leotech.cdp.model.asset;

public class ContentClassPostQuery {

	String key;
	String contentClass;
	String categoryId;

	int startIndex = 0;
	int limit = 10;

	public ContentClassPostQuery(String key, String contentClass, String categoryId) {
		super();
		this.key = key;
		this.contentClass = contentClass;
		this.categoryId = categoryId;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getContentClass() {
		return contentClass;
	}

	public void setContentClass(String contentClass) {
		this.contentClass = contentClass;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}
