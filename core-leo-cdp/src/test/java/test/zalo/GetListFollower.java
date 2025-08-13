package test.zalo;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.vng.zalo.sdk.APIException;
import com.vng.zalo.sdk.oa.ZaloOaClient;

import leotech.cdp.data.service.ZaloApiService;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author hien
 */
public class GetListFollower {

	public static void main(String[] args) throws APIException {
		ZaloOaClient client = new ZaloOaClient();
		String access_token = ZaloApiService.AccessTokenUtil.getValue();

		Map<String, String> headers = new HashMap<>();
		headers.put("access_token", access_token);

		JsonObject data = new JsonObject();
		data.addProperty("offset", 0);
		data.addProperty("count", 10);

		Map<String, Object> params = new HashMap<>();
		params.put("data", data.toString());

		JsonObject excuteRequest = client.excuteRequest("https://openapi.zalo.me/v3.0/oa/user/getlist", "GET", params,
				null, headers, null);

		System.err.println(excuteRequest);

		System.exit(0);
	}
}
