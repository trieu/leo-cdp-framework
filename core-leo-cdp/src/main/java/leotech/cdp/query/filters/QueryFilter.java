package leotech.cdp.query.filters;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

public abstract class QueryFilter {

	public final static int ORDER_BY_TIME_DESC = 1;
	public final static int ORDER_BY_TIME_ASC = 2;

	public final static int ORDER_BY_SCORE_DESC = 3;
	public final static int ORDER_BY_SCORE_ASC = 4;

	@Expose
	protected String name;

	@Expose
	protected List<String> categoryIds = new ArrayList<>();

	@Expose
	protected List<String> groupIds = new ArrayList<>();

	@Expose
	protected List<String> postIds = new ArrayList<>();

	@Expose
	protected List<String> keywords = new ArrayList<>();

	@Expose
	protected List<String> topicKeys = new ArrayList<>(16);// by user-generated

	@Expose
	protected int orderBy = ORDER_BY_TIME_DESC;

	protected long networkId = 0; // the content network ID

	public QueryFilter(String name, long networkId) {
		this.name = name;
		this.networkId = networkId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getCategoryIds() {
		return categoryIds;
	}

	public void setCategoryIds(List<String> categoryIds) {
		this.categoryIds = categoryIds;
	}



	public List<String> getGroupIds() {
		return groupIds;
	}

	public void setGroupIds(List<String> groupIds) {
		this.groupIds = groupIds;
	}

	public List<String> getPostIds() {
		return postIds;
	}

	public void setPostIds(List<String> postIds) {
		this.postIds = postIds;
	}

	public List<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}

	public List<String> getTopicKeys() {
		return topicKeys;
	}

	public void setTopicKeys(List<String> topicKeys) {
		this.topicKeys = topicKeys;
	}

	public int getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(int orderBy) {
		this.orderBy = orderBy;
	}

	public long getNetworkId() {
		return networkId;
	}

	public void setNetworkId(long networkId) {
		this.networkId = networkId;
	}

}
