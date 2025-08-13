package leotech.cdp.domain.schema;

import static leotech.cdp.domain.schema.JourneyFlowSchema.GENERAL_DATA_FUNNEL;
import static leotech.cdp.domain.schema.JourneyFlowSchema.STANDARD_CUSTOMER_FLOW;

import java.util.HashMap;
import java.util.Map;

import leotech.cdp.model.journey.DataFlowStage;
import leotech.cdp.model.journey.FlowType;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * Default Data Funnel for all business
 * 
 * @author tantrieuf31
 * @since 2022
 */
public class CustomerFunnel {

	public static final String NEW_VISITOR = "New Visitor";
	public static final String RETURNING_VISITOR = "Returning Visitor";
	
	public static final String LEAD = "Lead";
	public static final String PROSPECT = "Prospect";
	
	public static final String NEW_CUSTOMER = "New Customer";
	public static final String ENGAGED_CUSTOMER = "Engaged Customer";
	
	public static final String HAPPY_CUSTOMER = "Happy Customer";
	public static final String CUSTOMER_ADVOCATE = "Customer Advocate";
	
	public static final String UNHAPPY_CUSTOMER = "Unhappy Customer";
	public static final String TERMINATED_CUSTOMER = "Terminated Customer";
	

	// 1 visitor
	public static final DataFlowStage FUNNEL_STAGE_VISITOR = new DataFlowStage(1, NEW_VISITOR, GENERAL_DATA_FUNNEL,
			STANDARD_CUSTOMER_FLOW, FlowType.MARKETING_METRIC);
	public static final DataFlowStage FUNNEL_STAGE_RETURNNING_VISITOR = new DataFlowStage(2, RETURNING_VISITOR,
			GENERAL_DATA_FUNNEL, STANDARD_CUSTOMER_FLOW, FlowType.MARKETING_METRIC);

	// 2 lead
	public static final DataFlowStage FUNNEL_STAGE_LEAD = new DataFlowStage(3, LEAD, GENERAL_DATA_FUNNEL,
			STANDARD_CUSTOMER_FLOW, FlowType.MARKETING_METRIC);
	public static final DataFlowStage FUNNEL_STAGE_PROSPECT = new DataFlowStage(4, PROSPECT, GENERAL_DATA_FUNNEL,
			STANDARD_CUSTOMER_FLOW, FlowType.KEY_BUSINESS_METRIC);

	// 3 customer
	public static final DataFlowStage FUNNEL_STAGE_NEW_CUSTOMER = new DataFlowStage(5, NEW_CUSTOMER,
			GENERAL_DATA_FUNNEL, STANDARD_CUSTOMER_FLOW, FlowType.KEY_BUSINESS_METRIC);
	public static final DataFlowStage FUNNEL_STAGE_ENGAGED_CUSTOMER = new DataFlowStage(6, ENGAGED_CUSTOMER,
			GENERAL_DATA_FUNNEL, STANDARD_CUSTOMER_FLOW, FlowType.KEY_BUSINESS_METRIC);

	// 4 happy
	public static final DataFlowStage FUNNEL_STAGE_HAPPY_CUSTOMER = new DataFlowStage(7, HAPPY_CUSTOMER,
			GENERAL_DATA_FUNNEL, STANDARD_CUSTOMER_FLOW, FlowType.EXPERIENCE_METRIC);
	public static final DataFlowStage FUNNEL_STAGE_CUSTOMER_ADVOCATE = new DataFlowStage(8, CUSTOMER_ADVOCATE,
			GENERAL_DATA_FUNNEL, STANDARD_CUSTOMER_FLOW, FlowType.EXPERIENCE_METRIC);

	// 5 unhappy
	public static final DataFlowStage FUNNEL_STAGE_UNHAPPY_CUSTOMER = new DataFlowStage(9, UNHAPPY_CUSTOMER,
			GENERAL_DATA_FUNNEL, STANDARD_CUSTOMER_FLOW, FlowType.EXPERIENCE_METRIC);
	public static final DataFlowStage FUNNEL_STAGE_TERMINATED_CUSTOMER = new DataFlowStage(10, TERMINATED_CUSTOMER,
			GENERAL_DATA_FUNNEL, STANDARD_CUSTOMER_FLOW, FlowType.EXPERIENCE_METRIC);

	private static final Map<String, String> FUNNEL_MAP = new HashMap<String, String>();
	static {
		// 1
		FUNNEL_MAP.put(FunnelMetaData.STAGE_NEW_VISITOR, CustomerFunnel.NEW_VISITOR);
		// 2
		FUNNEL_MAP.put(FunnelMetaData.STAGE_RETURNING_VISITOR, CustomerFunnel.RETURNING_VISITOR);
		// 3
		FUNNEL_MAP.put(FunnelMetaData.STAGE_LEAD, CustomerFunnel.LEAD);
		// 4
		FUNNEL_MAP.put(FunnelMetaData.STAGE_PROSPECT, CustomerFunnel.PROSPECT);
		// 5
		FUNNEL_MAP.put(FunnelMetaData.STAGE_NEW_CUSTOMER, CustomerFunnel.NEW_CUSTOMER);
		// 6
		FUNNEL_MAP.put(FunnelMetaData.STAGE_ENGAGED_CUSTOMER, CustomerFunnel.ENGAGED_CUSTOMER);
		// 7
		FUNNEL_MAP.put(FunnelMetaData.STAGE_HAPPY_CUSTOMER, CustomerFunnel.HAPPY_CUSTOMER);
		// 8
		FUNNEL_MAP.put(FunnelMetaData.STAGE_CUSTOMER_ADVOCATE, CustomerFunnel.CUSTOMER_ADVOCATE);
		// 9
		FUNNEL_MAP.put(FunnelMetaData.STAGE_UNHAPPY_CUSTOMER, CustomerFunnel.UNHAPPY_CUSTOMER);
		// 10
		FUNNEL_MAP.put(FunnelMetaData.STAGE_TERMINATED_CUSTOMER, CustomerFunnel.TERMINATED_CUSTOMER);
	}
	
	/**
	 * @param funnelKey
	 * @return a valid customer Funnel Name
	 */
	public static final String getCustomerFunnelName(String funnelKey) {
		if(StringUtil.isNotEmpty(funnelKey)) {
			return FUNNEL_MAP.getOrDefault(funnelKey, StringPool.BLANK);
		}
		return StringPool.BLANK;
	}
}
