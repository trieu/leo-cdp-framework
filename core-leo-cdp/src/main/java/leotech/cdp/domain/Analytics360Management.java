package leotech.cdp.domain;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import leotech.cdp.dao.Analytics360DaoUtil;
import leotech.cdp.dao.DailyReportUnitDaoUtil;
import leotech.cdp.dao.JourneyMapDao;
import leotech.cdp.domain.cache.RedisCache;
import leotech.cdp.model.analytics.DashboardEventReport;
import leotech.cdp.model.analytics.DashboardReport;
import leotech.cdp.model.analytics.EventMatrixReport;
import leotech.cdp.model.analytics.JourneyEventStatistics;
import leotech.cdp.model.analytics.JourneyProfileReport;
import leotech.cdp.model.analytics.JourneyReport;
import leotech.cdp.model.analytics.StatisticCollector;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.journey.JourneyMap;
import leotech.system.util.IdGenerator;
import leotech.system.util.TaskRunner;
import rfx.core.util.StringUtil;

/**
 * Analytics 360 report data management
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class Analytics360Management {
	
	private static final int CACHE_POOL_SIZE = 10000;
	static final String PROFILE_TOTAL_STATISTICS = "profileTotalStatistics";
	public static final String YEARS = "years";
	public static final String MONTHS = "months";
	public static final String DAYS = "days";
	public static final String HOURS = "hours";
	
	private static final int TEN_MINUTES = 10;
	private static final int TTL_PROFILE_STATS = 3600 * 12; // 12 hours
	private static final int TIME_TO_UPDATE_CACHE = TTL_PROFILE_STATS - 900;
	
	// ------- BEGIN Cache Main Dashboard
	static final CacheLoader<String, List<StatisticCollector> > cacheLoaderStatisticCollector = new CacheLoader<>() {
		@Override
		public List<StatisticCollector>  load(String key) {
			List<StatisticCollector> profileTotalStats = null;
			System.out.println("SET CACHE profileTotalStatistics");
			
			// get cache from Redis
			String json = RedisCache.getCache(key);
			if(StringUtil.isNotEmpty(json)) {
				Type listType = new TypeToken<ArrayList<StatisticCollector>>() {}.getType();
				profileTotalStats = new Gson().fromJson(json, listType);
			}
			
			// compute profile statistics 
			if( PROFILE_TOTAL_STATISTICS.equalsIgnoreCase(key)) {
				if(profileTotalStats == null) {
					// nothing in cache, try to query database
					profileTotalStats = Analytics360DaoUtil.collectProfileTotalStatistics();
					RedisCache.setCacheWithExpiry(key, profileTotalStats, TTL_PROFILE_STATS, true);
					System.out.println("MISS REDIS CACHE profileTotalStatistics, try to query database");
				}
				else if(RedisCache.shouldUpdateCache(key, TIME_TO_UPDATE_CACHE)) {
					TaskRunner.runInThreadPools(()->{
						// run in background to update cache
						List<StatisticCollector> newStats = Analytics360DaoUtil.collectProfileTotalStatistics();
						RedisCache.setCacheWithExpiry(PROFILE_TOTAL_STATISTICS, newStats, TTL_PROFILE_STATS, true);
						System.out.println("REFRESH CACHE profileTotalStatistics in background");
					});
				}
				
				return profileTotalStats;
			}
			return profileTotalStats == null? new ArrayList<StatisticCollector>(0) : profileTotalStats;
		}
	};

	static final LoadingCache<String, List<StatisticCollector> > cacheStatisticCollector = CacheBuilder.newBuilder().maximumSize(CACHE_POOL_SIZE)
			.expireAfterWrite(60, TimeUnit.SECONDS).build(cacheLoaderStatisticCollector);
	
	// ------ END Cache Main Dashboard
	

	// ------- BEGIN Cache Main Dashboard
	static final CacheLoader<DashboardReportCacheKey, DashboardReport> cacheLoaderMainDashboard = new CacheLoader<>() {
		@Override
		public DashboardReport load(DashboardReportCacheKey key) {
			System.out.println("MISS CACHE DashboardReport");
			return getDashboardReport(key.journeyMapId, key.beginFilterDate, key.endFilterDate, key.timeUnit);
		}
	};

	static final LoadingCache<DashboardReportCacheKey, DashboardReport> cacheMainDashboard = CacheBuilder.newBuilder().maximumSize(CACHE_POOL_SIZE)
			.expireAfterWrite(TEN_MINUTES, TimeUnit.MINUTES).build(cacheLoaderMainDashboard);
	
	public static void clearCacheDashboardReport() {
		// FIXME so stupid to clear all, think better solution
		cacheMainDashboard.invalidateAll();
	}
	// ------ END Cache Main Dashboard
	
	/**
	 * @return profileTotalStatistics
	 */
	public static List<StatisticCollector> getProfileTotalStatistics() {
		List<StatisticCollector> report = null;
		try {
			report = cacheStatisticCollector.get(PROFILE_TOTAL_STATISTICS);
		} catch (Exception e) {
			// skip
		}
		report = report == null ? new ArrayList<>(0) : report;
		return report;
	}
	
	// ----- BEGIN Event Report
	static CacheLoader<DashboardReportCacheKey, DashboardEventReport> cacheLoaderEventReport = new CacheLoader<>() {
		@Override
		public DashboardEventReport load(DashboardReportCacheKey key) {
			System.out.println("MISS CACHE DashboardEventReport");
			return getSummaryEventDataReport(key.journeyMapId, key.beginFilterDate, key.endFilterDate);
		}
	};

	static final LoadingCache<DashboardReportCacheKey, DashboardEventReport> cacheEventReport = CacheBuilder.newBuilder().maximumSize(10000)
			.expireAfterWrite(TEN_MINUTES, TimeUnit.MINUTES).build(cacheLoaderEventReport);
	
	public static void clearCacheEventReport() {
		// FIXME so stupid to clear all, think better solution
		cacheEventReport.invalidateAll();
	}
	
	public static void clearCacheProfileReport() {
		cacheStatisticCollector.invalidate(PROFILE_TOTAL_STATISTICS);
		RedisCache.deleteCache(PROFILE_TOTAL_STATISTICS);
	}
	// ------ END Event Report
	
	/**
	 * @author Trieu Nguyen
	 * @since 2024
	 *
	 */
	public static final class DashboardReportCacheKey {
		public final String journeyMapId;
		public String beginFilterDate, endFilterDate;
		public String timeUnit;
		public String cacheKey;
		
		public DashboardReportCacheKey(String journeyMapId, String beginFilterDate, String endFilterDate, String timeUnit) {
			super();
			this.journeyMapId = journeyMapId;
			this.beginFilterDate = beginFilterDate;
			this.endFilterDate = endFilterDate;
			this.timeUnit = timeUnit;
			this.cacheKey = IdGenerator.createHashedId(journeyMapId+beginFilterDate+endFilterDate+timeUnit);
		}
		
		public DashboardReportCacheKey(String journeyMapId, String beginFilterDate, String endFilterDate) {
			super();
			this.journeyMapId = journeyMapId;
			this.beginFilterDate = beginFilterDate;
			this.endFilterDate = endFilterDate;
			this.cacheKey = IdGenerator.createHashedId(journeyMapId+beginFilterDate+endFilterDate);
		}

		@Override
		public String toString() {
			return cacheKey;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(cacheKey);
		}
		
		@Override
		public boolean equals(Object obj) {
			return cacheKey.equals(obj.toString());
		}
	}
	
	/**
	 * @param key as DashboardReportCacheKey
	 * @return DashboardReport
	 */
	public static DashboardReport getDashboardReport(DashboardReportCacheKey key) {
		DashboardReport report = null;
		try {
			report = cacheMainDashboard.get(key);
			System.out.println("HIT CACHE DashboardReport");
		} catch (Exception e) {
			// skip
		}
		if(report == null) {
			System.out.println("MISS CACHE DashboardReport");
			report = getDashboardReport(key.journeyMapId, key.beginFilterDate, key.endFilterDate, key.timeUnit);
			cacheMainDashboard.put(key, report);
		}
		return report;
	}

	/**
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @param timeUnit
	 * @return
	 */
	public static DashboardReport getDashboardReport(String journeyMapId, String beginFilterDate, String endFilterDate, String timeUnit) {
	
		// customer profile funnel
		List<StatisticCollector> profileFunnelData = Analytics360DaoUtil.collectProfileFunnelStatistics(journeyMapId, beginFilterDate, endFilterDate);
		
		// total event 
		//List<StatisticCollector> eventTotalStats = Analytics360DaoUtil.collectTrackingEventTotalStatistics();
		//List<StatisticCollector> eventFunnelInDatetimeStats = Analytics360DaoUtil.collectTrackingEventTotalStatistics(beginFilterDate, endFilterDate);
		
		return new DashboardReport(beginFilterDate, endFilterDate, timeUnit, profileFunnelData);
	}

	/**
	 * @param journeyMapId
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @return dataForFunnelGraph
	 */
	public static List<List<Integer>> getDataForFunnelGraph(String journeyMapId, String beginFilterDate, String endFilterDate) {
		// customer journey report
		JourneyReport journeyReport = JourneyMapDao.getJourneyProfileStatistics(journeyMapId, beginFilterDate,endFilterDate);
		List<List<Integer>> journeyStatsData = journeyReport.getDataForFunnelGraph();
		return journeyStatsData;
	}


	/**
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @param timeUnit
	 * @return
	 */
	public static List<StatisticCollector> getEventTimeseriesData(String beginFilterDate, String endFilterDate, String timeUnit) {
		List<StatisticCollector> eventTimeseriesData;
		if(Analytics360Management.MONTHS.equals(timeUnit)) {
			eventTimeseriesData = Analytics360DaoUtil.collectEventMonthlyStatistics(beginFilterDate, endFilterDate);
		} 
		else if(Analytics360Management.HOURS.equals(timeUnit)) {
			eventTimeseriesData = Analytics360DaoUtil.collectEventHourlyStatistics(beginFilterDate, endFilterDate);
		} 
		else if(Analytics360Management.DAYS.equals(timeUnit)) {
			eventTimeseriesData = Analytics360DaoUtil.collectEventDailyStatistics(beginFilterDate, endFilterDate);
		} 
		else {
			eventTimeseriesData = Analytics360DaoUtil.collectEventYearlyStatistics(beginFilterDate, endFilterDate);
		}
		return eventTimeseriesData;
	}

	/**
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @param timeUnit
	 * @return
	 */
	public static List<StatisticCollector> getProfileTimeseriesData(String beginFilterDate, String endFilterDate, String timeUnit) {
		List<StatisticCollector> profileTimeseriesData;
		if(Analytics360Management.MONTHS.equals(timeUnit)) {
			profileTimeseriesData = Analytics360DaoUtil.collectProfileMonthlyStatistics(beginFilterDate, endFilterDate);
		} 
		else if(Analytics360Management.HOURS.equals(timeUnit)) {
			profileTimeseriesData = Analytics360DaoUtil.collectProfileHourlyStatistics(beginFilterDate, endFilterDate);
		} 
		else if(Analytics360Management.DAYS.equals(timeUnit)) {
			profileTimeseriesData = Analytics360DaoUtil.collectProfileDailyStatistics(beginFilterDate, endFilterDate);
		} 
		else {
			profileTimeseriesData = Analytics360DaoUtil.collectProfileYearlyStatistics(beginFilterDate, endFilterDate);
		}
		return profileTimeseriesData;
	}
	
	/**
	 * 
	 * Get Daily Report Units For All Profiles using date filter
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static DashboardEventReport getSummaryEventDataReport(String journeyMapId, String beginFilterDate, String endFilterDate) {
		DashboardEventReport report = DailyReportUnitDaoUtil.getDashboardEventReport(journeyMapId, Profile.COLLECTION_NAME, beginFilterDate, endFilterDate);
		return report;
	}
	
	/**
	 * @param key
	 * @return
	 */
	public static DashboardEventReport getSummaryEventDataReport(DashboardReportCacheKey key) {
		DashboardEventReport report = null;
		try {
			report = cacheEventReport.get(key);
			System.out.println("HIT CACHE DashboardEventReport");
		} catch (Exception e) {
			// skip
		}
		if(report == null) {
			System.out.println("MISS CACHE DashboardEventReport");
			report = getSummaryEventDataReport(key.journeyMapId, key.beginFilterDate, key.endFilterDate);
			cacheEventReport.put(key, report);
		}
		return report;
	}
	
	
	/**
	 * 
	 * Get Daily Report Units For One Profile using date filter
	 * 
	 * @param profileId
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static DashboardEventReport getDashboardEventReportForProfile(String journeyMapId, String profileId, String beginFilterDate, String endFilterDate) {
		DashboardEventReport report = DailyReportUnitDaoUtil.getDashboardEventReport(journeyMapId, Profile.COLLECTION_NAME, profileId, beginFilterDate, endFilterDate);
		return report;
	}
	
	/**
	 * @param profileId
	 * @param journeyId
	 * @return
	 */
	public static JourneyProfileReport getJourneyEventStatisticsForProfile(String profileId, String journeyId) {
		JourneyEventStatistics stats = JourneyMapDao.getJourneyMapStatisticsForProfile(profileId, journeyId);
		int funnelIndex = stats.getFunnelIndex();
		String funnelIndexName = DataFlowManagement.getFunnelStageByOrderIndex(funnelIndex).getName();
		JourneyProfileReport report = new JourneyProfileReport(stats.getDataForFunnelGraph(), stats.getJourneyFunnelValue(), funnelIndexName, stats.getScoreCX());
		return report;
	}
	

	/**
	 * 
	 * Get Daily Report Units For One Segment using date filter
	 * 
	 * @param profileId
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static DashboardEventReport getDashboardEventReportForSegment(String segmentId, String beginFilterDate, String endFilterDate) {
		Segment segment = SegmentDataManagement.getSegmentById(segmentId);
		DashboardEventReport finalReport = new DashboardEventReport(beginFilterDate, endFilterDate);
		if(segment != null) {
			DashboardEventReport report = DailyReportUnitDaoUtil.getSegmentDashboardEventReport(segment, beginFilterDate, endFilterDate);
			
			// daily
			finalReport.updateReportMap(segmentId, report.getReportMap());
			
			// summary
			finalReport.updateReportSummary(report.getReportSummary());
			
			// set total
			finalReport.setProfileCount(segment.getTotalCount());
		}
		return finalReport;
	}

	/**
	 * @param profileId
	 * @param journeyMapId
	 * @return
	 */
	public static JourneyMap getJourneyMapReportForProfile(String profileId, String journeyMapId) {
		return JourneyMapManagement.getJourneyMapReportForProfile(profileId, journeyMapId);
	}
	
	/**
	 * @param refJourneyId
	 * @param refProfileId
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @return
	 */
	public static Map<String, Object> getEventMatrixReportModel(String refJourneyId, String refProfileId, String beginFilterDate, String endFilterDate) {
		List<EventMatrixReport> dataList = Analytics360DaoUtil.getEventMatrixReports(refJourneyId, refProfileId, beginFilterDate, endFilterDate);
		return EventMatrixReport.toDataMap(dataList);
	}
}
