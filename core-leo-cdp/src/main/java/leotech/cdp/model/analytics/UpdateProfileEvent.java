package leotech.cdp.model.analytics;

import com.google.gson.Gson;

import leotech.cdp.model.customer.Device;
import rfx.core.util.StringUtil;

/**
 * The trigger for updating profile data in real-time
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class UpdateProfileEvent {

	String sessionKey;
	String profileId;
	String observerId;
	String touchpointRefDomain;
	String deviceId;
	TrackingEvent event;
	FeedbackEvent feedbackEvent;

	public UpdateProfileEvent(String deviceId, String sessionKey, String profileId, String observerId, String touchpointRefDomain, TrackingEvent event, FeedbackEvent feedbackEvent) {
		super();
		this.deviceId = deviceId;
		this.sessionKey = sessionKey;
		this.profileId = profileId;
		this.observerId = observerId;
		this.touchpointRefDomain = touchpointRefDomain;
		this.event = event;
		this.feedbackEvent = feedbackEvent;
	}
	
	public UpdateProfileEvent(String profileId, String observerId, String touchpointRefDomain, TrackingEvent event, FeedbackEvent feedbackEvent) {
		super();
		this.deviceId = Device.CDP_API_DEVICE.getId();
		this.profileId = profileId;
		this.observerId = observerId;
		this.touchpointRefDomain = touchpointRefDomain;
		this.event = event;
		this.feedbackEvent = feedbackEvent;
	}
	
	public UpdateProfileEvent(String deviceId, TrackingEvent e, FeedbackEvent fbe) {
		super();
		this.deviceId = deviceId;
		this.profileId = e.getRefProfileId();
		this.observerId = e.getObserverId();
		this.touchpointRefDomain = e.getRefTouchpointHost();
		this.event = e;
		this.feedbackEvent = fbe;
	}
	
	public String key() {
		return event.getId();
	}
	
	public String value() {
		return new Gson().toJson(this);
	}

	public FeedbackEvent getFeedbackEvent() {
		return feedbackEvent;
	}

	public void setFeedbackEvent(FeedbackEvent feedbackEvent) {
		this.feedbackEvent = feedbackEvent;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public String getObserverId() {
		return StringUtil.safeString(observerId);
	}

	public void setObserverId(String observerId) {
		this.observerId = observerId;
	}

	public String getTouchpointRefDomain() {
		return touchpointRefDomain;
	}

	public void setTouchpointRefDomain(String touchpointRefDomain) {
		this.touchpointRefDomain = touchpointRefDomain;
	}
	
	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public TrackingEvent getEvent() {
		return event;
	}

	public void setEvent(TrackingEvent event) {
		this.event = event;
	}
	

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	public boolean isFeedbackEvent() {
		return feedbackEvent != null;
	}
}
