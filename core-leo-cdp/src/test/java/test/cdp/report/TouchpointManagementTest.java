package test.cdp.report;

import java.time.Instant;
import java.util.List;

import leotech.cdp.domain.TouchpointManagement;
import leotech.cdp.model.analytics.CytoscapeData;
import leotech.cdp.model.analytics.TouchpointFlowReport;
import rfx.core.util.Utils;

public class TouchpointManagementTest {

	public static void main(String[] args) {

		
		long startTime = Instant.now().toEpochMilli();
		
		testTouchpointFlowReport();
		
		testTouchpointFlowReport();
		
		long endTime = Instant.now().toEpochMilli();
		 
        long timeElapsed = endTime - startTime;
        System.out.println("Execution time in milliseconds: " + timeElapsed);
		
		Utils.exitSystemAfterTimeout(1000);
	}

	private static void testTouchpointFlowReport() {
		String beginFilterDate = "";
		String endFilterDate = "";
		String journeyMapId = "";
		String profileId = "24gIbVACrSmVc5eavU2lVM";
		int startIndex = 00;
		int numberFlow = 150; //number of flow edges
		
		List<TouchpointFlowReport> reports;
		boolean reportForProfile = ! profileId.isBlank();
		if( reportForProfile ) {
			reports = TouchpointManagement.getTouchpointFlowReportForProfile(profileId, journeyMapId, beginFilterDate, endFilterDate, startIndex, numberFlow);
		}
		else {
			reports = TouchpointManagement.getTouchpointFlowReportForJourney(journeyMapId, beginFilterDate, endFilterDate, startIndex, numberFlow);
		}
		
		System.out.println("report.size "+reports.size());
		System.out.println("TouchpointFlowReport \n "+reports.get(0));
		CytoscapeData data = new CytoscapeData(journeyMapId, reports, true);
		System.out.println("report.getNodes "+data.getNodes().size());
		System.out.println("report.getEdges "+data.getEdges().size());
	}
}
