package test.analytics;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import leotech.cdp.dao.Analytics360DaoUtil;
import leotech.cdp.model.analytics.StatisticCollector;
import rfx.core.util.Utils;

public class Analytics360Test {

	public static void main(String[] args) {
		String beginFilterDate = "2024-04-20";
		String endFilterDate = "2024-09-05";
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		//DashboardReport d = Analytics360Service.getDashboardReport("2018-01-05T02:56:12.102Z", "2020-09-05T02:56:12.102Z");
		
		// profile
		long b, d= 0;
		
		b = System.currentTimeMillis();
		System.out.println("collectProfileTotalStatistics");
		List<StatisticCollector> statsTotalP = Analytics360DaoUtil.collectProfileTotalStatistics();
		System.out.println(gson.toJson(statsTotalP));
		
		d = System.currentTimeMillis() - b;
		b = System.currentTimeMillis();
		System.out.println("[collectProfileTotalStatistics] duration: " + d);
		System.out.println("\n--------\n");
		
		System.exit(1);
		
		System.out.println("collectProfileFunnelStatistics");
		List<StatisticCollector> dailyStatsP = Analytics360DaoUtil.collectProfileFunnelStatistics("",beginFilterDate, endFilterDate);
		System.out.println(gson.toJson(dailyStatsP));
		
		d = System.currentTimeMillis() - b;
		b = System.currentTimeMillis();
		System.out.println("[collectProfileFunnelStatistics] duration: " + d);
		
		System.out.println("\n--------\n");
		
		System.out.println("collectProfileDailyStatistics");
		List<StatisticCollector> timeseriesDataP = Analytics360DaoUtil.collectProfileDailyStatistics(beginFilterDate, endFilterDate);
		System.out.println(gson.toJson(timeseriesDataP));
		
		d = System.currentTimeMillis() - b;
		b = System.currentTimeMillis();
		System.out.println("[collectProfileDailyStatistics] duration: " + d);
		
		System.out.println("\n--------\n");
		
		// events
		System.out.println("collectTrackingEventTotalStatistics");
		List<StatisticCollector> statsTotalE = Analytics360DaoUtil.collectTrackingEventTotalStatistics();
		System.out.println(gson.toJson(statsTotalE));
		
		d = System.currentTimeMillis() - b;
		b = System.currentTimeMillis();
		System.out.println("[collectTrackingEventTotalStatistics] duration: " + d);
		System.out.println("\n--------\n");
		
		System.out.println("collectTrackingEventTotalStatistics");
		List<StatisticCollector> dailyStatsE = Analytics360DaoUtil.collectTrackingEventTotalStatistics("",beginFilterDate, endFilterDate);
		System.out.println(gson.toJson(dailyStatsE));
		
		d = System.currentTimeMillis() - b;
		b = System.currentTimeMillis();
		System.out.println("[collectTrackingEventTotalStatistics] duration: " + d);
		System.out.println("\n--------\n");
		
		System.out.println("collectEventDailyStatistics");
		List<StatisticCollector> timeseriesDataE = Analytics360DaoUtil.collectEventDailyStatistics(beginFilterDate, endFilterDate);
		System.out.println(gson.toJson(timeseriesDataE));
		
		d = System.currentTimeMillis() - b;
		b = System.currentTimeMillis();
		System.out.println("[collectEventDailyStatistics] duration: " + d);
		Utils.exitSystemAfterTimeout(1000);
	}
}
