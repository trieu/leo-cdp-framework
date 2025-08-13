package leotech.system.communication;

import java.io.IOException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import leotech.system.config.ActivationChannelConfigs;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MobileSmsSender {
	


	static ActivationChannelConfigs configs = ActivationChannelConfigs.loadMobileGatewaySmsServiceConfigs();
	static final String URL_SECURED_BY_BASIC_AUTHENTICATION = "http://api.infobip.com//sms/1/text/advanced";

	
	public static void send(String phoneNumber, String content) {
		if(configs == null) {
			System.err.println("MobileGatewaySmsServiceConfig is NULL");
			return;
		}
	    try {
	    	OkHttpClient client = new OkHttpClient();
	    	String AuthorizationHeader = configs.getValue("Authorization");
	    	JsonObject message = new JsonObject();
			message.put("destinations", new JsonArray().add(new JsonObject().put("to", phoneNumber)) );
			message.put("text", content);
			
			JsonArray messages = new JsonArray();
			messages.add(message);
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("messages", messages);
		    
			String json = jsonObject.encode();
			System.out.println("MobileSmsSender " + json);
			
			RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
			
		    Request request = new Request.Builder()
		      .url(URL_SECURED_BY_BASIC_AUTHENTICATION)
		      .addHeader("Authorization", AuthorizationHeader)
		      .post(body)
		      .build();
		    
	    	Call call = client.newCall(request);
			Response response = call.execute();
			System.out.println(response.body().string());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	

}
