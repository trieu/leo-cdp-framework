package test.cdp.profile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import leotech.cdp.dao.ProfileDaoUtil;

public class TestProfileDefaultExtAttributes {

	public static void main(String[] args) {
		Map<String, Object> extAttributes = new HashMap<String, Object>();
		extAttributes.put("str", "test");
		extAttributes.put("code", 1);
		extAttributes.put("demo", true);
		extAttributes.put("test", new Date());
		ProfileDaoUtil.saveProfileExtAttributes(extAttributes);
	}
}
