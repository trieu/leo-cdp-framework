package leotech.system.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import leotech.system.exception.InvalidDataException;
import leotech.system.model.SystemService;
import leotech.system.util.LogUtil;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rfx.core.util.StringUtil;


/**
 * Airflow Data Service
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class AirflowService {

	private static final String HEADER_AUTHORIZATION = "Authorization";
	private static final MediaType CONTENT_TYPE_JSON = MediaType.get("application/json; charset=utf-8");
	private static final String DAG_RUNS = "/dagRuns";
	private static final String SERVICE_API_URL = "service_api_url";
	
	static final String AIRFLOW_SERVER = "local_airflow_server";
	static final SystemService AIRFLOW_CONFIG = SystemConfigsManagement.getSystemServiceById(AIRFLOW_SERVER);
	
	public static class AuthInfo {
		String username;
		String password;
		public AuthInfo(String username, String password) {
			super();
			this.username = username;
			this.password = password;
		}
		public String getUsername() {
			return username;
		}
		public String getPassword() {
			return password;
		}
	}
	

	/**
	 * @param dagId
	 * @return
	 */
	protected static String getDagUrl(String dagId) {
		if(StringUtil.isEmpty(dagId)) {
			throw new InvalidDataException(
					"The dagId is NULL or empty. Please set a valid dagId !");
		}
		String baseUrl = AIRFLOW_CONFIG.getConfigs().getOrDefault(SERVICE_API_URL, "").toString();
		if (baseUrl.isBlank()) {
			throw new InvalidDataException(
					"The config [service_api_url] of [etl_airflow_server] is NULL or empty. Please set a valid URL !");
		}
		if(!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}
		return baseUrl + dagId + DAG_RUNS;
	}
	
	protected static AuthInfo getAirflowAuthInfo() {
		String u = AIRFLOW_CONFIG.getConfigs().getOrDefault("airflow_authorization_username", "").toString();
		String p = AIRFLOW_CONFIG.getConfigs().getOrDefault("airflow_authorization_password", "").toString();
		if (u.isBlank()) {
			throw new InvalidDataException(
					"The config [airflow_authorization_username] of [etl_airflow_server] is NULL or empty. Please set a valid username !");
		}
		if (p.isBlank()) {
			throw new InvalidDataException(
					"The config [airflow_authorization_password] of [etl_airflow_server] is NULL or empty. Please set a valid password !");
		}
		return new AuthInfo(u, p);
	}
	

	

	/**
	 * trigger Dag Job
	 * 
	 * @param segmentId
	 * @param dagUrl
	 */
	public static boolean triggerDagJob(String dagId, Map<String, Object> confParams) {
		LogUtil.logInfo(AirflowService.class, "Send data to airflow, " + " dagId: " + dagId + " serviceParams : " + confParams);
		String dagUrl = getDagUrl(dagId);
		
		// Basic Auth
		AuthInfo authInfo = getAirflowAuthInfo();
		String username = authInfo.getUsername();
        String password = authInfo.getPassword();

        // JSON body
        Map<String, Object> conf = new HashMap<String, Object>();
        conf.put("conf", confParams);
        String jsonBody = new Gson().toJson(conf);
	
        // Make the request and handle the response
        try {
            // Create the request body
            RequestBody body = RequestBody.create( jsonBody, CONTENT_TYPE_JSON);
            String credential = Credentials.basic(username, password);

            // Build the request
            Request request = new Request.Builder()
                    .url(dagUrl)
                    .post(body)
                    .addHeader(HEADER_AUTHORIZATION, credential)
                    .build();
            
            Response response = new OkHttpClient().newCall(request).execute();
        	
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // Get the response body
            String responseBody = response.body().string();
            LogUtil.logInfo(AirflowService.class, "dagUrl: " + dagUrl + " response: \n " + responseBody);
            return StringUtil.isNotEmpty(responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(AirflowService.class, "dagUrl: " + dagUrl + " error: \n " + e.getMessage());
        }
        return false;
	}
	
}
