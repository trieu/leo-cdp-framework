package leotech.cdp.data.service;

import java.util.HashMap;
import java.util.Map;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetTemplateDaoUtil;
import leotech.cdp.data.DataServiceJob;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.Touchpoint;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rfx.core.util.HttpClientUtil;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;


/**
 * 
 * https://developers.brevo.com/ <br>
 * 
 * CDP Data Service for Brevo (SendInBlue) API
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
@Deprecated
public final class ZaloDataService extends DataServiceJob {

	static final String API_KEY = "api-key";

	public ZaloDataService() {
		super();
	}

	@Override
	protected void initConfigs() {

	}

	@Override
	protected String doMyJob() {
		return this.processSegmentDataActivation();
	}

	@Override
	protected String processSegment(String segmentId, String segmentName, long segmentSize) {

		createOrUpdateTag(segmentName);
		// synch profile to Zalo
		super.processProfilesInSegment(segmentId, segmentSize);
		return done(segmentId, segmentSize);

	}

	@Override
	protected String processProfile(Profile profile) {
		String primaryPhone = profile.getPrimaryPhone();

		System.out.println(profile.toString());
		if (StringUtil.isNotEmpty(primaryPhone)) {
			updateUser(profile, primaryPhone);
			return primaryPhone;
		}

		return StringPool.BLANK;
	}

	@Override
	protected String processTouchpoint(Touchpoint touchpoint) {
		// skip
		return StringPool.BLANK;
	}

	@Override
	protected String processTouchpointHub(String touchpointHubId, String touchpointHubName, long touchpointHubSize) {
		// skip
		return StringPool.BLANK;
	}

	/**
	 * https://developers.sendinblue.com/reference/createcontact
	 * 
	 * @param profile
	 * @param primaryEmail
	 * @throws ApiException
	 */
	public static final void updateUser(Profile profile, String primaryPhone) {
		System.out.println("createOrUpdateContact phone: " + primaryPhone);

		// call API
		// https://developers.zalo.me/docs/official-account/quan-ly/quan-ly-thong-tin-nguoi-dung/cap-nhat-thong-tin-khach-hang-quan-tam-oa
	}

	/**
	 * @param segmentName
	 * @throws ApiException
	 */
	public static final void createOrUpdateTag(String segmentName) {
		// call API
		// https://developers.zalo.me/docs/official-account/quan-ly/quan-ly-thong-tin-nguoi-dung/gan-nhan-khach-hang
	}

	public static final void getFollowers() {
		// call API
		// https://developers.zalo.me/docs/official-account/quan-ly/quan-ly-thong-tin-nguoi-dung/lay-danh-sach-khach-hang-quan-tam-oa

		String json = HttpClientUtil.executeGet("https://zalo-apps.bigdatavietnam.org/getfollowers");
		JsonArray list = new JsonArray(json);
		for (Object obj : list) {
			JsonObject o = (JsonObject) obj;
			String user_id = o.getString("user_id");
			System.out.println("user_id " + user_id);
			getUserInfo(user_id);
		}
	}

	public static final void getUserInfo(String userId) {
		// call API
		// https://developers.zalo.me/docs/official-account/quan-ly/quan-ly-thong-tin-nguoi-dung/lay-danh-sach-khach-hang-quan-tam-oa

		String json = HttpClientUtil.executeGet("https://zalo-apps.bigdatavietnam.org/getprofile/" + userId);
		JsonObject obj = new JsonObject(json);

		JsonObject data = obj.getJsonObject("data");
		if (data != null && obj.getInteger("error") == 0) {
			String display_name = data.getString("display_name");
			System.out.println("display_name " + display_name);

			String avatar = data.getString("avatar");
			System.out.println("avatar " + avatar);
		}
	}

	public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	static OkHttpClient client = new OkHttpClient();
	public static final void sendPromotionMessage(String userId, String assetTemplateId) {
		// call API
		// https://developers.zalo.me/docs/official-account/quan-ly/quan-ly-thong-tin-nguoi-dung/lay-danh-sach-khach-hang-quan-tam-oa
		String url = "https://zalo-apps.bigdatavietnam.org/send_message_promotion";
		try {
			//String json = FileUtils.readFileAsString("./resources/content-templates/zalo-promotion.json");
			AssetTemplate tpl = AssetTemplateDaoUtil.getById(assetTemplateId);
			Handlebars handlebars = new Handlebars();
	        Template template = handlebars.compileInline(tpl.getJsonMetadata());
	        
	        Map<String, String> map = new HashMap<String, String>();
	        map.put("user_id", userId);
			String json = template.apply(map);
			
			System.out.println(json);

			RequestBody body = RequestBody.Companion.create(json, JSON);

			Request request = new Request.Builder().url(url).post(body).build();
			Call call = client.newCall(request);
			
			Response response = call.execute();
			System.out.println(response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	


}
