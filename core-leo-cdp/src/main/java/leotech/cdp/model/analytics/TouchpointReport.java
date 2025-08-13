package leotech.cdp.model.analytics;

import leotech.cdp.model.journey.Touchpoint;

/**
 * The report object for touchpoint
 * 
 * @author tantrieuf31
 * @since 2022
 */
public final class TouchpointReport extends ReportProfileEvent {
	
	Touchpoint touchpoint = null;
	
	public TouchpointReport() {
	}

	public Touchpoint getTouchpoint() {
		return touchpoint;
	}

	public void setTouchpoint(Touchpoint touchpoint) {
		this.touchpoint = touchpoint;
	}

}
