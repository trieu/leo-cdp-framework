package leotech.cdp.model.analytics;

import java.util.Map;

import com.google.gson.Gson;

import rfx.core.util.StringUtil;

/**
 * the score model for LEO CX
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class ScoreCX {

	int positive = 0, neutral = 0, negative = 0;
	int sentimentScore = 0;
	int sentimentType = 0;
	int positivePercentage = 0, neutralPercentage = 0, negativePercentage = 0;
	boolean happy = true;

	public ScoreCX() {
		// default
	}
	

	public ScoreCX(TrackingEvent e) {
		// from database
		Map<String, Object> map = e.getEventData();
		if(map.containsKey("FeedbackEventID")) {
			this.positive = StringUtil.safeParseInt(map.getOrDefault("CX_Score_Positive", "0"));
			this.neutral = StringUtil.safeParseInt(map.getOrDefault("CX_Score_Neutral", "0"));
			this.negative = StringUtil.safeParseInt(map.getOrDefault("CX_Score_Negative", "0"));	
		}
	}

	public ScoreCX(int positive, int neutral, int negative) {
		super();
		this.positive = positive;
		this.neutral = neutral;
		this.negative = negative;
	}
	
	public boolean hasScoreData() {
		int sum = this.positive + this.neutral + this.negative;
		return sum > 0 && sum <= 100;
	}
	
	public int getPositive() {
		return positive;
	}

	public void setPositive(int positive) {
		this.positive = positive;
	}
	
	public int getNeutral() {
		return neutral;
	}

	public void setNeutral(int neutral) {
		this.neutral = neutral;
	}
	
	public int getNegative() {
		return negative;
	}

	public void setNegative(int negative) {
		this.negative = negative;
	}
	
	public void computeSentimentScoreAndPercentage() {
		computeSentimentScore();
		computeSentimentType();
		computePercentage();
		this.happy = sentimentScore > 0 && this.positive > 0;
	}

	public ScoreCX increase(ScoreCX score) {
		this.positive += score.getPositive();
		this.neutral += score.getNeutral();
		this.negative += score.getNegative();
		
		this.computeSentimentScoreAndPercentage();
		return this;
	}

	private ScoreCX computePercentage() {
		int sum = this.positive + this.neutral + this.negative;
		if(sum > 0) {
			this.positivePercentage = (int) Math.floor(this.positive * 100 / sum);
			this.negativePercentage = (int) Math.floor(this.negative * 100 / sum);
			this.neutralPercentage = 100 - (this.positivePercentage + this.negativePercentage);
		}
		return this;
	}
	
	public int getSentimentScore() {
		return sentimentScore;
	}
	
	public boolean isHappy() {
		computeSentimentScoreAndPercentage();
		return happy;
	}

	private void computeSentimentScore() {
		double ratio = this.neutral / 1.42F;
		this.sentimentScore = this.positive + (int)(Math.ceil(ratio)) - this.negative;
	}

	public void setSentimentScore(int sentimentScore) {
		this.sentimentScore = sentimentScore;
	}

	public int getSentimentType() {
		computeSentimentType();
		return sentimentType;
	}

	private void computeSentimentType() {
		this.sentimentType = 0;
		if (positive > neutral && positive > negative) {
			sentimentType = 1;
		} else if (negative > positive && negative > neutral) {
			sentimentType = -1;
		}
	}

	public void setSentimentType(int sentimentType) {
		this.sentimentType = sentimentType;
	}

	public int getPositivePercentage() {
		return positivePercentage;
	}

	public void setPositivePercentage(int positivePercentage) {
		this.positivePercentage = positivePercentage;
	}

	public int getNeutralPercentage() {
		return neutralPercentage;
	}

	public void setNeutralPercentage(int neutralPercentage) {
		this.neutralPercentage = neutralPercentage;
	}

	public int getNegativePercentage() {
		return negativePercentage;
	}

	public void setNegativePercentage(int negativePercentage) {
		this.negativePercentage = negativePercentage;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
