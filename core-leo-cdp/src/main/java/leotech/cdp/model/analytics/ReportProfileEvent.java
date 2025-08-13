package leotech.cdp.model.analytics;

/**
 * The report counting object for touchpoint and event observer
 * 
 * @author tantrieuf31
 * @since 2022
 */
public class ReportProfileEvent {

	protected long profileCount = 0;
	protected long eventCount = 0;

	public ReportProfileEvent() {
		super();
	}

	public long getProfileCount() {
		return profileCount;
	}
	
	public long getProfileCount(long defaultValue) {
		return profileCount > 0 ? profileCount : defaultValue;
	}

	public void setProfileCount(long profileCount) {
		this.profileCount = profileCount;
	}
	
	public void updateProfileCount(long profileCount) {
		this.profileCount += profileCount;
	}
	
	public void updateProfileCount(ReportProfileEvent e) {
		this.profileCount += e.getProfileCount();
	}

	public long getEventCount() {
		return eventCount;
	}
	
	public long getEventCount(long defaultValue) {
		return eventCount > 0 ? eventCount : defaultValue;
	}

	public void setEventCount(long eventCount) {
		this.eventCount = eventCount;
	}
	
	public void updateEventCount(long eventCount) {
		this.eventCount += eventCount;
	}

}