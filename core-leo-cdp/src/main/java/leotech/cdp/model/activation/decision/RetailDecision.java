package leotech.cdp.model.activation.decision;

/**
 * RetailDecision
 *
 * Decisions specific to Retail & E-commerce.
 */
public final class RetailDecision {

    private RetailDecision() {}

    public static final String COUPON = "coupon";
    public static final String DISCOUNT = "discount";
    public static final String VOUCHER = "voucher";
    public static final String FREE_SHIPPING = "free_shipping";

    public static final String ABANDON_CART = "abandon_cart";
    public static final String UPSELL = "upsell";
    public static final String CROSS_SELL = "cross_sell";

    public static final String LOYALTY_REWARD = "loyalty_reward";
    public static final String WINBACK = "winback";
}
