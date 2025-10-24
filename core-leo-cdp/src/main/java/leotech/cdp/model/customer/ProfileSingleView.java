package leotech.cdp.model.customer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.devskiller.friendly_id.FriendlyId;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import leotech.cdp.dao.DailyReportUnitDaoUtil;
import leotech.cdp.dao.DeviceDaoUtil;
import leotech.cdp.dao.TargetMediaUnitDaoUtil;
import leotech.cdp.domain.DeviceManagement;
import leotech.cdp.domain.JourneyMapManagement;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.domain.TouchpointManagement;
import leotech.cdp.domain.schema.CustomerFunnel;
import leotech.cdp.domain.schema.FunnelMetaData;
import leotech.cdp.model.SingleViewAnalyticalObject;
import leotech.cdp.model.analytics.DailyReportUnit;
import leotech.cdp.model.analytics.TouchpointReport;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.JourneyMapRefKey;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.marketing.TargetMediaUnit;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.StringUtil;

/**
 * the single view of profile, get all data from multiple sources for Customer 360 Analytics Single-View
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class ProfileSingleView extends Profile implements SingleViewAnalyticalObject {

	// only show recent data in 3 months
	private static final int DAILY_REPORT_MONTHS = -3;

	@Expose
	private Map<String, Date> activationTimeline = new HashMap<String, Date>();
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	private Device lastUsedDevice;
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected Set<Device> usedDevices;
	
	// skip when storing into database
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	private Map<String,DailyReportUnit> dailyReportUnits = new ConcurrentHashMap<>();
	
	// skip when storing into database
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	private List<Touchpoint> topEngagedTouchpoints = new ArrayList<Touchpoint>(MAX_ENGAGED_TOUCHPOINT_SIZE);
	
	// skip when storing into database
	@Expose
	@com.arangodb.velocypack.annotations.Expose(serialize = false, deserialize = false)
	private SortedSet<TargetMediaUnit> targetMediaUnits;
	

	public ProfileSingleView() {
		// for gson
	}
	
	
	
	@Override
	public void unifyData() {
		loadPrimaryAvatar();
		loadLastTrackingEvents();
		loadUsedDevices();
		loadProfileTypeAsText();
		loadGenderAndMarialStatus();
		

		loadEngagedTouchpoints();
		loadDailyReportUnits();
		loadTargetMediaUnits();
		updateSegmentRefKeys();
		updateJourneyMapRefKeys();
	}
	
	/**
	 * new CRM_USER profile with email, phone and crm Ref ID
	 * 
	 * @param observerId
	 * @param email
	 * @param phone
	 * @param crmRefId
	 * @return
	 */
	public final static ProfileSingleView newCrmProfile(String email, String phone, String crmRefId, String funnelStage) {
		ProfileSingleView p = new ProfileSingleView();
		Touchpoint touchpoint = TouchpointManagement.LEO_CDP_ADMIN_FORM;
		p.initNewProfile(0, "", ProfileType.CRM_IMPORTED_CONTACT, CRM_IMPORT, touchpoint , "", "", email, phone, "", crmRefId,"","","","");
		p.setFunnelStage(funnelStage);
		return p;
	}
	
	/**
	 * @param email
	 * @return
	 */
	public final static ProfileSingleView newProfileFromTouchpoint(ProfileIdentity profileIds, Touchpoint fromTouchpoint, EventObserver observer, String funnelStage) {
		ProfileSingleView p = new ProfileSingleView();
		p.setFunnelStage(funnelStage);
		int type = ProfileType.DIRECT_INPUT_CONTACT;
		String crmId = profileIds.getCrmRefId();
		String citizenId = profileIds.getFirstGovernmentIssuedID();
		String appId = profileIds.getFirstApplicationID();
		String fintechId = profileIds.getFirstFintechSystemID();
		String loyaltyId = profileIds.getFirstLoyaltyID();
		String primaryEmail = profileIds.getPrimaryEmail();
		String primaryPhone = profileIds.getPrimaryPhone();
		p.initNewProfile(0, "", type, observer.getId(), fromTouchpoint , "", "", primaryEmail, primaryPhone, "",crmId, citizenId, appId, fintechId, loyaltyId);
		return p;
	}
	
	
	/**
	 * 
	 * for Finance Industry (email, phone, citizen ID, loan application ID)
	 * 
	 * @param email
	 * @param phone
	 * @return
	 */
	public final static ProfileSingleView newImportedCustomerProfile(String email, String phone, String citizenId, String applicationId, String bankingSystemId) {
		ProfileSingleView p = new ProfileSingleView(); 
		Touchpoint fromTouchpoint = TouchpointManagement.LEO_CDP_IMPORTER;
		p.initNewProfile(0, "", ProfileType.CUSTOMER_CONTACT, CDP_API, fromTouchpoint , "", "", email, phone, "","", citizenId, applicationId, bankingSystemId, "");
		p.setFunnelStage(FunnelMetaData.STAGE_NEW_CUSTOMER);
		int funnelIndex = CustomerFunnel.FUNNEL_STAGE_NEW_CUSTOMER.getOrderIndex();// New Customer
		int journeyMapStage = EventMetric.JOURNEY_STAGE_ACTION; 
		p.setInJourneyMapDefault(CONTACT, funnelIndex, journeyMapStage);
		return p;
	}
	
	/**
	 * for Importer, create CRM_USER profile with email, phone and crm Ref ID
	 * 
	 * @param observerId
	 * @param email
	 * @param phone
	 * @param crmRefId
	 * @return
	 */
	public final static ProfileSingleView newImportedProfile(String crmId, String firstName, String lastName, String email, String phone, String genderStr, int age, String dateOfBirth,
			String permanentLocation, String livingLocation ) {
		ProfileSingleView p = new ProfileSingleView();		
		Touchpoint touchpoint = TouchpointManagement.LEO_CDP_IMPORTER;
		p.initNewProfile(0, "", ProfileType.CRM_IMPORTED_CONTACT, CRM_IMPORT, touchpoint , "", "", email, phone, "", crmId,"","","", "");
		p.setFunnelStage(FunnelMetaData.STAGE_LEAD);
		p.setPersonalInformation(crmId, firstName, lastName, email, phone, genderStr, age, dateOfBirth, permanentLocation, livingLocation);
		return p;
	}
	
	public ProfileSingleView(int type) {
		this.type = type;
		this.lastObserverId = CDP_INPUT;
		this.typeAsText = ProfileType.getTypeAsText(ProfileType.DIRECT_INPUT_CONTACT);
		this.primaryEmail = "";
		this.primaryPhone = "";
		this.genderAsText = "-";
		
		Touchpoint srcTouchpoint = TouchpointManagement.LEO_CDP_ADMIN_FORM;
		this.lastTouchpointId = srcTouchpoint.getId();
		this.lastTouchpoint = srcTouchpoint;
	}


	/**
	 * new ANONYMOUS profile for web or app tracking
	 * 
	 * @param observerId
	 * @param lastTouchpointId
	 * @param lastSeenIp
	 * @param usedDeviceId
	 */
	public static ProfileSingleView newAnonymousProfile(String observerId, Touchpoint srcTouchpoint,
			String lastSeenIp, String visitorId, String usedDeviceId, String fingerprintId) {
		ProfileSingleView p = new ProfileSingleView();
		p.initNewProfile(0, visitorId, ProfileType.ANONYMOUS_VISITOR, observerId, srcTouchpoint, lastSeenIp, usedDeviceId,
				"", "", fingerprintId, "","","","","");
		return p;
	}
	
	
	/**
	 * @param observerId
	 * @param srcTouchpoint
	 * @param lastSeenIp
	 * @return
	 */
	public static ProfileSingleView newProfileForAPI(Date createdAt,String observerId, Touchpoint srcTouchpoint, String lastSeenIp) {
		ProfileSingleView p = new ProfileSingleView();
		p.setCreatedAt(createdAt);
		String keyHints = observerId + srcTouchpoint.getId() + System.currentTimeMillis();
		String visitorId = FriendlyId.toFriendlyId(UUID.nameUUIDFromBytes(keyHints.getBytes()));
		p.initNewProfile(0, visitorId, ProfileType.ANONYMOUS_VISITOR, observerId, srcTouchpoint, lastSeenIp, "",
				"", "", "", "","","","","");
		return p;
	}

	/**
	 * new IDENTIFIED profile with email
	 * 
	 * @param sessionKey
	 * @param visitorId
	 * @param observerId
	 * @param lastTouchpointId
	 * @param lastSeenIp
	 * @param usedDeviceId
	 * @param email
	 * @return
	 */
	public static ProfileSingleView newContactProfile(String observerId, Touchpoint srcTouchpoint,
			String lastSeenIp, String visitorId, String usedDeviceId, String fingerprintId, BasicContactData contact) {
		ProfileSingleView p = new ProfileSingleView();
		p.initNewProfile(0, visitorId, ProfileType.CUSTOMER_CONTACT, observerId, srcTouchpoint, lastSeenIp, usedDeviceId,
				contact.email, contact.phone, fingerprintId, "","","","","");
		p.setFirstName(contact.firstName);
		p.setLastName(contact.lastName);
		return p;
	}

	/**
	 * new CRM_USER profile with email, phone and crm Ref ID
	 * 
	 * @param observerId
	 * @param email
	 * @param phone
	 * @param crmRefId
	 * @return
	 */
	public static ProfileSingleView newSocialLoginProfile(String visitorId, String firstName,
			String lastName, String email, String refId, String source) {
		ProfileSingleView p = new ProfileSingleView();
		Touchpoint touchpoint = TouchpointManagement.DIRECT_TRAFFIC_WEB;
		p.initNewProfile(0, visitorId, ProfileType.LOGIN_USER_CONTACT, SSO_LOGIN, touchpoint, "", "", email, "", "", "","","","", "");
		p.setFirstName(firstName);
		p.setLastName(lastName);
		p.setSocialMediaProfile(source, refId);
		return p;
	}


	
	void updateSegmentRefKeys() {
		List<String> idList = this.inSegments.stream().map(ref->{
			return ref.getId();
		}).collect(Collectors.toList());
		this.inSegments = SegmentDataManagement.getRefKeysByIds(idList);
	}
	
	void updateJourneyMapRefKeys() {
		List<String> journeyIds = this.inJourneyMaps.stream().map(ref->{
			return ref.getId();
		}).collect(Collectors.toList());
		Set<JourneyMapRefKey> jouneyMapRefKeys = JourneyMapManagement.getRefKeysByIds(journeyIds);
		this.updateInJourneyMaps(jouneyMapRefKeys);
	} 

	private void loadTargetMediaUnits() {
		List<TargetMediaUnit> mediaUnitsList = TargetMediaUnitDaoUtil.getByProfileId(id);
		this.targetMediaUnits = new TreeSet<TargetMediaUnit>(mediaUnitsList);
	}

	private void loadPrimaryAvatar() {
		if( StringUtil.isEmpty(this.primaryAvatar) && ! this.personalAvatars.isEmpty() ) {
			// set default avatar of empty
			this.primaryAvatar = this.personalAvatars.iterator().next();
		}
	}

	private void loadUsedDevices() {
		if(this.lastUsedDeviceId != null) {
			this.lastUsedDevice = DeviceManagement.getById(this.lastUsedDeviceId);
		}
		Set<String> deviceIds = this.usedDeviceIds;
		this.usedDevices = new HashSet<>(deviceIds.size());
		for (String deviceId : deviceIds) {
			Device dv = DeviceDaoUtil.getById(deviceId);
			if(dv != null) {
				this.usedDevices.add(dv);
			}
		}
	}

	public boolean engageAtTouchpoint(Touchpoint tp) {
		return engageAtTouchpoint(tp, false);
	}

	public boolean engageAtTouchpoint(Touchpoint tp, boolean eventTracking) {
		boolean rs = false;
		if (tp != null) {
			String tpId = tp.getId();
			if (!this.topEngagedTouchpointIds.contains(tpId)) {
				if(eventTracking) {
					setLastTouchpoint(tp);
					setLastTouchpointId(tp.getId());
				}
				int size = topEngagedTouchpointIds.size();
				if (!topEngagedTouchpointIds.isEmpty() && size >= MAX_ENGAGED_TOUCHPOINT_SIZE) {
					topEngagedTouchpointIds.remove(Iterables.get(topEngagedTouchpointIds, 0));
				}
				this.topEngagedTouchpointIds.add(tpId);
			}
		}
		return rs;
	}

	private final void loadEngagedTouchpoints() {
		// last recent touchpoint
		if(this.lastTrackingEvent != null) {
			Touchpoint srcTouchpoint = this.lastTrackingEvent.getSrcTouchpoint();
			if(srcTouchpoint != null) {
				this.lastTouchpoint = srcTouchpoint;
				this.lastTouchpointId = this.lastTouchpoint.getId();	
			}
		}
		// top 1000 recent touchpoints
		Set<String> touchpointIds = this.topEngagedTouchpointIds;
		this.topEngagedTouchpoints.clear();
		for (String touchpointId : touchpointIds) {
			Touchpoint tp = TouchpointManagement.getById(touchpointId);
			if(tp != null) {
				this.topEngagedTouchpoints.add(tp);
			}
		}
		Collections.sort(this.topEngagedTouchpoints);
	}
	
	/**
	 * get daily report from the last tracked event, go back to 2 months
	 */
	private final void loadDailyReportUnits() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DAY_OF_YEAR, 1);
		String toDate = DateTimeUtil.formatDate(cal.getTime(), DateTimeUtil.DATE_FORMAT_PATTERN);

		cal.add(Calendar.MONTH, DAILY_REPORT_MONTHS);
		String fromDate = DateTimeUtil.formatDate(cal.getTime(), DateTimeUtil.DATE_FORMAT_PATTERN);
		
		String profileId = this.id;
		this.dailyReportUnits = DailyReportUnitDaoUtil.getDailyReportUnitMapForProfile(profileId, fromDate, toDate);
	}



	public Device getLastUsedDevice() {
		return lastUsedDevice;
	}
	
	public void setLastUsedDevice(Device lastUsedDevice) {
		this.lastUsedDevice = lastUsedDevice;
	}

	public Set<Device> getUsedDevices() {
		if(usedDevices == null) {
			usedDevices = new HashSet<Device>(0);
		}
		return usedDevices;
	}

	public void setUsedDevices(Set<Device> usedDevices) {
		this.usedDevices = usedDevices;
	}

	public List<Touchpoint> getTopEngagedTouchpoints() {
		return topEngagedTouchpoints;
	}

		
	public SortedSet<TargetMediaUnit> getTargetMediaUnits() {
		return targetMediaUnits;
	}

	public void setTargetMediaUnits(SortedSet<TargetMediaUnit> targetMediaUnits) {
		this.targetMediaUnits = targetMediaUnits;
	}
	
	
	/**
	 * update daily and hourly statistics
	 * 
	 * @param reportedDate
	 * @param eventName
	 * @param journeyMapId
	 */
	public synchronized void updateDailyReportUnit(Date reportedDate, String eventName, String journeyMapId) {
		// summary statistics
		this.updateEventCount(journeyMapId, eventName);
		
		// daily statistics
		String key = DailyReportUnit.buildMapKey(reportedDate, eventName, journeyMapId);
		DailyReportUnit reportUnit = this.dailyReportUnits.get(key);
		if(reportUnit == null) {
			String profileId = this.id;
			reportUnit = new DailyReportUnit(journeyMapId, COLLECTION_NAME, profileId, eventName, reportedDate);
			reportUnit.updateCountValue();
		}
		else {
			reportUnit.setUpdatedAt(reportedDate);
			reportUnit.updateCountValue();
		}
		this.dailyReportUnits.put(key, reportUnit);
	}
	
	public void setActivationTimeline(Map<String, Date> activationTimeline) {
		this.activationTimeline = activationTimeline;
	}

	public void setDailyReportUnits(Map<String, DailyReportUnit> dailyReportUnits) {
		this.dailyReportUnits = dailyReportUnits;
	}
	
	public void clearDailyReportUnits() {
		this.dailyReportUnits.clear();
	}

	public Map<String, DailyReportUnit> getDailyReportUnits() {
		return dailyReportUnits;
	}

	public Map<String, Date> getActivationTimeline() {
		return activationTimeline;
	}
	
	public void setActivationTimeline(String activationRule) {
		this.activationTimeline.put(activationRule, new Date()); 
	}
	

	public boolean shouldDirectActivation(String activationRule, long activatingDuration) {
		Date recordedDate =  this.activationTimeline.get(activationRule); 
		if(recordedDate == null) {
			//it's the first-time
			return true;
		}
		LocalDateTime now = LocalDateTime.now(ZoneId.of("GMT"));
		LocalDateTime recordedDateGMT = LocalDateTime.ofInstant(recordedDate.toInstant(), ZoneId.of("GMT"));
		long diff = ChronoUnit.HOURS.between(now, recordedDateGMT );
		return diff > activatingDuration;
	}
	
	/**
	 * this method is used for update data from Web Form, for only fields in profile with AutoSetDataByWebForm
	 * 
	 * @param updatingAttributes
	 * @return the id of updated profile
	 */
	public void updateDataWithAttributeMap(Map<String, Object> updatingAttributes) {
		ProfileModelUtil.updateWithAttributeMap(this, updatingAttributes);
	}
	
	/**
	 * @param engagedTouchpoints
	 */
	public final void doVectorization(List<TouchpointReport> engagedTouchpoints) {
		// build vectors of contentKeywords from touchpointNames 
		List<String> touchpointNames = engagedTouchpoints.stream().map(t->{ 
			if(t != null) {
				String name = t.getTouchpoint().getName();
				return name;
			}
			return "";
		}).filter(s -> {return !s.isBlank() && s.indexOf("http")<0;}).limit(42).collect(Collectors.toList());
		this.contentKeywords.addAll(touchpointNames);
		
		Set<String> purchasedCategories = this.purchasedItems.stream().map(t->{ 
			return t.getCategoryName();
		}).filter(s -> {return !s.isBlank();}).collect(Collectors.toSet());
		this.productKeywords.addAll(purchasedCategories);
		
		// make sure identities
		this.socialMediaProfiles.entrySet().forEach(e->{
			String uid = e.getKey() + ":" + e.getValue();
			identities.add(uid);
		});
		
		// build vectors of taxonomy metadata 
		this.identitiesAsStr = transformSetOfStrToStr(this.identities);
		this.dataLabelsAsStr = transformSetOfStrToStr(this.dataLabels);
		this.inSegmentsAsStr = transformSetOfRefKeysToStr(this.inSegments);
		this.inCampaignsAsStr = transformSetOfRefKeysToStr(this.inCampaigns);
		this.inAccountsAsStr = transformSetOfRefKeysToStr(this.inAccounts);
		this.inJourneyMapsAsStr = transformSetOfRefKeysToStr(this.inJourneyMaps);
	}


	
	/**
	 * @param engagedTouchpoints
	 */
	public final void doDataEnrichment(List<TouchpointReport> engagedTouchpoints) {
		this.setUpdatedAt(new Date());
		this.computeDataQualityScore();
		this.doVectorization(engagedTouchpoints);
	}
	
	/**
	 * compute the data quality score
	 */
	@Override
	public void computeDataQualityScore() {
		super.computeDataQualityScore();
	}

	public void runPersonalizationJob() {
		// TODO
	}




	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}
	
	@Override
	public void clearAllReportData() {
		System.out.println("ProfileSingleView.clearAllReportData");
		super.clearAllReportData();
		this.clearContextSessionKeys();
		this.clearDailyReportUnits();
	}
}
