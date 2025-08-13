package leotech.system.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import leotech.system.model.AppMetadata;
import rfx.core.util.FileUtils;

/**
 * 
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class AppMetadataUtil {

	private static final String CONFIGS_JSON = "./configs/app-metadata-configs.json";

	// TODO add shared redis cache here, load MediaNetwork from database

	// default app templates
	public static final String RECOMMENDER_TEMPLATE_FOLDER = "recommender";
	public static final String WEB_FORM_TEMPLATE_FOLDER = "web-form";
	public static final String DEFAULT_ADMIN_TEMPLATE_FOLDER = "leocdp-admin";
	public static final String DEFAUFT_WEB_TEMPLATE_FOLDER = "content-hub";

	final static AppMetadata DEFAULT_CONTENT_NETWORK = new AppMetadata("Genesis Network", "localhost","localhost:9190", DEFAUFT_WEB_TEMPLATE_FOLDER);

	final static Map<String, AppMetadata> mapHost2App = new HashMap<>();

	static {
		try {
			Type listType = new TypeToken<ArrayList<AppMetadata>>(){}.getType();
			String json = FileUtils.readFileAsString(CONFIGS_JSON);
			List<AppMetadata> apps = new Gson().fromJson(json, listType);
			System.out.println(apps);
			for (AppMetadata app : apps) {
				String host = app.getDomain();
				mapHost2App.put(host, app);
			}
			System.out.println(CONFIGS_JSON + " loaded OK with mapHostToMediaApp.size = "+ mapHost2App.size());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static AppMetadata getContentNetwork(String networkDomain) {
		AppMetadata network = mapHost2App.get(networkDomain);

		if (network == null) {
			System.err.println("NOT FOUND MediaNetwork for domain: " + networkDomain);
			return DEFAULT_CONTENT_NETWORK;
		}
		return network;
	}

	public static String getWebTemplateFolder(String networkDomain) {
		AppMetadata network = getContentNetwork(networkDomain);
		return network.getWebTemplateFolder();
	}
	

}
