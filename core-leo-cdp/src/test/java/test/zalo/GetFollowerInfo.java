package test.zalo;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.vng.zalo.sdk.APIException;
import com.vng.zalo.sdk.oa.ZaloOaClient;

import leotech.cdp.data.service.ZaloApiService;
import leotech.cdp.model.social.ZaloUserDetail;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author hien
 */
public class GetFollowerInfo {

	private static final String URL_OPENAPI_ZALO_ME_V3_0_OA_USER_DETAIL = "https://openapi.zalo.me/v3.0/oa/user/detail";
	
	public static ZaloUserDetail getZaloUserInfo(String user_id) {
		try {
			ZaloOaClient client = new ZaloOaClient();
			String access_token = ZaloApiService.AccessTokenUtil.getValue();

			Map<String, String> headers = new HashMap<>();
			headers.put("access_token", access_token);

			JsonObject data = new JsonObject();
			data.addProperty("user_id", user_id);

			Map<String, Object> params = new HashMap<>();
			params.put("data", data.toString());

			JsonObject jsonObject = client.excuteRequest(URL_OPENAPI_ZALO_ME_V3_0_OA_USER_DETAIL, "GET", params, null,
					headers, null);

			ZaloUserDetail u = new ZaloUserDetail(user_id, jsonObject);
			return u;
		} catch (APIException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws APIException {
		String user_id = "4451434524018839561";

		ZaloUserDetail u = getZaloUserInfo(user_id);
		
		System.out.println("User : " + u);

		System.exit(0);
	}



	
}
