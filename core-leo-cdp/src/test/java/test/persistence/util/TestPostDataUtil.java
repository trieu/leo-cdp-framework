package test.persistence.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import leotech.cdp.dao.AssetContentDaoPublicUtil;
import leotech.cdp.dao.AssetContentDaoUtil;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.ContentType;
import rfx.core.util.StringUtil;

public class TestPostDataUtil {
	static String itemId = null;
	static String groupId = null;
	static String ownerId = "admin";

	@BeforeAll
	public void setup() {
		System.out.println("setup");
		groupId = "10000-5d88679c7945f3297d57321109b713d2ac1723ed";
	}

	@AfterAll
	public void clean() {
		System.out.println("clean");
	}

	@Test
	@Order(1)
	public void saveNewPost() {
		AssetContent post = new AssetContent();
		post.initNewItem("", "", "Viet Nam Market", "test", ContentType.HTML_TEXT, ownerId);
		itemId = AssetContentDaoUtil.save(post);
		System.out.println("PostDaoUtil.save " + itemId);
		assertTrue(itemId != null);
	}

	@Test
	@Order(2)
	public void getById() {
		AssetContent post = AssetContentDaoUtil.getById(itemId);
		assertNotNull(post);
		System.out.println(post.getTitle());
	}

	@Test
	@Order(3)
	public void updatePost() {
		AssetContent post = new AssetContent();
		post.initNewItem("", "", "Viet Nam Market", "test", ContentType.HTML_TEXT, ownerId);
		post.setGroupId(groupId);
		String updatePostId = AssetContentDaoUtil.save(post);
		System.out.println("PostDaoUtil.save " + itemId);
		assertTrue(itemId.equals(updatePostId));
	}

	@Test
	@Order(4)
	public void listByPage() {

		List<AssetContent> posts = AssetContentDaoPublicUtil.list("", groupId, 0, 5);
		for (AssetContent post2 : posts) {
			System.out.println("PostDaoUtil.listByPage " + post2.getTitle());
		}
		assertTrue(posts.size() > 0);
	}

	@Test
	@Order(5)
	public void addDocumentsPostForPage() {
		List<AssetContent> posts = AssetContentDaoPublicUtil.list("",groupId, 0, 50);
		if (posts.size() < 50) {
			for (int i = 1; i <= 50; i++) {
				AssetContent post = new AssetContent();
				String mediaInfo = "/public/uploaded-files/377f14d50d98c550c5569c91582e8c48362e7c40.pdf";
				post.initNewItem("", groupId, "Document " + i, mediaInfo , ContentType.OFFICE_DOCUMENT, ownerId);
				post.setGroupId(groupId);
				post.setKeyword("document");
				post.setKeyword("test");
				HashMap<String, String> images = new HashMap<String, String>();
				images.put("https://i.gadgets360cdn.com/large/pdf_pixabay_1493877090501.jpg", "");
				post.setHeadlineImages(images);
				post.setDescription("dest test");
				post.setPrivacyStatus(1);
				post.setType(ContentType.OFFICE_DOCUMENT);
				String itemId = AssetContentDaoUtil.save(post);
				System.out.println("PostDaoUtil.save " + itemId);
				assertTrue(StringUtil.isNotEmpty(itemId));
			}
		}
	}

}
