package leotech.system.model;

import com.google.gson.Gson;

import rfx.core.util.CharPool;
import rfx.core.util.SecurityUtil;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * @author Trieu
 * 
 *         Data Model for Log Database
 *
 */
public class LogData {

	public static final String BEACON_DELIMITER = "\\|";
	public static final int HALF_DAY = 43200;

	protected int deviceId;
	protected int deviceType;
	protected int userSegmentId;
	protected long locationId;
	protected long hashUrl;
	protected String url;

	// Ad Unit fields
	protected long deliveredTime;

	protected String uuid = "0";

	// Ad Item fields
	protected long loggedTime;
	protected String contentId;
	protected String categoryId;
	protected String groupId;

	// Ad metric
	String metric;

	// Logging Event
	private int view;
	private int play;
	private int save;
	private int comment;

	public LogData() {
	}

	// for ad request creator
	public LogData(String uuid, long deliveredTime) {
		this.metric = "request";
		// TODO
	}

	public LogData(String metric, String uuid, int placementId, int lineItemId, int orderId, int priceModel, int adType,
			int adFormatId, int publisherId, int advertiserId, long deliveredTime) {
		this.metric = metric;
		// TODO
	}

	// for ad request creator
	public String getBeaconData() {
		String rawBeacon = StringUtil.toString(uuid, "|", contentId, "|", categoryId, "|", groupId, "|", deliveredTime);
		return SecurityUtil.encryptBeaconValue(rawBeacon);
	}

	// for ad log creator
	public LogData(String metric, String encodedStr, long loggedTime) {
		if (metric == null) {
			throw new IllegalArgumentException("metric can not be NULL");
		}
		this.metric = metric;
		String raw = SecurityUtil.decryptBeaconValue(encodedStr);
		String[] toks = raw.split(BEACON_DELIMITER);

		int length = toks.length;
		if (length == 10) {
			String uuid = toks[0];
			if (StringUtil.isEmpty(uuid)) {
				uuid = "0";
			}
			this.uuid = uuid;

			this.deliveredTime = StringUtil.safeParseInt(toks[9]);
			setLoggedTime(loggedTime);
		} else {
			throw new IllegalArgumentException("decryptBeaconValue is invalid" + raw);
		}

	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	public String toLogRecord() {
		StringBuilder s = new StringBuilder();

		// event time
		char tab = CharPool.TAB;
		s.append(this.loggedTime).append(tab);

		// advertising data
		s.append(this.metric).append(tab);
		// TODO

		s.append(StringPool.NEW_LINE);
		return s.toString();
	}

	public long getDeliveredTime() {
		return deliveredTime;
	}

	public void setDeliveredTime(int deliveredTime) {
		this.deliveredTime = deliveredTime;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public long getLoggedTime() {
		return loggedTime;
	}

	public void setLoggedTime(long loggedTime) {
		long diff = loggedTime - this.deliveredTime;
		if (diff >= 0 && diff < HALF_DAY) {
			this.loggedTime = loggedTime;
		} else {
			throw new IllegalArgumentException(
					"loggedTime must be larger than deliveredTime and engagedTime less than one day");
		}
	}

	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public int getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(int deviceId) {
		this.deviceId = deviceId;
	}

	public int getUserSegmentId() {
		return userSegmentId;
	}

	public void setUserSegmentId(int userSegmentId) {
		this.userSegmentId = userSegmentId;
	}

	public long getLocationId() {
		return locationId;
	}

	public void setLocationId(long locationId) {
		this.locationId = locationId;
	}

	public long getHashUrl() {
		return hashUrl;
	}

	public void setHashUrl(long hashUrl) {
		this.hashUrl = hashUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public int getView() {
		return view;
	}

	public void setView(int view) {
		this.view = view;
	}

	public int getPlay() {
		return play;
	}

	public void setPlay(int play) {
		this.play = play;
	}

	public int getSave() {
		return save;
	}

	public void setSave(int save) {
		this.save = save;
	}

	public int getComment() {
		return comment;
	}

	public void setComment(int comment) {
		this.comment = comment;
	}

	public void setDeliveredTime(long deliveredTime) {
		this.deliveredTime = deliveredTime;
	}

	public int getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(int deviceType) {
		this.deviceType = deviceType;
	}

}
