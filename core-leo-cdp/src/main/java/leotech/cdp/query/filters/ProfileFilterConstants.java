package leotech.cdp.query.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import rfx.core.util.StringPool;

public class ProfileFilterConstants {

	// String keys
	public static final String JOURNEY_MAP_ID = "journeyMapId";
	public static final String SEARCH_KEYWORDS = "searchKeywords";
	public static final String VISITOR_ID = "visitorId";
	public static final String PROFILE_ID = "profileId";
	public static final String EMAILS = "emails";
	public static final String PHONES = "phones";
	public static final String APPLICATION_IDS = "applicationIDs";
	public static final String FINTECH_SYSTEM_IDS = "fintechSystemIDs";
	public static final String GOVERNMENT_ISSUED_IDS = "governmentIssuedIDs";
	public static final String LOYALTY_IDS = "loyaltyIDs";
	public static final String FINGERPRINT_ID = "fingerprintId";
	public static final String CRM_REF_ID = "crmRefId";
	public static final String LAST_TOUCHPOINT_NAME = "lastTouchpointName";
	public static final String SEGMENT_NAME = "segmentName";
	public static final String SEGMENT_ID = "segmentId";
	public static final String DATA_LABEL = "dataLabel";
	public static final String MEDIA_CHANNEL = "mediaChannel";
	public static final String EXCLUDE_PROFILE_ID = "excludeProfileId";
	public static final String LAST_SEEN_IP = "lastSeenIp";
	public static final String LAST_USED_DEVICE_ID = "lastUsedDeviceId";

	// Boolean keys
	public static final String SHOW_VISITOR = "showVisitor";
	public static final String SHOW_LEAD_AND_PROSPECT = "showLeadAndProspect";
	public static final String SHOW_CUSTOMER = "showCustomer";

	// Defaults
	public static final String DEFAULT_STRING = StringPool.BLANK;
	public static final boolean DEFAULT_BOOLEAN = true;

	/*
	 * Splits a string by semicolon and returns a list of non-blank strings.
	 * If the input string is null or blank, returns an empty list.
	 * returns List<String>
	 */
	public static List<String> splitToList(String value) {
		if (value == null || value.isBlank())
			return Collections.emptyList();
		return Arrays.stream(value.split(StringPool.SEMICOLON)).filter(s -> !s.isBlank()).collect(Collectors.toList());
	}
}