package leotech.cdp.query.filters;

import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.system.model.SystemUser;
import rfx.core.util.StringUtil;

/**
 * to filter segments in database
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class SegmentFilter extends DataFilter {

	/**
	 * auto managed by LEO
	 */
	@Expose
	boolean managedBySystem = false;

	/**
	 * to apply sophisticated data processing techniques to yield more information
	 * and customer insights
	 */
	@Expose
	boolean forDeepAnalytics = false;

	/**
	 * for prediction LTV or important metrics
	 */
	@Expose
	boolean forPredictiveAnalytics = false;

	/**
	 * moment of truth connection
	 */
	@Expose
	boolean forPersonalization = false;

	/**
	 * passive connection
	 */
	@Expose
	boolean forEmailMarketing = false;

	/**
	 * pro-active connection
	 */
	@Expose
	boolean forRealtimeMarketing = false;

	@Expose
	boolean forReTargeting; // 6

	@Expose
	boolean forLookalikeTargeting; // 7
	

	public SegmentFilter() {
		this.start = 0;
		this.length = 0;
	}

	public boolean isFilteringAndGetAll() {
		return start == 0 && length == 0 && StringUtil.isEmpty(uri);
	}

	public SegmentFilter(SystemUser systemUser, String uri, JsonObject paramJson) {
		super(systemUser, uri, paramJson);
	}
	
	public SegmentFilter(SystemUser systemUser, String uri, MultiMap map) {
		super(systemUser, uri, map);
	}

	public boolean isManagedBySystem() {
		return managedBySystem;
	}

	public void setManagedBySystem(boolean managedBySystem) {
		this.managedBySystem = managedBySystem;
	}

	public boolean isForDeepAnalytics() {
		return forDeepAnalytics;
	}

	public void setForDeepAnalytics(boolean forDeepAnalytics) {
		this.forDeepAnalytics = forDeepAnalytics;
	}

	public boolean isForPredictiveAnalytics() {
		return forPredictiveAnalytics;
	}

	public void setForPredictiveAnalytics(boolean forPredictiveAnalytics) {
		this.forPredictiveAnalytics = forPredictiveAnalytics;
	}

	public boolean isForPersonalization() {
		return forPersonalization;
	}

	public void setForPersonalization(boolean forPersonalization) {
		this.forPersonalization = forPersonalization;
	}

	public boolean isForEmailMarketing() {
		return forEmailMarketing;
	}

	public void setForEmailMarketing(boolean forEmailMarketing) {
		this.forEmailMarketing = forEmailMarketing;
	}

	public boolean isForRealtimeMarketing() {
		return forRealtimeMarketing;
	}

	public void setForRealtimeMarketing(boolean forRealtimeMarketing) {
		this.forRealtimeMarketing = forRealtimeMarketing;
	}

	public boolean isForReTargeting() {
		return forReTargeting;
	}

	public void setForReTargeting(boolean forReTargeting) {
		this.forReTargeting = forReTargeting;
	}

	public boolean isForLookalikeTargeting() {
		return forLookalikeTargeting;
	}

	public void setForLookalikeTargeting(boolean forLookalikeTargeting) {
		this.forLookalikeTargeting = forLookalikeTargeting;
	}
	
	
	
	@Override
	public int hashCode() {
		return Objects.hash(new Gson().toJson(this));
	}
	
	@Override
	public boolean equals(Object obj) {
		return this.toString().equals(obj.toString());
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
