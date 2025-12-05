package leotech.cdp.domain;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import leotech.cdp.dao.TouchpointDaoUtil;
import leotech.cdp.model.analytics.TouchpointFlowReport;
import leotech.cdp.model.analytics.TouchpointReport;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointType;
import leotech.cdp.query.filters.DataFilter;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.util.IdGenerator;
import leotech.system.util.UrlUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;


/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class TouchpointManagement {

	private static final int CACHE_TIME = 15;

	static class TouchpointFlowReportCacheKey {
		public final String refProfileId, refJourneyId, beginFilterDate, endFilterDate;
		public final int startIndex, numberFlow;
		String cacheKey;

		public TouchpointFlowReportCacheKey(String refJourneyId, String beginFilterDate, String endFilterDate,
				int startIndex, int numberFlow) {
			super();
			this.refProfileId = "";
			this.refJourneyId = refJourneyId;
			this.beginFilterDate = beginFilterDate;
			this.endFilterDate = endFilterDate;
			this.startIndex = startIndex;
			this.numberFlow = numberFlow;
			buildCacheKey();
		}

		public TouchpointFlowReportCacheKey(String refProfileId, String refJourneyId, String beginFilterDate,
				String endFilterDate, int startIndex, int numberFlow) {
			super();
			this.refProfileId = refProfileId;
			this.refJourneyId = refJourneyId;
			this.beginFilterDate = beginFilterDate;
			this.endFilterDate = endFilterDate;
			this.startIndex = startIndex;
			this.numberFlow = numberFlow;
			buildCacheKey();			
		}
		
		void buildCacheKey() {
			String keyHint = this.refProfileId + refJourneyId + beginFilterDate + endFilterDate + startIndex + "" + numberFlow;
			this.cacheKey = IdGenerator.createHashedId(keyHint);
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

	// ------- BEGIN Main Dashboard
	static CacheLoader<TouchpointFlowReportCacheKey, List<TouchpointFlowReport>> cacheLoaderTouchpointFlowReport = new CacheLoader<>() {
		@Override
		public List<TouchpointFlowReport> load(TouchpointFlowReportCacheKey key) {
			if(key != null && StringUtil.isEmpty(key.refProfileId)) {
				System.out.println("MISS CACHE , cache load getTouchpointFlowReportForJourney");
				return getTouchpointFlowReportForJourney(key.refJourneyId, key.beginFilterDate, key.endFilterDate,
						key.startIndex, key.numberFlow);
			}
			else {
				System.out.println("MISS CACHE , cache load getTouchpointFlowReportForProfile");
				return getTouchpointFlowReportForProfile(key.refProfileId, key.refJourneyId, key.beginFilterDate, key.endFilterDate,
						key.startIndex, key.numberFlow);
			}
		}
	};
	static final LoadingCache<TouchpointFlowReportCacheKey, List<TouchpointFlowReport>> cacheTouchpointFlowReport = CacheBuilder
			.newBuilder().maximumSize(10000).expireAfterWrite(CACHE_TIME, TimeUnit.MINUTES)
			.build(cacheLoaderTouchpointFlowReport);
	
	static CacheLoader<String, Touchpoint> cacheLoaderTouchpoint = new CacheLoader<>() {
		@Override
		public Touchpoint load(String id) {
			return TouchpointDaoUtil.getById(id);
		}
	};
	static final LoadingCache<String, Touchpoint> cacheTouchpoint = CacheBuilder
			.newBuilder().maximumSize(200000).expireAfterWrite(CACHE_TIME, TimeUnit.MINUTES)
			.build(cacheLoaderTouchpoint);

	public static final Touchpoint DIRECT_TRAFFIC_WEB = new Touchpoint("DIRECT_TRAFFIC_WEB", TouchpointType.WEB_APP,
			"DIRECT_TRAFFIC_WEB", false);
	public static final Touchpoint LEO_CDP_ADMIN_FORM = new Touchpoint("LEO_CDP_ADMIN_FORM", TouchpointType.CDP_API,
			"LEO_CDP_ADMIN_FORM", false);
	public static final Touchpoint CRM_IMPORT = new Touchpoint("CRM_IMPORT", TouchpointType.CDP_API, "CRM_IMPORT",
			false);
	public static final Touchpoint LEO_CDP_API = new Touchpoint("LEO_CDP_API", TouchpointType.CDP_API, "LEO_CDP_API",
			false);
	public static final Touchpoint LEO_CDP_IMPORTER = new Touchpoint("LEO_CDP_IMPORTER", TouchpointType.CDP_API,
			"LEO_CDP_IMPORTER", false);

	static {
		init();
	}

	protected static void init() {
		try {
			TouchpointDaoUtil.save(DIRECT_TRAFFIC_WEB);
			TouchpointDaoUtil.save(LEO_CDP_ADMIN_FORM);
			TouchpointDaoUtil.save(CRM_IMPORT);
			TouchpointDaoUtil.save(LEO_CDP_API);
			TouchpointDaoUtil.save(LEO_CDP_IMPORTER);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * get Touchpoint from cache, if not found then query database
	 * 
	 * @param id
	 * @return Touchpoint
	 */
	public final static Touchpoint getById(String id) {
		Touchpoint tp = null;
		try {
			tp = cacheTouchpoint.get(id);
		} catch (Exception e) {
			// skip
		}
		if(tp == null) {
			tp = TouchpointDaoUtil.getById(id);
		}
		return tp;
	}

	/**
	 * @param type
	 * @param touchpointUrl
	 * @param firstPartyData
	 * @return
	 */
	private static Touchpoint getOrCreateParentTouchpoint(int type, String touchpointUrl, boolean firstPartyData) {
		String hostname = UrlUtil.getHostName(touchpointUrl);
		Touchpoint tp = TouchpointDaoUtil.getByName(hostname);
		if (tp == null) {
			tp = new Touchpoint(hostname, TouchpointType.WEBSITE, hostname);
			tp.setFirstPartyData(firstPartyData);
			tp.setRootNode(true);
			TouchpointDaoUtil.save(tp);
		}
		return tp;
	}

	/**
	 * the main function to get Touchpoint , to init new session or save tracking
	 * event
	 * 
	 * @param name
	 * @param touchpointType
	 * @param touchpointUrl
	 * @param firstPartyData
	 * @return
	 */
	public final static Touchpoint getOrCreate(String name, int touchpointType, String touchpointUrl, boolean firstPartyData) {
		Touchpoint tp = DIRECT_TRAFFIC_WEB;
		if (StringUtil.isNotEmpty(touchpointUrl)) {
			tp = TouchpointDaoUtil.getByUrl(touchpointUrl);
			if (tp == null) {
				Touchpoint parent = getOrCreateParentTouchpoint(touchpointType, touchpointUrl, firstPartyData);

				tp = new Touchpoint(name, touchpointType, touchpointUrl);
				tp.setFirstPartyData(firstPartyData);
				tp.setParentId(parent.getId());

				// run in a threadpool
				TouchpointDaoUtil.save(tp);
			}
		}
		return tp;
	}

	/**
	 * @param touchpointUrl
	 * @param name
	 * @param touchpointType
	 * @return
	 */
	public final static Touchpoint importTouchpoint(String touchpointUrl, String name, int touchpointType) {
		if (StringUtil.isNotEmpty(touchpointUrl)) {
			Touchpoint tp = TouchpointDaoUtil.getByUrl(touchpointUrl);
			if (tp == null) {
				Touchpoint parent = getOrCreateParentTouchpoint(touchpointType, touchpointUrl, true);

				tp = new Touchpoint(name, touchpointType, touchpointUrl);
				tp.setFirstPartyData(true);
				tp.setParentId(parent.getId());

				TouchpointDaoUtil.save(tp);
			}
			return tp;
		}
		return LEO_CDP_IMPORTER;
	}



	/**
	 * for fake test generator
	 * 
	 * @param createdAt
	 * @param name
	 * @param type
	 * @param touchpointUrl
	 * @param firstPartyData
	 * @return
	 */
	public static Touchpoint getOrCreateForTesting(Date createdAt, String name, int type, String touchpointUrl,
			boolean firstPartyData) {
		if (StringUtil.isNotEmpty(touchpointUrl)) {
			Touchpoint tp = TouchpointDaoUtil.getByUrl(touchpointUrl);

			if (tp == null) {
				Touchpoint parent = getOrCreateParentTouchpoint(type, touchpointUrl, firstPartyData);

				tp = new Touchpoint(name, type, touchpointUrl);
				tp.setFirstPartyData(firstPartyData);
				tp.setParentId(parent.getId());
				tp.setCreatedAt(createdAt);

				// run in a threadpool
				TouchpointDaoUtil.save(tp);
			}
			return tp;
		}
		return DIRECT_TRAFFIC_WEB;
	}

	/**
	 * @param name
	 * @param type
	 * @param touchpointUrl
	 * @return
	 */
	public static Touchpoint getOrCreateNew(String name, int type, String touchpointUrl) {
		if (StringUtil.isNotEmpty(touchpointUrl)) {
			if( ! touchpointUrl.contains("://") ) {
				touchpointUrl = "https://"+SystemMetaData.DOMAIN_CDP_OBSERVER+"/touchpoint?q="+ touchpointUrl + " _at_ " + name;
			}
			Touchpoint tp = TouchpointDaoUtil.getByUrlAndName(touchpointUrl, name);
			if (tp == null) {
				Touchpoint parentTouchpoint = getOrCreateParentTouchpoint(type, touchpointUrl, true);
				tp = new Touchpoint(name, type, touchpointUrl);
				tp.setParentId(parentTouchpoint.getId());
				// run in a threadpool
				TouchpointDaoUtil.save(tp);
			} else {
				// update name if the data source is different
				if (!name.equalsIgnoreCase(tp.getName())) {
					tp.setName(name);
					TouchpointDaoUtil.save(tp);
				}
			}
			return tp;
		}
		return DIRECT_TRAFFIC_WEB;
	}

	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload filter(DataFilter filter) {
		JsonDataTablePayload rs = TouchpointDaoUtil.filter(filter);
		// TODO caching
		return rs;
	}

	/**
	 * @param journeyId
	 * @param touchpointType
	 * @param startIndex
	 * @param numberResult
	 * @return List<TouchpointReport>
	 */
	public static List<TouchpointReport> getTouchpointReport(String journeyId, int touchpointType,
			String beginFilterDate, String endFilterDate, int startIndex, int numberResult) {
		return TouchpointDaoUtil.getTouchpointReport(journeyId, touchpointType, beginFilterDate, endFilterDate,
				startIndex, numberResult);
	}

	/**
	 * @param profileId
	 * @param journeyId
	 * @param touchpointType
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<TouchpointReport> getTouchpointReportForProfile(String profileId, String journeyId,
			int touchpointType, int startIndex, int numberResult) {
		return TouchpointDaoUtil.getTouchpointReportForProfile(profileId, journeyId, touchpointType, startIndex, numberResult);
	}

	/**
	 * get Touchpoint Flow Report For Journey
	 * 
	 * @param refJourneyId
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @return
	 */
	public static List<TouchpointFlowReport> getTouchpointFlowReportForJourney(String refJourneyId,
			String beginFilterDate, String endFilterDate, int startIndex, int numberFlow) {
		List<TouchpointFlowReport> report = null;
		TouchpointFlowReportCacheKey key = new TouchpointFlowReportCacheKey(refJourneyId, beginFilterDate,
				endFilterDate, startIndex, numberFlow);
		try {
			report = cacheTouchpointFlowReport.get(key);
			System.out.println("getTouchpointFlowReportForJourney HIT CACHE TouchpointFlowReport");
		} catch (Exception e) {
			// skip
		}
		if (report == null) {
			System.out.println("getTouchpointFlowReportForJourney MISS CACHE TouchpointFlowReport");
			report = TouchpointDaoUtil.getTouchpointFlowReportStatistics("", refJourneyId, beginFilterDate,
					endFilterDate, startIndex, numberFlow);
			cacheTouchpointFlowReport.put(key, report);
		}
		return report;
	}

	/**
	 * get Touchpoint Flow Report For Profile
	 * 
	 * @param refProfileId
	 * @param refJourneyId
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @return
	 */
	public static List<TouchpointFlowReport> getTouchpointFlowReportForProfile(String refProfileId, String refJourneyId,
			String beginFilterDate, String endFilterDate, int startIndex, int numberFlow) {
		List<TouchpointFlowReport> report = null;
		TouchpointFlowReportCacheKey key = new TouchpointFlowReportCacheKey(refProfileId, refJourneyId, beginFilterDate,endFilterDate, startIndex, numberFlow);
		try {
			report = cacheTouchpointFlowReport.get(key);
			System.out.println("getTouchpointFlowReportForProfile HIT CACHE TouchpointFlowReport");
		} catch (Exception e) {
			// skip
		}
		if (report == null) {
			System.out.println("getTouchpointFlowReportForProfile MISS CACHE TouchpointFlowReport");
			report = TouchpointDaoUtil.getTouchpointFlowReportStatistics(refProfileId, refJourneyId, beginFilterDate, endFilterDate, startIndex, numberFlow);
			cacheTouchpointFlowReport.put(key, report);
		}
		return report;
	}

}
