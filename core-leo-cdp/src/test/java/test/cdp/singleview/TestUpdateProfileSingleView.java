package test.cdp.singleview;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.model.customer.ProfileSingleView;
import rfx.core.util.Utils;

public class TestUpdateProfileSingleView {

	public static void main(String[] args) {
		String profileId = "7ivU9rTJQjhJlLeqiMzlaj";
		
		ProfileSingleView profile = ProfileDaoUtil.getProfileById(profileId);
		if(profile != null) {
			
		}
		else {
			System.err.println("Not found!");
		}
		Utils.exitSystemAfterTimeout(200000);
	}
}
