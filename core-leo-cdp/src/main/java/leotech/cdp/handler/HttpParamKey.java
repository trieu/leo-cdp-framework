package leotech.cdp.handler;

/**
 * HTTP parameter key
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class HttpParamKey {
	
	public static final String SOURCE = "source";
	public static final String ACCESS_TOKEN_KEY = "tokenkey";
	public static final String ACCESS_TOKEN_VALUE = "tokenvalue";
	
    public static final String CTX_SESSION_KEY = "ctxsk";
    public static final String FINGERPRINT_ID = "fgp";
    
    public static final String PROFILE_ID = "pfid";
    public static final String VISITOR_ID = "visid";
    
    // Identity Information
    public static final String CRM_REF_ID = "crmRefId";
    public static final String GOVERNMENT_ISSUED_IDS = "governmentIssuedIDs";
    public static final String APPLICATION_IDS = "applicationIDs";
    public static final String LOYALTY_IDS = "loyaltyIDs";
    public static final String FINTECH_SYSTEM_IDS = "fintechSystemIDs";
    
    public static final String UPDATE_BY_KEY = "updateByKey";
    public static final String DEDUPLICATE = "deduplicate";
    public static final String OVERWRITE_DATA = "overwriteData";
    
    
    public static final String SOURCE_IP = "sourceip";
    public static final String USER_AGENT = "useragent";
    
    // Contact Information
    public static final String PRIMARY_EMAIL = "primaryEmail";
    public static final String PRIMARY_PHONE = "primaryPhone";
    public static final String SOCIAL_MEDIA_PROFILES = "socialMediaProfiles";
    public static final String BUSINESS_CONTACTS = "businessContacts";
    
    
    public static final String FUNNEL_STAGE = "funnelStage";
    
    // core IDs or profile
    public static final String TARGET_UPDATE_EMAIL = "targetUpdateEmail";
    public static final String TARGET_UPDATE_PHONE = "targetUpdatePhone";
    public static final String TARGET_UPDATE_CRMID = "targetUpdateCrmId";
    
    public static final String TARGET_UPDATE_APPLICATION_ID = "targetUpdateaApplicationID";
    public static final String TARGET_UPDATE_SOCIAL_MEDIA_ID = "targetUpdateaSocialMediaID";
    public static final String TARGET_UPDATE_GOVERNMENT_ISSUED_ID = "targetUpdateaGovernmentIssuedID";
    public static final String TARGET_UPDATE_BANKING_SYSTEM_ID = "targetUpdateaBankingSystemID";
    
    public static final String DATA_LABELS = "dataLabels";
    
    public static final String CREATED_AT = "createdat";
    public static final String UPDATED_AT = "updatedat";
    
    public static final String GENDER = "gender";
    public static final String AGE = "age";
    public static final String DATE_OF_BIRTH = "dateOfBirth";
    
    public static final String NAME = "name";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    
    public static final String LOGIN_ID = "loginid";
    public static final String LOGIN_PROVIDER = "loginprovider";
    public static final String PROFILE_DATA = "profiledata";
    public static final String EXT_ID_DATA = "extiddata";
   
    public static final String FORM_NAME = "form";

    public static final String CONTENT_ID = "ctid";
    public static final String TEMPLATE_ID = "tplid";

    public static final String JOURNEY_MAP_ID = "journeymapid";
    public static final String TARGET_MEDIA_ID = "tgmid";
    public static final String OBSERVER_ID = "obsid";
    public static final String TOUCHPOINT_HUB_ID = "tphubid";
    public static final String TOUCHPOINT_NAME = "tpname";
    public static final String TOUCHPOINT_TYPE = "tptype";
    public static final String TOUCHPOINT_URL = "tpurl";
   
    public static final String TOUCHPOINT_REFERRER_URL = "tprefurl";
    public static final String TOUCHPOINT_REFERRER_DOMAIN = "tprefdomain";
    
    
    public static final String EVENT_TIME = "eventtime";
    public static final String IMAGE_URLS = "imageUrls";
    public static final String VIDEO_URLS = "videoUrls";
    
    public static final String APP_ID = "appid";
    public static final String MEDIA_HOST = "mediahost";
    public static final String USER_DEVICE_ID = "udeviceid";
    
    public static final String SRC_EVENT_KEY = "srceventk";
    public static final String EVENT_METRIC_NAME = "metric";
    public static final String EVENT_VALUE = "ev";
    public static final String EVENT_DATA = "eventdata";
    public static final String EVENT_RAW_JSON_DATA = "rawJsonData";
    public static final String EVENT_JSON_DATA = "jsonData";
    
    public static final String SHOPPING_CART_ITEMS = "scitems";
    public static final String SHOPPING_CART_ITEMS_FULL = "shoppingitems";
    public static final String SERVICE_ITEMS = "svitems";
    public static final String TRADING_ITEMS = "tditems";
    
    public static final String FEEDBACK_TEXT = "feedback";
    public static final String MESSAGE_TEXT = "message";
   
    public static final String TRANSACTION_ID = "tsid";
    public static final String TRANSACTION_SHIPPING_INFO = "tsshippinginfo";
    public static final String TRANSACTION_TAX = "tstax";
    public static final String TRANSACTION_SHIPPING_VALUE = "tsshippingvalue";
    public static final String TRANSACTION_STATUS = "tsstatus";
    public static final String TRANSACTION_VALUE = "tsval";
    public static final String TRANSACTION_CURRENCY = "tscur";
    public static final String TRANSACTION_DISCOUNT = "tsdiscount";
    public static final String TRANSACTION_PAYMENT = "tspayment";
    public static final String EXT_DATA = "extData";
    
    public static final String START_INDEX = "sidx";
    public static final String NUMBER_RESULT = "nrs";
    
    
    public static final String DEV_ENV = "dev";
    public static final String PRO_ENV = "pro";
    public static final String QC_ENV = "qc";
    public static final String DATA_ENVIRONMENT = "envi";
	public static final String RATING_SCORE = "ratingscore";
	
	
}
