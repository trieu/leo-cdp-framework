package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import leotech.cdp.dao.EventMetricDaoUtil;
import leotech.cdp.domain.cache.EventMetricCache;
import leotech.cdp.domain.comparators.EventMetricComparators;
import leotech.cdp.domain.schema.JourneyFlowSchema;
import leotech.cdp.model.journey.EventMetric;

/**
 * Manages EventMetric metadata with in-memory caching for high-throughput data processing. <br>
 * <br>
 * Admin (refresh == true) always bypasses cache and fetches fresh data from DAO. <br>
 * Runtime (refresh == false) always uses cached values for performance. <br>
 *<br>
 * This class also schedules automatic refresh every 3 minutes to ensure CDP nodes 
 * stay consistent without requiring manual reload. <br>
 *<br>
 * @author: tantrieuf31 <br>
 * @since 2020
 */
public class EventMetricManagement {

    /** Global thread-safe cache for all flows */
    private static final Map<String, EventMetricCache> cacheMap = new ConcurrentHashMap<>();

    /** Scheduled executor for periodic auto-refresh */
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Predefined built-in event types */
    public static final EventMetric UNCLASSIFIED_EVENT = createUnclassifiedEvent();
    public static final EventMetric RECOMMENDATION = createRecommendationEvent();

    /** Static initializer */
    static {
        // Initial load (fresh)
        loadCache(true);

        // Auto refresh every 3 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshAllFlows();
            } catch (Exception ex) {
                ex.printStackTrace(); // avoid silent failures
            }
        }, 3, 3, TimeUnit.MINUTES);
    }

    // -------------------------------------------------------------------
    // Factory for built-in events
    // -------------------------------------------------------------------

    private static EventMetric createUnclassifiedEvent() {
        return new EventMetric(
            JourneyFlowSchema.STANDARD_EVENT_FLOW,
            "unclassified-event",
            "Unclassified Event",
            0,
            EventMetric.NO_SCORING,
            EventMetric.FIRST_PARTY_DATA,
            DataFlowManagement.CUSTOMER_PROFILE_FUNNEL_STAGE.getId(),
            EventMetric.JOURNEY_STAGE_AWARENESS
        );
    }

    private static EventMetric createRecommendationEvent() {
        return new EventMetric(
            JourneyFlowSchema.STANDARD_EVENT_FLOW,
            "recommend",
            "Recommend",
            0,
            EventMetric.NO_SCORING,
            EventMetric.FIRST_PARTY_DATA,
            "prospect",
            EventMetric.JOURNEY_STAGE_AWARENESS
        );
    }

    // -------------------------------------------------------------------
    // Cache Loaders
    // -------------------------------------------------------------------

    /**
     * Loads GENERAL_BUSINESS_EVENT flow cache.
     *
     * @param refresh true = always reload from database (Admin UI)
     * @return cache instance
     */
    public static EventMetricCache loadCache(boolean refresh) {
        return loadEventMetricCache(JourneyFlowSchema.GENERAL_BUSINESS_EVENT, refresh);
    }

    public static EventMetricCache loadCache() {
        return loadCache(false);
    }

    /**
     * Loads EventMetric cache for a given flow.
     *
     * refresh == true → DO NOT use cache, ALWAYS load from DAO.  
     * refresh == false → return cached version (runtime).
     */
    public static EventMetricCache loadEventMetricCache(String flowName, boolean refresh) {

        // Admin UI always requires a fresh snapshot
        if (refresh) {
            List<EventMetric> freshData = EventMetricDaoUtil.getEventMetricsByFlowName(flowName);
            EventMetricCache newCache = new EventMetricCache();
            newCache.putAll(freshData);
            cacheMap.put(flowName, newCache);
            return newCache;
        }

        // Runtime: use cached version
        return cacheMap.computeIfAbsent(flowName, name -> {
            // Load from DB on first use
            List<EventMetric> metrics = EventMetricDaoUtil.getEventMetricsByFlowName(flowName);
            EventMetricCache c = new EventMetricCache();
            c.putAll(metrics);
            return c;
        });
    }

    // -------------------------------------------------------------------
    // CRUD Operations
    // -------------------------------------------------------------------

    /**
     * Save & update cache.
     */
    public static String save(EventMetric eventMetric) {
        String id = EventMetricDaoUtil.save(eventMetric, true);

        // Update cache for the corresponding flow
        EventMetricCache cache = loadEventMetricCache(eventMetric.getFlowName(), false);
        cache.put(eventMetric.getEventName(), eventMetric);

        return id;
    }

    public static EventMetric getEventMetricByName(String eventName) {
        if (eventName == null) return UNCLASSIFIED_EVENT;
        return loadCache().get(eventName).orElse(UNCLASSIFIED_EVENT);
    }

    public static List<EventMetric> getAllSortedEventMetrics(boolean refresh) {
        List<EventMetric> list = new ArrayList<>(loadCache(refresh).getAll());
        Collections.sort(list, EventMetricComparators.SCORE_COMPARATOR);
        return list;
    }

    public static List<EventMetric> getAllSortedEventMetrics() {
        return getAllSortedEventMetrics(false);
    }

    /**
     * Delete from DB first → then remove from cache.
     */
    public static String delete(String eventName) {
        Optional<EventMetric> opt = loadCache().get(eventName);
        if (opt.isPresent()) {
            EventMetricDaoUtil.delete(opt.get());
            loadCache().remove(eventName);
        }
        return eventName;
    }

    public static int getCacheSize() {
        return loadCache().size();
    }

    /**
     * Fully clear all caches.
     */
    public static void clearAll() {
        cacheMap.values().forEach(EventMetricCache::clear);
        cacheMap.clear();
    }

    // -------------------------------------------------------------------
    // Auto Refresh (every 3 minutes)
    // -------------------------------------------------------------------

    /**
     * Reloads all flow caches (used by scheduler).
     */
    private static void refreshAllFlows() {
        for (String flowName : cacheMap.keySet()) {
            List<EventMetric> metrics = EventMetricDaoUtil.getEventMetricsByFlowName(flowName);

            EventMetricCache newCache = new EventMetricCache();
            newCache.putAll(metrics);

            // Atomic swap
            cacheMap.put(flowName, newCache);
        }
    }
}