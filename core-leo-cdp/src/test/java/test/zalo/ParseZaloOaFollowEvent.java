package test.zalo;

import java.util.List;

import leotech.cdp.dao.WebhookDataEventDao;
import leotech.cdp.domain.processor.ZaloWebhookProcessor;
import leotech.cdp.model.analytics.WebhookDataEvent;
import rfx.core.util.Utils;

public class ParseZaloOaFollowEvent {

	public static void main(String[] args) {
		
		List<WebhookDataEvent> events = WebhookDataEventDao.listAllBySource(ZaloWebhookProcessor.ZALO);
		for (WebhookDataEvent e : events) {
			ZaloWebhookProcessor.processWebhookEvent(e);
		}
	
		Utils.exitSystemAfterTimeout(1000);
		// ... (access follower data or other elements as needed)
	}

	
}
