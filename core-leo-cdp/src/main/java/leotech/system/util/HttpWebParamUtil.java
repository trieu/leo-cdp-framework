package leotech.system.util;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.customer.LearningCourse;
import rfx.core.util.StringUtil;

/**
 * the util class to parse and get input parameters
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class HttpWebParamUtil {

	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private static final String _127_0_0_1 = "127.0.0.1";
	static final String unknown = "unknown";

	/**
	 * @param request
	 * @return
	 */
	public final static String getFullRemoteIPs(HttpServerRequest request) {
		String ipAddress = request.headers().get(X_FORWARDED_FOR);
		if (!StringUtil.isNullOrEmpty(ipAddress) && !unknown.equalsIgnoreCase(ipAddress)) {
			return ipAddress;
		} else {
			ipAddress = getIpAsString(request.remoteAddress());
		}
		return ipAddress;
	}

	/**
	 * @param request
	 * @return
	 */
	public final static String getRemoteIP(HttpServerRequest request) {
		String ipAddress = request.headers().get(X_FORWARDED_FOR);
		if (!StringUtil.isNullOrEmpty(ipAddress) && !unknown.equalsIgnoreCase(ipAddress)) {
			String[] toks = ipAddress.split(",");
			int len = toks.length;
			if (len > 0) {
				ipAddress = toks[0];
			} else {
				return ipAddress;
			}
		} else {
			ipAddress = getIpAsString(request.remoteAddress());
		}
		return ipAddress;
	}

	/**
	 * @param address
	 * @return
	 */
	public final static String getIpAsString(SocketAddress address) {
		try {
			if (address instanceof InetSocketAddress) {
				return ((InetSocketAddress) address).getAddress().getHostAddress();
			}
			String[] toks = address.toString().split("/");
			if (toks.length > 1) {
				String[] toks2 = toks[1].split(":");
				if (toks2.length > 0) {
					return toks2[0];
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return _127_0_0_1;
	}

	/**
	 * @param request
	 * @return
	 */
	public final static String getRequestInfo(HttpServerRequest request) {
		StringBuilder reqInfo = new StringBuilder();

		String remoteAddress = request.remoteAddress().toString();

		MultiMap headers = request.headers();

		reqInfo.append(" <br> IP remoteAddress: ").append(remoteAddress);
		for (Entry<String, String> header : headers) {
			reqInfo.append("<br> ").append(header.getKey()).append(" = ").append(header.getValue());
		}

		return reqInfo.toString();
	}

	/**
	 * @param params
	 * @param paramName
	 * @param defaultValue
	 * @return
	 */
	public final static int getInteger(MultiMap params, String paramName, int defaultValue) {
		return StringUtil.safeParseInt(params.get(paramName), defaultValue);
	}

	/**
	 * @param params
	 * @param paramName
	 * @param defaultValue
	 * @return
	 */
	public final static int getInteger(JsonObject params, String paramName, int defaultValue) {
		if (params.containsKey(paramName)) {
			try {
				return params.getInteger(paramName, defaultValue);
			} catch (Exception e) {
				return StringUtil.safeParseInt(params.getString(paramName), defaultValue);
			}
		}
		return defaultValue;
	}

	/**
	 * @param params
	 * @param paramName
	 * @param defaultValue
	 * @return
	 */
	public final static boolean getBoolean(MultiMap params, String paramName, boolean defaultValue) {
		String value = params.get(paramName);
		if (value != null) {
			return Boolean.parseBoolean(value);
		}
		return defaultValue;
	}

	/**
	 * @param params
	 * @param paramName
	 * @param defaultValue
	 * @return
	 */
	public final static boolean getBoolean(JsonObject params, String paramName, boolean defaultValue) {
		String value = params.getString(paramName);
		if (value != null) {
			return Boolean.parseBoolean(value);
		}
		return defaultValue;
	}

	/**
	 * @param params
	 * @param paramName
	 * @param defaultValue
	 * @return
	 */
	public final static double getDouble(MultiMap params, String paramName, double defaultValue) {
		String num = params.get(paramName);
		if (num == null) {
			return defaultValue;
		}
		return StringUtil.safeParseDouble(num);
	}

	/**
	 * @param params
	 * @param paramName
	 * @return
	 */
	public static final String getString(MultiMap params, String paramName) {
		return getString(params, paramName, "", true);
	}

	/**
	 * @param params
	 * @param paramName
	 * @param defaultValue
	 * @return
	 */
	public static final String getString(MultiMap params, String paramName, String defaultValue) {
		return getString(params, paramName, defaultValue, true);
	}

	/**
	 * @param params
	 * @param paramName
	 * @param defaultValue
	 * @param xssClean
	 * @return
	 */
	public final static String getString(MultiMap params, String paramName, String defaultValue, boolean xssClean) {
		try {
			String val = StringUtil.safeString(params.get(paramName), defaultValue);
			if (StringUtil.isNotEmpty(val)) {
				String result = java.net.URLDecoder.decode(val, StandardCharsets.UTF_8.name());
				// FIXME remove later
				result = java.net.URLDecoder.decode(result, StandardCharsets.UTF_8.name());
				if (xssClean) {
					result = XssFilterUtil.clean(result);
				}
				return result;
			}
		} catch (Exception e) {
			// not going to happen - value came from JDK's own StandardCharsets
		}
		return "";
	}

	/**
	 * @param paramJson
	 * @param paramName
	 * @param defaultValue
	 * @return
	 */
	public final static String getString(JsonObject paramJson, String paramName, String defaultValue) {
		return paramJson.getString(paramName, defaultValue);
	}

	public final static List<String> getListFromRequestParams(MultiMap params, String paramName) {
		List<String> list = null;
		try {
			String jsonStr = StringUtil.decodeUrlUTF8(params.get(paramName));
			if (StringUtil.isNotEmpty(jsonStr)) {
				Gson gson = new Gson();
				Type strMapType = new TypeToken<ArrayList<String>>() {
				}.getType();
				list = gson.fromJson(jsonStr, strMapType);
			}
		} catch (JsonSyntaxException e) {
			System.err.println(e.getMessage());
		}
		if (list == null) {
			list = new ArrayList<>(0);
		}
		return list;
	}

	/**
	 * @param paramJson
	 * @param paramName
	 * @return
	 */
	public final static List<String> getListFromRequestParams(JsonObject paramJson, String paramName) {
		List<String> list = null;
		try {
			JsonArray jsonArray = paramJson.getJsonArray(paramName);
			if (jsonArray != null) {
				list = jsonArray.getList();
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		if (list == null) {
			list = new ArrayList<>(0);
		}
		return list;
	}

	/**
	 * @param params
	 * @param paramName
	 * @return
	 */
	public final static Map<String, Object> getHashMapFromRequestParams(MultiMap params, String paramName) {
		Map<String, Object> map = null;
		try {
			String jsonStr = StringUtil.decodeUrlUTF8(params.get(paramName));
			if (StringUtil.isNotEmpty(jsonStr)) {
				Gson gson = new Gson();
				Type strMapType = new TypeToken<Map<String, Object>>() {
				}.getType();
				map = gson.fromJson(jsonStr, strMapType);
			}
		} catch (JsonSyntaxException e) {
			System.err.println(e.getMessage());
		}

		if (map == null) {
			map = new HashMap<String, Object>(0);
		}
		return map;
	}

	/**
	 * get Event Data
	 * 
	 * @param params
	 * @return Map<String, Object>
	 */
	public final static Map<String, Object> getEventData(MultiMap params) {
		Map<String, Object> map = null;
		try {
			String jsonStr = StringUtil.decodeUrlUTF8(params.get(HttpParamKey.EVENT_DATA));
			if (StringUtil.isNotEmpty(jsonStr)) {
				Gson gson = new Gson();
				Type strMapType = new TypeToken<Map<String, Object>>() {
				}.getType();
				map = gson.fromJson(jsonStr, strMapType);
			}
		} catch (JsonSyntaxException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		if (map == null) {
			map = new HashMap<String, Object>(0);
		}
		return map;
	}

	public final static Map<String, Object> getMapFromRequestParams(JsonObject params, String paramName) {
		Object value = params.getValue(paramName);
		if (StringUtil.isEmpty(value)) {
			return new HashMap<String, Object>(0);
		} else {
			JsonObject map = params.getJsonObject(paramName);
			return map != null ? map.getMap() : new HashMap<String, Object>(0);
		}
	}

	/**
	 * @param map
	 * @param key
	 * @return
	 */
	public final static Set<LearningCourse> getLearningCourses(Map<String, Object> map) {
		Object obj = map.get("learningCourses");
		if(obj != null) {
			try {
				ArrayList<LinkedTreeMap<String, Object>> list = (ArrayList<LinkedTreeMap<String, Object>>)obj;
				
				Set<LearningCourse> set = new HashSet<LearningCourse>(list.size());
				list.forEach(e->{
					set.add(new LearningCourse(e));
				});
				return set;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new HashSet<LearningCourse>(0);
	}

	/**
	 * @param map
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public final static String getString(Map<String, Object> map, String key, String defaultValue) {
		return XssFilterUtil.clean(map.getOrDefault(key, defaultValue));
	}

	/**
	 * @param map
	 * @param key
	 * @return
	 */
	public final static String getString(Map<String, Object> map, String key) {
		return getString(map, key, "");
	}

	/**
	 * get phone number
	 * 
	 * @param map
	 * @param key
	 * @return formated string for phone number
	 */
	public final static String getPhoneNumber(Map<String, Object> map, String key) {
		return getString(map, key, "").replaceAll("[^\\d]", "");
	}

	/**
	 * @param HttpServerRequest req
	 * @param String            eventName
	 * @return
	 */
	public final static FeedbackEvent getFeedbackEventFromHttpPost(HttpServerRequest req, String eventName) {
		MultiMap formData = req.formAttributes();
		String jsonStr = StringUtil.decodeUrlUTF8(formData.get(HttpParamKey.EVENT_DATA));
		if (StringUtil.isNotEmpty(jsonStr)) {
			Gson gson = new Gson();
			FeedbackEvent fbe = gson.fromJson(jsonStr, FeedbackEvent.class);
			fbe.setEventName(eventName);
			return fbe;
		}
		return null;
	}

	/**
	 * @param jsonStr
	 * @return
	 */
	public static Map<String, Object> parseJsonToGetHashMap(String jsonStr) {
		Map<String, Object> map = null;
		try {
			if (StringUtil.isNotEmpty(jsonStr)) {
				Gson gson = new Gson();
				Type strMapType = new TypeToken<Map<String, Object>>() {
				}.getType();
				map = gson.fromJson(jsonStr, strMapType);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		return map != null ? map : new HashMap<>(0);
	}

	/**
	 * @param formData
	 * @param name
	 * @return
	 */
	public final static Set<String> getHashSet(JsonObject formData, String name) {
		Set<String> set = null;
		try {
			String jsonStr = StringUtil.decodeUrlUTF8(formData.getString(name, ""));
			if (StringUtil.isNotEmpty(jsonStr)) {
				Gson gson = new Gson();
				Type type = new TypeToken<HashSet<String>>() {
				}.getType();
				set = gson.fromJson(jsonStr, type);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return set != null ? set : new HashSet<>(0);
	}

	/**
	 * @param params
	 * @param paramName
	 * @return
	 */
	public final static Map<String, Set<String>> getMapSetFromRequestParams(MultiMap params, String paramName) {
		Map<String, Set<String>> map = null;
		String jsonStr = StringUtil.decodeUrlUTF8(params.get(paramName));
		if (StringUtil.isNotEmpty(jsonStr)) {
			Gson gson = new Gson();
			Type strMapType = new TypeToken<Map<String, Set<String>>>() {
			}.getType();
			map = gson.fromJson(jsonStr, strMapType);
		}

		if (map == null) {
			map = new HashMap<String, Set<String>>(0);
		}
		return map;
	}
}
