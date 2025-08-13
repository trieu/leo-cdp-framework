package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import leotech.cdp.dao.DataFlowStageDaoUtil;
import leotech.cdp.domain.schema.JourneyFlowSchema;
import leotech.cdp.model.journey.DataFlowStage;
import leotech.cdp.model.journey.FlowType;
import rfx.core.util.StringUtil;

/**
 * Metadata for event funnel flow and customer funnel flow
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class DataFlowManagement {

	public static final String MEDIA_EVENT_FLOW = "media_event_flow";
	public static final String TRAVEL_EVENT_FLOW = "travel_event_flow";

	// TODO move to database in Enterprise version

	public static final DataFlowStage CUSTOMER_PROFILE_FUNNEL_STAGE = new DataFlowStage(0, "Customer Profile", "",
			"default_data_flow", FlowType.SYSTEM_METRIC);

	// default data flow stage for new visitor
	static final DataFlowStage newVisitorStage;

	// default data flow stage for new visitor
	static final DataFlowStage returningVisitorStage;

	// default data flow stage for customer lead
	static final DataFlowStage leadStage;

	static final List<DataFlowStage> dataFunnelStages = new ArrayList<DataFlowStage>();

	static final Map<String, DataFlowStage> dataFlowCache = new ConcurrentHashMap<String, DataFlowStage>(100);
	static final Map<Integer, DataFlowStage> orderIndex = new ConcurrentHashMap<Integer, DataFlowStage>(100);

	static {
		newVisitorStage = DataFlowStageDaoUtil.getDataFlowStageById("new-visitor");
		returningVisitorStage = DataFlowStageDaoUtil.getDataFlowStageById("returning-visitor");
		leadStage = DataFlowStageDaoUtil.getDataFlowStageById("lead");

		// funnel data flow
		updateFunnelDataCache();
	}

	public static void updateFunnelDataCache() {
		List<DataFlowStage> dataFlowStages = DataFlowStageDaoUtil.getDataFlowStages(JourneyFlowSchema.STANDARD_CUSTOMER_FLOW);
		dataFunnelStages.addAll(dataFlowStages);
		for (DataFlowStage funnelStage : dataFunnelStages) {
			dataFlowCache.put(funnelStage.getId(), funnelStage);
			orderIndex.put(funnelStage.getOrderIndex(), funnelStage);
		}
	}

	public static List<DataFlowStage> getCustomerFunnelStages() {
		return dataFunnelStages;
	}

	public static Map<String, DataFlowStage> getDataFlowCache() {
		return dataFlowCache;
	}

	public static DataFlowStage getFunnelStageById(String id) {
		if (StringUtil.isEmpty(id)) {
			return newVisitorStage;
		}
		return dataFlowCache.getOrDefault(id, newVisitorStage);
	}

	public static DataFlowStage getFunnelStageByOrderIndex(int funnelIndex) {
		if (funnelIndex == 0) {
			return newVisitorStage;
		}
		return orderIndex.getOrDefault(funnelIndex, newVisitorStage);
	}

	public static DataFlowStage getNewVisitorStage() {
		return newVisitorStage;
	}

	public static DataFlowStage getReturningVisitorStage() {
		return returningVisitorStage;
	}

	public static DataFlowStage getLeadStage() {
		return leadStage;
	}
}
