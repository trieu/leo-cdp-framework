package test.cdp.profile;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.model.customer.Profile;
import rfx.core.util.Utils;

public class TestSaveProfileAttributes {

	public static void main(String[] args) {
		Profile destProfile = ProfileDaoUtil.getByPrimaryEmail("contact@uspa.tech");
		
		String profileId = destProfile.getId();
		Map<String, Object> coreAttributes = new HashMap<String, Object>();
		coreAttributes.put("dataQualityScore", 100);
		coreAttributes.put("totalCLV", 100.23);
		coreAttributes.put("totalCAC", 50.87);
		
		Calendar cal = Calendar.getInstance();
		cal.set(1986, 1, 31);
		coreAttributes.put("dateOfBirth", cal.getTime());
		
		ProfileDaoUtil.saveProfileCoreAttributes(profileId, coreAttributes);
		
		Utils.exitSystemAfterTimeout(2000);
	}
}
