package leotech.cdp.model.analytics;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.SingleViewAnalyticalObject;
import rfx.core.util.StringUtil;

public final class FeedbackRatingReport extends FeedbackData implements SingleViewAnalyticalObject {


	@Expose
	String fromDate = "";

	@Expose
	String toDate = "";
	

	@Expose
	Map<String, ScoreCX> dailyScoreCX = new HashMap<String, ScoreCX>();

	@Expose
	int positiveProfileCount = 0;

	@Expose
	int neutralProfileCount = 0;

	@Expose
	int negativeProfileCount = 0;


	public FeedbackRatingReport() {
	}

	public FeedbackRatingReport(String fromDate, String toDate, String feedbackDataType) {
		super();
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.timePeriod = fromDate + "_" + toDate;
		this.feedbackDataType = feedbackDataType;
	}


	@Override
	public boolean dataValidation() {
		return  StringUtil.isNotEmpty(this.refTemplateId) 
				&& StringUtil.isNotEmpty(this.group) && StringUtil.isNotEmpty(this.evaluatedObject) && StringUtil.isNotEmpty(this.header) ;
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (dataValidation()) {
			if(StringUtil.isEmpty(timePeriod) && StringUtil.isNotEmpty(fromDate) && StringUtil.isNotEmpty(toDate)) {
				timePeriod = fromDate + "-" + toDate;
			}
			String keyHint = timePeriod + header + group + evaluatedObject + feedbackType  ;
			
			System.out.println("keyHint "+keyHint);
			this.id = createId(this.id, keyHint);
		} else {
			System.err.println(new Gson().toJson(this));
			throw new IllegalArgumentException("check isReadyForSave is failed ");
		}
		return this.id;
	}

	@Override
	public void unifyData() {
		// TODO
	}
	
	public String getFeedbackDataType() {
		return feedbackDataType;
	}

	public void setFeedbackDataType(String feedbackDataType) {
		this.feedbackDataType = feedbackDataType;
	}


	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}

	public String getToDate() {
		return toDate;
	}

	public void setToDate(String toDate) {
		this.toDate = toDate;
	}

	public Map<String, ScoreCX> getDailyScoreCX() {
		return dailyScoreCX;
	}

	public void setDailyScoreCX(Map<String, ScoreCX> dailyScoreCX) {
		this.dailyScoreCX = dailyScoreCX;
	}

	public void setPositiveProfileCount(int positiveProfileCount) {
		this.positiveProfileCount = positiveProfileCount;
	}

	public int getNeutralProfileCount() {
		return neutralProfileCount;
	}

	public void setNeutralProfileCount(int neutralProfileCount) {
		this.neutralProfileCount = neutralProfileCount;
	}

	public int getNegativeProfileCount() {
		return negativeProfileCount;
	}

	public void setNegativeProfileCount(int negativeProfileCount) {
		this.negativeProfileCount = negativeProfileCount;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}


}
