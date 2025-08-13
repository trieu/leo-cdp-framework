package leotech.cdp.handler.api;

import com.google.gson.Gson;

import io.vertx.core.json.JsonObject;
import leotech.cdp.model.journey.EventObserver;

/**
 * data model for CDP API
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class DataApiPayload {

	EventObserver observer;
	JsonObject jsonObject;

	public DataApiPayload(EventObserver observer, JsonObject jsonObject) {
		super();
		this.observer = observer;
		this.jsonObject = jsonObject;
	}

	public EventObserver getObserver() {
		return observer;
	}

	public JsonObject getJsonObject() {
		return jsonObject;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
