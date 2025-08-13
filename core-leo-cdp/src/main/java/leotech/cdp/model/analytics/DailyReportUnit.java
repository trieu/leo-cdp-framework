package leotech.cdp.model.analytics;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
 * Daily Report Unit for time series analytics
 * 
 * @author tantrieu31
 * @since 2020
 *
 */
public final class DailyReportUnit extends PersistentObject implements Comparable<DailyReportUnit> {
	
	static final String YYYY_MM_DD_HH_00_00 = "yyyy-MM-dd HH:00:00";
	
	public final static String buildMapKey(Date reportedDate, String eventName, String journeyMapId) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_PATTERN);
		return simpleDateFormat.format(reportedDate) + "#" + eventName + "#" + journeyMapId;
	}

	public static final String COLLECTION_NAME = getCdpCollectionName(DailyReportUnit.class);
	static ArangoCollection instance;
	
	public static ArangoCollection getCollectionInstance() {
		if (instance == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			instance = arangoDatabase.collection(COLLECTION_NAME);

			instance.ensurePersistentIndex(Arrays.asList("objectName","objectId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("objectName","objectId","journeyMapId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("dateKey","eventName"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("year","month","day","hour","eventName","journeyMapId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("year","month","day","hour","objectName","objectId","eventName"), new PersistentIndexOptions().unique(false));
			
			instance.ensurePersistentIndex(Arrays.asList("createdAt","objectName","objectId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("createdAt","objectName"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("createdAt","objectName","journeyMapId"), new PersistentIndexOptions().unique(false));
			instance.ensurePersistentIndex(Arrays.asList("createdAt","objectName","objectId", "journeyMapId"), new PersistentIndexOptions().unique(false));
		}
		return instance;
	}

	@Override
	public ArangoCollection getDbCollection() {
		return getCollectionInstance();
	}

	@Key
	@Expose
	String id;

	@Expose
	String dateKey;
	
	@Expose
	int year;
	
	@Expose
	int month;
	
	@Expose
	int day;
	
	@Expose
	int hour;
	
	@Expose
	String journeyMapId = "";
	
	@Expose
	String objectId = "";
	
	@Expose
	String objectName = "";
	
	@Expose
	String eventName = "";

	@Expose
	volatile long dailyCount = 0;

	// hourly view of statistics from Profile Analytics Jobs
	// E.g: { "2020-05-05T02:00:00.000Z" : {"submit-contact" : 1 } }
	@Expose
	Map<String, Map<String, Long>> hourlyEventStatistics = new HashMap<>();

	@Expose
	Date createdAt;

	@Expose
	Date updatedAt;

	@Expose
	int partitionId;

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.journeyMapId) && StringUtil.isNotEmpty(this.objectName) && StringUtil.isNotEmpty(this.eventName) && this.createdAt != null;
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (dataValidation()) {
			String keyHint =  this.objectName + this.objectId + this.eventName + this.dateKey + this.journeyMapId;
			this.id = createId(this.id, keyHint);
			return this.id;
		} else {
			newIllegalArgumentException("objectName, objectId, eventName and createdAt are required ");
		}
		return null;
	}

	public DailyReportUnit() {
		// for Gson
	}

	/**
	 * init with existing dailyCount for DailyReportUnit
	 * 
	 * @param journeyMapId
	 * @param objectName
	 * @param objectId
	 * @param eventName
	 * @param dailyCount
	 * @param reportedDate
	 */
	public DailyReportUnit(String journeyMapId, String objectName, String objectId, String eventName, long dailyCount, Date reportedDate) {
		super();
		this.init(journeyMapId, objectName, objectId, eventName, dailyCount, reportedDate);
	}
	
	
	/**
	 * init for new DailyReportUnit
	 * 
	 * @param journeyMapId
	 * @param objectName
	 * @param objectId
	 * @param eventName
	 * @param reportedDate
	 */
	public DailyReportUnit(String journeyMapId, String objectName, String objectId, String eventName, Date reportedDate) {
		super();
		this.init(journeyMapId, objectName, objectId, eventName, 0L, reportedDate);
	}
	
	private final void init(String journeyMapId, String objectName, String objectId, String eventName, long dailyCount, Date reportedDate) {
		this.journeyMapId = journeyMapId;
		this.objectName = objectName;
		this.objectId = objectId;
		this.eventName = eventName;
		this.dailyCount = dailyCount;
		this.createdAt = reportedDate;
		this.updatedAt = reportedDate;
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(reportedDate);
		this.year = calendar.get(Calendar.YEAR);
		this.month = calendar.get(Calendar.MONTH) + 1;
		this.day = calendar.get(Calendar.DAY_OF_MONTH);
		this.hour = calendar.get(Calendar.HOUR_OF_DAY);
		this.dateKey = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_PATTERN).format(reportedDate);
		this.buildHashedId();
	}
	
	public final void updateReportData(DailyReportUnit dailyReportUnit) {
		this.dailyCount += dailyReportUnit.getDailyCount();
		Map<String, Map<String, Long>> newHourlyEventStatistics = dailyReportUnit.getHourlyEventStatistics();
		newHourlyEventStatistics.forEach((String newHourKey, Map<String, Long> newEventMap)->{
			Map<String, Long> cMap = this.hourlyEventStatistics.getOrDefault(newHourKey, new HashMap<String, Long>(24));
			newEventMap.forEach( (newEventKey, newCount) -> {
				long count = cMap.getOrDefault(newEventKey, 0L) + newCount;
				cMap.put(newEventKey, count);
			});
		});
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
		if(StringUtil.isEmpty(this.journeyMapId)) {
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
	
	
	public final synchronized void updateCountValue() {
		this.dailyCount++;
		updateHourlyEventCount();
		
//		System.out.println("DailyReportUnit.dailyCount = " + dailyCount);
//		System.out.println("DailyReportUnit.hourlyEventStatistics \n " + hourlyEventStatistics);
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

	private final void updateHourlyEventCount() {
		String hourtimeStr = new SimpleDateFormat(YYYY_MM_DD_HH_00_00).format(this.createdAt);
		Map<String, Long> stats = hourlyEventStatistics.getOrDefault(hourtimeStr, new ConcurrentHashMap<String, Long>(24));
		long c = stats.getOrDefault(this.eventName, 0L) + 1L;
		stats.put(this.eventName, c);
		hourlyEventStatistics.put(hourtimeStr, stats);
	}

	public void resetHourlyEventCount() {
		this.hourlyEventStatistics.clear();
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
		return Objects.hash(dateKey);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(this.dateKey).append(" ").append(this.eventName).append(" ").append(this.dailyCount);
		s.append("\n\n").append(this.hourlyEventStatistics).append("\n");
		return s.toString();
	}
}
