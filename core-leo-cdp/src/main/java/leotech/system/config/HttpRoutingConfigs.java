package leotech.system.config;

import java.util.HashMap;

import com.google.gson.Gson;

import leotech.system.version.SystemMetaData;
import rfx.core.util.FileUtils;

/**
 * HTTP Routing configs for data worker
 * 
 * @author tantrieuf31
 *
 */
public final class HttpRoutingConfigs {

	public static final String FILE_HTTP_ROUTING_CONFIGS_JSON = "./configs/http-routing-configs.json";

	String name;
	String host;
	int port;
	String classNameHttpRouter;
	boolean bodyHandlerEnabled = false;
	boolean sockJsHandlerEnabled = false;
	String defaultDbConfig;

	public static final class HttpRoutingConfigsMap {
		private HashMap<String, HttpRoutingConfigs> map;

		public HttpRoutingConfigsMap() {
		}

		public HashMap<String, HttpRoutingConfigs> getMap() {
			if (map == null) {
				map = new HashMap<String, HttpRoutingConfigs>(0);
			}
			return map;
		}

		public void setMap(HashMap<String, HttpRoutingConfigs> map) {
			this.map = map;
		}
	}

	static HttpRoutingConfigsMap map = null;

	/**
	 * get http configs by key in the file configs/http-routing-configs.json
	 * 
	 * @param configKey
	 * @return
	 */
	public static HttpRoutingConfigs load(String configKey) {
		try {
			if (map == null) {
				String json = FileUtils.readFileAsString(FILE_HTTP_ROUTING_CONFIGS_JSON);
				map = new Gson().fromJson(json, HttpRoutingConfigsMap.class);
			}
			return map.getMap().get(configKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static HttpRoutingConfigs loadDefaultWorkerConfig() {
		return load("defaultWorker");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getClassNameHttpRouter() {
		return classNameHttpRouter;
	}

	public void setClassNameHttpRouter(String classNameHttpRouter) {
		this.classNameHttpRouter = classNameHttpRouter;
	}

	public boolean isBodyHandlerEnabled() {
		return bodyHandlerEnabled;
	}

	public void setBodyHandlerEnabled(boolean bodyHandlerEnabled) {
		this.bodyHandlerEnabled = bodyHandlerEnabled;
	}

	public boolean isSockJsHandlerEnabled() {
		return sockJsHandlerEnabled;
	}

	public void setSockJsHandlerEnabled(boolean sockJsHandlerEnabled) {
		this.sockJsHandlerEnabled = sockJsHandlerEnabled;
	}

	public String getDefaultDbConfig() {
		if (defaultDbConfig == null) {
			defaultDbConfig = SystemMetaData.MAIN_DATABASE_CONFIG;
		}
		return defaultDbConfig;
	}

	public void setDefaultDbConfig(String defaultDbConfig) {
		this.defaultDbConfig = defaultDbConfig;
	}

}
