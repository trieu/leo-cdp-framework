package leotech.system.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import leotech.cdp.model.asset.ContentClassPostQuery;
import leotech.system.version.SystemMetaData;
import rfx.core.util.HashUtil;
import rfx.core.util.StringUtil;


public final class AppMetadata {

	public static final long DEFAULT_ID = 10000L;

	// data for website and progressive web app
	String domain = SystemMetaData.DOMAIN_CDP_ADMIN;
	
	
	
	String baseLeoObserverUrl = SystemMetaData.DOMAIN_CDP_OBSERVER;
	
	String dnsDomainLeoBot = SystemMetaData.DOMAIN_CHATBOT;

	String dnsDomainLeoAdmin = SystemMetaData.DOMAIN_CDP_ADMIN;
	
	String dnsDomainWebSocket = SystemMetaData.DOMAIN_WEB_SOCKET;
	
	String baseStaticUrl = SystemMetaData.DOMAIN_STATIC_CDN;
	
	String baseUploaderUrl = SystemMetaData.DOMAIN_CDP_ADMIN;
	
	String baseUploadedFilesUrl = SystemMetaData.DOMAIN_UPLOADED_FILES;
	

	
	long appId = DEFAULT_ID;
	
	String name = ""; // Big Data Vietnam Content Network
	
	String uri = ""; //
	
	String webTemplateFolder = ""; // e.g: bigdatavietnam
	
	
	boolean webTemplateCache = true;
	
	boolean dataApiCache = false;

	// data for native app on mobile devices (Android, iOS)
	
	String androidAppId = ""; // e.g: bigdatavietnam-org
	
	String iosAppId = "";
	
	String appTemplateFolder = ""; //

	// --------------------------------------------
	
	String contentCategoryId = "";
	
	String publicContentClass = "";
	
	List<ContentClassPostQuery> contentClassPostQueries;
	
	String pageTitle = "";
	
	String pageHeaderLogo = "";
	
	String baseDeliveryApiUrl = "";
	
	String adsTxtContent = "";
	
	String googleTrackingId = "";

	int numberSimilarPostsInList = 7;

	public AppMetadata() {
	}

	public AppMetadata(String name, String uri, String domain, String webTemplateFolder) {
		super();
		this.name = name;
		this.uri = uri;
		this.domain = domain;
		this.webTemplateFolder = webTemplateFolder;

		if (!uri.equals("admin")) {
			this.appId = hashToNetworkId(domain);
		}
	}

	public static long hashToNetworkId(String domain) {
		return DEFAULT_ID + HashUtil.hashUrl128Bit(domain);
	}

	public long getAppId() {
		return appId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setAppId(long appId) {
		this.appId = appId;
	}

	public String getWebTemplateFolder() {
		return webTemplateFolder;
	}

	public void setWebTemplateFolder(String webTemplateFolder) {
		this.webTemplateFolder = webTemplateFolder;
	}

	public String getAndroidAppId() {
		return androidAppId;
	}

	public void setAndroidAppId(String androidAppId) {
		this.androidAppId = androidAppId;
	}

	public String getIosAppId() {
		return iosAppId;
	}

	public void setIosAppId(String iosAppId) {
		this.iosAppId = iosAppId;
	}

	public String getAppTemplateFolder() {
		return appTemplateFolder;
	}

	public void setAppTemplateFolder(String appTemplateFolder) {
		this.appTemplateFolder = appTemplateFolder;
	}

	public List<ContentClassPostQuery> getContentClassPostQueries() {
		return contentClassPostQueries;
	}

	public void setContentClassPostQueries(List<ContentClassPostQuery> contentClassPostQueries) {
		this.contentClassPostQueries = contentClassPostQueries;
	}

	public String getContentCategoryId() {
		return contentCategoryId;
	}

	public void setContentCategoryId(String contentCategoryId) {
		this.contentCategoryId = contentCategoryId;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public String getPageHeaderLogo() {
		return pageHeaderLogo;
	}

	public void setPageHeaderLogo(String pageHeaderLogo) {
		this.pageHeaderLogo = pageHeaderLogo;
	}

	public String getBaseStaticUrl() {
		return baseStaticUrl;
	}

	public void setBaseStaticUrl(String baseStaticUrl) {
		this.baseStaticUrl = baseStaticUrl;
	}

	public String getDnsDomainLeoAdmin() {
		return dnsDomainLeoAdmin;
	}

	public void setDnsDomainLeoAdmin(String dnsDomainLeoAdmin) {
		this.dnsDomainLeoAdmin = dnsDomainLeoAdmin;
	}
	
	

	public String getDnsDomainWebSocket() {
		return dnsDomainWebSocket;
	}

	public void setDnsDomainWebSocket(String dnsDomainWebSocket) {
		this.dnsDomainWebSocket = dnsDomainWebSocket;
	}

	public String getBaseDeliveryApiUrl() {
		return baseDeliveryApiUrl;
	}

	public void setBaseDeliveryApiUrl(String baseDeliveryApiUrl) {
		this.baseDeliveryApiUrl = baseDeliveryApiUrl;
	}

	public String getBaseUploaderUrl() {
		return baseUploaderUrl;
	}

	public void setBaseUploaderUrl(String baseUploaderUrl) {
		this.baseUploaderUrl = baseUploaderUrl;
	}
	
	public String getBaseUploadedFilesUrl() {
		return baseUploadedFilesUrl;
	}

	public void setBaseUploadedFilesUrl(String baseUploadedFilesUrl) {
		this.baseUploadedFilesUrl = baseUploadedFilesUrl;
	}

	public String getBaseLeoObserverUrl() {
		return baseLeoObserverUrl;
	}

	public void setBaseLeoObserverUrl(String baseLeoObserverUrl) {
		this.baseLeoObserverUrl = baseLeoObserverUrl;
	}



	public String getDnsDomainLeoBot() {
		return dnsDomainLeoBot;
	}

	public void setDnsDomainLeoBot(String dnsDomainLeoBot) {
		this.dnsDomainLeoBot = dnsDomainLeoBot;
	}

	public String getAdsTxtContent() {
		return adsTxtContent;
	}

	public void setAdsTxtContent(String adsTxtContent) {
		this.adsTxtContent = adsTxtContent;
	}

	public String getGoogleTrackingId() {
		return googleTrackingId;
	}

	public void setGoogleTrackingId(String googleTrackingId) {
		this.googleTrackingId = googleTrackingId;
	}

	public int getNumberSimilarPostsInList() {
		return numberSimilarPostsInList;
	}

	public void setNumberSimilarPostsInList(int numberSimilarPostsInList) {
		this.numberSimilarPostsInList = numberSimilarPostsInList;
	}
	
	public boolean isWebTemplateCache() {
		return webTemplateCache;
	}

	public void setWebTemplateCache(boolean webTemplateCache) {
		this.webTemplateCache = webTemplateCache;
	}

	public boolean isDataApiCache() {
		return dataApiCache;
	}

	public void setDataApiCache(boolean dataApiCache) {
		this.dataApiCache = dataApiCache;
	}

	public String getPublicContentClass() {
		return publicContentClass;
	}

	public List<String> getPublicContentClassList() {
		if (StringUtil.isEmpty(publicContentClass)) {
			publicContentClass = "standard";
		}
		String[] toks = publicContentClass.split(",");
		List<String> contentClasses = new ArrayList<String>(toks.length);
		for (int i = 0; i < toks.length; i++) {
			contentClasses.add(toks[i]);
		}
		return contentClasses;
	}

	public void setPublicContentClass(String publicContentClass) {
		this.publicContentClass = publicContentClass;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
