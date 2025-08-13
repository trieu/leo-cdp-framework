package leotech.system.util;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import rfx.core.util.StringPool;

public final class HttpClientGetUtil {
	
	private static final int CONNECT_TIMEOUT = 10 * 1000; // 10 seconds
	private static final Charset CHARSET_UTF8 = Charset.forName(StringPool.UTF_8);
	
	private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.61 Safari/537.36";
	
	private final static int MAX_SIZE = 20;
	
	private static final String PROXY_HOST = "admin.leocdp.com";
	private static final int PROXY_PORT = 3600;
	
	private static ConcurrentMap<Integer, HttpClient> httpClientPool = new ConcurrentHashMap<>(MAX_SIZE);

	static final HttpClient getThreadSafeClient() throws Exception {
		int slot = (int) (Math.random() * (MAX_SIZE + 1));
		return getThreadSafeClient(slot);
	}
	
	private static final HttpClient getThreadSafeClient(int slot) throws Exception {
		HttpClient httpClient = httpClientPool.get(slot);
		if (httpClient == null) {
			PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
			// Increase max total connection to 200
			cm.setMaxTotal(300);
			// Increase default max connection per route to 20
			cm.setDefaultMaxPerRoute(30);
			// Increase max connections for localhost:80 to 50
			HttpHost localhost = new HttpHost("locahost", 80);
			cm.setMaxPerRoute(new HttpRoute(localhost), 50);

			Builder custom = RequestConfig.custom();
			custom.setConnectTimeout(CONNECT_TIMEOUT);
			
			//CredentialsProvider credentialsPovider = initProxy(custom);

		
			HttpClientBuilder clientBuilder = HttpClients.custom();
			clientBuilder.setConnectionManager(cm).setDefaultRequestConfig(custom.build());
			
			//clientBuilder.setDefaultCredentialsProvider(credentialsPovider);
			
			httpClient = clientBuilder.build();
			httpClientPool.put(slot, httpClient);
			
		}
		return httpClient;
	}

	protected static CredentialsProvider initProxy(Builder custom) {
		// proxy
		HttpHost proxy = new HttpHost(PROXY_HOST,PROXY_PORT);
		custom.setProxy(proxy);
		CredentialsProvider credentialsPovider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("trieu", "Hellboy@12345");
		credentialsPovider.setCredentials(new AuthScope(PROXY_HOST, PROXY_PORT), credentials);
		return credentialsPovider;
	}

	public final static String call(String urlString) {
		HttpResponse response = null;
		HttpClient httpClient = null;
		String html = StringPool.BLANK;
		int slot = (int) (Math.random() * (MAX_SIZE + 1));
		HttpGet httpget = null;
		try {
			URL url = new URL(urlString);
			httpget = new HttpGet(url.toURI());
			httpget.setHeader("User-Agent", USER_AGENT);
			httpget.setHeader("Accept-Charset", "utf-8");
			httpget.setHeader("Accept", "text/html,application/xhtml+xml,application/json");
			httpget.setHeader("Cache-Control", "max-age=3, must-revalidate, private");

			httpClient = getThreadSafeClient(slot);
			response = httpClient.execute(httpget);

			int code = response.getStatusLine().getStatusCode();
			if (code == 200) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					html = EntityUtils.toString(entity, CHARSET_UTF8);
				}
			}
		} catch (Exception e) {
			System.err.println("[HttpClientGetUtil] "+e.getMessage());
			httpClientPool.remove(slot);
		} finally {
			response = null;
		}
		return html;
	}


}
