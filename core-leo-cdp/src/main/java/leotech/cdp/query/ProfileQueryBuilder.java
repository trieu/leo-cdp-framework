package leotech.cdp.query;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import leotech.cdp.model.analytics.FeedbackData;
import leotech.cdp.model.analytics.FinanceEvent;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.customer.Device;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.query.filters.ProfileFilter;
import leotech.system.util.database.PersistentObject;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * Profile Query Builder
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public class ProfileQueryBuilder {
	static Logger logger = LoggerFactory.getLogger(ProfileQueryBuilder.class);
	
	static final String PROFILE_LAST_EVENTS = " LET lastItemViewEvent = FIRST( FOR e IN cdp_trackingevent FILTER e.refProfileId == p._key AND e.metricName == 'item-view' SORT e.createdAt DESC LIMIT 1 RETURN UNSET(e, ['rawJsonData','partitionId','sessionKey','state','environment']) ) \n "
			+ " LET lastPurchaseEvent = FIRST( FOR e IN cdp_trackingevent FILTER e.refProfileId == p._key AND (e.metricName == 'purchase' OR e.metricName == 'order-checkout') SORT e.createdAt DESC LIMIT 1 RETURN UNSET(e, ['rawJsonData','partitionId','sessionKey','state','environment']) ) \n "
			+ " LET lastTrackingEvent = FIRST( FOR e IN cdp_trackingevent FILTER e.refProfileId == p._key SORT e.createdAt DESC LIMIT 1 RETURN UNSET(e, ['rawJsonData','partitionId','sessionKey','state','environment']) ) \n "
			+ " LET events = {lastTrackingEvent:lastTrackingEvent, lastPurchaseEvent : lastPurchaseEvent, lastItemViewEvent: lastItemViewEvent} ";

	static final String CDP_EVENT_JOIN = " f.refProfileId == d._key ";
	static final String CDP_DEVICE_JOIN = " (f._key == d.lastUsedDeviceId  || f._key IN d.usedDeviceIds) ";
	static final String CDP_TOUCHPOINT_JOIN = " (f._key == d.lastTouchpointId  || f._key IN d.topEngagedTouchpointIds) ";
	static final String CDP_EVENT_OBSERVER_JOIN = " f._key == d.lastObserverId ";

	/**
	 * @param withLastEvents
	 * @param filterCreateAt
	 * @param filterDataSet
	 * @param parsedAql
	 * @param startIndex
	 * @param numberResult
	 * @param profileFields
	 * @return AQL for Segment Query
	 */
	public static String buildArangoQueryString(boolean withLastEvents, String filterDataSet, String parsedAql, int startIndex, int numberResult, List<String> profileFields) {
		// build AQL to find matchedProfiles
		StringBuilder finalAql = new StringBuilder("LET matchedProfiles = ( FOR d in ").append(Profile.COLLECTION_NAME);
		
		finalAql.append(" FILTER d.status > 0 ");
		
		// data filter on another collection
		boolean hasfilterDataSet = StringUtil.isNotEmpty(filterDataSet);
		if(hasfilterDataSet) {
			finalAql.append(" FOR f in ").append(filterDataSet).append(" FILTER ");
		}
		
		if ( ! parsedAql.isEmpty()) {
			parsedAql = buildAqlToFilterData(parsedAql);
			
			if(TrackingEvent.COLLECTION_NAME.equals(filterDataSet) 
					|| FinanceEvent.COLLECTION_NAME.equals(filterDataSet) 
					|| FeedbackData.COLLECTION_NAME.equals(filterDataSet)) {
				finalAql.append(" ").append(CDP_EVENT_JOIN).append(" ").append(parsedAql).append(" ");
			}
			else if(Device.COLLECTION_NAME.equals(filterDataSet)) {
				finalAql.append(" ").append(CDP_DEVICE_JOIN).append(" ").append(parsedAql).append(" ");
			}
			else if(Touchpoint.COLLECTION_NAME.equals(filterDataSet)) {
				finalAql.append(" ").append(CDP_TOUCHPOINT_JOIN).append(" ").append(parsedAql).append(" ");
			}
			else if(EventObserver.COLLECTION_NAME.equals(filterDataSet)) {
				finalAql.append(" ").append(CDP_EVENT_OBSERVER_JOIN).append(" ").append(parsedAql).append(" ");
			}
			else {
				finalAql.append(parsedAql);
			}
		}
		
		int size = profileFields.size();
		if (size == 0) {
			// select all fields
			finalAql.append(" RETURN DISTINCT d ) ");
		} 
		else {
			// only selected fields are returned
			finalAql.append(" RETURN DISTINCT { ");
			for (int i = 0; i < size; i++) {
				String field = profileFields.get(i);
				if (field.equals("id") || field.equals("_key")) {
					finalAql.append(field).append(":").append("d._key");
				} else {
					finalAql.append(field).append(":").append("d.").append(field);
				}

				if (i < size - 1) {
					finalAql.append(", ");
				}
			}
			finalAql.append(" } ) ");
		}
			
		// pagination
		if(withLastEvents) {
			finalAql.append(" FOR p in matchedProfiles ");
			finalAql.append(PROFILE_LAST_EVENTS);
			finalAql.append(" SORT p.updatedAt DESC LIMIT ").append(startIndex).append(",").append(numberResult).append(" RETURN MERGE(p, events) ");
		}
		else {
			finalAql.append(" FOR p in matchedProfiles SORT p.updatedAt DESC LIMIT ").append(startIndex).append(",").append(numberResult).append(" RETURN p ");
		}
		String aql = finalAql.toString();
		
		if(SystemMetaData.isDevMode()) {
			String s = aql.substring(0, aql.indexOf("RETURN DISTINCT"));
			logger.info(" buildArangoQueryString: \n" + s);
		}
		
		return aql;
	}
	
	/**
	 * 
	 * to build AQL to UNION_DISTINCT of indexedProfileIds and matchedProfilesIds to refresh in_segments
	 * 
	 * @param segmentId
	 * @param filterDataSet
	 * @param parsedAql
	 * @return
	 */
	public static String buildSegmentQueryToBuildIndex(String segmentId, String filterDataSet, String parsedAql) {
		if(StringUtil.isEmpty(segmentId)) {
			return "";
		}
		// build AQL to find matchedProfiles
		StringBuilder aql = new StringBuilder();
		
		aql.append(" LET indexedProfileIds = ( FOR p1 in ").append(Profile.COLLECTION_NAME);
		aql.append(" FILTER '").append(segmentId).append("' IN p1.inSegments[*].id AND p1.status > 0 RETURN p1._key ) ");
		
		aql.append(" LET matchedProfilesIds = ( FOR d in ").append(Profile.COLLECTION_NAME);
		
		aql.append(" FILTER d.status > 0 ");
		
		// data filter on another collection
		boolean hasfilterDataSet = StringUtil.isNotEmpty(filterDataSet);
		if(hasfilterDataSet) {
			aql.append(" FOR f in ").append(filterDataSet).append(" FILTER ");
		}
		
		if ( ! parsedAql.isEmpty()) {
			parsedAql = buildAqlToFilterData(parsedAql);
			
			if(TrackingEvent.COLLECTION_NAME.equals(filterDataSet) 
					|| FinanceEvent.COLLECTION_NAME.equals(filterDataSet) 
					|| FeedbackData.COLLECTION_NAME.equals(filterDataSet)) {
				aql.append(" ").append(CDP_EVENT_JOIN).append(" ").append(parsedAql).append(" ");
			}
			else if(Device.COLLECTION_NAME.equals(filterDataSet)) {
				aql.append(" ").append(CDP_DEVICE_JOIN).append(" ").append(parsedAql).append(" ");
			}
			else if(Touchpoint.COLLECTION_NAME.equals(filterDataSet)) {
				aql.append(" ").append(CDP_TOUCHPOINT_JOIN).append(" ").append(parsedAql).append(" ");
			}
			else if(EventObserver.COLLECTION_NAME.equals(filterDataSet)) {
				aql.append(" ").append(CDP_EVENT_OBSERVER_JOIN).append(" ").append(parsedAql).append(" ");
			}
			else {
				aql.append(parsedAql);
			}
		}
		
		aql.append(" RETURN DISTINCT d._key ) ");
		aql.append(" LET ids = UNION_DISTINCT(matchedProfilesIds, indexedProfileIds) FOR id IN ids RETURN id ");
		
		String a = aql.toString();
		
		if(SystemMetaData.isDevMode()) {
			String l = aql.substring(0, a.indexOf("RETURN DISTINCT")+15);
			logger.info(" buildArangoQueryString: \n" + l);
		}
		
		return a;
	}
	
	/**
	 * @param parsedFilterAql
	 * @return
	 */
	public static String buildCoutingQuery(String parsedFilterAql) {
		return buildCoutingQuery(null, null, parsedFilterAql);
	}

	/**
	 * @param filterCreateAt
	 * @param joinCollectionName
	 * @param parsedAql
	 * @return
	 */
	public  static String buildCoutingQuery(String profileIdToCheck, String joinCollectionName, String parsedAql) {
		StringBuilder aql = new StringBuilder("RETURN LENGTH( FOR d in ").append(Profile.COLLECTION_NAME);
		
		// could this profile be in this segment ?
		if(StringUtil.isNotEmpty(profileIdToCheck)) {
			aql.append(" FILTER d._key == '").append(profileIdToCheck).append("' ");
		}
		
		aql.append(" FILTER d.status > 0 ");
		
		// data filter on another collection
		boolean hasJoinCollection = StringUtil.isNotEmpty(joinCollectionName);
		if(hasJoinCollection) {
			aql.append(" FOR f in ").append(joinCollectionName).append(" FILTER ");
		}

		if ( !parsedAql.isEmpty() ) {
			parsedAql = buildAqlToFilterData(parsedAql);
			
			if(Device.COLLECTION_NAME.equals(joinCollectionName)) {
				aql.append(" ").append(CDP_DEVICE_JOIN).append(" ").append(parsedAql).append("  ");
			}
			else if(Touchpoint.COLLECTION_NAME.equals(joinCollectionName)) {
				aql.append(" ").append(CDP_TOUCHPOINT_JOIN).append(" ").append(parsedAql).append("  ");
			}
			else if(EventObserver.COLLECTION_NAME.equals(joinCollectionName)) {
				aql.append(" ").append(CDP_EVENT_OBSERVER_JOIN).append(" ").append(parsedAql).append("  ");
			}
			else if(TrackingEvent.COLLECTION_NAME.equals(joinCollectionName) 
					|| FinanceEvent.COLLECTION_NAME.equals(joinCollectionName) 
					|| FeedbackData.COLLECTION_NAME.equals(joinCollectionName)) {
				aql.append(" ").append(CDP_EVENT_JOIN).append(" ").append(parsedAql).append("  ");
			}
			else {
				aql.append(parsedAql);
			}
		}
		aql.append(" RETURN DISTINCT {id:d._key} ) ");

		return aql.toString();
	}

	/**
	 * FIXME not good ideas but OK 
	 * 
	 * @param parsedAql
	 * @return
	 */
	private static String buildAqlToFilterData(String originalQuery) {
		originalQuery = originalQuery.trim();
		String parsedAql = " FILTER " + originalQuery + " ";
		return parsedAql;
	}
	

	/**
	 * in Profile Management
	 * 
	 * @param filter
	 * @param countingTotal
	 * @return the AQL from ProfileFilter
	 */
	public static String buildAqlFromProfileFilter(ProfileFilter filter, boolean countingTotal) {
		String aql;
		if(filter.isForDeduplication()) {
			aql = aqlToGetDuplicateProfiles(filter, countingTotal);
			logger.info("aqlToGetDuplicateProfiles \n" + aql);
		}
		else {
			aql = aqlToGetProfiles(filter, countingTotal);
			logger.info("aqlToGetProfiles \n" + aql);
		}
		return aql;
	}

	/**
	 * @param filter
	 * @param countingTotal
	 * @return
	 */
	static String aqlToGetProfiles(ProfileFilter filter, boolean countingTotal) {
		StringBuilder aql = new StringBuilder();
		
		String journeyMapId = filter.getJourneyMapId();
		String loginUsername = filter.getLoginUsername();
		String segmentId = filter.getSegmentId();
		boolean isNotAdmin = ! filter.hasAdminRole();
		int status = filter.getStatus();
		
		String profileId = filter.getProfileId();
		String visitorId = filter.getVisitorId();
		List<String> emails = filter.getEmails();
		List<String> phones = filter.getPhones();
		String fingerprintId = filter.getFingerprintId();
		String crmRefId = filter.getCrmRefId();
		
		String searchKeywords = filter.getSearchKeywords();
		String lastTouchpointName = filter.getLastTouchpointName();
		String segmentName = filter.getSegmentName().toLowerCase();
		String dataLabel = filter.getDataLabel();
		String mediaChannel = filter.getMediaChannel();
		String excludeProfileId = filter.getExcludeProfileId();
		
		String authorizedViewer = filter.getAuthorizedViewer();
		String authorizedEditor = filter.getAuthorizedEditor();
		
		if(countingTotal) {
			aql.append(" RETURN LENGTH( ");
		}
		// BEGIN query
		aql.append("FOR p in ").append(Profile.COLLECTION_NAME);
		
		if(status == Profile.STATUS_DEAD) {
			aql.append(" FILTER (p.status == ").append(Profile.STATUS_DEAD);
		}
		else {
			aql.append(" FILTER (p.status == ").append(status);
		}
		
		if(StringUtil.isNotEmpty(excludeProfileId)) {
			// get all profiles of person, the sort by data quality score
			aql.append(" AND p._key != '").append(excludeProfileId).append("' ");
		}

		// email
		for (String email : emails) {
			if(StringUtil.isNotEmpty(email)) {
				aql.append(" AND (p.primaryEmail LIKE \"%").append(email).append("%\" OR \"").append(email).append("\" IN p.secondaryEmails) ");
			}
		}
		// phone
		for (String phone : phones) {
			if(StringUtil.isNotEmpty(phone)) {
				aql.append(" AND (p.primaryPhone LIKE \"%").append(phone).append("%\" OR \"").append(phone).append("\" IN p.secondaryPhones) ");
			}
		}
		
		// extra filtering profile 
		if(StringUtil.isNotEmpty(profileId)) {
			aql.append(" AND p._key == \"").append(profileId).append("\" ");
		}
		else if(StringUtil.isNotEmpty(visitorId)) {
			aql.append(" AND p.visitorId == \"").append(visitorId).append("\" ");
		}
		else if(StringUtil.isNotEmpty(crmRefId)) {
			aql.append(" AND p.crmRefId == \"").append(crmRefId).append("\" ");
		}
		else if(StringUtil.isNotEmpty(fingerprintId)) {
			aql.append(" AND p.fingerprintId == \"").append(fingerprintId).append("\" ");			
		}
		else if(StringUtil.isNotEmpty(lastTouchpointName)) {
			aql.append(" AND LOWER(p.lastTouchpoint.name) LIKE \"%").append(lastTouchpointName.toLowerCase()).append("%\" ");			
		}
		else if(StringUtil.isNotEmpty(segmentName)) {
			aql.append(" AND LENGTH(FOR s IN p.inSegments FILTER LOWER(s.name) LIKE \"%").append(segmentName).append("%\" RETURN s) > 0 ");
		}
		
		//filter lead profile
		if(filter.showAllActiveProfile()) {
			if(filter.showOnlyVisitor()) {
				aql.append(" AND ( (p.firstName == '' AND p.lastName == '') AND (p.primaryEmail == '' AND p.primaryPhone == '') OR p.type == 0 ) ");
			}
			else if(filter.showLeadAndProspectAndCustomer()) {
				aql.append(" AND ( (p.firstName != '' OR p.lastName != '') AND (p.primaryEmail != '' OR  p.primaryPhone != '') AND p.type > 0 ) ");
			}
			else if(filter.showLeadOrProspectOrCustomer()) {
				aql.append(" AND ( (p.firstName != '' OR p.lastName != '') AND (p.primaryEmail != '' OR  p.primaryPhone != '') AND p.type > 0 ) ");
				
				if(filter.showOnlyLeadOrProspect()) {
					aql.append(" AND (p.funnelStage == 'lead' OR p.funnelStage == 'prospect' ) ");
				}
				else {
					aql.append(" AND (p.funnelStage == 'new-customer' OR p.funnelStage == 'engaged-customer' ");
					aql.append(" OR p.funnelStage == 'happy-customer' OR p.funnelStage == 'customer-advocate' OR p.funnelStage == 'unhappy-customer' OR p.funnelStage == 'terminated-customer' )");
				}
			}
			else if(filter.showVisitorOrLead()) {
				aql.append(" AND ( p.funnelStage == 'new-visitor' OR p.funnelStage == 'returning-visitor' ");
				aql.append(" OR p.funnelStage == 'lead' OR p.funnelStage == 'prospect' )");
			}
			else if(filter.showVisitorOrCustomer()) {
				aql.append(" AND (p.funnelStage == 'new-customer' OR p.funnelStage == 'engaged-customer' ");					
				aql.append(" OR p.funnelStage == 'happy-customer' OR p.funnelStage == 'customer-advocate' OR p.funnelStage == 'unhappy-customer' OR p.funnelStage == 'terminated-customer' ");
				aql.append(" OR p.funnelStage == 'new-visitor' OR p.funnelStage == 'returning-visitor' )");
			}
		}
		else {
			aql.append(" AND p.status < 1 ");
		}
		
		// Filter data by modification date 
		String beginDate = filter.getBeginFilterDate();
		if(StringUtil.isNotEmpty(beginDate)) {
			aql.append(" AND p.updatedAt >= \"").append(beginDate).append("\" ");
		}
		// endFilterDate
		String endDate = filter.getEndFilterDate();
		if(StringUtil.isNotEmpty(endDate)) {
			aql.append(" AND p.updatedAt <= \"").append(endDate).append("\" ");
		}

		// end SEARCH block
		aql.append(") ");


		// search by keywords in firstName or lastName or email or phone
		if(StringUtil.isNotEmpty(searchKeywords)) {
			if(searchKeywords.length() >= 2) {
				searchKeywords = searchKeywords.toLowerCase();
				aql.append(" FILTER (");
				aql.append(" p.firstName LIKE \"%").append(searchKeywords.toLowerCase()).append("%\" ");
				aql.append(" OR p.lastName LIKE \"%").append(searchKeywords.toLowerCase()).append("%\" ");
				aql.append(" OR p.primaryEmail LIKE \"%").append(searchKeywords.toLowerCase()).append("%\" ");
				aql.append(" OR p.primaryPhone LIKE \"%").append(searchKeywords.toLowerCase()).append("%\" ");
				aql.append(" OR LENGTH ( FOR data IN p.secondaryEmails FILTER CONTAINS(data, \"").append(searchKeywords).append("\") return data) > 0 ");
				aql.append(" OR LENGTH ( FOR data IN p.secondaryPhones FILTER CONTAINS(data, \"").append(searchKeywords).append("\") return data) > 0 ");
				aql.append(" )");
			}
		}

		// check data authorization
		if(StringUtil.isNotEmpty(loginUsername) && isNotAdmin) {
			// systemUserId must be in the authorizedViewers or authorizedViewers is empty
			aql.append(" FILTER LENGTH ( FOR data IN p.authorizedEditors FILTER CONTAINS(data, \"").append(loginUsername).append("\") return data) > 0 ");
		}

		if(StringUtil.isNotEmpty(journeyMapId)) {
			aql.append(" FILTER LENGTH ( FOR data IN p.inJourneyMaps[*].id FILTER CONTAINS(data, \"").append(journeyMapId).append("\") return data) > 0 ");
		}

		if(StringUtil.isNotEmpty(dataLabel)) {
			aql.append(" FILTER LENGTH ( FOR data IN p.dataLabels FILTER CONTAINS(data, \"").append(dataLabel).append("\") return data) > 0 ");
		}
		else if(StringUtil.isNotEmpty(mediaChannel)) {
			aql.append(" FILTER LENGTH ( FOR data IN p.mediaChannels FILTER CONTAINS(data, \"").append(mediaChannel).append("\") return data) > 0 ");
		}
		else if(StringUtil.isNotEmpty(authorizedViewer)) {
			aql.append(" FILTER LENGTH ( FOR data IN p.authorizedViewers FILTER CONTAINS(data, \"").append(authorizedViewer).append("\") return data) > 0 ");
		}
		else if(StringUtil.isNotEmpty(authorizedEditor)) {
			aql.append(" FILTER LENGTH ( FOR data IN p.authorizedEditors FILTER CONTAINS(data, \"").append(authorizedEditor).append("\") return data) > 0 ");
		}
		else if(StringUtil.isNotEmpty(segmentId)) {
			aql.append(" FILTER LENGTH ( FOR data IN p.inSegments[*].id FILTER CONTAINS(data, \"").append(segmentId).append("\") return data) > 0 ");
		}

		
		// total result
		if(countingTotal) {
			aql.append(" RETURN p._key ");
		} 
		else {
			// pagination
			JsonArray orderableFields = filter.getOrderableFields();
			if(orderableFields.size() > 0) {
				JsonObject field = orderableFields.getJsonObject(0);
				String fieldName = field.getString("field", "updatedAt");
				String direction = field.getString("dir", "desc").toUpperCase();
				aql.append(" SORT p.").append(fieldName).append(" ").append(direction);
			}
			else {
				aql.append(" SORT p.updatedAt DESC ");
			}
			
			aql.append(" LIMIT @startIndex, @numberResult ");
			aql.append(" RETURN p ");
		}
		
		if(countingTotal) {
			aql.append(" ) ");
		}
		
		return aql.toString();
	}
	
	/**
	 * @param filter
	 * @param countingTotal
	 * @return
	 */
	static String aqlToGetDuplicateProfiles(ProfileFilter filter, boolean countingTotal) {
		StringBuilder aql = new StringBuilder();
		
		String excludeProfileId = filter.getExcludeProfileId();
		String visitorId = filter.getVisitorId();
		List<String> emails = filter.getEmails();
		List<String> phones = filter.getPhones();
		
		String fingerprintId = filter.getFingerprintId();
		String crmRefId = filter.getCrmRefId();
		String lastSeenIp = filter.getLastSeenIp();
		String lastUsedDeviceId = filter.getLastUsedDeviceId();
		
		List<String> loyaltyIDs = filter.getLoyaltyIDs();
		List<String> governmentIssuedIDs = filter.getGovernmentIssuedIDs();
		List<String> applicationIDs = filter.getApplicationIDs();
		List<String> fintechSystemIDs = filter.getFintechSystemIDs();
	
		if(countingTotal) {
			aql.append(" RETURN LENGTH( ");
		}
		
		aql.append("FOR p in ").append(Profile.COLLECTION_NAME);
		aql.append(" FILTER ");
		
		
		if(StringUtil.isNotEmpty(excludeProfileId) && !filter.isDataDeduplicationJob()) {
			// get all profiles of person, the sort by data quality score
			aql.append(" p._key != '").append(excludeProfileId).append("' AND ( ");
		}
		else {
			aql.append("( ");
		}
		
		for (String email : emails) {
			if(StringUtil.isNotEmpty(email)) {
				aql.append(" (p.primaryEmail == '").append(email).append("' OR '").append(email).append("' IN p.secondaryEmails) OR ");
			}
		}
		
		for (String phone : phones) {
			if(StringUtil.isNotEmpty(phone)) {
				aql.append(" (p.primaryPhone == '").append(phone).append("' OR '").append(phone).append("' IN p.secondaryPhones) OR ");
			}
		}
		
		for (String loyaltyID : loyaltyIDs) {
			if(StringUtil.isNotEmpty(loyaltyID)) {
				aql.append(" ('").append(loyaltyID).append("' IN p.loyaltyIDs) OR ");
			}
		}
		for (String governmentIssuedID : governmentIssuedIDs) {
			if(StringUtil.isNotEmpty(governmentIssuedID)) {
				aql.append(" ('").append(governmentIssuedID).append("' IN p.governmentIssuedIDs) OR ");
			}
		}
		for (String applicationID : applicationIDs) {
			if(StringUtil.isNotEmpty(applicationID)) {
				aql.append(" ('").append(applicationID).append("' IN p.applicationIDs) OR ");
			}
		}
		for (String fintechSystemID : fintechSystemIDs) {
			if(StringUtil.isNotEmpty(fintechSystemID)) {
				aql.append(" ('").append(fintechSystemID).append("' IN p.fintechSystemIDs) OR ");
			}
		}
		// FIXME
		
		if(StringUtil.isNotEmpty(crmRefId)) {
			aql.append(" p.crmRefId == '").append(crmRefId).append("' OR ");
		}
		if(StringUtil.isNotEmpty(fingerprintId) && StringUtil.isNotEmpty(lastSeenIp) && StringUtil.isNotEmpty(lastUsedDeviceId)) {
			aql.append(" (p.fingerprintId == '").append(fingerprintId).append("' AND ");
			aql.append(" p.lastSeenIp == '").append(lastSeenIp).append("' AND ");	
			aql.append(" p.lastUsedDeviceId == '").append(lastUsedDeviceId).append("' ) OR ");	
		}
		
		if(StringUtil.isNotEmpty(visitorId)) {
			aql.append(" p.visitorId == '").append(visitorId).append("' ) AND ");
		}
		else {
			aql.append(" p.visitorId == '_' ) AND ");
		}
		
		aql.append(" p.status == ").append(PersistentObject.STATUS_ACTIVE).append(" ");
		
		// Filter data by modification date 
		String beginDate = filter.getBeginFilterDate();
		if(StringUtil.isNotEmpty(beginDate)) {
			aql.append(" AND p.updatedAt >= \"").append(beginDate).append("\" ");
		}
		// endFilterDate
		String endDate = filter.getEndFilterDate();
		if(StringUtil.isNotEmpty(endDate)) {
			aql.append(" AND p.updatedAt <= \"").append(endDate).append("\" ");
		}
		
		// total result
		if(countingTotal) {
			aql.append(" RETURN p._key ");
		} 
		else {
			// pagination
			aql.append(" SORT p.dataQualityScore DESC ");
			if( ! filter.isDataDeduplicationJob() ) {
				// listing in profile info
				aql.append(" LIMIT @startIndex, @numberResult ");
			}
			
			aql.append(" RETURN p ");
		}
		
		if(countingTotal) {
			aql.append(" ) ");
		}
		
		return aql.toString();
	}
}
