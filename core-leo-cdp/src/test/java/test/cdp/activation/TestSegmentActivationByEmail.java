package test.cdp.activation;

import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.model.activation.ActivationRuleType;
import leotech.cdp.model.asset.AssetType;
import leotech.cdp.model.customer.Segment;
import rfx.core.util.Utils;

public class TestSegmentActivationByEmail {

	public static void main(String[] args) {
		Segment segment = SegmentDataManagement.getSegmentById("SCiqZUPpnBhslXe2LZFMU");

		System.out.println(segment.getName());

		updateRules(segment);

		//ActivationRuleManagement.activateSegment(segment);

		Utils.exitSystemAfterTimeout(20000);
	}

	protected static void updateRules(Segment segment) {
		int assetType = AssetType.EMAIL_CONTENT;
		String activationType = ActivationRuleType.PRODUCT_RECOMMENDATION_EMAIL;


		String assetTemplateId = "2C6fysYAnafzSzw8eeV9pi";
		String dataServiceId = "brevo";

		
		
//		Utils.sleep(2000);
//		segment.setActivationRule(actionType,
//				new ActivationRule(3, assetType, assetTemplateId, dataServiceId, -2, ActivationRuleManagement.SENDING_EMAIL));
//		Utils.sleep(2000);
//		segment.setActivationRule(actionType,
//				new ActivationRule(1, assetType, assetTemplateId, dataServiceId, 4, ActivationRuleManagement.SENDING_EMAIL));
		
		SegmentDataManagement.updateSegment(segment);
	}

	
}
