package leotech.cdp.domain.processor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.model.analytics.WebhookDataEvent;
import leotech.cdp.model.journey.EventObserver;

/**
 * Zalo OA Data Processor
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class FacebookWebhookProcessor {
	
	public static final String FACEBOOK = "facebook";

	public static void processWebhookEvent(WebhookDataEvent e) {

		EventObserver observer = EventObserverManagement.getById(e.getObserverId());
		String journeyMapId = observer.getJourneyMapId();
		
		// Parse the JSON string
		Gson gson = new Gson();
		JsonObject jsonObject = gson.fromJson(e.getPayload(), JsonObject.class);

		System.out.println();
	}
}
