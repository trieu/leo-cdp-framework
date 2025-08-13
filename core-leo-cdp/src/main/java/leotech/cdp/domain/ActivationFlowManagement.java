package leotech.cdp.domain;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import io.rocketbase.mail.EmailTemplateBuilder;
import io.rocketbase.mail.EmailTemplateBuilder.EmailTemplateConfigBuilder;
import io.rocketbase.mail.config.TbConfiguration;
import io.rocketbase.mail.config.config.TbBodyConfig;
import io.rocketbase.mail.config.config.TbBodyConfig.TbBodyBorder;
import io.rocketbase.mail.config.config.TbBodyConfig.TbBodyDark;
import io.rocketbase.mail.config.config.TbBoxConfig;
import io.rocketbase.mail.config.config.TbBoxConfig.TbBoxDark;
import io.rocketbase.mail.model.HtmlTextEmail;
import leotech.cdp.dao.TargetMediaUnitDaoUtil;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.ProcessedContent;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.marketing.EmailMessage;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.cdp.utils.ProfileDataValidator;
import leotech.system.communication.EmailSender;
import leotech.system.communication.PushNotificationSender;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 *
 */
public class ActivationFlowManagement {
	static String logo = "https://demobookshop.leocdp.com/wp-content/uploads/2020/09/bookshop-logo-416x416.png";
	static TbBodyConfig tbBodyConfig = new TbBodyConfig("#F4F4F7", new TbBodyBorder("1px", "#EAEAEC"),
			new TbBodyDark("#f7f7f7", "#292828"));

	static TbBoxConfig tbBoxConfig = new TbBoxConfig("24px", "#F4F4F7", "2px dashed #CBCCCF", new TbBoxDark("#F4F4F7"));

	static String getHtmlMailWithProduct(String name, String productName, double price, String productLink)
			throws IOException {
		EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder();

		TbConfiguration config = TbConfiguration.newInstance();
		config.getContent().setFull(true);

		config.setBody(tbBodyConfig);
		config.setBox(tbBoxConfig);
		config.getHeader().setColor("#1100fa");
		config.getTable().getItem().setColor("#1100fa");
		config.getFooter().setColor("#1100fa");

		BigDecimal priceFormated = new BigDecimal(price);
		priceFormated = priceFormated.setScale(2, RoundingMode.HALF_EVEN);

		HtmlTextEmail htmlTextEmail = builder.configuration(config).header().logo(logo).logoHeight(120)
				.text("Bookshop ").and().text("Hi " + name + ",").and().text("You may like this book ").and()
				.tableSimple("").headerRow("Description", "Amount").itemRow(productName, priceFormated).and()
				.button("Product View", productLink).green().right().and().copyright("USPA").url("https://uspa.tech")
				.suffix(". All rights reserved.").and().footerText("This is a demo email from CDP").and()
				// .footerImage(TESTONEPIXEL_PNG).width(1)
				.build();
		String html = htmlTextEmail.getHtml();
		return html;
	}

	static String getThanksEmail(String name) throws IOException {
		EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder();

		TbConfiguration config = TbConfiguration.newInstance();
		config.getContent().setFull(true);

		config.setBody(tbBodyConfig);
		config.setBox(tbBoxConfig);
		config.getHeader().setColor("#1100fa");
		config.getTable().getItem().setColor("#1100fa");
		config.getFooter().setColor("#1100fa");

		HtmlTextEmail htmlTextEmail = builder.configuration(config).header().logo(logo).logoHeight(120)
				.text("Email Confirmation ").and().text("Hi " + name + ",")
				.and().text("Thanks for your information ")
				.and()
				.copyright("USPA").url("https://uspa.tech").suffix(". All rights reserved.").and()
				.footerText("This is a demo email from CDP").and()
				// .footerImage(TESTONEPIXEL_PNG).width(1)
				.build();
		String html = htmlTextEmail.getHtml();
		return html;
	}

	public static void sendThanksEmail(String profileId, String toEmailAddress, String name) {
		try {
			String content = ActivationFlowManagement.getThanksEmail(name);
			EmailMessage messageModel = new EmailMessage("contact@uspa.tech", toEmailAddress, name, profileId, "Thanks for your information", content);
			EmailSender.sendToSmtpServer(messageModel);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendRecommendationByEmail(AssetTemplate template, String profileId, String toEmailAddress, String name, String productName, double price, String productLink) {
		try {
			if(ProfileDataValidator.isValidEmail(toEmailAddress)) {
				String content = ActivationFlowManagement.getHtmlMailWithProduct(name, productName, price, productLink);
				System.out.println(content);
				EmailMessage messageModel = new EmailMessage("contact@uspa.tech", toEmailAddress, name, profileId, "Product you may like: " + productName, content);
				EmailSender.sendToSmtpServer(messageModel);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendRecommendationByPushMessage(String oneSignalPlayerId, String productName, String productLink, String heading) {
		if(StringUtil.isNotEmpty(oneSignalPlayerId)) {
			if(oneSignalPlayerId.length() > 5) {
				PushNotificationSender.notifyUser(oneSignalPlayerId, heading, productName, productLink );
			}
		}
	}
	
	// activation logic

	

	static ProcessedContent processContent(AssetTemplate tpl, Map<String, Object> context) {
		try {
			Handlebars handlebars = new Handlebars();
			Template titleTemplate = handlebars.compileInline(tpl.getTitle());
			Template contentTemplate = handlebars.compileInline(tpl.getMediaInfo());
			String title = titleTemplate.apply(context);
			String body = contentTemplate.apply(context);
			ProcessedContent content = new ProcessedContent(title, body);
			return content;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ProcessedContent("", "");
	}
	
	
	public static boolean recommendProductItems(Profile profile, AssetTemplate assetTemplate, List<ProductItem> productItems) {
		Map<String, Object> context = new HashMap<>();
		// TODO
		
		ProductItem item = productItems.get(0);
		String campaignId= item.getInCampaigns().iterator().next();
		String itemId = item.getId();
		String refProfileId = profile.getId();
		String landingPageUrl = item.getFullUrl();
		String landingPageName = item.getTitle();
		String imageUrl = item.getHeadlineImageUrl();
		String videoUrl = item.getHeadlineVideoUrl();
		
		String activationEventName = "email-click";
		String eventMetaDataId = EventMetricManagement.getEventMetricByName(activationEventName).getId();
		TargetMediaUnit targetMediaUnit = new TargetMediaUnit(campaignId, itemId, imageUrl, videoUrl, refProfileId, landingPageUrl, landingPageName, eventMetaDataId);
		TargetMediaUnitDaoUtil.save(targetMediaUnit);
		
		context.put("targetMediaUnit", targetMediaUnit);
		context.put("profile", profile);
		context.put("productItems", productItems);
		ProcessedContent c =  processContent(assetTemplate, context);
		System.out.println(c.getContent());
		return ! c.isEmpty();
	}
	
	public static boolean recommendContentItems(Profile profile, AssetTemplate assetTemplate, List<AssetContent> contentItems) {
		Map<String, Object> context = new HashMap<>();
		// TODO
		context.put("profile", profile);
		context.put("contentItems", contentItems);
		ProcessedContent c =  processContent(assetTemplate, context);
		System.out.println(c.getContent());
		return ! c.isEmpty();
	}
}
