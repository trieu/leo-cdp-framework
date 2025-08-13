package leotech.cdp.model.customer;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

/**
 * 
 * Finance Credit Event <br>
 * e.g data: https://www.kaggle.com/datasets/uciml/german-credit
 * 
 * training:
 * https://github.com/USPA-Technology/Loan-prediction-using-logistic-regression
 * video: https://www.youtube.com/watch?v=AB4BtP9RSNM video:
 * https://www.youtube.com/watch?v=XckM1pFgZmg prediction:
 * https://github.com/USPA-Technology/loan-prediction-form
 * 
 * 
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class FinanceCreditEvent {

	@Expose
	String savingAccountStatus = "";

	@Expose
	String checkingAccountStatus = "";

	@Expose
	long creditAmount = 0;

	@Expose
	int duration = 0;

	@Expose
	String purpose = "";

	@Expose
	String risk = "";

	public FinanceCreditEvent() {
		// gson
	}

	public FinanceCreditEvent(String savingAccountStatus, String checkingAccountStatus, long creditAmount, int duration,
			String purpose, String risk) {
		super();
		this.savingAccountStatus = savingAccountStatus;
		this.checkingAccountStatus = checkingAccountStatus;
		this.creditAmount = creditAmount;
		this.duration = duration;
		this.purpose = purpose;
		this.risk = risk;
	}

	public String getSavingAccountStatus() {
		return savingAccountStatus;
	}

	public void setSavingAccountStatus(String savingAccountStatus) {
		this.savingAccountStatus = savingAccountStatus;
	}

	public String getCheckingAccountStatus() {
		return checkingAccountStatus;
	}

	public void setCheckingAccountStatus(String checkingAccountStatus) {
		this.checkingAccountStatus = checkingAccountStatus;
	}

	public long getCreditAmount() {
		return creditAmount;
	}

	public void setCreditAmount(long creditAmount) {
		this.creditAmount = creditAmount;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public String getRisk() {
		return risk;
	}

	public void setRisk(String risk) {
		this.risk = risk;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
