package leotech.cdp.handler;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import leotech.system.common.PublicFileHttpRouter;

public class ZaloWebhookHandler {
	
	public static final String ZALO  = "/zalo";
	
	/**
	 * @param urlPath
	 * @param params
	 * @param resp
	 * @param outHeaders
	 * @return
	 */
	public static boolean verify(String urlPath, MultiMap params, HttpServerResponse resp, MultiMap outHeaders) {
		String localPath = "/public/zalo" + urlPath;
		return PublicFileHttpRouter.handle(resp, outHeaders, localPath, params);		
	}

}
