package test.persistence;

import java.io.IOException;

import leotech.cdp.domain.AssetCategoryManagement;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.model.SystemUser;
import rfx.core.util.Utils;

public class CreateCoreCategories {

	public static void main(String[] args) {
		SystemUser root = SystemUserDaoUtil.getByUserLogin("superadmin");
		try {
			AssetCategoryManagement.initDefaultSystemData(root);
			
			Utils.exitSystemAfterTimeout(1000);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
