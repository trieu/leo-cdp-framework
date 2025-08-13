package test.persistence.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import leotech.cdp.dao.AssetGroupDaoUtil;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.AssetType;
import leotech.cdp.model.asset.ContentType;

@TestMethodOrder(OrderAnnotation.class)
public class TestAssetGroupDataUtil {

	
	static String assetGroupId = null;
	static String ownerId = "root";
	static String categoryId = "test";
	static int contentType = ContentType.HTML_TEXT;


	@Test
	@Order(1)
	public void saveOne() {
		AssetGroup group = new AssetGroup("Viet Nam Market", categoryId, AssetType.CONTENT_ITEM_CATALOG, ownerId,"");
		assetGroupId = AssetGroupDaoUtil.save(group);
		System.out.println("PageDaoUtil.save " + assetGroupId);
		assertTrue(assetGroupId != null);
	}

	@Test
	@Order(2)
	public void getById() {
		System.out.println("assetGroupId " + assetGroupId);
		AssetGroup page = AssetGroupDaoUtil.getById(assetGroupId);
		assertNotNull(page);
		System.out.println(page.getTitle());
	}

	@Test
	@Order(3)
	public void saveList() {
		for (int i = 1; i <= 10; i++) {
			AssetGroup page = new AssetGroup("page" + i,  categoryId, AssetType.CONTENT_ITEM_CATALOG, ownerId,"");
			page.setCategoryId("6984204");
			String p = AssetGroupDaoUtil.save(page);
			System.out.println("PageDaoUtil.save " + p);
			assertTrue(p != null);
		}
	}

	@Test
	@Order(4)
	public void listByNetwork() {
		List<AssetGroup> pages = AssetGroupDaoUtil.list( 0, 10);
		System.out.println("pages.first " + pages.get(0).getTitle());
		System.out.println("pages.last " + pages.get(9).getTitle());
		assertTrue(pages.size() == 10);
	}
	
	@Test
	@Order(5)
	public void delete() {
		List<AssetGroup> assetGroups = AssetGroupDaoUtil.list( 0, 11);
		for (AssetGroup assetGroup : assetGroups) {
			String id = AssetGroupDaoUtil.delete(assetGroup.getId());
			System.out.println("deleted " + id);
		}
	}

}
