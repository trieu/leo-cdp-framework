package leotech.cdp.handler.api;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.journey.EventObserver;
import leotech.system.model.JsonDataPayload;

/**
 * Product Data API
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class ProductApiHandler extends BaseApiHandler {

	static final JsonDataPayload FAIL = JsonDataPayload
			.fail("You should set parameter " + HttpParamKey.PRIMARY_EMAIL + " or " + HttpParamKey.PRIMARY_PHONE);
	static final String API_PRODUCT_LIST = "/api/product/list";
	static final String API_PRODUCT_SAVE = "/api/product/save";

	/**
	 * to save tracking event
	 * 
	 * @param observer
	 * @param req
	 * @param uri
	 * @param paramJson
	 * @return
	 */
	@Override
	protected JsonDataPayload handlePost(EventObserver observer, HttpServerRequest req, String uri,
			JsonObject paramJson) {
		JsonDataPayload payload = null;
		try {
			switch (uri) {
			case API_PRODUCT_SAVE:
				// TODO
				payload = JsonDataPayload.fail("Not found any handler for URI " + uri);
				break;
			default:
				payload = JsonDataPayload.fail("Not found any handler for URI " + uri);
				break;
			}
		} catch (Exception e) {
			payload = JsonDataPayload.fail(e.getMessage());
			e.printStackTrace();
		}

		return payload;
	}

	/**
	 * to list events from HTTP GET
	 * 
	 * @param observer
	 * @param req
	 * @param uri
	 * @param urlParams
	 * @return
	 */
	@Override
	protected JsonDataPayload handleGet(EventObserver observer, HttpServerRequest req, String uri, MultiMap urlParams) {
		JsonDataPayload payload = null;
		if (uri.equals(API_PRODUCT_LIST)) {
			List<ProductItem> list = new ArrayList<ProductItem>();
			// TODO
			payload = JsonDataPayload.ok(uri, list);

		}
		return payload;
	}

	/**
	 * to create a tracking event for specific profile
	 * 
	 * @param req
	 * @param params
	 * @param eventName
	 * @param jsonData
	 * @param email
	 * @return
	 */
	protected static String saveProductItem(EventObserver observer, HttpServerRequest req, String eventName,
			JsonObject jsonData) {
		String observerId = observer.getId();

		System.err.println("CDP API can not save product");
		return "";

	}

}
