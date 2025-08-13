package leotech.cdp.model.marketing;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileModelUtil;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class FlowFacts {

	private Map<String, Object> contextMap = new HashMap<>();
	private FlowContext context = new FlowContext();

	public FlowFacts() {
		contextMap.put(AutomatedFlow.STR_CONTEXT, context);
	}

	public Map<String, Object> getContextMap() {
		return contextMap;
	}

	public void setContextMap(Map<String, Object> contextMap) {
		this.contextMap = contextMap;
	}
	
	public void setFact(String key, Object object) {
		this.contextMap.put(key, object);
	}

	public FlowContext getContext() {
		return context;
	}

	public void setContext(FlowContext context) {
		this.context = context;
	}

	public static FlowFacts buildFacts() {
		FlowFacts f = new FlowFacts();
		return f;
	}
	
	public static FlowFacts buildFacts(Profile profile) {
		FlowFacts f = new FlowFacts();
		f.setProfile(profile);
		return f;
	}

	public void setProfile(Profile profile) {
		this.context.setProfile(profile);
	}
	
	public void updateDateTimeContextMap() {
		Date systemDateTime = new Date();
		this.contextMap.put("systemDateTime", systemDateTime);
		this.contextMap.put("currentDate", ProfileModelUtil.formatDateInYYYY_MM_DD(systemDateTime));
		
	}
	
	public void setResult(Object result) {
		this.context.setResult(result);
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
