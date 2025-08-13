package leotech.cdp.model.marketing;

/**
 * Default Model of CDP, using AI and Marketing Automation to build next best actions for each profile <br>
 * The final purpose of this model is to help customer get the best experience at every Journey Stage
 * 
 * @author tantrieuf31
 *
 */
public final class NextBestAction {
	
	// ecommerce
	public static final String ADD_ITEM_TO_CART = "add-item-to-cart";
	public static final String CHECKOUT_ITEMS_IN_CART = "checkout-items-in-cart";
	public static final String BUY_SOME_ITEMS = "buy-some-items";
	public static final String GIVE_FEEDBACK = "give-feedback";
	public static final String SUBSCRIBE_A_SERVICE = "subscribe-a-service";

	// digital media
	public static final String READ_CONTENTS = "read-contents";
	public static final String WATCH_A_VIDEO = "watch-a-video";
	public static final String BROWSING_RALATED_ITEMS = "browsing-related-items";
	
	// reality experience
	public static final String TAKE_A_TRIP = "take-a-trip";
	public static final String CHECKIN_LOCATION = "checkin-location";
	
	// social community
	public static final String PLAY_A_GAME = "play-a-game";
	public static final String TAKE_A_COURSE = "take-a-course";
	public static final String READ_A_BOOK = "read-a-book";
	public static final String JOIN_A_COMMUNITY = "join-a-community";
	
}
