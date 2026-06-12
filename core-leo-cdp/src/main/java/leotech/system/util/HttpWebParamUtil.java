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
 * Utility class to parse and extract input parameters safely from HTTP requests.
 * * @author tantrieuf31
 * @since 2020
 */
public final class HttpWebParamUtil {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String LOCAL_HOST_IP = "127.0.0.1";
    private static final String UNKNOWN = "unknown";
    
    // Gson is thread-safe. Reusing a single instance improves performance significantly.
    private static final Gson GSON = new Gson();

    /**
     * Gets the full remote IP addresses, accounting for proxy forwards.
     * * @param request The active HttpServerRequest
     * @return The IP address string
     */
    public final static String getFullRemoteIPs(HttpServerRequest request) {
        String ipAddress = request.headers().get(X_FORWARDED_FOR);
        if (!StringUtil.isNullOrEmpty(ipAddress) && !UNKNOWN.equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }
        return getIpAsString(request.remoteAddress());
    }

    /**
     * Gets the single origin remote IP, extracting the first IP if multiple are forwarded.
     * * @param request The active HttpServerRequest
     * @return The originating IP address
     */
    public final static String getRemoteIP(HttpServerRequest request) {
        String ipAddress = request.headers().get(X_FORWARDED_FOR);
        if (!StringUtil.isNullOrEmpty(ipAddress) && !UNKNOWN.equalsIgnoreCase(ipAddress)) {
            String[] toks = ipAddress.split(",");
            if (toks.length > 0) {
                return toks[0].trim();
            }
        }
        return getIpAsString(request.remoteAddress());
    }

    /**
     * Safely converts a SocketAddress to a string representation of the IP.
     * * @param address The socket address
     * @return The IP address as a String, defaulting to 127.0.0.1 on failure
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
        } catch (Exception e) {
            System.err.println("Failed to parse IP address: " + e.getMessage());
        }
        return LOCAL_HOST_IP;
    }

    /**
     * Extracts and formats all request headers and remote address for debugging.
     * * @param request The active HttpServerRequest
     * @return HTML formatted string of request info
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
     * Extracts an integer from a MultiMap safely.
     * * @param params Vert.x MultiMap of parameters
     * @param paramName The key to look up
     * @param defaultValue Fallback value if parsing fails
     * @return Parsed integer or default value
     */
    public final static int getInteger(MultiMap params, String paramName, int defaultValue) {
        return StringUtil.safeParseInt(params.get(paramName), defaultValue);
    }

    /**
     * Extracts an integer from a JsonObject safely, handling potential type mismatches.
     * * @param params JsonObject containing the data
     * @param paramName The key to look up
     * @param defaultValue Fallback value if parsing fails
     * @return Parsed integer or default value
     */
    public final static int getInteger(JsonObject params, String paramName, int defaultValue) {
        if (params != null && params.containsKey(paramName)) {
            try {
                // Try direct cast first
                Object val = params.getValue(paramName);
                if (val instanceof Integer) {
                    return (Integer) val;
                }
                // Fallback to string parsing if the JSON contained the number as a string
                return StringUtil.safeParseInt(params.getString(paramName), defaultValue);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Extracts a boolean from a MultiMap safely.
     * * @param params Vert.x MultiMap of parameters
     * @param paramName The key to look up
     * @param defaultValue Fallback value if missing
     * @return Parsed boolean
     */
    public final static boolean getBoolean(MultiMap params, String paramName, boolean defaultValue) {
        String value = params.get(paramName);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Extracts a boolean from a JsonObject safely.
     * * @param params JsonObject containing the data
     * @param paramName The key to look up
     * @param defaultValue Fallback value if missing
     * @return Parsed boolean
     */
    public final static boolean getBoolean(JsonObject params, String paramName, boolean defaultValue) {
        if (params != null && params.containsKey(paramName)) {
            try {
                return params.getBoolean(paramName, defaultValue);
            } catch (ClassCastException e) {
                String value = params.getString(paramName);
                return value != null ? Boolean.parseBoolean(value) : defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Extracts a double from a MultiMap safely.
     * * @param params Vert.x MultiMap of parameters
     * @param paramName The key to look up
     * @param defaultValue Fallback value if missing
     * @return Parsed double
     */
    public final static double getDouble(MultiMap params, String paramName, double defaultValue) {
        String num = params.get(paramName);
        return num == null ? defaultValue : StringUtil.safeParseDouble(num);
    }

    /**
     * Extracts a string from a MultiMap with XSS cleaning applied by default.
     * * @param params Vert.x MultiMap of parameters
     * @param paramName The key to look up
     * @return Decoded and cleaned string, or empty string
     */
    public static final String getString(MultiMap params, String paramName) {
        return getString(params, paramName, "", true);
    }

    /**
     * Extracts a string from a MultiMap with a default value and XSS cleaning.
     * * @param params Vert.x MultiMap of parameters
     * @param paramName The key to look up
     * @param defaultValue Fallback value
     * @return Decoded and cleaned string
     */
    public static final String getString(MultiMap params, String paramName, String defaultValue) {
        return getString(params, paramName, defaultValue, true);
    }

    /**
     * Core string extraction method handling URL decoding and optional XSS filtering.
     * * @param params Vert.x MultiMap of parameters
     * @param paramName The key to look up
     * @param defaultValue Fallback value
     * @param xssClean Whether to apply XSS filtering
     * @return Processed string
     */
    public final static String getString(MultiMap params, String paramName, String defaultValue, boolean xssClean) {
        try {
            String val = StringUtil.safeString(params.get(paramName), defaultValue);
            if (StringUtil.isNotEmpty(val)) {
                // Decoded once. Removed the duplicate decode as it causes issues with genuinely encoded data
                String result = java.net.URLDecoder.decode(val, StandardCharsets.UTF_8.name());
                
                if (xssClean) {
                    result = XssFilterUtil.clean(result);
                }
                return result;
            }
        } catch (Exception e) {
            System.err.println("URL Decode error: " + e.getMessage());
        }
        return defaultValue != null ? defaultValue : "";
    }

    /**
     * Extracts a string from a JsonObject safely.
     * * @param paramJson JsonObject containing the data
     * @param paramName The key to look up
     * @param defaultValue Fallback value
     * @return Extracted string or default
     */
    public final static String getString(JsonObject paramJson, String paramName, String defaultValue) {
        return paramJson != null ? paramJson.getString(paramName, defaultValue) : defaultValue;
    }

    /**
     * Parses a URL-encoded JSON array string into a List of Strings.
     * * @param params Vert.x MultiMap
     * @param paramName The key containing the JSON string
     * @return List of strings, never null
     */
    public final static List<String> getListFromRequestParams(MultiMap params, String paramName) {
        List<String> list = null;
        try {
            String jsonStr = StringUtil.decodeUrlUTF8(params.get(paramName));
            if (StringUtil.isNotEmpty(jsonStr)) {
                Type strMapType = new TypeToken<ArrayList<String>>() {}.getType();
                list = GSON.fromJson(jsonStr, strMapType);
            }
        } catch (JsonSyntaxException e) {
            System.err.println("JSON Syntax Error in getListFromRequestParams: " + e.getMessage());
        }
        return list != null ? list : new ArrayList<>(0);
    }

    /**
     * Extracts a List of Strings directly from a JsonArray inside a JsonObject.
     * * @param paramJson The source JsonObject
     * @param paramName The key containing the JsonArray
     * @return List of strings, never null
     */
    @SuppressWarnings("unchecked")
    public final static List<String> getListFromRequestParams(JsonObject paramJson, String paramName) {
        List<String> list = null;
        try {
            if (paramJson != null && paramJson.containsKey(paramName)) {
                JsonArray jsonArray = paramJson.getJsonArray(paramName);
                if (jsonArray != null) {
                    list = jsonArray.getList();
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading JSON array: " + e.getMessage());
        }
        return list != null ? list : new ArrayList<>(0);
    }

    /**
     * Parses a URL-encoded JSON object string into a HashMap.
     * * @param params Vert.x MultiMap
     * @param paramName The key containing the JSON string
     * @return Map representing the JSON object, never null
     */
    public final static Map<String, Object> getHashMapFromRequestParams(MultiMap params, String paramName) {
        Map<String, Object> map = null;
        try {
            String jsonStr = StringUtil.decodeUrlUTF8(params.get(paramName));
            if (StringUtil.isNotEmpty(jsonStr)) {
                Type strMapType = new TypeToken<Map<String, Object>>() {}.getType();
                map = GSON.fromJson(jsonStr, strMapType);
            }
        } catch (JsonSyntaxException e) {
            System.err.println("JSON Syntax Error in getHashMapFromRequestParams: " + e.getMessage());
        }
        return map != null ? map : new HashMap<>(0);
    }

    /**
     * Dedicated method to parse CDP Event Data specifically.
     * * @param params Vert.x MultiMap containing the event payload
     * @return Parsed Event Data map, never null
     */
    public final static Map<String, Object> getEventData(MultiMap params) {
        Map<String, Object> map = null;
        try {
            String jsonStr = StringUtil.decodeUrlUTF8(params.get(HttpParamKey.EVENT_DATA));
            if (StringUtil.isNotEmpty(jsonStr)) {
                Type strMapType = new TypeToken<Map<String, Object>>() {}.getType();
                map = GSON.fromJson(jsonStr, strMapType);
            }
        } catch (JsonSyntaxException e) {
            System.err.println("JSON Syntax Error in getEventData: " + e.getMessage());
        }
        return map != null ? map : new HashMap<>(0);
    }

    /**
     * Extracts a nested JsonObject as a Map.
     * * @param params Source JsonObject
     * @param paramName Key containing the nested object
     * @return Extracted Map, never null
     */
    public final static Map<String, Object> getMapFromRequestParams(JsonObject params, String paramName) {
        if (params == null || !params.containsKey(paramName)) {
            return new HashMap<>(0);
        }
        
        Object value = params.getValue(paramName);
        if (StringUtil.isEmpty(value)) {
            return new HashMap<>(0);
        }
        
        try {
            JsonObject map = params.getJsonObject(paramName);
            return map != null ? map.getMap() : new HashMap<>(0);
        } catch (ClassCastException e) {
            System.err.println("Expected JsonObject for map mapping, got different type");
            return new HashMap<>(0);
        }
    }

    /**
     * Extracts and maps a set of LearningCourse objects from a raw data map.
     * * @param map Source data map containing "learningCourses"
     * @return Set of LearningCourse objects, never null
     */
    @SuppressWarnings("unchecked")
    public final static Set<LearningCourse> getLearningCourses(Map<String, Object> map) {
        Object obj = map.get("learningCourses");
        if (obj != null) {
            try {
                // Suppressed warning: Gson's default map parser uses ArrayList<LinkedTreeMap>
                ArrayList<LinkedTreeMap<String, Object>> list = (ArrayList<LinkedTreeMap<String, Object>>) obj;
                
                Set<LearningCourse> set = new HashSet<>(list.size());
                list.forEach(e -> set.add(new LearningCourse(e)));
                return set;
            } catch (Exception e) {
                System.err.println("Error parsing Learning Courses: " + e.getMessage());
            }
        }
        return new HashSet<>(0);
    }

    /**
     * Extracts a string from a generic Map and applies XSS cleaning.
     * * @param map Source map
     * @param key Key to extract
     * @param defaultValue Fallback value
     * @return Cleaned string
     */
    public final static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.getOrDefault(key, defaultValue);
        return XssFilterUtil.clean(val != null ? val.toString() : "");
    }

    /**
     * Extracts a string from a generic Map and applies XSS cleaning with empty default.
     * * @param map Source map
     * @param key Key to extract
     * @return Cleaned string
     */
    public final static String getString(Map<String, Object> map, String key) {
        return getString(map, key, "");
    }

    /**
     * Extracts a string and strips out all non-digit characters to format a phone number.
     * * @param map Source map
     * @param key Key containing the phone number
     * @return Digits-only string representing the phone number
     */
    public final static String getPhoneNumber(Map<String, Object> map, String key) {
        return getString(map, key, "").replaceAll("[^\\d]", "");
    }

    /**
     * Constructs a FeedbackEvent object mapping data from request parameters.
     * * @param eventParams MultiMap of request inputs
     * @param eventName Name of the event
     * @return Populated FeedbackEvent or null if parsing fails
     */
    public final static FeedbackEvent getFeedbackEvent(MultiMap eventParams, String eventName) {
        String jsonStr = StringUtil.decodeUrlUTF8(eventParams.get(HttpParamKey.EVENT_DATA));
        if (StringUtil.isNotEmpty(jsonStr)) {
            try {
                FeedbackEvent fbe = GSON.fromJson(jsonStr, FeedbackEvent.class);
                if (fbe != null) {
                    fbe.setEventName(eventName);
                    fbe.setTouchpointName(eventParams.get(HttpParamKey.TOUCHPOINT_NAME));
                    fbe.setTouchpointUrl(eventParams.get(HttpParamKey.TOUCHPOINT_URL));
                    fbe.setFingerprintId(eventParams.get(HttpParamKey.FINGERPRINT_ID));
                    fbe.setRefVisitorId(eventParams.get(HttpParamKey.VISITOR_ID));
                    return fbe;
                }
            } catch (JsonSyntaxException e) {
                System.err.println("Error parsing FeedbackEvent: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Converts a raw JSON string into a HashMap.
     * * @param jsonStr Raw JSON string
     * @return Parsed map, never null
     */
    public static Map<String, Object> parseJsonToGetHashMap(String jsonStr) {
        Map<String, Object> map = null;
        try {
            if (StringUtil.isNotEmpty(jsonStr)) {
                Type strMapType = new TypeToken<Map<String, Object>>() {}.getType();
                map = GSON.fromJson(jsonStr, strMapType);
            }
        } catch (Exception e) {
            System.err.println("Error in parseJsonToGetHashMap: " + e.getMessage());
        }
        return map != null ? map : new HashMap<>(0);
    }

    /**
     * Extracts a HashSet of strings mapped inside a JsonObject.
     * * @param formData Source JsonObject
     * @param name Key containing the JSON array string
     * @return Set of strings, never null
     */
    public final static Set<String> getHashSet(JsonObject formData, String name) {
        Set<String> set = null;
        try {
            if (formData != null) {
                String jsonStr = StringUtil.decodeUrlUTF8(formData.getString(name, ""));
                if (StringUtil.isNotEmpty(jsonStr)) {
                    Type type = new TypeToken<HashSet<String>>() {}.getType();
                    set = GSON.fromJson(jsonStr, type);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in getHashSet: " + e.getMessage());
        }
        return set != null ? set : new HashSet<>(0);
    }

    /**
     * Parses a complex nested Map structure from request parameters.
     * * @param params Vert.x MultiMap
     * @param paramName Key containing the JSON payload
     * @return Map of String to Set of Strings, never null
     */
    public final static Map<String, Set<String>> getMapSetFromRequestParams(MultiMap params, String paramName) {
        Map<String, Set<String>> map = null;
        try {
            String jsonStr = StringUtil.decodeUrlUTF8(params.get(paramName));
            if (StringUtil.isNotEmpty(jsonStr)) {
                Type strMapType = new TypeToken<Map<String, Set<String>>>() {}.getType();
                map = GSON.fromJson(jsonStr, strMapType);
            }
        } catch (JsonSyntaxException e) {
             System.err.println("JSON Syntax Error in getMapSetFromRequestParams: " + e.getMessage());
        }
        return map != null ? map : new HashMap<>(0);
    }
}