package leotech.system.communication;

import io.github.jklingsporn.vertx.push.PushClient;
import io.github.jklingsporn.vertx.push.PushClientOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import leotech.system.config.ActivationChannelConfigs;

public class PushNotificationSender {

	// OneSignal configs
	static ActivationChannelConfigs configs = ActivationChannelConfigs.loadPushNoticationServiceConfigs();
	static final String APP_ID = configs.getValue("appId");
	static final String REST_API_KEY = configs.getValue("restApiKey");
	
	static volatile PushClient client = null;

	static PushClient getSenderClient() {
		if(client == null) {
			PushClientOptions ops = new PushClientOptions();
			ops.setAppId(APP_ID);
			ops.setRestApiKey(REST_API_KEY);
			ops.setIgnoreAllPlayersAreNotSubscribed(true);
			client = PushClient.create(Vertx.vertx(), ops);
		}
		return client;
	}

	public static void notifyUser(String userId, String heading, String content, String url) {

		JsonArray targetByPlayerIds = new JsonArray().add(userId.trim());
		JsonObject contents = new JsonObject().put("en", content).put("vn", content);
		JsonObject headings = new JsonObject().put("en", heading).put("vn", heading);

		// setup the content of the message on the serverside
		getSenderClient()
				//.raw().addOptions(new SendOptions(data))
				.withContent(contents, url).withHeadings(headings)
				.targetByPlayerIds(targetByPlayerIds)
				.sendNow(h -> {
					System.out.println("sent ...");
					if (h.succeeded()) {
						JsonObject rs = h.result();
						boolean ok = rs.getInteger("recipients", 0) > 0;
						System.out.println("PushNotificationSender.notifyUser" + userId + " rs = " + ok);
					} else {
						h.cause().printStackTrace();
					}
				});

	}
}
