package leotech.system.model;

import rfx.core.util.StringUtil;

/**
 * for API of openstreetmap <br>
 * E.g https://nominatim.openstreetmap.org/reverse?format=json&lat=59.9358091&lon=30.3126222
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public class OsmGeoLocation {
	
	public static final class Address {

		String town = "";
		String county = "";		
		String city = "";
		String region = "";
		String state= "";
		String country= "";
		
		public Address() {
		}
		
		public String toLocationString() {
			StringBuilder s = new StringBuilder();
			if(StringUtil.isNotEmpty(town)) {
				s.append(town).append(", ");	
			}
			if(StringUtil.isNotEmpty(county)) {
				s.append(county).append(", ");	
			}
			if(StringUtil.isNotEmpty(city) && !county.equals(city)) {
				s.append(city).append(", ");	
			}
			if(StringUtil.isNotEmpty(region) && !region.equals(city)) {
				s.append(region).append(", ");	
			}
			if(StringUtil.isNotEmpty(state)) {
				s.append(state).append(", ");	
			}
			if(StringUtil.isNotEmpty(country)) {
				s.append(country);	
			}
			return s.toString();
		}
			
	}

	String place_id = "";
	String lat= "";
	String lon= "";
	String display_name= "";
	Address address = new Address();
	
	
	public OsmGeoLocation() {
		
	}
	
	public String getLocationName() {
		return address.toLocationString();
	}

	public String getPlace_id() {
		return place_id;
	}

	public void setPlace_id(String place_id) {
		this.place_id = place_id;
	}
	
	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLon() {
		return lon;
	}

	public void setLon(String lon) {
		this.lon = lon;
	}

	public String getDisplay_name() {
		return display_name;
	}

	public void setDisplay_name(String display_name) {
		this.display_name = display_name;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
	
	
}
