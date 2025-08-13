package leotech.cdp.model.analytics;

import com.google.gson.annotations.Expose;

/**
 * Event counter for standard reporting
 * 
 * @author tantrieuf31
 * @since 2021
 */
public final class EventCounter {

	@Expose
	long pageView = 0;

	@Expose
	long itemView = 0;

	@Expose
	long surveyView = 0;

	@Expose
	long qrCodeScan = 0;

	@Expose
	long clickDetails = 0;

	@Expose
	long checkIn = 0;

	@Expose
	long askQuestion = 0;

	@Expose
	long madePayment = 0;

	@Expose
	long submitRatingForm = 0;

	@Expose
	long submitFeedbackForm = 0;

	@Expose
	long negativeSocialReview = 0;

	@Expose
	long positiveSocialReview = 0;

	public EventCounter() {
		// gson
	}

	public long getPageView() {
		return pageView;
	}

	public void setPageView(long pageView) {
		this.pageView = pageView;
	}

	public long getItemView() {
		return itemView;
	}

	public void setItemView(long itemView) {
		this.itemView = itemView;
	}

	public long getSurveyView() {
		return surveyView;
	}

	public void setSurveyView(long surveyView) {
		this.surveyView = surveyView;
	}

	public long getQrCodeScan() {
		return qrCodeScan;
	}

	public void setQrCodeScan(long qrCodeScan) {
		this.qrCodeScan = qrCodeScan;
	}

	public long getClickDetails() {
		return clickDetails;
	}

	public void setClickDetails(long clickDetails) {
		this.clickDetails = clickDetails;
	}

	public long getCheckIn() {
		return checkIn;
	}

	public void setCheckIn(long checkIn) {
		this.checkIn = checkIn;
	}

	public long getAskQuestion() {
		return askQuestion;
	}

	public void setAskQuestion(long askQuestion) {
		this.askQuestion = askQuestion;
	}

	public long getMadePayment() {
		return madePayment;
	}

	public void setMadePayment(long madePayment) {
		this.madePayment = madePayment;
	}

	public long getSubmitRatingForm() {
		return submitRatingForm;
	}

	public void setSubmitRatingForm(long submitRatingForm) {
		this.submitRatingForm = submitRatingForm;
	}

	public long getSubmitFeedbackForm() {
		return submitFeedbackForm;
	}

	public void setSubmitFeedbackForm(long submitFeedbackForm) {
		this.submitFeedbackForm = submitFeedbackForm;
	}

	public long getNegativeSocialReview() {
		return negativeSocialReview;
	}

	public void setNegativeSocialReview(long negativeSocialReview) {
		this.negativeSocialReview = negativeSocialReview;
	}

	public long getPositiveSocialReview() {
		return positiveSocialReview;
	}

	public void setPositiveSocialReview(long positiveSocialReview) {
		this.positiveSocialReview = positiveSocialReview;
	}

}
