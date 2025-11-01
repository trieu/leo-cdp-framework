package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import leotech.cdp.dao.AssetContentDaoPublicUtil;
import leotech.cdp.dao.AssetContentDaoUtil;
import leotech.cdp.model.asset.AssetContent;
import leotech.system.version.SystemMetaData;

public class ContentItemManagement {

	private static final int NUMBER_SIMILAR_POSTS_IN_LIST = SystemMetaData.getInt("NUMBER_SIMILAR_POSTS_IN_LIST", 10);
	
	/**
	 * @param slug
	 * @return
	 */
	public static AssetContent getBySlug(String slug) {
		AssetContent p = AssetContentDaoPublicUtil.getBySlug(slug);
		return p;
	}

	/**
	 * @param id
	 * @param headlineOnly
	 * @return
	 */
	public static AssetContent getById(String id, boolean headlineOnly) {
		AssetContent p = AssetContentDaoUtil.getById(id, headlineOnly);
		return p;
	}
	
	public static AssetContent getById(String id) {
		AssetContent p = AssetContentDaoUtil.getById(id);
		return p;
	}

	/**
	 * @param contextGroupIds
	 * @param itemId
	 * @return
	 */
	public static List<AssetContent> getSimilarPosts(List<String> contextGroupIds, String itemId) {
		List<AssetContent> recommendedPosts = new ArrayList<>();
		for (String groupId : contextGroupIds) {
			List<AssetContent> posts = AssetContentDaoPublicUtil.listByGroup("",groupId).stream().filter(post -> {
				return !post.getId().equals(itemId);
			}).collect(Collectors.toList());

			if (posts.size() > NUMBER_SIMILAR_POSTS_IN_LIST) {
				Collections.shuffle(posts);
				posts = posts.subList(0, NUMBER_SIMILAR_POSTS_IN_LIST);
			}

			recommendedPosts.addAll(posts);
		}
		return recommendedPosts;
	}
}
