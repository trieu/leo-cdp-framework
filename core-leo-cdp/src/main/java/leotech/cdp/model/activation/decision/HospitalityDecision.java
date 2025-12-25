package leotech.cdp.model.activation.decision;

/**
 * HospitalityDecision
 *
 * Decisions for Hospitality / Travel domain.
 */
public final class HospitalityDecision {

    private HospitalityDecision() {}

    public static final String BOOKING_REMINDER = "booking_reminder";
    public static final String CHECKIN_REMINDER = "checkin_reminder";

    public static final String ROOM_UPGRADE = "room_upgrade";
    public static final String EXPERIENCE_UPSELL = "experience_upsell";

    public static final String POST_STAY_FEEDBACK = "post_stay_feedback";
}
