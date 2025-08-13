package leotech.system.util;

import static io.vertx.core.http.HttpHeaders.COOKIE;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.openlocationcode.OpenLocationCode;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Location;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import leotech.system.common.BaseHttpRouter;
import leotech.system.model.GeoLocation;
import leotech.system.model.OsmGeoLocation;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * Geo Location fro IP and location code
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class GeoLocationUtil {

	private static final String OPENSTREETMAP_SEARCH_BASE = "https://nominatim.openstreetmap.org/search?format=json&q=";
	static final int CACHE_TIME = 12;
	
	static final LoadingCache<String, GeoLocation> geoLocationCache = CacheBuilder.newBuilder().maximumSize(9000000)
			.expireAfterWrite(CACHE_TIME, TimeUnit.HOURS).build(new CacheLoader<String, GeoLocation>() {
				@Override
				public GeoLocation load(String ip) {
					return findGeoLocation(ip);
				}
			});
	
	static final LoadingCache<String, String> locationCodeQueryCache = CacheBuilder.newBuilder().maximumSize(9000000)
			.expireAfterWrite(CACHE_TIME, TimeUnit.HOURS).build(new CacheLoader<String, String>() {
				@Override
				public String load(String livingLocation) {
					return GeoLocationUtil.getLocationCodeFromOpenStreetMap(livingLocation);
				}
			});

	public static final String COOKIE_DOMAIN = "";// TODO
	public static final String COOKIE_LOCATION_ID = "leogeoid";
	public static final String COOKIE_LOCATION_CITY = "leogeoname";

	// An instance of MAXMIND database
	static volatile DatabaseReader mmdbReader = null;
	// init mmdbReader as static 
	static {
		try {
			String filePath = SystemMetaData.PATH_MAXMIND_DATABASE_FILE;
			if(StringUtil.isNotEmpty(filePath)) {
				File database = new File(filePath);
				if(database.isFile()) {
					mmdbReader = new DatabaseReader.Builder(database).build();
					System.out.println("[GeoLocationUtil] OK loaded Maxmind Database File: "+database.getAbsolutePath());
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

	/**
	 * @param ip
	 * @return
	 */
	public static final String getCityLocation(String ip) {
		String rs = "";
		if (mmdbReader != null) {
			try {
				InetAddress ipAddress = InetAddress.getByName(ip);
				CityResponse response = mmdbReader.city(ipAddress);
				City city = response.getCity();
				// System.out.println(city.getName());
				rs = city.getGeoNameId() + "__" + city.getName();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return rs;
	}

	/**
	 * @param req
	 * @return
	 */
	public static GeoLocation getValueFromCookie(HttpServerRequest req) {
		GeoLocation loc = new GeoLocation();
		String cookieString = req.headers().get(COOKIE);
		// System.out.println(cookieString);
		if (cookieString != null) {
			try {
				cookieString = URLDecoder.decode(cookieString, "UTF-8");
				Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
				// System.out.println("cookies "+cookies);
				for (Cookie cookie : cookies) {
					String name = cookie.name();
					if (name.equals(COOKIE_LOCATION_ID)) {
						int id = StringUtil.safeParseInt(cookie.value());
						loc.setGeoNameId(id);
					} else if (name.equals(COOKIE_LOCATION_CITY)) {
						String v = StringUtil.safeString(cookie.value());
						loc.setCityName(v);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return loc;
	}

	/**
	 * @param req
	 * @param resp
	 * @return
	 */
	public static GeoLocation processGeoLocation(HttpServerRequest req, HttpServerResponse resp) {
		GeoLocation loc = getValueFromCookie(req);
		if (loc.getGeoNameId() == 0) {
			String ip = HttpWebParamUtil.getRemoteIP(req);
			loc = getGeoLocation(ip);

			Cookie cookie = new DefaultCookie(COOKIE_LOCATION_ID, String.valueOf(loc.getGeoNameId()));
			cookie.setHttpOnly(false);
			cookie.setMaxAge(BaseHttpRouter.COOKIE_AGE_1_WEEK);
			cookie.setPath("/");
			cookie.setDomain(COOKIE_DOMAIN);
			resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(cookie));

			Cookie cookie2 = new DefaultCookie(COOKIE_LOCATION_CITY, String.valueOf(loc.getCityName()));
			cookie2.setHttpOnly(false);
			cookie2.setMaxAge(BaseHttpRouter.COOKIE_AGE_1_WEEK);
			cookie2.setPath("/");
			cookie2.setDomain(COOKIE_DOMAIN);
			resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(cookie2));
		}
		return loc;
	}

	/**
	 * @param ip
	 * @return
	 */
	public static final long getGeoNameId(String ip) {
		long geoNameId = 0;
		if (mmdbReader != null) {
			try {
				InetAddress ipAddress = InetAddress.getByName(ip);
				CityResponse response = mmdbReader.city(ipAddress);
				City city = response.getCity();
				System.out.println(city.getName());
				geoNameId = city.getGeoNameId();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return geoNameId;
	}

	/**
	 * @param ip
	 * @return GeoLocation
	 */
	public static final GeoLocation getGeoLocation(String ip) {
		try {
			if (!ip.equals("127.0.0.1")) {
				return geoLocationCache.get(ip);
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return new GeoLocation("", 1566083);
	}

	/**
	 * @param ip
	 * @return GeoLocation
	 */
	public static final GeoLocation findGeoLocation(String ip) {
		GeoLocation geoLoc = null;
		if(mmdbReader != null) {
			try {
				// fix for Google Cloud Proxy
				String[] toks = ip.split(",");
				if( toks.length > 0 ) {
					ip = toks[0];
				}
				
				InetAddress ipAddress = InetAddress.getByName(ip);
				CityResponse response = mmdbReader.city(ipAddress);
				if (response != null) {
					geoLoc = new GeoLocation("", 0);
					City city = response.getCity();
					if (city.getGeoNameId() != null) {
						geoLoc.setIp(ip);
						geoLoc.setCityName(city.getName());
						geoLoc.setGeoNameId(city.getGeoNameId());
						Location location = response.getLocation();
						Double latitude = location.getLatitude();
						geoLoc.setLatitude(latitude);
						Double longitude = location.getLongitude();
						geoLoc.setLongitude(longitude);
						
						geoLoc.setCountry(response.getCountry().getName());

						// convert lat, lon into locationCode for caching and storing
						String locationCode = OpenLocationCode.encode(latitude, longitude);
						geoLoc.setLocationCode(locationCode);
					}
					else {
						geoLoc = IpToLocationService.getGeoLocation(ip);
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		else {
			geoLoc = IpToLocationService.getGeoLocation(ip);
		}
		
		if(geoLoc == null) {
			geoLoc = new GeoLocation("", 0);
		}
		return geoLoc;
	}
	
	/**
	 * @param lat
	 * @param lon
	 * @return OsmGeoLocation
	 */
	public static OsmGeoLocation getLocationName(double lat, double lon) {
		String json = "";
		try {
			String url = "http://nominatim.openstreetmap.org/reverse?format=json&lat="+lat+"&lon="+lon;
			json = HttpClientGetUtil.call(url);
			System.out.println(json);
			if(StringUtil.isNotEmpty(json)) {
				return new Gson().fromJson(json, OsmGeoLocation.class);
			}
		} catch (Throwable e) {
			System.err.println("getLocationName, lat:"+lat + " lon:" + lon);
			System.err.println(json);
			e.printStackTrace();
		}
		return new OsmGeoLocation();
	}
	
	/**
	 * get OpenLocationCode from OPENSTREETMAP_SEARCH_BASE
	 * 
	 * @param locationQuery
	 * @return locationCode for profile
	 */
	public static String getLocationCodeFromOpenStreetMap(String locationQuery) {
		String url = OPENSTREETMAP_SEARCH_BASE + urlEncode(locationQuery);
		String json = HttpClientGetUtil.call(url);
		System.out.println(url + " \n LocationCodeFromLocationQuery \n " + json);
		
		@SuppressWarnings("serial")
		Type listType = new TypeToken<ArrayList<OsmGeoLocation>>(){}.getType();
		List<OsmGeoLocation>  queriedLocations = new Gson().fromJson(json, listType);
		
		String code = null;
		if(queriedLocations.size() > 0) {
			OsmGeoLocation locationData = queriedLocations.get(0);
			double lat = StringUtil.safeParseDouble(locationData.getLat());
			double lon = StringUtil.safeParseDouble(locationData.getLon());
			code = OpenLocationCode.encode(lat, lon);
		}
		return StringUtil.safeString(code);
	}
	
	public static String getLocationCodeFromLocationQuery(String locationQuery) {
		try {
			if (StringUtil.isNotEmpty(locationQuery)) {
				return locationCodeQueryCache.get(locationQuery);
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return "";
	}

	private static String urlEncode(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static void main(String[] args) throws Exception {
		//System.out.println("getLocationCodeFromLocationQuery " + getLocationCodeFromLocationQuery("AEON Mall Bình Tân"));
		GeoLocation geoLocation = getGeoLocation("125.235.214.191");
		System.out.println("test " + geoLocation);
		System.out.println(getLocationName(geoLocation.getLatitude(),geoLocation.getLongitude()).getLocationName());
		
//		OsmGeoLocation openStreetMapObj = GeoLocationUtil.getLocationName(10.6533482,106.870646);
//		System.out.println(openStreetMapObj.getLocationName());
	}
}
