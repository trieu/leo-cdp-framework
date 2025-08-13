package test.cdp.systemevent;

import java.util.List;

import leotech.cdp.model.activation.ActivationRule;
import leotech.system.domain.SystemEventManagement;
import leotech.system.model.SystemEvent;
import leotech.system.model.SystemUser;
import rfx.core.util.Utils;

public class TestSystemEvent {

	public static void main(String[] args) {
		//testSaveSystemEvent();
		testGetSystemEvent();
		Utils.exitSystemAfterTimeout(2000);
	}

	public static void testGetSystemEvent() {
		List<SystemEvent> systemEvents = SystemEventManagement.getByClassNameAndId(ActivationRule.class, "5mDxIfGZ7Lbgou7kXPtEDN", 0, 2);
		for (SystemEvent systemEvent : systemEvents) {
			System.out.println(systemEvent);
		}
	}
	
	public static void testSaveSystemEvent() {
		String data = "sync [Jetsetter Stretch Wool Suit] to SendInBlue";
		SystemEvent systemEvent = new SystemEvent(SystemUser.SUPER_ADMIN_LOGIN, ActivationRule.class,"5mDxIfGZ7Lbgou7kXPtEDN", "synchronization",data);
		SystemEventManagement.save(systemEvent);
	}
}
