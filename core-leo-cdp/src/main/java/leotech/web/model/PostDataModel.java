package leotech.web.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import leotech.cdp.model.asset.AssetContent;

public class PostDataModel extends WebData {

	private boolean isAdminRole = false;
	private String sessionUserId = "";
	private final List<AssetContent> posts;
	private List<AssetContent> recommendedPosts;
	private String contextPageId;

	public PostDataModel(String host, String templateFolder, String templateName, String pageTitle,
			List<AssetContent> posts) {
		super(host, templateFolder, templateName, pageTitle);
		this.posts = posts;
	}

	public PostDataModel(String host, String templateFolder, String templateName, String pageTitle, AssetContent post) {
		super(host, templateFolder, templateName, pageTitle);
		this.posts = Arrays.asList(post);
	}

	public List<AssetContent> getPosts() {
		return posts;
	}

	public List<AssetContent> getRecommendedPosts() {
		if (recommendedPosts == null) {
			recommendedPosts = new ArrayList<AssetContent>(0);
		}
		return recommendedPosts;
	}

	public void setRecommendedPosts(List<AssetContent> recommendedPosts) {
		this.recommendedPosts = recommendedPosts;
	}

	public boolean isAdminRole() {
		return isAdminRole;
	}

	public void setAdminRole(boolean isAdminRole) {
		this.isAdminRole = isAdminRole;
	}

	public String getSessionUserId() {
		return sessionUserId;
	}

	public void setSessionUserId(String sessionUserId) {
		this.sessionUserId = sessionUserId;
	}

	public String getContextPageId() {
		return contextPageId;
	}

	public void setContextPageId(String contextPageId) {
		this.contextPageId = contextPageId;
	}

}
