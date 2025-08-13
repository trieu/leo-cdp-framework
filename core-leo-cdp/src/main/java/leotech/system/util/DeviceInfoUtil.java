package leotech.system.util;



import java.util.Date;

import leotech.cdp.model.customer.Device;
import leotech.system.model.DeviceInfo;

/**
 * utility class to convert DeviceInfo to Device
 * 
 * @author tantrieuf31
 *
 */
public final class DeviceInfoUtil {
	
	
	public final static DeviceInfo getDeviceInfo(String useragent) {
		DeviceInfo device = null;
		try {
			if (useragent != null) {
				device = DeviceParserUtil.parseWithCache(useragent);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (device == null) {
			device = new DeviceInfo("Unknown", 0, "Unknown_Device", "Unknown_OS", "", "Unknown");
		}
		return device;
	}
	
	public final static Device getUserDevice(String useragent) {
		return new Device(getDeviceInfo(useragent));
	}
	
	public final static Device getUserDevice(String useragent, Date createdAt) {
		return new Device(getDeviceInfo(useragent), createdAt);
	}
	
	public final static Device getUserDevice(DeviceInfo dv, Date createdAt) {
		return new Device(dv, createdAt);
	}
	
	public final static Device getUserDevice(DeviceInfo dv) {
		return new Device(dv);
	}
}
