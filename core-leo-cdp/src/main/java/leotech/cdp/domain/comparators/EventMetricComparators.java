package leotech.cdp.domain.comparators;

import java.util.Comparator;

import leotech.cdp.model.journey.EventMetric;

public class EventMetricComparators {
    
    public static final Comparator<EventMetric> SCORE_COMPARATOR = 
        Comparator.comparingInt(event -> Math.abs(event.getScore()));
    
    public static final Comparator<EventMetric> NAME_COMPARATOR = 
        Comparator.comparing(EventMetric::getEventName);
    
    public static final Comparator<EventMetric> STAGE_COMPARATOR = 
        Comparator.comparing(EventMetric::getJourneyStage);
    
    // Combined comparator: sort by score, then by name
    public static final Comparator<EventMetric> SCORE_THEN_NAME_COMPARATOR = 
        SCORE_COMPARATOR.thenComparing(NAME_COMPARATOR);
}