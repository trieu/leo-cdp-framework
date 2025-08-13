package leotech.cdp.model.analytics;

public class StatisticsView {

	double avgTransactionValue = 0;
	double totalTransactionValue =0;
	
	public StatisticsView() {
		
	}
	
	public double getAvgTransactionValue() {
		avgTransactionValue = Math.round(avgTransactionValue * 100.0) / 100.0;
		return avgTransactionValue;
	}
	public void setAvgTransactionValue(double avgTransactionValue) {
		this.avgTransactionValue = avgTransactionValue;
	}
	public void setAvgTransactionValue(Double avgTransactionValue) {
		if(avgTransactionValue != null) {
			this.avgTransactionValue = avgTransactionValue;
		}
	}
	public double getTotalTransactionValue() {
		totalTransactionValue = Math.round(totalTransactionValue * 100.0) / 100.0;
		return totalTransactionValue;
	}
	public void setTotalTransactionValue(double totalTransactionValue) {
		this.totalTransactionValue = totalTransactionValue;
		
	}
	public void setTotalTransactionValue(Double totalTransactionValue) {
		if(totalTransactionValue != null) {
			this.totalTransactionValue = totalTransactionValue;
		}
	}
	
}
