package test.onesignal;

import leotech.system.communication.PushNotificationSender;
import rfx.core.util.Utils;

public class TestSimplePush {

	public static void main(String[] args) {
		
		String productLink = "https://demotrack.leocdp.net/ct/pMDwdSsxyrNB1nJxuqNLd";
		String content = "Artificial Intelligence Basics: A Non-Technical Introduction";
		String heading = "You may like this ";
		String userId = "";
		
		PushNotificationSender.notifyUser(userId, heading, content, productLink );
		Utils.exitSystemAfterTimeout(20000);
	}
	
}
