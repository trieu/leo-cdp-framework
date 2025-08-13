package leotech.cdp.model.analytics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.joda.time.DateTime;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.arangodb.model.TtlIndexOptions;
import com.google.gson.Gson;

import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileType;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * Context Session for Web Visitor
 * 
 * @author Trieu Nguyen
 * @since 2020
 *
 */
public final class ContextSession extends PersistentObject {

	public static final String DATE_HOUR_FORMAT_PATTERN = "yyyy-MM-dd-HH";
	static final DateFormat DATEHOUR_FORMAT = new SimpleDateFormat(DATE_HOUR_FORMAT_PATTERN);
	private static final int HOURS_OF_A_WEEK = 168;

	public static final String COLLECTION_NAME = getCdpCollectionName(ContextSession.class);
	static ArangoCollection instance;

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			instance = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields for fast lookup

			instance.ensurePersistentIndex(Arrays.asList("userDeviceId"),
					new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("locationCode"),
					new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("appId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("host"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("refTouchpointId"),
					new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("visitorId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("profileId"), new PersistentIndexOptions().unique(false));
			instance.ensureTtlIndex(Arrays.asList("autoDeleteAt"), new TtlIndexOptions().expireAfter(0));

		}
		return instance;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(ip) && StringUtil.isNotEmpty(sessionKey)
				&& StringUtil.isNotEmpty(refTouchpointId);
	}

	/**
	 * @param Date
	 * @return date string in format "yyyy-MM-dd-HH"
	 */
	public static String getSessionDateTimeKey(DateTime dt) {
		String tk = DATEHOUR_FORMAT.format(dt.toDate());
		int m = dt.getMinuteOfHour();
		if (m < 30) {
			tk += "-00";
		} else {
			tk += "-30";
		}
		return tk;
	}

	@Key
	String sessionKey;

	String dateTimeKey;
	String userDeviceId = "";
	String ip = "";
	String locationCode = "";
	String refMediaHost = "";
	String appId = "";
	String refTouchpointId = "";
	String srcTouchpointId = "";
	String observerId = "";
	String profileId = "";
	String visitorId = "";
	String fingerprintId = "";
	int profileType = ProfileType.ANONYMOUS_VISITOR;
	
	Date createdAt;
	Date updatedAt;
	
	Date autoDeleteAt;
	String environment;
	
	public ContextSession() {
		// 
	}

	public ContextSession(String observerId, DateTime dateTime, String dateTimeKey, String locationCode,
			String userDeviceId, String ip, String refMediaHost, String appId, String refTouchpointId,
			String srcTouchpointId, String profileId, int profileType, String visitorId, String fingerprintId, int hoursToDelete,
			String environment) {
		super();
		this.init(observerId, dateTime, dateTimeKey, locationCode, userDeviceId, ip, refMediaHost, appId, refTouchpointId,
				srcTouchpointId, profileId, profileType, visitorId,fingerprintId, hoursToDelete, environment);
	}

	public ContextSession(String observerId, DateTime dateTime, String dateTimeKey, String locationCode,
			String userDeviceId, String ip, String refMediaHost,  String appId, String refTouchpointId,
			String srcTouchpointId, String profileId, int profileType, String visitorId, String fingerprintId, String environment) {
		super();
		this.init(observerId, dateTime, dateTimeKey, locationCode, userDeviceId, ip, refMediaHost, appId, refTouchpointId,
				srcTouchpointId, profileId, profileType, visitorId, fingerprintId,  0, environment);
	}

	private void init(String observerId, DateTime dateTime, String dateTimeKey, String locationCode,
			String userDeviceId, String ip, String refMediaHost, String appId, String refTouchpointId,
			String srcTouchpointId, String profileId, int profileType, String visitorId, String fingerprintId, int hoursToDelete, String environment) {
		
		this.environment = environment;
		
		this.observerId = observerId;
		this.locationCode = locationCode;
		this.userDeviceId = userDeviceId;
		this.ip = ip;
		this.refMediaHost = refMediaHost;
		this.appId = appId;
		this.refTouchpointId = refTouchpointId;
		this.srcTouchpointId = srcTouchpointId;
		
		// profile keys
		this.profileId = profileId;
		this.profileType =  profileType;
		this.visitorId = visitorId;
		this.fingerprintId = fingerprintId;
		
		this.createdAt = dateTime.toDate();
		this.dateTimeKey = dateTimeKey;

		this.buildHashedId();

		if (hoursToDelete > HOURS_OF_A_WEEK) {
			this.autoDeleteAt = dateTime.plusHours(hoursToDelete).toDate();
		} else {
			this.autoDeleteAt = dateTime.plusHours(HOURS_OF_A_WEEK).toDate();
		}
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if(StringUtil.isNotEmpty(profileId) && StringUtil.isNotEmpty(userDeviceId) && StringUtil.isNotEmpty(fingerprintId)) {
			Calendar calendar = GregorianCalendar.getInstance(); 
			calendar.setTime(this.createdAt);  
			String mns = calendar.get(Calendar.MINUTE) < 30 ? "0" : "1";
			String keyHint =  environment + locationCode + userDeviceId + ip + appId + profileId + fingerprintId + mns;
			this.sessionKey = createHashedId(keyHint);
			return this.sessionKey;
		}
		else {
			throw new IllegalArgumentException("profileId and userDeviceId must not be NULL to create ContextSession");
		}
	}
	
	public void resetProfileKeys(Profile newProfile) {
		this.setVisitorId(newProfile.getVisitorId());
		this.setProfileId(newProfile.getId());
		this.setProfileType(newProfile.getType());
		this.setFingerprintId(newProfile.getFingerprintId());
		this.buildHashedId();
	}

	public String getUserDeviceId() {
		return userDeviceId;
	}

	public void setUserDeviceId(String userDeviceId) {
		this.userDeviceId = userDeviceId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getMediaHost() {
		return refMediaHost;
	}

	public void setMediaHost(String mediaHost) {
		this.refMediaHost = mediaHost;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getRefTouchpointId() {
		return refTouchpointId;
	}

	public void setRefTouchpointId(String refTouchpointId) {
		this.refTouchpointId = refTouchpointId;
	}

	public String getSrcTouchpointId() {
		return srcTouchpointId;
	}

	public void setSrcTouchpointId(String srcTouchpointId) {
		this.srcTouchpointId = srcTouchpointId;
	}
	
	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	
	public String getVisitorId() {
		return visitorId;
	}

	public void setVisitorId(String visitorId) {
		this.visitorId = visitorId;
	}

	public int getProfileType() {
		return profileType;
	}

	public void setProfileType(int profileType) {
		this.profileType = profileType;
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public Date getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public Date getAutoDeleteAt() {
		return autoDeleteAt;
	}

	public void setAutoDeleteAt(Date autoDeleteAt) {
		this.autoDeleteAt = autoDeleteAt;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public String getDateTimeKey() {
		return dateTimeKey;
	}

	public String getLocationCode() {
		return locationCode;
	}
	
	public void setLocationCode(String locationCode) {
		this.locationCode = locationCode;
	}

	public String getObserverId() {
		return observerId;
	}

	public void setObserverId(String observerId) {
		this.observerId = observerId;
	}

	public String getFingerprintId() {
		return fingerprintId;
	}

	public void setFingerprintId(String fingerprintId) {
		this.fingerprintId = fingerprintId;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + this.sessionKey;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
