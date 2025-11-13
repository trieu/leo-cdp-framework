package leotech.system.common;

import java.util.concurrent.ExecutorService;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import leotech.system.util.IoProcessors;
import leotech.system.version.SystemMetaData;

/**
 * Base Http Router
 * 
 * @author @tantrieuf31
 * @since 2020
 */
public abstract class BaseHttpRouter {

	public static final String PONG = "PONG";
	public static final String POWERED_BY = "PoweredBy";
	public static final String SERVER_VERSION = "LeoTech";

	public static final String HTTP = "http://";
	public final static String DEFAULT_PATH = "/";

	public final static String SPIDER = "Spider";
	public final static int COOKIE_AGE_1_DAY = 86400; // One week
	public final static int COOKIE_AGE_1_WEEK = COOKIE_AGE_1_DAY * 7; // 1 week
	public final static int COOKIE_AGE_2_WEEKS = COOKIE_AGE_1_DAY * 14; // 2 weeks
	public final static int COOKIE_AGE_10_YEAR = 31557600 * 10; // 10 years

	public static final String URI_GEOIP = "/geoip";
	public static final String URI_FAVICON_ICO = "/favicon.ico";
	public static final String URI_PING = "/ping";
	public static final String URI_SYSINFO = "/sysinfo";

	public static final String URI_ERROR_404 = "/404";
	public static final String URI_ERROR_500 = "/500";

	public static final String HTTP_METHOD_POST = "POST";
	public static final String HTTP_METHOD_PUT = "PUT";
	public static final String HTTP_METHOD_PATCH = "PATCH";
	public static final String HTTP_METHOD_GET = "GET";
	public static final String HTTP_METHOD_DELETE = "DELETE";
	public static final String HTTP_GET_OPTIONS = "options";
	public static final String HEADER_SESSION = "leouss";

	final static boolean CACHING_VIEW = SystemMetaData.isEnableCachingViewTemplates();

	final protected static Logger logger = LoggerFactory.getLogger(BaseHttpRouter.class);

	final protected RoutingContext context;
	
	final protected String host;
	final protected int port;
	
	final protected String nodeId;
	final protected String nodeInfo;

	public BaseHttpRouter(RoutingContext context, String host, Integer port) {
		this.context = context;
		this.host = host;
		this.port = port;
		this.nodeId = String.format("[%s:%d]", host, port);
		this.nodeInfo = String.format("%s_%s_%s", nodeId, SystemMetaData.BUILD_EDITION, SystemMetaData.BUILD_ID);
	}

	public RoutingContext getContext() {
		return context;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getNodeInfo() {
		return nodeInfo;
	}

	public static Cookie createCookie(String name, String value, String domain, String path) {
		Cookie cookie = Cookie.cookie(name, value);
		cookie.setDomain(domain);
		cookie.setPath(path);
		return cookie;
	}

	public static void setCacheControlHeader(MultiMap headers) {
		if (CACHING_VIEW) {
			headers.set(HttpHeaders.CACHE_CONTROL, "private, max-age=604800");
		} else {
			headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
		}
	}

	public static void setCorsHeaders(MultiMap headers, String origin) {
		if (headers == null) {
			throw new IllegalArgumentException("Headers cannot be null");
		}

		// Use a whitelist or fallback for security
		String allowOrigin = (origin != null && !origin.isEmpty()) ? origin : "*";

		headers.set("Accept-Ranges", "bytes");
		headers.set("Access-Control-Allow-Origin", allowOrigin);
		headers.set("Access-Control-Allow-Credentials", "true");

		// Expanded for better REST compatibility
		headers.set("Access-Control-Allow-Methods",
				String.join(", ", "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));

		headers.set("Access-Control-Allow-Headers", String.join(", ", "Origin", "Accept", "Content-Type",
				"Authorization", "Range", "leouss", "X-Requested-With"));

		headers.set("Access-Control-Expose-Headers", String.join(", ", "Content-Range", "Content-Length", "leouss"));

		headers.set("Access-Control-Max-Age", "86400"); // 1 day
		headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.set("Connection", "keep-alive");
		headers.set("P3P", "CP=\"CAO PSA OUR\"");
		headers.set("Pragma", "no-cache");
	}

	/**
	 * thread pool optimized for high-throughput but safe concurrent work - likely
	 * for Redis or network I/O tasks.
	 */
	protected static final ExecutorService PROCESSORS = IoProcessors.EXECUTOR;

	/**
	 * HTTP handle
	 * 
	 * @return
	 * @throws Exception
	 */
	abstract public void process() throws Exception;

	protected void respond(HttpServerResponse resp, String body) {
		resp.setStatusCode(HttpStatus.SC_OK).end(body);
	}

	protected void respondError(HttpServerResponse resp, int code, String body) {
		resp.setStatusCode(code).end(body);
	}

}