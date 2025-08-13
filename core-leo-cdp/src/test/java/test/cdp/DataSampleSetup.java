package test.cdp;

import leotech.cdp.domain.schema.JourneyFlowSchema;
import rfx.core.util.Utils;

public class DataSampleSetup {

	public static void main(String[] args) {
		JourneyFlowSchema schema = new JourneyFlowSchema();
		schema.init();
		
		Utils.exitSystemAfterTimeout(5000);
	}
}
