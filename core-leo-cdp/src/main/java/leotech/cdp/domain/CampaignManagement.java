package leotech.cdp.domain;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;

import leotech.cdp.dao.CampaignDaoUtil;
import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.marketing.Campaign;
import leotech.cdp.query.filters.CampaignFilter;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.util.LogUtil;
import leotech.system.util.TaskRunner;
import rfx.core.util.FileUtils;

/**
 * Data Activation Campaign
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class CampaignManagement {

	protected final static int PROFILE_PROCESS_BATCH_SIZE = 100;

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
		try {
			String json = FileUtils.readFileAsString("./resources/marketing-flow-templates/default-flow-template.json");
			campaign.setAutomatedFlowJson(json);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
		if (campaign != null) {
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
		if (cam != null) {

			TaskRunner.run(() -> {
				Segment targetSegment = SegmentDaoUtil.getSegmentById(cam.getTargetSegmentId());
				SegmentDataManagement.processProfilesInSegment(targetSegment, PROFILE_PROCESS_BATCH_SIZE, profile -> {
					// TODO
				});
			});
		} else {
			LogUtil.logError(CampaignManagement.class, "Not found any campaign with id: " + campaignId);
		}
	}

}
