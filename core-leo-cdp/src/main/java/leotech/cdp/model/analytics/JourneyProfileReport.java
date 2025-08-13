package leotech.cdp.model.analytics;

import java.util.List;

import com.google.gson.Gson;

/**
 * for Journey Report in a Profile
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class JourneyProfileReport {

	List<List<Integer>> reportData;
	int funnelIndex;
	String funnelIndexName;
	int total = 0;
	ScoreCX scoreCX = null;

	public JourneyProfileReport(List<List<Integer>> reportData, int funnelIndex, String funnelIndexName,
			ScoreCX scoreCX) {
		super();
		this.reportData = reportData;
		this.funnelIndex = funnelIndex;
		this.funnelIndexName = funnelIndexName;
		total = reportData.stream().map(list -> {
			return list.size() > 0 ? list.get(0) : 0;
		}).reduce(0, (a, b) -> a + b);
		this.scoreCX = scoreCX;
	}

	public List<List<Integer>> getReportData() {
		return reportData;
	}

	public void setReportData(List<List<Integer>> reportData) {
		this.reportData = reportData;
	}

	public int getFunnelIndex() {
		return funnelIndex;
	}

	public void setFunnelIndex(int funnelIndex) {
		this.funnelIndex = funnelIndex;
	}

	public String getFunnelIndexName() {
		return funnelIndexName;
	}

	public void setFunnelIndexName(String funnelIndexName) {
		this.funnelIndexName = funnelIndexName;
	}

	public ScoreCX getScoreCX() {
		return scoreCX;
	}

	public void setScoreCX(ScoreCX scoreCX) {
		this.scoreCX = scoreCX;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
