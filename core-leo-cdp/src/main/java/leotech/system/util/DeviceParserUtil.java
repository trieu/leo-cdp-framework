package leotech.system.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import leotech.system.model.DeviceInfo;
import rfx.core.util.StringUtil;
import rfx.core.util.useragent.Client;
import rfx.core.util.useragent.Parser;

/**
 * @author Trieu Nguyen
 * @since 2022
 *
 */
public class DeviceParserUtil {

	public static final String SMART_TV_LG = "SmartTV-LG";
	public static final String SMART_TV_SAMSUNG = "SmartTV-Samsung";
	public static final String SMART_TV_SONY = "SmartTV-Sony";
	public static final String DEVICE_TYPE_PC = "General_Desktop";
	public static final String DEVICE_TYPE_MOBILE_WEB = "General_Mobile";
	public static final String DEVICE_TYPE_TABLET = "General_Tablet";
	public static final String DEVICE_ANDROID = "Android";

	public final static int PC = 1;
	public final static int MOBILE_WEB = 2;
	public final static int TABLET = 3;
	public final static int NATIVE_APP = 4;
	public final static int SMART_TV = 5;

	public static int getPlatformId(Client client, boolean isNativeApp) {
		if (isNativeApp) {
			return NATIVE_APP;
		}
		return getDeviceType(client);
	}

	public static int getDeviceType(Client client) {
		String p = client.device.deviceType();
		if (DEVICE_TYPE_PC.equals(p)) {
			return PC;
		} else if (DEVICE_TYPE_MOBILE_WEB.equals(p)) {
			return MOBILE_WEB;
		} else if (DEVICE_TYPE_TABLET.equals(p)) {
			return TABLET;
		}
		return PC;
	}

	public static final class DeviceUserContext {
		public String useragent;
		public String screensize;

		public DeviceUserContext(String useragent, String screensize) {
			super();
			this.useragent = useragent;
			this.screensize = screensize;
		}

		@Override
		public int hashCode() {
			if(StringUtil.isEmpty(screensize)) {
				return StringUtil.safeString(useragent).hashCode();
			}
			return StringUtil.safeString(useragent+screensize).hashCode();
		}
	}

	static LoadingCache<DeviceUserContext, DeviceInfo> deviceInfoCache = CacheBuilder.newBuilder().maximumSize(10000)
			.expireAfterWrite(10, TimeUnit.DAYS).build(new CacheLoader<DeviceUserContext, DeviceInfo>() {
				@Override
				public DeviceInfo load(DeviceUserContext deviceUserContext) {
					DeviceInfo dv = parse(deviceUserContext);
					// saveDeviceInfo(dv);
					return dv;
				}
			});

	public static DeviceInfo parse(DeviceUserContext deviceUserContext) {
		String useragent = deviceUserContext.useragent;
		String screensize = deviceUserContext.screensize;
		
		Client uaClient = Parser.load().parse(useragent);
		int platformType = PC;
		String deviceName = "PC";
		String deviceOs = uaClient.os.family;
		String deviceType = uaClient.device.deviceType();
		
		platformType = getDeviceType(uaClient);
		deviceName = uaClient.device.family.split(" ")[0];
		String browserName = uaClient.userAgent.family;
		String osVersion = StringUtil.safeString(uaClient.os.major) + "." + StringUtil.safeString(uaClient.os.minor)+ "." + StringUtil.safeString(uaClient.os.patch,"0");
		
		return new DeviceInfo(deviceType, platformType, deviceName, deviceOs, osVersion, browserName, screensize);
	}

	/**
	 * @param useragent
	 * @param screensize
	 * @return
	 * @throws ExecutionException
	 */
	public static DeviceInfo parseUserAgentAndScreenSize(String useragent, String screensize) throws ExecutionException {
		DeviceUserContext k = new DeviceUserContext(useragent, screensize);
		DeviceInfo v = deviceInfoCache.get(k);
		return v;
	}

}
