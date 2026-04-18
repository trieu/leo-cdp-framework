package leotech.cdp.query.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.core.json.JsonObject;
import leotech.cdp.model.customer.Profile;
import leotech.system.model.SystemUser;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * the data model for datatables <br>
 * More at https://datatables.net
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class ProfileFilter extends DataFilter {

	String excludeProfileId;
	String visitorId;
	String profileId;
	List<String> emails, phones, applicationIDs, fintechSystemIDs, governmentIssuedIDs, loyaltyIDs;
	String fingerprintId;
	String crmRefId;

	String lastTouchpointName;
	String segmentName;
	String segmentId;
	String dataLabel;
	String mediaChannel;

	boolean showVisitor = false;
	boolean showLeadAndProspect = false;
	boolean showCustomer = false;
	boolean dataDeduplicationJob = false;

	String searchKeywords;
	String lastSeenIp;
	String lastUsedDeviceId;

	public ProfileFilter(boolean showAllActiveProfile, String segmentId, int start, int limit) {
		super();
		if (showAllActiveProfile) {
			this.showVisitor = true;
			this.showLeadAndProspect = true;
			this.showCustomer = true;
			this.status = Profile.STATUS_ACTIVE;
		}
		this.setSegmentId(segmentId);
		this.setStart(start);
		this.setLength(limit);
	}

	public ProfileFilter(boolean showAllActiveProfile, String segmentId, String email, String phone, int start, int limit) {
		super();
		if (showAllActiveProfile) {
			this.showVisitor = true;
			this.showLeadAndProspect = true;
			this.showCustomer = true;
			this.status = Profile.STATUS_ACTIVE;
		}
		this.setSegmentId(segmentId);
		this.setStart(start);
		this.setLength(limit);

		this.emails = ProfileFilterConstants.splitToList(email);
		this.phones = ProfileFilterConstants.splitToList(phone);

	}

	public ProfileFilter() {
		super();
	}

	public ProfileFilter(String excludeProfileId) {
		super();
		if (StringUtil.isNotEmpty(excludeProfileId)) {
			this.excludeProfileId = excludeProfileId;
			this.dataDeduplicationJob = true;
		} else {
			throw new IllegalArgumentException("excludeProfileId is empty or null");
		}
	}

	public ProfileFilter(SystemUser systemUser, String uri, JsonObject paramJson) {
		super(systemUser, uri, paramJson);

		this.journeyMapId = paramJson.getString(ProfileFilterConstants.JOURNEY_MAP_ID,
				ProfileFilterConstants.DEFAULT_STRING);
		this.showVisitor = paramJson.getBoolean(ProfileFilterConstants.SHOW_VISITOR,
				ProfileFilterConstants.DEFAULT_BOOLEAN);
		this.showLeadAndProspect = paramJson.getBoolean(ProfileFilterConstants.SHOW_LEAD_AND_PROSPECT,
				ProfileFilterConstants.DEFAULT_BOOLEAN);
		this.showCustomer = paramJson.getBoolean(ProfileFilterConstants.SHOW_CUSTOMER,
				ProfileFilterConstants.DEFAULT_BOOLEAN);

		this.searchKeywords = paramJson.getString(ProfileFilterConstants.SEARCH_KEYWORDS,
				ProfileFilterConstants.DEFAULT_STRING);
		this.visitorId = paramJson.getString(ProfileFilterConstants.VISITOR_ID, ProfileFilterConstants.DEFAULT_STRING);
		this.profileId = paramJson.getString(ProfileFilterConstants.PROFILE_ID, ProfileFilterConstants.DEFAULT_STRING);

		this.emails = ProfileFilterConstants.splitToList(paramJson.getString(ProfileFilterConstants.EMAILS, ProfileFilterConstants.DEFAULT_STRING));
		this.phones = ProfileFilterConstants.splitToList(paramJson.getString(ProfileFilterConstants.PHONES, ProfileFilterConstants.DEFAULT_STRING));

		this.applicationIDs = ProfileFilterConstants.splitToList(
				paramJson.getString(ProfileFilterConstants.APPLICATION_IDS, ProfileFilterConstants.DEFAULT_STRING));
		this.fintechSystemIDs = ProfileFilterConstants.splitToList(
				paramJson.getString(ProfileFilterConstants.FINTECH_SYSTEM_IDS, ProfileFilterConstants.DEFAULT_STRING));
		this.governmentIssuedIDs = ProfileFilterConstants.splitToList(
				paramJson.getString(ProfileFilterConstants.GOVERNMENT_ISSUED_IDS, ProfileFilterConstants.DEFAULT_STRING));
		this.loyaltyIDs = ProfileFilterConstants.splitToList(
				paramJson.getString(ProfileFilterConstants.LOYALTY_IDS, ProfileFilterConstants.DEFAULT_STRING));

		this.fingerprintId = paramJson.getString(ProfileFilterConstants.FINGERPRINT_ID,
				ProfileFilterConstants.DEFAULT_STRING);
		this.crmRefId = paramJson.getString(ProfileFilterConstants.CRM_REF_ID, ProfileFilterConstants.DEFAULT_STRING);

		this.lastTouchpointName = paramJson.getString(ProfileFilterConstants.LAST_TOUCHPOINT_NAME,
				ProfileFilterConstants.DEFAULT_STRING);
		this.segmentName = paramJson.getString(ProfileFilterConstants.SEGMENT_NAME,
				ProfileFilterConstants.DEFAULT_STRING);
		this.segmentId = paramJson.getString(ProfileFilterConstants.SEGMENT_ID, ProfileFilterConstants.DEFAULT_STRING);
		this.dataLabel = paramJson.getString(ProfileFilterConstants.DATA_LABEL, ProfileFilterConstants.DEFAULT_STRING);
		this.mediaChannel = paramJson.getString(ProfileFilterConstants.MEDIA_CHANNEL,
				ProfileFilterConstants.DEFAULT_STRING);

		this.excludeProfileId = paramJson.getString(ProfileFilterConstants.EXCLUDE_PROFILE_ID,
				ProfileFilterConstants.DEFAULT_STRING);
		this.lastSeenIp = paramJson.getString(ProfileFilterConstants.LAST_SEEN_IP,
				ProfileFilterConstants.DEFAULT_STRING);
		this.lastUsedDeviceId = paramJson.getString(ProfileFilterConstants.LAST_USED_DEVICE_ID,
				ProfileFilterConstants.DEFAULT_STRING);
	}

	public String getExcludeProfileId() {
		return excludeProfileId != null ? excludeProfileId : StringPool.BLANK;
	}

	public void setExcludeProfileId(String excludeProfileId) {
		this.excludeProfileId = excludeProfileId;
	}

	public String getSearchKeywords() {
		return searchKeywords != null ? searchKeywords : StringPool.BLANK;
	}

	public void setSearchKeywords(String searchKeywords) {
		this.searchKeywords = searchKeywords;
	}

	public String getVisitorId() {
		return visitorId != null ? visitorId : StringPool.BLANK;
	}

	public void setVisitorId(String visitorId) {
		this.visitorId = visitorId;
	}

	public String getFingerprintId() {
		return fingerprintId != null ? fingerprintId : StringPool.BLANK;
	}

	public void setFingerprintId(String fingerprintId) {
		this.fingerprintId = fingerprintId;
	}

	public String getCrmRefId() {
		return crmRefId != null ? crmRefId : StringPool.BLANK;
	}

	public void setCrmRefId(String crmRefId) {
		this.crmRefId = crmRefId;
	}

	public String getProfileId() {
		return profileId != null ? profileId : StringPool.BLANK;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	@Override
	public String getSegmentId() {
		return segmentId;
	}

	@Override
	public void setSegmentId(String segmentId) {
		this.segmentId = segmentId;
	}

	@Override
	public String getJourneyMapId() {
		return journeyMapId != null ? journeyMapId : StringPool.BLANK;
	}

	@Override
	public void setJourneyMapId(String journeyMapId) {
		this.journeyMapId = journeyMapId;
	}

	public String getLastTouchpointName() {
		return lastTouchpointName != null ? lastTouchpointName : StringPool.BLANK;
	}

	public void setLastTouchpointName(String lastTouchpointName) {
		this.lastTouchpointName = lastTouchpointName;
	}

	public String getSegmentName() {
		return segmentName != null ? segmentName : StringPool.BLANK;
	}

	public void setSegmentName(String segmentName) {
		this.segmentName = segmentName;
	}

	public boolean isShowVisitor() {
		return showVisitor;
	}

	public void setShowVisitor(boolean showVisitor) {
		this.showVisitor = showVisitor;
	}

	public boolean isShowLeadAndProspect() {
		return showLeadAndProspect;
	}

	public void setShowLeadAndProspect(boolean showLeadAndProspect) {
		this.showLeadAndProspect = showLeadAndProspect;
	}

	public boolean isShowCustomer() {
		return showCustomer;
	}

	public void setShowCustomer(boolean showCustomer) {
		this.showCustomer = showCustomer;
	}

	public boolean isForDeduplication() {
		return StringUtil.isNotEmpty(this.excludeProfileId);
	}

	public boolean isDataDeduplicationJob() {
		return dataDeduplicationJob;
	}

	public List<String> getApplicationIDs() {
		return applicationIDs == null ? new ArrayList<String>(0) : applicationIDs;
	}

	public void setApplicationIDs(List<String> applicationIDs) {
		this.applicationIDs = applicationIDs;
	}

	public List<String> getFintechSystemIDs() {
		return fintechSystemIDs == null ? new ArrayList<String>(0) : fintechSystemIDs;
	}

	public void setFintechSystemIDs(List<String> fintechSystemIDs) {
		this.fintechSystemIDs = fintechSystemIDs;
	}

	public List<String> getGovernmentIssuedIDs() {
		return governmentIssuedIDs == null ? new ArrayList<String>(0) : governmentIssuedIDs;
	}

	public void setGovernmentIssuedIDs(List<String> governmentIssuedIDs) {
		this.governmentIssuedIDs = governmentIssuedIDs;
	}

	public List<String> getLoyaltyIDs() {
		return loyaltyIDs == null ? new ArrayList<String>(0) : loyaltyIDs;
	}

	public void setLoyaltyIDs(List<String> loyaltyIDs) {
		this.loyaltyIDs = loyaltyIDs;
	}

	public List<String> getEmails() {
		return emails == null ? new ArrayList<String>(0) : emails;
	}

	public void setEmails(List<String> emails) {
		this.emails = emails;
	}

	public List<String> getPhones() {
		return phones == null ? new ArrayList<String>(0) : phones;
	}

	public void setPhones(List<String> phones) {
		this.phones = phones;
	}

	@Override
	public String getLoginUsername() {
		return loginUsername != null ? loginUsername : StringPool.BLANK;
	}

	@Override
	public void setLoginUsername(String loginUsername) {
		this.loginUsername = loginUsername;
	}

	public String getDataLabel() {
		return dataLabel != null ? dataLabel : StringPool.BLANK;
	}

	public void setDataLabel(String dataLabel) {
		this.dataLabel = dataLabel;
	}

	public String getMediaChannel() {
		return mediaChannel != null ? mediaChannel : StringPool.BLANK;
	}

	public void setMediaChannel(String mediaChannel) {
		this.mediaChannel = mediaChannel;
	}

	public boolean showLeadOrProspectOrCustomer() {
		return (this.showLeadAndProspect || this.showCustomer) && !this.showVisitor;
	}

	public boolean showVisitorOrCustomer() {
		return (this.showVisitor || this.showCustomer) && !this.showLeadAndProspect;
	}

	public boolean showVisitorOrLead() {
		return (this.showVisitor || this.showLeadAndProspect) && !this.showCustomer;
	}

	public boolean showLeadAndProspectAndCustomer() {
		return (this.showLeadAndProspect && this.showCustomer) && !this.showVisitor;
	}

	public boolean showOnlyVisitor() {
		return (!this.showLeadAndProspect && !this.showCustomer) && this.showVisitor;
	}

	public boolean showOnlyLeadOrProspect() {
		return (!this.showVisitor && !this.showCustomer) && this.showLeadAndProspect;
	}

	public boolean showOnlyCustomer() {
		return (!this.showLeadAndProspect && !this.showVisitor) && this.showCustomer;
	}

	public boolean showAllActiveProfile() {
		return (this.showLeadAndProspect || this.showCustomer || this.showVisitor)
				&& this.status == PersistentObject.STATUS_ACTIVE;
	}

	public String getLastSeenIp() {
		return StringUtil.safeString(lastSeenIp);
	}

	public void setLastSeenIp(String lastSeenIp) {
		this.lastSeenIp = lastSeenIp;
	}

	public String getLastUsedDeviceId() {
		return StringUtil.safeString(lastUsedDeviceId);
	}

	public void setLastUsedDeviceId(String lastUsedDeviceId) {
		this.lastUsedDeviceId = lastUsedDeviceId;
	}

}
