package test.cdp;

import leotech.cdp.domain.CampaignManagement;
import leotech.cdp.domain.ProfileQueryManagement;
import leotech.cdp.model.customer.ProfileSingleView;

public class TestMarketingAuto {

	public static void main(String[] args) {
		ProfileSingleView profile = ProfileQueryManagement.getByIdForSystem("5ZW1UPZZFaDPtycIKAfsBj");
		CampaignManagement.doMarketingAutomation(profile );
	}
}
