package test.cdp.singleview;

import java.util.ArrayList;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.model.customer.ProfileSingleView;
import rfx.core.util.Utils;

public class TestRecomputeProfileScore {

	public static void main(String[] args) {
		String profileId = "Gp8G79TWYowmkR7EhOfBQ";
		ProfileSingleView profile = ProfileDaoUtil.getProfileById(profileId);
		if (profile != null) {
			ProfileDataManagement.updateProfileFromEvents(profile, true, new ArrayList<>(0));
		}
		
		Utils.exitSystemAfterTimeout(15000);
	}
}
