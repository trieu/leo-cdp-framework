package leotech.cdp.handler.api;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import leotech.cdp.data.service.ZaloApiService;
import leotech.cdp.model.journey.EventObserver;
import leotech.system.model.JsonDataPayload;
import rfx.core.util.HttpRequestUtil;

/**
 * Profile Data API
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class SystemApiHandler extends BaseApiHandler {
	
	static final String API_SYSTEM_CALLBACK = "/api/system/callback";
	
	@Override
	protected JsonDataPayload handlePost(EventObserver observer, HttpServerRequest req, String uri, JsonObject jsonObject) {
		return NO_SUPPORT_HTTP_REQUEST;
	}

	@Override
	protected JsonDataPayload handleGet(EventObserver observer, HttpServerRequest req, String uri, MultiMap urlParams) {
		JsonDataPayload payload = null;
		try {
			switch (uri) {
			case API_SYSTEM_CALLBACK:
				if(observer.isDefaultEventObserver()) {
					String service = HttpRequestUtil.getParamValue("service", urlParams);
					if(service.equalsIgnoreCase("ZaloApiService")) {
						String token = HttpRequestUtil.getParamValue(ZaloApiService.ZALO_ACCESS_TOKEN, urlParams);
						ZaloApiService.AccessTokenUtil.setValue(token);
					}
				}
				return JsonDataPayload.ok(uri, "ok");
			default:
				payload = notFoundHttpHandler(uri);
				break;
			}
		} catch (Exception e) {
			payload = JsonDataPayload.fail(e.getMessage());
			e.printStackTrace();
		}

		return payload;
	}

}
