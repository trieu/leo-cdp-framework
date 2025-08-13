package leotech.ssp.adserver.model;

public class AdStatus {
	public static final int ADSTATUS_PAUSED = 0;// paused ad by bad data
	public static final int ADSTATUS_PENDING = 1;// valid ad, waiting for
													// approval
	public static final int ADSTATUS_RUNNING = 2;// deliverable ad unit
	public static final int ADSTATUS_OVERBOOKING_TOTAL = 3;// the ad performance
															// metrics is over,
															// the goal is
															// reached
	public static final int ADSTATUS_EXPIRED = 4;// expiration
	public static final int ADSTATUS_OVERBOOKING_DAILY = 5;// the daily goal is
															// reached
	public static final int ADSTATUS_OVERBOOKING_HOURLY = 6;// the hourly goal
															// is reached
	public static final int ADSTATUS_OVERBOOKING_FLIGHT = 7;// the goal of
															// flight is reached
	public static final int ADSTATUS_OVER_USER_BUDGET = 8;// the ad performance
															// metrics is over,
															// the goal is
															// reached
}
