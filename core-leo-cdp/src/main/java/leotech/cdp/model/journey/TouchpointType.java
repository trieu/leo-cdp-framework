package leotech.cdp.model.journey;

import java.util.HashMap;
import java.util.Map;

import leotech.system.util.ReflectionUtil;

/**
 * @author tantrieuf31
 * @since 2020
 */
public final class TouchpointType {
	
	final static Map<Integer, String> mapValueToName = new HashMap<>(50);
	final static Map<String, Integer> mapNameToValue = new HashMap<>(50);
	
	public static final int CDP_API = 0;
	
	// digital hub type
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

	// physical hub type
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
 
	/**
	 * An influencer is an individual who has the power to affect purchase decisions of others because of his/her authority <br>
	 * Key Influencer is people, personalities, or businesses that already have the attention of your target audience. 
	 */
	public static final int KEY_INFLUENCER = 27;
	
	// CX touchpoint type
	public static final int CUSTOMER_SERVICE = 28;
	public static final int FEEDBACK_SURVEY = 29;
	
	// system type for data storage
	public static final int DATA_OBSERVER = 30;
	public static final int REDIS_DATA_SOURCE = 31;
	public static final int KAFKA_DATA_SOURCE = 32;
	
	// Digital document is public document in the Internet, that anyone can click, download by scan-qr-code or click on the short URL
	public static final int DIGITAL_DOCUMENT = 33;
	
	public static final int CRM_DATABASE = 34;

	static {
		mapValueToName.putAll(ReflectionUtil.getConstantMap(TouchpointType.class));
		mapNameToValue.putAll(ReflectionUtil.getConstantIntegerMap(TouchpointType.class));
	}
	
	public static Map<Integer, String> getMapValueToName() {
		return mapValueToName;
	}
	
	public static String getTypeAsText(int type) {
		return mapValueToName.getOrDefault(type, "NO_DATA");
	}
	
	public static int getTypeAsInt(String type) {
		return mapNameToValue.getOrDefault(type, 0);
	}
}