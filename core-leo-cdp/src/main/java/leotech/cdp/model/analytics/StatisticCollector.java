package leotech.cdp.model.analytics;

import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.domain.DataFlowManagement;
import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.model.journey.DataFlowStage;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.FlowType;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31 
 * @since 2021
 *
 */
public final class StatisticCollector implements Comparable<StatisticCollector> {
	
	@Expose
	String collectorKey = "";
	
	@Expose
	double collectorCount = 0;
	
	@Expose
	int orderIndex = 0;
	
	@Expose
	Date dateTime = null;
	
	@Expose
	String dataFlowStageId = "";
	
	@Expose
	int flowType = FlowType.SYSTEM_METRIC;// 0 is system, 1 for marketing, 2 for sales, 3 for customer service

	public StatisticCollector() {
		// gson
	}

	public StatisticCollector(String collectorKey, double collectorCount, int orderIndex) {
		super();
		this.collectorKey = collectorKey;
		this.collectorCount = collectorCount;
		this.orderIndex = orderIndex;
	}
	
	public StatisticCollector(String collectorKey, double collectorCount, int orderIndex, String dataFlowStageId, int flowType) {
		super();
		this.collectorKey = collectorKey;
		this.collectorCount = collectorCount;
		this.orderIndex = orderIndex;
		this.dataFlowStageId = dataFlowStageId;
		this.flowType = flowType;
	}

	public int getOrderIndex() {
		return orderIndex;
	}

	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	public String getCollectorKey() {
		return collectorKey;
	}
	public void setCollectorKey(String collectorKey) {
		this.collectorKey = collectorKey;
	}
	public double getCollectorCount() {
		return collectorCount;
	}

	public void setCollectorCount(double collectorCount) {
		this.collectorCount = collectorCount;
	}
	
	public void unionSetCollectorCount(double collectorCount) {
		this.collectorCount += collectorCount;
	}

	public Date getDateTime() {
		return dateTime;
	}
	public void setDateTime(Date dateTime) {
		this.dateTime = dateTime;
	}

	public int getFlowType() {
		return flowType;
	}

	public void setFlowType(int flowType) {
		this.flowType = flowType;
	}

	public String getDataFlowStageId() {
		return dataFlowStageId;
	}

	public void setDataFlowStageId(String dataFlowStageId) {
		this.dataFlowStageId = dataFlowStageId;
	}
	
	

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}
	
	@Override
	public int hashCode() {
		if(StringUtil.isNotEmpty(collectorKey)) {
			return this.collectorKey.hashCode();
		}
		return 0;
	}

	@Override
	public int compareTo(StatisticCollector o) {
		if(this.orderIndex > o.getOrderIndex()) {
			return 1;
		}
		else if(this.orderIndex < o.getOrderIndex()) {
			return -1;
		}
		return 0;
	}
	
	public static CallbackQuery<StatisticCollector> callbackProfileStatisticCollector() {
		CallbackQuery<StatisticCollector> cb = new CallbackQuery<StatisticCollector>() {
			@Override
			public StatisticCollector apply(StatisticCollector obj) {
				DataFlowStage funnelStage = DataFlowManagement.getFunnelStageById(obj.getCollectorKey());
				int index = funnelStage.getOrderIndex();
				obj.setOrderIndex(index);
				obj.setCollectorKey(funnelStage.getName());
				obj.setDataFlowStageId(funnelStage.getId());
				obj.setFlowType(funnelStage.getFlowType());
				return obj;
			}
		};
		return cb;
	}
	
	public static CallbackQuery<StatisticCollector> callbackEventStatisticCollector() {
		CallbackQuery<StatisticCollector> cb = new CallbackQuery<StatisticCollector>() {
			@Override
			public StatisticCollector apply(StatisticCollector obj) {
				EventMetric metric = EventMetricManagement.getEventMetricByName(obj.getCollectorKey());
				int index = metric.getScore();
				obj.setOrderIndex(index);
				obj.setCollectorKey(metric.getEventLabel());
				return obj;
			}
		};
		return cb;
	}

}
