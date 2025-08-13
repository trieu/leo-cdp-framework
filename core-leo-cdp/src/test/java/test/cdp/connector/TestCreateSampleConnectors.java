package test.cdp.connector;

import leotech.cdp.dao.DataServiceDaoUtil;
import leotech.cdp.model.activation.DataService;
import rfx.core.util.Utils;

public class TestCreateSampleConnectors {

	public static void main(String[] args) {
		for (int i = 100; i < 300; i++) {
			DataService c = new DataService("test "+i);
			c.setDescription(" demo test "+i);
			String id = DataServiceDaoUtil.save(c, true);
			System.out.println(id);
		}
		
		Utils.exitSystemAfterTimeout(2000);
	}
}
