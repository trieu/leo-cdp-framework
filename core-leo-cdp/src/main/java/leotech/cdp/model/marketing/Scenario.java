package leotech.cdp.model.marketing;

import java.io.Serializable;

import com.google.gson.annotations.Expose;

/**
 * Scenario
 *
 * Represents a decision scenario consumed by an Agentic AI Agent to choose an
 * action and execute it via a marketing channel (e.g. Zalo, Email).
 */
public class Scenario implements Serializable {

	private static final long serialVersionUID = 1L;

	@Expose
	private long id;

	@Expose
	private String description;

	/**
	 * Logical decision key (used by agent) e.g. "coupon", "reminder", "upsell"
	 */
	@Expose
	private String decision;

	/**
	 * Human-readable label for UI / logs e.g. "Send 10% Coupon"
	 */
	@Expose
	private String decisionLabel;

	/**
	 * Execution channel for the agent e.g. "zalo", "email", "sms"
	 */
	@Expose
	private String channel;

	/**
	 * Reasoning / explanation used by Agentic AI for traceability and audit
	 */
	@Expose
	private String reasoning;

	// =========================
	// Constructors
	// =========================
	public Scenario() {
		// for JSON deserialization
	}

	public Scenario(long id, String description, String decision, String decisionLabel, String channel,
			String reasoning) {
		this.id = id;
		this.description = description;
		this.decision = decision;
		this.decisionLabel = decisionLabel;
		this.channel = channel;
		this.reasoning = reasoning;
	}

	// =========================
	// Getters & Setters
	// =========================
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDecision() {
		return decision;
	}

	public void setDecision(String decision) {
		this.decision = decision;
	}

	public String getDecisionLabel() {
		return decisionLabel;
	}

	public void setDecisionLabel(String decisionLabel) {
		this.decisionLabel = decisionLabel;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getReasoning() {
		return reasoning;
	}

	public void setReasoning(String reasoning) {
		this.reasoning = reasoning;
	}
}
