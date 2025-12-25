package leotech.cdp.model.customer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import com.arangodb.entity.Key;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import leotech.cdp.domain.DataFlowManagement;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.domain.schema.CustomerFunnel;
import leotech.cdp.model.AutoSetData;
import leotech.cdp.model.ExposeInSegmentList;
import leotech.cdp.model.ProfileMetaDataField;
import leotech.cdp.model.RefKey;
import leotech.cdp.model.analytics.FinanceEvent;
import leotech.cdp.model.analytics.GoogleUTM;
import leotech.cdp.model.analytics.OrderedItem;
import leotech.cdp.model.analytics.ScoreCX;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.journey.DataFlowStage;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.JourneyMapRefKey;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.utils.ProfileDataValidator;
import leotech.system.exception.InvalidDataException;
import leotech.system.util.database.PersistentObject;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * the abstract profile for B2C (a person profile) and B2B (a company profile)
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public abstract class AbstractProfile extends PersistentObject {
	
	private static final int MAXIMUM_TEXT_LENGTH = 50;
	public static final int PROFILE_NOTES_MAX_LENGTH = 3000;
	public static final String PROFILE_NOTES_NEW_LINE = "__";
	public static final int MAX_ENGAGED_TOUCHPOINT_SIZE = 1000;
	
	public static final String SSO_LOGIN = "sso-login";
	public static final String CDP_API = "cdp-api";
	public static final String CRM_IMPORT = "crm-import";
	public static final String CDP_INPUT = "cdp-input";	
	
	public static final int MERGED_BY_ID_KEYS = 11;
	
	public static final String UNKNOWN_PROFILE = "UNKNOWN PROFILE";
	public static final String CONTACT = "CONTACT";
	public static final String VISITOR = "VISITOR";
	
	
	@com.arangodb.velocypack.annotations.Expose(serialize = false, deserialize = false)
	String updateByKey = ""; // "": insert new, "primaryPhone": update using phone, "primaryEmail": update using email,..
	
	@com.arangodb.velocypack.annotations.Expose(serialize = false, deserialize = false)
	boolean deduplicate;
	
	@Key
	@Expose
	@ExposeInSegmentList
	protected String id;
	
	// the main ID after Identity Resolution processing
	@Expose
	protected String rootProfileId = "";

	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(identityResolutionKey = true, dataQualityScore=20, label="Contact CRM ID")
	@ExposeInSegmentList
	protected String crmRefId = "";

	@Expose
	protected Set<String> identities = new HashSet<>(100);
	protected String identitiesAsStr = "";
	
	@Expose
	@AutoSetData(setDataAsString = true)
	protected String schemaType = ProfileSchema.SCHEMA_TYPE_GENERAL;
	
	@Expose
	@AutoSetData(setDataAsInteger = true)
	@ExposeInSegmentList
	protected int type = ProfileType.ANONYMOUS_VISITOR;
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected String typeAsText;
	
	@Expose
	protected int mergeCode = 0;
	
	@Expose
	@ProfileMetaDataField(identityResolutionKey = true, dataQualityScore=10, label="Primary Email", synchronizable = true)
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected String primaryEmail = "";
	
	@Expose	
	@ProfileMetaDataField(identityResolutionKey = true, dataQualityScore=10, label="Secondary Emails")
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected Set<String> secondaryEmails = new HashSet<>(10);

	@Expose
	@ProfileMetaDataField(identityResolutionKey = true, dataQualityScore=10, label="Primary Phone", synchronizable = true)
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected String primaryPhone = "";
	
	@Expose	
	@ProfileMetaDataField(identityResolutionKey = true, dataQualityScore=10, label="Secondary Phones")
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected Set<String> secondaryPhones = new HashSet<>(10);
	
	/**
	 * Facebook, Google, Tiktok, Twitter,
	 */
	@Expose
	@AutoSetData(setDataAsJsonMap = true)
	@ProfileMetaDataField(identityResolutionKey = true, dataQualityScore=10, label="Social Media Profiles")
	@ExposeInSegmentList
	protected Map<String, String> socialMediaProfiles = new HashMap<>(30);
	
	@Expose
	@ExposeInSegmentList
	protected Date createdAt = new Date();

	@Expose
	@ExposeInSegmentList
	protected Date updatedAt = new Date();
	
	@Expose
	@ExposeInSegmentList
	protected Date updatedByCrmAt = new Date();

	@Expose
	@AutoSetData(setDataAsInteger = true)
	@ExposeInSegmentList
	protected int status = STATUS_ACTIVE;
	
	@Expose	
	@ProfileMetaDataField(dataQualityScore=2, label="Notes")
	@AutoSetData(setDataAsString = true)	
	protected String notes = "";
	
	// --- BEGIN taxonomy meta data
	
	@Expose
	@ProfileMetaDataField(dataQualityScore=1000,label="Data Verification")
	protected Boolean dataVerification = false;
	
	/**
	 * everyone should have a God to get information and grow the Self in the good path !?
	 */
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=20,label="Customer Persona URI")
	@ExposeInSegmentList
	protected String personaUri = "";
	
	/**
	 * for data classification
	 */
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Data Labels")
	@ExposeInSegmentList
	protected Set<String> dataLabels = new HashSet<>(100);
	protected String dataLabelsAsStr = "";// for text index and fast search
	
	
	/**
	 * In Campaigns <br>
	 * utm_campaign: Product, slogan, promo code, for example: spring_sale
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=5, label="In Campaigns")
	@ExposeInSegmentList
	protected Set<RefKey> inCampaigns = new HashSet<>(100);
	protected String inCampaignsAsStr = "";// for text index and fast search
	
	/**
	 * in Segments
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=5, label="In Segments")
	@ExposeInSegmentList
	protected Set<RefKey> inSegments = new HashSet<RefKey>(100);
	protected String inSegmentsAsStr = "";// for text index and fast search
	
	/**
	 * in B2B Accounts
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=5, label="In B2B Accounts")
	@ExposeInSegmentList
	protected Set<RefKey> inAccounts = new HashSet<>(100);
	protected String inAccountsAsStr = "";// for text index and fast search
	
	/**
	 * similar profiles
	 */
	@Expose
	protected Set<RefKey> similarProfiles = new HashSet<RefKey>(100);

	/**
	 * a profile can use multiple of product items and brands
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=5, label="In Journey Maps")
	protected Set<JourneyMapRefKey> inJourneyMaps = new HashSet<>();
	protected String inJourneyMapsAsStr = "";// for text index and fast search

		
	// END: TAXONAMY
	
	/**
	 * A list of login users who can view data, default only admin and super admin can view data
	 */
	@Expose
	protected Set<String> authorizedViewers = new HashSet<String>();
	
	/**
	 * A list of login users who can update data, default only admin and super admin can update data
	 */
	@Expose
	protected Set<String> authorizedEditors = new HashSet<String>();
		
	@Expose
	@ProfileMetaDataField(dataQualityScore=2, label="Last-seen IP")
	@ExposeInSegmentList
	protected String lastSeenIp = "";
	
	@Expose	
	@ProfileMetaDataField(dataQualityScore=1, label="Last Observer ID")
	protected String lastObserverId = "";

	@Expose
	@ProfileMetaDataField(dataQualityScore=1, label="Last Touchpoint ID")
	protected String lastTouchpointId = "";
	
	@Expose
	@ProfileMetaDataField(dataQualityScore=1, label="Last Touchpoint")
	@ExposeInSegmentList
	protected Touchpoint lastTouchpoint = null;
	
	@Expose
	protected Set<String> topEngagedTouchpointIds = new HashSet<String>(MAX_ENGAGED_TOUCHPOINT_SIZE);
	
	@Expose
	protected Set<String> fromTouchpointHubIds = new HashSet<String>(MAX_ENGAGED_TOUCHPOINT_SIZE);
	
	// --- END taxonomy meta data
	
	/// business
	
	/**
	 * business metadata
	 */
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=10, label="Business Data")
	protected Map<String, Set<String>> businessData = new HashMap<>();
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Business Industries")
	@ExposeInSegmentList
	protected Set<String> businessIndustries = new HashSet<>(50);


	// --- BEGIN Consent management, inputed by User Only
	
	// for web push and app push
	@Expose
	protected Map<String, Set<String>> notificationUserIds = new HashMap<>(20); 
	
	// receive = 0 is default value, 1 is agreed, -1 is rejected or blocked
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Receive WebPush")
	@ExposeInSegmentList
	protected int receiveWebPush = 0;
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Receive AppPush")
	@ExposeInSegmentList
	protected int receiveAppPush = 0;
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Receive Email")
	@ExposeInSegmentList
	protected int receiveEmail = 0;
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=5, label="Receive SMS")
	@ExposeInSegmentList
	protected int receiveSMS = 0;
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=2, label="Receive Ads")
	@ExposeInSegmentList
	protected int receiveAds = 1;
	
	@Expose
	@AutoSetData(setDataAsString = true)
	protected int recommendationModel = 0; // 0 = ranked by event score (default), 1 = ranked by IndexScore (marketer), 2 = automation by A.I model
	// --- END Consent management, inputed by User Only
	
	// --- BEGIN Marketing Data Model , inputed by Marketer
	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=20, label="Personal Problems")
	@ExposeInSegmentList
	protected Set<String> personalProblems = new HashSet<>(50);

	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=20, label="Solutions For Customer")
	@ExposeInSegmentList
	protected Set<String> solutionsForCustomer = new HashSet<>(50);
	
	/**
	 * copy data from utm_content <br>
	 * utm_content: Use to differentiate creatives or media channels. <br>
	 * For example, if you have two call-to-action links within the same email message, 
	 * you can use utm_content and set different values for each so you can tell which version is more effective.
	 */
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=10, label="Media Channels")
	@ExposeInSegmentList
	protected Set<String> mediaChannels = new HashSet<>(50);
	
	/**
	 * referrer media channels: google, youtube, facebook,...<br>
	 * utm_source: Referrer, for example: google, newsletter4, billboard
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=2, label="Referrer Channels")
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected Map<String, Integer> referrerChannels = new HashMap<>(50);
	
	/**
	 * subscribed media channels <br>
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=2, label="Subscribed Channels")
	@AutoSetData(setDataAsJsonMap = true)
	@ExposeInSegmentList
	protected Map<String, Integer> subscribedChannels = new HashMap<>(100);
	
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Google UTM Data")
	@AutoSetData(setDataAsString = true)
	protected Set<GoogleUTM> googleUtmData = new HashSet<>(100);

	/**
	 * utm_term
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Content Keywords")
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected Set<String> contentKeywords = new HashSet<>(100);
	
	@Expose
	@ProfileMetaDataField(dataQualityScore=20, label="Product Keywords")
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected Set<String> productKeywords = new HashSet<>(100);

	
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=30, label="Next Best Actions")
	protected Set<String> nextBestActions = new HashSet<>(100);
	
	// --- END Marketing Data Model
	
	// --- BEGIN Sales Data Model , inputed by Sales or CRM
	
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Sale Agencies")
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected Set<String> saleAgencies = new HashSet<>(10);
	
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Sale Agents")
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected Set<String> saleAgents = new HashSet<>(20);
	
	
	// --- BEGIN Finance
	
	/**
	 * e-commerce / retail / transaction-based tracking / 
	 */
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=42, label="Purchased Brands")
	@ExposeInSegmentList
	protected Set<String> purchasedBrands = new HashSet<>(1000);
	
	
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Shopping Cart Items")
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected Set<OrderedItem> shoppingItems = new HashSet<>(1000);
	protected Set<String> shoppingItemIds = new HashSet<>(1000);
	
	/**
	 * e-commerce / retail / transaction-based tracking
	 */
	@Expose
	@AutoSetData(setDataAsString = true)
	@ProfileMetaDataField(dataQualityScore=20, label="Purchased Items")
	@ExposeInSegmentList
	protected Set<OrderedItem> purchasedItems = new HashSet<>(10000);
	protected Set<String> purchasedItemIds = new HashSet<>(10000);
	
	/**
	 * finance / retail banking
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=20, label="Finance Records")
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected Set<FinanceRecord> financeRecords = new HashSet<>(100);
	
	/**
	 * for retail banking or micro-finance
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=50, label="Finance Credit Events")
	@AutoSetData(setDataAsString = true)
	@ExposeInSegmentList
	protected Set<FinanceCreditEvent> financeCreditEvents = new HashSet<>(100);
	
	// --- END Finance
	
	/**
	 * extra attributes
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Extra Attributes")
	@AutoSetData(setDataAsJsonMap = true)
	protected Map<String, Object> extAttributes = new HashMap<>(200);
	
	
	// --- BEGIN Quantitative Data Metrics
	/**
	 * 1) Journey Score: measure how far a profile goes in journey map
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=1, label="Journey Score")
	protected int journeyScore = 0;

	/**
	 * 2) Data Quality Score: measure how much the quality of profile data, has first name, last name, email and 
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=1, label="Data Quality Score")
	@ExposeInSegmentList
	@AutoSetData(setDataAsInteger = true)
	protected int dataQualityScore = 0; //
	
	@Expose
	protected Map<String, Integer> timeseriesDataQualityScore = new HashMap<>(); 

	// 
	/**
	 * 3) Lead scoring  
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Total Lead Score")
	@ExposeInSegmentList
	@AutoSetData(setDataAsInteger = true)
	protected int totalLeadScore = 0;
	
	@Expose
	protected Map<String, Integer> timeseriesLeadScore = new HashMap<>(); 
	
	
	/**
	 * 4) Prospect scoring  
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Total Prospect Score")
	@ExposeInSegmentList
	@AutoSetData(setDataAsInteger = true)
	protected int totalProspectScore = 0;
	
	@Expose
	protected Map<String, Integer> timeseriesProspectScore = new HashMap<>(); 
	
	
	/**
	 * 5) Engagement score from engagement events
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=15, label="Total Engagement Score")
	@ExposeInSegmentList
	@AutoSetData(setDataAsInteger = true)
	protected int totalEngagementScore = 0;
	
	@Expose
	protected Map<String, Integer> timeseriesEngagementScore = new HashMap<>(); 
	
	
	/**
	 * 6) business transaction value or purchasing value
	 */
	@Expose	
	@ProfileMetaDataField(dataQualityScore=100, label="Total Transaction Value")
	@AutoSetData(setDataAsDouble = true)
	@ExposeInSegmentList
	protected double totalTransactionValue = 0;
	
	@Expose
	protected Map<String, Double> timeseriesTransactionValue = new HashMap<>(); 
	
	/**
	 * 7) Customer Credit Score for Banking, Finance
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=50, label="Total Credit Score")
	@AutoSetData(setDataAsInteger = true)
	@ExposeInSegmentList
	protected int totalCreditScore = 0;
	
	@Expose
	protected Map<String, Integer> timeseriesCreditScore = new HashMap<>(); 
	
	/**
	 * 8) loyalty points program is where you create your own form of currency that customers can build up and spend at your company
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=50, label="Total Loyalty Score")
	@ExposeInSegmentList
	@AutoSetData(setDataAsInteger = true)
	protected int totalLoyaltyScore = 0;
	
	@Expose
	protected Map<String, Integer> timeseriesLoyaltyPoint = new HashMap<>(); 
	
	/**
	 * 9) Customer Acquisition Cost
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=20, label="Total CAC (Customer Acquisition Cost)")
	@AutoSetData(setDataAsDouble = true)
	@ExposeInSegmentList
	protected double totalCAC = 0; 
	
	@Expose
	protected Map<String, Double> timeseriesCAC = new HashMap<>(); 
	
	/**
	 * 10) Customer Lifetime Value
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=50, label="Total CLV (Customer Lifetime Value)")
	@AutoSetData(setDataAsDouble = true)
	@ExposeInSegmentList
	protected double totalCLV = 0; 
	
	@Expose
	protected Map<String, Double> timeseriesCLV = new HashMap<>(); 
	
	/**
	 * 11) Customer Feedback Score: can be positive or negative
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=20, label="Total CFS (Customer Feedback Score)")
	@ExposeInSegmentList
	@AutoSetData(setDataAsInteger = true)
	protected int totalCFS = 0;
	
	@Expose
	protected Map<String, ScoreCX> timeseriesCFS = new HashMap<>(); 
	
	@Expose
	protected int negativeCFS = 0;
	@Expose
	protected int neutralCFS = 0;
	@Expose
	protected int positiveCFS = 0;

	/**
	 * 12) Customer Effort Score:  to measure the ease of service experience with an organization
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Total CES (Customer Effort Score)")
	@ExposeInSegmentList
	@AutoSetData(setDataAsInteger = true)
	protected int totalCES = 0; 
	
	@Expose
	protected Map<String, ScoreCX> timeseriesCES = new HashMap<>(); 
	@Expose
	protected int negativeCES = 0; 
	@Expose
	protected int neutralCES = 0;
	@Expose
	protected int positiveCES = 0;
	
	/**
	 * 13) Customer Satisfaction Score: to measures customer satisfaction with a business, purchase, or interaction
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=20, label="Total CSAT (Customer Satisfaction Score)")
	@ExposeInSegmentList
	@AutoSetData(setDataAsInteger = true)
	protected int totalCSAT = 0; 
	
	@Expose
	protected Map<String, ScoreCX> timeseriesCSAT = new HashMap<>(); 
	@Expose
	protected int negativeCSAT = 0;
	@Expose
	protected int neutralCSAT = 0;
	@Expose
	protected int positiveCSAT = 0;

	/**
	 * 14) Net Promoter Score: percentage of customers rating their likelihood to recommend a company, a product, or a service to a friend or colleague 
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=20, label="Total NPS (Net Promoter Score)")
	@ExposeInSegmentList
	@AutoSetData(setDataAsInteger = true)
	protected int totalNPS = 0; 
	
	@Expose
	protected Map<String, ScoreCX> timeseriesNPS = new HashMap<>();
	@Expose
	protected int negativeNPS = 0;
	@Expose
	protected int neutralNPS = 0;
	@Expose
	protected int positiveNPS = 0;
	
	/**
	 * 15) total reach from fans or followers
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Estimated Network Reach")
	@AutoSetData(setDataAsInteger = true)
	@ExposeInSegmentList
	protected int totalEstimatedReach = 0; 
	
	@Expose
	protected Map<String, Integer> timeseriesReach = new HashMap<>(); 
	
	/**
	 * 16) RFM: Recency, Frequency, Monetary Score Value
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=30, label="RFM Score (Recency, Frequency, Monetary) ")
	@AutoSetData(setDataAsInteger = true)
	@ExposeInSegmentList
	protected int rfmScore = 0; 
	
	/**
	 * 17) RFE: Recency, Frequency, Engagement Score Value
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="RFE Score (Recency, Frequency, Engagement)")
	@AutoSetData(setDataAsInteger = true)
	@ExposeInSegmentList
	protected int rfeScore = 0; 
	
	/**
	 * 18) Churn Score
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=20, label="Churn Score")
	@AutoSetData(setDataAsDouble = true)
	@ExposeInSegmentList
	protected double churnScore = 0; 
	
	
	/**
	 * 19) extra score metrics
	 */
	@Expose
	@AutoSetData(setDataAsJsonMap = true)
	protected Map<String, Double> extMetrics = new HashMap<>();
 
	
	/**
	 * the timeline of first-time recorded event metrics <br>
	 *  E.g: {"content-view": "2020-05-05T02:56:12.102Z", "play_prvideo" : "2020-05-06T02:56:12.102Z" }
	 */
	@Expose
	protected Map<String,Date> funnelStageTimeline = new ConcurrentHashMap<>();
	
	/**
	 * all recorded event metric names ["content-view","item-view"]
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=5, label="Behavioral Events")
	@ExposeInSegmentList
	protected Set<String> behavioralEvents = new HashSet<String>(100);
	
	
	/**
	 * all recorded payment events
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=50, label="Payment Events")
	@ExposeInSegmentList
	protected Set<FinanceEvent> paymentEvents = new HashSet<FinanceEvent>();
	

	/**
	 * current name of funnel stage: Prospective Customer or Returning Visitor or First-time Customer
	 */
	@Expose
	@ExposeInSegmentList
	protected String funnelStage = DataFlowManagement.getNewVisitorStage().getId();
	
	/**
	 * summary view of statistics from Profile Analytics Jobs
	 */
	@Expose
	@ProfileMetaDataField(dataQualityScore=10, label="Event Statistics")
	@ExposeInSegmentList
	protected Map<String,Long> eventStatistics = new HashMap<>();
	
	// --- END Quantitative Data Metrics

	/**
	 *  when the database has more than 100,000 profile, need a good partitioning strategy
	 */
	@Expose
	protected int partitionId = 0;
	
	/**
	 * 1 is production data context, 0 is testing only, -1 is fake data
	 */
	@Expose
	@ExposeInSegmentList
	@AutoSetData(setDataAsInteger = true)
	protected int dataContext = 1; 
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public final boolean isNewProfile() {
		return StringUtil.isEmpty(this.id);
	}
	
	public void setId(String id) {
		if(StringUtil.isEmpty(this.id)) {
			this.id = id;
			setIdentities(id);
		}
	}
	
	public String getId() {
		if(this.id != null) {
			return id;
		} else {
			throw new IllegalArgumentException("Can not get profile.id, the ID is NULL and need to profile.buildHashedId()");
		}
	}
	
	public String getRootProfileId() {
		return rootProfileId;
	}

	public void setRootProfileId(String rootProfileId) {
		if(StringUtil.isNotEmpty(rootProfileId)) {
			this.rootProfileId = rootProfileId;
		}
	}
	
	public Set<String> getIdentities() {
		return identities;
	}

	public void setIdentities(Set<String> identities) {
		if(identities != null) {
			identities.forEach(s->{
				this.identities.add(s);
			});
		}
	}

	public void setIdentities(String identity) {
		if (isValidLength(identity)) {
			this.identities.add(identity);
		}
	}
	
	
	/**
	 * to add an identity into profile.identities
	 * 
	 * @param provider
	 * @param id
	 */
	public void setIdentity(String provider, String id) {
		if (StringUtil.isNotEmpty(provider) && StringUtil.isNotEmpty(id)) {
			this.identities.add(ProfileIdentity.buildIdPrefix(provider, id));
		}
	}
	
	public String getPrimaryEmail() {
		return primaryEmail;
	}

	public Set<String> getSecondaryEmails() {
		return secondaryEmails;
	}

	public void setSecondaryEmails(Set<String> emails) {
		if(emails != null) {
			emails.forEach(email->{
				setSecondaryEmails(email);
			});
		}
	}
	
	/**
	 * @param emails
	 */
	public void setSecondaryEmails(String emails) {
		if(StringUtil.isNotEmpty(emails)) {
			String[] toks = emails.split(StringPool.SEMICOLON);
			for (String email : toks) {
				// the secondaryEmails must not contain primaryEmail
				if(ProfileDataValidator.isValidEmail(email) && !email.equals(primaryEmail)) {				
					this.secondaryEmails.add(email);	
					this.setIdentity(ProfileIdentity.ID_PREFIX_EMAIL, email);
				}
			}
		}
	}

	/**
	 * @param primaryEmail
	 */
	public void setPrimaryEmail(String primaryEmail) {
		if(ProfileDataValidator.isValidEmail(primaryEmail)) {
			this.primaryEmail = primaryEmail;			
			this.receiveEmail = 1;
			this.setIdentity(ProfileIdentity.ID_PREFIX_EMAIL, primaryEmail);
		}
	}
	
	/**
	 * @param email
	 */
	public boolean setEmail(String email) {
		if (ProfileDataValidator.isValidEmail(email)) {
			if(StringUtil.isEmpty(this.primaryEmail)) {
				this.setPrimaryEmail(email);
			}
			else {
				this.setSecondaryEmails(this.primaryEmail);
				this.primaryEmail = email;
			}
			return true;
		}
		return false;
	}

	/**
	 * @return
	 */
	public String getPrimaryPhone() {
		return primaryPhone;
	}

	/**
	 * @param primaryPhone
	 */
	public void setPrimaryPhone(String primaryPhone) {
		if(ProfileDataValidator.isValidPhoneNumber(primaryPhone)) {
			this.primaryPhone = primaryPhone;
			this.receiveSMS = 1;
			this.setIdentity(ProfileIdentity.ID_PREFIX_PHONE, primaryPhone);
		}
	}
	
	/**
	 * @param phone
	 */
	public boolean setPhone(String phone) {
		if (ProfileDataValidator.isValidPhoneNumber(phone)) {
			if(StringUtil.isEmpty(this.primaryPhone)) {
				// check if primaryPhone is empty
				this.setPrimaryPhone(phone);
			}
			else {
				this.setSecondaryPhones(phone);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * @return
	 */
	public Set<String> getSecondaryPhones() {
		return secondaryPhones;
	}

	/**
	 * @param phones
	 */
	public void setSecondaryPhones(Set<String> phones) {
		if(phones != null) {
			phones.forEach(phone->{
				setSecondaryPhones(phone);
			});
		}
	}
	
	/**
	 * @param phones
	 */
	public void setSecondaryPhones(String phones) {
		if(StringUtil.isNotEmpty(phones)) {
			String[] toks = phones.split(StringPool.SEMICOLON);
			for (String phone : toks) {
				// the secondaryPhones must not contain primaryPhone
				if( ! phone.equals(this.primaryPhone)) {
					this.secondaryPhones.add(phone);	
					this.setIdentity(ProfileIdentity.ID_PREFIX_PHONE, phone);
				}
			}
		}
	}
	


	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		if(createdAt != null) {
			this.createdAt = createdAt;
		}
	}
	
	/**
	 * @param createdAt in string
	 */
	public void setCreatedAt(String createdAt) {
		setCreatedAt(ProfileModelUtil.parseDate(createdAt, SystemMetaData.DEFAULT_TIME_ZONE));
	}
	
	@Override
	public Date getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		if(updatedAt != null) {
			this.updatedAt = updatedAt;
		}
	}
	
	public Date getUpdatedByCrmAt() {
		return updatedByCrmAt;
	}

	public void setUpdatedByCrmAt(Date updatedByCrmAt) {
		if(updatedByCrmAt != null) {
			this.updatedByCrmAt = updatedByCrmAt;
		}
	}

	/**
	 * @param updatedAt in string
	 */
	public void setUpdatedByCrmAt(String updatedAt) {
		setUpdatedByCrmAt(ProfileModelUtil.parseDate(updatedAt, SystemMetaData.DEFAULT_TIME_ZONE));
	}
	
	public Set<RefKey> getInSegments() {
		return inSegments;
	}
	
	public Set<String> getInSegmentNames() {		
		return inSegments.stream().map(key-> {return key.getName();}).collect(Collectors.toSet());
	}
	
	public List<RefKey> getInSegmentsAsSortedSet() {
		List<RefKey> list = new ArrayList<>(this.inSegments);
		Collections.sort(list);
		return list;
	}

	public void setInSegments(Set<RefKey> inSegments) {
		if( inSegments != null ) {
			inSegments.forEach(k->{
				this.inSegments.add(k);
			});
		}
	}
	
	public void setInSegmentsAsNew(Set<RefKey> inSegments) {
		this.inSegments = inSegments;
	}
	
	public void setInSegment(RefKey refKey) {
		if(inSegments.contains(refKey)) {
			inSegments.forEach(rk->{
				boolean equals = rk.getId().equals(refKey.getId());
				if(equals) {
					rk.setName(refKey.getName());
					rk.setIndexScore(refKey.getIndexScore());
				}
			});
		} else {
			inSegments.add(refKey);
		}
	}
	
	public Set<RefKey> getSimilarProfiles() {
		return similarProfiles;
	}

	public void setSimilarProfiles(Set<RefKey> similarProfiles) {
		this.similarProfiles = similarProfiles;
	}
	
	
	public Set<RefKey> getInCampaigns() {
		return inCampaigns;
	}

	public void setInCampaigns(Set<RefKey> inCampaigns) {
		if( inCampaigns != null ) {
			inCampaigns.forEach(e->{
				this.inCampaigns.add(e);
			});
		}
	}
	
	public void setInCampaign(RefKey campaign) {
		if( campaign != null ) {
			this.inCampaigns.add(campaign);
		}
	}
	
	public void setCampaignUTM(String campaignId) {
		if( StringUtil.isNotEmpty(campaignId) ) {
			this.inCampaigns.add(new RefKey(campaignId, campaignId, "UTM"));
		}
	}

	public Set<RefKey> getInAccounts() {
		return inAccounts;
	}

	public void setInAccounts(Set<RefKey> accountIds) {
		if( accountIds != null ) {
			accountIds.forEach(e->{
				this.inAccounts.add(e);
			});
		}
	}
	
	public void setInAccount(RefKey refKey) {
		if(inAccounts.contains(refKey)) {
			inAccounts.forEach(rk->{
				if(rk.getId().equals(refKey.getId())) {
					rk.setName(refKey.getName());
					rk.setIndexScore(refKey.getIndexScore());
				}
			});
		} else {
			inAccounts.add(refKey);
		}
	}

	public void clearInJourneyMaps() {
		this.inJourneyMaps.clear();
	}
	
	public Set<JourneyMapRefKey> getInJourneyMaps() {
		return inJourneyMaps;
	}

	/**
	 * @param inJourneyMaps
	 */
	public void setInJourneyMaps(Set<JourneyMapRefKey> inJourneyMaps) {
		if(inJourneyMaps != null) {
			for (JourneyMapRefKey nref : inJourneyMaps) {
				for (JourneyMapRefKey cref : this.inJourneyMaps) {
					String journeyMapId = cref.getId();
					
					boolean check = journeyMapId.equals(nref.getId());
					if(check) {
						int newFunnelIndex = nref.getFunnelIndex();
						int newJourneyMapStage = nref.getIndexScore();
						int currentJourneyMapStage = cref.getIndexScore();
						int currentFunnelIndex = cref.getFunnelIndex();
						String currentProfileType = cref.getType();
						String newProfileType = nref.getType();
						
						check = check && (newJourneyMapStage > currentJourneyMapStage || newFunnelIndex > currentFunnelIndex) ;
						check = check && (currentProfileType.equals(VISITOR) && newProfileType.equals(CONTACT));
						if(check) {
							// update
							cref.setType(newProfileType);
							cref.setIndexScore(newJourneyMapStage);
							cref.setFunnelIndex(newFunnelIndex);
							cref.setScoreCX(nref.getScoreCX());
						}	
					}
					else {
						// add new
						this.inJourneyMaps.add(nref);
					}
				}
			}
		}
	}
	
	/**
	 * @param journeyMaps
	 */
	public void updateInJourneyMaps(Set<JourneyMapRefKey> journeyMaps) {		
		for (JourneyMapRefKey ref : journeyMaps) {
			for (JourneyMapRefKey cref : this.inJourneyMaps) {
				String journeyMapId = cref.getId();
				if(journeyMapId.equals(ref.getId())) {
					String journeyMapName = ref.getName();
					cref.setName(journeyMapName);
				}
			}
		}
	}
	

	public boolean setInJourneyMapDefaultForLead() {
		if(this.hasContactData()) {
			String profileType = CONTACT;
			int funnelIndex = 3;// LEAD
			int journeyMapStage = 2; // ATTRACTION
			return setInJourneyMapDefault(profileType, funnelIndex, journeyMapStage);
		}
		return false;
	}
	
	/**
	 * @param profileType
	 * @param funnelIndex
	 * @param journeyMapStage
	 * @return
	 */
	public boolean setInJourneyMapDefault(String profileType, int funnelIndex, int journeyMapStage) {
		boolean check = false;
		String journeyMapId = JourneyMap.DEFAULT_JOURNEY_MAP_ID;
		String journeyMapName = JourneyMap.DEFAULT_JOURNEY_MAP_NAME;
		JourneyMapRefKey e = new JourneyMapRefKey(journeyMapId, journeyMapName, profileType, journeyMapStage, funnelIndex);
		if(this.inJourneyMaps.contains(e)) {
			for (JourneyMapRefKey r : this.inJourneyMaps) {
				check = journeyMapId.equals(r.getId()) && (journeyMapStage >= r.getIndexScore() || !profileType.equals(r.getType())) ;
				if(check) {					
					r.setType(profileType);
					r.setIndexScore(journeyMapStage);
					r.setFunnelIndex(funnelIndex);
				}
			}
		}
		else {
			check = this.inJourneyMaps.add(e);	
		}
		return check;

	}
	
	public void setInJourneyMap(JourneyMapRefKey refKey) {
		this.inJourneyMaps.add(refKey);
	}
	
	public void setInJourneyMap(JourneyMap journeyMap, int journeyMapStage, int funnelIndex) {
		setInJourneyMap(journeyMap, journeyMapStage, funnelIndex, null);
	}
	
	/**
	 * to update journey map status of profile
	 * 
	 * @param journeyMap
	 * @param journeyMapStage
	 * @param journeyRef
	 */
	public void setInJourneyMap(JourneyMap journeyMap, int journeyMapStage, int funnelIndex, ScoreCX scoreCX) {
		String profileType = this.hasContactData() ? CONTACT : VISITOR;
		JourneyMapRefKey journeyRef = journeyMap.getJourneyMapRefKey();
		if(this.inJourneyMaps.contains(journeyRef)) {
			String journeyMapId = journeyMap.getId();
			for (JourneyMapRefKey r : this.inJourneyMaps) {
				boolean check = journeyMapId.equals(r.getId());
				check = check && (journeyMapStage >= r.getIndexScore() || funnelIndex > r.getFunnelIndex() || !profileType.equals(r.getType())) ;
				if(check) {					
					r.setType(profileType);
					r.setIndexScore(journeyMapStage);
					r.setFunnelIndex(funnelIndex);
					r.setScoreCX(scoreCX);
				}
			}
		}
		else {
			journeyRef.setType(profileType);
			journeyRef.setIndexScore(journeyMapStage);
			journeyRef.setFunnelIndex(funnelIndex);
			journeyRef.setScoreCX(scoreCX);
			this.inJourneyMaps.add(journeyRef);	
		}
	}

	
	public void removeJourneyMap(String journeyMapId) {
		this.inJourneyMaps.remove(new JourneyMapRefKey(journeyMapId));
	}
	
	public JourneyMapRefKey getOrCreateJourneyMapRefKey(JourneyMap journeyMap) {
		String journeyMapId = journeyMap.getId();
		if(this.inJourneyMaps.contains(new JourneyMapRefKey(journeyMapId))) {
			for (JourneyMapRefKey r : this.inJourneyMaps) {
				if(journeyMapId.equals(r.getId())) {
					return r;
				}
			}
		}
		String profileType = this.hasContactData() ? Profile.CONTACT : Profile.VISITOR;
		JourneyMapRefKey ref = journeyMap.getJourneyMapRefKey();
		ref.setType(profileType);
		return ref;
	}

	public Set<String> getAuthorizedEditors() {
		if(authorizedEditors == null) {
			authorizedEditors = new HashSet<>();
		}
		return authorizedEditors;
	}

	public void setAuthorizedEditors(Set<String> authorizedEditors) {
		if(authorizedEditors != null) {
			authorizedEditors.forEach(authorizedEditorId->{
				setAuthorizedEditor(authorizedEditorId);
			});
		}
	}
	
	public void setAuthorizedEditor(String authorizedEditorId) {
		if(StringUtil.isNotEmpty(authorizedEditorId)) {
			this.authorizedEditors.add(authorizedEditorId);
		}
	}
	
	public Set<String> getAuthorizedViewers() {
		return authorizedViewers;
	}

	public void setAuthorizedViewers(Set<String> authorizedViewers) {
		if(authorizedViewers != null) {
			authorizedViewers.forEach(authorizedViewerId->{
				setAuthorizedViewer(authorizedViewerId);
			});
		}
	}
	
	public void setAuthorizedViewer(String authorizedViewerId) {
		if(StringUtil.isNotEmpty(authorizedViewerId)) {
			this.authorizedViewers.add(authorizedViewerId);
		}
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	public Map<String, Integer> getSubscribedChannels() {
		return subscribedChannels;
	}

	public void setSubscribedChannels(Map<String, Integer> subscribedChannels) {
		if(subscribedChannels != null) {
			subscribedChannels.forEach((k,v)->{
				this.subscribedChannels.put(k,v);
			});
			
		}
	}
	
	public void unsubscribedChannel(String channel) {
		if (StringUtil.isNotEmpty(channel)) {
			subscribedChannels.put(channel, -1);
		}
	}
	
	public void setSubscribedChannels(String channel) {
		if (StringUtil.isNotEmpty(channel)) {
			int count = subscribedChannels.getOrDefault(channel, 0);
			if(count >= 0) {
				count++;
				subscribedChannels.put(channel, count);
			}
		}
	}
	
	public Set<String> getSaleAgencies() {
		return saleAgencies;
	}

	public void setSaleAgencies(Set<String> saleAgencies) {
		if(saleAgencies != null) {
			saleAgencies.forEach(e->{
				this.saleAgencies.add(e);
			});
		}
	}
	
	public void setSaleAgencies(String saleAgencies) {
		if(StringUtil.isNotEmpty(saleAgencies)) {
			String[] toks = saleAgencies.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.saleAgencies.add(tok);
			}
		}
	}

	public Set<String> getSaleAgents() {
		return saleAgents;
	}

	public void setSaleAgents(Set<String> saleAgents) {
		if(saleAgents != null) {
			saleAgents.forEach(e->{
				this.saleAgents.add(e);
			});
		}
	}
	
	public void setSaleAgents(String saleAgents) {
		if(StringUtil.isNotEmpty(saleAgents)) {
			String[] toks = saleAgents.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.saleAgents.add(tok);
			}
		}
	}

	public int getDataQualityScore() {
		return dataQualityScore;
	}

	public void setDataQualityScore(int dataQualityScore) {
		this.dataQualityScore = dataQualityScore;
	}


	public Map<String, Object> getExtAttributes() {
		return extAttributes;
	}

	public void setExtAttributes(Map<String, Object> attributes) {
		if(attributes != null) {
			attributes.forEach((String k, Object v)->{
				this.extAttributes.put(k, v);
			});
		}
	}
	
	public void setExtAttributes(String key, Object value) {
		this.extAttributes.put(key, value);
	}
	
	public void setExtAttributes(String json) {
		Type type = new TypeToken<Map<String, Object>>(){}.getType();
		Map<String, Object> myMap = new Gson().fromJson(json, type);
		this.extAttributes.putAll(myMap);
	}

	public Map<String,Double> getExtMetrics() {
		return extMetrics;
	}

	public void setExtMetrics(Map<String, Double> extMetrics) {
		this.extMetrics.putAll(extMetrics);
	}
	
	public void setExtMetrics(String json) {
		Type type = new TypeToken<Map<String, Double>>(){}.getType();
		Map<String, Double> myMap = new Gson().fromJson(json, type);
		this.extMetrics.putAll(myMap);
	}
	

	public int getJourneyScore() {
		return journeyScore;
	}

	public void setJourneyScore(int journeyScore) {
		this.journeyScore = journeyScore;
	}

	
	public double getTotalTransactionValue() {
		return totalTransactionValue;
	}

	public void setTotalTransactionValue(double totalTransactionValue) {
		if(totalTransactionValue > 0 ) {
			this.totalTransactionValue = totalTransactionValue;
		}
	}
	
	public void addTotalTransactionValue(double totalTransactionValue) {
		if(totalTransactionValue > 0 ) {
			this.totalTransactionValue += totalTransactionValue;
		}
	}
	
	public double getTotalCAC() {
		return totalCAC;
	}

	public void setTotalCAC(double totalCAC) {
		this.totalCAC = totalCAC;
	}
	
	public void setTotalCAC(String totalCAC) {
		this.totalCAC = StringUtil.safeParseDouble(totalCAC);
	}
	
	public void addTotalCAC(double totalCAC) {
		this.totalCAC += totalCAC;
	}

	public double getTotalCLV() {
		return totalCLV;
	}

	public void setTotalCLV(double totalCLV) {
		if(totalCLV > 0 ) {
			this.totalCLV = totalCLV;
		}
	}
	
	public void setTotalCLV(String totalCLV) {
		this.totalCLV = StringUtil.safeParseDouble(totalCLV);
	}
	
	public void addTotalCLV(double totalCLV) {
		if(totalCLV > 0 ) {
			this.totalCLV += totalCLV;
		}
	}

	public int getTotalLoyaltyScore() {
		return totalLoyaltyScore;
	}
	
	/**
	 * only customer profile (not lead) can be updated totalLoyaltyScore
	 * 
	 * @param totalLoyaltyScore
	 */
	public void setTotalLoyaltyScore(int totalLoyaltyScore) {
		if(totalLoyaltyScore >= 0 && this.type > ProfileType.CUSTOMER_CONTACT ) {
			this.totalLoyaltyScore = totalLoyaltyScore;
		}
	}
	
	public void addTotalLoyaltyScore(int totalLoyaltyScore) {
		if(totalLoyaltyScore > 0) {
			this.totalLoyaltyScore += totalLoyaltyScore;
		}
	}

	public int getTotalEstimatedReach() {
		return totalEstimatedReach;
	}

	public void setTotalEstimatedReach(int totalEstimatedReach) {
		if(totalEstimatedReach >= 0) {
			this.totalEstimatedReach = totalEstimatedReach;
		}
	}
	
	public void setTotalEstimatedReach(String totalEstimatedReach) {
		int val = StringUtil.safeParseInt(totalEstimatedReach);
		if(val > 0) {
			this.totalEstimatedReach = val;
		}
	}
	
	public void addTotalEstimatedReach(int totalEstimatedReach) {
		if(totalEstimatedReach >= 0) {
			this.totalEstimatedReach += totalEstimatedReach;
		}
		else {
			throw new InvalidDataException(" totalEstimatedReach must not be less than 0");
		}
	}


	public int getMergeCode() {
		return mergeCode;
	}

	public void setMergeCode(int mergeCode) {
		this.mergeCode = mergeCode;
	}
	
	public Set<String> getDataLabels() {
		if(dataLabels == null) {
			dataLabels = new HashSet<>(1);
		}
		return dataLabels;
	}

	public void setDataLabels(Set<String> dataLabels) {
		if(dataLabels != null) {
			dataLabels.forEach(s->{
				this.dataLabels.add(s);
			});
		}	
	}
	
	public void clearDataLabels() {
		this.dataLabels.clear();
	}
	
	/**
	 * @param dataLabels
	 */
	public void setDataLabels(String dataLabels) {
		this.setDataLabels(dataLabels, false);
	}
	
	/**
	 * @param dataLabels
	 * @param clearOldData
	 */
	public void setDataLabels(String dataLabelsStr, boolean clearOldData) {
		List<String> asList = Arrays.asList(dataLabelsStr.split(StringPool.SEMICOLON));
		if( asList.size() > 0 && clearOldData) {
			this.dataLabels.clear();
		}
		asList.forEach(s->{
			String label = s.trim();
			if(isValidLength(label)) {
				this.dataLabels.add(label);
			}
		});
	}
	
	/**
	 * @param dataLabel
	 */
	public void addDataLabel(String dataLabel) {
		this.dataLabels.add(dataLabel);
	}

	public String getPersonaUri() {
		return personaUri;
	}

	public void setPersonaUri(String personaUri) {
		if(StringUtil.isNotEmpty(personaUri)) {
			this.personaUri = personaUri;
		}
	}

	public boolean isDataVerification() {
		return dataVerification != null ? dataVerification.booleanValue() : false;
	}
	
	public boolean getDataVerification() {
		return isDataVerification();
	}

	public void setDataVerification(Boolean dataVerification) {
		this.dataVerification = dataVerification;
	}
	
	public void setDataVerification(String dataVerification) {
		this.dataVerification = Boolean.parseBoolean(StringUtil.safeString(dataVerification));
	}

	public int getPartitionId() {
		return partitionId;
	}

	public void setPartitionId(int partitionId) {
		this.partitionId = partitionId;
	}
	
	public Set<String> getPersonalProblems() {
		return personalProblems;
	}

	public void setPersonalProblems(Set<String> list) {
		if(list != null) {
			list.forEach(e->{
				this.personalProblems.add(e);
			});
		}
	}
	
	public void setPersonalProblems(String personalProblems) {
		if(personalProblems != null) {
			String[] toks = personalProblems.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.personalProblems.add(tok.trim());
			}
		}
	}

	public Set<String> getBusinessIndustries() {
		return businessIndustries;
	}

	public void setBusinessIndustries(Set<String> businessIndustries) {
		if(businessIndustries != null) {
			businessIndustries.forEach(e->{
				this.businessIndustries.add(e);
			});
		}
	}
	
	public void setBusinessIndustries(String businessIndustries) {
		if(StringUtil.isNotEmpty(businessIndustries)) {
			String[] toks = businessIndustries.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.businessIndustries.add(tok.trim());
			}
		}
	}
	
	public void addBusinessIndustry(String businessIndustry) {
		this.businessIndustries.add(businessIndustry);
	}

	public Set<String> getSolutionsForCustomer() {
		return solutionsForCustomer;
	}

	public void setSolutionsForCustomer(Set<String> solutionsForCustomer) {
		if(solutionsForCustomer != null) {
			solutionsForCustomer.forEach(e->{
				this.solutionsForCustomer.add(e);
			});
		}
	}
	
	public void setSolutionsForCustomer(String solutionsForCustomer) {
		if(StringUtil.isNotEmpty(solutionsForCustomer)) {
			String[] toks = solutionsForCustomer.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.solutionsForCustomer.add(tok.trim());
			}
		}
	}

	public Set<String> getMediaChannels() {
		if(mediaChannels == null) {
			this.mediaChannels = new HashSet<String>(0);
		}
		return mediaChannels;
	}

	public void setMediaChannels(Set<String> mediaChannels) {
		if(mediaChannels != null) {
			mediaChannels.forEach(e->{
				this.mediaChannels.add(e);
			});
		}
	}
	
	public void setMediaChannels(String mediaChannelsStr) {
		if(StringUtil.isNotEmpty(mediaChannelsStr)) {
			String[] toks = mediaChannelsStr.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.mediaChannels.add(tok);
			}
		}
	}
	
	public void addMediaChannel(String mediaChannel) {
		if(StringUtil.isNotEmpty(mediaChannel)) {
			this.mediaChannels.add(mediaChannel);
		}
	}

	public String getSchemaType() {
		return schemaType;
	}

	public void setSchemaType(String schemaType) {
		if(StringUtil.isNotEmpty(schemaType)) {
			this.schemaType = schemaType;
		}
	}
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
		this.typeAsText = ProfileType.getTypeAsText(type);
	}
	
	public final boolean isAnonymousProfile() {
		return this.type == ProfileType.ANONYMOUS_VISITOR;
	}
	
	public final boolean isMergeableProfile() {
		return this.type <= ProfileType.STUDENT_CONTACT;
	}
	
	public void loadProfileTypeAsText() {
		this.typeAsText = ProfileType.getTypeAsText(this.type);
	}
	
	public String getTypeAsText() {
		return this.typeAsText;
	}

	public void setTypeAsText(String typeAsText) {
		this.typeAsText = typeAsText;
	}

	public Set<String> getContentKeywords() {
		this.contentKeywords = this.contentKeywords == null ? new HashSet<>(0) : this.contentKeywords;
		return contentKeywords;
	}

	public void setContentKeywords(Set<String> contentKeywords) {
		if(contentKeywords != null) {
			contentKeywords.forEach(e->{
				this.contentKeywords.add(e);
			});
		}
	}

	public void setContentKeywords(String contentKeywordsStr) {
		if(StringUtil.isNotEmpty(contentKeywordsStr)) {
			String[] toks = contentKeywordsStr.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.contentKeywords.add(tok.trim());
			}
		}
	}

	public Set<String> getProductKeywords() {
		return productKeywords;
	}

	public void setProductKeywords(Set<String> productKeywords) {
		if(productKeywords != null) {
			productKeywords.forEach(e->{
				this.productKeywords.add(e);
			});
		}
	}
	
	public void setProductKeywords(String productKeywordsStr) {
		if(StringUtil.isNotEmpty(productKeywordsStr)) {
			String[] toks = productKeywordsStr.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				this.productKeywords.add(tok.trim());
			}
		}
	}

	public Set<String> getNextBestActions() {
		return nextBestActions;
	}

	public void setNextBestActions(Set<String> nextBestActions) {
		if(nextBestActions != null) {
			nextBestActions.forEach(e->{
				this.nextBestActions.add(e);
			});
		}
	}
	
	public void setNextBestActions(String nextBestAction) {
		if(StringUtil.isNotEmpty(nextBestAction)) {
			this.nextBestActions.add(nextBestAction);
		}
	}
	
	public void removeNextBestAction(String nextBestAction) {
		if(StringUtil.isNotEmpty(nextBestAction)) {
			this.nextBestActions.remove(nextBestAction);
		}
	}
	

	public Set<String> getPurchasedBrands() {
		return purchasedBrands;
	}

	public void setPurchasedBrands(Set<String> purchasedBrands) {
		if(purchasedBrands != null) {
			purchasedBrands.forEach(e->{
				this.purchasedBrands.add(e);
			});
		}
	}
	
	public void setPurchasedBrands(String purchasedBrandStr) {
		if(purchasedBrandStr != null) {
			String[] purchasedBrandArray = purchasedBrandStr.split(StringPool.SEMICOLON);
			for (String purchasedBrand : purchasedBrandArray) {
				this.setPurchasedBrand(purchasedBrand);
			}
		}
	}
	
	public void setPurchasedBrand(String purchasedBrand) {
		if(StringUtil.isNotEmpty(purchasedBrand)) {
			this.purchasedBrands.add(purchasedBrand);
		}
	}

	public Set<OrderedItem> getPurchasedItems() {
		return purchasedItems;
	}

	public void setPurchasedItems(Set<OrderedItem> purchasedItems) {
		if(purchasedItems != null) {
			purchasedItems.forEach(item->{
				this.purchasedItems.add(item);
				this.purchasedItemIds.add(item.getItemId());
			});
		}
	}
	
	
	public Set<String> getShoppingItemIds() {
		return shoppingItemIds;
	}

	public void setShoppingItemIds(Set<String> shoppingItemIds) {
		if(shoppingItemIds != null) {
			shoppingItemIds.forEach(s->{
				this.shoppingItemIds.add(s);
			});
		}		
	}

	public Set<String> getPurchasedItemIds() {
		return purchasedItemIds;
	}

	public void setPurchasedItemIds(Set<String> purchasedItemIds) {
		if(purchasedItemIds != null) {
			purchasedItemIds.forEach(s->{
				this.purchasedItemIds.add(s);
			});
		}		
	}

	public void setPurchasedItems(String purchasedItemsJson) {
		try {
			Type listType = new TypeToken<HashSet<OrderedItem>>(){}.getType();
			Set<OrderedItem> items = new Gson().fromJson(purchasedItemsJson, listType);
			if(items != null) {
				setPurchasedItems(items);
				items.forEach(item->{
					this.purchasedItemIds.add(item.getItemId());
				});
			}
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public void addPurchasedItem(OrderedItem item) {
		if(item != null) {
			String itemId = item.getItemId();
			this.purchasedItems.add(item);
			this.purchasedItemIds.add(itemId);
			this.shoppingItems.remove(item);
			this.shoppingItemIds.remove(itemId);
		}
	}
	
	public Set<OrderedItem> getShoppingItems() {
		return shoppingItems;
	}

	public void setShoppingItems(Set<OrderedItem> shoppingItems) {
		if(shoppingItems != null) {
			shoppingItems.forEach(item->{
				this.shoppingItems.add(item);
				this.shoppingItemIds.add(item.getItemId());
			});
		}
	}
	
	public void addShoppingItem(Date createdDate, ProductItem pItem, int quantity ) {
		if(pItem != null) {
			OrderedItem item = new OrderedItem(createdDate, pItem, quantity);
			this.shoppingItems.add(item);
			this.shoppingItemIds.add(item.getItemId());
		}
	}
	
	public void addShoppingItem(OrderedItem item) {
		if(item != null) {
			this.shoppingItems.add(item);
			this.shoppingItemIds.add(item.getItemId());
		}
	}
	
	public void setShoppingItems(String json) {
		try {
			Type listType = new TypeToken<HashSet<OrderedItem>>(){}.getType();
			Set<OrderedItem> shoppingItems = new Gson().fromJson(json, listType);
			if(shoppingItems != null) {
				setShoppingItems(shoppingItems);
			}
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, Set<String>> getBusinessData() {
		return businessData;
	}

	public void setBusinessData(Map<String, Set<String>> businessData) {
	    if (businessData == null || businessData.isEmpty()) {
	        return;
	    }

	    if (this.businessData == null) {
	        this.businessData = new HashMap<>();
	    }

	    // Merge entries safely by creating mutable copies
	    businessData.forEach((key, valueSet) -> {
	        Set<String> existingSet = this.businessData.getOrDefault(key, new HashSet<>());
	        if (valueSet != null) {
	            existingSet.addAll(valueSet);  // merge values
	        }
	        this.businessData.put(key, existingSet);
	    });
	}

	
	public Map<String, String> getSocialMediaProfiles() {
		return socialMediaProfiles;
	}

	public void setSocialMediaProfiles(Map<String, String> socialMediaProfiles) {
		if(socialMediaProfiles != null) {
			socialMediaProfiles.forEach((key,val)->{
				this.socialMediaProfiles.putIfAbsent(key, val);
			});
		}
	}
	
	public void setSocialMediaProfiles(String json) {
		if(json != null) {
			Type type = new TypeToken<Map<String, String>>(){}.getType();
			Map<String, String> myMap = new Gson().fromJson(json, type);
			this.socialMediaProfiles.putAll(myMap);
		}
	}
	
	public void importSocialMediaProfiles(String socialMediaProfilesStr) {
		if(socialMediaProfilesStr != null) {
			String[] toks = socialMediaProfilesStr.split(StringPool.SEMICOLON);
			for (String tok : toks) {
				String[] toks2 = tok.split(StringPool.COLON);
				if(toks2.length == 2) {
					String key = toks2[0].trim();
					String value = toks2[1].trim();
					if(StringUtil.isNotEmpty(key) && StringUtil.isNotEmpty(value)) {
						this.socialMediaProfiles.put(key, value);
					}
				}	
			}
		}
	}

	public final void setSocialMediaProfile(String source, String id) {
		if( StringUtil.isNotEmpty(source) &&  StringUtil.isNotEmpty(id)) {
			this.socialMediaProfiles.put(source, id);
			this.setIdentity(source, id);
		}
	}
	
	public void setSocialMediaId(String socialId) {
		if(StringUtil.isNotEmpty(socialId)) {
			String [] toks = socialId.split(":");
			if(toks.length == 2) {
				String socialPlatform = toks[0];
				String userId = toks[1];
				setSocialMediaProfile(socialPlatform, userId);
			}
		}
	}

	public String getCrmRefId() {
		return crmRefId;
	}

	public void setCrmRefId(String crmId) {
		if(StringUtil.isNotEmpty(crmId) && StringUtil.isEmpty(this.crmRefId)) {
			this.crmRefId = crmId;			
			this.setIdentity(ProfileIdentity.ID_PREFIX_CRM, crmId);
		}
	}
	
	public int getTotalLeadScore() {
		return totalLeadScore;
	}

	public void setTotalLeadScore(int totalLeadScore) {
		this.totalLeadScore = totalLeadScore;
	}
	
	public void addTotalLeadScore(int leadScore) {
		this.totalLeadScore += leadScore;
	}

	public int getTotalProspectScore() {
		return totalProspectScore;
	}

	public void setTotalProspectScore(int totalProspectScore) {
		this.totalProspectScore = totalProspectScore;
	}
	
	public void addTotalProspectScore(int totalProspectScore) {
		this.totalProspectScore += totalProspectScore;
	}
	
	public int getTotalEngagementScore() {
		return totalEngagementScore;
	}

	public void setTotalEngagementScore(int totalEngagementScore) {
		this.totalEngagementScore = totalEngagementScore;
	}
	
	public void addTotalEngagementScore(int totalEngagementScore) {
		this.totalEngagementScore += totalEngagementScore;
	}


	/**
	 * @return Customer Credit Score (CCS)
	 */
	public int getTotalCreditScore() {
		return totalCreditScore;
	}

	public void setTotalCreditScore(int totalCreditScore) {
		this.totalCreditScore = totalCreditScore;
	}
	
	public void setTotalCreditScore(String totalCreditScore) {
		this.totalCreditScore = StringUtil.safeParseInt(totalCreditScore);
	}
	
	public void addTotalCreditScore(int totalCreditScore) {
		this.totalCreditScore += totalCreditScore;
	}

	/**
	 * @return Customer Feedback Score (CFS)
	 */
	public int getTotalCFS() {
		return totalCFS;
	}

	public void setTotalCFS(int totalCFS) {
		this.totalCFS = totalCFS;
	}
	
	public Set<String> getNotificationUserIdsByProvider(String provider) {
		return notificationUserIds.getOrDefault(provider, new HashSet<String>(0));
	}
	
	public void setNotificationUserIds(Map<String, Set<String>> notificationUserIds) {
	    if (notificationUserIds == null || notificationUserIds.isEmpty()) {
	        return;
	    }

	    if (this.notificationUserIds == null) {
	        this.notificationUserIds = new HashMap<>();
	    }

	    // Merge entries safely into a mutable map
	    notificationUserIds.forEach((key, users) -> {
	        Set<String> existing = this.notificationUserIds.getOrDefault(key, new HashSet<>());
	        if (users != null) {
	            existing.addAll(users);
	        }
	        this.notificationUserIds.put(key, existing);
	    });
	}

	
	public Map<String, Set<String>> getNotificationUserIds() {
		return notificationUserIds;
	}

	public void setWebNotificationUserIds(Map<String, Set<String>> notificationUserIds) {
		this.notificationUserIds = notificationUserIds;
		this.receiveWebPush = 1;
	}
	
	public void setMobileNotificationUserIds(Map<String, Set<String>> notificationUserIds) {
		this.notificationUserIds = notificationUserIds;
		this.receiveAppPush = 1;
	}
	
	public Map<String, Integer> getReferrerChannels() {
		return referrerChannels;
	}

	public void setReferrerChannels(Map<String, Integer> referrerChannels) {
		if(referrerChannels != null) {
			referrerChannels.forEach((channel, updateCount )->{
				int count = this.referrerChannels.getOrDefault(channel, 0) + updateCount;
				this.referrerChannels.put(channel, count);
			});
		}
	}

	public void updateReferrerChannel(String channel) {
		if (StringUtil.isNotEmpty(channel)) {
			int count = referrerChannels.getOrDefault(channel, 0) + 1;
			referrerChannels.put(channel, count);
		}
	}
	
	public Set<FinanceRecord> getFinanceRecords() {
		return financeRecords;
	}

	public void setFinanceRecords(Set<FinanceRecord> financeRecords) {
		if(financeRecords != null) {
			financeRecords.forEach(e->{
				this.financeRecords.add(e);
			});
		}
	}
	
	public void setFinanceRecord(FinanceRecord financeRecord) {
		if(financeRecord != null) {
			this.financeRecords.add(financeRecord);
		}
	}
	
	public Set<FinanceCreditEvent> getFinanceCreditEvents() {
		return financeCreditEvents;
	}

	public void setFinanceCreditEvents(Set<FinanceCreditEvent> financeCreditEvents) {
		if(financeCreditEvents != null) {
			financeCreditEvents.forEach(e->{
				this.financeCreditEvents.add(e);
			});
		}
	}
	
	public void setFinanceCreditEvent(FinanceCreditEvent financeCreditEvent) {
		if(financeCreditEvent != null) {
			this.financeCreditEvents.add(financeCreditEvent);
		}
	}
	
	public void setFinanceCreditEvents(String json) {
		try {
			Type listType = new TypeToken<HashSet<FinanceCreditEvent>>(){}.getType();
			Set<FinanceCreditEvent> financeCreditEvents = new Gson().fromJson(json, listType);
			if(financeCreditEvents != null) {
				setFinanceCreditEvents(financeCreditEvents);
			}
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		}
	}

	public final String getFunnelStage() {
		return funnelStage;
	}

	public void setFunnelStage(String funnelStageKey) {
		String funnelStageName = CustomerFunnel.getCustomerFunnelName(funnelStageKey);
		if(StringUtil.isNotEmpty(funnelStageName)) {
			this.funnelStage = funnelStageKey;
		}
	}

	
	/**
	 * ok to receive email
	 * 
	 * @return
	 */
	public boolean isReceiveEmail() {
		return receiveEmail == 1;
	}
	
	public boolean isReceiveWebPush() {
		return this.receiveWebPush == 1;
	}
	
	/**
	 * check to receive mobile push notification
	 * 
	 * @return
	 */
	public boolean isReceiveAppPush() {
		return this.receiveAppPush == 1;
	}
	
	public boolean isReceiveSMS() {
		return this.receiveSMS == 1;
	}
	
	/**
	 * check to receive web push notification
	 * 
	 * @return
	 */
	public int getReceiveWebPush() {
		return receiveWebPush;
	}

	public void setReceiveWebPush(int receiveWebPush) {
		this.receiveWebPush = receiveWebPush;
	}
	
	public int getReceiveEmail() {
		return receiveEmail;
	}

	public void setReceiveEmail(int receiveEmail) {
		this.receiveEmail = receiveEmail;
	}

	public int getReceiveAppPush() {
		return receiveAppPush;
	}

	public void setReceiveAppPush(int receiveMobilePush) {
		this.receiveAppPush = receiveMobilePush;
	}
	
	public boolean canSendEmail() {
		return isReceiveEmail() && EmailValidator.getInstance().isValid(this.primaryEmail);
	}

	/**
	 * check to receive SMS
	 * 
	 * @return
	 */
	public int getReceiveSMS() {
		return receiveSMS;
	}

	public void setReceiveSMS(int receiveSMS) {
		this.receiveSMS = receiveSMS;
	}

	public int getReceiveAds() {
		return receiveAds;
	}

	public void setReceiveAds(int receiveAds) {
		this.receiveAds = receiveAds;
	}

	public int getRecommendationModel() {
		return recommendationModel;
	}

	public void setRecommendationModel(int recommendationModel) {
		this.recommendationModel = recommendationModel;
	}


	/**
	 * 
	 * @return Customer Satisfaction Score
	 */
	public int getTotalCSAT() {
		return totalCSAT;
	}

	/**
	 * Customer Satisfaction Score
	 * 
	 * @param totalCSAT
	 */
	public void setTotalCSAT(int totalCSAT) {
		this.totalCSAT = totalCSAT;
	}

	/**
	 * @return Customer Effort Score
	 */
	public int getTotalCES() {
		return totalCES;
	}

	/**
	 * Customer Effort Score
	 * 
	 * @param totalCES
	 */
	public void setTotalCES(int totalCES) {
		this.totalCES = totalCES;
	}
	
	public Map<String, ScoreCX> getTimeseriesCES() {
		return timeseriesCES;
	}

	public void setTimeseriesCES(Map<String, ScoreCX> timeseriesCES) {
		this.timeseriesCES.putAll(timeseriesCES);
	}
	
	public void addTimeseriesCES(String dateKey, ScoreCX score) {
		this.timeseriesCES.put(dateKey, score);
	}

	public int getNegativeCES() {
		return negativeCES;
	}

	public void setNegativeCES(int negativeCES) {
		this.negativeCES = negativeCES;
	}

	public int getNeutralCES() {
		return neutralCES;
	}

	public void setNeutralCES(int neutralCES) {
		this.neutralCES = neutralCES;
	}

	public int getPositiveCES() {
		return positiveCES;
	}

	public void setPositiveCES(int positiveCES) {
		this.positiveCES = positiveCES;
	}

	public int getTotalNPS() {
		return totalNPS;
	}

	public void setTotalNPS(int totalNPS) {
		this.totalNPS = totalNPS;
	}
	
	public Map<String, ScoreCX> getTimeseriesNPS() {
		return timeseriesNPS;
	}

	public void setTimeseriesNPS(Map<String, ScoreCX> timeseriesNPS) {
		this.timeseriesNPS.putAll(timeseriesNPS);
	}
	
	public void addDataTimeseriesNPS(String datetimeKey, ScoreCX nps) {
		this.timeseriesNPS.put(datetimeKey, nps);
	}

	/**
	 * @return RFM score 
	 */
	public int getRfmScore() {
		return rfmScore;
	}

	/**
	 * @param RFM score 
	 */
	public void setRfmScore(int rfmScore) {
		this.rfmScore = rfmScore;
	}
	
	public void setRfmScore(String rfmScore) {
		this.rfmScore = StringUtil.safeParseInt(rfmScore);
	}
	
	/**
	 * @return Churn score
	 */
	public double getChurnScore() {
		return churnScore;
	}

	/**
	 * @param Churn score
	 */
	public void setChurnScore(double churnScore) {
		this.churnScore = churnScore;
	}
	
	public void setChurnScore(String churnScore) {
		this.churnScore = StringUtil.safeParseDouble(churnScore);
	}

	public int getRfeScore() {
		return rfeScore;
	}

	public void setRfeScore(int rfeScore) {
		this.rfeScore = rfeScore;
	}
	
	public void setRfeScore(String rfeScore) {
		this.rfeScore = StringUtil.safeParseInt(rfeScore);
	}

	public Map<String, Integer> getTimeseriesDataQualityScore() {
		return timeseriesDataQualityScore;
	}

	public void setTimeseriesDataQualityScore(Map<String, Integer> timeseriesDataQualityScore) {
		this.timeseriesDataQualityScore = timeseriesDataQualityScore;
	}
	
	public void addTimeseriesDataQualityScore(String dateKey, int score) {
		this.timeseriesDataQualityScore.put(dateKey, score);
	}

	public Map<String, Integer> getTimeseriesLeadScore() {
		return timeseriesLeadScore;
	}

	public void setTimeseriesLeadScore(Map<String, Integer> timeseriesLeadScore) {
		this.timeseriesLeadScore = timeseriesLeadScore;
	}
	
	public void addTimeseriesLeadScore(String dateKey, int score) {
		this.timeseriesLeadScore.put(dateKey, score);
	}

	public Map<String, Integer> getTimeseriesProspectScore() {
		return timeseriesProspectScore;
	}

	public void setTimeseriesProspectScore(Map<String, Integer> timeseriesProspectScore) {
		this.timeseriesProspectScore.putAll(timeseriesProspectScore);;
	}
	
	public void addTimeseriesProspectScore(String dateKey, int score) {
		this.timeseriesProspectScore.put(dateKey, score);
	}

	public Map<String, Integer> getTimeseriesEngagementScore() {
		return timeseriesEngagementScore;
	}

	public void setTimeseriesEngagementScore(Map<String, Integer> timeseriesEngagementScore) {
		this.timeseriesEngagementScore.putAll(timeseriesEngagementScore);
	}
	
	public void addTimeseriesEngagementScore(String dateKey, int score) {
		this.timeseriesEngagementScore.put(dateKey, score);
	}

	public Map<String, Double> getTimeseriesTransactionValue() {
		return timeseriesTransactionValue;
	}

	public void setTimeseriesTransactionValue(Map<String, Double> timeseriesTransactionValue) {
		this.timeseriesTransactionValue.putAll(timeseriesTransactionValue);
	}
	
	public void addTimeseriesTransactionValue(String dateKey, double score) {
		this.timeseriesTransactionValue.put(dateKey, score);
	}

	public Map<String, Integer> getTimeseriesCreditScore() {
		return timeseriesCreditScore;
	}

	public void setTimeseriesCreditScore(Map<String, Integer> timeseriesCreditScore) {
		this.timeseriesCreditScore.putAll(timeseriesCreditScore);
	}
	
	public void addTimeseriesCreditScore(String dateKey, int score) {
		this.timeseriesCreditScore.put(dateKey, score);
	}

	public Map<String, Integer> getTimeseriesLoyaltyPoint() {
		return timeseriesLoyaltyPoint;
	}

	public void setTimeseriesLoyaltyPoint(Map<String, Integer> timeseriesLoyaltyPoint) {
		this.timeseriesLoyaltyPoint.putAll(timeseriesLoyaltyPoint);
	}
	
	public void addTimeseriesLoyaltyPoint(String dateKey, int score) {
		this.timeseriesLoyaltyPoint.put(dateKey, score);
	}

	public Map<String, Double> getTimeseriesCAC() {
		return timeseriesCAC;
	}

	public void setTimeseriesCAC(Map<String, Double> timeseriesCAC) {
		this.timeseriesCAC.putAll(timeseriesCAC);
	}
	
	public void addTimeseriesCAC(String dateKey, double score) {
		this.timeseriesCAC.put(dateKey, score);
	}

	public Map<String, Double> getTimeseriesCLV() {
		return timeseriesCLV;
	}

	public void setTimeseriesCLV(Map<String, Double> timeseriesCLV) {
		this.timeseriesCLV.putAll(timeseriesCLV);
	}
	
	public void addTimeseriesCLV(String dateKey, double score) {
		this.timeseriesCLV.put(dateKey, score);
	}

	public Map<String, ScoreCX> getTimeseriesCFS() {
		return timeseriesCFS;
	}

	public void setTimeseriesCFS(Map<String, ScoreCX> timeseriesCFS) {
		this.timeseriesCFS.putAll(timeseriesCFS); 
	}
	
	public void addTimeseriesCFS(String dateKey, ScoreCX score) {
		this.timeseriesCFS.put(dateKey, score);
	}

	public int getNegativeCFS() {
		return negativeCFS;
	}

	public void setNegativeCFS(int negativeCFS) {
		this.negativeCFS = negativeCFS;
	}

	public int getNeutralCFS() {
		return neutralCFS;
	}

	public void setNeutralCFS(int neutralCFS) {
		this.neutralCFS = neutralCFS;
	}

	public int getPositiveCFS() {
		return positiveCFS;
	}

	public void setPositiveCFS(int positiveCFS) {
		this.positiveCFS = positiveCFS;
	}

	public Map<String, ScoreCX> getTimeseriesCSAT() {
		return timeseriesCSAT;
	}

	public void setTimeseriesCSAT(Map<String, ScoreCX> timeseriesCSAT) {
		this.timeseriesCSAT.putAll(timeseriesCSAT);
	}
	
	public void addTimeseriesCSAT(String dateKey, ScoreCX score) {
		this.timeseriesCSAT.put(dateKey, score);
	}

	public int getNegativeCSAT() {
		return negativeCSAT;
	}

	public void setNegativeCSAT(int negativeCSAT) {
		this.negativeCSAT = negativeCSAT;
	}

	public int getNeutralCSAT() {
		return neutralCSAT;
	}

	public void setNeutralCSAT(int neutralCSAT) {
		this.neutralCSAT = neutralCSAT;
	}

	public int getPositiveCSAT() {
		return positiveCSAT;
	}

	public void setPositiveCSAT(int positiveCSAT) {
		this.positiveCSAT = positiveCSAT;
	}

	public int getNegativeNPS() {
		return negativeNPS;
	}

	public void setNegativeNPS(int negativeNPS) {
		this.negativeNPS = negativeNPS;
	}

	public int getNeutralNPS() {
		return neutralNPS;
	}

	public void setNeutralNPS(int neutralNPS) {
		this.neutralNPS = neutralNPS;
	}

	public int getPositiveNPS() {
		return positiveNPS;
	}

	public void setPositiveNPS(int positiveNPS) {
		this.positiveNPS = positiveNPS;
	}

	public Map<String, Integer> getTimeseriesReach() {
		return timeseriesReach;
	}

	public void setTimeseriesReach(Map<String, Integer> timeseriesReach) {
		this.timeseriesReach.putAll(timeseriesReach);;
	}
	
	public void addTimeseriesReach(String dateKey, int reach) {
		this.timeseriesReach.put(dateKey, reach);
	}
	
	public Set<String> getBehavioralEvents() {
		return behavioralEvents;
	}

	public void setBehavioralEvents(Set<String> behavioralEvents) {
		behavioralEvents.forEach(e->{
			this.behavioralEvents.add(e);
		});
	}
	
	public void setBehavioralEvent(String eventName) {
		this.behavioralEvents.add(eventName);
	}
	
	public void resetBehavioralEvent() {
		this.behavioralEvents.clear();
	}

	public Set<FinanceEvent> getPaymentEvents() {
		return paymentEvents;
	}

	public void setPaymentEvents(Set<FinanceEvent> paymentEvents) {
		if(paymentEvents != null) {
			paymentEvents.forEach(e->{
				this.paymentEvents.add(e);
			});
		}
	}

	public Map<String, Date> getFunnelStageTimeline() {
		return funnelStageTimeline;
	}

	public void setFunnelStageTimeline(Map<String, Date> funnelStageTimeline) {
		this.funnelStageTimeline.putAll(funnelStageTimeline);
	}
	
	public boolean containFunnelStageTimeline(String journeyId, String funnelStageName) {
		String key = journeyId + "-" +funnelStageName;
		return this.funnelStageTimeline.containsKey(key);
	}
	
	public boolean updateFunnelStageTimeline(String journeyId, String funnelStageName) {
		String key = journeyId + "-" +funnelStageName;
		if(! this.funnelStageTimeline.containsKey(key) ) {
			this.funnelStageTimeline.put(key, new Date());
			return true;
		}
		return false;
	}
	
	public void clearFunnelStageTimeline() {
		this.funnelStageTimeline.clear();
	}

	public int getDataContext() {
		return dataContext;
	}

	public void setDataContext(int dataContext) {
		this.dataContext = dataContext;
	}
	
	public String getLastSeenIp() {
		return lastSeenIp;
	}

	public void setLastSeenIp(String lastSeenIp) {
		if(StringUtil.isNotEmpty(lastSeenIp)) {
			this.lastSeenIp = lastSeenIp;
		}
	}
	
	public String getLastObserverId() {
		return lastObserverId;
	}

	public void setLastObserverId(String observerId) {
		if(StringUtil.isNotEmpty(observerId)) {
			this.lastObserverId = observerId;
		}
	}
	
	public void setObserverAndTouchpointHub(String observerId, String touchpointHubId) {
		setLastObserverId(observerId);
		setFromTouchpointHubId(touchpointHubId);
	}
	
	public void setObserverAndTouchpointHub(String observerId, TouchpointHub tpHub) {
		setLastObserverId(observerId);
		if(tpHub != null) {
			setFromTouchpointHubId(tpHub.getId());
		}
	}
	
	public void setLastTouchpointId(String lastTouchpointId) {
		if(StringUtil.isNotEmpty(lastTouchpointId)) {
			this.lastTouchpointId = lastTouchpointId;
		}
	}
	
	public Set<String> getTopEngagedTouchpointIds() {
		return topEngagedTouchpointIds;
	}
	
	public void setTopEngagedTouchpointIds(Set<String> topEngagedTouchpointIds) {
		if(topEngagedTouchpointIds != null) {
			this.topEngagedTouchpointIds.addAll(topEngagedTouchpointIds);	
		}		
	}

	public Set<String> getFromTouchpointHubIds() {
		return fromTouchpointHubIds;
	}

	public void setFromTouchpointHubIds(Set<String> list) {
		if(list != null) {
			list.forEach(e->{
				this.fromTouchpointHubIds.add(e);
			});
		}
	}
	
	public void setFromTouchpointHubId(String fromTouchpointHubId) {
		if(StringUtil.isNotEmpty(fromTouchpointHubId)) {
			this.fromTouchpointHubIds.add(fromTouchpointHubId);
		}
	}

	public String getLastTouchpointId() {
		return lastTouchpointId;
	}
	
	public void setLastTouchpoint(Touchpoint lastTouchpoint) {
		if(lastTouchpoint != null) {
			this.lastTouchpoint = lastTouchpoint;	
		}		
	}
	
	public Touchpoint getLastTouchpoint() {
		return lastTouchpoint;
	}
	
	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		if(StringUtil.isNotEmpty(notes)) {
			this.notes = notes;
		}
	}
	
	public Set<GoogleUTM> getGoogleUtmData() {
		return googleUtmData;
	}

	public void setGoogleUtmData(Set<GoogleUTM> googleUtmData) {
		this.googleUtmData = googleUtmData;
	}
	
	public void setGoogleUtmData(GoogleUTM googleUtmData) {
		if(googleUtmData != null) {
			if(!googleUtmData.isEmpty()) {
				this.googleUtmData.add(googleUtmData);
			}
		}
	}
	
	public void setGoogleUtmData(String googleUtmData) {
		if(StringUtil.isNotEmpty(googleUtmData)) {
			GoogleUTM utm = new Gson().fromJson(googleUtmData, GoogleUTM.class);
			if(!utm.isEmpty()) {
				this.googleUtmData.add(utm);
			}
		}
	}

	public void appendNotes(String notes) {
		if(StringUtil.isNotEmpty(notes)) {
			try {
				String[] toks = notes.split(PROFILE_NOTES_NEW_LINE);
				StringBuilder s = new StringBuilder();
				
				if(StringUtil.isNotEmpty(this.notes)) {
					s.append(this.notes);
				}
				
				int c = 0;
				for (String string : toks) {
					String note = string.trim();
					if(StringUtil.isNotEmpty(note)) {
						s.append(note).append(StringPool.NEW_LINE);
					}
					c++;
					if(c > PROFILE_NOTES_MAX_LENGTH) {
						break;
					}
				}
				this.notes = StringUtils.abbreviate(s.toString(), ".", PROFILE_NOTES_MAX_LENGTH) ;
			} catch (Exception e) {}
		}
	}
	
	
	
    /////////////////////////////////////////////////////////////////////
	//////////////////////////// utility methods  //////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	
	
	public String getIdentitiesAsStr() {
		return identitiesAsStr;
	}

	public void setIdentitiesAsStr(String identitiesAsStr) {
		this.identitiesAsStr = identitiesAsStr;
	}

	public String getDataLabelsAsStr() {
		return dataLabelsAsStr;
	}

	public void setDataLabelsAsStr(String dataLabelsAsStr) {
		this.dataLabelsAsStr = dataLabelsAsStr;
	}

	public String getInCampaignsAsStr() {
		return inCampaignsAsStr;
	}

	public void setInCampaignsAsStr(String inCampaignsAsStr) {
		this.inCampaignsAsStr = inCampaignsAsStr;
	}

	public String getInSegmentsAsStr() {
		return inSegmentsAsStr;
	}

	public void setInSegmentsAsStr(String inSegmentsAsStr) {
		this.inSegmentsAsStr = inSegmentsAsStr;
	}

	public String getInAccountsAsStr() {
		return inAccountsAsStr;
	}

	public void setInAccountsAsStr(String inAccountsAsStr) {
		this.inAccountsAsStr = inAccountsAsStr;
	}

	public String getInJourneyMapsAsStr() {
		return inJourneyMapsAsStr;
	}

	public void setInJourneyMapsAsStr(String inJourneyMapsAsStr) {
		this.inJourneyMapsAsStr = inJourneyMapsAsStr;
	}


	/**
	 * this method is called when doing data de-duplication
	 * 
	 * @return TotalValue
	 */
	public final long getTotalValueOfData() {
		return ProfileModelUtil.computeTotalValueOfProfileData(this);
	}
	
	public final boolean hasTransactionalData(String journeyId) {
		long purchase = this.getEventCount(journeyId, BehavioralEvent.Commerce.PURCHASE);
		long subscribe = this.getEventCount(journeyId, BehavioralEvent.Commerce.SUBSCRIBE);
		long repurchase = this.getEventCount(journeyId, BehavioralEvent.Commerce.REPURCHASE);
		long enrolled = this.getEventCount(journeyId, BehavioralEvent.Education.ENROLL);
		long pay = this.getEventCount(journeyId, BehavioralEvent.Commerce.MADE_PAYMENT);
		boolean check = purchase > 0 || subscribe > 0 || repurchase > 0 || enrolled > 0 || pay > 0;
		return check;
	}
	
	public final boolean hasManyTransactions(String journeyId) {
		long purchase = this.getEventCount(journeyId, BehavioralEvent.Commerce.PURCHASE);
		long subscribe = this.getEventCount(journeyId, BehavioralEvent.Commerce.SUBSCRIBE);
		long repurchase = this.getEventCount(journeyId, BehavioralEvent.Commerce.REPURCHASE);
		long enrolled = this.getEventCount(journeyId, BehavioralEvent.Education.ENROLL);
		long pay = this.getEventCount(journeyId, BehavioralEvent.Commerce.MADE_PAYMENT);
		boolean check = purchase > 1 || subscribe > 1 || repurchase > 1 || enrolled > 1 || pay > 1;
		return check;
	}
	
	public final boolean hasFeedbackData(String journeyId) {
		long rating = this.getEventCount(journeyId, BehavioralEvent.Feedback.SUBMIT_RATING_FORM);
		long csat = this.getEventCount(journeyId, BehavioralEvent.Feedback.SUBMIT_CSAT_FORM);
		long ces = this.getEventCount(journeyId, BehavioralEvent.Feedback.SUBMIT_CES_FORM);
		long nps = this.getEventCount(journeyId, BehavioralEvent.Feedback.SUBMIT_NPS_FORM);
		boolean check = (rating > 0 || csat > 0 || nps > 0 || ces > 0);
		return check;
	}
	
	public abstract boolean hasContactData();
	
	public abstract boolean checkToDeleteForever();
	
	public abstract boolean recommendProductItems(AssetTemplate assetTemplate, List<ProductItem> productItems);
	
	public abstract boolean recommendContentItems(AssetTemplate assetTemplate, List<AssetContent> contentItems);
	

	/**
	 * @param funnelStage
	 */
	public final int classifyFunnelStageIndex(DataFlowStage funnelStage, JourneyMapRefKey journeyRef, ScoreCX scoreCX) {
		return ProfileModelUtil.classifyFunnelStageIndex(this, funnelStage, journeyRef, scoreCX);
	}
	
	public final boolean hasViewActivities(String journeyId) {
		long pageView = this.getEventCount(journeyId, BehavioralEvent.General.PAGE_VIEW);
		long itemView = this.getEventCount(journeyId, BehavioralEvent.General.ITEM_VIEW);
		long contentView = this.getEventCount(journeyId, BehavioralEvent.General.CONTENT_VIEW);
		return pageView > 0 || itemView > 0 || contentView > 0;
	}
	
	public final boolean hasActiveViewActivities(String journeyId) {
		long pageView = this.getEventCount(journeyId, BehavioralEvent.General.PAGE_VIEW);
		long itemView = this.getEventCount(journeyId, BehavioralEvent.General.ITEM_VIEW);
		long contentView = this.getEventCount(journeyId, BehavioralEvent.General.CONTENT_VIEW);
		return pageView > 10 || itemView > 10 || contentView > 10;
	}
	
	public final boolean hasItemViewData(String journeyId) {
		long itemView = this.getEventCount(journeyId, BehavioralEvent.General.ITEM_VIEW);
		return itemView > 0;
	}
	
	// key event summary statistics
	public synchronized Map<String, Long> getEventStatistics() {
		if(eventStatistics == null) {
			eventStatistics = new HashMap<String, Long>(0);
		}
		return eventStatistics;
	}
	
	public synchronized long getTotalActivityEvents() {
		if(eventStatistics != null) {
			return eventStatistics.values().stream().collect(Collectors.summingLong(Long::longValue));
		}
		return 0;
	}

	public synchronized void setEventStatistics(Map<String, Long> totalEventStatistics) {
		this.eventStatistics = totalEventStatistics;
	}
	
	public synchronized void updateEventCount(String journeyId, String eventName) {
		long eventCount = getEventCount(journeyId, eventName) + 1;
		String key = journeyId + "-" + eventName;
		this.eventStatistics.put(key, eventCount);
		
		// for real-time segmentation
		this.setBehavioralEvent(eventName);
	}
	
	public synchronized long getEventCount(String journeyId, String eventName) {
		String key = journeyId + "-" + eventName;
		return this.eventStatistics.getOrDefault(key, 0L);
	}
	
	public synchronized void clearEventStatistics() {
		this.journeyScore = 0;
	
		this.totalEstimatedReach = 0;
		
		this.totalLeadScore = 0;
		this.timeseriesLeadScore.clear();
		
		this.totalProspectScore = 0;
		this.timeseriesProspectScore.clear();
		
		this.totalEngagementScore = 0;
		this.timeseriesEngagementScore.clear();
		
		this.totalTransactionValue = 0;
		this.timeseriesTransactionValue.clear();
		
		this.totalLoyaltyScore = 0;
		this.timeseriesLoyaltyPoint.clear();
		
		this.totalCreditScore = 0;
		this.timeseriesCreditScore.clear();
		
		this.totalCAC = 0;
		this.timeseriesCAC.clear();
		
		this.totalCLV = 0;
		this.timeseriesCLV.clear();
		
		this.dataQualityScore = 0;
		this.totalLoyaltyScore = 0;
		
		this.totalCFS = 0;
		this.timeseriesCFS.clear();
		
		this.totalCES = 0;
		this.timeseriesCES.clear();
		
		this.totalCSAT = 0;
		this.timeseriesCSAT.clear();
		
		this.totalNPS = 0;
		this.timeseriesNPS.clear();
		
		this.financeRecords.clear();
		this.eventStatistics.clear();
	}
	

	
	public void clearAllReportData() {
		this.resetBehavioralEvent();
		this.clearFunnelStageTimeline();
		this.clearEventStatistics();
		this.clearInJourneyMaps();
	}
	
	/**
	 * Validates the length of the given input against a given rule
	 * 
	 * @param s
	 * @return true if s < MAXIMUM_TEXT_LENGTH and is not empty string
	 */
	public static final boolean isValidLength(String s) {
		if(StringUtil.isNotEmpty(s)) {
			return s.trim().length() < MAXIMUM_TEXT_LENGTH;
		}
		return false;
	}
	
	/**
	 * @param set
	 * @return
	 */
	public static final String transformSetOfRefKeysToStr(Set<? extends RefKey> set) {
		return set.stream().map(c->{return c.getName();}).collect(Collectors.joining(" ; ")).trim();
	}
	
	/**
	 * @param set
	 * @return
	 */
	public static final String transformSetOfStrToStr(Set<String> set) {
		return set.stream().collect(Collectors.joining(" ; ")).trim();
	}

	public String getUpdateByKey() {
		return updateByKey;
	}

	public void setUpdateByKey(String updateByKey) {
		this.updateByKey = updateByKey;
	}

	public boolean isDeduplicate() {
		return deduplicate;
	}

	public void setDeduplicate(boolean deduplicate) {
		this.deduplicate = deduplicate;
	}
	
	public boolean isInsertingData() {
		return StringUtil.isEmpty(this.updateByKey);
	}
}
