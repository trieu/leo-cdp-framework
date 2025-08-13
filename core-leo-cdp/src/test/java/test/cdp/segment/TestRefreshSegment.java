package test.cdp.segment;

import leotech.cdp.domain.SegmentDataManagement;

public class TestRefreshSegment {

	public static void main(String[] args) {
		String segmentId = "3rkeA3xEy7oNQ0OrlFIMze";
		
		boolean ok = SegmentDataManagement.refreshSegmentData(segmentId);
		
		//Utils.exitSystemAfterTimeout(9000);
		
		System.out.println("refreshSegmentData ok: "+ok);
	}
}
