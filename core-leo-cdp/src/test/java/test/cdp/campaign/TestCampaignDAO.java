package test.cdp.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import leotech.cdp.dao.CampaignDaoUtil;
import leotech.cdp.model.activation.ActivationRule;
import leotech.cdp.model.marketing.Campaign;
import leotech.cdp.query.filters.DataFilter;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import rfx.core.util.FileUtils;
import rfx.core.util.StringUtil;

@TestMethodOrder(OrderAnnotation.class)
public class TestCampaignDAO {

	static volatile ActivationRule activationRule;
	static volatile Campaign campaign;
	static volatile String createdCampaignId;
	static List<String> targetedSegmentIds = Arrays.asList("3og1APugf5q4dqxuFh9yLI");
	static StringBuilder output = new StringBuilder("");

	static {
		// TODO load from database
		String condition = "campaign.doActivation(profile, templateSelectorKey);";
		activationRule = ActivationRule.create(SystemUser.SUPER_ADMIN_LOGIN, "unit-test",
				ActivationRule.EVENT_TRIGGER_TASK, CampaignTestUtil.activateRuleName, condition);
		activationRule.setAction(condition);

		cleanTestData();
	}

	@Test
	@Order(1)
	public void testCreate1() {
		assertNotNull(activationRule);
		String ruleJsonUri = "./data/marketing-automation-test/rules-for-unit-test.json";
		String name = CampaignTestUtil.PREFIX_TITLE_DEMO_TEST + " for happy birthday";

		try {
			String jsonStr = FileUtils.readFileAsString(ruleJsonUri);
			campaign = new Campaign(targetedSegmentIds, name, jsonStr);
			campaign.setDescription("ruleJsonUri " + ruleJsonUri);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String createdCampaignId = CampaignDaoUtil.saveCampaign(campaign);
		boolean hasData = StringUtil.isNotEmpty(campaign.getAutomatedFlowJson());
		if (!hasData) {
			System.err.println("getAutomatedFlowJson is NULL");
		}
		assertTrue(hasData);
	}

	@Test
	@Order(1)
	public void testCreate2() {
		assertNotNull(activationRule);
		String ruleJsonUri = "./data/marketing-automation-test/rules-for-conversion.json";
		String name = CampaignTestUtil.PREFIX_TITLE_DEMO_TEST + " for conversion";

		try {
			String jsonStr = FileUtils.readFileAsString(ruleJsonUri);
			campaign = new Campaign(targetedSegmentIds, name, jsonStr);
			campaign.setDescription("ruleJsonUri " + ruleJsonUri);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String createdCampaignId = CampaignDaoUtil.saveCampaign(campaign);
		boolean hasData = StringUtil.isNotEmpty(campaign.getAutomatedFlowJson());
		if (!hasData) {
			System.err.println("getAutomatedFlowJson is NULL");
		}
		assertTrue(hasData);
	}

	@Test
	@Order(1)
	public void testCreateWithProducts() {
		assertNotNull(activationRule);
		String ruleJsonUri = "./data/marketing-automation-test/rules-for-unit-test.json";
		String name = CampaignTestUtil.PREFIX_TITLE_DEMO_TEST + " testCreateWithProducts";

		try {
			String jsonStr = FileUtils.readFileAsString(ruleJsonUri);
			campaign = new Campaign(targetedSegmentIds, name, jsonStr);
			campaign.setDescription("ruleJsonUri " + ruleJsonUri);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		createdCampaignId = CampaignDaoUtil.saveCampaign(campaign);

		boolean hasData = StringUtil.isNotEmpty(campaign.getAutomatedFlowJson());
		if (!hasData) {
			System.err.println("Need product data to test");
		}
		assertTrue(hasData);
	}

	@Test
	@Order(2)
	public void testRead() {
		assertNotNull(createdCampaignId);
		assertNotNull(campaign);
		System.out.println(createdCampaignId);
		Campaign queriedCampaign = CampaignDaoUtil.getCampaignById(createdCampaignId);
		assertEquals(queriedCampaign.getTagName(), campaign.getTagName());
		output.append("testRead queriedCampaign.tagName " + queriedCampaign.getTagName() + " \n ");
	}

	@Test
	@Order(3)
	public void testUpdate() {
		assertNotNull(campaign);
		campaign.setDescription("updated at " + new Date());

		String updatedId = CampaignDaoUtil.saveCampaign(campaign);
		assertNotNull(updatedId);
		Campaign queriedCampaign = CampaignDaoUtil.getCampaignById(updatedId);
		assertEquals(queriedCampaign.getDescription(), campaign.getDescription());

		output.append("testUpdate queriedCampaign.description " + queriedCampaign.getDescription() + " \n ");
	}

	@Test
	@Order(4)
	public void testList() {
		String ruleJsonUri = "./data/marketing-automation-test/rules-for-unit-test.json";
		for (int i = 0; i < 10; i++) {
			String name = CampaignTestUtil.PREFIX_TITLE_DEMO_TEST + " index " + i;
			try {
				String jsonStr = FileUtils.readFileAsString(ruleJsonUri);
				campaign = new Campaign(targetedSegmentIds, name, jsonStr);
				campaign.setDescription("ruleJsonUri " + ruleJsonUri);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String createdId = CampaignDaoUtil.saveCampaign(campaign);
			assertNotNull(createdId);
		}

		JsonDataTablePayload campaigns = CampaignDaoUtil.filterCampaigns(new DataFilter(0, 4));
		System.out.println(campaigns);
		long actual = campaigns.getRecordsTotal();

		assertEquals(true, actual > 0);
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
