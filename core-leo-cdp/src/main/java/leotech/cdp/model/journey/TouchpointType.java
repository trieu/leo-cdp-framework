package leotech.cdp.model.journey;

import java.util.Map;

import leotech.system.util.ReflectionUtil;

/**
 * TouchpointType constants and utility mapping for the CDP journey map. <br>
 * This class defines a comprehensive set of touchpoint types across various categories. <br>
 * Expanded to cover comprehensive Omni-channel, B2B, B2C, E-commerce, and Next-Gen AI touchpoints.
 * 
 * @author tantrieuf31
 * @since 2020
 */
public final class TouchpointType {
	
	public static final int CDP_API = 0;
	
	// ========================================================================
	// 1. DIGITAL HUBS & OWNED MEDIA
	// ========================================================================
	public static final int SEARCH_ENGINE = 1;
	public static final int WEB_APP = 2;
	public static final int WEBSITE = 3;
	public static final int MOBILE_APP = 4;
	public static final int AR_VR_APP = 5;
	public static final int IOT_APP = 6;
	public static final int CHATBOT = 7;
	public static final int VIDEO_CHANNEL = 8;
	public static final int SOCIAL_MEDIA = 9;
	public static final int WEB_PORTAL = 10;
	public static final int KNOWLEDGE_HUB = 11;
	public static final int DIGITAL_DOCUMENT = 33; // E.g., Public PDFs, Whitepapers accessed via Short-URLs
	public static final int COMMUNITY_FORUM = 60;  // Discord, Reddit, Proprietary Forums
	public static final int LOYALTY_PORTAL = 61;   // Dedicated portals for points/rewards management

	// ========================================================================
	// 2. PHYSICAL HUBS & REAL-WORLD LOCATIONS
	// ========================================================================
	public static final int RETAIL_STORE = 12;
	public static final int SHOPPING_TV = 13;
	public static final int SHOPPING_MALL = 14;
	public static final int COFFEE_SHOP = 15;
	public static final int CONFERENCE_HALL = 16;
	public static final int URBAN_PARK = 17;
	public static final int OFFICE_BUILDING = 18;
	public static final int EXPERIENCE_SPACE = 19;
	public static final int PR_EVENT_SPACE = 20;
	public static final int BILLBOARD_OUTDOOR = 21;
	public static final int BILLBOARD_INDOOR = 22;
	public static final int COMMUTER_STORE = 23;
	public static final int SPORTING_EVENT = 24;	
	public static final int COMMUNITY_SPACE = 25;
	public static final int SCHOOL_SPACE = 26;
	public static final int TRANSIT_STATION = 55;  // Airports, Train Stations, Bus Stops
	
	// ========================================================================
	// 3. DIRECT MESSAGING & COMMUNICATION (CRITICAL FOR CDP)
	// ========================================================================
	public static final int EMAIL_CAMPAIGN = 35;
	public static final int SMS_MESSAGE = 36;
	public static final int PUSH_NOTIFICATION = 37; // Mobile App or Web Push
	public static final int OTT_MESSAGING_APP = 38; // WhatsApp, Zalo, WeChat, FB Messenger
	public static final int VOICE_CALL_TELEMARKETING = 39; 

	// ========================================================================
	// 4. ADVERTISING & PAID MEDIA
	// ========================================================================
	public static final int DISPLAY_AD = 40;       // Banner ads, Programmatic
	public static final int SEARCH_AD = 41;        // Google Ads, Bing Ads (PPC)
	public static final int SOCIAL_AD = 42;        // Facebook Ads, TikTok Ads, LinkedIn Ads
	public static final int CTV_OTT_AD = 43;       // Connected TV (Netflix, Hulu, Smart TVs)
	public static final int AUDIO_PODCAST_AD = 44; // Spotify, Apple Podcasts

	// ========================================================================
	// 5. E-COMMERCE, SALES TECH & FINANCE
	// ========================================================================
	public static final int ONLINE_MARKETPLACE = 45; // Amazon, Shopee, Lazada
	public static final int AFFILIATE_NETWORK = 46;  // Referral links, Affiliate blogs
	public static final int POS_TERMINAL = 47;       // Point of Sale systems syncing offline purchases
	public static final int SELF_SERVICE_KIOSK = 48; // McDonald's Kiosks, Airport Check-in
	public static final int PAYMENT_GATEWAY = 49;    // Stripe, PayPal, VNPay checkout screens
	public static final int ATM_MACHINE = 56;        // Banking physical touchpoints

	// ========================================================================
	// 6. B2B & ENTERPRISE
	// ========================================================================
	public static final int WEBINAR = 50;            // Zoom, Google Meet events
	public static final int DIRECT_MAIL = 51;        // Physical post/letters sent to business or home
	public static final int PARTNER_PORTAL = 52;     // B2B Reseller / Distributor dashboards

	// ========================================================================
	// 7. HEALTHCARE, AUTOMOTIVE & WEARABLES
	// ========================================================================
	public static final int WEARABLE_DEVICE = 57;    // Apple Watch, Fitbit health sync
	public static final int TELEHEALTH_APP = 58;     // Virtual doctor visits
	public static final int IN_VEHICLE_SYSTEM = 59;  // Apple CarPlay, Android Auto, Tesla UI

	// ========================================================================
	// 8. NEXT-GEN, AI & METAVERSE
	// ========================================================================
	public static final int VOICE_ASSISTANT = 62;    // Amazon Alexa, Google Home, Siri
	public static final int GEN_AI_INTERFACE = 63;   // ChatGPT Plugins, Custom LLM prompt interfaces
	public static final int METAVERSE_SPACE = 64;    // Roblox, Sandbox, Spatial.io virtual activations

	// ========================================================================
	// 9. PEOPLE & CX (CUSTOMER EXPERIENCE)
	// ========================================================================
	/**
	 * An influencer is an individual who has the power to affect purchase decisions.
	 * Key Influencers are personalities or businesses that already have the attention of your target audience. 
	 */
	public static final int KEY_INFLUENCER = 27;
	public static final int CUSTOMER_SERVICE = 28;   // Zendesk, Freshdesk, Call Center agents
	public static final int FEEDBACK_SURVEY = 29;    // Typeform, SurveyMonkey, NPS scores

	// ========================================================================
	// 10. SYSTEM DATA SOURCES & INTEGRATIONS
	// ========================================================================
	public static final int DATA_OBSERVER = 30;
	public static final int REDIS_DATA_SOURCE = 31;
	public static final int KAFKA_DATA_SOURCE = 32;
	public static final int CRM_DATABASE = 34;       // Salesforce, HubSpot inbound syncs


	// -----------------------------------------------------------------------------------
	// CACHED REFLECTION MAPS (IMMUTABLE & THREAD-SAFE)
	// -----------------------------------------------------------------------------------
	
	private static final Map<Integer, String> VALUE_TO_NAME_MAP;
	private static final Map<String, Integer> NAME_TO_VALUE_MAP;

	static {
		// Java 10+ Map.copyOf() ensures strict Immutability for thread-safe global reads.
		// ReflectionUtil will automatically pick up all 64+ constants defined above.
		VALUE_TO_NAME_MAP = Map.copyOf(ReflectionUtil.getConstantMap(TouchpointType.class));
		NAME_TO_VALUE_MAP = Map.copyOf(ReflectionUtil.getConstantIntegerMap(TouchpointType.class));
	}
	
	/**
	 * Private constructor to prevent accidental instantiation of utility class.
	 */
	private TouchpointType() {
		throw new UnsupportedOperationException("Utility classes cannot be instantiated.");
	}
	
	/**
	 * Returns an unmodifiable map of touchpoint values to their string names.
	 * 
	 * @return Map<Integer, String>
	 */
	public static Map<Integer, String> getMapValueToName() {
		return VALUE_TO_NAME_MAP;
	}
	
	/**
	 * Safely retrieves the string name of the touchpoint type.
	 * Useful for AI tagging and UI display.
	 * 
	 * @param type the integer Touchpoint type
	 * @return String name or "NO_DATA" if not found
	 */
	public static String getTypeAsText(int type) {
		return VALUE_TO_NAME_MAP.getOrDefault(type, "NO_DATA");
	}
	
	/**
	 * Safely retrieves the integer value of the touchpoint type string.
	 * Allows an AI NLP pipeline to map string predictions back to database integers.
	 * 
	 * @param type the string Touchpoint name (e.g., "EMAIL_CAMPAIGN")
	 * @return int value or 0 if not found
	 */
	public static int getTypeAsInt(String type) {
		return NAME_TO_VALUE_MAP.getOrDefault(type, 0);
	}
}