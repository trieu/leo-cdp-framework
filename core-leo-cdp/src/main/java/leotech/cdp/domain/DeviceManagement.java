package leotech.cdp.domain;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import leotech.cdp.dao.DeviceDaoUtil;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.customer.Device;
import leotech.system.model.DeviceInfo;
import leotech.system.util.DeviceInfoUtil;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 *
 */
public final class DeviceManagement {
	
	private static final String DEFAULT_AGENT = "CDPClient/1.0 (Linux; JVM 11; CDPAPI/1.0)";
	// ------- cache ----------
	
	private static final int CACHE_TIME_IN_MINUTES = 30;
	static CacheLoader<DeviceInfo, String> cacheLoader = new CacheLoader<DeviceInfo, String>() {
        @Override
        public String load(DeviceInfo device) {
            return DeviceInfoUtil.getUserDevice(device).getId();
        }
    };
    static LoadingCache<DeviceInfo, String> cache = CacheBuilder.newBuilder().expireAfterWrite(CACHE_TIME_IN_MINUTES,TimeUnit.MINUTES).build(cacheLoader);
    
    private static final int CACHE_DEVICE_IN_HOURS = 12;
	static CacheLoader<String, Device> cacheDeviceLoader = new CacheLoader<>() {
        @Override
        public Device load(String id) {
            return DeviceDaoUtil.getById(id);
        }
    };
    static LoadingCache<String, Device> cacheDevice = CacheBuilder.newBuilder().expireAfterWrite(CACHE_DEVICE_IN_HOURS,TimeUnit.HOURS).build(cacheDeviceLoader);
    
    // ------- cache ----------

    
    // device of CDP API
    public static void initDefaultSystemData() {
 		DeviceDaoUtil.save(Device.CDP_API_DEVICE);
    }
    
	/**
	 * @param params
	 * @param device
	 * @return deviceId
	 */
	public final static String getDeviceId(MultiMap params, DeviceInfo device) {
		String deviceId = StringUtil.safeString(params.get(HttpParamKey.USER_DEVICE_ID));
		if(deviceId.isEmpty()) {
			deviceId = DeviceInfoUtil.getUserDevice(device).getId();
		}
		return deviceId;
	}
	
	/**
	 * @param request
	 * @return UserAgent as string
	 */
	public final static String getUserAgent(HttpServerRequest request) {
		String s = request.getHeader(HttpHeaderNames.USER_AGENT);
		if(StringUtil.isEmpty(s)) {
			s = DEFAULT_AGENT;
		}
		return s;
	}
	
	/**
	 * @param id
	 * @return Device
	 */
	public static Device getById(String id) {
		Device d = null;
		try {
			d = cacheDevice.get(id);
		} catch (Exception e) {
			d = DeviceDaoUtil.getById(id);
		}
		return d;
	}
}
