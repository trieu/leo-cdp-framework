package leotech.cdp.domain;

import java.util.List;

import leotech.cdp.dao.BusinessAccountDao;
import leotech.cdp.model.customer.BusinessAccount;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.query.filters.AccountListFilter;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;

/**
 * @author tantrieuf31
 * @since 2022/03/24
 *
 */
public class BusinessAccountManagement {

	public static JsonDataTablePayload filter(AccountListFilter filter) {
		JsonDataTablePayload list = BusinessAccountDao.filter(filter);
		return list;
	}

	public static long countTotalProfile(String accountId, String beginFilterDate, String endFilterDate) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload getProfilesInAccount(AccountListFilter filter) {
		List<Profile> profiles = null;
		long total = 0;
		long filtered = 0;
		int draw = 0;
		//TODO
		JsonDataTablePayload payload = JsonDataTablePayload.data(filter.getUri(), profiles, total, filtered, draw );
		return payload;
	}

	public static BusinessAccount newAccount() {
		BusinessAccount a = new BusinessAccount();
		return a;
	}

	public static BusinessAccount getById(String id) {
		return BusinessAccountDao.getById(id);
	}

	public static String exportAllProfilesInCSV(int csvType, String accountId, String key) {
		// TODO Auto-generated method stub
		return null;
	}

	public static String exportAllProfilesInJSON(String accountId, String key) {
		// TODO Auto-generated method stub
		return null;
	}

	public static boolean delete(SystemUser loginUser, String id, boolean deleteAllProfiles) {
		// TODO Auto-generated method stub
		return false;
	}

	public static String saveFromJson(SystemUser loginUser, String json) {
		// TODO Auto-generated method stub
		return null;
	}

	// TODO
}
