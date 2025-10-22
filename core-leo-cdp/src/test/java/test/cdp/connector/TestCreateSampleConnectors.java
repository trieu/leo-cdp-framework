package test.cdp.connector;

import leotech.cdp.dao.AgentDaoUtil;
import leotech.cdp.model.activation.Agent;
import rfx.core.util.Utils;

public class TestCreateSampleConnectors {

	public static void main(String[] args) {
		for (int i = 100; i < 300; i++) {
			Agent c = new Agent("test "+i);
			c.setDescription(" demo test "+i);
			String id = AgentDaoUtil.save(c, true);
			System.out.println(id);
		}
		
		Utils.exitSystemAfterTimeout(2000);
	}
}
