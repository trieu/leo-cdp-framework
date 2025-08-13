package leotech.ssp.adserver.model;

import com.google.gson.Gson;

public class AdTrackingUrl {
	String url;
	int delay;
	String event;
	
	public AdTrackingUrl(String event,String url, int delay) {
		super();
		this.event = event;
		this.url = url;
		this.delay = delay;
	}
	
	public String getEvent() {
		return event;
	}
	public void setEvent(String event) {
		this.event = event;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public int getDelay() {
		return delay;
	}
	public void setDelay(int delay) {
		this.delay = delay;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
