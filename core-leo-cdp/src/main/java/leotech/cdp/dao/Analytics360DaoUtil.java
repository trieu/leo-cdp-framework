package leotech.cdp.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.arangodb.ArangoDatabase;

import leotech.cdp.domain.Analytics360Management;
import leotech.cdp.domain.DataFlowManagement;
import leotech.cdp.model.analytics.EventMatrixReport;
import leotech.cdp.model.analytics.ProfileStatisticsView;
import leotech.cdp.model.analytics.RevenueStatisticsView;
import leotech.cdp.model.analytics.StatisticCollector;
import leotech.cdp.model.journey.DataFlowStage;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;
import leotech.system.version.SystemMetaData;

public final class Analytics360DaoUtil extends AbstractCdpDatabaseUtil {
	
	static final String CUSTOMER_SEGMENT = "Total Segment";
	static final String TOTAL_PROFILE = "Total Profile";
	static final String TOTAL_VISITOR = "Total Visitor";
	static final String TOTAL_INACTIVE_VISITOR = "Total Inactive Visitor";
	static final String TOTAL_CONTACT_PROFILE = "Total Contact";
	static final String TOTAL_CUSTOMER_PROFILE = "Total Customer";

	static final String TOTAL_REVENUE = "Total Revenue";
	static final String AVERAGE_REVENUE = "Avg Revenue / Customer";
	static final String CONVERSION_RATE = "Conversion Rate";
	
	// profile statistics
	static final String AQL_PROFILE_COLLECTOR_TOTAL = AqlTemplate.get("AQL_PROFILE_COLLECTOR_TOTAL");
	static final String AQL_PROFILE_STATS_COLLECTOR = AqlTemplate.get("AQL_PROFILE_STATS_COLLECTOR");
	static final String AQL_REVENUE_STATS_COLLECTOR = AqlTemplate.get("AQL_REVENUE_STATS_COLLECTOR");
	
	static final String AQL_PROFILE_COLLECTOR_IN_DATE_RANGE = AqlTemplate.get("AQL_PROFILE_COLLECTOR_IN_DATE_RANGE");
	static final String AQL_PROFILE_TIMESERIES_COLLECTOR = AqlTemplate.get("AQL_PROFILE_TIMESERIES_COLLECTOR");
	
	// event statistics
	static final String AQL_EVENT_COLLECTOR_TOTAL = AqlTemplate.get("AQL_EVENT_COLLECTOR_TOTAL");
	static final String AQL_EVENT_COLLECTOR_TOTAL_IN_DATE_RANGE = AqlTemplate.get("AQL_EVENT_COLLECTOR_TOTAL_IN_DATE_RANGE");
	static final String AQL_EVENT_TIMESERIES_COLLECTOR = AqlTemplate.get("AQL_EVENT_TIMESERIES_COLLECTOR");
	
	static final String AQL_EVENT_MATRIX_REPORT = AqlTemplate.get("AQL_EVENT_MATRIX_REPORT");
	static final String AQL_GET_TRACKING_EVENT_TIMESERIES_REPORT = AqlTemplate.get("AQL_GET_TRACKING_EVENT_TIMESERIES_REPORT");
	

	static ProfileStatisticsView getProfileStatistics() {
		ProfileStatisticsView stats = null;
	    try {
			ExecutorService executor = Executors.newSingleThreadExecutor();

			Future<ProfileStatisticsView> future = executor.submit(() -> {
				Map<String, Object> bindVars = new HashMap<>(1);
				bindVars.put("inactiveLimitDays", SystemMetaData.NUMBER_OF_DAYS_TO_KEEP_DEAD_VISITOR);
			    ArangoDbCommand<ProfileStatisticsView> statsQuery = new ArangoDbCommand<>(getCdpDatabase(), AQL_PROFILE_STATS_COLLECTOR, bindVars, ProfileStatisticsView.class);
			    return statsQuery.getSingleResult();
			});

			stats = future.get(); // Waits for the query to complete and retrieves the result
			executor.shutdown(); // Shuts down the executor
		}  catch (Exception e) {
			e.printStackTrace();
		}
	    return stats != null ? stats : new ProfileStatisticsView();
	}
	
	/**
	 * @return data for Total Customer Statistics box
	 */
	public static List<StatisticCollector> collectProfileTotalStatistics(){
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackProfileStatisticCollector();
		List<StatisticCollector> list = collectTotalStatistics(AQL_PROFILE_COLLECTOR_TOTAL, callback);
		Map<String,StatisticCollector> mapStats = new HashMap<>(list.size()*2);
		ProfileStatisticsView stats = getProfileStatistics();

		if(stats != null) {
			
			mapStats.put(TOTAL_PROFILE, new StatisticCollector(TOTAL_PROFILE, stats.getTotalProfile(), -12));
			mapStats.put(TOTAL_CONTACT_PROFILE, new StatisticCollector(TOTAL_CONTACT_PROFILE, stats.getTotalContactProfile(), -11));
			mapStats.put(TOTAL_CUSTOMER_PROFILE, new StatisticCollector(TOTAL_CUSTOMER_PROFILE, stats.getTotalCustomerProfile(), -10));
			
			mapStats.put(TOTAL_VISITOR, new StatisticCollector(TOTAL_VISITOR, stats.getTotalVisitor(), -9));
			mapStats.put(TOTAL_INACTIVE_VISITOR, new StatisticCollector(TOTAL_INACTIVE_VISITOR, stats.getTotalInactiveVisitor(), -8));
			mapStats.put(CUSTOMER_SEGMENT, new StatisticCollector(CUSTOMER_SEGMENT, stats.getTotalSegments(), -7));
		
		} else {
			mapStats.put(TOTAL_PROFILE, new StatisticCollector(TOTAL_PROFILE, 0, -12));
			mapStats.put(TOTAL_CONTACT_PROFILE, new StatisticCollector(TOTAL_CONTACT_PROFILE, 0, -11));
			mapStats.put(TOTAL_CUSTOMER_PROFILE, new StatisticCollector(TOTAL_CUSTOMER_PROFILE, 0, -10));

			mapStats.put(TOTAL_VISITOR, new StatisticCollector(TOTAL_VISITOR, 0, -9));
			mapStats.put(TOTAL_INACTIVE_VISITOR, new StatisticCollector(TOTAL_INACTIVE_VISITOR, 0, -8));
			mapStats.put(CUSTOMER_SEGMENT, new StatisticCollector(CUSTOMER_SEGMENT, 0, -7));
		}
		
		for (StatisticCollector c : list) {
			String dataFlowStageId = c.getDataFlowStageId();
			boolean check = DataFlowManagement.getDataFlowCache().get(dataFlowStageId) != null;
			if(check) {
				String collectorKey = c.getCollectorKey();
				mapStats.put(collectorKey, c);
			}
		}
		
		Queue<DataFlowStage> flowStages = new LinkedList<>(DataFlowManagement.getCustomerFunnelStages());
		while( ! flowStages.isEmpty()){
			DataFlowStage s = flowStages.poll();
			if(s != null) {
				String dataFlowStageId = s.getId();
				String name = s.getName();
				int flowType = s.getFlowType();
				int orderIndex = s.getOrderIndex();
				StatisticCollector holder = mapStats.get(name);
				if( holder == null ) {
					holder = new StatisticCollector(name, 0, orderIndex , dataFlowStageId, flowType);
					mapStats.put(name, holder);
				}
			}
		}
		
		List<StatisticCollector> finalResults = new ArrayList<>(mapStats.values());
		Collections.sort(finalResults);
		return finalResults;
	}
	
	/**
	 * @return
	 */
	public static List<StatisticCollector> collectRevenueTotalStatistics(){

		ArangoDbCommand<RevenueStatisticsView> statsQuery = new ArangoDbCommand<>(getCdpDatabase(), AQL_REVENUE_STATS_COLLECTOR, RevenueStatisticsView.class);
		RevenueStatisticsView stats = statsQuery.getSingleResult();
		Map<String,StatisticCollector> mapStats = new HashMap<>(2);
		if(stats != null) {
			mapStats.put(TOTAL_REVENUE, new StatisticCollector(TOTAL_REVENUE, stats.getTotalTransactionValue(), -2));
			mapStats.put(AVERAGE_REVENUE, new StatisticCollector(AVERAGE_REVENUE, stats.getAvgTransactionValue(), -1));
		
		} else {
			mapStats.put(TOTAL_REVENUE, new StatisticCollector(TOTAL_REVENUE, 0, -12));
			mapStats.put(AVERAGE_REVENUE, new StatisticCollector(AVERAGE_REVENUE, 0, -11));
		}
		List<StatisticCollector> finalResults = new ArrayList<>(mapStats.values());
		Collections.sort(finalResults);
		return finalResults;
	}
	
	/**
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @return data for dashboard funnel
	 */
	public static List<StatisticCollector> collectProfileFunnelStatistics(String journeyMapId, String beginFilterDate, String endFilterDate){
		String aql = AQL_PROFILE_COLLECTOR_IN_DATE_RANGE;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackProfileStatisticCollector();
		
		List<StatisticCollector> queriedList = collectTotalStatisticsInDateRange(journeyMapId, beginFilterDate, endFilterDate, aql, callback);
		
		// seeding empty data from defined funnel
		List<DataFlowStage> stages = DataFlowManagement.getCustomerFunnelStages();
		Map<String, StatisticCollector> funnelData = new HashMap<>(stages.size());
		for (DataFlowStage stage : stages) {
			StatisticCollector c = new StatisticCollector(stage.getName(), 0, stage.getOrderIndex());
			funnelData.put(c.getCollectorKey(), c);
		}
		
		// set real data from queried stats collector
		for (StatisticCollector collector : queriedList) {
			StatisticCollector c = funnelData.get(collector.getCollectorKey());
			if(c != null) {
				c.setCollectorCount(collector.getCollectorCount());
			}
		}
		
		//sorting for ordered stage in funnel
		List<StatisticCollector> finalResults = new ArrayList<StatisticCollector>(funnelData.values());
		Collections.sort(finalResults);
		return finalResults;
	}
	
	public static List<StatisticCollector> collectProfileHourlyStatistics(String beginFilterDate, String endFilterDate){
		String aql = AQL_PROFILE_TIMESERIES_COLLECTOR;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackProfileStatisticCollector();
		return collectTimeseriesData(Analytics360Management.HOURS,beginFilterDate, endFilterDate, aql, callback);
	}
	
	public static List<StatisticCollector> collectProfileDailyStatistics(String beginFilterDate, String endFilterDate){
		String aql = AQL_PROFILE_TIMESERIES_COLLECTOR;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackProfileStatisticCollector();
		return collectTimeseriesData(Analytics360Management.DAYS,beginFilterDate, endFilterDate, aql, callback);
	}
	
	public static List<StatisticCollector> collectProfileMonthlyStatistics(String beginFilterDate, String endFilterDate){
		String aql = AQL_PROFILE_TIMESERIES_COLLECTOR;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackProfileStatisticCollector();
		return collectTimeseriesData(Analytics360Management.MONTHS,beginFilterDate, endFilterDate, aql, callback);
	}
	
	public static List<StatisticCollector> collectProfileYearlyStatistics(String beginFilterDate, String endFilterDate){
		String aql = AQL_PROFILE_TIMESERIES_COLLECTOR;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackProfileStatisticCollector();
		return collectTimeseriesData(Analytics360Management.YEARS,beginFilterDate, endFilterDate, aql, callback);
	}
	
	// ---- Profile Statistics ---- //
	
	
	
	// ---- Tracking Event Statistics ---- //
	
	public static List<StatisticCollector> collectTrackingEventTotalStatistics(){
		String aql = AQL_EVENT_COLLECTOR_TOTAL;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackEventStatisticCollector();
		return collectTotalStatistics(aql,callback);
	}
	
	public static List<StatisticCollector> collectTrackingEventTotalStatistics(String journeyMapId, String beginFilterDate, String endFilterDate){
		String aql = AQL_EVENT_COLLECTOR_TOTAL_IN_DATE_RANGE;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackEventStatisticCollector();
		return collectTotalStatisticsInDateRange(journeyMapId, beginFilterDate, endFilterDate, aql, callback);
	}
	
	public static List<StatisticCollector> collectEventHourlyStatistics(String beginFilterDate, String endFilterDate){
		String aql = AQL_EVENT_TIMESERIES_COLLECTOR;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackEventStatisticCollector();
		return collectTimeseriesData(Analytics360Management.HOURS,beginFilterDate, endFilterDate, aql, callback);
	}
	
	public static List<StatisticCollector> collectEventDailyStatistics(String beginFilterDate, String endFilterDate){
		String aql = AQL_EVENT_TIMESERIES_COLLECTOR;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackEventStatisticCollector();
		return collectTimeseriesData(Analytics360Management.DAYS,beginFilterDate, endFilterDate, aql, callback);
	}
	
	public static List<StatisticCollector> collectEventMonthlyStatistics(String beginFilterDate, String endFilterDate){
		String aql = AQL_EVENT_TIMESERIES_COLLECTOR;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackEventStatisticCollector();
		return collectTimeseriesData(Analytics360Management.MONTHS,beginFilterDate, endFilterDate, aql, callback);
	}
	
	public static List<StatisticCollector> collectEventYearlyStatistics(String beginFilterDate, String endFilterDate){
		String aql = AQL_EVENT_TIMESERIES_COLLECTOR;
		CallbackQuery<StatisticCollector> callback = StatisticCollector.callbackEventStatisticCollector();
		return collectTimeseriesData(Analytics360Management.YEARS,beginFilterDate, endFilterDate, aql, callback);
	}
	
	// ---- Tracking Event Statistics ---- //

	/**
	 * @param aql
	 * @param callback
	 * @return
	 */
	protected static List<StatisticCollector> collectTotalStatistics(String aql, CallbackQuery<StatisticCollector> callback){
		ExecutorService executor = Executors.newSingleThreadExecutor();
	    Future<List<StatisticCollector>> future = executor.submit(() -> {
	        ArangoDatabase db = getCdpDatabase();
	        ArangoDbCommand<StatisticCollector> q = new ArangoDbCommand<>(db, aql, StatisticCollector.class, callback);
	        List<StatisticCollector> list = q.getResultsAsList();
	        Collections.sort(list);
	        return list;
	    });
	    List<StatisticCollector> sortedList = null;
		try {
			sortedList = future.get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // Waits for the computation to finish and retrieves the result
	    executor.shutdown(); // Shuts down the executor
		return sortedList != null ? sortedList : new ArrayList<StatisticCollector>(0);
	}
	

	/**
	 * @param journeyMapId
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @param aql
	 * @param callback
	 * @return
	 */
	static List<StatisticCollector> collectTotalStatisticsInDateRange(String journeyMapId, String beginFilterDate, String endFilterDate, String aql, CallbackQuery<StatisticCollector> callback){
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("journeyMapId", journeyMapId);
		bindVars.put("beginFilterDate", beginFilterDate);
		bindVars.put("endFilterDate", endFilterDate);
		ArangoDbCommand<StatisticCollector> q = new ArangoDbCommand<StatisticCollector>(db, aql, bindVars, StatisticCollector.class,callback);
		List<StatisticCollector> list = q.getResultsAsList();
		Collections.sort(list);
		return list;
	}
	
	/**
	 * @param truncatedTimeUnit
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @param aql
	 * @param callback
	 * @return
	 */
	static List<StatisticCollector> collectTimeseriesData(String truncatedTimeUnit, String beginFilterDate, String endFilterDate, String aql, CallbackQuery<StatisticCollector> callback){
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("beginFilterDate", beginFilterDate);
		bindVars.put("endFilterDate", endFilterDate);
		bindVars.put("truncatedUnit",truncatedTimeUnit);// https://www.arangodb.com/docs/3.7/aql/functions-date.html#date_trunc
		ArangoDbCommand<StatisticCollector> q = new ArangoDbCommand<StatisticCollector>(db, aql, bindVars, StatisticCollector.class,callback);
		List<StatisticCollector> list = q.getResultsAsList();
		Collections.sort(list);
		return list;
	}
	
	public static List<EventMatrixReport> getEventMatrixReports(String refJourneyId, String refProfileId, String beginFilterDate, String endFilterDate) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("refJourneyId", refJourneyId);
		bindVars.put("refProfileId", refProfileId);
		bindVars.put("beginFilterDate", beginFilterDate);
		bindVars.put("endFilterDate", endFilterDate);
		List<EventMatrixReport> list = new ArangoDbCommand<>(db, AQL_EVENT_MATRIX_REPORT, bindVars, EventMatrixReport.class).getResultsAsList();
		return list;
	}
}
