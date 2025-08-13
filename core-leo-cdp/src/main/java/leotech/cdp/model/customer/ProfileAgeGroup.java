package leotech.cdp.model.customer;

import java.util.HashMap;
import java.util.Map;

import leotech.system.util.ReflectionUtil;

/**
 * 	
 *	Age group: [1 => 6 - 10], [2 => 11 - 17],  [3 => 18 - 24], [4 => 25 - 34], [5 => 35 - 44], [6 => 45 - 54], [7 => 55 - 64], [8 => 65 - 74], [9 => 75+ ]
 *	
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class ProfileAgeGroup {
	
	public static final String NO_DATA_STR = "NO_DATA";

	public static final int NO_DATA = 0;
	
	public static final int AGE_GROUP_1_10 = 1;

	public static final int AGE_GROUP_11_17 = 2;

	public static final int AGE_GROUP_18_24 = 3;

	public static final int AGE_GROUP_25_34 = 4;

	public static final int AGE_GROUP_35_44 = 5;

	public static final int AGE_GROUP_45_54 = 6;

	public static final int AGE_GROUP_55_64 = 7;
	
	public static final int AGE_GROUP_65_74 = 8;
	
	public static final int AGE_GROUP_75_120 = 9;
	
	final static Map<Integer, String> mapValueToName = new HashMap<>(10);
	final static Map<String, Integer> mapNameToValue = new HashMap<>(10);
	
	static {
		mapValueToName.putAll(ReflectionUtil.getConstantMap(ProfileAgeGroup.class));
		mapNameToValue.putAll(ReflectionUtil.getConstantIntegerMap(ProfileAgeGroup.class));
	}
	
	public static Map<Integer, String> getMapValueToName() {
		return mapValueToName;
	}
	
	public static String getAsLabelString(int ageGroup) {
		return mapValueToName.getOrDefault(ageGroup, NO_DATA_STR);
	}
	
	public static String getAsLabelString(int ageGroup, String df) {
		return mapValueToName.getOrDefault(ageGroup, df);
	}
	
	public static int getValueAsInt(String ageGroupStr) {
		return mapNameToValue.getOrDefault(ageGroupStr, NO_DATA);
	}
	
	public static int convertAgeNumberToAgeGroup(int age) {
		if(age > 0 && age <= 10) {
			return AGE_GROUP_1_10;
		}
		else if(age >= 11 && age <= 17) {
			return AGE_GROUP_11_17;
		}
		else if(age >= 18 && age <= 24) {
			return AGE_GROUP_18_24;
		}
		else if(age >= 25 && age <= 34) {
			return AGE_GROUP_25_34;
		}
		else if(age >= 35 && age <= 44) {
			return AGE_GROUP_35_44;
		}
		else if(age >= 45 && age <= 54) {
			return AGE_GROUP_45_54;
		}
		else if(age >= 55 && age <= 64) {
			return AGE_GROUP_55_64;
		}
		else if(age >= 65 && age <= 74) {
			return AGE_GROUP_65_74;
		}
		else if(age >= 75) {
			return AGE_GROUP_75_120;
		}
		else {
			return NO_DATA;
		}
	}
	
}
