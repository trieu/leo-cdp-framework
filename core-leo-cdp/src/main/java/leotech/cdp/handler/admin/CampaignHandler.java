package leotech.cdp.handler.admin;

import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.CampaignManagement;
import leotech.cdp.model.marketing.Campaign;
import leotech.cdp.query.filters.CampaignFilter;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;

/**
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class CampaignHandler extends SecuredHttpDataHandler {
	
	private static final String NEW_CAMPAIGN = "new";

	// for dataList view
	static final String API_LIST_WITH_FILTER = "/cdp/campaigns/filter";
	
	// for Object View
	static final String API_SAVE_CAMPAIGN = "/cdp/campaign/save";
	static final String API_UPDATE_CAMPAIGN_STATUS = "/cdp/campaign/status/save";
	static final String API_GET_CAMPAIGN = "/cdp/campaign/get";
	static final String API_REMOVE_CAMPAIGN = "/cdp/campaign/remove";
	
	public CampaignHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}

	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			if (isAuthorized(loginUser, Campaign.class)) {
				switch (uri) {
					case API_LIST_WITH_FILTER : {
						// the list-view component at datatables.net needs POST method to avoid long URL 
						CampaignFilter filter = new CampaignFilter(loginUser, uri, paramJson);
						JsonDataTablePayload payload = CampaignManagement.filter(filter);
						payload.setUserLoginPermission(loginUser, Campaign.class);
						return payload;
					}
					case API_GET_CAMPAIGN: {
						String id = paramJson.getString("id", NEW_CAMPAIGN);
						Campaign model;
						if (id.equals(NEW_CAMPAIGN)) {
							model = CampaignManagement.newCampaign();
						} else {
							model = CampaignManagement.getById(id);
						}
						return JsonDataPayload.ok(uri, model, loginUser, Campaign.class);
					}
					case API_SAVE_CAMPAIGN : {
						String json = paramJson.getString("dataObject", "{}");
						String savedId = CampaignManagement.saveCampaignFromJson(json);
						if (savedId != null) {
							return JsonDataPayload.ok(uri, savedId, loginUser, Campaign.class);
						} else {
							return JsonDataPayload.fail("failed to save Campaign data into database", 500);
						}
					}
					case API_UPDATE_CAMPAIGN_STATUS : {
						String id = paramJson.getString("id", "");
						int status = paramJson.getInteger("status", Campaign.STATUS_DRAFT);
						boolean rs = false;
						if( ! id.isEmpty() ) {
							String savedId = CampaignManagement.updateCampaignStatus(id, status);
							rs = id.equals(savedId);
						}
						return JsonDataPayload.ok(uri, rs, loginUser, Campaign.class);
					}
					case API_REMOVE_CAMPAIGN : {
						String id = paramJson.getString("id", "");
						boolean rs = false;
						if( ! id.isEmpty() ) {
							String deletedId = CampaignManagement.removeCampaign(id);
							rs = id.equals(deletedId);
						}
						return JsonDataPayload.ok(uri, rs, loginUser, Campaign.class);
					}
					default : {
						return JsonErrorPayload.NO_HANDLER_FOUND;
					}
				}
			}
			return JsonErrorPayload.NO_AUTHORIZATION;

		} else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			if (isAuthorized(loginUser, Campaign.class)) {
				switch (uri) {
					case API_LIST_WITH_FILTER: {
						CampaignFilter filter = new CampaignFilter(loginUser, uri, params);
						JsonDataTablePayload payload = CampaignManagement.filter(filter);
						return JsonDataPayload.ok(uri, payload, loginUser, Campaign.class);
					}
					case API_GET_CAMPAIGN : {
						String id = HttpWebParamUtil.getString(params,"id", NEW_CAMPAIGN);
						Campaign model;
						if (id.equals(NEW_CAMPAIGN)) {
							model = CampaignManagement.newCampaign();
						} else {
							model = CampaignManagement.getById(id);
						}
						return JsonDataPayload.ok(uri, model, loginUser, Campaign.class);
					}

					default :
						return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			} else {
				return JsonErrorPayload.NO_AUTHORIZATION;
			}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}

}
