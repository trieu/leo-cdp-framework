package test.cdp.clv;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import leotech.cdp.domain.scoring.ComputeProfileSimpleCLV;
import leotech.cdp.domain.scoring.ComputeProfileCLV;
import leotech.cdp.domain.scoring.RetentionPeriodType;

/**
 * @author thomas
 * @see https://chat.openai.com/c/4f3c25cd-528d-4a90-8948-cd8f02d5595c
 *
 */
public class TestCLV {

	
	static List<ComputeProfileCLV> computations = new ArrayList<>();


	static Date parseDateString(String s) {
		DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy");
		return formatter.parseDateTime(s).toDate();
	}

	static void customer1CLV() {
		double discountRate = 0.05; // Adjust this to your business's specific discount rate
		ComputeProfileCLV c1 = new ComputeProfileSimpleCLV("111", "Alice", discountRate, defaultRetentionPeriodType);
		c1.makePurchase(parseDateString("16/08/2023"), 100.0);
		c1.makePurchase(parseDateString("19/08/2023"), 150.0);
		c1.makePurchase(parseDateString("20/09/2023"), 120.0);
		c1.makePurchase(parseDateString("23/09/2023"), 200.0);
		computations.add(c1);
	}

	static void print(ComputeProfileCLV c, double clv) {
		int size = c.getPurchasedValues().size();
		double tv = c.getTotalPurchasedValue();
		System.out.printf("Customer %s %s's CLV: $%.2f. Total orders %d . Total purchased value  $%.2f \n \n", c.getProfileId(), c.getName(), clv, size, tv);
	}

	static void customer2CLV() {
		double discountRate = 0.01; // Adjust this to your business's specific discount rate
		ComputeProfileCLV c2 = new ComputeProfileSimpleCLV("222", "Bob", discountRate,  defaultRetentionPeriodType);
		c2.makePurchase(parseDateString("16/07/2023"), 100.0);
		c2.makePurchase(parseDateString("19/08/2023"), 150.0);
		c2.makePurchase(parseDateString("20/09/2023"), 120.0);
		c2.makePurchase(parseDateString("23/09/2023"), 200.0);
		computations.add(c2);
	}

	static void customer3CLV() {
		double discountRate = 0.1; // Adjust this to your business's specific discount rate
		ComputeProfileCLV c3 = new ComputeProfileSimpleCLV("333", "Tom", discountRate,  defaultRetentionPeriodType);
		c3.makePurchase(parseDateString("16/01/2023"), 500.0);
		c3.makePurchase(parseDateString("19/10/2023"), 600.0);
		computations.add(c3);
	}

	static void customer4CLV() {
		double discountRate = 0.1; // Adjust this to your business's specific discount rate
		ComputeProfileCLV c4 = new ComputeProfileSimpleCLV("444", "Peter", discountRate, defaultRetentionPeriodType);
		c4.makePurchase(parseDateString("19/10/2022"), 600.0);
		computations.add(c4);
	}
	
	static void customer5CLV() {
		double discountRate = 0.1; // Adjust this to your business's specific discount rate
		ComputeProfileCLV c5 = new ComputeProfileSimpleCLV("555", "Thomas", discountRate, defaultRetentionPeriodType);
		c5.makePurchase(parseDateString("9/5/2022"), 300.0);
		c5.makePurchase(parseDateString("12/6/2021"), 100.0);
		c5.makePurchase(parseDateString("28/9/2022"), 200.0);
		c5.makePurchase(parseDateString("1/1/2023"), 10.0);
		c5.makePurchase(parseDateString("10/5/2023"), 210.0);
		c5.makePurchase(parseDateString("1/6/2023"), 20.0);
		c5.makePurchase(parseDateString("1/8/2023"), 2000.0);
		c5.makePurchase(parseDateString("9/11/2023"), 120.0);
		computations.add(c5);
	}

	static int defaultRetentionPeriodType = RetentionPeriodType.MONTH.getValue();
	
	public static void main(String[] args) {
		customer1CLV();
		customer2CLV();
		customer3CLV();
		customer4CLV();
		customer5CLV();
		 //
        long churnThreshold = ComputeProfileCLV.CHURN_THRESHOLD_IN_120_DAYS;
        double churnProbability = ComputeProfileCLV.computeChurnProbability(computations, churnThreshold);
        
        System.out.println("churnProbability " + churnProbability + " \n ");
        
        for (ComputeProfileCLV c : computations) {
			double clv = c.calculateCLV(churnProbability);
			print(c, clv);
		}
	}

}
