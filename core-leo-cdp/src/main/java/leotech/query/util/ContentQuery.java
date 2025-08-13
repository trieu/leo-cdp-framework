package leotech.query.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import rfx.core.util.StringUtil;

public class ContentQuery implements Serializable {

	private static final long serialVersionUID = -593243868837720868L;

	private String deviceOS = "";
	private String deviceName = "";

	private String uuid;
	private List<String> segmentIds;
	private List<String> topicIds;
	private String locationId;
	private String contentId = "";
	private String url = "";
	private String callback = "";

	public ContentQuery() {
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public String getUrl() {
		if (url == null) {
			url = "";
		}
		return url;
	}

	public void setUrl(String purl) {
		this.url = purl;
	}

	public String getDeviceOS() {
		return deviceOS;
	}

	public void setDeviceOS(String deviceOS) {
		this.deviceOS = deviceOS;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public List<String> getSegmentIds() {
		if (segmentIds == null) {
			segmentIds = new ArrayList<String>(0);
		}
		return segmentIds;
	}

	public void setSegmentIds(List<String> segmentIds) {
		this.segmentIds = segmentIds;
	}

	public List<String> getTopicIds() {
		if (topicIds == null) {
			topicIds = new ArrayList<String>(0);
		}
		return topicIds;
	}

	public void setTopicIds(List<String> topicIds) {
		this.topicIds = topicIds;
	}

	public String getLocationId() {
		return locationId;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	public String hashKey() {
		return StringUtil.join(":", locationId, deviceOS, deviceName, url, contentId, getSegmentIds(),
				getTopicIds(), callback);
	}

	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}

	// for cache key
	@Override
	public int hashCode() {
		int key = 0;
		if (StringUtil.isEmpty(contentId)) {
			key = hashKey().hashCode();
		} else {
			key = String.valueOf("ct:" + contentId).hashCode();
		}
		// System.out.println("AdQuery "+ key);
		return key;
	}

	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == (obj.hashCode());
	}
}
