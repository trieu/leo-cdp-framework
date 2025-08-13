package leotech.cdp.model.analytics;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

/**
 * the data object for primary default reporting
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public class DashboardReport {

	@Expose
	String beginFilterDate;

	@Expose
	String endFilterDate;

	@Expose
	String timeUnit;
	
	@Expose
	List<StatisticCollector> profileFunnelData;

	@Expose
	List<List<Integer>> journeyStatsData = new ArrayList<List<Integer>>(5);
	
	@Expose
	List<StatisticCollector> profileTotalStats;
	
	public DashboardReport() {

	}
	
	public DashboardReport(String beginFilterDate, String endFilterDate, String timeUnit, List<StatisticCollector> profileFunnelData) {
		super();
		this.beginFilterDate = beginFilterDate;
		this.endFilterDate = endFilterDate;
		this.timeUnit = timeUnit;
		this.profileFunnelData = profileFunnelData;
	}
	
	public DashboardReport(String beginFilterDate, String endFilterDate, String timeUnit,List<StatisticCollector> profileFunnelData , 
																						List<StatisticCollector> profileTotalStats) {
		super();
		this.beginFilterDate = beginFilterDate;
		this.endFilterDate = endFilterDate;
		this.timeUnit = timeUnit;
		this.profileFunnelData = profileFunnelData;
		this.profileTotalStats = profileTotalStats;
	}


	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	public String getBeginFilterDate() {
		return beginFilterDate;
	}

	public void setBeginFilterDate(String beginFilterDate) {
		this.beginFilterDate = beginFilterDate;
	}

	public String getEndFilterDate() {
		return endFilterDate;
	}

	public void setEndFilterDate(String endFilterDate) {
		this.endFilterDate = endFilterDate;
	}

	public String getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(String timeUnit) {
		this.timeUnit = timeUnit;
	}

	public List<StatisticCollector> getProfileTotalStats() {
		return profileTotalStats;
	}

	public void setProfileTotalStats(List<StatisticCollector> profileTotalStats) {
		this.profileTotalStats = profileTotalStats;
	}

	public List<StatisticCollector> getProfileFunnelData() {
		return profileFunnelData;
	}

	public void setProfileFunnelData(List<StatisticCollector> profileFunnelData) {
		this.profileFunnelData = profileFunnelData;
	}

	public List<List<Integer>> getJourneyStatsData() {
		return journeyStatsData;
	}

	public void setJourneyStatsData(List<List<Integer>> journeyStatsData) {
		this.journeyStatsData = journeyStatsData;
	}

}
