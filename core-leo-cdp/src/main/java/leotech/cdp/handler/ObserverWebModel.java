package leotech.cdp.handler;

import java.io.IOException;
import java.util.Map;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.internal.text.StringEscapeUtils;
import com.google.gson.Gson;

import io.vertx.core.MultiMap;
import leotech.cdp.dao.AssetTemplateDaoUtil;
import leotech.cdp.domain.AssetItemManagement;
import leotech.cdp.domain.ProductItemManagement;
import leotech.cdp.domain.TouchpointHubManagement;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.FeedbackType;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.common.BaseHttpHandler;
import leotech.system.domain.SystemConfigsManagement;
import leotech.system.util.AppMetadataUtil;
import leotech.system.version.SystemMetaData;
import leotech.web.model.WebData;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

public final class ObserverWebModel {
	public static final String TEMPLATE_HTML_IFRAME = "template-html-iframe";
	public static final String TEMPLATE_SURVEY_FORM = "template-survey-form";
	public static final String TARGET_MEDIA_TPL_FOLDER = "target-media";

	public static WebData buildWebFormDataModel(MultiMap reqHeaders, MultiMap params) {
		Map<String, String> keywordMap = ProductItemManagement.getKeywordsForSubscriptionForm();

		String referer = StringUtil.safeString(reqHeaders.get(BaseHttpHandler.REFERER), "");
		
		String touchpointName = StringUtil.decodeUrlUTF8(params.get(HttpParamKey.TOUCHPOINT_NAME));
		touchpointName = StringEscapeUtils.unescapeHtml4(touchpointName);
		
		String touchpointUrl = StringUtil.decodeUrlUTF8(params.get(HttpParamKey.TOUCHPOINT_URL));
		touchpointUrl = StringEscapeUtils.unescapeHtml4(touchpointUrl);
		
		String defObserverId = TouchpointHub.DATA_OBSERVER.getId();
		String observerId = StringUtil.safeString(params.get(HttpParamKey.OBSERVER_ID),defObserverId);
		
		// not in embedded iframe, maybe from QR code scan
		if( ! defObserverId.equals(observerId) && touchpointUrl.isEmpty()) {
			TouchpointHub tpHub = TouchpointHubManagement.getByObserverId(observerId);
			if(tpHub != null) {
				touchpointUrl = tpHub.getUrl();
				touchpointName = tpHub.getName();
			}
		}
		
		boolean preview = StringUtil.safeString(params.get("preview")).equals("1");
		String templateId = StringUtil.safeString(params.get(HttpParamKey.TEMPLATE_ID));

		String host = SystemMetaData.DOMAIN_CDP_ADMIN;
		String tplFolderName = AppMetadataUtil.WEB_FORM_TEMPLATE_FOLDER;
		String title = "";

		String feedbackTypeAsText = "";
		String surveyJsonMetaData = "{}";
		String feedbackHtmlForm = "";
		String headCode = "";
		String bodyCode = "";
		
		String defaultForm = TouchpointHubManagement.DEFAULT_FORM_NAME;
		String formName = StringUtil.safeString(params.get(HttpParamKey.FORM_NAME), defaultForm);
		
		String defaultTrackingMetric = BehavioralEvent.STR_SUBMIT_FEEDBACK_FORM;
		
		if (StringUtil.isNotEmpty(templateId)) {
			AssetTemplate template = AssetTemplateDaoUtil.getById(templateId);
			if (template != null) {
				title = template.getTitle();
				int templateType = template.getTemplateType();
				feedbackTypeAsText = FeedbackType.getFeedbackTypeAsText(templateType);
				headCode = template.getHeadCode();
				
				if(templateType == FeedbackType.SURVEY) {
					// customer research or feedback
					surveyJsonMetaData = template.getJsonMetadata();
					formName = TEMPLATE_SURVEY_FORM;
				} 
				else if(templateType == FeedbackType.CONTACT) {
					// lead contact
					surveyJsonMetaData = template.getJsonMetadata();
					formName = TEMPLATE_SURVEY_FORM;
					defaultTrackingMetric = BehavioralEvent.STR_SUBMIT_CONTACT;
				} 
				else {
					// star rating HTML
					bodyCode = template.getBodyCode();
					formName = TEMPLATE_HTML_IFRAME;
				}
			}
			else {
				System.err.println("Not found AssetTemplate for templateId:"+templateId);
			}
		}
		
		WebData model = new WebData(host, tplFolderName, formName);
		model.setDefaultTrackingMetric(defaultTrackingMetric);

		model.setPageTitle(title);
		model.setCustomData("preview", preview);
		model.setCustomData("templateId", templateId);
		model.setCustomData("observerId", observerId);
		model.setCustomData("referer", referer);
		
		model.setCustomData("srcTouchpointName", touchpointName);
		model.setCustomData("srcTouchpointUrl", touchpointUrl);
		
		model.setCustomData("feedbackTypeAsText", feedbackTypeAsText);
		model.setCustomData("feedbackHtmlForm", feedbackHtmlForm);
		
		model.setCustomData("headCode", headCode);
		model.setCustomData("bodyCode", bodyCode);
		model.setCustomData("contentKeywords", new Gson().toJson(keywordMap));
		model.setCustomData("surveyJsonMetaData", surveyJsonMetaData);
		return model;
	}

	public static String getTemplateHtml(MultiMap reqHeaders, MultiMap params) {
		String id = StringUtil.safeString(params.get(HttpParamKey.TEMPLATE_ID));

		String html = "";
		if (StringUtil.isNotEmpty(id)) {
			AssetTemplate tpl = AssetTemplateDaoUtil.getById(id);
			if (tpl != null) {
				try {
					Handlebars handlebars = new Handlebars();
					Template template = handlebars.compileInline(tpl.getMediaInfo());
					Object model = new Gson().fromJson(tpl.getJsonMetadata(), Object.class);
					html = template.apply(model);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return html;
	}

	public static WebData buildRecommenerModel(String observerId, String vid, String srcTpUrl, boolean forProduct) {
		String templateName;
		if(forProduct) {
			templateName = "product-recommendation";
		}
		else {
			templateName = "content-recommendation";
		}
		
		WebData model = new WebData(SystemMetaData.DOMAIN_CDP_ADMIN, TARGET_MEDIA_TPL_FOLDER, templateName);
		model.setCustomData("observerId", observerId);
		model.setCustomData("visitorId", vid);
		model.setCustomData("touchpointUrl", srcTpUrl);
		
		Object maxF = SystemConfigsManagement.getLeoRecommenderConfigs().getOrDefault("service_max_item_for_profile", 50);
		Object maxR = SystemConfigsManagement.getLeoRecommenderConfigs().getOrDefault("service_max_item_for_request", 10);
		model.setCustomData("max_item_for_profile", maxF);
		model.setCustomData("max_item_for_request", maxR);
		// add keyword to filter
		
		return model;
	}
	
	
	public static WebData buildClickRedirectData(TargetMediaUnit media, String landingPageUrl, String referer, String host, String tpl, String observerId, String adId, String placementId, String campaignId) {
		WebData model = new WebData(host, TARGET_MEDIA_TPL_FOLDER, tpl);
		
		model.setCustomData("adId", adId);
		model.setCustomData("placementId", placementId);
		model.setCustomData("campaignId", campaignId);
		
		model.setCustomData("observerId", observerId);
		model.setCustomData("referer", referer);
		model.setCustomData("injectedVisitorId", media.getRefVisitorId());
		model.setCustomData("affiliateId", media.getRefAffiliateId());
		model.setCustomData("targetMediaUrl", landingPageUrl);

		Map<String, String> customData = media.getCustomData();
		
		// newsletterSubscription webform
		String newsletterSubscription = customData.getOrDefault("newsletterSubscription","");
		if(StringPool.TRUE.equals(newsletterSubscription)) {
			model.setCustomData("newsletterSubscription", true);
			model.setCustomData("newsletterHeader", customData.getOrDefault("newsletterHeader", "Subscribe to our Newsletter") );
			model.setCustomData("newsletterDescription", customData.getOrDefault("newsletterDescription", "Please enter your name and email for newsletter subscription") );
			model.setCustomData("newsletterButtonText", customData.getOrDefault("newsletterButtonText", "OK"));
		}
		else {
			model.setCustomData("newsletterSubscription", false);
			model.setCustomData("newsletterHeader", "" );
			model.setCustomData("newsletterDescription", "" );
			model.setCustomData("newsletterButtonText", "");
		}
		
		// contentId
		AssetContent content = media.getContentItem();
		if (content != null) {
			model.setCustomData("contentId", content.getId());
		}

		// PRODUCT
		ProductItem product = media.getProductItem();
		if (product != null) {
			model.setCustomData("srcTouchpointName", "Product Item: " + product.getTitle());
			model.setCustomData("productId", product.getProductId());
			model.setCustomData("productIdType", product.getProductIdType());
		} else {
			model.setCustomData("srcTouchpointName", media.getLandingPageName());
			model.setCustomData("productId", "");
			model.setCustomData("productIdType", "");
		}
		return model;
	}
	
	public static WebData buildContentDataModel(MultiMap reqHeaders, MultiMap params, String slug) {
		String referer = StringUtil.safeString(reqHeaders.get(BaseHttpHandler.REFERER),"direct");
		String injectedVisitorId = StringUtil.safeString(params.get(HttpParamKey.VISITOR_ID));
		String observerId = TouchpointHub.DATA_OBSERVER.getId();
		
		AssetContent content = AssetItemManagement.getContentItemBySlug(slug);
		if(content != null) {
			String host = SystemMetaData.DOMAIN_CDP_ADMIN;
			String tplFolderName = AppMetadataUtil.RECOMMENDER_TEMPLATE_FOLDER;
			String title = content.getTitle();
			String tplName;
			if(content.isPresentation()) {
				tplName = AssetContent.PRESENTATION_FOLDER_NAME;
			}
			else {
				tplName = AssetContent.CONTENT_FOLDER_NAME;
			}
			WebData model = new WebData(host, tplFolderName, tplName);
			model.setPageTitle(title);
			model.setCustomData("referer", referer);
			model.setCustomData("observerId", observerId);
			model.setCustomData("content", content);
			model.setCustomData("injectedVisitorId", injectedVisitorId);
			return model;
		}
		return null;
	}
}
