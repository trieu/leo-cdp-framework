package test.cdp.recommendation;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TestCallPersonalization {

	static OkHttpClient client = new OkHttpClient();
	public static final MediaType JSON = MediaType.get("application/json");
	static String authKey = "personalization_test";

	static String post(String url, String json) throws IOException {
		RequestBody body = RequestBody.create(json, JSON);
		Request request = new Request.Builder().url(url).post(body).build();
		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		}
	}
	
	static String get(String url) throws IOException {

		Request request = new Request.Builder().url(url).addHeader("Authorization", authKey ).get().build();
		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		}
	}

	public static void main(String[] args) throws IOException {
		String json = get("http://localhost:8000/recommend/crm_11?top_n=3");

		 // Parse JSON string into JsonObject
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

		System.out.println(jsonObject.get("recommended_products").getAsJsonArray());
	}
}
