package leotech.cdp.model.analytics;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.journey.JourneyMap;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.StringUtil;

/**
 * Daily Report Unit for time series analytics <br>
 * DailyReportUnit is the basic unit for daily report data, it is used for
 * storing aggregated count of events for a specific object (e.g: profile,
 * product, content, etc) on a specific day. <br>
 * 
 * ArangoDB collection: cdp_dailyreportunit
 * 
 * @author tantrieu31
 * @since 2020
 */
public final class DailyReportUnit extends PersistentObject implements Comparable<DailyReportUnit> {

	private static final String YYYY_MM_DD_HH_00_00 = "yyyy-MM-dd HH:00:00";

	// Thread-safe formatters for JDK 11
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
			.ofPattern(DateTimeUtil.DATE_FORMAT_PATTERN).withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_00_00)
			.withZone(ZoneId.systemDefault());

	public static final String COLLECTION_NAME = getCdpCollectionName(DailyReportUnit.class);

	// for thread-safe lazy initialization
	private static volatile ArangoCollection instance;

	public static ArangoCollection getCollectionInstance() {
		if (instance == null) {
			synchronized (DailyReportUnit.class) {
				if (instance == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);
					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);


					// Covers: [objectName], [objectName, objectId], [objectName, objectId, journeyMapId], etc.
					col.ensurePersistentIndex(Arrays.asList("objectName", "objectId", "journeyMapId", "eventName"),pIdxOpts);

					// Fast lookup for daily timeseries charts
					col.ensurePersistentIndex(Arrays.asList("dateKey", "eventName"), pIdxOpts);

					// Master Time-based index. Covers granular querying by Date components.
					col.ensurePersistentIndex(Arrays.asList("year", "month", "day", "hour", "eventName", "objectName", "objectId"),pIdxOpts);

					// Master CreatedAt index.
					// Replaces 4 previous redundant indices: [createdAt], [createdAt, objectName],
					// etc.
					col.ensurePersistentIndex(Arrays.asList("createdAt", "objectName", "objectId", "journeyMapId", "eventName"),pIdxOpts);

					instance = col;
				}
			}
		}
		return instance;
	}

	@Override
	public ArangoCollection getDbCollection() {
		return getCollectionInstance();
	}

	public static String buildMapKey(Date reportedDate, String eventName, String journeyMapId) {
		return DATE_FORMATTER.format(reportedDate.toInstant()) + "#" + eventName + "#" + journeyMapId;
	}

	@Key
	@Expose
	private String id;

	@Expose
	private String dateKey;

	@Expose
	private int year;

	@Expose
	private int month;

	@Expose
	private int day;

	@Expose
	private int hour;

	@Expose
	private String journeyMapId = "";

	@Expose
	private String objectId = "";

	@Expose
	private String objectName = "";

	@Expose
	private String eventName = "";

	@Expose
	private volatile long dailyCount = 0;

	// hourly view of statistics from Profile Analytics Jobs
	// E.g: { "2020-05-05 02:00:00" : {"submit-contact" : 1 } }
	@Expose
	private Map<String, Map<String, Long>> hourlyEventStatistics = new ConcurrentHashMap<>();

	@Expose
	private Date createdAt;

	@Expose
	private Date updatedAt;

	@Expose
	private int partitionId;

	public DailyReportUnit() {
		// for Gson
	}

	public DailyReportUnit(String journeyMapId, String objectName, String objectId, String eventName,
			Date reportedDate) {
		this(journeyMapId, objectName, objectId, eventName, 0L, reportedDate);
	}

	public DailyReportUnit(String journeyMapId, String objectName, String objectId, String eventName, long dailyCount,
			Date reportedDate) {
		this.journeyMapId = journeyMapId;
		this.objectName = objectName;
		this.objectId = objectId;
		this.eventName = eventName;
		this.dailyCount = dailyCount;
		this.createdAt = reportedDate;
		this.updatedAt = reportedDate;

		// FIX: Use modern JDK 11 Time API instead of legacy java.util.Calendar
		ZonedDateTime zdt = reportedDate.toInstant().atZone(ZoneId.systemDefault());
		this.year = zdt.getYear();
		this.month = zdt.getMonthValue();
		this.day = zdt.getDayOfMonth();
		this.hour = zdt.getHour();
		this.dateKey = DATE_FORMATTER.format(zdt);

		this.buildHashedId();
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.journeyMapId) && StringUtil.isNotEmpty(this.objectName)
				&& StringUtil.isNotEmpty(this.eventName) && this.createdAt != null;
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (dataValidation()) {
			String keyHint = this.objectName + this.objectId + this.eventName + this.dateKey + this.journeyMapId;
			this.id = createId(this.id, keyHint);
			return this.id;
		} else {
			throw new IllegalArgumentException(
					"objectName, objectId, eventName and createdAt are required to create DailyReportUnit");
		}
	}

	/**
	 * Merges another DailyReportUnit's statistics into this one. Thread-safe.
	 */
	public synchronized void updateReportData(DailyReportUnit dailyReportUnit) {
		this.dailyCount += dailyReportUnit.getDailyCount();
		Map<String, Map<String, Long>> newHourlyEventStatistics = dailyReportUnit.getHourlyEventStatistics();
		newHourlyEventStatistics.forEach((newHourKey, newEventMap) -> {
			Map<String, Long> myEventMap = this.hourlyEventStatistics.computeIfAbsent(newHourKey,k -> new ConcurrentHashMap<>(24));
			newEventMap.forEach((newEventKey, newCount) -> {
				myEventMap.merge(newEventKey, newCount, Long::sum);
			});
		});
	}

	public synchronized void updateCountValue() {
		this.dailyCount++;
		updateHourlyEventCount();
	}

	private void updateHourlyEventCount() {
		String hourtimeStr = HOUR_FORMATTER.format(this.createdAt.toInstant());
		Map<String, Long> stats = this.hourlyEventStatistics.computeIfAbsent(hourtimeStr,
				k -> new ConcurrentHashMap<>(24));
		stats.merge(this.eventName, 1L, Long::sum);
	}

	public void resetHourlyEventCount() {
		this.hourlyEventStatistics.clear();
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

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

	public String getId() {
		return id;
	}

	public int getPartitionId() {
		return partitionId;
	}

	public String getDateKey() {
		return dateKey;
	}

	public void setDateKey(String dateKey) {
		this.dateKey = dateKey;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public int getHour() {
		return hour;
	}

	public void setHour(int hour) {
		this.hour = hour;
	}

	public String getObjectId() {
		return StringUtil.safeString(objectId);
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public String getJourneyMapId() {
		if (StringUtil.isEmpty(this.journeyMapId)) {
			this.journeyMapId = JourneyMap.DEFAULT_JOURNEY_MAP_ID;
		}
		return this.journeyMapId;
	}

	public void setJourneyMapId(String journeyMapId) {
		this.journeyMapId = journeyMapId;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public long getDailyCount() {
		return dailyCount;
	}

	public void setDailyCount(long dailyCount) {
		this.dailyCount = dailyCount;
	}

	public Map<String, Map<String, Long>> getHourlyEventStatistics() {
		if (hourlyEventStatistics == null) {
			hourlyEventStatistics = new ConcurrentHashMap<>(0);
		}
		return hourlyEventStatistics;
	}

	public void setHourlyEventStatistics(Map<String, Map<String, Long>> hourlyEventStatistics) {
		this.hourlyEventStatistics = hourlyEventStatistics;
	}

	@Override
	public int compareTo(DailyReportUnit o) {
		return this.createdAt.compareTo(o.getCreatedAt());
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(dateKey, objectId, eventName);
	}

	@Override
	public String toString() {
		return this.dateKey + " " + this.eventName + " " + this.dailyCount + "\n\n" + this.hourlyEventStatistics + "\n";
	}
}