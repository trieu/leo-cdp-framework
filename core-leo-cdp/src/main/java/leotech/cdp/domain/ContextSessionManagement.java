package leotech.cdp.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;

import com.google.gson.Gson;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import leotech.cdp.dao.ContextSessionDaoUtil;
import leotech.cdp.domain.schema.BehavioralEvent;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.analytics.ContextSession;
import leotech.cdp.model.customer.LearningCourse;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileUpdateData;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointType;
import leotech.system.common.BaseHttpHandler;
import leotech.system.model.DeviceInfo;
import leotech.system.model.GeoLocation;
import leotech.system.util.GeoLocationUtil;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.UrlUtil;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;

/**
 * Context Session Management
 * 
 * @author @tantrieuf31
 * @since 2020
 */
public final class ContextSessionManagement {

	public static final int AFTER_30_MINUTES = 1800;
	static ShardedJedisPool jedisPool = RedisConfigs.load().get("realtimeDataStats").getShardedJedisPool();

	/**
	 * @param sourceIP
	 * @param params
	 * @param deviceInfo
	 * @param dateTime
	 * @param dateTimeKey
	 * @return
	 */
	public final static ContextSession createWebContextSession(String sourceIP, MultiMap params, DeviceInfo deviceInfo,
			DateTime dateTime, String dateTimeKey) {
		// profile params
		String visitorId = StringUtil.safeString(params.get(HttpParamKey.VISITOR_ID));
		if (StringUtil.isEmpty(visitorId)) {
			throw new IllegalArgumentException(
					"ContextSessionManagement.createWebContextSession is failed because the visitorId in params is NULL");
		}

		GeoLocation loc = GeoLocationUtil.getGeoLocation(sourceIP);

		String observerId = StringUtil.safeString(params.get(HttpParamKey.OBSERVER_ID));
		String userDeviceId = DeviceManagement.getDeviceId(params, deviceInfo);
		String mediaHost = StringUtil.safeString(params.get(HttpParamKey.MEDIA_HOST));
		String appId = StringUtil.safeString(params.get(HttpParamKey.APP_ID));

		// touchpoint params
		String touchpointName = StringUtil.decodeUrlUTF8(params.get(HttpParamKey.TOUCHPOINT_NAME)).trim();
		if (touchpointName.isEmpty()) {
			touchpointName = mediaHost;
		}

		String touchpointUrl = StringUtil.decodeUrlUTF8(params.get(HttpParamKey.TOUCHPOINT_URL));
		String touchpointRefUrl = StringUtil.decodeUrlUTF8(params.get(HttpParamKey.TOUCHPOINT_REFERRER_URL));
		String touchpointRefDomain = UrlUtil.getHostName(touchpointRefUrl);
		// owned media has data from itself , earned media is from facebook or google or
		// youtube
		boolean isFromOwnedMedia = mediaHost.equals(touchpointRefDomain);

		// touch-point info process
		Touchpoint refTouchPoint = TouchpointManagement.getOrCreate(touchpointRefDomain, TouchpointType.WEB_APP,
				touchpointRefUrl, isFromOwnedMedia);
		Touchpoint srcTouchpoint = TouchpointManagement.getOrCreateNew(touchpointName, TouchpointType.WEB_APP,
				touchpointUrl);
		String refTouchpointId = refTouchPoint.getId();
		String srcTouchpointId = srcTouchpoint.getId();

		String fingerprintId = StringUtil.safeString(params.get(HttpParamKey.FINGERPRINT_ID));

//		String email = StringUtil.safeString(params.get(ApiParamKey.EMAIL));
//		String phone = StringUtil.safeString(params.get(ApiParamKey.PHONE));
//		
//		String loginId = StringUtil.safeString(params.get(ApiParamKey.LOGIN_ID));
//		String loginIdProvider = StringUtil.safeString(params.get(ApiParamKey.LOGIN_PROVIDER));

		String env = StringUtil.safeString(params.get(HttpParamKey.DATA_ENVIRONMENT), HttpParamKey.DEV_ENV);
		String locationCode = loc.getLocationCode();

		// load profile ID from DB
		Profile profile = ProfileDataManagement.updateOrCreateFromWebTouchpoint(observerId, srcTouchpoint,
				touchpointRefDomain, sourceIP, visitorId, userDeviceId, fingerprintId);
		String profileId = profile.getId();
		visitorId = profile.getVisitorId();
		int profileType = profile.getType();

		// create new
		ContextSession ctxSession = new ContextSession(observerId, dateTime, dateTimeKey, locationCode, userDeviceId,
				sourceIP, mediaHost, appId, refTouchpointId, srcTouchpointId, profileId, profileType, visitorId,
				fingerprintId, env);

		// save into database
		ContextSessionDaoUtil.create(ctxSession);

		return ctxSession;
	}

	/**
	 * @param req
	 * @param params
	 * @param device
	 * @return
	 */
	public static ContextSession get(HttpServerRequest req, MultiMap params, DeviceInfo device) {
		return get(null, req, params, device);
	}

	/**
	 * to update session to merge profile data
	 * 
	 * @param srcProfile
	 * @param destProfile
	 */
	public static void updateContextSessionToMergeProfile(Profile srcProfile, Profile finalProfile) {
		String oldProfileId = srcProfile.getId();
		String newProfileId = finalProfile.getId();
		String newVisitorId = finalProfile.getVisitorId();

		// update database
		ContextSessionDaoUtil.updateContextSessionAfterMerge(oldProfileId, newProfileId, newVisitorId);

		// update redis
		srcProfile.getContextSessionKeys().forEach((String sessionKey, Date updatedDate) -> {
			RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
				@Override
				protected Boolean build() throws JedisException {
					String json = null;
					if (StringUtil.isNotEmpty(sessionKey)) {
						json = jedis.get(sessionKey);
					}
					if (json != null) {
						ContextSession ctxSession = new Gson().fromJson(json, ContextSession.class);
						ctxSession.setProfileId(newProfileId);
						ctxSession.setVisitorId(newVisitorId);

						String sessionJson = new Gson().toJson(ctxSession);

						Pipeline p = jedis.pipelined();
						p.del(sessionKey);
						p.set(sessionKey, sessionJson);
						p.expire(sessionKey, AFTER_30_MINUTES);
						p.sync();
						return true;
					}
					return false;
				}
			};
			boolean rs = cmd.execute().booleanValue();
			if (rs) {
				// merge session
				finalProfile.setContextSessionKey(sessionKey, updatedDate);
			}
		});
		srcProfile.clearContextSessionKeys();
	}

	/**
	 * get or create new context session for a profile
	 * 
	 * @param clientSessionKey
	 * @param req
	 * @param params
	 * @param device
	 * @return
	 */
	public static ContextSession get(final String clientSessionKey, HttpServerRequest req, MultiMap params,
			DeviceInfo device) {
		if (!device.isWebCrawler()) {
			RedisCommand<ContextSession> cmd = new RedisCommand<ContextSession>(jedisPool) {
				@Override
				protected ContextSession build() throws JedisException {
					String json = null;
					if (StringUtil.isNotEmpty(clientSessionKey)) {
						json = jedis.get(clientSessionKey);
					}
					ContextSession ctxSession = null;
					DateTime dateTime = new DateTime();

					if (json == null) {
						// the session is expired, so create a new one and commit to database
						String dateTimeKey = ContextSession.getSessionDateTimeKey(dateTime);
						String ip = HttpWebParamUtil.getRemoteIP(req);
						ctxSession = createWebContextSession(ip, params, device, dateTime, dateTimeKey);

						if (ctxSession != null) {
							String newSessionKey = ctxSession.getSessionKey();
							String sessionJson = new Gson().toJson(ctxSession);

							Pipeline p = jedis.pipelined();
							p.set(newSessionKey, sessionJson);
							p.expire(newSessionKey, AFTER_30_MINUTES);
							p.sync();
						}
					} else {
						// get from database for event recording
						ctxSession = new Gson().fromJson(json, ContextSession.class);

					}
					return ctxSession;
				}
			};
			return cmd.execute();
		}
		return null;
	}

	/**
	 * @param req
	 * @param params
	 * @param device
	 * @return
	 */
	public static ContextSession initSession(HttpServerRequest req, MultiMap params, DeviceInfo device) {
		final String ip = HttpWebParamUtil.getRemoteIP(req);
		RedisCommand<ContextSession> cmd = new RedisCommand<ContextSession>(jedisPool) {
			@Override
			protected ContextSession build() throws JedisException {
				ContextSession ctxSession = null;
				
				DateTime dateTime = new DateTime();
				String dateTimeKey = ContextSession.getSessionDateTimeKey(dateTime);

				// create a new one and commit to database
				ctxSession = createWebContextSession(ip, params, device, dateTime, dateTimeKey);
				
				if(ctxSession != null) {
					String newSessionKey = ctxSession.getSessionKey();
					String sessionJson = new Gson().toJson(ctxSession);

					Pipeline p = jedis.pipelined();
					p.set(newSessionKey, sessionJson);
					p.expire(newSessionKey, AFTER_30_MINUTES);
					p.sync();
				}
				
				return ctxSession;
			}
		};

		return cmd.execute();
	}

	/**
	 * @param req
	 * @param params
	 * @param ctxSession
	 * @param device
	 * @return
	 */
	public static int updateProfileData(HttpServerRequest req, MultiMap params, ContextSession ctxSession,DeviceInfo device) {
		String updatedProfileId = ctxSession.getProfileId();

		String observerId = ctxSession.getObserverId();
		String deviceId = DeviceManagement.getDeviceId(params, device);
		String environment = StringUtil.safeString(params.get(HttpParamKey.DATA_ENVIRONMENT), HttpParamKey.PRO_ENV);

		String fingerprintId = HttpWebParamUtil.getString(params, HttpParamKey.FINGERPRINT_ID);
		String usedDeviceId = ctxSession.getUserDeviceId();
		String sourceIP = HttpWebParamUtil.getRemoteIP(req);

		MultiMap formData = req.formAttributes();

		String srcObserverId = HttpWebParamUtil.getString(formData, HttpParamKey.OBSERVER_ID, "");
		String srcTouchpointName = HttpWebParamUtil.getString(formData, HttpParamKey.TOUCHPOINT_NAME, "");
		String srcTouchpointUrl = HttpWebParamUtil.getString(formData, HttpParamKey.TOUCHPOINT_URL, "");
		String refTouchpointUrl = StringUtil.safeString(req.getHeader(BaseHttpHandler.REFERER));
		String touchpointRefDomain = UrlUtil.getHostName(refTouchpointUrl);

		Map<String, Set<String>> extData = HttpWebParamUtil.getMapSetFromRequestParams(formData, HttpParamKey.EXT_DATA);
		Set<String> contentKeywords = extData.getOrDefault("contentKeywords", new HashSet<String>(0));
		Set<String> productKeywords = extData.getOrDefault("productKeywords", new HashSet<String>(0));

		Map<String, Object> profileData = HttpWebParamUtil.getHashMapFromRequestParams(formData, HttpParamKey.PROFILE_DATA);
		
		String email = HttpWebParamUtil.getString(profileData, "email");
		String phone = HttpWebParamUtil.getPhoneNumber(profileData, "phone");

		String firstName = HttpWebParamUtil.getString(profileData, "firstName");
		String lastName = HttpWebParamUtil.getString(profileData, "lastName");

		String livingLocation = HttpWebParamUtil.getString(profileData, "livingLocation");
		String workingHistory = HttpWebParamUtil.getString(profileData, "workingHistory");
		String jobTitles = HttpWebParamUtil.getString(profileData, "jobTitles");
		String personalProblems = HttpWebParamUtil.getString(profileData, "personalProblems");

		String genderStr = HttpWebParamUtil.getString(profileData, "genderStr");
		int age = StringUtil.safeParseInt(profileData.get("age"));
		String dateOfBirth = HttpWebParamUtil.getString(profileData, "dateOfBirth");

		String loginId = HttpWebParamUtil.getString(profileData, "loginId");
		String loginProvider = HttpWebParamUtil.getString(profileData, "loginProvider");

		Set<LearningCourse> learningCourses = HttpWebParamUtil.getLearningCourses(profileData);

		Touchpoint srcTouchpoint = TouchpointManagement.getOrCreateNew(srcTouchpointName, TouchpointType.WEB_APP, srcTouchpointUrl);

		ProfileUpdateData profileUpdate = new ProfileUpdateData(updatedProfileId, loginId, loginProvider, firstName,
				lastName, email, phone, genderStr, age, dateOfBirth, observerId, srcTouchpoint, sourceIP, usedDeviceId,
				contentKeywords, productKeywords, livingLocation, workingHistory, jobTitles, personalProblems,
				learningCourses);

		System.out.println(profileUpdate);
		System.out.println("isUpdateContact " + profileUpdate.isUpdateContact());
		String webformProvider = HttpWebParamUtil.getString(profileData, "webformProvider");
		String notificationProvider = HttpWebParamUtil.getString(profileData, "notificationProvider");

		try {
			// social login likes Facebook, LinkedIn, Google
			if (profileUpdate.isUpdateContact()) {
//				System.out.println("profileUpdate \n "+profileUpdate);
				// update profile
				String newProfileId = ProfileDataManagement.updateBasicProfileInfo(profileUpdate);

				if (!newProfileId.equals(updatedProfileId)) {
					ctxSession.setProfileId(newProfileId);
					ContextSessionDaoUtil.update(ctxSession);
				}

				String eventMetric = BehavioralEvent.STR_USER_LOGIN;
				if (StringUtil.isEmpty(loginId)) {
					eventMetric = BehavioralEvent.STR_SUBMIT_CONTACT;
				}

				EventObserverManagement.recordEventFromWeb(new Date(), ctxSession, srcObserverId, environment,
						fingerprintId, deviceId, sourceIP, device, srcTouchpointName, srcTouchpointUrl,
						refTouchpointUrl, touchpointRefDomain, eventMetric, profileData);
			}
			// confirmed notification
			else if (StringUtil.isNotEmpty(notificationProvider)) {
				String notificationUserId = HttpWebParamUtil.getString(profileData, "notificationUserId", "");
				ProfileDataManagement.setWebNotificationUserId(updatedProfileId, notificationProvider,
						notificationUserId);
			}
			// WEB FORM submit
			else if (StringUtil.isNotEmpty(webformProvider)) {
				ProfileDataManagement.updateBasicProfileInfo(profileUpdate);

				EventObserverManagement.recordEventFromWeb(new Date(), ctxSession, srcObserverId, environment,
						fingerprintId, deviceId, sourceIP, device, srcTouchpointName, srcTouchpointUrl,
						refTouchpointUrl, touchpointRefDomain, BehavioralEvent.STR_SUBMIT_CONTACT, profileData);
			}
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return 404;
		}

		return 102;
	}

	/**
	 * @param profileId
	 * @return
	 */
	public static ContextSession getByProfileId(String profileId) {
		List<ContextSession> sessions = ContextSessionDaoUtil.getSessionsByProfileId(profileId);
		if (sessions.size() > 0) {
			return sessions.get(0);
		}
		return null;
	}
}
