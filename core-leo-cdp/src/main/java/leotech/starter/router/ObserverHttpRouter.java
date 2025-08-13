package leotech.starter.router;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.handler.WebhookDataHandler;
import leotech.cdp.handler.ObserverHttpGetHandler;
import leotech.cdp.handler.ObserverHttpPostHandler;
import leotech.system.common.BaseHttpHandler;
import leotech.system.common.BaseHttpRouter;
import leotech.system.model.DeviceInfo;
import leotech.system.template.HandlebarsTemplateUtil;
import leotech.system.util.DeviceInfoUtil;
import leotech.system.util.HttpTrackingUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * @author Trieu Nguyen diagram:
 * https://github.com/USPA-Technology/leo-cdp-build/blob/master/leo-cdp-event-observer-data-flow.md
 */
public final class ObserverHttpRouter extends BaseHttpRouter {

	public static final String INVALID = "invalid";
	public static final String FAILED = "failed";
	public static final String OK = "ok";

	public static final String PREFIX_TARGET_MEDIA_CLICK_TRACKING = "/ct/";
	public static final String PREFIX_TARGET_MEDIA_QR_CODE_TRACKING = "/qrct/";
	
	public static final String PREFIX_CONTEXT_SESSION_PROFILE_INIT = "/cxs-pf-init";
	public static final String PREFIX_CONTEXT_SESSION_PROFILE_UPDATE = "/cxs-pf-update";
	
	// HTML render
	public static final String PREFIX_CONTENT  = "/content/";
	public static final String PREFIX_PRESENTATION  = "/presentation/";
	public static final String PREFIX_WEBFORM = "/webform";
	public static final String PREFIX_FEEDBACK_SCORE = "/feedback-score";
	public static final String PREFIX_WEB_TEMPLATE_HTML = "/webhtml";
	
	// Recommender JSON
	public static final String PREFIX_JSON_RECOMMENDER_PRODUCTS = "/ris/json/products";
	public static final String PREFIX_JSON_RECOMMENDER_CONTENTS = "/ris/json/contents";
	
	// Recommender HTML
	public static final String PREFIX_HTML_RECOMMENDER_PRODUCTS = "/ris/html/products";
	public static final String PREFIX_HTML_RECOMMENDER_CONTENTS = "/ris/html/contents";
	
	public static final String PREFIX_EVENT_VIEW = "/etv";
	public static final String PREFIX_EVENT_ACTION = "/eta";
	public static final String PREFIX_EVENT_CONVERSION = "/etc";
	public static final String PREFIX_EVENT_FEEDBACK = "/efb";

	public static final String PREFIX_TOUCHPOINT  = "/touchpoint";
	
	public static final String BASE_URL_TARGET_MEDIA_CLICK_TRACKING = "https://" + SystemMetaData.DOMAIN_CDP_OBSERVER + PREFIX_TARGET_MEDIA_CLICK_TRACKING;


	public ObserverHttpRouter(RoutingContext context) {
		super(context);
		// caching or not caching templates in resources
		boolean enableCaching = SystemMetaData.isEnableCachingViewTemplates();
		if (enableCaching) {
			HandlebarsTemplateUtil.enableUsedCache();
		} else {
			HandlebarsTemplateUtil.disableUsedCache();
		}
		// trigger a scheduler for data summary computation ?
	}

	@Override
	public boolean handle() throws Exception {
		HttpServerRequest req = context.request();

		String httpMethod = req.rawMethod();
		String urlPath = StringUtil.safeString(req.path());
	
		MultiMap reqHeaders = req.headers();
		MultiMap params = req.params();
		HttpServerResponse resp = req.response();
		MultiMap outHeaders = resp.headers();
		outHeaders.set(CONNECTION, HttpTrackingUtil.HEADER_CONNECTION_CLOSE);
		outHeaders.set(POWERED_BY, SERVER_VERSION);
		
		String useragent = StringUtil.safeString(req.getHeader(BaseHttpHandler.USER_AGENT));
		DeviceInfo device = DeviceInfoUtil.getDeviceInfo(useragent);

		try {
			System.out.println("urlPath " + urlPath);
			String origin = StringUtil.safeString(reqHeaders.get(BaseHttpHandler.ORIGIN), "*");
			
			// WEBHOOK and Domain Verifier
			boolean rs = WebhookDataHandler.process(this.context, req, urlPath, reqHeaders, params, resp, outHeaders, device, origin);
			if(rs) {
				return true;
			}
			// EVENT tracking for real user 
			else if( device.isNotWebCrawler() ) {
				if(httpMethod.equalsIgnoreCase(HTTP_METHOD_GET)) {
					return ObserverHttpGetHandler.process(this.context, req, urlPath, reqHeaders, params, resp, outHeaders, device, origin);
				} 
				else if(httpMethod.equalsIgnoreCase(HTTP_METHOD_POST) || httpMethod.equalsIgnoreCase(HTTP_METHOD_PUT)) {
					return ObserverHttpPostHandler.process(this.context, req, urlPath, reqHeaders, params, resp, outHeaders, device, origin);
				}
			}
			// no handler found
			resp.end("CDP Observer_"+DEFAULT_RESPONSE_TEXT);
			return false;
		} catch (Exception e) {
			// resp.end();
			System.err.println("urlPath:" + urlPath + " error:" + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
	
}