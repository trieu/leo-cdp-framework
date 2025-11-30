package test.persistence.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.model.AppMetadata;
import leotech.system.model.SystemUser;

public class TestUserDataUtil {
	private static final String TEST_EMAIL = "test1@gmail.com";
	private static final String PASS = "qwerty";
	private static final String USERLOGIN = "unit_test";
	static String userId = null;

	@BeforeAll
	public static void setup() {
		System.out.println("setup");

	}

	@AfterAll
	public static void clean() {
		System.out.println("clean");
		// boolean ok = !UserDaoUtil.deleteByUserLogin(USERLOGIN).isEmpty();
		// System.out.println("SETUP NEW TEST, clear data by deleteByUserLogin OK: " +
		// ok);
	}

	@Test
	@Order(1)
	public void saveNewUserAndActivate() {
		SystemUser user = new SystemUser(USERLOGIN, PASS, USERLOGIN, TEST_EMAIL, AppMetadata.DEFAULT_ID);
		userId = SystemUserDaoUtil.createNewSystemUser(user);
		System.out.println("UserDaoUtil.save " + userId);
		assertTrue(userId != null);

		boolean ok = SystemUserDaoUtil.activateAsSuperAdmin(user.getUserLogin());
		assertTrue(ok);

	}

	@Test
	@Order(2)
	public void checkLogin() {
		boolean ok = SystemUserDaoUtil.checkSystemUserLogin(USERLOGIN, PASS);
		System.out.println("UserDaoUtil.checkLogin " + USERLOGIN);
		assertTrue(ok);
	}

	@Test
	@Order(3)
	public void saveNew10UsersAndActivate() {
		for (int i = 1; i <= 10; i++) {
			String displayName = "Name Tester number:" + i;
			String userLogin = "tester" + i;
			SystemUser user = new SystemUser(userLogin, "123456", displayName, userLogin + "@example.com",
					AppMetadata.DEFAULT_ID);
			userId = SystemUserDaoUtil.createNewSystemUser(user);
			System.out.println("UserDaoUtil.save " + userId);
			assertTrue(userId != null);

			boolean ok = SystemUserDaoUtil.activateAsGuest(userLogin);
			assertTrue(ok);

			String id = SystemUserDaoUtil.deleteSystemUserByUserLogin(userLogin);
			assertTrue(user.getKey().equals(id));
		}
	}

	@Test
	@Order(4)
	public void getAllUsers() {
		List<SystemUser> users = SystemUserDaoUtil.listAllUsers(true);
		assertTrue(users.size() >= 10);
		for (SystemUser user : users) {
			System.out.println("listAllUsersInNetwork.user " + user.getUserLogin());
		}
	}

	@Test
	@Order(5)
	public void deactivateUser() {
		boolean ok = SystemUserDaoUtil.deactivateSystemUser(USERLOGIN);
		System.out.println("UserDaoUtil.deactivate " + USERLOGIN);
		assertTrue(ok);

		boolean mustBeFalse = !SystemUserDaoUtil.checkSystemUserLogin(USERLOGIN, "abc");
		assertTrue(mustBeFalse);
	}

}
