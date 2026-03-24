package leotech.cdp.model.journey;

import java.util.Arrays;
import java.util.Date;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.domain.DataFlowManagement;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * Event metric is meta-data for modeling event data stream as a single atomic
 * object. <br>
 * Event metric is used to define the event data stream and track the customer
 * journey. <br>
 * <br>
 * 
 * ArangoDB collection: cdp_eventmetric
 * 
 * @author Trieu Nguyen (Thomas)
 * @since 2020
 */
public final class EventMetric extends PersistentObject {

	private static final String ADVOCACY = "ADVOCACY";
	private static final String ACTION = "ACTION";
	private static final String ASK = "ASK";
	private static final String ATTRACTION = "ATTRACTION";
	private static final String AWARENESS = "AWARENESS";

	static final String CUSTOMER_ADVOCATE = "customer-advocate";
	static final String ENGAGED_CUSTOMER = "engaged-customer";
	static final String NEW_CUSTOMER = "new-customer";
	static final String PROSPECT = "prospect";

	public static final String COLLECTION_NAME = getCdpCollectionName(EventMetric.class);

	// Volatile for thread-safe lazy initialization
	private static volatile ArangoCollection dbCollection;

	// Cached Slugify to prevent object thrashing and heavy regex recompilation
	private static final Slugify SLUGIFY = new Slugify();

	private static final int MAX_LEN_EVENT_NAME = 50;

	public static final int NO_SCORING = 0;

	// Core metrics
	public static final int SCORING_LEAD_METRIC = 1;
	public static final int SCORING_PROSPECT_METRIC = 2;
	public static final int SCORING_ENGAGEMENT_METRIC = 3;
	public static final int SCORING_DATA_QUALITY_METRIC = 4;
	public static final int SCORING_ACQUISITION_METRIC = 5;
	public static final int SCORING_LIFETIME_VALUE_METRIC = 6;

	// CX metrics
	public static final int SCORING_EFFORT_METRIC = 7;
	public static final int SCORING_SATISFACTION_METRIC = 8;
	public static final int SCORING_FEEDBACK_METRIC = 9;
	public static final int SCORING_PROMOTER_METRIC = 10;

	// Business metrics
	public static final int SCORING_CREDIT_METRIC = 11;
	public static final int SCORING_LOYALTY_METRIC = 12;

	// Evil metric
	public static final int SCORING_DETRACTOR_METRIC = 13;

	// Data types
	public static final int FIRST_PARTY_DATA = 1;
	public static final int SECOND_PARTY_DATA = 2;
	public static final int THIRD_PARTY_DATA = 3;

	// Metric types for 5A Customer Journey Map
	public static final int JOURNEY_STAGE_AWARENESS = 1;
	public static final int JOURNEY_STAGE_ATTRACTION = 2;
	public static final int JOURNEY_STAGE_ASK = 3;
	public static final int JOURNEY_STAGE_ACTION = 4;
	public static final int JOURNEY_STAGE_ADVOCACY = 5;

	@Key
	@Expose
	private String id;

	@Expose
	private String eventName;

	@Expose
	private String eventLabel;

	@Expose
	private Date createdAt;

	@Expose
	private Date updatedAt;

	@Expose
	private int score = 0;

	@Expose
	private int cumulativePoint = 0;

	@Expose
	private int scoreModel = NO_SCORING;

	@Expose
	private int journeyStage = JOURNEY_STAGE_AWARENESS;

	@Expose
	private int dataType = 0;

	@Expose
	private boolean showInObserverJS = false;

	@Expose
	private String funnelStageId;

	@Expose
	private String flowName;

	@Expose
	private boolean systemMetric = false;

	@Expose
	private String journeyMapId = "";

	public EventMetric() {
		// Default for Gson
	}

	// -----------------------------------------------------------------------------------
	// FIX: Constructor Chaining to heavily reduce code duplication (DRY principle)
	// -----------------------------------------------------------------------------------

	public EventMetric(String flowName, String eventName, String eventLabel, int score, int scoreModel, int dataType,
			String funnelStageId, int journeyStage) {
		this(flowName, eventName, eventLabel, score, scoreModel, dataType, funnelStageId, 0, journeyStage, false);
	}

	public EventMetric(String flowName, String eventName, String eventLabel, int score, int scoreModel, int dataType,
			String funnelStageId, int journeyStage, boolean systemMetric) {
		this(flowName, eventName, eventLabel, score, scoreModel, dataType, funnelStageId, 0, journeyStage,
				systemMetric);
	}

	public EventMetric(String flowName, String eventName, String eventLabel, int score, int scoreModel, int dataType,
			String funnelStageId, int cumulativePoint, int journeyStage) {
		this(flowName, eventName, eventLabel, score, scoreModel, dataType, funnelStageId, cumulativePoint, journeyStage,
				true);
	}

	// Master Constructor
	public EventMetric(String flowName, String eventName, String eventLabel, int score, int scoreModel, int dataType,
			String funnelStageId, int cumulativePoint, int journeyStage, boolean systemMetric) {
		initBaseData(eventName);

		this.flowName = flowName;
		this.eventLabel = eventLabel;
		this.score = score;
		this.scoreModel = scoreModel;
		this.funnelStageId = funnelStageId;
		this.cumulativePoint = cumulativePoint;
		this.systemMetric = systemMetric;

		setDataType(dataType);
		setJourneyStage(journeyStage);

		Date now = new Date();
		this.createdAt = now;
		this.updatedAt = now;

		this.buildHashedId();
	}

	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			// FIX: Double-checked locking to prevent index race conditions
			synchronized (EventMetric.class) {
				if (dbCollection == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);

					PersistentIndexOptions uniqueOpts = new PersistentIndexOptions().unique(true);
					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);

					// Fast exact lookup
					col.ensurePersistentIndex(Arrays.asList("eventName"), uniqueOpts);

					// Core UI querying index.
					// Covers ["flowName"], ["flowName", "systemMetric"], and ["flowName",
					// "systemMetric", "dataType"]
					col.ensurePersistentIndex(Arrays.asList("flowName", "systemMetric", "dataType"), pIdxOpts);

					// Used for mapping journey stages
					col.ensurePersistentIndex(Arrays.asList("journeyMapId", "funnelStageId"), pIdxOpts);

					dbCollection = col;
				}
			}
		}
		return dbCollection;
	}

	private void initBaseData(String eventName) {
		if (eventName != null && eventName.length() <= MAX_LEN_EVENT_NAME) {
			this.eventName = eventName.toLowerCase().replaceAll("[^a-z0-9]", "-");
			this.dataType = FIRST_PARTY_DATA;
		} else {
			throw new IllegalArgumentException("Event name must not be null and must have a length less than "
					+ MAX_LEN_EVENT_NAME + " characters");
		}
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (StringUtil.isNotEmpty(this.eventName)) {
			// FIX: Utilizing cached Slugify instance. Removed side-effect of setting
			// createdAt here!
			this.id = SLUGIFY.slugify(this.eventName);
			return this.id;
		} else {
			throw new IllegalArgumentException("eventName is required to save EventMetric");
		}
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(eventName) && StringUtil.isNotEmpty(id);
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public String getEventLabel() {
		return eventLabel;
	}

	public void setEventLabel(String eventLabel) {
		this.eventLabel = eventLabel;
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

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getCumulativePoint() {
		return cumulativePoint;
	}

	public void setCumulativePoint(int cumulativePoint) {
		this.cumulativePoint = cumulativePoint;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		if (dataType > 0 && dataType <= 3) {
			this.dataType = dataType;
		}
	}

	public int getJourneyStage() {
		return journeyStage;
	}

	public String getJourneyStageName() {
		// Java 11 switch statement for cleaner, faster execution
		switch (this.journeyStage) {
		case JOURNEY_STAGE_AWARENESS:
			return AWARENESS;
		case JOURNEY_STAGE_ATTRACTION:
			return ATTRACTION;
		case JOURNEY_STAGE_ASK:
			return ASK;
		case JOURNEY_STAGE_ACTION:
			return ACTION;
		case JOURNEY_STAGE_ADVOCACY:
			return ADVOCACY;
		default:
			return "";
		}
	}

	public void setJourneyStage(int journeyStage) {
		if (journeyStage >= 1 && journeyStage <= 5) {
			this.journeyStage = journeyStage;
		}
	}

	public boolean isScoringForCLV() {
		return this.scoreModel == EventMetric.SCORING_LIFETIME_VALUE_METRIC;
	}

	public boolean isScoreModelForCX() {
		return this.scoreModel == EventMetric.SCORING_SATISFACTION_METRIC
				|| this.scoreModel == EventMetric.SCORING_FEEDBACK_METRIC
				|| this.scoreModel == EventMetric.SCORING_EFFORT_METRIC
				|| this.scoreModel == EventMetric.SCORING_PROMOTER_METRIC;
	}

	public boolean isLeadMetric() {
		return this.scoreModel == SCORING_LEAD_METRIC || this.scoreModel == SCORING_ACQUISITION_METRIC;
	}

	public boolean isProspectiveMetric() {
		return this.scoreModel == SCORING_PROSPECT_METRIC || this.scoreModel == SCORING_ACQUISITION_METRIC;
	}

	public boolean isConversion() {
		DataFlowStage funnelStage = getFunnelStage();
		return isScoringForCLV() && funnelStage != null && funnelStage.isCustomerMetric();
	}

	public final boolean isPurchasingEvent() {
		return isConversion(); // Identical logic as isConversion(), simplified
	}

	public int getScoreModel() {
		return scoreModel;
	}

	public void setScoreModel(int scoreModel) {
		this.scoreModel = scoreModel;
	}

	public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}

	public String getFunnelStageId() {
		return funnelStageId;
	}

	public void setFunnelStageId(String funnelStageId) {
		this.funnelStageId = funnelStageId;
	}

	public DataFlowStage getFunnelStage() {
		return DataFlowManagement.getFunnelStageById(funnelStageId);
	}

	public String getJourneyMapId() {
		return journeyMapId;
	}

	public void setJourneyMapId(String journeyMapId) {
		this.journeyMapId = journeyMapId;
	}

	public boolean isShowInObserverJS() {
		return showInObserverJS;
	}

	public void setShowInObserverJS(boolean showMethodJS) {
		this.showInObserverJS = showMethodJS;
	}

	public boolean isSystemMetric() {
		return systemMetric;
	}

	public void setSystemMetric(boolean systemMetric) {
		this.systemMetric = systemMetric;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}