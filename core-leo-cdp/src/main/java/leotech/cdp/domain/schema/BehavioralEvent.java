package leotech.cdp.domain.schema;

/**
 * Behavioral Event Types
 * 
 * Organize events by domain category for easier maintenance and expansion.
 *
 * @author tantrieuf31
 * @since 2020
 */
public final class BehavioralEvent {

	/*
	 * GENERAL EXPERIENCE EVENTS
	 * ============================================================
	 */
	public static final class General {
		public static final String JOIN_COMMUNITY = "join-community";
		public static final String JOIN_WEBINAR = "join-webinar";
		public static final String JOIN_WORKSHOP = "join-workshop";

		public static final String SMS_CLICK = "sms-click";
		public static final String NOTIFICATION_CLICK = "notification-click";

		public static final String DATA_IMPORT = "data-import";
		public static final String AD_IMPRESSION = "ad-impression";

		public static final String PAGE_VIEW = "page-view";
		public static final String ADMIN_VIEW = "admin-view";

		public static final String ACCEPT_TRACKING = "accept-tracking";
		public static final String ENGAGED_SESSION = "engaged-session";

		public static final String SEARCH = "search";
		public static final String CONTENT_VIEW = "content-view";
		public static final String ITEM_VIEW = "item-view";

		public static final String SHORT_LINK_CLICK = "short-link-click";
		public static final String QR_CODE_SCAN = "qr-code-scan";
		public static final String PLAY_VIDEO = "play-video";
		public static final String VIDEO_VIEW = "video-view";

		public static final String USER_SEND_TEXT = "user-send-text";
		public static final String USER_LOGIN = "user-login";

		public static final String EMAIL_CLICK = "email-click";
		public static final String SOCIAL_SHARING = "social-sharing";

		public static final String FILE_DOWNLOAD = "file-download";

		public static final String FOLLOW = "follow";
		public static final String UNFOLLOW = "unfollow";

		public static final String LIKE = "like";
		public static final String UNLIKE = "unlike";
		public static final String RECOMMEND = "recommend";

		public static final String ADD_WISHLIST = "add-wishlist";

		public static final String ASK_QUESTION = "ask-question";
		public static final String SUBMIT_CONTACT = "submit-contact";

		public static final String PRODUCT_TRIAL = "product-trial";
	}

	/*
	 * EDUCATION & LEARNING EVENTS
	 * ============================================================
	 */
	public static final class Education {
		public static final String COURSE_VIEW = "course-view";
		public static final String ENROLL = "enroll";

		public static final String ATTENDED_WORKSHOP = "attended-workshop";
		public static final String ATTENDED_CLASS = "attended-class";

		public static final String COURSE_FEEDBACK = "course-feedback";
		public static final String COURSE_REVIEW = "course-review";

		// New suggested education metrics
		public static final String START_LESSON = "start-lesson";
		public static final String COMPLETE_LESSON = "complete-lesson";
		public static final String SUBMIT_ASSIGNMENT = "submit-assignment";
		public static final String PASS_EXAM = "pass-exam";
		public static final String FAIL_EXAM = "fail-exam";
		public static final String REQUEST_TUTOR_SUPPORT = "request-tutor-support";
	}

	/*
	 * E-COMMERCE & PURCHASE EVENTS
	 * ============================================================
	 */
	public static final class Commerce {
		public static final String ADD_TO_CART = "add-to-cart";
		public static final String REMOVE_FROM_CART = "remove-from-cart";

		public static final String PURCHASE = "purchase";
		public static final String FIRST_PURCHASE = "first-purchase";
		public static final String REPURCHASE = "repurchase";

		public static final String PURCHASE_INTENT = "purchase-intent";
		public static final String ORDER_CHECKOUT = "order-checkout";
		public static final String MADE_PAYMENT = "made-payment";

		public static final String FIRST_SUBSCRIBE = "first-subscribe";
		public static final String SUBSCRIBE = "subscribe";
		public static final String REPEAT_SUBSCRIBE = "repeat-subscribe";

		public static final String REGISTER_ACCOUNT = "register-account";
	}

	/*
	 * CUSTOMER SUPPORT / FEEDBACK
	 * ============================================================
	 */
	public static final class Feedback {
		public static final String SUBMIT_FEEDBACK_FORM = "submit-feedback-form";
		public static final String SUBMIT_LEAD_FORM = "submit-lead-form";
		public static final String SUBMIT_COMMENT_FORM = "submit-comment-form";

		public static final String CHAT_FOR_SUPPORT = "chat-for-support";

		public static final String SUBMIT_RATING_FORM = "submit-rating-form";
		public static final String SUBMIT_CES_FORM = "submit-ces-form";
		public static final String SUBMIT_CSAT_FORM = "submit-csat-form";
		public static final String SUBMIT_NPS_FORM = "submit-nps-form";

		public static final String PRODUCT_FEEDBACK = "product-feedback";
		public static final String PRODUCT_REVIEW = "product-review";

		public static final String SERVICE_FEEDBACK = "service-feedback";
		public static final String SERVICE_REVIEW = "service-review";

		public static final String EVENT_FEEDBACK = "event-feedback";

		public static final String NEGATIVE_FEEDBACK = "negative-feedback";
		public static final String POSITIVE_FEEDBACK = "positive-feedback";
	}

	/*
	 * FINANCE / BANKING / LOAN EVENTS
	 * ============================================================
	 */
	public static final class Finance {
		public static final String APPLY_LOAN = "apply-loan";
		public static final String APPROVE_LOAN = "approve-loan";

		// New finance metrics
		public static final String LOAN_REPAYMENT = "loan-repayment";
		public static final String CREDIT_SCORE_CHECK = "credit-score-check";
		public static final String OPEN_BANK_ACCOUNT = "open-bank-account";
		public static final String TRANSFER_MONEY = "transfer-money";
		public static final String PAY_BILL = "pay-bill";
	}

	/*
	 * STOCK TRADING EVENTS
	 * ============================================================
	 */
	public static final class StockTrading {
		public static final String VIEW_STOCK = "view-stock";
		public static final String BUY_STOCK = "buy-stock";
		public static final String SELL_STOCK = "sell-stock";

		public static final String ADD_TO_WATCHLIST = "add-stock-watchlist";
		public static final String REMOVE_FROM_WATCHLIST = "remove-stock-watchlist";

		public static final String VIEW_PORTFOLIO = "view-portfolio";
		public static final String EXECUTE_TRADE = "execute-trade";
	}

	/*
	 * TRAVEL EVENTS ============================================================
	 */
	public static final class Travel {
		public static final String BOOKING = "booking";
		public static final String CHECK_IN = "check-in";
		public static final String CHECK_OUT = "check-out";

		// New travel metrics
		public static final String SEARCH_FLIGHT = "search-flight";
		public static final String SEARCH_HOTEL = "search-hotel";
		public static final String VIEW_DESTINATION = "view-destination";
		public static final String CANCEL_BOOKING = "cancel-booking";
		public static final String ADD_TRAVEL_WISHLIST = "add-travel-wishlist";
	}

	/*
	 * REAL ESTATE EVENTS
	 * ============================================================
	 */
	public static final class RealEstate {
		public static final String VIEW_PROPERTY = "view-property";
		public static final String REQUEST_TOUR = "request-property-tour";
		public static final String SCHEDULE_TOUR = "schedule-property-tour";

		public static final String SUBMIT_MORTGAGE_FORM = "submit-mortgage-form";
		public static final String PROPERTY_FAVORITE = "property-favorite";
		public static final String CONTACT_AGENT = "contact-agent";

		public static final String OFFER_SUBMISSION = "submit-property-offer";
		public static final String MORTGAGE_PRE_APPROVAL = "mortgage-pre-approval";
	}

	/*
	 * SERVICE INDUSTRY EVENTS (Spa, Massage, Hospitality)
	 * ============================================================
	 */
	public static final class ServiceIndustry {

		/* ---------------------- SPA & MASSAGE ---------------------- */
		public static final String SPA_BOOKING = "spa-booking";
		public static final String SPA_RESCHEDULE = "spa-reschedule";
		public static final String SPA_CANCEL = "spa-cancel";

		public static final String MASSAGE_SESSION_START = "massage-session-start";
		public static final String MASSAGE_SESSION_END = "massage-session-end";

		public static final String SELECT_THERAPIST = "select-therapist";
		public static final String RATE_THERAPIST = "rate-therapist";

		public static final String BUY_SPA_PACKAGE = "buy-spa-package";
		public static final String SPA_MEMBERSHIP_SUBSCRIBE = "spa-membership-subscribe";
		public static final String SPA_MEMBERSHIP_RENEW = "spa-membership-renew";
		public static final String SPA_MEMBERSHIP_CANCEL = "spa-membership-cancel";

		/* ---------------------- HOSPITALITY ------------------------ */
		public static final String HOTEL_ROOM_VIEW = "hotel-room-view";
		public static final String HOTEL_BOOKING = "hotel-booking";
		public static final String HOTEL_CANCEL_BOOKING = "hotel-cancel-booking";

		public static final String HOTEL_CHECK_IN = "hotel-check-in";
		public static final String HOTEL_CHECK_OUT = "hotel-check-out";

		public static final String REQUEST_ROOM_SERVICE = "request-room-service";
		public static final String REQUEST_HOUSEKEEPING = "request-housekeeping";
		public static final String REQUEST_AMENITY = "request-amenity";

		public static final String HOTEL_FEEDBACK = "hotel-feedback";
		public static final String HOTEL_STAY_REVIEW = "hotel-stay-review";

		/* ---------------------- SERVICES (GENERIC) ------------------ */
		public static final String SERVICE_BOOKING = "service-booking";
		public static final String SERVICE_CANCEL = "service-cancel";
		public static final String SERVICE_RESCHEDULE = "service-reschedule";
		public static final String SERVICE_CHECK_IN = "service-check-in";
		public static final String SERVICE_CHECK_OUT = "service-check-out";

		public static final String SERVICE_FEEDBACK = "service-feedback";
		public static final String SERVICE_REVIEW = "service-review";
	}

	/*
	 * B2B MARKETING ============================================================
	 */
	public static final class B2BMarketing {
		/* ---------------------- B2B MARKETING ----------------------- */
		public static final String B2B_LEAD_CAPTURE = "b2b-lead-capture";
		public static final String B2B_FORM_SUBMIT = "b2b-form-submit";
		public static final String B2B_REQUEST_DEMO = "b2b-request-demo";
		public static final String B2B_REQUEST_PROPOSAL = "b2b-request-proposal";

		public static final String B2B_OPEN_DECK = "b2b-open-deck";
		public static final String B2B_DOWNLOAD_CASE_STUDY = "b2b-download-case-study";
		public static final String B2B_DOWNLOAD_WHITEPAPER = "b2b-download-whitepaper";

		public static final String B2B_ATTEND_WEBINAR = "b2b-attend-webinar";
		public static final String B2B_MEETING_BOOKED = "b2b-meeting-booked";
		public static final String B2B_MEETING_COMPLETED = "b2b-meeting-completed";

		public static final String B2B_PIPELINE_STAGE_UPDATE = "b2b-pipeline-stage-update";
		public static final String B2B_INTENT_SIGNAL = "b2b-intent-signal";
	}
}
