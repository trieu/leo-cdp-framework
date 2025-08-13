package leotech.cdp.model.customer;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import leotech.system.util.ReflectionUtil;

public final class ProfileType {
	
	public final static String UNKNOWN = "unknown";
	
	// no direct contact data 
	public final static int ANONYMOUS_VISITOR = 0;
	
	// these profile types must have contact information
	public final static int LOGIN_USER_CONTACT = 1;
	public final static int CUSTOMER_CONTACT = 2;
	public final static int STUDENT_CONTACT = 3;
	
	public final static int CRM_IMPORTED_CONTACT = 4;
	public final static int DIRECT_INPUT_CONTACT = 5;
	
	public final static int INFLUENCER_CONTACT = 6;
	public final static int CLIENT_CONTACT = 7;
	public final static int B2B_PARTNER_CONTACT = 8;
	
	public final static int EMPLOYEE_CONTACT = 9;
	public final static int KEY_ACCOUNT_CONTACT = 10;
	
	// System
	public final static int SYSTEM_USER_CONTACT = 11;
	
	final static Map<Integer, String> mapValueToName = new HashMap<>(10);
	final static Map<String, Integer> mapNameToValue = new HashMap<>(10);
	
	static {
		mapValueToName.putAll(ReflectionUtil.getConstantMap(ProfileType.class));
		mapNameToValue.putAll(ReflectionUtil.getConstantIntegerMap(ProfileType.class));
	}
	
	public static Map<Integer, String> getMapValueToName() {
		return mapValueToName;
	}
	
	
	public static String getTypeAsText(int type) {
		return mapValueToName.getOrDefault(type, "");
	}
	
	public static int getTypeAsInt(String type) {
		return mapNameToValue.getOrDefault(type, -1);
	}
	
	public static String getMapValueToNameAsJson() {
		return new Gson().toJson(mapValueToName);
	}
	
}
