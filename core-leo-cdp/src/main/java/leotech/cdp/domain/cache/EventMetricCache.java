package leotech.cdp.domain.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import leotech.cdp.model.journey.EventMetric;

public class EventMetricCache {

	private final Map<String, EventMetric> cache;

	public void invalidateAll() {
		this.cache.clear();
		AdminRedisCacheUtil.clearCacheAllObservers();
	}

	public void invalidate(String cacheKey) {
		// Implement specific cache invalidation if needed
		this.cache.remove(cacheKey);
		AdminRedisCacheUtil.clearCacheAllObservers();
	}

	public EventMetricCache() {
		this.cache = new ConcurrentHashMap<>(1000);
	}

	public EventMetricCache(int initialCapacity) {
		this.cache = new ConcurrentHashMap<>(initialCapacity);
	}

	public EventMetricCache(List<EventMetric> eventMetrics) {
		this.cache = new ConcurrentHashMap<>(eventMetrics.size());
		this.putAll(eventMetrics);
	}

	public Optional<EventMetric> get(String eventName) {
		return Optional.ofNullable(cache.get(eventName));
	}

	public void put(String eventName, EventMetric metric) {
		cache.put(eventName, metric);
	}

	public void putAll(Collection<EventMetric> metrics) {
		for (EventMetric metric : metrics) {
			cache.put(metric.getEventName(), metric);
		}
	}

	public Optional<EventMetric> remove(String eventName) {
		return Optional.ofNullable(cache.remove(eventName));
	}

	public Collection<EventMetric> getAll() {
		return cache.values();
	}

	public int size() {
		return cache.size();
	}

	public void clear() {
		cache.clear();
	}

	public boolean contains(String eventName) {
		return cache.containsKey(eventName);
	}
}