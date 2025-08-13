package leotech.cdp.model.customer;

import rfx.core.util.StringUtil;

/**
 * Profile Gender Code, valid value is from 0 to 7
 * 
 * @author tantrieuf31
 * @since 2022
 */
public final class ProfileGenderCode {
	
	public static final int FEMALE = 0;
	
	public static final int MALE = 1;
	
	public static final int UNKNOWN = 7;

	public static final String GENDER_TRANSGENDER = "transgender";

	public static final String GENDER_BISEXUAL = "bisexual";

	public static final String GENDER_GAY = "gay";

	public static final String GENDER_LESBIAN = "lesbian";

	public static final String GENDER_LGBT = "lgbt";

	public static final String GENDER_MALE = "male";

	public static final String GENDER_FEMALE = "female";
	
	public static final String GENDER_UNKNOWN = "unknown";
	

	
	public static final int getIntegerValue(String genderStr) {
		int genderInt = UNKNOWN;
		if(StringUtil.isEmpty(genderStr)) {
			return genderInt;
		}		
		String s = genderStr.toLowerCase().trim();
		if (GENDER_FEMALE.equals(s)) {
			genderInt = FEMALE;
		} 
		else if (GENDER_MALE.equals(s)) {
			genderInt = MALE;
		} 
		else if (GENDER_LGBT.equals(s)) {
			genderInt = 2;
		} 
		else if (GENDER_LESBIAN.equals(s)) {
			genderInt = 3;
		} 
		else if (GENDER_GAY.equals(s)) {
			genderInt = 4;
		}
		else if (GENDER_BISEXUAL.equals(s)) {
			genderInt = 5;
		}
		else if (GENDER_TRANSGENDER.equals(s)) {
			genderInt = 6;
		}
		return genderInt;
	}
	
	public static final String getStringValue(int genderInt) {
		String gender = GENDER_UNKNOWN;
		
		return gender;
	}
}
