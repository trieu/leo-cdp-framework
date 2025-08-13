package test.cdp.campaign;

import java.text.ParseException;
import java.util.Date;

import com.google.common.collect.Sets;

import leotech.cdp.domain.AutomatedFlowManagement;
import leotech.cdp.model.marketing.FlowFacts;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.FileUtils;
import rfx.core.util.StringUtil;

public class MarketingAutomationUnitTest {

	public static void main(String[] args) throws Exception {
		// Read JSON file
		String ruleJsonUri = "./data/marketing-automation-test/rules-for-unit-test.json";
		String jsonStr = FileUtils.readFileAsString(ruleJsonUri);
		if (jsonStr == null || StringUtil.isEmpty(jsonStr)) {
			System.err.println(" EMPTY DATA " + ruleJsonUri);
			return;
		}

		testHappyBirthdayEmailOK(jsonStr);
		testHappyBirthdayEmailSkip(jsonStr);
	}

	static void testHappyBirthdayEmailOK(String jsonStr) throws ParseException {
		Date dateOfBirth = DateTimeUtil.parseDateStr("03/07/2024");
		TestProfileModel profile = new TestProfileModel(Sets.newHashSet("purchase"),
				dateOfBirth, "John");
		FlowFacts facts = FlowFacts.buildFacts();
		facts.setProfile(profile);
		AutomatedFlowManagement.processFlowchartJson(jsonStr, facts);
	}

	static void testHappyBirthdayEmailSkip(String jsonStr) throws ParseException {
		Date dateOfBirth = DateTimeUtil.parseDateStr("03/07/2024");
		TestProfileModel profile = new TestProfileModel(Sets.newHashSet("purchase"),
				dateOfBirth, "Tom");
		FlowFacts facts = FlowFacts.buildFacts();
		facts.setProfile(profile);
		AutomatedFlowManagement.processFlowchartJson(jsonStr, facts);
	}

}
