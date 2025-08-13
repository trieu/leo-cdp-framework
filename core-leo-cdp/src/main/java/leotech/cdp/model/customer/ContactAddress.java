package leotech.cdp.model.customer;

import java.util.Objects;

import com.google.gson.annotations.Expose;

/**
 * 
 * Data entity to store contact addresses
 * 
 * @author Trieu Nguyen (tantrieuf31)
 * @since 2023
 *
 */
public class ContactAddress {
	
	@Expose
	protected String locationCode = "";

	@Expose	
	protected String livingCountry = "";
	
	@Expose	
	protected String livingLocation = "";
	
	@Expose	
	protected String livingCity = "";
	
	public ContactAddress() {
		// json
	}
	
	public ContactAddress(String locationCode, String livingLocation, String livingCountry,  String livingCity) {
		super();
		this.locationCode = locationCode;
		this.livingLocation = livingLocation;
		this.livingCountry = livingCountry;
		this.livingCity = livingCity;				
	}

	public String getLocationCode() {
		return locationCode;
	}

	public void setLocationCode(String locationCode) {
		this.locationCode = locationCode;
	}

	public String getLivingCountry() {
		return livingCountry;
	}

	public void setLivingCountry(String livingCountry) {
		this.livingCountry = livingCountry;
	}

	public String getLivingCity() {
		return livingCity;
	}

	public void setLivingCity(String livingCity) {
		this.livingCity = livingCity;
	}

	public String getLivingLocation() {
		return livingLocation;
	}

	public void setLivingLocation(String livingLocation) {
		this.livingLocation = livingLocation;
	}

	@Override
	public boolean equals(Object obj) {	
		return this.hashCode() == obj.hashCode();
	}
	
	@Override
	public int hashCode() {		
		return Objects.hash(locationCode, livingLocation, livingCountry, livingCity);
	}
	
}
