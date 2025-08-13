package test.persistence;

import static org.junit.jupiter.api.Assertions.assertTrue;

import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.model.SystemUser;

public class ResetSystemUserPass {

	public static void main(String[] args) {
		String userLogin = SystemUser.SUPER_ADMIN_LOGIN;
		// SystemUser user = new SystemUser(userLogin, "FullstackEngineer@2020", userLogin, "trieu@leocdp.com", AppMetadata.DEFAULT_ID);
		String userId = SystemUserDaoUtil.updateSystemUserPassword(userLogin,  "FullstackEngineer@2020");
		System.out.println("UserDaoUtil.save " + userId);
		assertTrue(userId != null);

		boolean ok = SystemUserDaoUtil.activateAsStandarUser(userLogin);
		System.out.println(ok);
	}
}
