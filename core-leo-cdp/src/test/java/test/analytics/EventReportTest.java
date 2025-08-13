package test.analytics;

import java.util.List;
import java.util.Map;

import leotech.cdp.domain.Analytics360Management;
import leotech.cdp.model.analytics.DailyReportUnit;
import leotech.cdp.model.analytics.DashboardEventReport;
import rfx.core.util.Utils;

public class EventReportTest {

	public static void main(String[] args) {
		reportForAllProfiles();
		//reportForOneProfile();
		//reportForOneSegment();
		
		Utils.exitSystemAfterTimeout(4000);
	}

	public static void reportForAllProfiles() {
		String fromDate = "2022-05-15T02:56:12.102Z";
		String toDate = "2022-10-27T02:56:12.102Z";
		
		DashboardEventReport report = Analytics360Management.getSummaryEventDataReport("",fromDate, toDate);
		Map<String, List<DailyReportUnit>> map = report.getReportMap();
		map.forEach( (String event, List<DailyReportUnit> list) -> {
			System.out.println("event " + event);
			for (DailyReportUnit d : list) {
				System.out.println(d);
			}
		});
		
		
		System.out.println("getProfileCount "+report.getProfileCount());
		System.out.println("\n");
		System.out.println(report.getReportSummary());
		
		System.out.println("\n ---------- \n");
	}
	
	public static void reportForOneProfile() {
		String fromDate = "2021-05-15T02:56:12.102Z";
		String toDate = "2021-05-20T02:56:12.102Z";
		
		String profileId = "3C2BZ0AKN2LKDRLG2RH664";
		DashboardEventReport report = Analytics360Management.getDashboardEventReportForProfile("",profileId, fromDate, toDate);
		Map<String, List<DailyReportUnit>> map = report.getReportMap();
		map.forEach( (String event, List<DailyReportUnit> list) -> {
			System.out.println("event " + event);
			for (DailyReportUnit d : list) {
				System.out.println(d);
			}
		});
		
		System.out.println("\n ---------- \n");
	}
	
	public static void reportForOneSegment() {
		String fromDate = "2021-07-07T23:26:34+07:00";
		String toDate = "2021-07-11T23:26:34+07:00";
		
		String segmentId = "4UmEzvm1Lk34z4K0r6MmBH";
		DashboardEventReport report = Analytics360Management.getDashboardEventReportForSegment(segmentId, fromDate, toDate);
		System.out.println(report.getReportSummary());
		
		Map<String, List<DailyReportUnit>> map = report.getReportMap();
		map.forEach( (String event, List<DailyReportUnit> list) -> {
			System.out.println("event " + event);
			for (DailyReportUnit d : list) {
				System.out.println(d);
			}
		});
		
		System.out.println("\n ---------- \n");
	}
}
