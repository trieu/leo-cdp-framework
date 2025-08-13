package leotech.cdp.handler;

import org.apache.http.HttpStatus;

import com.google.gson.Gson;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.domain.TargetMediaUnitManagement;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.common.BaseHttpHandler;
import leotech.system.model.DeviceInfo;
import leotech.system.util.HttpTrackingUtil;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.version.SystemMetaData;
import leotech.web.model.WebData;
import rfx.core.util.StringUtil;

public final class ObserverWedHandler {
	
	
	private static final int MIN_LENGTH_FOR_VALID_URL = 10;

	/**
	 * @param req
	 * @param urlPath
	 * @param reqHeaders
	 * @param params
	 * @param resp
	 * @param device
	 */
	public static void processHtmlRecommender(HttpServerRequest req, String urlPath, MultiMap reqHeaders, MultiMap params, HttpServerResponse resp, boolean forProduct) {
		String observerId = HttpWebParamUtil.getString(params, HttpParamKey.OBSERVER_ID, "");
		String vid = HttpWebParamUtil.getString(params, HttpParamKey.VISITOR_ID, "");
		String tpUrl = HttpWebParamUtil.getString(params, HttpParamKey.TOUCHPOINT_URL, "");
		resp.setStatusCode(HttpStatus.SC_OK);
		if (!vid.isEmpty()) {
			WebData model = ObserverWebModel.buildRecommenerModel(observerId, vid , tpUrl, forProduct);
			String html = WebData.renderHtml(model);
			resp.end(html);
		} else {
			resp.end("");
		}
	}

	/**
	 * @param req
	 * @param urlPath
	 * @param reqHeaders
	 * @param params
	 * @param resp
	 * @param device
	 */
	public static void processShortLinkClick(HttpServerRequest req, String urlPath, MultiMap reqHeaders, MultiMap params, HttpServerResponse resp, DeviceInfo device) {
		String[] toks = urlPath.split("/");
		String targetMediaId = toks[toks.length - 1];

		if (!targetMediaId.isEmpty()) {
			TargetMediaUnit media = TargetMediaUnitManagement.getById(targetMediaId);
			if(media != null) {
				String landingPageUrl = media.getLandingPageUrl();
				if (landingPageUrl.length() > MIN_LENGTH_FOR_VALID_URL) {
					
					String referer = StringUtil.safeString(reqHeaders.get(BaseHttpHandler.REFERER), "");
					String host = SystemMetaData.DOMAIN_CDP_ADMIN;
					String eventName = BehavioralEvent.STR_SHORT_LINK_CLICK;
					
					EventObserver observer = EventObserverManagement.getEventObserver(media);
					if(observer != null) {
						String observerId = observer.getId();	
						String adId = StringUtil.safeString(params.get("adId"), "");
						String placementId = StringUtil.safeString(params.get("placementId"), "");
						String campaignId = StringUtil.safeString(params.get("campaignId"), "");
						
						// build model
						WebData model = ObserverWebModel.buildClickRedirectData(media, landingPageUrl, referer, host,  eventName, observerId, adId,  placementId, campaignId);
						String html = WebData.renderHtml(model);
						
						resp.setStatusCode(HttpStatus.SC_OK);
						resp.headers().add("Cache-Control", "no-cache, no-store, must-revalidate");
						resp.headers().add("Pragma", "no-cache");
						resp.headers().add("Expires", "0");
						resp.end(html);
					} else {
						String html = "Not found any default Leo Data Observer!";
						resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
						resp.end(html);
					}
				} else {
					HttpTrackingUtil.trackingResponse(req);
				}
			}
			else {
				HttpTrackingUtil.trackingResponse(req);
			}
		} else {
			resp.setStatusCode(404);
			resp.end("TargetMediaUnit ID is INVALID");
		}
	}
	
	/**
	 * @param req
	 * @param urlPath
	 * @param reqHeaders
	 * @param params
	 * @param resp
	 * @param device
	 */
	public static void processQrCodeScan(HttpServerRequest req, String urlPath, MultiMap reqHeaders, MultiMap params, HttpServerResponse resp, DeviceInfo device) {
		String[] toks = urlPath.split("/");
		String id = toks[toks.length - 1];

		if (!id.isEmpty()) {
			TargetMediaUnit media = TargetMediaUnitManagement.getById(id);
			if(media != null) {
				String url = media.getLandingPageUrl();
				if (url.length() > 10) {
					
					String referer = StringUtil.safeString(reqHeaders.get(BaseHttpHandler.REFERER), "");
					String host = SystemMetaData.DOMAIN_CDP_ADMIN;
					String tplFolderName = ObserverWebModel.TARGET_MEDIA_TPL_FOLDER;
					String tpl = BehavioralEvent.STR_QR_CODE_SCAN;
					
					WebData model = new WebData(host, tplFolderName, tpl);
					model.setCustomData("observerId", media.getRefObserverId());
					model.setCustomData("touchpointHubId", media.getRefTouchpointHubId());
					model.setCustomData("referer", referer);
					model.setCustomData("landingPageName", media.getLandingPageName() );
					model.setCustomData("targetMediaUrl", url);
					
					System.out.println(new Gson().toJson(media));
					
					String html = WebData.renderHtml(model);
					resp.setStatusCode(HttpStatus.SC_OK);
					resp.end(html);
					
				} else {
					HttpTrackingUtil.trackingResponse(req);
				}
			}
			else {
				HttpTrackingUtil.trackingResponse(req);
			}
		} else {
			resp.setStatusCode(404);
			resp.end("TargetMediaUnit ID is INVALID");
		}
	}
}
