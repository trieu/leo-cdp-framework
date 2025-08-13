package leotech.system.util;

import static io.vertx.core.http.HttpHeaders.COOKIE;

import java.net.URLDecoder;
import java.util.Set;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import rfx.core.util.StringUtil;

public class CookieUserSessionUtil {

    static final String COOKIE_KEY = "leousersession";
    
    public static String getUserSession(RoutingContext context, String defaultVal) {
	io.vertx.ext.web.@Nullable Cookie value = context.getCookie(COOKIE_KEY);
	if(value != null) {	    
	    return value.getValue();
	}
	return defaultVal;
    }

    public static String getUserSession(HttpServerRequest req) {
	String value = "";
	String cookieString = req.headers().get(COOKIE);
	// System.out.println(cookieString);
	if (cookieString != null) {
	    try {
		cookieString = URLDecoder.decode(cookieString, "UTF-8");
		Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
		// System.out.println("cookies "+cookies);
		for (Cookie cookie : cookies) {
		    String name = cookie.name();
		    if (name.equals(COOKIE_KEY)) {
			value = StringUtil.safeString(cookie.value());
		    }
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	return value;
    }
}
