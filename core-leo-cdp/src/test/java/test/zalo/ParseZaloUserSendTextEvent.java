package test.zalo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import leotech.cdp.model.social.ZaloUserDetail;

public class ParseZaloUserSendTextEvent {

	 public static void main(String[] args) {
	        String jsonString = "{\"event_name\":\"user_send_text\",\"app_id\":\"123\",\"sender\":{\"id\":\"123\"},\"recipient\":{\"id\":\"13445\"},\"message\":{\"text\":\"Mình đã kiểm tra gối thì OK . Nhưng sản phẩm bên mình ko có hút chân ko ạ \",\"msg_id\":\"123aaa\"},\"timestamp\":\"1718195456222\"}";

	        // Parse the JSON string
	        Gson gson = new Gson();
	        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

	        // Access event name
	        String eventName = jsonObject.get("event_name").getAsString();

	        // Access app ID
	        String appId = jsonObject.get("app_id").getAsString();

	        // Access sender object and its properties
	        JsonObject senderObject = jsonObject.getAsJsonObject("sender");
	        if (senderObject != null) {
	            String senderId = senderObject.get("id").getAsString();
	            // Access other sender properties as needed
	            ZaloUserDetail u = GetFollowerInfo.getZaloUserInfo(senderId);
	            System.out.println(u);
	        }

	        // Access recipient object and its properties
	        JsonObject recipientObject = jsonObject.getAsJsonObject("recipient");
	        if (recipientObject != null) {
	            String recipientId = recipientObject.get("id").getAsString();
	            // Access other recipient properties as needed
	        }

	        // Access message object and its properties
	        JsonObject messageObject = jsonObject.getAsJsonObject("message");
	        if (messageObject != null) {
	            String text = messageObject.get("text").getAsString();
	            String msgId = messageObject.get("msg_id").getAsString();
	            // Access other message properties as needed
	            System.out.println("text: " + text);
	        }

	        // Access timestamp
	        String timestamp = jsonObject.get("timestamp").getAsString();

	        // Print extracted data
	        System.out.println("Event Name: " + eventName);
	        System.out.println("App ID: " + appId);
	        System.out.println("Timestamp: " + timestamp);

	        // ... (access other elements as needed)
	    }
}

