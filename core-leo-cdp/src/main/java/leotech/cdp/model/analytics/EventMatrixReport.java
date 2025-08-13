package leotech.cdp.model.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The event matrix report, as data object for
 * https://chartjs-chart-matrix.pages.dev/samples/category.html
 * 
 * @author tantrieuf31
 * @since 2022
 */
public class EventMatrixReport {

	@SerializedName("y")
	@Expose
	String labelY;

	@SerializedName("x")
	@Expose
	String labelX = "";

	@SerializedName("v")
	@Expose
	long value = 0;
	
	@Expose
	String journeyMapId = "";
	
	@Expose
	String touchpointHubId = "";

	public EventMatrixReport() {
		//
	}

	public String getLabelY() {
		return labelY;
	}

	public void setLabelY(String labelY) {
		this.labelY = labelY;
	}

	public String getLabelX() {
		return labelX;
	}

	public void setLabelX(String labelX) {
		this.labelX = labelX;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}
	
	
	
	public String getJourneyMapId() {
		return journeyMapId;
	}

	public void setJourneyMapId(String journeyMapId) {
		this.journeyMapId = journeyMapId;
	}

	public String getTouchpointHubId() {
		return touchpointHubId;
	}

	public void setTouchpointHubId(String touchpointHubId) {
		this.touchpointHubId = touchpointHubId;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	/**
	 * convert dataList to dataMap for LEO Admin Event Matrix Report
	 * 
	 * @param dataList
	 * @return
	 */
	public static Map<String, Object> toDataMap(List<EventMatrixReport> dataList) {
		Map<String, Long> reportSummary = new HashMap<String, Long>();
		List<String> xLabels = new ArrayList<String>();
		List<String> yLabels = new ArrayList<String>();
		
		for (EventMatrixReport data : dataList) {
			String labelX = data.getLabelX();
			if(!xLabels.contains(labelX)) {
				xLabels.add(labelX);	
			}
			
			String labelY = data.getLabelY();
			if(!yLabels.contains(labelY)) {
				yLabels.add(labelY);	
			}
			
			long c = reportSummary.getOrDefault(labelX, 0L) + data.getValue();
			reportSummary.put(labelX, c);
		}
		
		// update ordering 
		Collections.reverse(yLabels);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("dataList", dataList);
		map.put("xLabels", xLabels);
		map.put("yLabels", yLabels);
		map.put("reportSummary", reportSummary);
		return map;
	}

}
