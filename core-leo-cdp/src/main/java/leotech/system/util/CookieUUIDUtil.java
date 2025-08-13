package leotech.system.util;

import static io.vertx.core.http.HttpHeaders.COOKIE;
import static io.vertx.core.http.HttpHeaders.USER_AGENT;

import java.net.URLDecoder;
import java.util.Set;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.spdy.SpdyHeaders.HttpNames;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import leotech.system.common.BaseHttpRouter;
import rfx.core.util.SecurityUtil;
import rfx.core.util.StringUtil;

public class CookieUUIDUtil {

	static final String unknown = "unknown";
	public static final String COOKIE_UUID = "leouuid";

	public static String getUUID(HttpServerRequest req) {
		String uuid = null;
		String cookieString = req.headers().get(COOKIE);
		// System.out.println(cookieString);
		if (cookieString != null) {
			try {
				cookieString = URLDecoder.decode(cookieString, "UTF-8");
				Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
				// System.out.println("cookies "+cookies);
				for (Cookie cookie : cookies) {
					String name = cookie.name();
					if (name.equals(COOKIE_UUID)) {
						uuid = cookie.value();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return uuid;
	}

	public static String getRemoteIP(HttpServerRequest request) {
		String ipAddress = request.headers().get("X-Forwarded-For");
		if (!StringUtil.isNullOrEmpty(ipAddress) && !unknown.equalsIgnoreCase(ipAddress)) {
			// LogUtil.dumpToFileIpLog(ipAddress);
			String[] toks = ipAddress.split(",");
			int len = toks.length;
			if (len > 1) {
				ipAddress = toks[len - 1];
			} else {
				return ipAddress;
			}
		}
		return ipAddress;
	}

	/**
	 * 
	 * 
	 * @param req
	 * @param resp
	 * @return UUID string
	 */
	public static String generateCookieUUID(HttpServerRequest req, HttpServerResponse resp) {
		String uuid = getUUID(req);
		if (uuid == null) {
			MultiMap headers = req.headers();
			String userAgent = headers.get(USER_AGENT);
			String host = headers.get(HttpNames.HOST);
			String ip = getRemoteIP(req);
			uuid = SecurityUtil.sha1(userAgent + ip + System.currentTimeMillis()).substring(0, 24);

			Cookie uuidCookie = new DefaultCookie(COOKIE_UUID, uuid);
			uuidCookie.setHttpOnly(false);
			uuidCookie.setMaxAge(BaseHttpRouter.COOKIE_AGE_10_YEAR);
			uuidCookie.setPath("/");
			uuidCookie.setDomain(host);
			resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(uuidCookie));
		}
		// System.out.println("UUID: " + uuid);
		return uuid;
	}
}
