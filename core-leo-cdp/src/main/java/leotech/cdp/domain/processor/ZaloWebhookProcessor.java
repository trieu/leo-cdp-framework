package leotech.cdp.domain.processor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import leotech.cdp.data.service.ZaloApiService;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.analytics.WebhookDataEvent;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.social.ZaloUserDetail;
import rfx.core.util.StringUtil;

/**
 * Zalo OA Data Processor
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class ZaloWebhookProcessor {
	
	public static final String ZALO = "zalo";

	public static void processWebhookEvent(WebhookDataEvent e) {

		EventObserver observer = EventObserverManagement.getById(e.getObserverId());
		String journeyMapId = observer.getJourneyMapId();
		
		// Parse the JSON string
		Gson gson = new Gson();
		JsonObject jsonObject = gson.fromJson(e.getPayload(), JsonObject.class);

		String eventName = "";
		if(jsonObject.has("event_name")) {
			eventName = jsonObject.get("event_name").getAsString();
		}
		
		String appId = jsonObject.get("app_id").getAsString();
		long timestamp = jsonObject.get("timestamp").getAsLong();
		Date createdAt = new Date(timestamp);
		
		System.out.println("ZaloOaDataProcessor.eventName " + eventName);
		
		if (eventName.equalsIgnoreCase("follow") || eventName.equalsIgnoreCase("unfollow")) {
			processEventFollowAndUnfollow(observer, journeyMapId, jsonObject, eventName, appId, timestamp,createdAt);
		}
		
		else if (eventName.startsWith("user_send")) {
			processEventUseSendData(observer, journeyMapId, jsonObject, eventName, appId, createdAt);
		}

	}

	private static void processEventUseSendData(EventObserver observer, String journeyMapId, JsonObject jsonObject, String eventName, String appId, Date createdAt) {
		String observerId = observer.getId();
		// Access sender object and its properties
		JsonObject senderObject = jsonObject.getAsJsonObject("sender");
		JsonObject recipientObject = jsonObject.getAsJsonObject("recipient");
		JsonObject messageObject = jsonObject.getAsJsonObject("message");
		
		System.out.println("ZaloOaDataProcessor.processEventUseSendData " + messageObject);
		
		if (senderObject != null && messageObject != null && recipientObject != null) {
		    String senderId = senderObject.get("id").getAsString();
		    String recipientId = recipientObject.get("id").getAsString();
		    // Access other sender properties as needed
		    ZaloUserDetail u = ZaloApiService.getZaloUserInfo(senderId);
		    System.out.println(u);
		    if(u.isValidVisitor()) {
				
				Profile p = ProfileDataManagement.getOrCreateFromZaloOA(observer, u);
				System.out.println("getOrCreateFromZaloOA " + p.getId());
				
				JsonElement text = messageObject.get("text");
				
				String message = text != null ? text.getAsString(): StringUtil.toString(messageObject);
		        Map<String, Object> eventData = new HashMap<>();

				eventData.put("zaloAppId", appId);
				eventData.put("recipientId", recipientId);
				
				eventName = eventName.replaceAll("_", "-");
				EventObserverManagement.saveEventFromApi(p, observerId, "",createdAt,  journeyMapId, HttpParamKey.PRO_ENV,
						"127.0.0.1", observer.getType(), observer.getName(), observer.getDataSourceUrl(), "",
						"", eventName, message, eventData);
		    }
		    else {
		    	System.out.println("ZaloUserDetail is null for senderId " + senderId);
		    }
		}
	}

	private static void processEventFollowAndUnfollow(EventObserver observer, String journeyMapId,
			JsonObject jsonObject, String eventName, String appId, long timestamp, Date createdAt) {
		// Access data
		String observerId = observer.getId();
		String source = jsonObject.get("source").getAsString();
		
		
		String oaId = jsonObject.get("oa_id").getAsString();
		String zaloOaUrl = "https://zalo.me/" + oaId;

		// Access follower object (if needed)
		JsonObject followerObject = jsonObject.getAsJsonObject("follower");
		if (followerObject != null) {
			String followerId = followerObject.get("id").getAsString();
			ZaloUserDetail u = ZaloApiService.getZaloUserInfo(followerId);
			if(u.isValidVisitor()) {
				System.out.println(u);
				
				Profile p = ProfileDataManagement.getOrCreateFromZaloOA(observer, u);
				System.out.println(p);
				
				Map<String, Object> eventData = new HashMap<>();
				eventData.put("zaloOaId", oaId);
				eventData.put("source", source);
				eventData.put("zaloAppId", appId);
				eventData.put("zaloOaUrl", zaloOaUrl);
				EventObserverManagement.saveEventFromApi(p, observerId, "",createdAt, journeyMapId, HttpParamKey.PRO_ENV,
						"127.0.0.1", observer.getType(), observer.getName(), observer.getDataSourceUrl(), "",
						"", eventName, eventData);
			}
			
			// Access other follower properties as needed
		}

		// Print extracted data
		System.out.println("zaloOaUrl: " + zaloOaUrl);
		System.out.println("Event Name: " + eventName);
		System.out.println("Source: " + source);
		System.out.println("App ID: " + appId);
		System.out.println("Timestamp: " + timestamp);
	}
}
