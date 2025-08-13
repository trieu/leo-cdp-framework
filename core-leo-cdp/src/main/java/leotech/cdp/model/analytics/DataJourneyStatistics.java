package leotech.cdp.model.analytics;

import java.util.List;

/**
 * The statistics container for 5As customer journey path
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public interface DataJourneyStatistics {
	public List<List<Integer>> getDataForFunnelGraph() ;
}
