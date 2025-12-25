package test.cdp.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import leotech.cdp.dao.CampaignDaoUtil;
import leotech.cdp.model.marketing.Campaign;

@TestMethodOrder(OrderAnnotation.class)
public class TestCampaignDAO {

	static {
		// TODO load from database

		cleanTestData();
	}

	@Test
	@Order(1)
	public void testCreate1() {

	}

	@Test
	@Order(1)
	public void testCreate2() {

	}

	@Test
	@Order(1)
	public void testCreateWithProducts() {

	}

	@Test
	@Order(2)
	public void testRead() {

	}

	@Test
	@Order(3)
	public void testUpdate() {

	}

	@Test
	@Order(4)
	public void testList() {

	}

	@Test
	@Order(7)
	public void testDelete() {
		int c = cleanTestData();
		System.out.println("Deleted campaign: " + c);
	}

	static int cleanTestData() {
		int c = 0;
		List<Campaign> campaigns = CampaignDaoUtil.listCampaigns(0, 100);
		for (Campaign campaign : campaigns) {
			if (campaign.getName().startsWith(CampaignTestUtil.PREFIX_TITLE_DEMO_TEST)) {
				String deletedId = CampaignDaoUtil.delete(campaign);
				assertEquals(campaign.getId(), deletedId);
				c++;
			}
		}
		return c;
	}

	@Test
	@Order(8)
	public void testDone() {
		int c = 0;
		List<Campaign> campaigns = CampaignDaoUtil.listCampaigns(0, 100);
		for (Campaign campaign : campaigns) {
			if (campaign.getName().startsWith(CampaignTestUtil.PREFIX_TITLE_DEMO_TEST)) {
				c++;
			}
		}
		assertEquals(c, 0);
	}

	////////////////////////////// test utility methods
	////////////////////////////// /////////////////////////////////

}
