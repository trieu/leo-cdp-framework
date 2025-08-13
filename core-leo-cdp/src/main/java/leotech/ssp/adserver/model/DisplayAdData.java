package leotech.ssp.adserver.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;



/**
 * @author tantrieuf31
 * @since 2020
 *
 */
public class DisplayAdData extends AdDataBaseModel {

	@Expose
	protected int timeToShowLogo = 1000;
	
	@Expose
	protected boolean hideLogo = false;
	
	@Expose
	protected int width = 0;
	
	@Expose
	protected int height = 0;
	
	@Expose
	protected int adFormatId = 0;
	
	@Expose
	protected List<String> tracking3rdUrls = new ArrayList<>();
	
	@Expose
	protected List<AdTrackingUrl> streamAdTrackingUrls;
	
	@Expose
	protected String js3rdCode;

	public DisplayAdData(String placementId, String adMedia, String clickthroughUrl, String clickActionText, String campaignId,
			int adType, int width, int height) {
		super();
		this.placementId = placementId;
		this.adMedia = adMedia;
		this.clickthroughUrl = clickthroughUrl;
		this.clickActionText = clickActionText;
		this.campaignId = campaignId;
		this.adType = adType;
		this.width = width;
		this.height = height;
	}

	public DisplayAdData(int adType, int width, int height, String placementId, String adCode) {
		super();
		this.adType = adType;
		this.width = width;
		this.height = height;
		this.placementId = placementId;
		if (adType == AdType.ADTYPE_BIDDING_AD) {
			this.backupAdCode = adCode;
			this.adCode = adCode;
		} else {
			this.adCode = adCode;
		}
	}

	public DisplayAdData(int adType, int width, int height, String placementId, String adCode, boolean hideLogo) {
		super();
		this.adType = adType;
		this.width = width;
		this.height = height;
		this.placementId = placementId;
		this.adCode = adCode;
		this.hideLogo = hideLogo;
	}

	public DisplayAdData(String campaignId, int adType, int width, int height, String placementId, String adCode) {
		super();
		this.campaignId = campaignId;
		this.adType = adType;
		this.width = width;
		this.height = height;
		this.placementId = placementId;
		this.adCode = adCode;
	}

	public DisplayAdData() {
		super();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public List<String> getTracking3rdUrls() {
		return tracking3rdUrls;
	}

	public void addTracking3rdUrl(String tracking3rdUrl) {
		this.tracking3rdUrls.add(tracking3rdUrl);
	}

	public void addTracking3rdUrls(List<String> tracking3rdUrls) {
		this.tracking3rdUrls.addAll(tracking3rdUrls);
	}

	public void setStreamAdTrackingUrls(List<AdTrackingUrl> streamAdTrackingUrls) {
		this.streamAdTrackingUrls = streamAdTrackingUrls;
	}

	public void addStreamAdTrackingUrl(AdTrackingUrl streamAdTrackingUrl) {
		if (this.streamAdTrackingUrls == null) {
			this.streamAdTrackingUrls = new ArrayList<>();
		}
		this.streamAdTrackingUrls.add(streamAdTrackingUrl);
	}

	public List<AdTrackingUrl> getStreamAdTrackingUrls() {
		return streamAdTrackingUrls;
	}

	public String getJs3rdCode() {
		return js3rdCode;
	}

	public void setJs3rdCode(String js3rdCode) {
		this.js3rdCode = js3rdCode;
	}

	public boolean isHideLogo() {
		return hideLogo;
	}

	public void setHideLogo(boolean hideLogo) {
		this.hideLogo = hideLogo;
	}

	public int getTimeToShowLogo() {
		return timeToShowLogo;
	}

	public void setTimeToShowLogo(int timeToShowLogo) {
		this.timeToShowLogo = timeToShowLogo;
	}

	public int getAdFormatId() {
		return adFormatId;
	}

	public void setAdFormatId(int adFormatId) {
		this.adFormatId = adFormatId;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}