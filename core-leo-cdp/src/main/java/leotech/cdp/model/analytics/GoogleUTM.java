package leotech.cdp.model.analytics;

import java.util.Map;

import rfx.core.util.StringUtil;

/**
 *  Data model to store Google UTM parameter, https://ga-dev-tools.google/ga4/campaign-url-builder/
 *  
 * @author tantrieuf31
 * @since 2024
 *
 */
public class GoogleUTM {

	String utmId; // Campaign ID
	String utmSource; // Campaign Source
	String utmMedium; // Campaign Medium
	String utmCampaign; // Campaign Name
	String utmTerm; // Campaign Term
	String utmContent; // Campaign Content
	
	public GoogleUTM() {
		
	}
	
	public GoogleUTM(Map<String, Object> eventData) {
		if(eventData != null) {
			this.utmId = eventData.getOrDefault("utm_id", "").toString();
			this.utmSource = eventData.getOrDefault("utm_source", "").toString();
			this.utmMedium = eventData.getOrDefault("utm_medium", "").toString();
			this.utmCampaign = eventData.getOrDefault("utm_campaign", "").toString();
			this.utmTerm = eventData.getOrDefault("utm_term", "").toString();
			this.utmContent = eventData.getOrDefault("utm_content", "").toString();
		}
	}

	public GoogleUTM(String utmId, String utmSource, String utmMedium, String utmCampaign, String utmTerm,
			String utmContent) {
		super();
		this.utmId = utmId;
		this.utmSource = utmSource;
		this.utmMedium = utmMedium;
		this.utmCampaign = utmCampaign;
		this.utmTerm = utmTerm;
		this.utmContent = utmContent;
	}

	public String getUtmId() {
		return utmId;
	}

	public void setUtmId(String utmId) {
		this.utmId = utmId;
	}

	public String getUtmSource() {
		return utmSource;
	}

	public void setUtmSource(String utmSource) {
		this.utmSource = utmSource;
	}

	public String getUtmMedium() {
		return utmMedium;
	}

	public void setUtmMedium(String utmMedium) {
		this.utmMedium = utmMedium;
	}

	public String getUtmCampaign() {
		return utmCampaign;
	}

	public void setUtmCampaign(String utmCampaign) {
		this.utmCampaign = utmCampaign;
	}

	public String getUtmTerm() {
		return utmTerm;
	}

	public void setUtmTerm(String utmTerm) {
		this.utmTerm = utmTerm;
	}

	public String getUtmContent() {
		return utmContent;
	}

	public void setUtmContent(String utmContent) {
		this.utmContent = utmContent;
	}

	@Override
	public int hashCode() {
		return StringUtil.join("", utmId, utmSource, utmMedium, utmCampaign, utmTerm, utmContent).hashCode();
	}
	
	public boolean isEmpty() {
		return StringUtil.isEmpty(utmId) && StringUtil.isEmpty(utmMedium) && StringUtil.isEmpty(utmSource) 
				&& StringUtil.isEmpty(utmCampaign) && StringUtil.isEmpty(utmContent) && StringUtil.isEmpty(utmTerm);
	}

	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}
	
	
}
