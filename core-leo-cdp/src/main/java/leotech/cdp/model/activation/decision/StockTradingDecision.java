package leotech.cdp.model.activation.decision;

/**
 * StockTradingDecision
 *
 * Decisions for Stock Trading / Finance domain.
 */
public final class StockTradingDecision {

    private StockTradingDecision() {}

    public static final String TRADE_SIGNAL = "trade_signal";
    public static final String PRICE_ALERT = "price_alert";

    public static final String STOP_LOSS_WARNING = "stop_loss_warning";
    public static final String TAKE_PROFIT_ALERT = "take_profit_alert";

    public static final String PORTFOLIO_REBALANCE = "portfolio_rebalance";
    public static final String MARKET_UPDATE = "market_update";

    public static final String RISK_EXPLANATION = "risk_explanation";
}
