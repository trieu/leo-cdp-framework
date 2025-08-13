package test.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.vertx.core.json.JsonObject;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.model.Notification;
import leotech.system.model.SystemUser;

@TestMethodOrder(OrderAnnotation.class)
public class TestNotification {

	@Test
	@Order(1)
	public void testCreate1() {
		JsonObject o = new JsonObject().put("message", "Ok").put("status", "test");
		String userKey = "178861";
		SystemUserDaoUtil.setNotification(userKey, new Notification(o));
		
		SystemUser u = SystemUserDaoUtil.getSystemUserByKey(userKey);
		List<Notification> notifications = u.getNotifications();
		assertTrue(notifications.size() > 0);
		
		Notification n = notifications.get(0);
		System.out.println(n);
		assertEquals(n.getMessage(), "Ok");
		assertEquals(n.getType(), "test");
	}
}
