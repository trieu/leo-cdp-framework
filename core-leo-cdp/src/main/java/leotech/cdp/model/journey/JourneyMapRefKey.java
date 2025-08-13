package leotech.cdp.model.journey;

import java.util.Objects;

import com.google.gson.Gson;

import leotech.cdp.model.RefKey;
import leotech.cdp.model.analytics.ScoreCX;

/**
 * reference key for AbstractProfile.inJourneyMaps
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class JourneyMapRefKey extends RefKey {

	ScoreCX scoreCX = new ScoreCX();
	int funnelIndex = 0; // = cdp_dataflowstage.orderIndex

	// marketing 
	int leadScore = 0;
	int prospectScore = 0;
	int engagementScore = 0;
	
	// sales
	double transactionValue = 0;
	int creditScore = 0;
	int loyaltyScore = 0;

	// Customer Acquisition Cost of journey map 
	double acquisitionCost = 0;
	
	// Customer Lifetime Value of journey map 
	double lifetimeValue = 0;
	
	// CX
	int scoreCFS = 0;
	int scoreCES = 0;
	int scoreCSAT = 0;
	int scoreNPS = 0;

	public JourneyMapRefKey() {
		// for ArangoDB
	}

	public JourneyMapRefKey(String id) {
		super(id);
	}

	public JourneyMapRefKey(String id, String name) {
		super(id, name, "", 1);
	}

	public JourneyMapRefKey(String id, String name, String type) {
		super(id, name, type, 1);
	}

	public JourneyMapRefKey(String id, String name, String type, int journeyStage, int funnelIndex) {
		super(id, name, type, journeyStage);
		this.funnelIndex = funnelIndex;
	}

	public JourneyMapRefKey(String id, String name, String type, int journeyStage, int funnelIndex, ScoreCX scoreCX) {
		super(id, name, type, journeyStage);
		this.funnelIndex = funnelIndex;
		setScoreCX(scoreCX);
	}

	public ScoreCX getScoreCX() {
		return scoreCX;
	}

	public void setScoreCX(ScoreCX scoreCX) {
		if (scoreCX != null) {
			this.scoreCX = scoreCX;
		}
	}

	public int getFunnelIndex() {
		return funnelIndex;
	}

	public void setFunnelIndex(int funnelIndex) {
		this.funnelIndex = funnelIndex;
	}
	
	

	public int getLeadScore() {
		return leadScore;
	}

	public void setLeadScore(int leadScore) {
		this.leadScore = leadScore;
	}

	public int getProspectScore() {
		return prospectScore;
	}

	public void setProspectScore(int prospectScore) {
		this.prospectScore = prospectScore;
	}

	public int getEngagementScore() {
		return engagementScore;
	}

	public void setEngagementScore(int engagementScore) {
		this.engagementScore = engagementScore;
	}

	public double getTransactionValue() {
		return transactionValue;
	}

	public void setTransactionValue(double transactionValue) {
		this.transactionValue = transactionValue;
	}

	public int getCreditScore() {
		return creditScore;
	}

	public void setCreditScore(int creditScore) {
		this.creditScore = creditScore;
	}

	public int getLoyaltyScore() {
		return loyaltyScore;
	}

	public void setLoyaltyScore(int loyaltyScore) {
		this.loyaltyScore = loyaltyScore;
	}

	public double getAcquisitionCost() {
		return acquisitionCost;
	}

	public void setAcquisitionCost(double acquisitionCost) {
		this.acquisitionCost = acquisitionCost;
	}

	public double getLifetimeValue() {
		return lifetimeValue;
	}

	public void setLifetimeValue(double lifetimeValue) {
		this.lifetimeValue = lifetimeValue;
	}

	public int getScoreCFS() {
		return scoreCFS;
	}

	public void setScoreCFS(int scoreCFS) {
		this.scoreCFS = scoreCFS;
	}

	public int getScoreCES() {
		return scoreCES;
	}

	public void setScoreCES(int scoreCES) {
		this.scoreCES = scoreCES;
	}

	public int getScoreCSAT() {
		return scoreCSAT;
	}

	public void setScoreCSAT(int scoreCSAT) {
		this.scoreCSAT = scoreCSAT;
	}

	public int getScoreNPS() {
		return scoreNPS;
	}

	public void setScoreNPS(int scoreNPS) {
		this.scoreNPS = scoreNPS;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
