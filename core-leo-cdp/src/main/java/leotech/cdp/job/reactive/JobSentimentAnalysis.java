package leotech.cdp.job.reactive;

import java.io.IOException;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JobSentimentAnalysis {

	static String url = "http://leosentimentanalysis-env.eba-zfpijjzc.ap-south-1.elasticbeanstalk.com/analysis/";

	/** Gson-deserialized API response (record support requires Gson 2.10+, see docs/06 Wave 2). */
	public record SentimentAnalysisResult(double neg, double neu, double pos, double compound) {
	}

	/** Gson-serialized request payload; component names match the JSON field names. */
	public record SentimentAnalysisParams(String profileId, String eventId, String message) {

		public SentimentAnalysisParams(String message) {
			this("", "", message);
		}
	}

	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	public static SentimentAnalysisResult doTask(SentimentAnalysisParams params) {
		SentimentAnalysisResult rs = null;
		try {
			OkHttpClient client = new OkHttpClient();
			String jsonParams = new Gson().toJson(params);
			RequestBody body = RequestBody.create(JSON,jsonParams); // new
			// RequestBody body = RequestBody.create(JSON, json); // old
			Request request = new Request.Builder().url(url).post(body).build();
			Response response = client.newCall(request).execute();
			String jsonOutput = response.body().string();
			rs = new Gson().fromJson(jsonOutput, SentimentAnalysisResult.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rs;
	}
	
	public static void main(String[] args) {
		String message1 = "Great resource for anyone who wants to a refresher on digital marketing, or wants to learn about digital marketing techniques. This is my 'go to' book for all things digital marketing.";
		String rs1 = new Gson().toJson(doTask(new SentimentAnalysisParams(message1)));
		System.out.println(rs1);
		
		String message2 = "Lúc mua trong giỏ hàng có hiện lên quà tặng nhưng lúc giao hàng thì hoàn toàn ko có quà tặng kèm là ntn vậy? Khác gì lừa khách hàng đâu ạ";
		String rs2 = new Gson().toJson(doTask(new SentimentAnalysisParams(message2)));
		System.out.println(rs2);
	}
}
