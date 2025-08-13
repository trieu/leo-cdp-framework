package test.cdp.profile;

import leotech.cdp.model.customer.Profile;
import leotech.system.util.database.PersistentObject;

public class TestPartitioning {

	public static void main(String[] args) {
		String id = "11xpnxcj0FtYG4EM8dLihD";
		int p = PersistentObject.createPartitionId(id, Profile.class);
		System.out.println(p);
	}
}
