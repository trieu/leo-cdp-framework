package leotech.cdp.model.customer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.activation.ActivationRule;
import leotech.cdp.model.analytics.ConversionReport;
import leotech.cdp.model.analytics.PerformanceReport;
import leotech.cdp.query.SegmentQuery;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * A segment is the group of profiles, built using query builder (AQL)
 * 
 * @author tantrieu31
 * @since 2020-08-27
 */
public final class Segment extends PersistentObject implements Comparable<Segment> {
	
	public final static class SegmentRef {
		@Expose
		String id;
		
		@Expose
		String name = ""; 
		
		@Expose
		long totalCount = 0; 
		
		public SegmentRef() {
			// gson
		}
	}
	
	public static final int STATUS_DELETED = -4; // 
	public static final int STATUS_INACTIVE_QUERY = 0;// for reporting and analytics on-demand
	public static final int STATUS_ACTIVE_QUERY = 1;// for actively query into cached profiles
	public static final int STATUS_ACTIVATED = 2; // activated by automated campaigns
	
	public static final String COLLECTION_NAME = getCdpCollectionName(Segment.class);
	static ArangoCollection dbCol;

	@Override
	public ArangoCollection getDbCollection() {
		if (dbCol == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();
			dbCol = arangoDatabase.collection(COLLECTION_NAME);
			
			// ensure indexing key fields for fast lookup
			dbCol.ensurePersistentIndex(Arrays.asList("indexScore"), new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("name"), new PersistentIndexOptions().unique(false) );
			dbCol.ensurePersistentIndex(Arrays.asList("type"), new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("ownerUsername"), new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("maintainerUsername"), new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("keywords[*]"), new PersistentIndexOptions().unique(false));
		}
		if(dbCol != null) {
			return dbCol;
		}
		throw new NullPointerException("dbCol is NULL, can not update " + COLLECTION_NAME);
	}

	@Key
	@Expose
	String id;
	
	@Expose
	String parentId;

	@Expose
	String name = ""; 
	
	@Expose
	String description = ""; 
	
	@Expose
	String imageAvatarUri = ""; 
	
	@Expose
	int type = SegmentType.AD_HOC_QUERY;

	@Expose
	int status = STATUS_ACTIVE_QUERY;
	
	@Expose
	boolean realtimeQuery = false;
	
	@Expose
	boolean autoUpdateProfiles = true;

	/**
	 * check rules_basic at https://querybuilder.js.org/assets/demo-basic.js <br>
	 * JSON rules for jQuery QueryBuilder https://github.com/USPA-Technology/visual-query-parser
	 */
	@Expose
	String jsonQueryRules;
	
	/**
	 * Custom ArangoDB Query to filter data
	 */
	@Expose
	String customQueryFilter = "";
	
	@Expose
	String queryHashedId;

	@Expose
	// default fields to expose for report !?
	List<String> selectedFields = new ArrayList<String>();
	
	/**
	 * segment size or total profile in segment
	 */
	@Expose
	long totalCount = 0; 
	
	@Expose
	int totalDataQualityScore = 0;
	
	@Expose
	long totalLeadScore = 0; 
	
	@Expose
	long totalProspectScore = 0; 
	
	@Expose
	long totalEngagementScore = 0;
	
	@Expose
	double totalTransactionValue = 0;
	
	@Expose
	long totalCreditScore = 0;
	
	@Expose
	double totalCAC = 0; 
	
	@Expose
	double totalCLV = 0; // the total of Customer Lifetime Value from all customer profiles
	
	@Expose
	float avgCFS = 0; // the average of Customer Feedback Score from all customer profiles
	
	@Expose
	float totalCES = 0; // the average of Customer Effort Score from all customer profiles
	
	@Expose
	float avgCSAT = 0; // the average of Customer Satisfaction Score from all customer profiles
	
	@Expose
	float avgNPS = 0; // the average of Net Promoter Score from all customer profiles
	
	@Expose
	List<PerformanceReport> performanceReports = new ArrayList<>();
	
	
	/**
	 * calculated report of  the advertisement response/conversion rate <br>
	 * https://www.quora.com/How-can-I-calculate-the-advertisement-response-conversion-rate
	 */
	@Expose
	ConversionReport conversionReport = null; 

	/**
	 *  for indexing and search
	 */
	@Expose
	Set<String> keywords = new HashSet<String>();

	/**
	 *   media campaigns are used for activating segment
	 */
	@Expose
	List<String> campaignIds = new ArrayList<String>(); 

	/**
	 * extra attributes
	 */
	@Expose
	Map<String, String> extData = new HashMap<>(); 
	
	/**
	 * auto managed by LEO
	 */
	@Expose
	boolean managedBySystem = false; 
	
	/**
	 * to apply sophisticated data processing techniques to yield more information and customer insights
	 */
	@Expose
	boolean forDeepAnalytics = false; // 1
	
	/**
	 * for prediction LTV or important metrics
	 */
	@Expose
	boolean forPredictiveAnalytics = false; // 2
	
	/**
	 * moment of truth connection
	 */
	@Expose
	boolean forPersonalization = false; // 3
	
	/**
	 * Email marketing is the act of sending a commercial message, typically to a group of people, using email. 
	 * In its broadest sense, every email sent to a potential or current customer could be considered email marketing. 
	 * It involves using email to send advertisements, request business, or solicit sales or donations
	 */
	@Expose
	boolean forEmailMarketing = false; // 4
	
	/** 
	 * Real-time marketing is marketing performed "on-the-fly" to determine an appropriate or optimal approach to a particular customer at a particular time and place.
	 */
	@Expose
	boolean forRealtimeMarketing = false; // 5
	
	/** 
	 * Behavioral retargeting is a form of online targeted advertising by which online advertising is targeted to consumers based on their previous Internet actions
	 */
	@Expose
	boolean forReTargeting = false; // 6
	
	/** 
	 * Lookalike targeting is a form of audience targeting that uses lookalike audiences to increase your ROI. 
	 * Lookalike targeting is used when you want to target an audience that is similar to your existing customer base
	 */
	@Expose
	boolean forLookalikeTargeting = false; // 7
	
	/** 
	 * process of establishing consistency among data from a segment in CDP to third-party platform 
	 */
	@Expose
	boolean for3rdSynchronization = false; // 8
	
	/**
	 * who created this segment 
	 */
	@Expose
	String ownerUsername = "";
	
	/**
	 * All SystemUsers who can update data
	 */
	@Expose
	Set<String> authorizedEditors = new HashSet<String>();
	
	/**
	 * All SystemUsers who can view data
	 */
	@Expose
	Set<String> authorizedViewers = new HashSet<String>();
	
	/**
	 *  the ranking index score for order list of segment in activation plan
	 */
	@Expose
	int indexScore = 1; 
	
	@Expose
	protected Date createdAt = new Date();

	@Expose
	protected Date updatedAt = new Date();
	
	@Expose
	protected boolean updateIndex = false;
	
	@Expose
	String exportedFileUrlCsvForAds = ""; 
	
	@Expose
	Date exportedDateForAds = null; 
	
	@Expose
	String exportedFileUrlCsvForExcel = ""; 
	
	@Expose
	Date exportedDateForExcel = null; 
	
	/**
	 * a list of rules to activate data in the segment <br>
	 * skip when storing into database
	 */
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	private List<ActivationRule> activationRules = new ArrayList<>();
	
	
	@Override
	public int compareTo(Segment o) {
		if(o.getIndexScore() < this.getIndexScore()) {
			return 1;
		} else if(o.getIndexScore() > this.getIndexScore()) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean dataValidation() {
		boolean ok = StringUtil.isNotEmpty(this.name) && StringUtil.isNotEmpty(this.jsonQueryRules) && this.selectedFields != null;
		buildHashedId();
		return ok;
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if(StringUtil.isNotEmpty(jsonQueryRules)) {
			String keyHint = this.name + this.jsonQueryRules + this.type + this.ownerUsername;
			this.id = createId(this.id, keyHint);
		}
		else {
			newIllegalArgumentException("jsonQueryRules is required");
		}
		return this.id;
	}
	
	public Segment() {
		// for Gson
	}
	
	public Segment(int indexScore) {
		super();
		this.indexScore = indexScore;
		this.id = "";
		this.selectedFields = ProfileModelUtil.getExposedFieldNamesInSegmentList();
	}
	
	public Segment(String name, String jsonQueryRules, String customQueryFilter) {
		super();
		this.name = name;
		this.jsonQueryRules = jsonQueryRules;
		this.customQueryFilter = customQueryFilter;
		if( StringUtil.isNotEmpty(name) ) {
			buildHashedId();
		} else {
			this.id = "";
		}
		this.selectedFields = ProfileModelUtil.getExposedFieldNamesInSegmentList();
	}
	
	/**
	 * @param name
	 * @param jsonQueryRules
	 */
	public Segment(String name, String jsonQueryRules) {
		super();
		this.name = name;
		this.jsonQueryRules = jsonQueryRules;
		this.customQueryFilter = "";
		if( StringUtil.isNotEmpty(name) ) {
			buildHashedId();
		} else {
			this.id = "";
		}
		this.selectedFields = ProfileModelUtil.getExposedFieldNamesInSegmentList();
	}
	
	public Segment(String name, String jsonQueryRules, List<String> selectedFields) {
		super();
		this.name = name;
		this.jsonQueryRules = jsonQueryRules;
		if( StringUtil.isNotEmpty(name) ) {
			buildHashedId();
		} else {
			this.id = "";
		}
		this.selectedFields = selectedFields;
	}
	
	public Segment(String jsonQueryRules, String customQueryFilter, boolean realtimeQuery) {
		super();
		this.jsonQueryRules = jsonQueryRules;
		this.customQueryFilter = customQueryFilter;
		this.realtimeQuery = realtimeQuery;
		this.buildHashedId();
	}
	
	public SegmentQuery buildQuery() {
		// default query
		return buildQuery(0, 20, true, false);
	}
	
	public SegmentQuery buildQuery(int startIndex, int numberResult) {
		return buildQuery(startIndex, numberResult, this.selectedFields, false, false);
	}
	
	public SegmentQuery buildQuery(int startIndex, int numberResult, boolean realtimeQuery) {
		return buildQuery(startIndex, numberResult, this.selectedFields, realtimeQuery, false);
	}

	public SegmentQuery buildQuery(int startIndex, int numberResult, boolean realtimeQuery, boolean withLastEvents) {
		return buildQuery(startIndex, numberResult, this.selectedFields, realtimeQuery, withLastEvents);
	}
	
	public SegmentQuery buildQuery(int startIndex, int numberResult, List<String> selectedFields, boolean realtimeQuery) {
		return buildQuery(startIndex, numberResult, selectedFields, realtimeQuery, false);
	}
	
	public SegmentQuery buildQueryToIndexProfiles() {
		return buildQuery(0, 50, new ArrayList<String>(0), true, false);
	}
	
	public SegmentQuery buildQuery(int startIndex, int numberResult, List<String> selectedFields, boolean realtimeQuery, boolean withLastEvents) {
		// build query
		String segmentId = this.id;
		SegmentQuery q = new SegmentQuery(this.indexScore, segmentId, this.name, this.jsonQueryRules, this.customQueryFilter, selectedFields, realtimeQuery, withLastEvents);
		this.queryHashedId = q.getQueryHashedId();
		
		// pagination
		q.setStartIndex(startIndex);
		q.setNumberResult(numberResult);
		return q;
	}
	
	public String getQueryHashedId() {
		return queryHashedId;
	}
	
	public void setQueryHashedId(String queryHashedId) {
		this.queryHashedId = queryHashedId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getStatus() {
		return status;
	}
	
	public boolean isDeletedStatus() {
		return status == Segment.STATUS_DELETED;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}

	public int getIndexScore() {
		return indexScore;
	}

	public void setIndexScore(int indexScore) {
		this.indexScore = indexScore;
	}
	
	public Map<String, String> getExtData() {
		return extData;
	}

	public void setExtData(Map<String, String> extData) {
		this.extData = extData;
	}
	
	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public Date getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	public String getId() {
		if(id == null) {
			id = buildHashedId();
		}
		return id;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getJsonQueryRules() {
		return jsonQueryRules;
	}

	public void setJsonQueryRules(String jsonQueryRules) {
		this.jsonQueryRules = jsonQueryRules;
	}

	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
		if(totalCount >=0 ) {
			this.totalCount = totalCount;
		}
	}

	public List<String> getSelectedFields() {
		return selectedFields;
	}

	public void setSelectedFields(List<String> selectedFields) {
		if(selectedFields != null) {
			this.selectedFields = selectedFields;
		}
	}	
	
	public ConversionReport getConversionReport() {
		return conversionReport;
	}

	public void setConversionReport(ConversionReport conversionReport) {
		this.conversionReport = conversionReport;
	}

	public int getTotalActivationCampaign() {
		return campaignIds.size();
	}

	public List<String> getCampaignIds() {
		return campaignIds;
	}
	
	public void addCampaignId(String cid) {
		campaignIds.add(cid);
	}

	public void setCampaignIds(List<String> campaignIds) {
		if(campaignIds != null) {
			this.campaignIds = campaignIds;
		}
	}

	public boolean isManagedBySystem() {
		return managedBySystem;
	}

	public void setManagedBySystem(boolean managedBySystem) {
		this.managedBySystem = managedBySystem;
	}


	public boolean isForPersonalization() {
		return forPersonalization;
	}

	public void setForPersonalization(boolean forPersonalization) {
		this.forPersonalization = forPersonalization;
	}

	public String getOwnerUsername() {
		if(this.ownerUsername == null) {
			return "";
		}
		return ownerUsername;
	}

	public void setOwnerUsername(String ownerUsername) {
		if(StringUtil.isEmpty(this.ownerUsername)) {
			this.ownerUsername = ownerUsername;
		}
	}
	
	public Set<String> getAuthorizedEditors() {
		if(this.authorizedEditors == null) {
			this.authorizedEditors = new HashSet<>();
		}
		return this.authorizedEditors;
	}

	public void setAuthorizedEditors(Set<String> authorizedEditors) {
		if(authorizedEditors != null) {
			this.authorizedEditors = authorizedEditors;
		}
	}

	public void setAuthorizedEditors(String authorizedUsername) {
		this.authorizedEditors.add(authorizedUsername);
	}
	
	public Set<String> getAuthorizedViewers() {
		if(authorizedViewers == null) {
			authorizedViewers = new HashSet<>();
		}
		return authorizedViewers;
	}

	public void setAuthorizedViewers(Set<String> authorizedViewers) {
		if(authorizedViewers != null) {
			this.authorizedViewers = authorizedViewers;
		}
	}
	
	public void setAuthorizedViewer(String authorizedViewer) {
		this.authorizedViewers.add(authorizedViewer);
	}

	public boolean isForDeepAnalytics() {
		return forDeepAnalytics;
	}

	public void setForDeepAnalytics(boolean forDeepAnalytics) {
		this.forDeepAnalytics = forDeepAnalytics;
	}

	public boolean isForPredictiveAnalytics() {
		return forPredictiveAnalytics;
	}

	public void setForPredictiveAnalytics(boolean forPredictiveAnalytics) {
		this.forPredictiveAnalytics = forPredictiveAnalytics;
	}

	public boolean isForEmailMarketing() {
		return forEmailMarketing;
	}

	public void setForEmailMarketing(boolean forEmailMarketing) {
		this.forEmailMarketing = forEmailMarketing;
	}

	public boolean isForRealtimeMarketing() {
		return forRealtimeMarketing;
	}

	public void setForRealtimeMarketing(boolean forRealtimeMarketing) {
		this.forRealtimeMarketing = forRealtimeMarketing;
	}

	public boolean isForReTargeting() {
		return forReTargeting;
	}

	public void setForReTargeting(boolean forReTargeting) {
		this.forReTargeting = forReTargeting;
	}

	public boolean isForLookalikeTargeting() {
		return forLookalikeTargeting;
	}

	public void setForLookalikeTargeting(boolean forLookalikeTargeting) {
		this.forLookalikeTargeting = forLookalikeTargeting;
	}

	public boolean isFor3rdSynchronization() {
		return for3rdSynchronization;
	}

	public void setFor3rdSynchronization(boolean for3rdSynchronization) {
		this.for3rdSynchronization = for3rdSynchronization;
	}
	
	public String getImageAvatarUri() {
		return imageAvatarUri;
	}

	public void setImageAvatarUri(String imageAvatarUri) {
		if(imageAvatarUri != null) {
			this.imageAvatarUri = imageAvatarUri;
		}
	}

	public boolean hasActivationRules() {
		return this.activationRules.size() > 0;
	}
	
	public void clearActivationRules() {
		this.activationRules.clear();
	}
	
	public final Queue<ActivationRule> getActivationRulesAsQueue() {
		return new PriorityQueue<>(this.activationRules);
	}

	public void setActivationRules(List<ActivationRule> activationRules) {
		if(activationRules != null) {
			this.activationRules.addAll(activationRules);
			if(this.hasActivationRules()) {
				this.status = STATUS_ACTIVATED;
			}
		}
	}

	public List<ActivationRule> getActivationRules() {
		if(activationRules == null) {
			activationRules = new ArrayList<ActivationRule>(0);
		}
		return activationRules;
	}
	
	// getter and setter for scoring metrics

	public int getTotalDataQualityScore() {
		return totalDataQualityScore;
	}

	public void setTotalDataQualityScore(int totalDataQualityScore) {
		this.totalDataQualityScore = totalDataQualityScore;
	}

	public long getTotalLeadScore() {
		return totalLeadScore;
	}

	public void setTotalLeadScore(long totalLeadScore) {
		this.totalLeadScore = totalLeadScore;
	}

	public long getTotalProspectScore() {
		return totalProspectScore;
	}

	public void setTotalProspectScore(long totalProspectScore) {
		this.totalProspectScore = totalProspectScore;
	}

	public long getTotalEngagementScore() {
		return totalEngagementScore;
	}

	public void setTotalEngagementScore(long totalEngagementScore) {
		this.totalEngagementScore = totalEngagementScore;
	}

	public double getTotalTransactionValue() {
		return totalTransactionValue;
	}

	public void setTotalTransactionValue(double totalTransactionValue) {
		this.totalTransactionValue = totalTransactionValue;
	}

	public long getTotalCreditScore() {
		return totalCreditScore;
	}

	public void setTotalCreditScore(long totalCreditScore) {
		this.totalCreditScore = totalCreditScore;
	}

	public double getTotalCAC() {
		return totalCAC;
	}

	public void setTotalCAC(double totalCAC) {
		this.totalCAC = totalCAC;
	}

	public double getTotalCLV() {
		return totalCLV;
	}

	public void setTotalCLV(double totalCLV) {
		this.totalCLV = totalCLV;
	}

	public float getAvgCFS() {
		return avgCFS;
	}

	public void setAvgCFS(float avgCFS) {
		this.avgCFS = avgCFS;
	}

	public float getTotalCES() {
		return totalCES;
	}

	public void setTotalCES(float totalCES) {
		this.totalCES = totalCES;
	}

	public float getAvgCSAT() {
		return avgCSAT;
	}

	public void setAvgCSAT(float avgCSAT) {
		this.avgCSAT = avgCSAT;
	}

	public float getAvgNPS() {
		return avgNPS;
	}

	public void setAvgNPS(float avgNPS) {
		this.avgNPS = avgNPS;
	}
	
	public List<PerformanceReport> getPerformanceReports() {
		return performanceReports;
	}

	public void setPerformanceReports(List<PerformanceReport> performanceReports) {
		this.performanceReports = performanceReports;
	}

	public boolean shouldUpdateSize(int minMinutesToUpdateProfiles) {
		return this.getMinutesSinceLastUpdate() > minMinutesToUpdateProfiles && this.autoUpdateProfiles;
	}
	
	public String getExportedFileUrlCsvForAds() {
		return exportedFileUrlCsvForAds;
	}

	public void setExportedFileUrlCsvForAds(String exportedFileUrlCsvForAds) {
		this.exportedFileUrlCsvForAds = exportedFileUrlCsvForAds;
		this.exportedDateForAds = new Date();
	}

	public String getExportedFileUrlCsvForExcel() {
		return exportedFileUrlCsvForExcel;
	}

	public void setExportedFileUrlCsvForExcel(String exportedFileUrlCsvForExcel) {
		this.exportedFileUrlCsvForExcel = exportedFileUrlCsvForExcel;
		this.exportedDateForExcel = new Date();
	}
	
	public Date getExportedDateForAds() {
		return exportedDateForAds;
	}
	
	public Date getExportedDateForExcel() {
		return exportedDateForExcel;
	}
	
	public boolean isUpdateIndex() {
		return updateIndex;
	}

	public void setUpdateIndex(boolean updateIndex) {
		this.updateIndex = updateIndex;
	}
	
	public boolean isRealtimeQuery() {
		return realtimeQuery;
	}

	public void setRealtimeQuery(boolean realtimeQuery) {
		this.realtimeQuery = realtimeQuery;
	}

	public boolean isAutoUpdateProfiles() {
		return autoUpdateProfiles;
	}

	public void setAutoUpdateProfiles(boolean autoUpdateProfiles) {
		this.autoUpdateProfiles = autoUpdateProfiles;
	}

	public String getCustomQueryFilter() {
		return customQueryFilter;
	}

	public void setCustomQueryFilter(String customQueryFilter) {
		this.customQueryFilter = customQueryFilter;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
}