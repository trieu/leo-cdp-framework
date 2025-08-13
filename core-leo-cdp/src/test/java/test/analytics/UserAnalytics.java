package test.analytics;

import java.util.ArrayList;
import java.util.List;

import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.model.AppMetadata;
import leotech.system.model.SystemUser;
import leotech.system.util.EncryptorAES;

public class UserAnalytics {

	public static void main(String[] args) {
		List<SystemUser> updatedPassUsers = new ArrayList<>();
		List<SystemUser> users = SystemUserDaoUtil.listAllUsersInNetwork(true, AppMetadata.DEFAULT_ID);
		int c1 = 0, c2 = 0;
		for (SystemUser user : users) {
			String userLogin = user.getUserLogin();
			String userPass = "12345678";
			String orginalPass = EncryptorAES.passwordHash(userLogin, userPass);
			if (orginalPass.equals(user.getUserPass())) {
				c1++;
				System.out.println(c1 + " NO CHANGE PASS " + user.getDisplayName() + " " + user.getUserPass());
			} else {
				c2++;
				updatedPassUsers.add(user);
				System.out.println(c2 + " CHANGE PASS " + user.getDisplayName() + " " + user.getUserPass());
			}
		}
	}
}
