package leotech.cdp.model.analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.customer.Segment;

public class DashboardEventReport {

	@Expose
	String beginFilterDate;

	@Expose
	String endFilterDate;

	@Expose
	Map<String, List<DailyReportUnit>> reportMap;

	@Expose
	Map<String, Long> reportSummary;

	@Expose
	long profileCount = 0;

	public DashboardEventReport() {
		this.reportSummary = new HashMap<String, Long>();
		this.reportMap = new HashMap<String, List<DailyReportUnit>>();
	}

	public DashboardEventReport(String beginFilterDate, String endFilterDate) {
		super();
		this.beginFilterDate = beginFilterDate;
		this.endFilterDate = endFilterDate;
		this.reportSummary = new HashMap<String, Long>();
		this.reportMap = new HashMap<String, List<DailyReportUnit>>();
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
	
	public long getProfileCount() {
		return profileCount;
	}

	public void setProfileCount(long profileCount) {
		this.profileCount = profileCount;
	}
	
	///

	public Map<String, List<DailyReportUnit>> getReportMap() {
		return reportMap;
	}

	public void setReportMap(Map<String, List<DailyReportUnit>> reportMap) {
		this.reportMap = reportMap;
	}
	
	public synchronized void updateReportMap(String segmentId, Map<String, List<DailyReportUnit>> newReportMap) {
		newReportMap.forEach((String k, List<DailyReportUnit> newList)->{
			// get current list 
			List<DailyReportUnit> cList = this.reportMap.getOrDefault(k, new ArrayList<>());
			// loop in the newList
			for (DailyReportUnit nData : newList) {
				nData.setObjectId(segmentId);
				nData.setObjectName(Segment.COLLECTION_NAME);
				cList.add(nData);
			}
			this.reportMap.put(k, cList);
		});
	}
	
	public synchronized void addReportMap(String eventName, DailyReportUnit report) {
		List<DailyReportUnit> list = this.reportMap.getOrDefault(eventName, new ArrayList<DailyReportUnit>());
		list.add(report);
		this.reportMap.put(eventName, list);
	}

	public Map<String, Long> getReportSummary() {
		return reportSummary;
	}

	public void setReportSummary(Map<String, Long> reportSummary) {
		this.reportSummary = reportSummary;
	}
	
	public synchronized void updateReportSummary(Map<String, Long> reportSummary) {
		reportSummary.forEach((k,v)->{
			long cValue = this.reportSummary.getOrDefault(k, 0L) + v;
			this.reportSummary.put(k, cValue);
		});
	}

	public void addReportSummary(String eventName, long newVal) {
		long value = this.reportSummary.getOrDefault(eventName, 0L) + newVal;
		this.reportSummary.put(eventName, value);
	}
	

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
