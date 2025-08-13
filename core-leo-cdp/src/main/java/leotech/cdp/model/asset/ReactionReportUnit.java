package leotech.cdp.model.asset;

import java.util.Date;

import com.google.gson.annotations.Expose;

/**
 * 
 * the feedback reaction for a specific items
 * 
 * @author tantrieuf31
 * @since 2023
 *
 */
public final class ReactionReportUnit {

	@Expose
	Date createdAt;
	
	@Expose
	String eventName;
	
	@Expose
	int count;

	public ReactionReportUnit() {

	}

	public ReactionReportUnit(String eventName, int count) {
		super();
		this.eventName = eventName;
		this.count = count;
		this.createdAt = new Date();
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}


	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

}
