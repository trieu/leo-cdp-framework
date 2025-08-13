package leotech.cdp.model.customer;

import java.util.HashMap;
import java.util.Map;

import leotech.system.util.ReflectionUtil;

/**
 * Code defining the marital status of a person. <br>
 * Using standard code of U.S. Department of Health & Human Services <br>
 * https://ushik.ahrq.gov/ViewItemDetails?system=sdo&itemKey=133169000 <br> <br>
 * 
 * 1 Common Law <br>
 * 2 Registered Domestic Partner <br>
 * 3 Not Applicable <br>
 * 4 Divorced <br>
 * 5 Single <br>
 * 6 Unknown <br>
 * 7 Married <br>
 * 8 Unreported <br>
 * 9 Separated <br>
 * 10 Unmarried (Single or Divorced or Widowed)<br>
 * 11 Widowed <br>
 * 12 Legally Separated <br>
 */
public final class ProfileMaritalCode {
	
	public static final int Common_Law = 1;
	public static final int Registered_Domestic_Partner = 2;
	public static final int Not_Applicable = 3;
	public static final int Divorced = 4;
	public static final int Single = 5;
	public static final int Unknown = 6;
	public static final int Married = 7;
	public static final int Unreported = 8;
	public static final int Separated = 9;
	public static final int Unmarried = 10;
	public static final int Widowed = 11;
	public static final int Legally_Separated = 12;
	
	final static Map<Integer, String> mapValueToName = new HashMap<>(12);
	final static Map<String, Integer> mapNameToValue = new HashMap<>(12);
	
	static {
		mapValueToName.putAll(ReflectionUtil.getConstantMap(ProfileMaritalCode.class));
		mapNameToValue.putAll(ReflectionUtil.getConstantIntegerMap(ProfileMaritalCode.class));
	}
	
	public static String getMaritalCodeAsText(int code) {
		return mapValueToName.getOrDefault(code, "Unknown");
	}
	
	public static int getMaritalCodeAsInt(String code) {
		return mapNameToValue.getOrDefault(code, Unknown);
	}
}
