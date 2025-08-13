package test.cdp.clv;

import java.util.ArrayList;
import java.util.List;

public class CustomerLifetimeValue {

	private static double discountRate = 0.1; // Discount rate for future cash flows

	public static void main(String[] args) {

		// Define the customer's purchase history
		List<Double> purchaseHistory = new ArrayList<Double>();
		purchaseHistory.add(498.0); // First purchase
		purchaseHistory.add(275.0); // Second purchase
		//purchaseHistory.add(50.0); // Third purchase

		// Calculate the customer's CLV using a probabilistic model
		double clv = calculateCLV(purchaseHistory, 0, 0.2, 36);
		System.out.println("Customer Lifetime Value: $ " + clv);
	}

	public static double calculateCLV(List<Double> purchaseHistory, double initialValue, double retentionRate, int months) {

		double totalValue = 0;
		double futureValue = 0;
		int numPurchases = purchaseHistory.size();

		// Calculate the total value of the customer's purchases
		for (int i = 0; i < numPurchases; i++) {
			totalValue += purchaseHistory.get(i);
		}

		// Calculate the average value of the customer's purchases
		double averageValue = totalValue / numPurchases;

		// Calculate the expected number of purchases in the next 12 months
		double expectedPurchases = retentionRate * numPurchases;

		// Calculate the expected value of those purchases
		double expectedValue = expectedPurchases * averageValue;

		// Calculate the future value of those purchases, discounted to present value
		for (int i = 0; i < months; i++) {
			futureValue += expectedValue / Math.pow(1 + discountRate, i + 1);
		}

		// Add the present value of the customer's historical purchases to the future
		// value
		double clv = totalValue + futureValue - initialValue;

		return clv;
	}
}
