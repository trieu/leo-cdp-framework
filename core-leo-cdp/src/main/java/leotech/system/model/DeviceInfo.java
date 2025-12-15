package leotech.system.model;

import java.util.Objects;

import com.google.gson.Gson;

import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 * @since 2019
 *
 */
public final class DeviceInfo {
	public static final String DEVICE_NAME_SPIDER = "Spider";
	
	public final int platformType;
	public final String deviceName;
	public final String deviceType;
	public final String deviceOs;
	public final String deviceOsVersion;
	public final String browserName;
	public final String screenSize;

	public final int id;

	public DeviceInfo(String deviceType, int platformType, String deviceName, String deviceOs, String deviceOsVersion, String browserName, String screenSize) {
		super();
		this.deviceType = deviceType;
		this.platformType = platformType;
		this.deviceName = deviceName;
		this.deviceOs = deviceOs;
		this.deviceOsVersion = deviceOsVersion;
		this.browserName = browserName;
		this.screenSize = screenSize; // = window.screen.width + "x" + window.screen.height; 
		this.id = hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}

	@Override
	public int hashCode() {
		if(StringUtil.isEmpty(screenSize)) {
			return Objects.hash(platformType, deviceName, deviceOs, browserName);
		}
		return Objects.hash(platformType, deviceName, deviceOs, browserName, screenSize);
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	public boolean isWebCrawler() {
		return DEVICE_NAME_SPIDER.equalsIgnoreCase(deviceName);
	}
	
	public boolean isNotWebCrawler() {
		return !isWebCrawler();
	}
	
	public boolean isEmpty() {
		return StringUtil.isEmpty(deviceName) && StringUtil.isEmpty(deviceOs) && StringUtil.isEmpty(browserName);
	}
	
	public boolean isUnknownDevice() {
		return "Unknown".equalsIgnoreCase(deviceType);
	}
}
