package leotech.cdp.query.filters;

import io.vertx.core.json.JsonObject;
import leotech.system.model.SystemUser;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * the metadata to fiter system event in database
 * 
 * @author tantrieuf31
 * @since 2023
 */
public final class SystemEventFilter extends DataFilter {
	
	String objectName;
	String objectId;
	String action;
	String accessIp;

	public SystemEventFilter() {
		this.start = 0;
		this.length = 0;
	}
	
	public SystemEventFilter(SystemUser systemUser, String uri, JsonObject paramJson) {
		super(systemUser, uri, paramJson);
		
		// core parameters
		this.objectName = paramJson.getString("objectName", StringPool.BLANK);
		this.objectId = paramJson.getString("objectId", StringPool.BLANK);
		this.action = paramJson.getString("action", StringPool.BLANK);
		this.accessIp = paramJson.getString("accessIp", StringPool.BLANK);
		
		// filter from User Login Report view
		setLoginUsername(paramJson.getString("requestUserLogin", StringPool.BLANK));
	}
	

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		if(StringUtil.isNotEmpty(objectName)) {
			this.objectName = objectName;
		}
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		if(StringUtil.isNotEmpty(objectId)) {
			this.objectId = objectId;
		}
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		if(StringUtil.isNotEmpty(action)) {
			this.action = action;
		}
	}

	public String getAccessIp() {
		return accessIp;
	}

	public void setAccessIp(String accessIp) {
		if(StringUtil.isNotEmpty(accessIp)) {
			this.accessIp = accessIp;
		}
	}
}
