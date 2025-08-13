package leotech.cdp.model.analytics;

/**
 * @author thomas
 *
 */
public abstract class ImportingEventCallback {
	protected TrackingEvent event = null;
	public ImportingEventCallback() {
	}
	public void setUpdateProfileEvent(TrackingEvent event) {
		this.event = event;
	}
	abstract public TrackingEvent apply();
}
