package test.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.vertx.core.json.JsonObject;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.model.AppMetadata;
import leotech.system.model.Notification;
import leotech.system.model.SystemUser;

@TestMethodOrder(OrderAnnotation.class)
public class TestNotification {

	private static final String USER_LOGIN = "notif_test_user";
	// Self-seeded: the old test hard-coded userKey "178861" (a stale historical key),
	// so it NPE'd whenever that user was absent. Create our own user and use its real key.
	private static String userKey;

	@BeforeAll
	public static void setup() {
		deleteIfExists(); // idempotent re-run (delete throws on absent user, so guard it)
		SystemUser user = new SystemUser(USER_LOGIN, "qwerty", "Notif Test", "notif_test@example.com", AppMetadata.DEFAULT_ID);
		userKey = SystemUserDaoUtil.createNewSystemUser(user);
	}

	@AfterAll
	public static void clean() {
		deleteIfExists();
	}

	private static void deleteIfExists() {
		if (SystemUserDaoUtil.getByUserLogin(USER_LOGIN) != null) {
			SystemUserDaoUtil.deleteSystemUserByUserLogin(USER_LOGIN);
		}
	}

	@Test
	@Order(1)
	public void testCreate1() {
		// Notification(JsonObject) reads the "type" key (not "status"); the assertion below
		// checks getType()=="test", so build with "type".
		JsonObject o = new JsonObject().put("message", "Ok").put("type", "test");
		SystemUserDaoUtil.setNotification(userKey, new Notification(o));

		SystemUser u = SystemUserDaoUtil.getSystemUserByKey(userKey);
		List<Notification> notifications = u.getNotifications();
		assertTrue(notifications.size() > 0);

		Notification n = notifications.getFirst();
		System.out.println(n);
		assertEquals(n.getMessage(), "Ok");
		assertEquals(n.getType(), "test");
	}
}
