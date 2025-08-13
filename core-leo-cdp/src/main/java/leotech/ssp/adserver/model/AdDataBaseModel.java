package leotech.ssp.adserver.model;

import com.google.gson.annotations.Expose;

/**
 * @author trieu
 * 
 *         base model for special ad data format (mobile, infeed, inpage,  masterhead, 360, lightbox)
 *
 */
public abstract class AdDataBaseModel {

	@Expose
	protected String adMedia;// Video Ad URL or Banner Ad URL
	
	@Expose
	protected String clickthroughUrl;// Call to action destination URL
	
	@Expose
	protected String clickActionText;// Call to action text
	
	@Expose
	protected String campaignId;
	
	@Expose
	protected String adBeacon;
	
	@Expose
	protected int adType = 0;
	
	@Expose
	protected String placementId = "";
	
	@Expose
	protected String adCode = "";// 3rd JavaScript code
	
	@Expose
	protected String backupAdCode = "";// backup ad-code of placement

	public AdDataBaseModel() {
		super();
	}

	public String getAdText() {
		return adMedia;
	}

	public String getClickthroughUrl() {
		return clickthroughUrl;
	}

	public String getClickActionText() {
		return clickActionText;
	}

	public String getAdBeacon() {
		return adBeacon;
	}

	public void setAdText(String adText) {
		this.adMedia = adText;
	}

	public void setClickthroughUrl(String clickthroughUrl) {
		this.clickthroughUrl = clickthroughUrl;
	}

	public void setClickActionText(String clickActionText) {
		this.clickActionText = clickActionText;
	}

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}

	public int getAdType() {
		return adType;
	}

	public String getAdCode() {
		if (adCode == null) {
			adCode = "";
		}
		return adCode;
	}

	public void setAdCode(String adCode) {
		this.adCode = adCode;
	}

	public String getAdMedia() {
		if (adMedia == null) {
			adMedia = "";
		}
		return adMedia;
	}

	public void setAdMedia(String adMedia) {
		this.adMedia = adMedia;
	}

	

	public String getPlacementId() {
		return placementId;
	}

	public void setPlacementId(String placementId) {
		this.placementId = placementId;
	}

	public void setAdBeacon(String adBeacon) {
		this.adBeacon = adBeacon;
	}

	public void setAdType(int adType) {
		this.adType = adType;
	}

	public final AdDataBaseModel setBeaconData(String beacon) {
		this.adBeacon = beacon;
		return this;
	}

	public String getBackupAdCode() {
		return backupAdCode;
	}

	public void setBackupAdCode(String backupAdCode) {
		this.backupAdCode = backupAdCode;
	}

}