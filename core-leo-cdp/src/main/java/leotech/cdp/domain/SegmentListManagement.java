package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.query.filters.SegmentFilter;

/**
 * Segment List Management
 * 
 * @author tantrieuf31
 * @since 2025
 *
 */
public class SegmentListManagement {

	final private static int CACHE_TIME = 60;
	
	static CacheLoader<SegmentFilter, List<Segment>> cacheLoaderSegments = new CacheLoader<>() {
		@Override
		public  List<Segment> load(SegmentFilter filter) {
			if(filter.isGetAll()) {
				System.out.println("in cache call SegmentDaoUtil.getAllActiveSegments");
				return SegmentDaoUtil.getAllActiveSegments();
			}
			return new ArrayList<Segment>(0);
		}
	};

	static final LoadingCache<SegmentFilter, List<Segment>> cacheSegments = CacheBuilder.newBuilder().maximumSize(20000)
			.expireAfterWrite(CACHE_TIME, TimeUnit.SECONDS).build(cacheLoaderSegments);
	
	public static void refreshCacheSegments(SegmentFilter filter) {
		cacheSegments.refresh(filter);
	}
	
	public static void clearAllCacheSegments() {
		cacheSegments.invalidateAll();
	}
	
	public static List<Segment> getAllActiveSegments() {
		SegmentFilter filter = new SegmentFilter();
		filter.setLength(Integer.MAX_VALUE);
		List<Segment> list = null;
		try {
			list = cacheSegments.get(filter);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(list == null) {
			list = SegmentDaoUtil.getAllActiveSegments();
		}
		return list;
	}
}
