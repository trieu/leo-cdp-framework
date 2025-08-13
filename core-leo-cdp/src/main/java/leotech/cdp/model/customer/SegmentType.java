package leotech.cdp.model.customer;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public final class SegmentType {
	// ad-hoc query
	public final static int AD_HOC_QUERY = 0;
	
	// https://learn.g2.com/market-segmentation
	public final static int GEOGRAPHIC = 1;
	public final static int DEMOGRAPHIC = 2;
	public final static int PSYCHOGRAPHIC = 3;
	public final static int BEHAVIORAL = 4;

	// common segment type for customer acquisition
	public final static int FIRST_RETARGETING = 5;
	public final static int LOOKALIKE = 6;

	// common segment type for customer retention
	public final static int RFM_ANALYSIS = 7; // https://clevertap.com/blog/rfm-analysis/
	public final static int CHURN = 8;
	
	public final static int DATA_LABELS = 9;

}
