package leotech.cdp.model.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import leotech.system.util.XssFilterUtil;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * FeedbackData is the object, from ETL process, that transforms a Tracking
 * Event. <br>
 * A FeedbackData can be a survey response, a customer review, a rating, a
 * comment, a satisfaction score, a net promoter score, a customer effort score,
 * or any other type of feedback data. <br>
 * It can be associated with a specific touch-point, campaign, product, content,
 * or profile. <br>
 * <br>
 * 
 * ArangoDB Collection: cdp_feedbackdata <br>
 * 
 * @author Trieu Nguyen
 * @since 2021
 */
public abstract class FeedbackData extends PersistentObject {

	protected static final String SURVEY_RATING_CHOICES = "Rating_Choices";

	public static final String COLLECTION_NAME = getCdpCollectionName(FeedbackData.class);

	// FIX: Volatile for thread-safe lazy initialization
	private static volatile ArangoCollection dbCollection;

	public static ArangoCollection dbCollection() {
		if (dbCollection == null) {
			// FIX: Double-checked locking to prevent index race conditions
			synchronized (FeedbackData.class) {
				if (dbCollection == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);
					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);

					// --------------------------------------------------------------------------------
					// ARANGODB 3.11 INDEX OPTIMIZATION (RocksDB Engine)
					// Removed 5 separate redundant indices that appended "createdAt" at the end.
					// Using Left-to-Right prefixing, we combined them into broader composite
					// indices
					// which drastically reduces storage size and I/O write times during ETL jobs.
					// --------------------------------------------------------------------------------

					// Used to query all feedback for a specific Template or Profile over time
					col.ensurePersistentIndex(Arrays.asList("refTemplateId", "refProfileId", "createdAt"), pIdxOpts);

					// Used to drill down feedback by Hub -> Touchpoint -> Event -> Time
					col.ensurePersistentIndex(
							Arrays.asList("refTouchpointHubId", "refTouchpointId", "eventName", "createdAt"), pIdxOpts);

					// Used to classify feedback types logically over time
					col.ensurePersistentIndex(Arrays.asList("feedbackType", "createdAt"), pIdxOpts);

					dbCollection = col;
				}
			}
		}
		return dbCollection;
	}

	@Override
	public ArangoCollection getDbCollection() {
		return dbCollection();
	}

	@Key
	@Expose
	protected String id;

	@Expose
	protected String feedbackDataType = "";

	// ========================================================================
	// REFERENCE METADATA
	// ========================================================================

	@Expose
	protected String refTemplateId = "";

	@Expose
	protected String refProductItemId = "";

	@Expose
	protected String refContentItemId = "";

	@Expose
	protected String refTouchpointHubId = "";

	@Expose
	protected String refTouchpointId = "";

	@Expose
	protected String refCampaignId = "";

	@Expose
	protected String refDataFlowStageId = "";

	// ========================================================================
	// FEEDBACK CLASSIFICATION
	// ========================================================================

	@Expose
	protected String feedbackType = "";

	@Expose
	protected ScoreCX scoreCX = new ScoreCX();

	// ========================================================================
	// SURVEY FIELDS
	// ========================================================================

	@Expose
	protected String header = ""; // header text

	@Expose
	protected String group = ""; // taxonomy, e.g., Computer Science, brand, or category

	@Expose
	protected String evaluatedObject = ""; // abstract entity like a course, a product, or a service

	@Expose
	protected String evaluatedItem = ""; // physical entity like a lecture, video, URL, place, or touch-point

	@Expose
	protected String evaluatedPerson = ""; // a teacher, a sales agent

	@Expose
	protected String surveyChoicesId;

	@Expose
	protected List<SurveyChoice> surveyChoices;

	@Expose
	protected String timePeriod = "";

	@Expose
	protected Date createdAt = new Date();

	public FeedbackData() {
		// Default constructor for Gson
	}

	public void cleanXss() {
		XssFilterUtil.cleanAllHtmlTags(this, FeedbackData.class);
	}

	public static String getCollectionName() {
		return COLLECTION_NAME;
	}

	/**
	 * Parses a Vert.x JsonObject to extract Survey Choices and their assigned score
	 * weights.
	 * 
	 * @param obj the raw Vert.x JsonObject
	 */
	protected void parseSurveyChoices(JsonObject obj) {
		JsonArray jsonArray = obj.getJsonArray(SURVEY_RATING_CHOICES);
		if (jsonArray != null && !jsonArray.isEmpty()) {
			int size = jsonArray.size();
			this.surveyChoices = new ArrayList<>(size);

			for (Object object : jsonArray) {
				String choice = StringUtil.safeString(String.valueOf(object)).trim();

				// FIX: Added safe length checks to prevent index out-of-bounds exceptions on
				// malformed payloads
				int startingIndex = choice.indexOf('[');
				int closingIndex = choice.indexOf(']');

				if (startingIndex >= 0 && closingIndex > startingIndex) {
					String rawScore = choice.substring(startingIndex + 1, closingIndex);
					int choiceScore = StringUtil.safeParseInt(rawScore);
					this.surveyChoices.add(new SurveyChoice(choice, choiceScore, 0));
				}
			}

			// Generate deterministic Hash ID for these specific survey choices
			this.surveyChoicesId = createHashedId(this.surveyChoices.toString());
		} else {
			this.surveyChoices = new ArrayList<>(0);
		}
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	@Override
	public final Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public final void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public Date getUpdatedAt() {
		return createdAt;
	}

	@Override
	public final void setUpdatedAt(Date updatedAt) {
		/* skip */ }

	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.createdAt);
	}

	public final String getId() {
		return id;
	}

	public final void setId(String id) {
		this.id = id;
	}

	public String getFeedbackDataType() {
		return feedbackDataType;
	}

	public void setFeedbackDataType(String feedbackDataType) {
		this.feedbackDataType = feedbackDataType;
	}

	public final String getRefTemplateId() {
		return refTemplateId;
	}

	public final void setRefTemplateId(String refTemplateId) {
		this.refTemplateId = refTemplateId;
	}

	public String getRefProductItemId() {
		return refProductItemId;
	}

	public void setRefProductItemId(String refProductItemId) {
		this.refProductItemId = refProductItemId;
	}

	public String getRefContentItemId() {
		return refContentItemId;
	}

	public void setRefContentItemId(String refContentItemId) {
		this.refContentItemId = refContentItemId;
	}

	public String getRefTouchpointHubId() {
		return refTouchpointHubId;
	}

	public void setRefTouchpointHubId(String refTouchpointHubId) {
		this.refTouchpointHubId = refTouchpointHubId;
	}

	public String getRefTouchpointId() {
		return refTouchpointId;
	}

	public void setRefTouchpointId(String refTouchpointId) {
		this.refTouchpointId = refTouchpointId;
	}

	public String getRefCampaignId() {
		return refCampaignId;
	}

	public void setRefCampaignId(String refCampaignId) {
		this.refCampaignId = refCampaignId;
	}

	public String getRefDataFlowStageId() {
		return refDataFlowStageId;
	}

	public void setRefDataFlowStageId(String refDataFlowStageId) {
		this.refDataFlowStageId = refDataFlowStageId;
	}

	public String getFeedbackType() {
		return feedbackType;
	}

	public void setFeedbackType(String feedbackType) {
		this.feedbackType = feedbackType;
	}

	public ScoreCX getScoreCX() {
		return scoreCX;
	}

	public void setScoreCX(ScoreCX scoreCX) {
		this.scoreCX = scoreCX;
	}

	public final String getHeader() {
		return header;
	}

	public final void setHeader(String header) {
		this.header = header;
	}

	public final String getGroup() {
		return group;
	}

	public final void setGroup(String group) {
		this.group = group;
	}

	public String getEvaluatedObject() {
		return evaluatedObject;
	}

	public void setEvaluatedObject(String evaluatedObject) {
		this.evaluatedObject = evaluatedObject;
	}

	public String getEvaluatedItem() {
		return evaluatedItem;
	}

	public void setEvaluatedItem(String evaluatedItem) {
		this.evaluatedItem = evaluatedItem;
	}

	public String getEvaluatedPerson() {
		return evaluatedPerson;
	}

	public void setEvaluatedPerson(String evaluatedPerson) {
		this.evaluatedPerson = evaluatedPerson;
	}

	public String getSurveyChoicesId() {
		return surveyChoicesId;
	}

	public void setSurveyChoicesId(String surveyChoicesId) {
		this.surveyChoicesId = surveyChoicesId;
	}

	public List<SurveyChoice> getSurveyChoices() {
		return surveyChoices;
	}

	public String getTimePeriod() {
		return timePeriod;
	}

	public void setTimePeriod(String timePeriod) {
		this.timePeriod = timePeriod;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
}