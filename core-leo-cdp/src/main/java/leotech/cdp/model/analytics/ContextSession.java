package leotech.cdp.model.analytics;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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
 * Context Session for Web Visitor <br>
 * 
 * Context Session is created when a visitor first comes to the website, and it
 * will be updated with new touchpoint and event data until the session expires. <br>
 * 
 * ArangoDB collection: cdp_contextsession
 * 
 * @author Trieu Nguyen
 * @since 2020
 */
public final class ContextSession extends PersistentObject {

	public static final String DATE_HOUR_FORMAT_PATTERN = "yyyy-MM-dd-HH";
	
	// Joda-Time's thread-safe DateTimeFormatter
	private static final DateTimeFormatter DATEHOUR_FORMAT = DateTimeFormat.forPattern(DATE_HOUR_FORMAT_PATTERN);
	
	private static final int HOURS_OF_A_WEEK = 168;

	public static final String COLLECTION_NAME = getCdpCollectionName(ContextSession.class);
	
	// ensure proper double-checked locking behavior
	private static volatile ArangoCollection instance;

	@Key
	private String sessionKey;
	
	private String dateTimeKey;
	private String userDeviceId = "";
	private String ip = "";
	private String locationCode = "";
	private String refMediaHost = "";
	private String appId = "";
	private String refTouchpointId = "";
	private String srcTouchpointId = "";
	private String observerId = "";
	private String profileId = "";
	private String visitorId = "";
	private String fingerprintId = "";
	private int profileType = ProfileType.ANONYMOUS_VISITOR;
	
	private Date createdAt;
	private Date updatedAt;
	private Date autoDeleteAt;
	private String environment;

	public ContextSession() {
		// Default constructor for serialization
	}

	public ContextSession(String observerId, DateTime dateTime, String dateTimeKey, String locationCode,
			String userDeviceId, String ip, String refMediaHost, String appId, String refTouchpointId,
			String srcTouchpointId, String profileId, int profileType, String visitorId, String fingerprintId, 
			String environment) {
		
		// Refactored to use constructor chaining
		this(observerId, dateTime, dateTimeKey, locationCode, userDeviceId, ip, refMediaHost, appId, refTouchpointId,
				srcTouchpointId, profileId, profileType, visitorId, fingerprintId, 0, environment);
	}

	public ContextSession(String observerId, DateTime dateTime, String dateTimeKey, String locationCode,
			String userDeviceId, String ip, String refMediaHost, String appId, String refTouchpointId,
			String srcTouchpointId, String profileId, int profileType, String visitorId, String fingerprintId, 
			int hoursToDelete, String environment) {
		
		this.environment = environment;
		this.observerId = observerId;
		this.locationCode = locationCode;
		this.userDeviceId = userDeviceId;
		this.ip = ip;
		this.refMediaHost = refMediaHost;
		this.appId = appId;
		this.refTouchpointId = refTouchpointId;
		this.srcTouchpointId = srcTouchpointId;
		
		this.profileId = profileId;
		this.profileType = profileType;
		this.visitorId = visitorId;
		this.fingerprintId = fingerprintId;
		
		this.createdAt = dateTime.toDate();
		this.dateTimeKey = dateTimeKey;

		this.buildHashedId();

		int actualHoursToDelete = (hoursToDelete > HOURS_OF_A_WEEK) ? hoursToDelete : HOURS_OF_A_WEEK;
		this.autoDeleteAt = dateTime.plusHours(actualHoursToDelete).toDate();
	}

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			// Double-checked locking to prevent race conditions during concurrent index creation
			synchronized (ContextSession.class) {
				if (instance == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);

					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);
					
					// ensure indexing key fields for fast lookup
					col.ensurePersistentIndex(Arrays.asList("userDeviceId"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("locationCode"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("appId"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("host"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("refTouchpointId"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("visitorId"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("profileId"), pIdxOpts);
					
					col.ensureTtlIndex(Arrays.asList("autoDeleteAt"), new TtlIndexOptions().expireAfter(0));
					
					instance = col;
				}
			}
		}
		return instance;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(ip) 
				&& StringUtil.isNotEmpty(sessionKey)
				&& StringUtil.isNotEmpty(refTouchpointId);
	}

	/**
	 * @param dt org.joda.time.DateTime
	 * @return date string in format "yyyy-MM-dd-HH-[00|30]"
	 */
	public static String getSessionDateTimeKey(DateTime dt) {
		// Simplified and safe from threading issues
		String tk = DATEHOUR_FORMAT.print(dt);
		return tk + (dt.getMinuteOfHour() < 30 ? "-00" : "-30");
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (StringUtil.isNotEmpty(profileId) && StringUtil.isNotEmpty(userDeviceId) && StringUtil.isNotEmpty(fingerprintId)) {
			// Refactored to use JDK 11 java.time API instead of legacy GregorianCalendar
			LocalDateTime ldt = LocalDateTime.ofInstant(this.createdAt.toInstant(), ZoneId.systemDefault());
			
			String mns = ldt.getMinute() < 30 ? "0" : "1";
			String keyHint = environment + locationCode + userDeviceId + ip + appId + profileId + fingerprintId + mns;
			
			this.sessionKey = createHashedId(keyHint);
			return this.sessionKey;
		} else {
			throw new IllegalArgumentException("profileId, userDeviceId, and fingerprintId must not be NULL to create ContextSession");
		}
	}
	
	public void resetProfileKeys(Profile newProfile) {
		this.setVisitorId(newProfile.getVisitorId());
		this.setProfileId(newProfile.getId());
		this.setProfileType(newProfile.getType());
		this.setFingerprintId(newProfile.getFingerprintId());
		this.buildHashedId();
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	public String getUserDeviceId() { return userDeviceId; }
	public void setUserDeviceId(String userDeviceId) { this.userDeviceId = userDeviceId; }

	public String getIp() { return ip; }
	public void setIp(String ip) { this.ip = ip; }

	public String getMediaHost() { return refMediaHost; }
	public void setMediaHost(String mediaHost) { this.refMediaHost = mediaHost; }

	public String getAppId() { return appId; }
	public void setAppId(String appId) { this.appId = appId; }

	public String getRefTouchpointId() { return refTouchpointId; }
	public void setRefTouchpointId(String refTouchpointId) { this.refTouchpointId = refTouchpointId; }

	public String getSrcTouchpointId() { return srcTouchpointId; }
	public void setSrcTouchpointId(String srcTouchpointId) { this.srcTouchpointId = srcTouchpointId; }
	
	public String getProfileId() { return profileId; }
	public void setProfileId(String profileId) { this.profileId = profileId; }

	public String getVisitorId() { return visitorId; }
	public void setVisitorId(String visitorId) { this.visitorId = visitorId; }

	public int getProfileType() { return profileType; }
	public void setProfileType(int profileType) { this.profileType = profileType; }

	@Override
	public Date getCreatedAt() { return createdAt; }
	@Override
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

	@Override
	public Date getUpdatedAt() { return updatedAt; }
	@Override
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	public String getEnvironment() { return environment; }
	public void setEnvironment(String environment) { this.environment = environment; }

	public Date getAutoDeleteAt() { return autoDeleteAt; }
	public void setAutoDeleteAt(Date autoDeleteAt) { this.autoDeleteAt = autoDeleteAt; }

	public String getSessionKey() { return sessionKey; }

	public String getDateTimeKey() { return dateTimeKey; }

	public String getLocationCode() { return locationCode; }
	public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

	public String getObserverId() { return observerId; }
	public void setObserverId(String observerId) { this.observerId = observerId; }

	public String getFingerprintId() { return fingerprintId; }
	public void setFingerprintId(String fingerprintId) { this.fingerprintId = fingerprintId; }

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + this.sessionKey;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}