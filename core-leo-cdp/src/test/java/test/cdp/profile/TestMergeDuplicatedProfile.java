package test.cdp.profile;

import java.util.ArrayList;
import java.util.List;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.ProfileMergeService;
import leotech.cdp.domain.TouchpointHubManagement;
import leotech.cdp.domain.schema.CustomerFunnel;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.system.model.ImportingResult;
import leotech.system.util.UrlUtil;
import rfx.core.util.Utils;

public class TestMergeDuplicatedProfile {
	static String importFileUrl = "/home/thomas/0-github/leo-cdp/core-leo-cdp/data-of-real-customer/combined_2022.csv";
	static String importedTouchpointName = "shopee";
	static String touchpointUrl = "https://shopee.vn";
	static String hostname = UrlUtil.getHostName(touchpointUrl);
	
	static TouchpointHub tpHub = TouchpointHubManagement.getByHostName(hostname);
	
	

	public static void main(String[] args) {
		String funnelId =  CustomerFunnel.FUNNEL_STAGE_NEW_CUSTOMER.getId();
		String observerId = "2r78tgCmj0hH7EkyDTEU20";
		String dataLabelsStr = "Shopee HCM; Shopee 2022";
		boolean overwriteOldData = true;
		ImportingResult rs = ProfileDataManagement.importCsvAndSaveProfile(importFileUrl, observerId, dataLabelsStr, funnelId, overwriteOldData);
		System.out.println(rs);
		// new MergeDuplicateProfilesJob().doTheJob();
		
		Utils.exitSystemAfterTimeout(120000);
	}

	 static void testMergeSingleProfile() {
		Profile destProfile = ProfileDaoUtil.getByPrimaryEmail("trieu77777@gmail.com");
		List<Profile> listToBeUnified = new ArrayList<Profile>();
		listToBeUnified.add(ProfileDaoUtil.getByPrimaryEmail("tantrieuf31@gmail.com"));
		
		ProfileMergeService.mergeProfileData(destProfile, listToBeUnified);
	}
}
