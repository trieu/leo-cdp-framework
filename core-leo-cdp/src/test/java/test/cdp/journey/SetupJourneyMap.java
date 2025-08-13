package test.cdp.journey;

import leotech.cdp.dao.TouchpointHubDaoUtil;
import leotech.cdp.domain.JourneyMapManagement;
import leotech.cdp.model.journey.TouchpointHub;
import rfx.core.util.Utils;

public class SetupJourneyMap {

	public static void main(String[] args) {
		JourneyMapManagement.initDefaultSystemData();
		TouchpointHubDaoUtil.save(TouchpointHub.DATA_OBSERVER);
//		String id = createNewAndSave("demo 2").getId();
//		System.out.println(id);
		Utils.exitSystemAfterTimeout(1000);
	}
}
