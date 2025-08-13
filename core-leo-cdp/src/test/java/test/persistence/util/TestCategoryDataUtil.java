package test.persistence.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import leotech.cdp.dao.AssetCategoryDaoUtil;
import leotech.cdp.model.asset.AssetCategory;

public class TestCategoryDataUtil {
	static String saveId = "";

	@BeforeAll
	public void setup() {
		saveId = "";
		System.out.println("setup");
	}

	@AfterAll
	public void clean() {
		AssetCategoryDaoUtil.deleteById(saveId);
		System.out.println("clean");
	}

	@Test
	@Order(1)    
	public void saveOne() {
		AssetCategory cat = new AssetCategory("test", 10001);
		saveId = AssetCategoryDaoUtil.save(cat);
		System.out.println("CategoryDaoUtil.save " + saveId);
		assertTrue(saveId != null);
	}

	@Test
	@Order(2)    
	public void getById() {
		AssetCategory cat = AssetCategoryDaoUtil.getById(saveId);
		assertNotNull(cat);
		assertTrue(cat.getNetworkId() == 10001);
		System.out.println(cat.getName());
	}

	@Test
	@Order(3)    
	public void saveList() {
		for (int i = 1; i <= 5; i++) {
			AssetCategory page = new AssetCategory("category-" + i, 10001);
			String p = AssetCategoryDaoUtil.save(page);
			System.out.println("CategoryDaoUtil.save " + p);
			assertTrue(p != null);
		}
	}

	@Test
	@Order(4) 
	public void listByNetwork() {
		List<AssetCategory> cats = AssetCategoryDaoUtil.listAllByNetwork(10001);
		assertTrue(cats.size() > 0);
		for (AssetCategory cat : cats) {
			System.out.println("cats.first " + cat.getName() + " key " + cat.getId());
		}
	}
}
