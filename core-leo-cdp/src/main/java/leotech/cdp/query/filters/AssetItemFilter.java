package leotech.cdp.query.filters;

import io.vertx.core.json.JsonObject;
import leotech.system.model.SystemUser;

public final class AssetItemFilter extends DataFilter {

	String groupId;
	
	public AssetItemFilter() {
		super();
	}
	
	public AssetItemFilter(SystemUser systemUser, String uri, JsonObject paramJson) {
		super(systemUser, uri, paramJson);
		this.groupId = paramJson.getString("groupId","");
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	
}
