package leotech.cdp.model.journey;

import com.google.gson.Gson;

import leotech.system.util.QrCodeUtil;

/**
 * Data Object to store QR code metadata
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class QrCodeData {

	String trackingUrl = "";
	String shortUrl = "";
	
	String qrCodeImage = "";
	String landingPageUrl = "";
	
	public QrCodeData() {
		
	}

	public QrCodeData(String trackingUrl,String shortLinkUrl, String landingPageUrl, String qrImagePrefix) {
		super();
		this.trackingUrl = trackingUrl;
		this.shortUrl = shortLinkUrl;
		this.landingPageUrl = landingPageUrl;
		this.qrCodeImage = QrCodeUtil.generate(qrImagePrefix, trackingUrl);
	}

	public String getTrackingUrl() {
		return trackingUrl;
	}

	public void setTrackingUrl(String trackingUrl) {
		this.trackingUrl = trackingUrl;
	}

	public String getLandingPageUrl() {
		return landingPageUrl;
	}

	public void setLandingPageUrl(String landingPageUrl) {
		this.landingPageUrl = landingPageUrl;
	}

	public String getQrCodeImage() {
		return qrCodeImage;
	}

	public void setQrCodeImage(String qrCodeImage) {
		this.qrCodeImage = qrCodeImage;
	}
	
	public String getShortUrl() {
		return shortUrl;
	}
	
	@Override
	public int hashCode() {
		return landingPageUrl != null ? super.hashCode() : 0;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
}
