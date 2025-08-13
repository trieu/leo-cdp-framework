package leotech.system.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import leotech.system.dao.SystemServiceDaoUtil;
import leotech.system.model.SystemService;

/**
 * Activation Channel Configuration
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public class ActivationChannelConfigs {

	public static final String LOCAL_ADMIN_MAIL_SERVER = "local_admin_mail_server";
	public static final String LOCAL_SMTP_MAIL_SERVER = "local_smtp_mail_server";
	public static final String PUSH_NOTIFICATION_SERVICE = "push_notification_service";
	public static final String MOBILE_GATEWAY_SMS_SERVICE = "mobile_gateway_sms_service";

	Map<String, Object> configs;

	protected ActivationChannelConfigs() {
	}

	public ActivationChannelConfigs(Map<String, Object> configs) {
		super();
		this.configs = configs;
	}

	public Map<String, Object> getConfigs() {
		return configs;
	}

	public String getValue(String name) {
		if (configs != null) {
			return configs.getOrDefault(name, "").toString();
		}
		return "";
	}

	@Override
	public String toString() {
		if (configs != null) {
			return new Gson().toJson(configs);
		}
		return "configs is NULL";
	}

	public static final class ActivationChannelConfigsMap {

		private Map<String, ActivationChannelConfigs> map = new HashMap<>();

		public ActivationChannelConfigsMap() {
		}

		public Map<String, ActivationChannelConfigs> getMap() {
			return map;
		}
		
		public void set(String k, ActivationChannelConfigs v) {
			this.map.put(k, v);
		}

	}

	static ActivationChannelConfigsMap channelConfigsMap = null;

	public static ActivationChannelConfigs load(String configKey) {
		try {
			if (channelConfigsMap == null) {
				channelConfigsMap = loadActivationChannelConfigsMap();
			}
			ActivationChannelConfigs configs = channelConfigsMap.getMap().get(configKey);
			return configs;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void reload() {
		try {
			channelConfigsMap = loadActivationChannelConfigsMap();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static ActivationChannelConfigsMap loadActivationChannelConfigsMap() {
		ActivationChannelConfigsMap theMap = new ActivationChannelConfigsMap();
		List<SystemService> list = SystemServiceDaoUtil.getAllSystemConfigs();
		
		for (SystemService systemConfig : list) {
			ActivationChannelConfigs channelConfigs = new ActivationChannelConfigs(systemConfig.getConfigs());
			theMap.set(systemConfig.getId(), channelConfigs);
		}
		return theMap;
	}

	public static ActivationChannelConfigs loadLocalSmtpMailServerConfigs() {
		return load(LOCAL_SMTP_MAIL_SERVER);
	}

	public static ActivationChannelConfigs loadLocalAdminMailServerConfigs() {
		return load(LOCAL_ADMIN_MAIL_SERVER);
	}

	public static ActivationChannelConfigs loadPushNoticationServiceConfigs() {
		return load(PUSH_NOTIFICATION_SERVICE);
	}

	public static ActivationChannelConfigs loadMobileGatewaySmsServiceConfigs() {
		return load(MOBILE_GATEWAY_SMS_SERVICE);
	}

}
