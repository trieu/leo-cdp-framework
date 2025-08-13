package leotech.cdp.data.service;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.vng.zalo.sdk.APIException;
import com.vng.zalo.sdk.oa.ZaloOaClient;

import leotech.cdp.model.social.ZaloUserDetail;
import rfx.core.configs.WorkerConfigs;
import rfx.core.util.StringUtil;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class ZaloApiService {
	public static final String ZALO_ACCESS_TOKEN = "zaloAccessToken";
	public static final String URL_OPENAPI_ZALO_ME_V3_0_OA_USER_DETAIL = "https://openapi.zalo.me/v3.0/oa/user/detail";
	
	public static class AccessTokenUtil {

		static String accessToken = WorkerConfigs.load().getCustomConfig(ZALO_ACCESS_TOKEN).trim();

		public static final String getValue() {
			return accessToken;
		}
		
		public static final void setValue(String s) {
			if(StringUtil.isNotEmpty(s)) {
				accessToken = s;
			}
		}
	}

	
	public static ZaloUserDetail getZaloUserInfo(String user_id) {
		try {
			ZaloOaClient client = new ZaloOaClient();
			String access_token = AccessTokenUtil.getValue();

			Map<String, String> headers = new HashMap<>();
			headers.put("access_token", access_token);

			JsonObject data = new JsonObject();
			data.addProperty("user_id", user_id);

			Map<String, Object> params = new HashMap<>();
			params.put("data", data.toString());

			JsonObject jsonObject = client.excuteRequest(URL_OPENAPI_ZALO_ME_V3_0_OA_USER_DETAIL, "GET", params, null, headers, null);

			ZaloUserDetail u = new ZaloUserDetail(user_id, jsonObject);
			return u;
		} catch (APIException e) {
			e.printStackTrace();
		}
		return new ZaloUserDetail(user_id);
	}
}
