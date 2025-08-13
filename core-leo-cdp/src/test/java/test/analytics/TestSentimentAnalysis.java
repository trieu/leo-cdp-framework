package test.analytics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TestSentimentAnalysis {

	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	static String httpPost(String url, String json) throws IOException {
		OkHttpClient client = new OkHttpClient();
		RequestBody body = RequestBody.create(JSON, json); // new
		// RequestBody body = RequestBody.create(JSON, json); // old
		Request request = new Request.Builder().url(url).post(body).build();
		Response response = client.newCall(request).execute();
		return response.body().string();
	}

	public static void main(String[] args) {

		Map<String, String> map1 = new HashMap<String, String>();
		map1.put("profileId", "");
		map1.put("eventId", "");
		String goodComment = "Great resource for anyone who wants to a refresher on digital marketing, or wants to learn about digital marketing techniques. This is my 'go to' book for all things digital marketing.";
		map1.put("message", goodComment);

		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("profileId", "");
		map2.put("eventId", "");
		String badComment = "The print quality of the hardback is blurry and difficult to read. I have had this problem with 2 copied I have ordered from Amazon. The content of the book is excellent. Shame about the dodgy printing. Hopefully the paperback will be an improvement.";
		map2.put("message", badComment);

		String url = "http://localhost:8000/analysis/";
		String json1 = new Gson().toJson(map1);
		String json2 = new Gson().toJson(map2);
		try {
			String jsonStr1 = httpPost(url, json1);
			JsonObject jsonObj1 = new JsonObject(jsonStr1);
			System.out.println(map1);
			System.out.println(jsonObj1);

			System.out.println("\n");

			String jsonStr2 = httpPost(url, json2);
			JsonObject jsonObj2 = new JsonObject(jsonStr2);
			System.out.println(map2);
			System.out.println(jsonObj2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
