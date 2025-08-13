package leotech.system.util;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.devskiller.friendly_id.FriendlyId;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.MultiMap;
import rfx.core.util.RandomUtil;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

public class TokenUtil {

	public static Map<String, List<String>> getQueryMap(String query) {
		QueryStringDecoder decoder = new QueryStringDecoder("?" + query);
		return decoder.parameters();
	}

	public static String getParam(Map<String, List<String>> params, String key, String defaultVal, String where) {
		if (params.containsKey(key)) {
			List<String> vals = params.get(key);
			if (vals != null) {
				if (!StringUtil.isEmpty(where)) {
					for (String val : vals) {
						if (val.contains(where)) {
							return val;
						}
					}
				} else {
					return vals.get(0);
				}

			}
		}
		return defaultVal;
	}

	public static String getParam(Map<String, List<String>> params, String key, String defaultVal, int index) {
		if (params.containsKey(key)) {
			List<String> vals = params.get(key);
			if (vals != null) {
				if (index >= 0 && index < vals.size()) {
					return StringUtil.decodeUrlUTF8(vals.get(index));
				}
			}
		}
		return defaultVal;
	}

	public static String getParam(MultiMap params, String key) {
		return getParam(params, key, StringPool.BLANK);
	}

	public static String getParam(MultiMap params, String key, String defaultVal) {
		if (params.contains(key)) {
			List<String> vals = params.getAll(key);
			if (vals != null) {
				for (String val : vals) {
					if (StringUtil.isNotEmpty(val)) {
						return val;
					}
				}
			}
		}
		return defaultVal;
	}

	public static String getParam(Map<String, List<String>> params, String key) {
		return getParam(params, key, StringPool.BLANK);
	}

	public static String getParam(Map<String, List<String>> params, String key, String defaultVal) {
		if (params.containsKey(key)) {
			List<String> vals = params.get(key);
			if (vals != null) {
				for (String val : vals) {
					if (StringUtil.isNotEmpty(val)) {
						return val;
					}
				}
			}
		}
		return defaultVal;
	}

	public static String extractUrlValue(String tok) {
		try {
			tok = tok.substring(tok.indexOf("&url=http") + 5);
			String v = tok.substring(0, tok.indexOf("&"));
			return v;
		} catch (Throwable e) {
		}
		return StringPool.BLANK;
	}

	public static String extractUUID(String tok) {
		try {
			tok = tok.substring(tok.indexOf("apluuid=") + 9);
			int lastIndex = tok.indexOf(";");
			String v = lastIndex > 0 ? tok.substring(0, lastIndex) : tok;
			return v;
		} catch (Throwable e) {
		}
		return StringPool.BLANK;
	}

	public static String extractRefererURL(String cookie) {
		try {
			cookie = cookie.substring(cookie.indexOf("referer=") + 8);
			int lastIndex = cookie.indexOf(";");
			String v = lastIndex > 0 ? cookie.substring(0, lastIndex) : cookie;
			return v;
		} catch (Throwable e) {
		}
		return StringPool.BLANK;
	}

	public static String extractDomainFromUrl(String url) {
		try {
			url = url.substring(url.indexOf("://") + 3);
			String v = url.substring(0, url.indexOf("/"));
			if (!v.startsWith("="))
				return v;
		} catch (Throwable e) {
		}
		return StringPool.BLANK;
	}

	public static int getViewToClick(Map<String, List<String>> params) {
		try {
			String freq = TokenUtil.getParam(params, "freq", "1:1");
			String[] toks = freq.split(":");
			if (toks.length == 2) {
				return StringUtil.safeParseInt(toks[0], 1);
			}
		} catch (Exception e) {
		}
		return 1;
	}
	
	public final static String getRandomToken(String keyHint) {
		keyHint += (System.currentTimeMillis() + RandomUtil.getRandom(100000));
		return RandomUtil.getRandom(9999999) + "_" +FriendlyId.toFriendlyId(UUID.nameUUIDFromBytes(keyHint.getBytes()));
	}

}
