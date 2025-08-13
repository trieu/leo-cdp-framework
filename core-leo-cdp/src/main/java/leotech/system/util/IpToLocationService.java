package leotech.system.util;

import com.google.openlocationcode.OpenLocationCode;

import io.vertx.core.json.JsonObject;
import leotech.system.model.GeoLocation;
import rfx.core.util.StringUtil;

/**
 * Free Ip To Location from https://ip-api.com/docs
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class IpToLocationService {

	private static final String LON = "lon";
	private static final String LAT = "lat";
	private static final String CITY = "city";
	private static final String COUNTRY = "country";
	private static final String HTTP_API_PREFIX = "http://ip-api.com/json/";
	
	private static String callWebApi(String ip) {
		String url = HTTP_API_PREFIX+ip;
		String jsonStr = HttpClientGetUtil.call(url);
		return jsonStr;
	}
	
	/**
	 * @param ip
	 * @return GeoLocation
	 */
	public static GeoLocation getGeoLocation(String ip) {
		String jsonStr = callWebApi(ip);
		if(StringUtil.isNotEmpty(jsonStr)) {
			try {
				JsonObject json = new JsonObject(jsonStr);
				if(check(json)) {
					GeoLocation geoLoc = convertJsonObjectToGeoLocation(ip, json);
					return geoLoc;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private static boolean check(JsonObject json) {
		return json.containsKey(COUNTRY) && json.containsKey(CITY) && json.containsKey(LAT) && json.containsKey(LON);
	}
	
	private static GeoLocation convertJsonObjectToGeoLocation(String ip, JsonObject json) {
		GeoLocation geoLoc = new GeoLocation("", 0);
		geoLoc.setIp(ip);
		geoLoc.setCityName(json.getString(CITY));
		
		Double latitude = json.getDouble(LAT);
		geoLoc.setLatitude(latitude);
		Double longitude = json.getDouble(LON);
		geoLoc.setLongitude(longitude);

		String locationCode = OpenLocationCode.encode(latitude, longitude);
		geoLoc.setLocationCode(locationCode);
		geoLoc.setCountry(json.getString(COUNTRY));
		return geoLoc;
	}
	
	public static void main(String[] args) {
		System.out.println(getGeoLocation("125.235.214.191"));
	}
}
