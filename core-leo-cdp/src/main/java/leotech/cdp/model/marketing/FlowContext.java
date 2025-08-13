package leotech.cdp.model.marketing;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.Gson;

import leotech.cdp.model.customer.Profile;

public class FlowContext {
	
	public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);
	
	Object result = "";
	Profile profile;
	
	public FlowContext() {
	}
	
	public FlowContext(Profile profile) {
		super();
		this.profile = profile;
	}
	
	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object conditionResult) {
		this.result = conditionResult;
	}
	
	public void loadProfilesInSegment(String segmentId) {
		System.out.println("loadProfilesInSegment " + segmentId);
	}
	
	public boolean checkConditionResult(String conditionResult) {
		return String.valueOf(result).equalsIgnoreCase(conditionResult);
	}
	
	public boolean isCurrentDateEqualProfileBirthday(String currentDate) {
		Date dateOfBirth = this.profile.getDateOfBirth();
		if(dateOfBirth != null && currentDate != null) {
			return currentDate.equals(DATE_FORMAT.format(dateOfBirth)); 
		}
		return false;
	}
	
	public void end() {
		System.out.println("END ...");
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}


}
