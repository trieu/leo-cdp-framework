package leotech.cdp.query.filters;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class CampaignFilter extends DataFilter {
	String segmentId;

	public CampaignFilter(SystemUser systemUser, String uri, MultiMap params) {
		// to check permission
		this.isSuperAdmin = systemUser.hasSuperAdminRole();
		this.hasAdminRole = systemUser.hasAdminRole();
		this.loginUsername = systemUser.getUserLogin();
		this.profileVisitorId = systemUser.getProfileVisitorId();
		
		this.uri = uri;
		
		this.segmentId = HttpWebParamUtil.getString(params,"segmentId", "");
		this.start =   HttpWebParamUtil.getInteger(params,"startIndex", 0);
		this.length = HttpWebParamUtil.getInteger(params,"numberResult", 20);
	}
	
	public CampaignFilter(SystemUser systemUser, String uri, JsonObject paramJson) {
		super(systemUser, uri, paramJson);
	}

	public String getSegmentId() {
		return segmentId;
	}

	public void setSegmentId(String segmentId) {
		this.segmentId = segmentId;
	}
	
}
