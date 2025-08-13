package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetContentDaoPublicUtil;
import leotech.cdp.dao.AssetContentDaoUtil;
import leotech.cdp.dao.AssetGroupDaoUtil;
import leotech.cdp.model.asset.AssetCategory;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.AssetType;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.SystemUser;
import leotech.system.util.KeywordUtil;

/**
 * Asset Group Management
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class AssetGroupManagement {

	// TODO add shared redis cache here

	public static String save(JsonObject paramJson, SystemUser loginUser) {
		String ownerId = loginUser.getKey();
		String groupId = paramJson.getString("groupId", "");
		String categoryId = paramJson.getString("categoryId", "");
		String title = paramJson.getString("title", "");
		String mediaInfo = paramJson.getString("mediaInfo", "");
		
		// digital asset type
		int assetType = paramJson.getInteger("assetType", AssetType.CONTENT_ITEM_CATALOG);
		
		// TODO need validate and check security data
		AssetGroup group;
		if (groupId.isEmpty()) {
			// create new page
			group = new AssetGroup(title,  categoryId, assetType, ownerId, mediaInfo);
		} else {
			// update existed page
			group = AssetGroupDaoUtil.getById(groupId);
			if (group.isEditable(ownerId)) {
				group.setTitle(title);
				group.setMediaInfo(mediaInfo);

				// update slug Content URI for SEO or user-friendly ID 
				String slug = paramJson.getString("slug", "");
				group.setSlug(slug);
			} else {
				throw new IllegalAccessError("You do not have permission to edit the content");
			}
		}

		group.setCategoryIds(Arrays.asList(categoryId));

		// privacyStatus for authorization check
		int privacyStatus = paramJson.getInteger("privacyStatus", 0);
		group.setPrivacyStatus(privacyStatus);

		// set score for ranking pages
		long rankingScore = paramJson.getLong("rankingScore");
		group.setRankingScore(rankingScore);

		// contentClass for JavaScript OOP
		String contentClass = paramJson.getString("contentClass", "");
		group.setContentClass(contentClass);

		// description default for SEO
		String description = paramJson.getString("description", "");
		if (!description.isEmpty()) {
			group.setDescription(description);
		}

		// headline images
		JsonObject jsonHeadlineImages = paramJson.getJsonObject("headlineImages", new JsonObject());
		Map<String, String> headlineImages = new HashMap<>(jsonHeadlineImages.size());
		jsonHeadlineImages.forEach(e -> {
			String key = e.getKey();
			String val = e.getValue().toString();
			if (!key.isEmpty()) {
				headlineImages.put(key, val);
			}
		});
		group.setHeadlineImages(headlineImages);

		// keywords
		group.clearKeywords();
		JsonArray jsonKeywords = paramJson.getJsonArray("keywords", new JsonArray());
		jsonKeywords.forEach(e -> {
			String keyword = e.toString();
			if (!keyword.isEmpty()) {
				group.setKeyword(keyword);
			}
		});
		
		// eventNamesForSegmentation
		group.clearEventNamesForSegmentation();
		JsonArray eventNamesForSegmentation = paramJson.getJsonArray("eventNamesForSegmentation", new JsonArray());
		eventNamesForSegmentation.forEach(e -> {
			String eventName = e.toString();
			if (!eventName.isEmpty()) {
				group.setEventNamesForSegmentation(eventName);
			}
		});

		// contentClass => keyword filters
		group.clearMapContentClassKeywords();
		JsonObject mapContentClassKeywords = paramJson.getJsonObject("mapContentClassKeywords", new JsonObject());
		mapContentClassKeywords.forEach(e -> {
			String className = e.getKey();
			JsonArray filterList = (JsonArray) e.getValue();
			List<String> keywords = new ArrayList<>(filterList.size());
			filterList.forEach(e1 -> {
				String keyword = e1.toString();
				if (!keyword.isEmpty()) {
					keywords.add(KeywordUtil.normalizeForSEO(keyword));
				}
			});
			group.setContentClassKeywords(className, keywords);
		});

		// custom data
		JsonObject jsonCustomData = paramJson.getJsonObject("customData", new JsonObject());
		Map<String, String> customData = new HashMap<>(jsonCustomData.size());
		jsonCustomData.forEach(e -> {
			String key = e.getKey();
			String val = e.getValue().toString();
			if (!key.isEmpty()) {
				customData.put(key, val);
			}
		});
		group.setCustomData(customData);

		return AssetGroupDaoUtil.save(group);
	}

	/**
	 * 
	 * must be carefull call this method
	 * 
	 * @param id
	 * @return
	 */
	public static boolean deleteAssetGroup(String id) {
		List<AssetContent> list = AssetContentDaoPublicUtil.listByGroup("", id);
		list.parallelStream().forEach(item->{
			AssetContentDaoUtil.delete(item.getId());
		});
		boolean ok = AssetGroupDaoUtil.delete(id) != null;
		return ok;
	}

	public static AssetGroup getGroupWithContents(String slug, int startIndex, int numberResult) {
		AssetGroup group = AssetGroupDaoUtil.getBySlug(slug);
		if(group != null) {
			String groupId = group.getId();
			List<AssetContent> posts = AssetContentDaoPublicUtil.list("",groupId, startIndex, numberResult);
			group.setContentItemsWithOrderByTime(posts);
		}
		return group;
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static AssetGroup getAssetGroupById(String id) {
		AssetGroup group = AssetGroupDaoUtil.getById(id);
		if(group == null) {
			throw new InvalidDataException("Not found AssetGroup with id: " + id);
		}
		return group;
	}

	/**
	 * @param categoryId
	 * @return
	 */
	public static List<AssetGroup> getAssetGroupsByCategory(String categoryId) {
		List<AssetGroup> groups = AssetGroupDaoUtil.listByCategory(categoryId);
		return groups;
	}
	
	/**
	 * @return
	 */
	public static List<AssetGroup> getAllAssetGroupsForSegmentation() {
		List<AssetGroup> groups = AssetGroupDaoUtil.getAllAssetGroupsForSegmentation();
		return groups;
	}

	/**
	 * @param categoryId
	 * @return
	 */
	public static List<AssetGroup> getAssetGroupsByCategoryWithPublicPrivacy(String categoryId) {
		List<AssetGroup> groups = AssetGroupDaoUtil.listByCategoryWithPublicPrivacy(categoryId);
		Collections.sort(groups);
		return groups;
	}

	/**
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<AssetGroup> getAssetGroups(int startIndex, int numberResult) {
		List<AssetGroup> pages = AssetGroupDaoUtil.list( startIndex, numberResult);
		return pages;
	}
	
	/**
	 * @param assetType
	 * @return
	 */
	public static List<AssetGroup> getAllGroupsByAssetType(int assetType) {
		AssetCategory cat = AssetCategoryManagement.getCategoryMapByAssetType().get(assetType);
		if(cat != null) {
			String categoryId = cat.getId();
			return getAssetGroupsByCategory(categoryId);
		}
		return new ArrayList<>(0);
	}
	
	/**
	 * get Default Group For Product
	 * 
	 * @return the Default AssetGroup
	 */
	public static AssetGroup getDefaultGroupForProduct() {		
		return AssetGroupDaoUtil.getDefaultGroupForProduct();
	}
	
	/**
	 * get Default Group  For Content
	 * 
	 * @return the Default AssetGroup
	 */
	public static AssetGroup getDefaultGroupForContent() {		
		return AssetGroupDaoUtil.getDefaultGroupForContent();
	}
	
	/**
	 * get Default Group Id For Presentation
	 * 
	 * @return
	 */
	public static AssetGroup getDefaultGroupForPresentation() {		
		return AssetGroupDaoUtil.getDefaultGroupForPresentation();
	}
}
