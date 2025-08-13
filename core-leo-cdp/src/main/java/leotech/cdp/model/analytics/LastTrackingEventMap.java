package leotech.cdp.model.analytics;

import java.util.HashMap;
import java.util.Map;

/**
 * to collect last events of profile by specified metrics: item-view, purchase
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class LastTrackingEventMap {
	
	private TrackingEvent lastEvent = null;
    private Map<String, TrackingEvent> events;
    
    public LastTrackingEventMap() {
		// gson
	}
    
    public static LastTrackingEventMap emptyMap() {
    	LastTrackingEventMap map = new LastTrackingEventMap();
    	map.events = new HashMap<String, TrackingEvent>();
    	return map;
    }

    // Constructor
    public LastTrackingEventMap(Map<String, TrackingEvent> events) {
        this.events = events;
    }

    // Getter for events
    public Map<String, TrackingEvent> getEvents() {
        return events;
    }

    // Setter for events
    public void setEvents(Map<String, TrackingEvent> events) {
        this.events = events;
    }
    
    public TrackingEvent getLastEvent() {
		return lastEvent;
	}

	public void setLastEvent(TrackingEvent lastEvent) {
		this.lastEvent = lastEvent;
	}

	// Example method to get a specific TrackingEvent by metricName
    public TrackingEvent getEventByMetricName(String metricName) {
        return events.get(metricName);
    }

    @Override
    public String toString() {
        return "LastTrackingEventMap{" +
                "events=" + events +
                '}';
    }
}
