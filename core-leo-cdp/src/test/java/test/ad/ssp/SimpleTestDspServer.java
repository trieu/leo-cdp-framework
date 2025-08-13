package test.ad.ssp;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;

public class SimpleTestDspServer {

	static String getBidResponse() {
		String data = "";
		try {
			data =  Files.toString(new File("./data/bid-response-video.json"), Charsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		HttpServer server = vertx.createHttpServer();
		server.requestHandler(new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String uri = request.uri();
				System.out.println(uri);
				if(uri.equals("/winnoticeurl")) {
					request.response().end("");
				} else {
					request.response().end(getBidResponse());
				}
			}
		});
		server.listen(9890);
	}

}
