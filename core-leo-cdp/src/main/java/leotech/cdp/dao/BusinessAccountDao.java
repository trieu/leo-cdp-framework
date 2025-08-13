package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.customer.BusinessAccount;
import leotech.cdp.query.filters.AccountListFilter;
import leotech.system.config.AqlTemplate;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.util.TaskRunner;
import leotech.system.util.database.ArangoDbCommand;

/**
 * Business Account Data Access Object
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class BusinessAccountDao extends AbstractCdpDatabaseUtil  {
	
	static final String AQL_COUNT_TOTAL_ACTIVE_ACCOUNTS = "RETURN LENGTH(FOR s in "+ BusinessAccount.COLLECTION_NAME+" FILTER s.status >= 0 RETURN s._key)";
	
	static final String AQL_GET_ACCOUNTS_BY_PAGINATION = AqlTemplate.get("AQL_GET_ACCOUNTS_BY_PAGINATION");
	static final String AQL_COUNT_ACCOUNT_FOR_PAGINATION = AqlTemplate.get("AQL_COUNT_ACCOUNT_FOR_PAGINATION");
	
	static final String AQL_GET_ACCOUNT_BY_ID = AqlTemplate.get("AQL_GET_ACCOUNT_BY_ID");
	static final String AQL_GET_ACCOUNTS_TO_DELETE_FOREVER = AqlTemplate.get("AQL_GET_ACCOUNTS_TO_DELETE_FOREVER");
	
	/**
	 * create new account
	 * 
	 * @param account
	 * @return
	 */
	public static String create(BusinessAccount account) {
		if (account.dataValidation() ) {
			ArangoCollection col = account.getDbCollection();
			if (col != null) {
				// update total size of account
				
				long totalProfile  = 0;//TODO
				account.setTotalProfile(totalProfile);
				col.insertDocument(account);
				
				return account.getId();
			}
		}
		return null;
	}
	
	/**
	 * update account 
	 * 
	 * @param account
	 * @return
	 */
	public static String update(BusinessAccount account) {
		if(account != null) {
			if (account.dataValidation()) {
				ArangoCollection col = account.getDbCollection();
				if (col != null) {
					String segmentId = account.getId();
					// update time
					account.setUpdatedAt(new Date());
					// update total size of account
					long totalProfile  = 0;//TODO
					account.setTotalProfile(totalProfile);
					col.updateDocument(segmentId, account, getUpdateOptions());
					
					// update account metadata for all matched profiles
					TaskRunner.run(()->{
						
					});
					return segmentId;
				}
			}
		}
		return null;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static List<BusinessAccount> runFilterQuery(AccountListFilter filter) {
		ArangoDatabase db = getCdpDatabase();
		
		// dynamic query builder for filtering data
		String aql;
		
		Map<String, Object> bindVars = new HashMap<>(10);

		aql = AQL_GET_ACCOUNTS_BY_PAGINATION;
		bindVars.put("startIndex", filter.getStart());
		bindVars.put("numberResult", filter.getLength());
		bindVars.put("sortField", filter.getSortField("indexScore"));
		bindVars.put("sortDirection", filter.getSortDirection());
		bindVars.put("searchValue", filter.getFormatedSearchValue());
		
		// check permission
		bindVars.put("hasAdminRole", filter.hasAdminRole());
		bindVars.put("loginUsername", filter.getLoginUsername());
		
		ArangoDbCommand<BusinessAccount> q = new ArangoDbCommand<BusinessAccount>(db, aql , bindVars, BusinessAccount.class);
		List<BusinessAccount> list = q.getResultsAsList();
		return list;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload filter(AccountListFilter filter){
		int draw = filter.getDraw();
		List<BusinessAccount> list = runFilterQuery(filter);
		long recordsTotal = countTotal();
		long recordsFiltered = getTotalRecordsFiltered(filter);
		return JsonDataTablePayload.data(filter.getUri(), list, recordsTotal, recordsFiltered, draw);
	}
	
	/**
	 * @return
	 */
	public static long countTotal() {
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, AQL_COUNT_TOTAL_ACTIVE_ACCOUNTS, Long.class).getSingleResult();
		return c;
	}
	
	/**
	 * @param filter
	 * @return
	 */
	public static long getTotalRecordsFiltered(AccountListFilter filter) {
		String aql = AQL_COUNT_ACCOUNT_FOR_PAGINATION;
		
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("searchValue", filter.getFormatedSearchValue());
		bindVars.put("hasAdminRole", filter.hasAdminRole());
		bindVars.put("loginUsername", filter.getLoginUsername());
		bindVars.put("loginUsername", filter.getProfileVisitorId());
		
		ArangoDatabase db = getCdpDatabase();
		long c =  new ArangoDbCommand<Long>(db, aql, bindVars, Long.class).getSingleResult();
		return c;
	}
	
	/**
	 * @param id
	 * @return
	 */
	public static BusinessAccount getById(String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		BusinessAccount account = new ArangoDbCommand<BusinessAccount>(db, AQL_GET_ACCOUNT_BY_ID, bindVars, BusinessAccount.class).getSingleResult();
		if(account != null) {
			// update total size of account
			long totalProfile  = 0;//TODO
			account.setTotalProfile(totalProfile);
		}
		return account;
	}
	
	/**
	 * @return
	 */
	public static List<BusinessAccount> getAllRemovedSegmentsToDeleteForever() {
		ArangoDatabase db = getCdpDatabase();
		List<BusinessAccount> list = new ArangoDbCommand<BusinessAccount>(db, AQL_GET_ACCOUNTS_TO_DELETE_FOREVER, BusinessAccount.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param account
	 * @return
	 */
	public static boolean delete(BusinessAccount account) {
		ArangoCollection col = account.getDbCollection();
		if (col != null) {
			String accountId = account.getId();
			col.deleteDocument(accountId);
			
			// delete all RefKey of profile
			TaskRunner.run(()->{
				// TODO
			});
			
			return true;
		}
		return false;
	}
}
