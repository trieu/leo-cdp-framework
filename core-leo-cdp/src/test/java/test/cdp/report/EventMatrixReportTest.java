package test.cdp.report;

import java.util.List;
import java.util.Map;

import leotech.cdp.dao.Analytics360DaoUtil;
import leotech.cdp.domain.Analytics360Management;
import leotech.cdp.model.analytics.EventMatrixReport;
import leotech.system.model.JsonDataPayload;

public class EventMatrixReportTest {

	public static void main(String[] args) {
		List<EventMatrixReport> reports = Analytics360DaoUtil.getEventMatrixReports("", "76ycCtSGi1W14FVYuY1Joj", "", "");
		
		JsonDataPayload rs = new JsonDataPayload(reports);
		System.out.println(rs.toString());
		
		Map<String, Object> model = Analytics360Management.getEventMatrixReportModel("", "76ycCtSGi1W14FVYuY1Joj", "", "");
		JsonDataPayload rs2 = new JsonDataPayload(model);
		System.out.println(rs2.toString());
		
	}
	
}
