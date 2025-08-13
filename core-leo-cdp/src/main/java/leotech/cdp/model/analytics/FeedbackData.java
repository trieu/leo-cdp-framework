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
 * FeedbackData is the object, from ETL process that transform Tracking Event
 * (comment or rating)
 * 
 * @author Trieu Nguyen
 * @since 2021
 *
 */
public abstract class FeedbackData extends PersistentObject {
	
	protected static final String SURVEY_RATING_CHOICES = "Rating_Choices";

	public static final String COLLECTION_NAME = getCdpCollectionName(FeedbackData.class);
	private static ArangoCollection dbCollection;
	
	public static ArangoCollection dbCollection() {
		if (dbCollection == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			dbCollection = arangoDatabase.collection(COLLECTION_NAME);
			dbCollection.ensurePersistentIndex(Arrays.asList("refTemplateId","createdAt"),new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("refTouchpointHubId","createdAt"),new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("refProfileId","createdAt"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("feedbackType","createdAt"), new PersistentIndexOptions().unique(false));
			dbCollection.ensurePersistentIndex(Arrays.asList("eventName","createdAt"),new PersistentIndexOptions().unique(false));
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

	// ref metadata

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

	// feedback classification

	@Expose
	protected String feedbackType = "";

	@Expose
	protected ScoreCX scoreCX = new ScoreCX();

	// survey fields

	@Expose
	protected String header = ""; // header text

	@Expose
	protected String group = ""; // taxonomy, Computer Science, a brand or any category

	@Expose
	protected String evaluatedObject = ""; // abstract entity like a course, a product or a service

	@Expose
	protected String evaluatedItem = ""; // physical entity like a lecture, a video, a URL, a place , a touch-point

	@Expose
	protected String evaluatedPerson = "";// a teacher, a sales agent

	@Expose
	protected String surveyChoicesId;
	
	@Expose
	protected List<SurveyChoice> surveyChoices;

	@Expose
	protected String timePeriod = ""; //

	@Expose
	protected Date createdAt = new Date();
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void cleanXss() {
		XssFilterUtil.cleanAllHtmlTags(this, FeedbackData.class);
	}

	public FeedbackData() {
		//
	}

	public static String getCollectionName() {
		return COLLECTION_NAME;
	}

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
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.createdAt);
	}

	@Override
	public final void setUpdatedAt(Date updatedAt) {
		// skip
	}

	public final String getId() {
		return id;
	}

	public final void setId(String id) {
		this.id = id;
	}

	public String getRefProductItemId() {
		return refProductItemId;
	}

	public void setRefProductItemId(String refProductItemId) {
		this.refProductItemId = refProductItemId;
	}

	public String getRefTouchpointHubId() {
		return refTouchpointHubId;
	}

	public void setRefTouchpointHubId(String refTouchpointHubId) {
		this.refTouchpointHubId = refTouchpointHubId;
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

	public final String getRefTemplateId() {
		return refTemplateId;
	}

	public final void setRefTemplateId(String refTemplateId) {
		this.refTemplateId = refTemplateId;
	}

	public String getRefContentItemId() {
		return refContentItemId;
	}

	public void setRefContentItemId(String refContentItemId) {
		this.refContentItemId = refContentItemId;
	}

	public String getFeedbackType() {
		return feedbackType;
	}

	public void setFeedbackType(String feedbackType) {
		this.feedbackType = feedbackType;
	}

	public String getEvaluatedItem() {
		return evaluatedItem;
	}

	public void setEvaluatedItem(String evaluatedItem) {
		this.evaluatedItem = evaluatedItem;
	}


	public String getTimePeriod() {
		return timePeriod;
	}

	public void setTimePeriod(String timePeriod) {
		this.timePeriod = timePeriod;
	}

	public String getEvaluatedObject() {
		return evaluatedObject;
	}

	public void setEvaluatedObject(String evaluatedObject) {
		this.evaluatedObject = evaluatedObject;
	}


	public String getEvaluatedPerson() {
		return evaluatedPerson;
	}

	public void setEvaluatedPerson(String evaluatedPerson) {
		this.evaluatedPerson = evaluatedPerson;
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

	public String getRefTouchpointId() {
		return refTouchpointId;
	}

	public void setRefTouchpointId(String refTouchpointId) {
		this.refTouchpointId = refTouchpointId;
	}
	
	public String getSurveyChoicesId() {
		return surveyChoicesId;
	}
	
	public void setSurveyChoicesId(String surveyChoicesId) {
		this.surveyChoicesId = surveyChoicesId;
	}
	
	public String getFeedbackDataType() {
		return feedbackDataType;
	}

	public void setFeedbackDataType(String feedbackDataType) {
		this.feedbackDataType = feedbackDataType;
	}

	public List<SurveyChoice> getSurveyChoices() {
		return surveyChoices;
	}

	protected void parseSurveyChoices(JsonObject obj) {
		JsonArray jsonArray = obj.getJsonArray(SURVEY_RATING_CHOICES);
		if(jsonArray != null) {
			int size = jsonArray.size();
			this.surveyChoices = new ArrayList<>(size);
			
			for (Object object : jsonArray) {
				String choice = object.toString();

				int startingIndex = choice.indexOf("[");
				int closingIndex = choice.indexOf("]");
				if(startingIndex >=0 && closingIndex > 0) {
					int choiceScore = StringUtil.safeParseInt(choice.substring(startingIndex + 1, closingIndex));
					this.surveyChoices.add(new SurveyChoice(choice, choiceScore, 0));
				}
			}
			this.surveyChoicesId = createHashedId(this.surveyChoices.toString());
		} 
		else {
			this.surveyChoices = new ArrayList<>(0);
		}
	}

	public ScoreCX getScoreCX() {
		return scoreCX;
	}

	public void setScoreCX(ScoreCX scoreCX) {
		this.scoreCX = scoreCX;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

	
}
