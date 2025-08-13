package leotech.ssp.adserver.service;

import java.util.ArrayList;
import java.util.List;

import leotech.cdp.domain.AssetItemManagement;
import leotech.cdp.domain.CampaignManagement;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.marketing.Campaign;
import leotech.ssp.adserver.model.AdType;
import leotech.ssp.adserver.model.DisplayAdData;
import rfx.core.util.StringUtil;

public class AdsQueryService {
	
	public static List<DisplayAdData> getAds(List<String> touchpointIds, String visitorId, String srcTouchpointUrl){
		
		List<DisplayAdData> ads = new ArrayList<>(touchpointIds.size());
		
		//TODO generate srcTouchpointId
		
		for (String touchpointId : touchpointIds) {
			DisplayAdData ad = getDisplayAdData(touchpointId, visitorId);
			ads.add(ad);
		}
		
		return ads;
	}
	
	public static DisplayAdData getDisplayAdData(String touchpointId, String visitorId) {
		// get running remarketing campaign
		Campaign campaign = CampaignManagement.queryActiveCampaign(touchpointId, visitorId);
		
		String productId = "";
		// get last action
		TrackingEvent lastAction = ProfileDataManagement.getLastTrackingEventForRetargeting(visitorId);
		if(lastAction != null) {
			//FIXME remove fake SKU data later
			System.out.println(" [lastAction] " + lastAction);
			String productIds = lastAction.getEventData().getOrDefault("productIds", "").toString();
			if(StringUtil.isNotEmpty(productIds)) {
				String[] toks = productIds.split(",");
				productId = toks[0];
			}
		}
		
		int adType = AdType.ADTYPE_IMAGE_DISPLAY_AD;
		int width = 324;
		int height = 324;
		
		if(StringUtil.isNotEmpty(productId) && campaign != null) {
			String campaignId = campaign.getId();
			ProductItem item = AssetItemManagement.getProductItemById(productId);
			System.out.println("getDisplayAdData productId " + productId);
			if(item != null) {
				String adLabel = item.getTitle();
				String mediaFullUrl = item.getHeadlineImageUrl();
				String clickThrough;
				if(item.getFullUrl().contains("#")) {
					clickThrough = item.getFullUrl() + "leoadscp=" + campaignId;
				} else {
					clickThrough = item.getFullUrl() + "#leoadscp=" + campaignId;
				}
				
				DisplayAdData ad = new DisplayAdData(touchpointId, mediaFullUrl , clickThrough , adLabel , campaignId , adType , width, height);
				//TODO generate becon here
				String beacon = "_demobeacon_";
				
				ad.setBeaconData(beacon );
				
				return ad;
			}
		}
		
		return null;
	}
}
