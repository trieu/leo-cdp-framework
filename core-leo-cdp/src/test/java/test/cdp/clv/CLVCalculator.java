package test.cdp.clv;

import java.util.Arrays;
import java.util.List;

public class CLVCalculator {

	private List<Double> transactionsPerPeriod;

	/**
	 * Constructor for CLV calculator class.
	 *
	 * @param transactionsPerPeriod List of transactions per period.
	 */
	public CLVCalculator(List<Double> transactionsPerPeriod) {
		this.transactionsPerPeriod = transactionsPerPeriod;
	}

	/**
	 * Method to calculate customer lifetime value.
	 *
	 * @param discountRate  Discount rate.
	 * @param retentionRate Customer retention rate.
	 * @param periodLength  Period length in years.
	 * @return Customer lifetime value.
	 */
	public double calculateCLV(double discountRate, double retentionRate, double periodLength) {
		double clv = 0.0;
		double t = 1.0;
		for (double transactions : transactionsPerPeriod) {
			clv += transactions * Math.pow((1 + discountRate), (-t));
			t += periodLength;
		}
		clv = clv * (1 - retentionRate) / discountRate;
		return clv;
	}

	public static void main(String[] args) {

		// List of transactions per period
		List<Double> transactionsPerPeriod = Arrays.asList(100.0, 75.0, 50.0);

		// Create a new CLV calculator
		CLVCalculator clvCalculator = new CLVCalculator(transactionsPerPeriod);

		// Calculate CLV with a discount rate of 10%, retention rate of 70%, and period
		// length of 1 year
		double clv = clvCalculator.calculateCLV(0.1, 0.7, 12.0);

		// Print the CLV
		System.out.println("Customer lifetime value: " + clv);
	}
}
