package test.cdp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.openlocationcode.OpenLocationCode;

import leotech.cdp.dao.ContextSessionDaoUtil;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.domain.TouchpointManagement;
import leotech.cdp.model.analytics.ContextSession;
import leotech.cdp.model.customer.Device;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointType;
import leotech.system.model.DeviceInfo;
import leotech.system.util.DeviceInfoUtil;
import leotech.system.util.GeoLocationUtil;
import rfx.core.util.FileUtils;
import rfx.core.util.RandomUtil;
import rfx.core.util.Utils;

public class GenerateFakeEventData {

	static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	static List<String> urls = new ArrayList<String>();

	// Leo Web Observer for channel: Video Content Hub
	static String observerId = "4zfVva5ed1ZPqTMf489Qax";
	
	static {
		urls.add("https://bookstore.bigdatavietnam.org/html/group/data-science-books");
		urls.add("https://bookstore.bigdatavietnam.org/html/group/van-hoc-tac-pham-kinh-dien");
		urls.add("https://bookstore.bigdatavietnam.org/html/group/business-book");
		urls.add("https://bookstore.bigdatavietnam.org/html/group/artificial-intelligence-books");
		urls.add("https://bookstore.bigdatavietnam.org/html/group/programming-books");
		urls.add("https://bookstore.bigdatavietnam.org/html/group/basic-science-books");
		
		urls.add("https://bookstore.bigdatavietnam.org/html/post/co-nen-tin-vao-truc-giac-tu-duy-nhanh-va-cham-review-sach-hay-nen-oc-10000-0cf9df83f1504fa1ffdb895e189436e0e59269d8");
		urls.add("https://bookstore.bigdatavietnam.org/html/post/phuong-phap-hoc-tap-hieu-qua-10000-5a85f139e8da00743966221c3227a7e4f179690a");
		urls.add("https://bookstore.bigdatavietnam.org/html/post/sach-hay-moi-ngay-so-34-bach-khoa-tri-thuc-cho-tre-em-10000-09bf98db38b826fd5b060478dd4d8310d3407aef");
		urls.add("https://bookstore.bigdatavietnam.org/html/post/best-data-engineer-books-of-2019-10000-5a6678100921a2b18adc67749c633fbe3b10f8a7");
		urls.add("https://bookstore.bigdatavietnam.org/html/post/data-science-in-5-minutes-10000-eb9f16cb1d8c5cf8341a3ba6fa765b68751a7233");
		urls.add("https://bookstore.bigdatavietnam.org/html/post/van-hoc-kinh-ien-nen-oc-phan-1-10000-a35df0453f31c013c753648eb00505c32375038e");
		urls.add("https://bookstore.bigdatavietnam.org/html/post/ha-khuat-review-hanh-trinh-ve-phuong-ong-oc-e-hieu-ung-ve-the-gioi-tam-linh-10000-ed4202db471fba9d6235b9cd17f15814de26b137");
		urls.add("https://bookstore.bigdatavietnam.org/html/post/oi-ngan-ung-ngu-dai-robin-sharma-sach-noi-hay-10000-746088d33089cf854245b1d8c92cac41388739c4");
		urls.add("https://bookstore.bigdatavietnam.org/html/post/audio-bo-tu-quyen-luc-a-ung-dung-tri-tue-nhan-tao-ai-vao-doanh-nghiep-cua-minh-nhu-the-nao-10000-0104d5155725cb91a03af99f436637637d07ab47");
		urls.add("https://bookstore.bigdatavietnam.org/html/post/what-is-a-data-strategy-10000-3730d2c1036fc06883048c268b51f8d31a077183");
		urls.add("https://bookstore.bigdatavietnam.org/html/post/top-10-programming-books-of-all-time-development-books-10000-5e251f28b97cf7d7b41f940643ac36b40f293141");
		
	}
	
	
	
	public static String  getRamdomlyTouchpointUrl() {
	    Random rand = new Random();
	    return urls.get(rand.nextInt(urls.size()));
	}

	static void generateEvents(Date loggedAt, String locationCode, String visitorId, String profileId, DeviceInfo deviceInfo, String userDeviceId, int profileType) {
		String eventName = "content-view";
		
		String fingerprintId = "";
		String sourceIP = "127.0.0.1";
		int addedMinutes = RandomUtil.getRandomInteger(300, 50);

		Date createdAt = DateUtils.addMinutes(loggedAt, addedMinutes);
		DateTime dateTime = new DateTime(createdAt);
		String dateTimeKey = ContextSession.getSessionDateTimeKey(dateTime);

		// touchpoint
		String touchpointRefDomain = "bookstore.bigdatavietnam.org";
		String refTouchpointUrl = "https://" + touchpointRefDomain + "/";
		boolean isFromOwnedMedia = touchpointRefDomain.equals("bookstore.bigdatavietnam.org");

		Touchpoint refTouchPoint = TouchpointManagement.getOrCreateForTesting(createdAt,
				touchpointRefDomain, TouchpointType.WEB_APP, refTouchpointUrl, isFromOwnedMedia);
		String srcTouchpointUrl = getRamdomlyTouchpointUrl();
		Touchpoint srcTouchpoint = TouchpointManagement.getOrCreateForTesting(createdAt,
				"bookstore.bigdatavietnam.org", TouchpointType.WEB_APP, srcTouchpointUrl, true);
		
		String refTouchpointId = refTouchPoint.getId();
		String srcTouchpointId = srcTouchpoint.getId();

		ContextSession ctxSession = new ContextSession(observerId, dateTime, dateTimeKey, locationCode,
				userDeviceId, "127.0.0.1", "bookstore.bigdatavietnam.org", "", refTouchpointId, srcTouchpointId,
				profileId, profileType, visitorId,"", "pro");
		ContextSessionDaoUtil.create(ctxSession);
		
		
		EventObserverManagement.recordEventFromWeb(createdAt, ctxSession, observerId, "pro", fingerprintId, userDeviceId, sourceIP,
				deviceInfo, "Book Video Review", srcTouchpointUrl, refTouchpointUrl, touchpointRefDomain, eventName);
		String refTouchpointUrl2 = srcTouchpointUrl;
		
		int randomPageviews = RandomUtil.getRandomInteger(20, 3);
		for (int i = 0; i < randomPageviews; i++) {
			int addedSeconds = RandomUtil.getRandomInteger(60, 5);

			Date trackedTime = DateUtils.addSeconds(createdAt, addedSeconds);
			
			String srcTouchpointUrl2 = getRamdomlyTouchpointUrl();
			
			// pageview event
			EventObserverManagement.recordEventFromWeb(trackedTime, ctxSession, observerId, "pro", fingerprintId, userDeviceId, sourceIP,
					deviceInfo, "Book Video Review", srcTouchpointUrl2, refTouchpointUrl2, touchpointRefDomain, eventName);
			refTouchpointUrl2 = srcTouchpointUrl2;
		}
	}
	
	

	public static void main(String[] args) throws Exception {

		JsonArray jsonArray = new Gson().fromJson(FileUtils.readFileAsString("./data/MOCK_DATA_TEST_2.json"),JsonArray.class);
		for (JsonElement je : jsonArray) {
			JsonObject jo = je.getAsJsonObject();

			// ID key
			String email = jo.get("email").getAsString();

			// time
			String sessionDate = jo.get("session_date").getAsString();
			String sessionTime = jo.get("session_time").getAsString();
			Date loggedAt = dateFormatter.parse(sessionDate + " " + sessionTime);
			System.out.println("#### createdAt " + loggedAt);

			// geolocation
			double lat = jo.get("lat").getAsDouble();
			double lon = jo.get("lon").getAsDouble();
			String locationCode = OpenLocationCode.encode(lat, lon);

			ProfileSingleView profile = ProfileDaoUtil.getByPrimaryEmail(email);
			
			String visitorId = profile.getVisitorId();
			
			// Device 
			String user_agent = jo.get("user_agent").getAsString();
			DeviceInfo deviceInfo = DeviceInfoUtil.getDeviceInfo(user_agent);
			Device userDevice = DeviceInfoUtil.getUserDevice(deviceInfo, loggedAt);
			String userDeviceId = userDevice.getId();
			
			profile.setLastUsedDeviceId(userDeviceId);
			profile.setLocationCode(locationCode);
			String livingLocation = GeoLocationUtil.getLocationName(lat, lon).getLocationName();
			profile.setLivingLocation(livingLocation);
			int age = RandomUtil.getRandomInteger(65, 16);
			profile.setAge(age);
			
			ProfileDaoUtil.saveProfile(profile);
			
			System.out.println(livingLocation);
			
			generateEvents(loggedAt, locationCode, visitorId, profile.getId(), deviceInfo, userDeviceId, profile.getType());
		}
		Utils.exitSystemAfterTimeout(5000);

	}
}
