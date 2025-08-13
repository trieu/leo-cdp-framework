package leotech.cdp.model.customer;

import com.google.gson.annotations.Expose;

/**
 * 
 * Finance Record <br>
 * e.g data: https://docs.google.com/spreadsheets/d/1yihWzhTJr3qeQH-miuCqbXRAkirOkRmR/edit?usp=sharing&ouid=108357463841498827395&rtpof=true&sd=true
 * 
 * training: https://github.com/USPA-Technology/Loan-prediction-using-logistic-regression
 * video: https://www.youtube.com/watch?v=AB4BtP9RSNM
 * video: https://www.youtube.com/watch?v=XckM1pFgZmg
 * prediction: https://github.com/USPA-Technology/loan-prediction-form
 * 
 * 
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class FinanceRecord {
	
	// Loan Application
	// https://codepen.io/jtmp2r/pen/eNrwxG
	// https://form.123formbuilder.com/js-form-username-428314.html
	// https://github.com/helanan/banking-form


	@Expose
	protected String refApplicationId = "";
	
	@Expose
	protected String refProductId = "";
	
	// TODO

}
