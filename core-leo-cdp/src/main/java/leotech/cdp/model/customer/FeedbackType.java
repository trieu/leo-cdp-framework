package leotech.cdp.model.customer;

import java.util.HashMap;
import java.util.Map;

import leotech.system.util.ReflectionUtil;

/**
 * type of forms to collect feedback data for CX improvement
 * 
 * @author tantrieuf31
 * 
 */
public final class FeedbackType {
	
	final static Map<Integer, String> mapValueToName = new HashMap<>(6);
	final static Map<String, Integer> mapNameToValue = new HashMap<>(6);

	public static final int SURVEY = 0;
	public static final int CONTACT = 1;
	public static final int RATING = 2;
	public static final int CES = 3;
	public static final int CSAT = 4;
	public static final int NPS = 5;
	
	static {
		mapValueToName.putAll(ReflectionUtil.getConstantMap(FeedbackType.class));
		mapNameToValue.putAll(ReflectionUtil.getConstantIntegerMap(FeedbackType.class));
	}
	
	public static Map<Integer, String> getMapValueToName() {
		return mapValueToName;
	}
	
	public static String getFeedbackTypeAsText(int type) {
		return mapValueToName.getOrDefault(type, "RATING");
	}
	
	public static int getFeedbackTypeAsInt(String type) {
		return mapNameToValue.getOrDefault(type, -1);
	}
}