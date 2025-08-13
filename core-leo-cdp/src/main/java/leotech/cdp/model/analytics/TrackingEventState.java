package leotech.cdp.model.analytics;

/**
 * the state of TrackingEvent (cdp_trackingevent).
 * Purpose: data recomputation or data archive 
 * 
 * @author Trieu (trieu@leocdp.com)
 * @since 2022
 *
 */
public final class TrackingEventState {

	public static final int STATE_RAW_DATA = 0;
	public static final int STATE_PROCESSED = 1;
	public static final int STATE_CHECKED = 2;
	public static final int STATE_ARCHIVED = -1;
}
