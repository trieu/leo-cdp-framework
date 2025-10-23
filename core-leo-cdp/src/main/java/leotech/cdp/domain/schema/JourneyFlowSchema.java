package leotech.cdp.domain.schema;

import static leotech.cdp.domain.schema.CustomerFunnel.FUNNEL_STAGE_CUSTOMER_ADVOCATE;
import static leotech.cdp.domain.schema.CustomerFunnel.FUNNEL_STAGE_ENGAGED_CUSTOMER;
import static leotech.cdp.domain.schema.CustomerFunnel.FUNNEL_STAGE_HAPPY_CUSTOMER;
import static leotech.cdp.domain.schema.CustomerFunnel.FUNNEL_STAGE_LEAD;
import static leotech.cdp.domain.schema.CustomerFunnel.FUNNEL_STAGE_NEW_CUSTOMER;
import static leotech.cdp.domain.schema.CustomerFunnel.FUNNEL_STAGE_PROSPECT;
import static leotech.cdp.domain.schema.CustomerFunnel.FUNNEL_STAGE_RETURNNING_VISITOR;
import static leotech.cdp.domain.schema.CustomerFunnel.FUNNEL_STAGE_TERMINATED_CUSTOMER;
import static leotech.cdp.domain.schema.CustomerFunnel.FUNNEL_STAGE_UNHAPPY_CUSTOMER;
import static leotech.cdp.domain.schema.CustomerFunnel.FUNNEL_STAGE_VISITOR;

import java.util.ArrayList;
import java.util.List;

import leotech.cdp.dao.DataFlowStageDaoUtil;
import leotech.cdp.dao.EventMetricDaoUtil;
import leotech.cdp.model.journey.DataFlowStage;
import leotech.cdp.model.journey.EventMetric;

/**
 * Journey Data Flow Schema
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class JourneyFlowSchema {
	
	public static final String STANDARD_EVENT_FLOW = "standard_event_flow";
	public static final String STANDARD_USER_FLOW = "standard_user_flow";
	public static final String STANDARD_CUSTOMER_FLOW = "standard_customer_flow";

	public static final String GENERAL_BUSINESS = "general_business";
	public static final String GENERAL_BUSINESS_EVENT = "general_business_event";
	public static final String GENERAL_DATA_FUNNEL = "general_data_funnel";
		
	public static final String BEHAVIORAL_METRICS = "behavioral_metrics";
	
	public final void initDataFunnelSchema(boolean forceUpdate) {

		// Customer Data Funnel
		List<DataFlowStage> defaultCustomerStages = new ArrayList<DataFlowStage>();
		defaultCustomerStages.add(FUNNEL_STAGE_VISITOR);
		defaultCustomerStages.add(FUNNEL_STAGE_RETURNNING_VISITOR);
		defaultCustomerStages.add(FUNNEL_STAGE_LEAD);

		defaultCustomerStages.add(FUNNEL_STAGE_PROSPECT);
		defaultCustomerStages.add(FUNNEL_STAGE_NEW_CUSTOMER);
		defaultCustomerStages.add(FUNNEL_STAGE_ENGAGED_CUSTOMER);
		
		defaultCustomerStages.add(FUNNEL_STAGE_HAPPY_CUSTOMER);
		defaultCustomerStages.add(FUNNEL_STAGE_CUSTOMER_ADVOCATE);
		
		defaultCustomerStages.add(FUNNEL_STAGE_UNHAPPY_CUSTOMER);
		defaultCustomerStages.add(FUNNEL_STAGE_TERMINATED_CUSTOMER);

		
		// insert new data
		for (DataFlowStage flowStage : defaultCustomerStages) {
			String id = DataFlowStageDaoUtil.save(flowStage, forceUpdate);
			System.out.println("Saved Ok " + flowStage.getName() + " " + id);
		}
	}
	
	public void initEventMetricMetaData(boolean forceUpdate) {
		String flowName = GENERAL_BUSINESS_EVENT;
		List<EventMetric> eventMetrics = new ArrayList<EventMetric>();
		
		EventMetric dataImported = new EventMetric(flowName, BehavioralEvent.STR_DATA_IMPORT, "Data Import", 0,
				EventMetric.SCORING_DATA_QUALITY_METRIC, EventMetric.THIRD_PARTY_DATA, FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_AWARENESS, true);
		eventMetrics.add(dataImported);
		
		EventMetric adImpression = new EventMetric(flowName, BehavioralEvent.STR_AD_IMPRESSION, "Ad Impression", 1,
				EventMetric.SCORING_ACQUISITION_METRIC, EventMetric.THIRD_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_AWARENESS, true);
		adImpression.setShowInObserverJS(true);
		eventMetrics.add(adImpression);

		// STAGE_NEW_VISITOR
		EventMetric pageView = new EventMetric(flowName, BehavioralEvent.STR_PAGE_VIEW, "Page View", 1,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_AWARENESS, true);
		pageView.setShowInObserverJS(true);
		eventMetrics.add(pageView);
		
		// User accept tracking must be an action
		EventMetric acceptTracking = new EventMetric(flowName, BehavioralEvent.STR_ACCEPT_TRACKING, "Accept Tracking", 1,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_ATTRACTION, true);
		acceptTracking.setShowInObserverJS(true);
		eventMetrics.add(acceptTracking);
		
		// Key event metric to measure User engagement
		EventMetric engagedSession = new EventMetric(flowName, BehavioralEvent.STR_ENGAGED_SESSION, "Engaged Session", 2,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_ATTRACTION, true);
		engagedSession.setShowInObserverJS(true);
		eventMetrics.add(engagedSession);
		
		EventMetric like = new EventMetric(flowName, BehavioralEvent.STR_LIKE, "Like", 3,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_ACTION, true);
		like.setShowInObserverJS(true);
		eventMetrics.add(like);
		
		EventMetric unlike = new EventMetric(flowName, BehavioralEvent.STR_UNLIKE, "Unlike", -3,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_ACTION, true);
		like.setShowInObserverJS(true);
		eventMetrics.add(unlike);

		EventMetric contentView = new EventMetric(flowName, BehavioralEvent.STR_CONTENT_VIEW, "Content View", 4,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_ATTRACTION, true);
		contentView.setShowInObserverJS(true);
		eventMetrics.add( contentView);
		
		EventMetric search = new EventMetric(flowName, BehavioralEvent.STR_SEARCH, "Search", 5,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_ASK, true);
		search.setShowInObserverJS(true);
		eventMetrics.add( search);

		EventMetric itemView = new EventMetric(flowName, BehavioralEvent.STR_ITEM_VIEW, "Item View", 6,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_ATTRACTION, true);
		itemView.setShowInObserverJS(true);
		eventMetrics.add(itemView);
		
		eventMetrics.add(new EventMetric(flowName, "survey-view", "Survey View", 7,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_ATTRACTION, true));
		
		EventMetric clickDetails = new EventMetric(flowName, "click-details", "Click Details", 8,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_ASK, true);
		clickDetails.setShowInObserverJS(true);
		eventMetrics.add(clickDetails);
		
		
		EventMetric playVideo = new EventMetric(flowName, BehavioralEvent.STR_PLAY_VIDEO, "Play Video", 9,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_VISITOR,EventMetric.JOURNEY_STAGE_ASK, true);
		playVideo.setShowInObserverJS(true);
		eventMetrics.add(playVideo);
		
		// STAGE_LEAD
		EventMetric userSendText = new EventMetric(flowName, BehavioralEvent.STR_USER_SEND_TEXT, "User Send Text", 10,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_LEAD,EventMetric.JOURNEY_STAGE_ASK, true);
		eventMetrics.add(userSendText);
		
		// admin
		EventMetric adminView = new EventMetric(flowName, BehavioralEvent.STR_ADMIN_VIEW, "Admin View", 10,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_LEAD,EventMetric.JOURNEY_STAGE_ACTION, true);
		adminView.setShowInObserverJS(true);
		eventMetrics.add(adminView);

		EventMetric submitContact = new EventMetric(flowName, BehavioralEvent.STR_SUBMIT_CONTACT, "Submit Contact",10, 
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_LEAD, EventMetric.JOURNEY_STAGE_ACTION, true);
		submitContact.setShowInObserverJS(true);
		eventMetrics.add(submitContact);
		
		EventMetric follow = new EventMetric(flowName, BehavioralEvent.STR_FOLLOW, "Follow",10, 
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_LEAD, EventMetric.JOURNEY_STAGE_ACTION, true);
		eventMetrics.add(follow);
		
		EventMetric unfollow = new EventMetric(flowName, BehavioralEvent.STR_UNFOLLOW, "Unfollow",-10, 
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_LEAD, EventMetric.JOURNEY_STAGE_ACTION, true);
		eventMetrics.add(unfollow);
		
		EventMetric fileDownload = new EventMetric(flowName, BehavioralEvent.STR_FILE_DOWNLOAD, "File Download", 10,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_LEAD,EventMetric.JOURNEY_STAGE_ACTION, true);
		fileDownload.setShowInObserverJS(true);
		eventMetrics.add(fileDownload);
		
		EventMetric registerAccount = new EventMetric(flowName, "register-account", "Register Account",11, 
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_LEAD, EventMetric.JOURNEY_STAGE_ACTION, true);
		registerAccount.setShowInObserverJS(true);
		eventMetrics.add(registerAccount);
		
		EventMetric userLogin = new EventMetric(flowName, BehavioralEvent.STR_USER_LOGIN, "User Login",12, 
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_LEAD, EventMetric.JOURNEY_STAGE_ACTION, true);
		userLogin.setShowInObserverJS(true);
		eventMetrics.add(userLogin);
		
		EventMetric shortLinkClick = new EventMetric(flowName, BehavioralEvent.STR_SHORT_LINK_CLICK, "Short Link Click", 14,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_LEAD, EventMetric.JOURNEY_STAGE_ASK, true);
		shortLinkClick.setShowInObserverJS(true);
		eventMetrics.add(shortLinkClick);
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_QR_CODE_SCAN, "QR Code Scan", 14,
				EventMetric.SCORING_LEAD_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_LEAD, EventMetric.JOURNEY_STAGE_ASK, true));
		
		// STAGE_PROSPECT
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_NOTIFICATION_CLICK, "Notification Click", 15,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ASK, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_SMS_CLICK, "SMS Click", 16,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ASK, true));

		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_EMAIL_CLICK, "Email Click", 16,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ASK, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_JOIN_WORKSHOP, "Join Workshop", 17,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ASK, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_JOIN_WEBINAR, "Join Webinar", 18,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ASK, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_JOIN_COMMUNITY, "Join Community", 19,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ASK, true));
				
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_CHECK_IN, "Check In", 20,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ASK, true));
		
		EventMetric askQuestion = new EventMetric(flowName, BehavioralEvent.STR_ASK_QUESTION, "Ask Question", 21,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA, FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ASK, true);
		askQuestion.setShowInObserverJS(true);
		eventMetrics.add(askQuestion);
		
		eventMetrics.add( new EventMetric(flowName, BehavioralEvent.STR_CHAT_FOR_SUPPORT, "Chat for Support", 22, 
				EventMetric.SCORING_FEEDBACK_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_PROSPECT, EventMetric.JOURNEY_STAGE_ASK, true));
				
		EventMetric productTrial = new EventMetric(flowName, BehavioralEvent.STR_PRODUCT_TRIAL, "Product Trial", 23,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ASK, true);
		productTrial.setShowInObserverJS(true);
		eventMetrics.add(productTrial);
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_ADD_WISHLIST, "Add Wishlist", 24,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ACTION, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_ADD_TO_CART, "Add To Cart", 24,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ACTION, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_REMOVE_FROM_CART, "Remove From Cart", 0,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ACTION, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_APPLY_LOAN, "Apply Loan", 25,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ACTION, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_BOOKING, "Booking", 25,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT,EventMetric.JOURNEY_STAGE_ACTION, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_ORDER_CHECKOUT, "Order Checkout", 26,
				EventMetric.SCORING_PROSPECT_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_PROSPECT, EventMetric.JOURNEY_STAGE_ACTION, true));
		
		int cumulativePoint = 1;

		// STAGE_NEW_CUSTOMER
		EventMetric approveLoan = new EventMetric(flowName, BehavioralEvent.STR_APPROVE_LOAN, "Approve Loan", 27,
				EventMetric.SCORING_LIFETIME_VALUE_METRIC, EventMetric.FIRST_PARTY_DATA, FunnelMetaData.STAGE_NEW_CUSTOMER,cumulativePoint,EventMetric.JOURNEY_STAGE_ACTION, true);
		approveLoan.setShowInObserverJS(false);
		eventMetrics.add(approveLoan);
		
		EventMetric purchase = new EventMetric(flowName, BehavioralEvent.STR_PURCHASE, "Purchase", 27,
				EventMetric.SCORING_LIFETIME_VALUE_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_CUSTOMER,cumulativePoint,EventMetric.JOURNEY_STAGE_ACTION, true);
		purchase.setShowInObserverJS(true);
		eventMetrics.add(purchase);
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_ENROLL, "Enroll", 28,
				EventMetric.SCORING_LIFETIME_VALUE_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_CUSTOMER,cumulativePoint,EventMetric.JOURNEY_STAGE_ACTION, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_SUBSCRIBE, "Subscribe", 29,
				EventMetric.SCORING_LIFETIME_VALUE_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_CUSTOMER,cumulativePoint,EventMetric.JOURNEY_STAGE_ACTION, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_MADE_PAYMENT, "Made Payment", 30,
				EventMetric.SCORING_LIFETIME_VALUE_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_NEW_CUSTOMER,cumulativePoint,EventMetric.JOURNEY_STAGE_ACTION, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_SUBMIT_FEEDBACK_FORM, "Submit Feedback Form",31, 
				EventMetric.SCORING_FEEDBACK_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_ENGAGED_CUSTOMER, cumulativePoint,EventMetric.JOURNEY_STAGE_ADVOCACY, true));
		
		eventMetrics.add( new EventMetric(flowName, BehavioralEvent.STR_SUBMIT_RATING_FORM, "Submit Rating Form", 32, 
				EventMetric.SCORING_FEEDBACK_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_ENGAGED_CUSTOMER, cumulativePoint,EventMetric.JOURNEY_STAGE_ADVOCACY, true));
		
		eventMetrics.add( new EventMetric(flowName, BehavioralEvent.STR_SUBMIT_COMMENT_FORM, "Submit Comment Form", 33, 
				EventMetric.SCORING_FEEDBACK_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_ENGAGED_CUSTOMER, cumulativePoint,EventMetric.JOURNEY_STAGE_ADVOCACY, true));
		
		eventMetrics.add(new EventMetric(flowName, BehavioralEvent.STR_REPURCHASE, "Repurchase", 34,
				EventMetric.SCORING_LIFETIME_VALUE_METRIC, EventMetric.FIRST_PARTY_DATA, FunnelMetaData.STAGE_ENGAGED_CUSTOMER,cumulativePoint,EventMetric.JOURNEY_STAGE_ACTION, true));
		
		
		// STAGE_ENGAGED_CUSTOMER
		cumulativePoint = 2;

		eventMetrics.add( new EventMetric(flowName, BehavioralEvent.STR_SUBMIT_CES_FORM, "Submit CES Form", 35, 
				EventMetric.SCORING_EFFORT_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_ENGAGED_CUSTOMER, cumulativePoint,EventMetric.JOURNEY_STAGE_ADVOCACY, true));
		
		eventMetrics.add( new EventMetric(flowName, BehavioralEvent.STR_SUBMIT_CSAT_FORM, "Submit CSAT Form", 36, 
				EventMetric.SCORING_SATISFACTION_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_ENGAGED_CUSTOMER, cumulativePoint,EventMetric.JOURNEY_STAGE_ADVOCACY, true));
		
		eventMetrics.add( new EventMetric(flowName, BehavioralEvent.STR_SUBMIT_NPS_FORM, "Submit NPS Form", 37, 
				EventMetric.SCORING_PROMOTER_METRIC, EventMetric.FIRST_PARTY_DATA,   FunnelMetaData.STAGE_ENGAGED_CUSTOMER, cumulativePoint,EventMetric.JOURNEY_STAGE_ADVOCACY, true));	
	
		//  CX metrics, can customer go to the loyalty loop or churn ?
		cumulativePoint = 3;
		eventMetrics.add(new EventMetric(flowName, "positive-feedback", "Positive Feedback", 42,
				EventMetric.SCORING_PROMOTER_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_HAPPY_CUSTOMER,cumulativePoint,EventMetric.JOURNEY_STAGE_ADVOCACY));

		eventMetrics.add(new EventMetric(flowName, "negative-feedback", "Negative Feedback", -40,
				EventMetric.SCORING_DETRACTOR_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_UNHAPPY_CUSTOMER, -1,EventMetric.JOURNEY_STAGE_ADVOCACY));

		eventMetrics.add(new EventMetric(flowName, "positive-social-review", "Positive Social Review", 45,
				EventMetric.SCORING_PROMOTER_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_CUSTOMER_ADVOCATE,3,EventMetric.JOURNEY_STAGE_ADVOCACY));

		eventMetrics.add(new EventMetric(flowName, "negative-social-review", "Negative Social Review", -44,
				EventMetric.SCORING_DETRACTOR_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_UNHAPPY_CUSTOMER, -1,EventMetric.JOURNEY_STAGE_ADVOCACY));
	
		eventMetrics.add(new EventMetric(flowName, "viral-positive-review", "Viral Positive Review", 50,
				EventMetric.SCORING_PROMOTER_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_CUSTOMER_ADVOCATE, 5,EventMetric.JOURNEY_STAGE_ADVOCACY));
		
		eventMetrics.add(new EventMetric(flowName, "product-return", "Product Return", -33,
				EventMetric.SCORING_DETRACTOR_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_TERMINATED_CUSTOMER, -3, EventMetric.JOURNEY_STAGE_ACTION));
		
		eventMetrics.add(new EventMetric(flowName, "unsubscribe-service", "Unsubscribe Service", -44,
				EventMetric.SCORING_DETRACTOR_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_TERMINATED_CUSTOMER, -4, EventMetric.JOURNEY_STAGE_ACTION));
		
		eventMetrics.add(new EventMetric(flowName, "cancel-contract", "Cancel Contract", -55,
				EventMetric.SCORING_DETRACTOR_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_TERMINATED_CUSTOMER, -5, EventMetric.JOURNEY_STAGE_ACTION));
		
		eventMetrics.add(new EventMetric(flowName, "viral-negative-review", "Viral Negative Review", -66,
				EventMetric.SCORING_DETRACTOR_METRIC, EventMetric.FIRST_PARTY_DATA,  FunnelMetaData.STAGE_UNHAPPY_CUSTOMER, -6, EventMetric.JOURNEY_STAGE_ADVOCACY));
		
		
		// insert new
		for (EventMetric metric : eventMetrics) {
			EventMetricDaoUtil.save(metric, forceUpdate);
		}
	}
	
	public void init(boolean forceUpdate) {
		this.initDataFunnelSchema(forceUpdate);
		this.initEventMetricMetaData(forceUpdate);
	}
	
	public void init() {
		this.initDataFunnelSchema(false);
		this.initEventMetricMetaData(false);
	}
	
	public static void initDefaultSystemData() {
		new JourneyFlowSchema().init();
    }
	
	public static void upgradeDefaultSystemData(boolean forceUpdate) {
		new JourneyFlowSchema().init(forceUpdate);
    }

}
