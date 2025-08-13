package leotech.cdp.domain.scoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.Years;

import leotech.cdp.model.analytics.TrackingEvent;
import rfx.core.util.StringUtil;

/**
 * abstract class for Customer Lifetime Value Computation
 * 
 * @author tantrieuf31
 * @since 2023
 *
 */
public abstract class ComputeProfileCLV {

	protected String profileId;
	protected String name;

	protected int retentionPeriodType = RetentionPeriodType.MONTH.getValue();
	protected double discountRate = 0;

	protected List<Double> purchasedValues; //
	protected double totalPurchasedValue = 0;
	protected List<Date> purchaseDates;
	
	public ComputeProfileCLV() {
		// for gson
		this.purchasedValues = new ArrayList<>();
		this.purchaseDates = new ArrayList<>();
	}
	
	public ComputeProfileCLV(String profileId, String name, double discountRate, int retentionPeriodType) {
		init(profileId, name, discountRate, retentionPeriodType);
	}
	
	public void init(String profileId, String name, double discountRate, int retentionPeriodType) {
		this.profileId = profileId;
		this.name = name;
		this.purchasedValues = new ArrayList<>();
		this.purchaseDates = new ArrayList<>();
		this.discountRate = discountRate;
		this.retentionPeriodType = retentionPeriodType;
	}

	// Getters and setters as needed

	public String getProfileId() {
		return profileId;
	}

	public String getName() {
		return name;
	}

	public List<Double> getPurchasedValues() {
		return purchasedValues;
	}

	public List<Date> getPurchaseDates() {
		return purchaseDates;
	}
	
	public void makePurchase(TrackingEvent event) {
		purchaseDates.add(event.getCreatedAt());
		purchasedValues.add(event.getTransactionValue());
	}

	public void makePurchase(Date purchaseDate, double purchasedValue) {
		purchaseDates.add(purchaseDate);
		purchasedValues.add(purchasedValue);
	}
	
	public int getRetentionPeriodType() {
		return retentionPeriodType;
	}

	public double getTotalPurchasedValue() {
		return totalPurchasedValue;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected int getTimeDiff(int i) {
		LocalDate date1 = LocalDate.fromDateFields(purchaseDates.get(i));
		LocalDate date2 = LocalDate.fromDateFields(purchaseDates.get(i - 1));

		int diff = 0;
		if (this.retentionPeriodType == RetentionPeriodType.DAY.getValue()) {
			diff = Days.daysBetween(date1, date2).getDays();
		} else if (this.retentionPeriodType == RetentionPeriodType.WEEK.getValue()) {
			diff = Weeks.weeksBetween(date1, date2).getWeeks();
		} else if (this.retentionPeriodType == RetentionPeriodType.MONTH.getValue()) {
			diff = Months.monthsBetween(date1, date2).getMonths();
		} else if (this.retentionPeriodType == RetentionPeriodType.YEAR.getValue()) {
			diff = Years.yearsBetween(date1, date2).getYears();
		} else {
			diff = Months.monthsBetween(date1, date2).getMonths();
		}
		return Math.abs(diff);
	}

	/**
	 * @return Average Purchase Frequency
	 */
	protected double calculateAveragePurchaseFrequency() {
		int purchasingCount = purchaseDates.size();
		if (purchaseDates.isEmpty() || purchasingCount == 1) {
			return 0.0; // Cannot compute frequency with less than 2 purchases.
		}
		double totalFrequency = 0.0;
		for (int i = 1; i < purchasingCount; i++) {
			long timeDiff = getTimeDiff(i);
			totalFrequency += timeDiff;
		}
		if (totalFrequency == 0) {
			totalFrequency = 1;
		}

		// Calculate the average frequency as the reciprocal of the average time difference.
		double averageFrequency = (purchasingCount - 1) / totalFrequency;
		return averageFrequency;
	}
	
	/**
	 *  get default churn threshold (e.g., 30 days of inactivity)
	 */
	public static final long CHURN_THRESHOLD_IN_30_DAYS = 30 * 24 * 60 * 60 * 1000L;
	public static final long CHURN_THRESHOLD_IN_60_DAYS = CHURN_THRESHOLD_IN_30_DAYS * 2;
	public static final long CHURN_THRESHOLD_IN_90_DAYS = CHURN_THRESHOLD_IN_30_DAYS * 3;
	public static final long CHURN_THRESHOLD_IN_120_DAYS = CHURN_THRESHOLD_IN_30_DAYS * 4;
	public static final long CHURN_THRESHOLD_IN_150_DAYS = CHURN_THRESHOLD_IN_30_DAYS * 5;
	public static final long CHURN_THRESHOLD_IN_180_DAYS = CHURN_THRESHOLD_IN_30_DAYS * 6;
	
	/**
	 * @param targetComputationList
	 * @param churnThresholdMillis
	 * @return the probability of customer churn in double
	 */
	public static double computeChurnProbability(List<ComputeProfileCLV> targetComputationList, long churnThresholdMillis) {
        if (targetComputationList.isEmpty()) {
            return 0.0;
        }
        int inactiveCount = 0;
        long currentTimeMillis = System.currentTimeMillis();

        for (ComputeProfileCLV clvComputation : targetComputationList) {
            List<Date> purchaseDates = clvComputation.getPurchaseDates();
            
            // make sure the data is sorted
            Collections.sort(purchaseDates);
            
            System.out.println("clvComputation profileId " + clvComputation.getProfileId());
    		for (Date date : purchaseDates) {
    			System.out.println(date);
    		}
			if (!purchaseDates.isEmpty()) {
                Date lastPurchaseDate = purchaseDates.get(purchaseDates.size() - 1);
                System.out.println("lastPurchaseDate " +lastPurchaseDate + " \n\n");
                if (currentTimeMillis - lastPurchaseDate.getTime() > churnThresholdMillis) {
                    inactiveCount++;
                }
            } else {
                // If a customer has never made a purchase, consider them inactive.
                inactiveCount++;
            }
        }
        double churnProbability = (double) inactiveCount / targetComputationList.size();
        return churnProbability;
    }
	
	/**
	 * @return true if all required data (purchasedValues, purchaseDates, profileId) for CLV computation is OK
	 */
	public boolean checkForDataValidation() {
		return !purchasedValues.isEmpty() && !purchaseDates.isEmpty() && StringUtil.isNotEmpty(profileId) ;
	}
	
	/**
	 * call the computation of CLV
	 * 
	 * @param churnProbability
	 * @return CLV
	 */
	abstract public double calculateCLV(double churnProbability);

}