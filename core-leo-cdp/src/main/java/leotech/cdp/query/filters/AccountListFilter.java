package leotech.cdp.query.filters;

import io.vertx.core.json.JsonObject;
import leotech.system.model.SystemUser;
import rfx.core.util.StringUtil;

/**
 * to filter segments in database
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class AccountListFilter extends DataFilter {

	String accountId;

	public AccountListFilter() {
		this.start = 0;
		this.length = 0;
	}

	public boolean isFilteringAndGetAll() {
		return start == 0 && length == 0 && StringUtil.isEmpty(uri);
	}

	public AccountListFilter(SystemUser systemUser, String uri, JsonObject paramJson) {
		super(systemUser, uri, paramJson);
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

}
