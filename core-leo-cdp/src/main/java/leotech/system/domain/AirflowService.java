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
 * Trigger DAG runs via Airflow REST API
 *
 * @author Trieu Nguyen
 * @since 2024
 */
public final class AirflowService {

	// =========================
	// Constants
	// =========================
	private static final String HEADER_AUTHORIZATION = "Authorization";
	private static final MediaType CONTENT_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

	private static final String DAG_RUNS = "/dagRuns";
	private static final String SERVICE_API_URL = "service_api_url";

	private static final String CONF_USERNAME = "airflow_authorization_username";
	private static final String CONF_PASSWORD = "airflow_authorization_password";

	static final String AIRFLOW_SERVER = "local_airflow_server";

	// =========================
	// Static Dependencies
	// =========================
	static final SystemService AIRFLOW_CONFIG = SystemConfigsManagement.getSystemServiceById(AIRFLOW_SERVER);

	private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
	private static final Gson GSON = new Gson();

	private AirflowService() {
		// utility class
	}

	// =========================
	// Auth Model
	// =========================
	public static final class AuthInfo {
		private final String username;
		private final String password;

		public AuthInfo(String username, String password) {
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

	// =========================
	// Internal Helpers
	// =========================
	private static SystemService requireAirflowConfig() {
		if (AIRFLOW_CONFIG == null) {
			throw new InvalidDataException("Airflow system service [" + AIRFLOW_SERVER + "] is NOT configured.");
		}
		return AIRFLOW_CONFIG;
	}

	private static String requireConfig(String key) {
		Object value = requireAirflowConfig().getConfigs().get(key);

		if (value == null || StringUtil.isEmpty(value.toString())) {
			throw new InvalidDataException("Missing or empty Airflow config key: [" + key + "]");
		}
		return value.toString();
	}

	protected static String getDagUrl(String dagId) {
		if (StringUtil.isEmpty(dagId)) {
			throw new InvalidDataException("dagId is NULL or empty. Please provide a valid dagId.");
		}

		String baseUrl = requireConfig(SERVICE_API_URL);

		if (!baseUrl.endsWith("/")) {
			baseUrl += "/";
		}

		return baseUrl + dagId + DAG_RUNS;
	}

	protected static AuthInfo getAirflowAuthInfo() {
		String username = requireConfig(CONF_USERNAME);
		String password = requireConfig(CONF_PASSWORD);
		return new AuthInfo(username, password);
	}

	// =========================
	// Public API
	// =========================
	/**
	 * Trigger an Airflow DAG run
	 *
	 * @param dagId      DAG ID
	 * @param confParams DAG run conf payload
	 * @return true if request succeeds
	 */
	public static boolean triggerDagJob(String dagId, Map<String, Object> confParams) {

		LogUtil.logInfo(AirflowService.class, "Trigger Airflow DAG | dagId=" + dagId + " | params=" + confParams);

		String dagUrl = getDagUrl(dagId);
		AuthInfo authInfo = getAirflowAuthInfo();

		Map<String, Object> payload = new HashMap<>();
		payload.put("conf", confParams);

		String jsonBody = GSON.toJson(payload);

		try {
			RequestBody body = RequestBody.create(jsonBody, CONTENT_TYPE_JSON);
			String credential = Credentials.basic(authInfo.getUsername(), authInfo.getPassword());

			Request request = new Request.Builder().url(dagUrl).post(body).addHeader(HEADER_AUTHORIZATION, credential)
					.build();

			try (Response response = HTTP_CLIENT.newCall(request).execute()) {

				if (!response.isSuccessful()) {
					throw new IOException("Airflow call failed | status=" + response.code());
				}

				String responseBody = response.body() != null ? response.body().string() : "";

				LogUtil.logInfo(AirflowService.class, "Airflow response | dagUrl=" + dagUrl + "\n" + responseBody);

				return StringUtil.isNotEmpty(responseBody);
			}

		} catch (Exception e) {
			LogUtil.logError(AirflowService.class, "Trigger DAG failed | dagUrl=" + dagUrl);
			return false;
		}
	}
}
