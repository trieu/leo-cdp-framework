package leotech.cdp.model.activation.decision;

/**
 * CommonDecision
 *
 * Cross-industry decisions shared by all Agentic AI flows.
 */
public final class CommonDecision {

    private CommonDecision() {}

    public static final String DO_NOTHING = "do_nothing";
    public static final String SUPPRESS = "suppress";
    public static final String HUMAN_HANDOFF = "human_handoff";

    public static final String REMINDER = "reminder";
    public static final String FOLLOW_UP = "follow_up";
    public static final String PAYMENT_NUDGE = "payment_nudge";

    public static final String EDUCATE = "educate";
    public static final String FEEDBACK_REQUEST = "feedback_request";
}
