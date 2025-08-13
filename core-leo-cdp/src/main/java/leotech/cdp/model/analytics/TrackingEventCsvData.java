package leotech.cdp.model.analytics;

import java.time.Instant;
import java.util.Date;

import com.google.gson.annotations.Expose;

import leotech.system.exception.InvalidDataException;
import rfx.core.util.StringUtil;

/**
 * The data model for Tracking Event in CSV format
 * 
 * @author tantrieuf31
 * @since 2022
 */
public class TrackingEventCsvData {
	
	public static final int EVENT_CSV_DATA_LENGHT = 35;

	@Expose
	String profileId ;
	
	@Expose
	String crmRefId ;
	
	@Expose
	String primaryEmail ;
	
	@Expose
	String primaryPhone ;
	
	@Expose
	String observerId ;
	
	@Expose
	String journeyMapId ;
	
	@Expose
	int fraudScore = 0;
	
	@Expose
	String eventTimeStr;
	
	@Expose
	String eventName;
	
	@Expose
	long eventValue ;
	
	@Expose
	String message;
	
	@Expose
	Date eventTime;
	
	public TrackingEventCsvData(String[] dataRow) {
		if(dataRow.length == EVENT_CSV_DATA_LENGHT) {
			this.profileId = StringUtil.safeString(dataRow[0], "_");
			this.crmRefId = StringUtil.safeString(dataRow[1], "_");
			this.primaryEmail = StringUtil.safeString(dataRow[2], "_");
			this.primaryPhone = StringUtil.safeString(dataRow[3], "_");
			
			this.observerId = StringUtil.safeString(dataRow[4]);
			this.journeyMapId =  StringUtil.safeString(dataRow[5]);
			this.fraudScore =  StringUtil.safeParseInt(dataRow[6]);
			this.eventTimeStr = dataRow[7];
			this.eventName = dataRow[8];
			this.eventValue = StringUtil.safeParseLong(dataRow[9]);
			this.message = dataRow[10];
			
			this.eventTime = Date.from(Instant.parse(eventTimeStr));
			// skip, just show first 11 collumn
		}
		else {
			throw new InvalidDataException("The number of column must be " + EVENT_CSV_DATA_LENGHT);
		}
	}
	
	
}
