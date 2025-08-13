package leotech.cdp.handler.admin;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.BusinessAccountManagement;
import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.domain.ProfileQueryManagement;
import leotech.cdp.model.customer.BusinessAccount;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.query.filters.AccountListFilter;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;

/**
 * Business Account handler for admin
 * 
 * @author tantrieuf31
 * @since 2022/03/24
 *
 */
public final class BusinessAccountHandler extends SecuredHttpDataHandler {

	// for dataList view
	static final String LIST_WITH_FILTER = "/cdp/account/filter";
	static final String EXPORT_CSV_PROFILES = "/cdp/account/export-csv";
	static final String EXPORT_JSON_PROFILES = "/cdp/account/export-json";
	
	// for dataList view
	static final String SAVE_ACCOUNT = "/cdp/account/save";
	static final String LOAD_ACCOUNT_INFO = "/cdp/account/load";
	static final String LOAD_PROFILES_IN_ACCOUNT = "/cdp/account/profiles";
	
	static final String LOAD_ACCOUNT_STATISTICS = "/cdp/account/statistics";
	static final String DELETE_ACCOUNT = "/cdp/account/delete";
	
	public BusinessAccountHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	/**
	 * HTTP POST handler
	 */
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			switch (uri) {
				// list
				case LIST_WITH_FILTER: {
					return loadAccounts(uri, paramJson, loginUser);
				}
				// get profiles
				case LOAD_PROFILES_IN_ACCOUNT: {
					return loadProfilesInAccount(uri, paramJson, loginUser);
				}
				// statistics
				case LOAD_ACCOUNT_STATISTICS: {
					return loadAccountStatistics(uri, paramJson, loginUser);
				}
				// info
				case LOAD_ACCOUNT_INFO: {
					return loadAccountInfo(uri, paramJson, loginUser);
				}
				// save
				case SAVE_ACCOUNT: {
					try {
						String json = paramJson.getString("dataObject", "{}");
						String accountId = BusinessAccountManagement.saveFromJson(loginUser, json);
						if (accountId != null) {
							return JsonDataPayload.ok(uri, accountId, loginUser, BusinessAccount.class);
						} else {
							return JsonDataPayload.fail("failed to save BusinessAccount data into database", 500);
						}
					} catch (Exception e) {
						return JsonDataPayload.fail(e.getMessage(), 500);
					}
				}
				// delete
				case DELETE_ACCOUNT: {
					try {
						String id = paramJson.getString("id", "");
						boolean deleteAllProfiles = paramJson.getBoolean("deleteAllProfiles",false);
						boolean rs = BusinessAccountManagement.delete(loginUser, id, deleteAllProfiles);
						return JsonDataPayload.ok(uri, rs, loginUser, BusinessAccount.class);
					} catch (Exception e) {
						return JsonDataPayload.fail(e.getMessage(), 500);
					}
				}
				default: {
					return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			}
		}
		else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}


	/**
	 * HTTP GET handler
	 */
	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			switch (uri) {
				case LOAD_ACCOUNT_INFO: {
					return loadSingleAccount(uri, params, loginUser);
				}
				case EXPORT_CSV_PROFILES : {
					return exportProfilesAsCsv(uri, params, loginUser);
				}
				case EXPORT_JSON_PROFILES : {
					return exportProfilesAsJson(uri, params, loginUser);
				}
				default:
					return JsonErrorPayload.NO_HANDLER_FOUND;
				}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}


	/**
	 * data exporting as JSON
	 */
	JsonDataPayload exportProfilesAsJson(String uri, MultiMap params, SystemUser loginUser) {
		String accountId = HttpWebParamUtil.getString(params,"accountId", "");
		String exportedFileUri = "";
		if (!accountId.isEmpty()) {
			exportedFileUri = BusinessAccountManagement.exportAllProfilesInJSON(accountId, loginUser.getKey());
		}
		return JsonDataPayload.ok(uri, exportedFileUri, loginUser, Profile.class);
	}


	/**
	 * data exporting as CSV
	 */
	JsonDataPayload exportProfilesAsCsv(String uri, MultiMap params, SystemUser loginUser) {
		String accountId = HttpWebParamUtil.getString(params,"accountId", "");
		int csvType = HttpWebParamUtil.getInteger(params, "csvType", 0);
		String exportedFileUri = "";
		if (!accountId.isEmpty()) {
			exportedFileUri = BusinessAccountManagement.exportAllProfilesInCSV(csvType, accountId, loginUser.getKey());
		}
		return JsonDataPayload.ok(uri, exportedFileUri, loginUser, Profile.class);
	}


	JsonDataPayload loadSingleAccount(String uri, MultiMap params, SystemUser loginUser) {
		String id = HttpWebParamUtil.getString(params, "id", "new");
		BusinessAccount sm;
		if (id.equals("new")) {
			sm = BusinessAccountManagement.newAccount();
		} else {
			sm = BusinessAccountManagement.getById(id);
		}
		return JsonDataPayload.ok(uri, sm, loginUser, BusinessAccount.class);
	}
	
	/**
	 * load a single account for reporting, analytics and data administration
	 */
	JsonDataPayload loadAccountInfo(String uri, JsonObject paramJson, SystemUser loginUser) {
		String id = paramJson.getString("id", "new");
		BusinessAccount account;
		if (id.equals("new")) {
			account = BusinessAccountManagement.newAccount();
		} else {
			account = BusinessAccountManagement.getById(id);
		}
		if(account != null) {
			long totalProfilesInSystem = ProfileQueryManagement.countTotalOfProfiles();
			Map<String, Long> stats = ImmutableMap.of("totalProfilesInAccount", account.getTotalProfile(),"totalProfilesInSystem", totalProfilesInSystem);
			
			// event metric key for account builder UI
			Collection<EventMetric> metrics = EventMetricManagement.getAllSortedEventMetrics();
			Map<String, String> behavioralEventMap = new TreeMap<String, String>();
			metrics.forEach(m -> {
				behavioralEventMap.put(m.getId(), m.getEventLabel());
			});
			Map<String, Object> data = ImmutableMap.of("accountData", account, "accountStats", stats, "behavioralEventMap", behavioralEventMap);
			JsonDataPayload result = JsonDataPayload.ok(uri, data, loginUser, BusinessAccount.class);
			boolean checkToEditAccount = loginUser.checkToEditAccount(account);
			result.setCanEditData(checkToEditAccount);
			result.setCanDeleteData(checkToEditAccount);
			return result;
		}
		return JsonDataPayload.fail("BusinessAccount is not found, invalid account ID: " + id, 404);
	}
	

	JsonDataPayload loadAccounts(String uri, JsonObject paramJson, SystemUser loginUser) {
		// the list-view component at datatables.net needs POST method to avoid long URL
		AccountListFilter filter = new AccountListFilter(loginUser, uri, paramJson);
		JsonDataTablePayload payload = BusinessAccountManagement.filter(filter);
		payload.setUserLoginPermission(loginUser, BusinessAccount.class);
		return payload;
	}


	JsonDataPayload loadProfilesInAccount(String uri, JsonObject paramJson, SystemUser loginUser) {
		AccountListFilter filter = new AccountListFilter(loginUser, uri, paramJson);
		JsonDataTablePayload payload = BusinessAccountManagement.getProfilesInAccount(filter);
		payload.setUserLoginPermission(loginUser, BusinessAccount.class);
		return payload;
	}

	JsonDataPayload loadAccountStatistics(String uri, JsonObject paramJson, SystemUser loginUser) {
		String accountId = paramJson.getString("accountId", "");
		String beginFilterDate = paramJson.getString("beginFilterDate", "");
		String endFilterDate = paramJson.getString("endFilterDate", "");

		long querySize = BusinessAccountManagement.countTotalProfile(accountId, beginFilterDate, endFilterDate);
		long totalProfilesInSystem = ProfileQueryManagement.countTotalOfProfiles();
		Map<String, Long> stats = ImmutableMap.of("totalProfilesInAccount", querySize, "totalProfilesInSystem",totalProfilesInSystem);
		return JsonDataPayload.ok(uri, stats, loginUser, BusinessAccount.class);
	}

}