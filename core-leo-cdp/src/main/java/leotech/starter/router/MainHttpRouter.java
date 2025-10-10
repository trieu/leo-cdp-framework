package leotech.starter.router;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.handler.MainHttpRouterUtil;
import leotech.system.common.BaseHttpHandler;
import leotech.system.common.BaseHttpHandler.JsonErrorPayload;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.BaseWebRouter;
import leotech.system.common.PublicFileHttpRouter;
import leotech.system.common.SecuredApiHandler;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.domain.SystemInfo;
import leotech.system.model.DeviceInfo;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.system.template.HandlebarsTemplateUtil;
import leotech.system.template.TemplateUtil;
import leotech.system.util.AppMetadataUtil;
import leotech.system.util.DeviceInfoUtil;
import leotech.system.util.HttpTrackingUtil;
import leotech.system.util.LogUtil;
import leotech.system.version.SystemMetaData;
import leotech.web.model.WebData;
import rfx.core.configs.WorkerConfigs;
import rfx.core.util.StringUtil;

/**
 * Main HTTP Server Router
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class MainHttpRouter extends BaseHttpRouter {

	private static final String RESOURCES_APP_TEMPLATES = "./resources/app-templates";
	private static final String _1 = "1";
	private static final String ADS_TXT_CONTENT = WorkerConfigs.load().getCustomConfig("ADS_TXT_CONTENT");
	private static final String MIME_TYPE_HTML = ContentType.TEXT_HTML.getMimeType();
	
	public static final String URI_LEO_CDP_IMPORTER = "/LEO_CDP_IMPORTER";
	public static final String URI_HTML_ROUTER = "/html/";
	public static final String URI_ADMIN_ROUTER = "/admin";
	public static final String URI_API_ROUTER = "/api";
	public static final String URI_APP_ROUTER = "/app";
	public static final String URI_VIEW_ROUTER = "/view";
	public static final String URI_DEFAULT_ROUTER = "/";
	public static final String URI_FILE_UPLOADER_ROUTER = "/file-uploader";
	public static final String URI_FAVICON_ICO = "/favicon.ico";
	public static final String URI_VERSION = "/version";
	public static final String URI_ADS_TXT = "/ads.txt";
	
	static final Map<String, String> STATIC_FILES = new ConcurrentHashMap<>(10000);

	public MainHttpRouter(RoutingContext context) {
		super(context);
		// caching or not caching templates in resources
		boolean enableCaching = SystemMetaData.isEnableCachingViewTemplates();
		if (enableCaching) {
			HandlebarsTemplateUtil.enableUsedCache();
		} else {
			HandlebarsTemplateUtil.disableUsedCache();
		}
	}
	
	

	@Override
	public boolean handle() throws Exception {
		HttpServerRequest req = context.request();
		HttpServerResponse resp = context.response();

		MultiMap outHeaders = resp.headers();
		outHeaders.set(CONNECTION, HttpTrackingUtil.HEADER_CONNECTION_CLOSE);
		outHeaders.set(POWERED_BY, SERVER_VERSION);

		// CORS Header
		MultiMap reqHeaders = req.headers();
		String origin = StringUtil.safeString(reqHeaders.get(BaseHttpHandler.ORIGIN), "*");

		// User Info
		String userSession = StringUtil.safeString(reqHeaders.get(BaseWebRouter.HEADER_SESSION));

		// ---------------------------------------------------------------------------------------------------
		String absoluteURI = req.absoluteURI();
		System.out.println("absoluteURI " + absoluteURI);
		
		String path = req.path();
		MultiMap params = req.params();

		// allow set host from query params or get default from Http-Host
		String host = StringUtil.safeString(params.get("host"), req.host().replace("www.", ""));

		String userAgent = req.getHeader(HttpHeaderNames.USER_AGENT);
		DeviceInfo device = DeviceInfoUtil.getDeviceInfo(userAgent);
		
		// API of CDP
		if (path.equals(URI_API_ROUTER)) {
			outHeaders.set(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);
			String json = SecuredApiHandler.process(context, req);
			resp.end(json);
		}

		// DEFAULT page
		// Static File Handler with CDN (Images, CSS, JS, JSON,...)
		else if (path.startsWith(PublicFileHttpRouter.PUBLIC_FILE_ROUTER)) {
			BaseHttpRouter.setCacheControlHeader(outHeaders);
			PublicFileHttpRouter.handle(resp, outHeaders, path, params);
		}
		
		// static HTML view Ajax loader
		else if (path.startsWith(URI_VIEW_ROUTER)) {
			BaseHttpRouter.setCacheControlHeader(outHeaders);
			viewRoutingHandler(req, resp, path, host);
		}

		// Progressive Web App in Mobile Hibrid App
		else if (path.startsWith(URI_APP_ROUTER)) {
			outHeaders.set(CONTENT_TYPE, MIME_TYPE_HTML);
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);

			// handler as SPA app for end-user
			// String appId = req.getParam("appid");
			String[] toks = path.split(URI_DEFAULT_ROUTER);
			int length = toks.length;
			if (length > 1) {
				String appId = toks[length - 1];
				handleWebPageRequest(params, false, host, appId, "index", resp);
			}
		}
		
		// Admin app
		else if (path.equals(URI_ADMIN_ROUTER)) {
			outHeaders.set(CONTENT_TYPE, MIME_TYPE_HTML);
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);
			String tplFolder = AppMetadataUtil.DEFAULT_ADMIN_TEMPLATE_FOLDER;
			handleWebPageRequest(params, false, host, tplFolder, "index", resp);
		}

		// HTML render public data as website for SEO
		else if (path.startsWith(URI_HTML_ROUTER)) {
			// for SEO or Facebook BOT
			outHeaders.set(CONTENT_TYPE, MIME_TYPE_HTML);
			BaseHttpRouter.setCacheControlHeader(outHeaders);
			WebData model = MainHttpRouterUtil.build(path, host, params, userSession);
			String html = WebData.renderHtml(model);
			resp.setStatusCode(model.getHttpStatusCode());
			resp.end(html);
		}

		// Ads.TXT
		else if (path.equalsIgnoreCase(URI_ADS_TXT)) {
			resp.setStatusCode(HttpStatus.SC_OK);
			resp.end(ADS_TXT_CONTENT);
		}

		// Favicon
		else if (path.equalsIgnoreCase(URI_FAVICON_ICO)) {
			resp.end("");
		}
		
		// Uploading File HTTP POST Handler
		else if(path.equalsIgnoreCase(URI_FILE_UPLOADER_ROUTER)) {
			return callUploadFileHandler(req, resp, reqHeaders, userSession);
		}

		// ---------------------------------------------------------------------------------------------------
		else if (path.equalsIgnoreCase(URI_PING)) {
			resp.end(PONG);
		} 
		
		else if (path.equalsIgnoreCase(URI_SYSINFO)) {
			SystemInfo systemInfo = new SystemInfo();
			resp.end(systemInfo.toString());
		} 
		
		else if (path.equalsIgnoreCase(URI_VERSION)) {
			resp.end("CDP Admin_"+DEFAULT_RESPONSE_TEXT);
		} 
		
		else if (path.equalsIgnoreCase(URI_LEO_CDP_IMPORTER)) {
			resp.end("CDP DATA IMPORTER version: "+DEFAULT_RESPONSE_TEXT);
		} 
		
		// 
		else if (path.equals(URI_DEFAULT_ROUTER)) {
			outHeaders.set(CONTENT_TYPE, MIME_TYPE_HTML);
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);

			boolean isSearchEngineBot = SPIDER.equals(device.deviceName);
			// detect template folder name from domain
			String tplFolderName = AppMetadataUtil.getWebTemplateFolder(host);
			if (tplFolderName.isEmpty()) {
				resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
				resp.end(TemplateUtil._404);
				return false;
			}
			// then handle app template for end-user
			handleWebPageRequest(params, isSearchEngineBot, host, tplFolderName, "index", resp);
		}
		
		else {
			// JSON data API handler for Leo Content Hub
			AdminHttpRouter adminApiRouter = new AdminHttpRouter(context);
			adminApiRouter.enableAutoRedirectToHomeIf404();
			adminApiRouter.handle();
		}
		return true;
	}

	/**
	 * @param req
	 * @param resp
	 * @param reqHeaders
	 * @param userSession
	 * @return
	 */
	private boolean callUploadFileHandler(HttpServerRequest req, HttpServerResponse resp, MultiMap reqHeaders, String userSession) {
		SystemUser loginUser = SecuredHttpDataHandler.initSystemUser(userSession, req.path(), req.params());
		boolean ok = false;
		if (loginUser != null) {
			JsonDataPayload dataPayload = UploaderHttpRouter.uploadHandler(loginUser, context, req, reqHeaders);
			resp.setStatusCode(201).end(dataPayload.toString());
			resp.close();
			ok = true;
		}
		if (!ok) {
			resp.setStatusCode(504).end(JsonErrorPayload.NO_AUTHORIZATION.toString());
			resp.close();
		}
		return ok;
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// utilities //
	
	private static final String isValidFile(String path, String admin, String networkDomain) {
		String staticFilePath = STATIC_FILES.get(path);
		if(staticFilePath != null) {
			return staticFilePath;
		}
		else {			
			boolean isAdminReq = admin.equals(_1);
			if (isAdminReq) {
				networkDomain = SystemMetaData.DOMAIN_CDP_ADMIN;
			}
			String tplFolder = AppMetadataUtil.getWebTemplateFolder(networkDomain);
			String relPath = RESOURCES_APP_TEMPLATES + URI_DEFAULT_ROUTER + tplFolder;
			staticFilePath = path.replace(URI_VIEW_ROUTER, relPath).replaceAll("%20", " ");
			
			File file = new File(staticFilePath);			
			if(file.isFile()) {
				STATIC_FILES.put(path, staticFilePath);
				LogUtil.logInfo(MainHttpRouter.class, "STATIC_FILES.put OK pathname: " + staticFilePath);
				return staticFilePath;	
			}
			else {
				return null;
			}				
		}
	}
	
	void viewRoutingHandler(HttpServerRequest req, HttpServerResponse resp, String path, String networkDomain) {
		try {
			
			String admin =  StringUtil.safeString( req.params().get("admin") ) ;
			String staticFilePath = isValidFile(path, admin, networkDomain);
			if (staticFilePath != null) {
				resp.sendFile(staticFilePath);
			} else {
				resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
				resp.end(TemplateUtil._404);
			}
		} catch (Exception e) {
			resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.end(TemplateUtil._500);
		}
	}

	void handleWebPageRequest(MultiMap params, boolean isSearchEngineBot, String host, String tplFolderName, String tplName, HttpServerResponse resp) {
		WebData model = MainHttpRouterUtil.buildModel(host, tplFolderName, tplName, params);

		String html = WebData.renderHtml(model);
		resp.setStatusCode(model.getHttpStatusCode());
		if (isSearchEngineBot) {
			// TODO, run headless Google Chrome to get full HTML, then caching as file  in shared folder
			String fullHtml = html;
			resp.end(fullHtml);
		} else {
			resp.end(html);
		}
	}

}