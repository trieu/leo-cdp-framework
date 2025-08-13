package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

import leotech.cdp.dao.JourneyMapDao;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.TouchpointHubDaoUtil;
import leotech.cdp.model.analytics.JourneyReport;
import leotech.cdp.model.analytics.TouchpointHubReport;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.JourneyMapRefKey;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.model.journey.TouchpointType;
import leotech.cdp.query.filters.DataFilter;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * Journey Map Data Management
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class JourneyMapManagement {

	public static final String GOOGLE_COM = "https://google.com";
	public static final String YOUTUBE_COM = "https://youtube.com";
	
	private static final int CACHE_TIME = 5;

	static CacheLoader<String, List<JourneyMap>> cacheLoader = new CacheLoader<>() {
		@Override
		public List<JourneyMap> load(String userLogin) {
			System.out.println("MISS CACHE JourneyMap");
			if(userLogin.isEmpty()) {
				return JourneyMapDao.getAllJourneyMaps();
			}
			return JourneyMapDao.getAllJourneyMapsForUser(userLogin);
		}
	};
	static final LoadingCache<String, List<JourneyMap>> cache = CacheBuilder.newBuilder()
			.maximumSize(50000).expireAfterWrite(CACHE_TIME, TimeUnit.MINUTES).build(cacheLoader);
	
	static CacheLoader<String, Set<JourneyMapRefKey>> cacheJourneyMapRefKeyLoader = new CacheLoader<>() {
		@Override
		public Set<JourneyMapRefKey> load(String idsAsStr) {
			List<String> ids = Arrays.asList(idsAsStr.split(StringPool.UNDERLINE));
			return JourneyMapDao.getRefKeysByIds(ids);
		}
	};
	static final LoadingCache<String, Set<JourneyMapRefKey>> cacheJourneyMapRefKey = CacheBuilder.newBuilder()
			.maximumSize(100000).expireAfterWrite(CACHE_TIME, TimeUnit.MINUTES).build(cacheJourneyMapRefKeyLoader);
	
	public static void clearCacheJourneyMap() {
		// FIXME so stupid to clear all, think better solution
		cache.invalidateAll();
	}
	
	/**
	 * @param idList
	 * @return List<JourneyMap>
	 */
	public static Set<JourneyMapRefKey> getRefKeysByIds(List<String> ids) {
		Set<JourneyMapRefKey> set = null;
		try {
			set = cacheJourneyMapRefKey.get(StringUtil.joinFromList(StringPool.UNDERLINE, ids));
		} catch (Exception e) {
			set = JourneyMapDao.getRefKeysByIds(ids);
		}
		return set;
	}
	
	/**
	 * for admin only
	 * 
	 * @param id
	 * @return
	 */
	public static JourneyMap getJourneyMapForAdminDashboard(String id) {
		return getById(id, true, false , true);
	}
	
	/**
	 * @param journeyId
	 * @param withTouchpointHubs
	 * @param reports
	 * @param returnDefaultMapIfNotFound
	 * @return
	 */
	public static JourneyMap getById(String journeyId, boolean withTouchpointHubs, boolean withFullReports, boolean returnDefaultMapIfNotFound) {
		// TODO caching data
		JourneyMap map;
		if(StringUtil.isEmpty(journeyId)) {
			map = JourneyMapManagement.getDefaultJourneyMap(true);
		}
		else {
			map = JourneyMapDao.getById(journeyId);
		}
		if (map != null) {
			if (withTouchpointHubs) {
				List<TouchpointHub> touchpointHubs = TouchpointHubDaoUtil.getTouchpointHubsByJourneyMap(journeyId);
				List<TouchpointHubReport> reports = null;
				if(withFullReports) {
					//reports = JourneyMapDao.getProfileReportForJourney(journeyId);
					reports = TouchpointHubDaoUtil.getTouchpointHubDetailReport(journeyId);
				}
				else {
					reports = TouchpointHubDaoUtil.getTouchpointHubProfileReport(journeyId);
				}
				
				return JourneyMap.setTouchpointHubsForJourneyMap(map, touchpointHubs, reports);
			}
			return map;
		} else if (returnDefaultMapIfNotFound) {
			return getDefaultJourneyMap(withTouchpointHubs);
		}
		throw new InvalidDataException("Not found any JourneyMap with id: " + journeyId);
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static JourneyMap getById(String id) {
		return getById(id, false, false, true);
	}
	

	/**
	 * @param id
	 * @param fullDataWithTouchpointHubs
	 * @param returnDefaultMapIfNotFound
	 * @return
	 */
	public static JourneyMap getById(String id, boolean fullDataWithTouchpointHubs, boolean returnDefaultMapIfNotFound) {
		return getById(id, fullDataWithTouchpointHubs, false, returnDefaultMapIfNotFound);
	}
	
	/**
	 * get all authorized JourneyMap 
	 * 
	 * @return List<JourneyMap>
	 */
	public static List<JourneyMap> getAllJourneyMapsForUser(SystemUser loginUser) {
		List<JourneyMap> list;
		String userLogin = loginUser.getUserLogin();
		try {
			if(loginUser.hasAdminRole()) {
				list = cache.get("");
			}
			else {
				list = cache.get(userLogin);
			}
			System.out.println("HIT CACHE JourneyMap");
		} catch (Exception e) {
			if(loginUser.hasAdminRole()) {
				list = JourneyMapDao.getAllJourneyMaps();
				cache.put("", list);
			}
			else {
				list = JourneyMapDao.getAllJourneyMapsForUser(userLogin);
				cache.put(userLogin, list);
			}
			System.out.println("MISS CACHE JourneyMap");
		}
		return list;
	}

	/**
	 * 
	 */
	public static void initDefaultSystemData() {
		List<TouchpointHub> hubs = new ArrayList<TouchpointHub>();

		TouchpointHub google = new TouchpointHub("Google", TouchpointType.SEARCH_ENGINE, false, GOOGLE_COM, 1);
		hubs.add(google);
		
		TouchpointHub youtube = new TouchpointHub("YouTube", TouchpointType.VIDEO_CHANNEL, false, YOUTUBE_COM, 2);
		hubs.add(youtube);

		JourneyMap map = JourneyMap.buildDefaultJourneyMap(hubs);
		JourneyMapManagement.saveJourney(map);
	}

	/**
	 * @param eventObserver
	 * @param srcTouchpoint
	 * @return
	 */
	public static String getIdFromObserverOrTouchpoint(EventObserver eventObserver, Touchpoint srcTouchpoint) {
		String updateJourneyId = JourneyMap.DEFAULT_JOURNEY_MAP_ID;
		if (eventObserver != null) {
			// TODO caching data
			String journeyMapId = eventObserver.getJourneyMapId();
			if (StringUtil.isNotEmpty(journeyMapId)) {
				updateJourneyId = journeyMapId;
			}
		} else if (srcTouchpoint != null) {
			// TODO caching data
			TouchpointHub hub = TouchpointHubManagement.getByHostName(srcTouchpoint.getHostname());
			if (hub != null) {
				String journeyMapId = hub.getJourneyMapId();
				if (StringUtil.isNotEmpty(journeyMapId)) {
					updateJourneyId = journeyMapId;
				}
			}
		}
		return updateJourneyId;
	}

	/**
	 * 
	 */
	public static void upgradeDefaultSystemData() {
		List<TouchpointHub> hubs = TouchpointHubManagement.getAll();
		List<TouchpointHub> upgradeHubs = new ArrayList<TouchpointHub>(hubs.size());
		Set<String> indexUrl = new HashSet<>(hubs.size());
		for (TouchpointHub hub : hubs) {
			String url = hub.getUrl();
			if (indexUrl.contains(url)) {
				TouchpointHubManagement.delete(hub.getId());
			} else {
				hub.setJourneyMapId(JourneyMap.DEFAULT_JOURNEY_MAP_ID);
				upgradeHubs.add(hub);
				indexUrl.add(url);
			}
		}
		JourneyMap map = JourneyMap.buildDefaultJourneyMap(new ArrayList<>(upgradeHubs));
		JourneyMapManagement.saveJourney(map);
	}


	
	/**
	 * get all JourneyMap 
	 * 
	 * @return List<JourneyMap>
	 */
	public static List<JourneyMap> getAllJourneyMaps() {
		 return JourneyMapDao.getAllJourneyMaps();
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload loadJourneyMapsByFilter(DataFilter filter) {
		return JourneyMapDao.loadJourneyMapsByFilter(filter);
	}



	/**
	 * @param map
	 */
	public static boolean saveJourney(JourneyMap map) {
		boolean ok = JourneyMapDao.save(map) != null;
		ok = ok && TouchpointHubManagement.saveTouchpointHubs(map.getSortedTouchpointHubs());
		clearCacheJourneyMap();
		return ok;
	}

	/**
	 * for leocdp admin webapp, 
	 * 
	 * @param journeyMapId
	 * @param touchpointHubs
	 * @return
	 */
	public static JourneyMap saveTouchpointHubsAndEventObservers(String journeyMapId, List<TouchpointHub> touchpointHubs) {
		JourneyMap map = getById(journeyMapId);
		map = JourneyMap.setTouchpointHubsForJourneyMap(map, touchpointHubs, true);
		boolean ok = saveJourney(map);
		if (ok) {
			return map;
		}
		return null;
	}


	
	/**
	 * @param journeyMapId
	 * @return
	 */
	public final static JourneyMap getJourneyMap(String journeyMapId) {
		JourneyMap journeyMap;
		if(StringUtil.isEmpty(journeyMapId)) {
			journeyMap = JourneyMapManagement.getDefaultJourneyMap(false);
		}
		else {
			journeyMap = JourneyMapManagement.getById(journeyMapId, false, true);
		}
		return journeyMap;
	}
	
	


	/**
	 * @param name
	 * @return
	 */
	public static JourneyMap createNewAndSave(String name) {
		if (JourneyMapDao.getByName(name) == null) {
			JourneyMap map = new JourneyMap(name);
			String mapId = map.getId();

			List<TouchpointHub> touchpointHubs = new ArrayList<TouchpointHub>(2);
			touchpointHubs.add(new TouchpointHub("Google", TouchpointType.SEARCH_ENGINE, false, GOOGLE_COM, 1, mapId));

			TouchpointHub leoObserverHub = TouchpointHub.DATA_OBSERVER;
			leoObserverHub.setJourneyMapId(mapId);
			touchpointHubs.add(leoObserverHub);

			map = JourneyMap.setTouchpointHubsForJourneyMap(map, touchpointHubs, true);
			if (saveJourney(map)) {
				return map;
			}
			return null;
		} else {
			throw new InvalidDataException("Error - Duplicated journey map with the name " + name);
		}
	}

	/**
	 * @param fullDataWithTouchpointHubs
	 * @return
	 */
	public static JourneyMap getDefaultJourneyMap(boolean fullDataWithTouchpointHubs) {
		if (fullDataWithTouchpointHubs) {
			String id = JourneyMap.DEFAULT_JOURNEY_MAP_ID;
			List<TouchpointHub> touchpointHubs = TouchpointHubDaoUtil.getTouchpointHubsByJourneyMap(id);
			return JourneyMap.buildDefaultJourneyMap(touchpointHubs);
		}
		return JourneyMapDao.getById(JourneyMap.DEFAULT_JOURNEY_MAP_ID);
	}

	/**
	 * @param beginFilterDate
	 * @param endFilterDate
	 * @return
	 */
	public static JourneyReport getJourneyProfileStatistics(String beginFilterDate, String endFilterDate) {
		JourneyReport rs = JourneyMapDao.getJourneyProfileStatistics(beginFilterDate, endFilterDate);
		return rs;
	}

	/**
	 * the data is not deleted, we need to remove it from valid data view, set
	 * status of object = -4
	 * 
	 * @param journeyMapId
	 * @return
	 */
	/**
	 * @param journeyMapId
	 * @return
	 */
	public static boolean deleteJourneyMap(String journeyMapId) {
		if (JourneyMap.DEFAULT_JOURNEY_MAP_ID.equals(journeyMapId)) {
			throw new InvalidDataException("Can not delete default journey map !");
		} else {
			JourneyMap map = JourneyMapDao.getById(journeyMapId);
			if (map != null) {
				List<TouchpointHub> touchpointHubs = TouchpointHubDaoUtil.getTouchpointHubsByJourneyMap(journeyMapId);
				for (TouchpointHub h : touchpointHubs) {
					TouchpointHubDaoUtil.delete(h);
				}
				clearCacheJourneyMap();
				return JourneyMapDao.delete(map);
			}
			throw new InvalidDataException("Can not delete default journey map !");
		}
	}

	/**
	 * @param journeyMapId
	 * @param name
	 * @return
	 */
	public static JourneyMap updateName(String journeyMapId, String name) {
		JourneyMap map = getById(journeyMapId, true, false, false);
		if (map != null) {
			map.setName(name);
			boolean ok = JourneyMapDao.save(map) != null;
			if (ok) {
				clearCacheJourneyMap();
				return map;
			}
		}
		throw new InvalidDataException("Not found journey, system can not update name for journey map id:" + journeyMapId);
	}
	
	/**
	 * @param journeyMapId
	 * @param authorizedViewers
	 * @param authorizedEditors
	 * @return
	 */
	public static JourneyMap updateJourneyDataAuthorization(String journeyMapId, Set<String> authorizedViewers, Set<String> authorizedEditors, boolean updateAllProfilesInJourney) {
		JourneyMap map = getById(journeyMapId, true, false, false);
		if (map != null) {
			Set<String> removedViewers = map.updateAuthorizedViewersAndReturnRemovedViewers(authorizedViewers);
			Set<String> removedEditors = map.updateAuthorizedEditorsAndReturnRemovedEditors(authorizedEditors);
			boolean ok = JourneyMapDao.save(map) != null;
			if (ok) { 
				clearCacheJourneyMap();
				ProfileDaoUtil.updateProfileAuthorizationByJourneyId(journeyMapId, authorizedViewers, authorizedEditors, removedViewers, removedEditors, updateAllProfilesInJourney);
				return map;
			}
		}
		throw new InvalidDataException("Not found journey, system can not update User Authorization for journey map id:" + journeyMapId);
	}
	
	public static JourneyMap updateJourneyDataAuthorization(String journeyMapId, String authorizedViewer, String authorizedEditor, String userLogin, boolean updateAllProfilesInJourney) {
		JourneyMap journey = getById(journeyMapId, true, false, false);
		if (journey != null) {
			Set<String> authorizedViewers = Sets.newHashSet(), authorizedEditors= Sets.newHashSet(), removedViewers= Sets.newHashSet(), removedEditors= Sets.newHashSet();
			if(authorizedViewer.equals(userLogin)) {
				authorizedViewers = journey.setAuthorizedViewer(userLogin);
			}
			else {
				removedViewers = journey.removeAuthorizedViewer(userLogin);
			}
			if(authorizedEditor.equals(userLogin)) {
				authorizedEditors = journey.setAuthorizedEditor(userLogin);
			}
			else {
				removedEditors = journey.removeAuthorizedEditor(userLogin);
			}
			String saveId = JourneyMapDao.save(journey);
			if (saveId != null) {
				ProfileDaoUtil.updateProfileAuthorizationByJourneyId(journeyMapId, authorizedViewers, authorizedEditors, removedViewers, removedEditors, updateAllProfilesInJourney);
				return journey;
			}
		}
		throw new InvalidDataException("Not found journey, system can not update User Authorization for journey map id:" + journeyMapId);
	}


	
	/**
	 * @param profileId
	 * @param journeyId
	 * @return
	 */
	public static JourneyMap getJourneyMapReportForProfile(String profileId, String journeyId) {
		List<TouchpointHubReport> reports = JourneyMapDao.getJourneyReportForProfile(profileId, journeyId);
		JourneyMap map = JourneyMapDao.getById(journeyId);
		List<TouchpointHub> touchpointHubs = TouchpointHubDaoUtil.getTouchpointHubsByJourneyMap(journeyId);
		return JourneyMap.setTouchpointHubsForJourneyMap(map, touchpointHubs, reports);
	}
	
	/**
	 * @param userLogin
	 */
	public static void removeAllAuthorizedJourneys(String userLogin) {
		JourneyMapDao.removeAllAuthorizedJourneys(userLogin);
	}

}
