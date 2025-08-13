package test.cdp.activation;

import leotech.cdp.domain.ActivationRuleManagement;
import rfx.core.util.Utils;

public class ActivationServiceTest {

	public static void main(String[] args) {
		ActivationRuleManagement.startScheduledJobsForAllDataServices();
		Utils.exitSystemAfterTimeout(60000);
	}
	
}
