package leotech.cdp.model.customer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Precision;

import leotech.cdp.domain.EventDataManagement;
import leotech.cdp.model.analytics.TrackingEvent;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * Profile Exporting Data Utility Class to create custom audiences in Ads Platforms
 * 
 * @author tantrieuf31
 *
 */
public final class ProfileExportingDataUtil {

	private static final String QUOTE = "\"";

	private static final String DD_MM_YYYY_HH_MM_SS = "dd-MM-yyyy hh:mm:ss";
	
	public static final String FACEBOOK_UID = "FACEBOOK_UID";
	public static final String GOOGLE_UID = "GOOGLE_UID";
	public static final String LINKEDIN_UID = "LINKEDIN_UID";
	public static final String TWITTER_UID = "TWITTER_UID";
	public static final String TIKTOK_UID = "TIKTOK_UID";
	
	public static final String CSV_FACEBOOK_HEADER = "uid,email,phone,fn,ln,dob,ct,st,zip,country,value";
	public static final String CSV_LEO_CDP_HEADER = "uid,email,phone,first-name,last-name,birthday,city,state,zip,country,value,acquisition,prospect,engagement,transaction,loyalty,cfs,ces,nps,csat,web-visitor-id,crm-id,labels,in-segments,notes,exported-date,top-100-events";
	public static final List<Object> CSV_FACEBOOK_HEADER_GOOGLE_SHEET = List.of("uid","email","phone","fn","ln","dob","ct","st","zip","country","value");
	public static final List<Object> CSV_LEO_CDP_HEADER_GOOGLE_SHEET = List.of("uid","email","phone","fn","ln","dob","ct","st","zip","country","value","acquisition","prospect","engagement","transaction","loyalty","cfs","ces","nps","csat","web-visitor-id","crm-id","labels","in-segments","notes","exported-date","top-100-events");

	public static final int CSV_TYPE_LEO_CDP = 0;
	public static final int CSV_TYPE_FACEBOOK = 1;
	public static final int CSV_TYPE_GOOGLE = 2;
	public static final int CSV_TYPE_TWITTER = 3;
	public static final int CSV_TYPE_LINKEDIN = 4;
	public static final int CSV_TYPE_TIKTOK = 5;
	
	/**
	 * to export data as CSV format
	 * 
	 * @param Profile p
	 * @param int csvType
	 * @return
	 */
	final public static String exportDataAsFileCSV(Profile p, int csvType) {
		StringBuffer s = new StringBuffer();

		// default data for Facebook 
		String uid;
		if(csvType == CSV_TYPE_FACEBOOK) {
			uid = StringUtil.safeString(p.extAttributes.get(FACEBOOK_UID),"");
		}
		else if(csvType == CSV_TYPE_GOOGLE) {
			uid = StringUtil.safeString(p.extAttributes.get(GOOGLE_UID),"");
		}
		else if(csvType == CSV_TYPE_LINKEDIN) {
			uid = StringUtil.safeString(p.extAttributes.get(LINKEDIN_UID),"");
		}
		else if(csvType == CSV_TYPE_TWITTER) {
			uid = StringUtil.safeString(p.extAttributes.get(TWITTER_UID),"");
		}
		else if(csvType == CSV_TYPE_TIKTOK) {
			uid = StringUtil.safeString(p.extAttributes.get(TIKTOK_UID),"");
		}
		else {
			uid = p.id;
		}
		s.append(uid).append(StringPool.COMMA);
		
		s.append(StringUtil.safeString(p.primaryEmail, "")).append(StringPool.COMMA);

		if(StringUtil.isNotEmpty(p.primaryPhone)) {
			s.append("'").append(StringUtil.safeString(p.primaryPhone, "")).append(StringPool.COMMA);
		}
		else {
			s.append(StringPool.SPACE).append(StringPool.COMMA);
		}
		s.append(StringUtil.safeString(p.firstName, "")).append(StringPool.COMMA);
		s.append(StringUtil.safeString(p.lastName, "")).append(StringPool.COMMA);
		
		String dateOfBirth = p.dateOfBirth != null ? DateTimeUtil.formatDate(p.dateOfBirth, DateTimeUtil.DATE_FORMAT_PATTERN) : "";
		s.append(StringUtil.safeString(dateOfBirth)).append(StringPool.COMMA);
		s.append(StringUtil.safeString(p.livingCity, "")).append(StringPool.COMMA);
		s.append(StringUtil.safeString(p.livingState, "")).append(StringPool.COMMA);
		s.append(StringUtil.safeString(p.currentZipCode, "")).append(StringPool.COMMA);
		
		s.append(StringUtil.safeString(p.livingCountry, "")).append(StringPool.COMMA);
		s.append(StringUtil.safeString( Precision.round(p.totalCLV,2) , "0"));
		
		if(csvType == CSV_TYPE_LEO_CDP) {
			s.append(StringPool.COMMA);
			s.append(StringUtil.safeString( Precision.round(p.totalCAC,2) , "0")).append(StringPool.COMMA);
			s.append(p.totalProspectScore).append(StringPool.COMMA);
			s.append(p.totalEngagementScore).append(StringPool.COMMA);
			s.append(Precision.round(p.totalTransactionValue,2)).append(StringPool.COMMA);
			s.append(p.totalLoyaltyScore).append(StringPool.COMMA);
			
			s.append(p.totalCFS).append(StringPool.COMMA);
			s.append(p.totalCES).append(StringPool.COMMA);
			s.append(p.totalNPS).append(StringPool.COMMA);
			s.append(p.totalCSAT).append(StringPool.COMMA);
			
			s.append(StringUtil.safeString(p.visitorId, "_")).append(StringPool.COMMA);
			s.append(StringUtil.safeString(p.crmRefId, "_")).append(StringPool.COMMA);
			
			s.append(joinStringForCsvData(p.getDataLabels(), StringPool.SEMICOLON)).append(StringPool.COMMA);
			s.append(joinStringForCsvData(p.getInSegmentNames(), StringPool.SEMICOLON)).append(StringPool.COMMA);
			s.append(safeStringForCsv(p.notes)).append(StringPool.COMMA);
			s.append(DateTimeUtil.formatDate(new Date(), DD_MM_YYYY_HH_MM_SS)).append(StringPool.COMMA);
			s.append(getTrackingEventAsString(p.id));
			
			// add event			
			
		}
		
		// TODO add more another CSV format here
		
		return s.toString();
	}

	public static List<Object> exportDataAsGoogleSheetRow(Profile p, int csvType) {
		String uid;
		int length = 0;
		if(csvType == CSV_TYPE_FACEBOOK) {
			uid = StringUtil.safeString(p.extAttributes.get(FACEBOOK_UID),"");
			length = CSV_FACEBOOK_HEADER_GOOGLE_SHEET.size();
		}
		else if(csvType == CSV_TYPE_GOOGLE) {
			uid = StringUtil.safeString(p.extAttributes.get(GOOGLE_UID),"");
		}
		else if(csvType == CSV_TYPE_LINKEDIN) {
			uid = StringUtil.safeString(p.extAttributes.get(LINKEDIN_UID),"");
		}
		else if(csvType == CSV_TYPE_TWITTER) {
			uid = StringUtil.safeString(p.extAttributes.get(TWITTER_UID),"");
		}
		else if(csvType == CSV_TYPE_TIKTOK) {
			uid = StringUtil.safeString(p.extAttributes.get(TIKTOK_UID),"");
		}
		else {
			uid = p.id;
			length = CSV_FACEBOOK_HEADER_GOOGLE_SHEET.size();
		}

		List<Object> row = new ArrayList<>(length);

		row.add(uid);
		row.add(StringUtil.safeString(p.primaryEmail, ""));

		if(StringUtil.isNotEmpty(p.primaryPhone)) {
			row.add(StringUtil.safeString(p.primaryPhone, ""));
		}
		else {
			row.add(StringPool.SPACE);
		}

		row.add(StringUtil.safeString(p.firstName, ""));
		row.add(StringUtil.safeString(p.lastName, ""));

		String dateOfBirth = p.dateOfBirth != null ? DateTimeUtil.formatDate(p.dateOfBirth, DateTimeUtil.DATE_FORMAT_PATTERN) : "";
		row.add(dateOfBirth);
		row.add(StringUtil.safeString(p.livingCity, ""));
		row.add(StringUtil.safeString(p.livingState, ""));
		row.add(StringUtil.safeString(p.currentZipCode, ""));

		row.add(StringUtil.safeString(p.livingCountry, ""));
		row.add(StringUtil.safeString( Precision.round(p.totalCLV,2) , "0"));


		if(csvType == CSV_TYPE_LEO_CDP) {
			row.add(StringUtil.safeString(Precision.round(p.totalCAC,2) , "0"));
			row.add(p.totalProspectScore);
			row.add(p.totalEngagementScore);
			row.add(Precision.round(p.totalTransactionValue,2));
			row.add(p.totalLoyaltyScore);

			row.add(p.totalCFS);
			row.add(p.totalCES);
			row.add(p.totalNPS);
			row.add(p.totalCSAT);

			row.add(StringUtil.safeString(p.visitorId, "_"));
			row.add(StringUtil.safeString(p.crmRefId, "_"));

			row.add(joinStringForCsvData(p.getDataLabels(), StringPool.SEMICOLON));
			row.add(joinStringForCsvData(p.getInSegmentNames(), StringPool.SEMICOLON));
			row.add(safeStringForCsv(p.notes));
			row.add(DateTimeUtil.formatDate(new Date(), DD_MM_YYYY_HH_MM_SS));
			row.add(getTrackingEventAsString(p.id));

			// add event
		}

		// TODO add more another CSV format here

		return row;
	}

	public static List<Object> getGoogleSheetHeader(int csvType) {
		if(csvType == CSV_TYPE_FACEBOOK) {
			return CSV_FACEBOOK_HEADER_GOOGLE_SHEET;
		}
		else {
			return CSV_LEO_CDP_HEADER_GOOGLE_SHEET;
		}
	}

	static String safeStringForCsv(String s) {
		return QUOTE + StringUtil.safeString(s, "_").replaceAll(QUOTE, "") + QUOTE;
	}
	
	static String joinStringForCsvData(Set<String> toks, String delimiter){
		int size = toks.size();
		if(size == 0) {
			return "_";
		}
		
		StringBuilder s = new StringBuilder();
		s.append(QUOTE);
		for (String tok : toks) {
			s.append(StringPool.SPACE).append(tok).append(StringPool.SPACE).append(delimiter);
		}
		s.append(QUOTE);
		return s.toString();
	}
	
	static String getTrackingEventAsString(String profileId){
		List<TrackingEvent> list = EventDataManagement.getTrackingEventsOfProfile(profileId, "", 0, 100);
		StringBuilder s = new StringBuilder();
		s.append(QUOTE);
		String delimiter = StringPool.SEMICOLON;
		for (TrackingEvent e : list) {
			String time = DateTimeUtil.formatDate(e.getCreatedAt(), DD_MM_YYYY_HH_MM_SS);

			if(StringUtil.isNotEmpty(e.getMetricName())) {
				s.append(StringPool.SPACE).append(e.getMetricName());
			}

			s.append(StringPool.COLON).append(e.getMetricValue());

			if(StringUtil.isNotEmpty(time)) {
				s.append(StringPool.COLON).append(time);
			}

			if(StringUtil.isNotEmpty(e.getSrcTouchpointUrl())) {
				s.append(StringPool.COLON).append(e.getSrcTouchpointUrl());
			}

			if(StringUtil.isNotEmpty(e.getSrcTouchpointName())) {
				s.append(StringPool.COLON).append(e.getSrcTouchpointName().replaceAll(QUOTE, ""));
			}

			s.append(StringPool.SPACE).append(delimiter);
		}
		s.append(QUOTE);
		return s.toString();
	}
}
