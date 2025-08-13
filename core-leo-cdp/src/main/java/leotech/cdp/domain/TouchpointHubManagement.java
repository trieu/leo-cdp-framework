package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import leotech.cdp.dao.EventObserverDaoUtil;
import leotech.cdp.dao.TouchpointHubDaoUtil;
import leotech.cdp.model.analytics.TouchpointHubReport;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.model.journey.TouchpointType;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * Touchpoint Data Hub
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class TouchpointHubManagement {
	
	public static final String DEFAULT_FORM_NAME = "template-collect-profile";
	
	private static final int QUERY_CACHE_TIME = 48;
	
	static CacheLoader<String, TouchpointHub> cacheLoaderById = new CacheLoader<>() {
		@Override
		public TouchpointHub load(String id) {
			return TouchpointHubDaoUtil.getById(id);
		}
	};

	static CacheLoader<String, TouchpointHub> cacheLoaderByObserverId = new CacheLoader<>() {
		@Override
		public TouchpointHub load(String observerId) {
			return TouchpointHubDaoUtil.getByObserverId(observerId);
		}
	};
	
	static CacheLoader<String, List<TouchpointHub>> cacheLoaderByJourneyId = new CacheLoader<>() {
		@Override
		public List<TouchpointHub> load(String journeyMapId) {
			return TouchpointHubDaoUtil.getTouchpointHubsByJourneyMap(journeyMapId);
		}
	};

	static final LoadingCache<String, TouchpointHub> cacheById = CacheBuilder.newBuilder().maximumSize(50000).expireAfterWrite(5, TimeUnit.MINUTES).build(cacheLoaderById);
	static final LoadingCache<String, TouchpointHub> cacheByObserverId = CacheBuilder.newBuilder().maximumSize(50000).expireAfterWrite(QUERY_CACHE_TIME, TimeUnit.HOURS).build(cacheLoaderByObserverId);
	static final LoadingCache<String, List<TouchpointHub>> cacheByJourneyId = CacheBuilder.newBuilder().maximumSize(50000).expireAfterWrite(1, TimeUnit.MINUTES).build(cacheLoaderByJourneyId);
	
	/**
	 * @param id
	 * @return
	 */
	public static TouchpointHub getById(String id) {
		TouchpointHub hub = null;
		try {
			hub = cacheById.get(id);
		} catch (Exception e) {
			hub = TouchpointHubDaoUtil.getById(id);
		}
		return hub;
	}
	
	/**
	 * @param observerId
	 * if observerId not found, the DATA_OBSERVER is return
	 * @return TouchpointHub
	 */
	public static TouchpointHub getByObserverId(String observerId) {
		TouchpointHub hub = TouchpointHub.DATA_OBSERVER;
		try {
			hub = cacheByObserverId.get(observerId);
		} catch (Exception e) {
			hub = TouchpointHubDaoUtil.getByObserverId(observerId);
		}
		return hub;
	}
	
	/**
	 * @param journeyMapId
	 * @return List<TouchpointHub> in specified journey map
	 */
	public static List<TouchpointHub> getByJourneyId(String journeyMapId) {
		List<TouchpointHub> hubs = null;
		if(StringUtil.isNotEmpty(journeyMapId)) {
			try {
				hubs = cacheByJourneyId.get(journeyMapId);
			} catch (Exception e) {
				hubs = TouchpointHubDaoUtil.getTouchpointHubsByJourneyMap(journeyMapId);
			}
		}
		if(hubs == null) {
			hubs = new ArrayList<TouchpointHub>(0);
		}
		return hubs;
	}
	
	/**
	 * @param hostname
	 * @return
	 */
	public static TouchpointHub getByHostName(String hostname) {
		return TouchpointHubDaoUtil.getByHostName(hostname);
	}

	/**
	 * @param hubs
	 */
	public final static boolean saveTouchpointHubs(List<TouchpointHub> hubs) {		
		boolean ok = true;
		for (TouchpointHub touchpointHub : hubs) {
			touchpointHub.buildHashedId();
			
			String observerId = touchpointHub.getObserverId();
			if(StringUtil.isEmpty(observerId)) {
				// create new EventObserver and save touchpointHub
				ok = ok && saveTouchpointHubAndEventObserver(touchpointHub, new EventObserver(touchpointHub));
			} 
			else {
				// update EventObserver and save touchpointHub
				EventObserver observer = EventObserverDaoUtil.getById(observerId);
				if(observer != null) {
					observer.update(touchpointHub);
				} else {
					observer = new EventObserver(touchpointHub);
				}
				ok = ok && saveTouchpointHubAndEventObserver(touchpointHub, observer);
			}
		}
		return ok;
	}
	
	/**
	 * to reset access token value for the default_key in accessTokens
	 * 
	 * @param touchpointHubId
	 */
	public static String resetAccessTokenForLeoObserverApi(String observerId) {
		EventObserver observer;
		if(EventObserver.DEFAULT_ACCESS_KEY.equals(observerId)) {
			observer = EventObserverDaoUtil.getDefaultDataObserver();
		}
		else {
			observer = EventObserverDaoUtil.getById(observerId);
		}
		if(observer != null) {
			String tokenName = EventObserver.DEFAULT_ACCESS_KEY;
			if(observer.getType() != TouchpointType.DATA_OBSERVER) {
				tokenName = observerId;
			}
			String value = observer.generateAccessToken(tokenName, SystemMetaData.DOMAIN_CDP_ADMIN, true);
			boolean ok = EventObserverDaoUtil.save(observer) != null;
			if(ok) {
				return value;
			}
		}
		return "";
	}

	/**
	 * @param touchpointHub
	 * @param observer
	 */
	final static boolean saveTouchpointHubAndEventObserver(TouchpointHub touchpointHub, EventObserver observer) {
		// for offline channel likes Retail Store or POS
		String channelUrl = touchpointHub.getUrl();

		// mapping URL to offline location
		observer.generateQrCodeImage(channelUrl);
		
		// 
		if(observer.getType() == TouchpointType.DATA_OBSERVER) {
			observer.generateAccessToken(EventObserver.DEFAULT_ACCESS_KEY, SystemMetaData.DOMAIN_CDP_ADMIN, false);
		}
		else {
			observer.generateAccessToken(observer.getId(), SystemMetaData.DOMAIN_CDP_ADMIN, false);
		}
		
		// 
		boolean ok = false;
		String savedObserverId = EventObserverDaoUtil.save(observer);
		if(StringUtil.isNotEmpty(savedObserverId)) {
			touchpointHub.setObserverId(savedObserverId);
			ok = TouchpointHubDaoUtil.save(touchpointHub) != null;
		}
		return ok;
	}
	


	/**
	 * @param id
	 * @return
	 */
	public final static boolean delete(String id) {
		TouchpointHub m = TouchpointHubDaoUtil.getById(id);
		if(m != null) {
			String observerId = m.getObserverId();
			EventObserver obj = EventObserverDaoUtil.getById(observerId);
			if(obj != null) {
				EventObserverDaoUtil.delete(obj);
			}
			return TouchpointHubDaoUtil.delete(m);
		}
		return false;
	}
	
	/**
	 * @return
	 */
	public static List<TouchpointHub> getAll() {
		return TouchpointHubDaoUtil.getAll();
	}
	
	public static Map<String, String> getSortedMapOfTouchpointHubNames() {
		Map<String, String> map = new TreeMap<String, String>();
		TouchpointHubDaoUtil.getAll().forEach(e->{
			map.put(e.getUrl(), e.getName());
		});
		return map;
	}
	
	/**
	 * get Touchpoint Hub Report
	 * 
	 * @param journeyId
	 * @param touchpointType
	 * @param startIndex
	 * @param numberResult
	 * @return List<TouchpointHubReport>
	 */
	public static List<TouchpointHubReport> getTouchpointHubReport(String journeyId,  String beginFilterDate, String endFilterDate, int maxTouchpointHubSize) {
		return TouchpointHubDaoUtil.getTouchpointHubReport(journeyId, beginFilterDate, endFilterDate, 0, maxTouchpointHubSize);
	}
	
	/**
	 * get Touchpoint Hub Report For Profile
	 * 
	 * @param profileId
	 * @param journeyId
	 * @return List<TouchpointHubReport>
	 */
	public static List<TouchpointHubReport> getTouchpointHubReportForProfile(String profileId, String journeyId) {
		return TouchpointHubDaoUtil.getTouchpointHubReportForProfile(profileId, journeyId);
	}
}
