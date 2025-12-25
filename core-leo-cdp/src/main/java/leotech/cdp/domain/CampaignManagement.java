package leotech.cdp.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.dao.CampaignDaoUtil;
import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.dao.graph.GraphProfile2Product;
import leotech.cdp.model.activation.ActivationRuleType;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.AssetType;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.marketing.Campaign;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.cdp.query.filters.CampaignFilter;
import leotech.cdp.utils.ProfileDataValidator;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.util.LogUtil;
import leotech.system.util.TaskRunner;
import rfx.core.util.FileUtils;
import rfx.core.util.StringUtil;

/**
 * Data Activation Campaign
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class CampaignManagement {
	static final String DEFAULT_MARKETING_FLOW_TEMPLATE = "./resources/marketing-flow-templates/default-flow-template.json";
	protected final static int PROFILE_PROCESS_BATCH_SIZE = 100;

	static Map<String, ProductItem> getEcommerceProducts() {
		List<ProductItem> items = AssetProductItemDaoUtil.list(0, 100);
		Map<String, ProductItem> map = new HashMap<String, ProductItem>(items.size());
		for (ProductItem productItem : items) {
			map.put(productItem.getProductId(),productItem);
		}
		return map;
	}

	// TODO
	public static void doMarketingAutomation(Profile profile) {
		String profileId = profile.getId();
		
		String toEmailAddress =  profile.getPrimaryEmail();
		String name = profile.getFirstName();
		String visitorId = profile.getVisitorId();
		
		// rules checking
		
		boolean check = ProfileDataValidator.isValidEmail(profile.getPrimaryEmail()) && profile.getTotalProspectScore() > 0;
		if( ! check) {
			// SKIP
			return;
		}

		String templateUS = "Hi {{profile.firstName}} , you may like this product {{#if targetMediaUnit}} {{targetMediaUnit.landingPageName}} {{targetMediaUnit.landingPageUrl}} {{/if}} !";
		AssetTemplate contentTemplateUS = new AssetTemplate("cate1","page1",ActivationRuleType.PRODUCT_RECOMMENDATION_EMAIL, AssetType.EMAIL_CONTENT, "junit", "Product you may like", templateUS);
	
		List<TargetMediaUnit> rsList = GraphProfile2Product.getRecommendedProductItemsForUser(profile.getId(), 0, 10, true);
		
		if( ! rsList.isEmpty() ) {
			TargetMediaUnit mediaUnit = rsList.get(0);
			System.out.println(mediaUnit);
			
			String clickThrough = mediaUnit.getTrackingLinkUrl();
			
			String productName = mediaUnit.getLandingPageName();
			double price = mediaUnit.getProductItem().getSalePrice();
			
			ActivationFlowManagement.sendRecommendationByEmail(contentTemplateUS, profileId, toEmailAddress, name, productName, price, clickThrough);
			
			// FIXME need to check config of segment
			// realtimeMarketingActions(profile, toPhone, productName, productLink);
		}
	}

	static void realtimeMarketingActions(Profile profile, String toPhone, String productName, String productLink) {
		// Push Message
		String heading = "You may like this product ";
		String oneSignalPlayerId = "";
		Set<String> userIds = profile.getNotificationUserIds().getOrDefault("onesignal", new HashSet<String>());
		if( ! userIds.isEmpty()) {
			oneSignalPlayerId = userIds.iterator().next();
		}
		ActivationFlowManagement.sendRecommendationByPushMessage(oneSignalPlayerId, productName, productLink, heading);
		
		if(StringUtil.isNotEmpty(toPhone)) {
			if(toPhone.length() > 8) {
				//MobileSmsSender.send(toPhone, "You may like " + productName + " at " + productLink );
			}
		}
	}
	
	// web admin handlers
	
	public static List<Campaign> list(int startIndex, int numberResult) {
		return CampaignDaoUtil.listCampaigns(startIndex, numberResult);
	}

	public static JsonDataTablePayload filter(CampaignFilter filter) {
		JsonDataTablePayload payload = CampaignDaoUtil.filterCampaigns(filter);
		return payload;
	}
	
	public static Campaign getById(String id) {
		Campaign queriedCampaign = CampaignDaoUtil.getCampaignById(id);
		return queriedCampaign;
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static String saveCampaign(String id) {
		Campaign campaign = new Campaign();
		String createdId = CampaignDaoUtil.saveCampaign(campaign);
		return createdId;
	}

	public static Campaign newCampaign() {
		Campaign campaign = new Campaign();
		String json = "";
		try {
			json = FileUtils.readFileAsString(DEFAULT_MARKETING_FLOW_TEMPLATE);
		} catch (IOException e) {
			System.err.println("Not found " + DEFAULT_MARKETING_FLOW_TEMPLATE);
			e.printStackTrace();
		}
		campaign.setAutomatedFlowJson(json);
		return campaign;
	}

	/**
	 * @param json
	 * @return
	 */
	public static String saveCampaignFromJson(String json) {
		Campaign campaign = new Gson().fromJson(json, Campaign.class);
		String savedId = CampaignDaoUtil.saveCampaign(campaign);
		return savedId;
	}

	/**
	 * @param id
	 * @return
	 */
	public static String removeCampaign(String id) {
		return updateCampaignStatus(id, Campaign.STATUS_REMOVED);
	}

	/**
	 * @param campaignId
	 * @param status
	 * @return
	 */
	public static String updateCampaignStatus(String campaignId, int status) {
		Campaign campaign = CampaignDaoUtil.getCampaignById(campaignId);
		if(campaign != null) {
			campaign.setStatus(status);
		}
		return null;
	}

	public static Campaign queryActiveCampaign(String touchpointId, String visitorId) {
		
		return null;
	}
	
	/**
	 * @param campaignId
	 */

	public static void runCampaign(String campaignId) {
		Campaign cam = CampaignDaoUtil.getCampaignById(campaignId);
		if(cam != null) {
			
			TaskRunner.run(()->{
				Segment targetSegment = SegmentDaoUtil.getSegmentById(cam.getTargetSegmentId());
				SegmentDataManagement.processProfilesInSegment(targetSegment, PROFILE_PROCESS_BATCH_SIZE, profile ->{
					// TODO 
				});
			});
		}
		else {
			LogUtil.logError(CampaignManagement.class, "Not found any campaign with id: " +campaignId );
		}
	}
	

}
