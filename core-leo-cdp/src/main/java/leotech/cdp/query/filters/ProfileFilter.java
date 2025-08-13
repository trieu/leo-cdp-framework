package leotech.cdp.query.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.core.json.JsonObject;
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
	
	
	public ProfileFilter() {
		super();
	}
	
	public ProfileFilter(String excludeProfileId) {
		super();
		this.excludeProfileId = excludeProfileId;
		this.dataDeduplicationJob = StringUtil.isNotEmpty(excludeProfileId);
	}

	public ProfileFilter(SystemUser systemUser, String uri, JsonObject paramJson) {
		super(systemUser, uri, paramJson);
		
		this.journeyMapId = paramJson.getString("journeyMapId", StringPool.BLANK);
		this.showVisitor = paramJson.getBoolean("showVisitor", true);
		this.showLeadAndProspect = paramJson.getBoolean("showLeadAndProspect", true);
		this.showCustomer = paramJson.getBoolean("showCustomer", true);
		
		this.searchKeywords = paramJson.getString("searchKeywords", StringPool.BLANK);
		this.visitorId = paramJson.getString("visitorId", StringPool.BLANK);
		this.profileId = paramJson.getString("profileId", StringPool.BLANK);
		
		this.emails = Arrays.asList(paramJson.getString("emails", StringPool.BLANK).split(StringPool.SEMICOLON));
		this.phones =  Arrays.asList(paramJson.getString("phones", StringPool.BLANK).split(StringPool.SEMICOLON));
		
		this.applicationIDs = Arrays.asList(paramJson.getString("applicationIDs", StringPool.BLANK).split(StringPool.SEMICOLON));
		this.fintechSystemIDs = Arrays.asList(paramJson.getString("fintechSystemIDs", StringPool.BLANK).split(StringPool.SEMICOLON));
		this.governmentIssuedIDs = Arrays.asList(paramJson.getString("governmentIssuedIDs", StringPool.BLANK).split(StringPool.SEMICOLON));
		this.loyaltyIDs = Arrays.asList(paramJson.getString("loyaltyIDs", StringPool.BLANK).split(StringPool.SEMICOLON));
		
		this.fingerprintId = paramJson.getString("fingerprintId", StringPool.BLANK);
		this.crmRefId = paramJson.getString("crmRefId", StringPool.BLANK);
		
		this.lastTouchpointName = paramJson.getString("lastTouchpointName", StringPool.BLANK);
		this.segmentName = paramJson.getString("segmentName", StringPool.BLANK);
		this.segmentId = paramJson.getString("segmentId", StringPool.BLANK);
		this.dataLabel = paramJson.getString("dataLabel", StringPool.BLANK);
		this.mediaChannel = paramJson.getString("mediaChannel", StringPool.BLANK);
		
		
		this.excludeProfileId = paramJson.getString("excludeProfileId", StringPool.BLANK);
		this.lastSeenIp = paramJson.getString("lastSeenIp", StringPool.BLANK);
		this.lastUsedDeviceId = paramJson.getString("lastUsedDeviceId", StringPool.BLANK);
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
	
	public String getSegmentId() {
		return segmentId;
	}

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
		return applicationIDs == null ? new ArrayList<String>(0): applicationIDs;
	}

	public void setApplicationIDs(List<String> applicationIDs) {
		this.applicationIDs = applicationIDs;
	}

	public List<String> getFintechSystemIDs() {
		return fintechSystemIDs == null ? new ArrayList<String>(0): fintechSystemIDs;
	}

	public void setFintechSystemIDs(List<String> fintechSystemIDs) {
		this.fintechSystemIDs = fintechSystemIDs;
	}

	public List<String> getGovernmentIssuedIDs() {
		return governmentIssuedIDs == null ? new ArrayList<String>(0): governmentIssuedIDs;
	}

	public void setGovernmentIssuedIDs(List<String> governmentIssuedIDs) {
		this.governmentIssuedIDs = governmentIssuedIDs;
	}

	public List<String> getLoyaltyIDs() {
		return loyaltyIDs == null ? new ArrayList<String>(0): loyaltyIDs;
	}

	public void setLoyaltyIDs(List<String> loyaltyIDs) {
		this.loyaltyIDs = loyaltyIDs;
	}

	public List<String> getEmails() {
		return emails == null ? new ArrayList<String>(0): emails;
	}

	public void setEmails(List<String> emails) {
		this.emails = emails;
	}

	public List<String> getPhones() {
		return phones == null ? new ArrayList<String>(0): phones;
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
		return (this.showLeadAndProspect || this.showCustomer || this.showVisitor) && this.status == PersistentObject.STATUS_ACTIVE;
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
