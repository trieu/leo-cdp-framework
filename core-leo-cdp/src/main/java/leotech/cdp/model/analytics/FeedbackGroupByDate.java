package leotech.cdp.model.analytics;

import java.util.Date;
import java.util.List;

/**
 * the data reporting container to group all feedback events by dateKey
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class FeedbackGroupByDate {

	Date date;
	String dateKey;
	List<FeedbackEvent> feedbackEvents;

	public FeedbackGroupByDate() {

	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getDateKey() {
		return dateKey;
	}

	public void setDateKey(String dateKey) {
		this.dateKey = dateKey;
	}

	public List<FeedbackEvent> getFeedbackEvents() {
		return feedbackEvents;
	}

	public void setFeedbackEvents(List<FeedbackEvent> feedbackEvents) {
		this.feedbackEvents = feedbackEvents;
	}

}
