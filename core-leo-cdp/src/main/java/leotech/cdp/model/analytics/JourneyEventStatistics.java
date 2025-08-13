package leotech.cdp.model.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;

/**
 * The 5As Customer Framework <br>
 * <br>
 * https://knowledge.leocdp.net/2022/05/the-5as-customer-framework.html <br>
 * https://datahub4uspa.leocdp.net/content/1w0bTjf2RNnhkm6hFZZ7HS-marketing-4-0-phan-3-mo-hinh-5a-trong-thoi-ai-so
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class JourneyEventStatistics implements DataJourneyStatistics{

	int countAwareness = 0, countAttraction = 0, countAsk = 0, countAction = 0, countAdvocacy = 0;
	
	// measure how far a profile goes in data funnel
	int funnelIndex = 0;
	
	// Customer experience
	ScoreCX scoreCX = null;

	public JourneyEventStatistics() {
		scoreCX = new ScoreCX();
	}

	public int getCountAwareness() {
		return countAwareness;
	}

	public void setCountAwareness(int countAwareness) {
		this.countAwareness = countAwareness;
	}

	public int getCountAttraction() {
		return countAttraction;
	}

	public void setCountAttraction(int countAttraction) {
		this.countAttraction = countAttraction;
	}

	public int getCountAsk() {
		return countAsk;
	}

	public void setCountAsk(int countAsk) {
		this.countAsk = countAsk;
	}

	public int getCountAction() {
		return countAction;
	}

	public void setCountAction(int countAction) {
		this.countAction = countAction;
	}

	public int getCountAdvocacy() {
		return countAdvocacy;
	}

	public void setCountAdvocacy(int countAdvocacy) {
		this.countAdvocacy = countAdvocacy;
	}
	
	public int getFunnelIndex() {
		return funnelIndex;
	}
	
	public int getJourneyFunnelValue() {
		return funnelIndex * 10;
	}

	public void setFunnelIndex(int funnelIndex) {
		this.funnelIndex = funnelIndex;
	}

	public ScoreCX getScoreCX() {
		if(scoreCX == null) {
			return new ScoreCX();
		}
		return scoreCX;
	}

	public void setScoreCX(ScoreCX scoreCX) {
		this.scoreCX = scoreCX;
	}

	@Override
	public List<List<Integer>> getDataForFunnelGraph() {
		List<List<Integer>> data = new ArrayList<List<Integer>>(5);
		data.add(Arrays.asList(countAwareness));
		data.add(Arrays.asList(countAttraction));
		data.add(Arrays.asList(countAsk));
		data.add(Arrays.asList(countAction));
		data.add(Arrays.asList(countAdvocacy));
		return data;
	}
	

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
