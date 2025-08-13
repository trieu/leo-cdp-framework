package leotech.cdp.domain.scoring;

/**
 * Default class for Customer Lifetime Value Computation
 * 
 * @author tantrieuf31
 * @since 2023
 * 
 */
public class ComputeProfileSimpleCLV extends ComputeProfileCLV {
	public ComputeProfileSimpleCLV() {
		super();
	}
	public ComputeProfileSimpleCLV(String profileId, String name, double discountRate, int retentionPeriodType) {
		super(profileId, name, discountRate, retentionPeriodType);
	}
	@Override
	public double calculateCLV(double churnProbability) {
		double baseClv = 0.0, totalPurchasedValue = 0;
		
		if (checkForDataValidation()) {
			for (int i = 0; i < purchasedValues.size(); i++) {
				double discountFactor = 1 / Math.pow(1 + this.discountRate, i);
				Double pv = purchasedValues.get(i);
				baseClv += (pv * discountFactor);
				totalPurchasedValue += pv;
			}
			this.totalPurchasedValue = totalPurchasedValue;
			
			// S
			double avgPurchaseFrequency = this.calculateAveragePurchaseFrequency();
			baseClv *= avgPurchaseFrequency;

			// Adjust CLV for churn probability
			double adjustedCLV = baseClv * (1 - churnProbability);
			return adjustedCLV;
		}
		return baseClv;
	}
}