package test.cdp;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.openlocationcode.OpenLocationCode;

import leotech.cdp.dao.ContextSessionDaoUtil;
import leotech.cdp.dao.DeviceDaoUtil;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.TouchpointManagement;
import leotech.cdp.model.analytics.ContextSession;
import leotech.cdp.model.customer.Device;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointType;
import leotech.system.model.DeviceInfo;
import leotech.system.util.DeviceInfoUtil;
import rfx.core.util.FileUtils;
import rfx.core.util.HashUtil;
import rfx.core.util.RandomUtil;

public class GenerateCdpTestData {
	
	static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
	static Map<String, Boolean> mapMediaDomain = new HashMap<String, Boolean>();
	static List<String> urlsToRamdom = new ArrayList<String>();
	
	static {
		mapMediaDomain.put("nbcnews.com", true);
		mapMediaDomain.put("nature.com", true);
		mapMediaDomain.put("forbes.com", true);
		mapMediaDomain.put("huffingtonpost.com", true);
		mapMediaDomain.put("prnewswire.com", true);
		mapMediaDomain.put("scientificamerican.com", true);
		mapMediaDomain.put("usatoday.com", true);
		mapMediaDomain.put("thetimes.co.uk", true);
		mapMediaDomain.put("tripadvisor.com", true);
		mapMediaDomain.put("washingtonpost.com", true);
		mapMediaDomain.put("wired.com", true);
		mapMediaDomain.put("usnews.com", true);
		mapMediaDomain.put("google.co.jp", true);
		mapMediaDomain.put("usnews.com", true);
		mapMediaDomain.put("bloomberg.com", true);
		mapMediaDomain.put("google.de", true);
		mapMediaDomain.put("indiatimes.com", true);
		mapMediaDomain.put("forbes.com", true);
		mapMediaDomain.put("pinterest.com", true);
		mapMediaDomain.put("google.co.jp", true);
		mapMediaDomain.put("about.com", true);
		mapMediaDomain.put("sun.com", true);
		
		urlsToRamdom.add("https://bookstore.bigdatavietnam.org/html/group/data-science-books");
		urlsToRamdom.add("https://bookstore.bigdatavietnam.org/html/group/van-hoc-tac-pham-kinh-dien");
		urlsToRamdom.add("https://bookstore.bigdatavietnam.org/html/group/business-book");
		urlsToRamdom.add("https://bookstore.bigdatavietnam.org/html/group/artificial-intelligence-books");
		urlsToRamdom.add("https://bookstore.bigdatavietnam.org/html/group/programming-books");
		urlsToRamdom.add("https://bookstore.bigdatavietnam.org/html/group/basic-science-books");
	}
	
	
	public static String getValidMediaDomain(int id, String domain_referer) {
		String defaultVal = id % 2 == 1 ? "facebook.com" : "bookstore.bigdatavietnam.org";
		return mapMediaDomain.getOrDefault(domain_referer, false) ? domain_referer : defaultVal;
	}
	
	public static String  getRamdomlyTouchpointUrl() {
	    Random rand = new Random();
	    return urlsToRamdom.get(rand.nextInt(urlsToRamdom.size()));
	}

    //Touchpoint: Facebook 
	
	public static void main(String[] args) throws JsonSyntaxException, IOException, ParseException {
		JsonArray jsonArray = new Gson().fromJson(FileUtils.readFileAsString("./data/MOCK_DATA_TEST_2.json"), JsonArray.class);
		for (JsonElement je : jsonArray) {
			JsonObject jo = je.getAsJsonObject();
			
			generareFakeProfileForTesting(jo);
		}
	}

	private static void generareFakeProfileForTesting(JsonObject jo) throws ParseException {
		String sourceIP = "127.0.0.1";
		int id = jo.get("id").getAsInt();
		boolean is_product_view = jo.get("is_product_view").getAsInt() == 1;
		
		// Personal Info
		String firstName = jo.get("first_name").getAsString();
		String lastName = jo.get("last_name").getAsString();
		String email = jo.get("email").getAsString();
		int gender = jo.get("gender").getAsString().equals("Male") ? 1 : 0;
		
		// Session date and time
		String sessionDate = jo.get("session_date").getAsString();
		String sessionTime = jo.get("session_time").getAsString();
		Date createdAt = dateFormatter.parse(sessionDate + " " + sessionTime);
		System.out.println("#### createdAt "+createdAt);
		
		// touchpoint
		String touchpointRefDomain = getValidMediaDomain(id,jo.get("domain_referer").getAsString());
		String refTouchpointUrl = "https://" + touchpointRefDomain + "/";
		boolean isFromOwnedMedia = touchpointRefDomain.equals("bookstore.bigdatavietnam.org");
		
		Touchpoint refTouchPoint = TouchpointManagement.getOrCreateForTesting(createdAt,touchpointRefDomain, TouchpointType.WEB_APP, refTouchpointUrl, isFromOwnedMedia);
		String srcTouchpointUrl = getRamdomlyTouchpointUrl();
		Touchpoint srcTouchpoint = TouchpointManagement.getOrCreateForTesting(createdAt,"bookstore.bigdatavietnam.org", TouchpointType.WEB_APP, srcTouchpointUrl, true);
		String refTouchpointId = refTouchPoint.getId();
		String srcTouchpointId = srcTouchpoint.getId();
		
		//geolocation
		double lat = jo.get("lat").getAsDouble();
		double lon = jo.get("lon").getAsDouble();
		String locationCode = OpenLocationCode.encode(lat, lon);
		
		// Device 
		String user_agent = jo.get("user_agent").getAsString();
		DeviceInfo deviceInfo = DeviceInfoUtil.getDeviceInfo(user_agent);
		Device userDevice = DeviceInfoUtil.getUserDevice(deviceInfo, createdAt);
		String userDeviceId = userDevice.getId();
		String fingerprintId = "";
		DeviceDaoUtil.save(userDevice);
		
		// web UUID
		String visitorId = RandomStringUtils.secure().nextAlphabetic(32).toLowerCase();
		
		// social login
		String source = "facebook";
		String refId = HashUtil.hashUrlCrc64(email)+"";
		
		//Leo Web Observer for channel: Video Content Hub
		String observerId = "4zfVva5ed1ZPqTMf489Qax";
		
		Profile profile = ProfileDataManagement.saveSocialLoginProfile(email, visitorId, firstName, lastName, refId, source, 
				observerId, srcTouchpoint, refTouchpointId, touchpointRefDomain, userDeviceId, gender, createdAt);
		String profileId = profile.getId();
		
		// create session
		DateTime dateTime = new DateTime(createdAt);
		String dateTimeKey = ContextSession.getSessionDateTimeKey(dateTime);
		ContextSession ctxSession = new ContextSession(observerId, dateTime, dateTimeKey, locationCode,
				userDeviceId, "127.0.0.1", "bookstore.bigdatavietnam.org", "", refTouchpointId, srcTouchpointId, profileId , profile.getType(), visitorId,"", "pro");
		ContextSessionDaoUtil.create(ctxSession);
		
		String eventName = "content-view";
		// pageview event
		EventObserverManagement.recordEventFromWeb(createdAt, ctxSession, observerId, "pro", fingerprintId, userDeviceId, sourceIP, deviceInfo,"Book Video Review", srcTouchpointUrl, refTouchpointUrl, touchpointRefDomain, eventName );
		
		eventName = "social-login";
		Date loginTime = DateUtils.addSeconds(createdAt, RandomUtil.getRandomInteger(300, 9)); 
		EventObserverManagement.recordEventFromWeb(loginTime,ctxSession, observerId, "pro", fingerprintId, userDeviceId, sourceIP, deviceInfo,"Book Video Review",srcTouchpointUrl, refTouchpointUrl,  touchpointRefDomain, eventName);
		
		
		System.out.println(new Gson().toJson(profile));
	}
}
