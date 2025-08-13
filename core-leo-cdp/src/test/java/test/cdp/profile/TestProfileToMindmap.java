package test.cdp.profile;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.model.customer.Profile;

public class TestProfileToMindmap {

	public static void main(String[] args) {
		String id = "5EzTqxmefv4xrU6hXKC9Ey";
		Profile p = ProfileDaoUtil.getProfileById(id);
	}
}
