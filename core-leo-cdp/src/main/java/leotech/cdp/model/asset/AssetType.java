package leotech.cdp.model.asset;

/**
 * Digital Asset Type for Marketing Data Hub
 * 
 * @author tantrieuf31
 * @since Sep 19, 2020
 *
 */
public class AssetType {

	// default value for any unclassified asset 
	public static final int UNCLASSIFIED = 0;
	
	// content for inbound marketing in owned media channel
	public static final int CONTENT_ITEM_CATALOG = 1;
	
	// product 
	public static final int PRODUCT_ITEM_CATALOG = 2;
	
	// Commercial Voucher: Similar to a coupon, gift certificate , Businesses may use vouchers for promotions or as a form of credit.
	public static final int SERVICE_ITEM_CATALOG = 3;
	
	// for outbound marketing in shared and earned media channer
	public static final int EMAIL_CONTENT = 4;
	
	// facebook, instagram
	public static final int SOCIAL_MEDIA_CONTENT = 5;
	
	// SMS / Push message notification
	public static final int TEXT_MESSAGE_CONTENT = 6;
	
	// interactive content for customer engagement in owned media channel
	public static final int WEB_HTML_CONTENT = 7;
	
	// Gamification creatives and mulmedia
	public static final int VIDEO_CATALOG = 8;
	
	// landing page of touchpoint
	public static final int HTML_LANDING_PAGE = 9;
	
	// display banner for PC and mobile, e.g: remarketing in Affiliate marketing channels
	public static final int AD_BANNER = 10;
	
	// VAST video ads for PC and mobile in Affiliate marketing channels
	public static final int AD_VIDEO = 11;
	
	// social event for PR and mass media
	public static final int SOCIAL_EVENT = 12;
	
	// feedback form to collect CX data
	public static final int FEEDBACK_FORM = 13;
	
	// game for lead generation and user engagement
	public static final int GAMIFICATION = 14;
	
	// short URL link for lead generation 
	public static final int SHORT_URL_LINK = 15;
	
	// HTML PRESENTATION using revealjs and Markdown
	public static final int PRESENTATION_ITEM_CATALOG = 16;
	
	// HTML Web FORM
	public static final int WEB_FORM = 17;
	
	// Trading Item
	public static final int TRADING_ITEM_CATALOG = 18;
}
