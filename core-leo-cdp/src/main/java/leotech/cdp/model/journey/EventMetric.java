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
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * @author Trieu Nguyen (Thomas)
 * 
 *  Event metric is meta-data for modeling event data stream as single atomic object
 *
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
	private static ArangoCollection dbCollection;

	private static final int MAX_LEN_EVENT_NAME = 50;
	
	public static final int NO_SCORING = 0;
	
	// core metrics
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
	
	// business metrics
	public static final int SCORING_CREDIT_METRIC = 11;
	public static final int SCORING_LOYALTY_METRIC = 12;
	
	// Evil metric
	public static final int SCORING_DETRACTOR_METRIC = 13;
	
	// data types
	public static final int FIRST_PARTY_DATA = 1;
	public static final int SECOND_PARTY_DATA = 2;
	public static final int THIRD_PARTY_DATA = 3;
	
	// metric types for 5A Customer Journey Map
	// Dr. Philip Kotler, the five stages (Awareness, Attraction, Ask, Action and Advocacy)
	public static final int JOURNEY_STAGE_AWARENESS = 1;
	public static final int JOURNEY_STAGE_ATTRACTION = 2;
	public static final int JOURNEY_STAGE_ASK = 3;
	public static final int JOURNEY_STAGE_ACTION = 4;
	public static final int JOURNEY_STAGE_ADVOCACY = 5;	
	
	@Key
	@Expose
	protected String id;

	@Expose
	protected String eventName;

	@Expose
	protected String eventLabel;

	@Expose
	protected Date createdAt;

	@Expose
	protected Date updatedAt;

	@Expose
	protected int score = 0;
	
	@Expose
	protected int cumulativePoint = 0;

	@Expose
	protected int scoreModel = NO_SCORING;
	
	@Expose
	protected int journeyStage = JOURNEY_STAGE_AWARENESS;

	@Expose
	protected int dataType = 0;
	
	@Expose
	protected boolean showInObserverJS = false;
	
	@Expose
	protected String funnelStageId;
	
	@Expose
	protected String flowName;
	
	@Expose
	protected boolean systemMetric = false;
	
	@Expose
	protected String journeyMapId = "";
	
	@Override
	public ArangoCollection getDbCollection() {
		if (dbCollection == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			dbCollection = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields for fast lookup
			dbCollection.ensurePersistentIndex(Arrays.asList("eventName"),new PersistentIndexOptions().unique(true));
			dbCollection.ensurePersistentIndex(Arrays.asList("dataType"),new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("flowName"),new PersistentIndexOptions().unique(false));

		}
		return dbCollection;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(eventName);
	}

	public EventMetric() {
		// json
	}

	
	public EventMetric(String flowName, String eventName, String eventLabel, int score, int scoreModel, int dataType, String funnelStageId, int journeyStage) {
		super();
		initBaseData(eventName);
		
		this.flowName = flowName;
		this.score = score;
		this.scoreModel = scoreModel;
		this.eventLabel = eventLabel;
		this.funnelStageId = funnelStageId;

		setDataType(dataType);
		setJourneyStage(journeyStage);
	}
	
	public EventMetric(String flowName, String eventName, String eventLabel, int score, int scoreModel, int dataType, String funnelStageId, int journeyStage, boolean systemMetric) {
		super();
		initBaseData(eventName);
		
		this.flowName = flowName;
		this.score = score;
		this.scoreModel = scoreModel;
		this.eventLabel = eventLabel;
		this.funnelStageId = funnelStageId;
		this.systemMetric = systemMetric;

		setDataType(dataType);
		setJourneyStage(journeyStage);
	}
	
	public EventMetric(String flowName, String eventName, String eventLabel, int score, int scoreModel, int dataType,  String funnelStageId, int cumulativePoint, int journeyStage) {
		super();
		initBaseData(eventName);
		
		this.flowName = flowName;
		this.score = score;
		this.cumulativePoint = cumulativePoint;
		this.scoreModel = scoreModel;
		this.eventLabel = eventLabel;
		this.funnelStageId = funnelStageId;
		this.systemMetric = true;
		
		setDataType(dataType);
		setJourneyStage(journeyStage);
	}
	
	public EventMetric(String flowName, String eventName, String eventLabel, int score, int scoreModel, int dataType,  String funnelStageId, int cumulativePoint, int journeyStage, boolean systemMetric) {
		super();
		initBaseData(eventName);
		
		this.flowName = flowName;
		this.score = score;
		this.cumulativePoint = cumulativePoint;
		this.scoreModel = scoreModel;
		this.eventLabel = eventLabel;
		this.funnelStageId = funnelStageId;
		this.systemMetric = systemMetric;
		
		setDataType(dataType);
		setJourneyStage(journeyStage);
	}



	private void initBaseData(String eventName) {
		if (eventName.length() <= MAX_LEN_EVENT_NAME) {
			this.eventName = eventName.toLowerCase().replaceAll("[^a-z0-9]", "-");
			this.dataType = FIRST_PARTY_DATA;
			this.buildHashedId();
		} else {
			throw new IllegalArgumentException(eventName + " must have the length less than 50 characters ");
		}
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if(StringUtil.isNotEmpty(this.eventName)) {
			this.createdAt = new Date();
			this.id = new Slugify().slugify(this.eventName);
		}
		else {
			newIllegalArgumentException("eventName is required to save EventMetric");
		}
		return this.id;
	}

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
		if(journeyStage == JOURNEY_STAGE_AWARENESS) {
			return AWARENESS;
		}
		else if(journeyStage == JOURNEY_STAGE_ATTRACTION) {
			return ATTRACTION;
		}
		else if(journeyStage == JOURNEY_STAGE_ASK) {
			return ASK;
		}
		else if(journeyStage == JOURNEY_STAGE_ACTION) {
			return ACTION;
		}
		else if(journeyStage == JOURNEY_STAGE_ADVOCACY) {
			return ADVOCACY;
		}
		return "";
	}

	public void setJourneyStage(int journeyStage) {
		if(journeyStage>=1 && journeyStage<=5) {
			this.journeyStage = journeyStage;
		}
	}
	
	
	public boolean isItemView() {
		return BehavioralEvent.General.ITEM_VIEW.equals(this.eventName);
	}
	
	public boolean isShortLinkClick() {
		return BehavioralEvent.General.SHORT_LINK_CLICK.equals(this.eventName);
	}
	
	public boolean isView() {
		return this.eventName.contains("view");
	}
	
	public boolean isScoreModelForCLV() {
		return this.scoreModel == EventMetric.SCORING_LIFETIME_VALUE_METRIC;
	}
	
	public boolean isScoreModelForCX() {
		return this.scoreModel == EventMetric.SCORING_SATISFACTION_METRIC || this.scoreModel == EventMetric.SCORING_FEEDBACK_METRIC 
				|| this.scoreModel == EventMetric.SCORING_EFFORT_METRIC || this.scoreModel == EventMetric.SCORING_PROMOTER_METRIC ;
	}
	
	public boolean isLeadMetric() {
		return this.isItemView() || this.isShortLinkClick();
	}
	
	public boolean isProspectiveMetric() {
		return isScoreModelForCLV() && PROSPECT.equals(this.funnelStageId);
	}
	
	public boolean isConversion() {
		return isScoreModelForCLV() && this.score > 0;
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
	
	public DataFlowStage getFunnelStage() {
		return DataFlowManagement.getFunnelStageById(funnelStageId);
	}

	public void setFunnelStageId(String funnelStageId) {
		this.funnelStageId = funnelStageId;
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

	public final boolean isPurchasingEvent() {
		return isConversion() && getFunnelStage().isCustomerMetric();
	}
}
