package leotech.cdp.model.activation.decision;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * MarketingDecisionCode
 *
 * Facade + JSON exporter for all decision codes
 * used by Agentic AI and API consumers.
 * 
 * @since 2025
 * @author Trieu Nguyen
 */
public final class MarketingDecisionCode {

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private MarketingDecisionCode() {}

    /**
     * Export all decision codes grouped by industry as JSON.
     *
     * @return JSON string
     */
    public static String toJson() {
        return GSON.toJson(toMap());
    }

    /**
     * Export all decision codes grouped by industry as Map
     * (useful for API responses without JSON string).
     */
    public static Map<String, List<String>> toMap() {
        Map<String, List<String>> data = new LinkedHashMap<>();

        data.put("common", List.of(
                CommonDecision.DO_NOTHING,
                CommonDecision.SUPPRESS,
                CommonDecision.HUMAN_HANDOFF,
                CommonDecision.REMINDER,
                CommonDecision.FOLLOW_UP,
                CommonDecision.PAYMENT_NUDGE,
                CommonDecision.EDUCATE,
                CommonDecision.FEEDBACK_REQUEST
        ));

        data.put("retail", List.of(
                RetailDecision.COUPON,
                RetailDecision.DISCOUNT,
                RetailDecision.VOUCHER,
                RetailDecision.FREE_SHIPPING,
                RetailDecision.ABANDON_CART,
                RetailDecision.UPSELL,
                RetailDecision.CROSS_SELL,
                RetailDecision.LOYALTY_REWARD,
                RetailDecision.WINBACK
        ));

        data.put("stock_trading", List.of(
                StockTradingDecision.TRADE_SIGNAL,
                StockTradingDecision.PRICE_ALERT,
                StockTradingDecision.STOP_LOSS_WARNING,
                StockTradingDecision.TAKE_PROFIT_ALERT,
                StockTradingDecision.PORTFOLIO_REBALANCE,
                StockTradingDecision.MARKET_UPDATE,
                StockTradingDecision.RISK_EXPLANATION
        ));

        data.put("real_estate", List.of(
                RealEstateDecision.PROPERTY_RECOMMENDATION,
                RealEstateDecision.NEW_LISTING_ALERT,
                RealEstateDecision.PRICE_DROP_ALERT,
                RealEstateDecision.VIEWING_INVITE,
                RealEstateDecision.MORTGAGE_ADVICE
        ));

        data.put("education", List.of(
                EducationDecision.COURSE_RECOMMENDATION,
                EducationDecision.ENROLLMENT_REMINDER,
                EducationDecision.LESSON_REMINDER,
                EducationDecision.CERTIFICATION_PROMPT,
                EducationDecision.LEARNING_PATH_SUGGESTION
        ));

        data.put("hospitality", List.of(
                HospitalityDecision.BOOKING_REMINDER,
                HospitalityDecision.CHECKIN_REMINDER,
                HospitalityDecision.ROOM_UPGRADE,
                HospitalityDecision.EXPERIENCE_UPSELL,
                HospitalityDecision.POST_STAY_FEEDBACK
        ));

        return data;
    }
}
