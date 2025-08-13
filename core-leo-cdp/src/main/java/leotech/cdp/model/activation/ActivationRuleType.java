package leotech.cdp.model.activation;

/**
 * Activation Rule Type
 * 
 * @author tantrieuf31 
 * @since 2023
 */
public class ActivationRuleType {

	// FREE version can run data service only
	public static final String RUN_DATA_SERVICE = "run-data-service";
	public static final String AFFILIATE_SHARE_LINK = "affiliate-share-link";

	// PAID version has email marketing rule by using campaign
	public static final String THANKS_REGISTRATION_EMAIL = "thanks-registration-email";
	public static final String NEWSLETTER_EMAIL = "newsletter-email";
	public static final String PRODUCT_RECOMMENDATION_EMAIL = "product-recommendation-email";
	public static final String CONTENT_RECOMMENDATION_EMAIL = "content-recommendation-email";
	public static final String ABANDONED_CART_EMAIL = "abandoned-cart-email";
	public static final String BIRTHDAY_EMAIL = "birthday-email";
	public static final String LIVESTREAM_EVENT_EMAIL = "livestream-event-email";
	public static final String COUPON_EMAIL = "coupon-email";

	// PAID version has web notification rules by using campaign
	public static final String ABANDONED_CART_WEB_PUSH = "abandoned-cart-web-push";
	public static final String FLASH_SALES_WEB_PUSH = "flash-sales-web-push";
	public static final String NEWSLETTER_WEB_PUSH = "newsletter-web-push";
	public static final String LIVESTREAM_EVENT_WEB_PUSH = "livestream-event-web-push";

	// PAID version has app notification rules by using campaign
	public static final String ABANDONED_CART_APP_PUSH = "abandoned-cart-app-push";
	public static final String FLASH_SALES_APP_PUSH = "flash-sales-app-push";
	public static final String NEWSLETTER_APP_PUSH = "newsletter-app-push";
	public static final String LIVESTREAM_EVENT_APP_PUSH = "livestream-event-app-push";
}
