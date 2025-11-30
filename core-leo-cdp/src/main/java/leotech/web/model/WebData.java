package leotech.web.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpStatus;

import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.customer.ProfileModelUtil;
import leotech.system.model.AppMetadata;
import leotech.system.template.TemplateUtil;
import leotech.system.util.AppMetadataUtil;
import leotech.system.util.database.InitDatabaseSchema;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * Web Data Object for HTML applications: Admin, Content and Event Observer
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public class WebData extends DefaultModel {
	private static final String BUILD_KEY = SystemMetaData.BUILD_VERSION + System.currentTimeMillis();

	protected String pageHeaderLogo;

	protected String baseStaticUrl;
	protected String dnsDomainLeoAdmin;
	protected String dnsDomainWebSocket;

	protected String dnsDomainLeoBot;
	protected String baseDeliveryApiUrl;
	protected String baseUploaderUrl;
	protected String baseUploadedFilesUrl;
	protected String baseLeoObserverUrl;

	protected boolean webTemplateCache = true;
	protected boolean dataApiCache = false;

	protected int httpStatusCode = HttpStatus.SC_OK;
	protected final String host;
	protected final String templateFolder;
	protected final String templateName;

	protected List<CategoryNavigator> categoryNavigators = new ArrayList<>();
	protected List<PageNavigator> topPageNavigators = new ArrayList<>();
	protected List<PageNavigator> bottomPageNavigators = new ArrayList<>();

	protected List<AssetContent> headlines = new ArrayList<>();
	protected List<ContentMediaBox> contentMediaBoxs = new ArrayList<>();

	protected Map<String, Object> customData = new HashMap<>();

	// data for meta tags in HTML
	protected String pageName = "";
	protected String pageTitle = "";
	protected String pageUrl = "";
	protected String pageImage = "";
	protected String pageDescription = "";
	protected String pageKeywords = "";
	protected String defaultTrackingMetric = "";

	// important data to check version
	protected boolean systemDbReady = InitDatabaseSchema.isSystemDbReady();
	protected boolean hasB2Bfeatures = SystemMetaData.hasB2Bfeatures();
	protected String leoCdpBuildVersion = SystemMetaData.BUILD_ID;

	protected String minifySuffix = SystemMetaData.MINIFY_SUFFIX;

	protected String industryDataModels = SystemMetaData.INDUSTRY_DATA_MODELS_STRING;

	protected String defaultPersonalizationService = SystemMetaData.DEFAULT_PERSONALIZATION_SERVICE;
	protected int maxTotalRecommendedItems = SystemMetaData.MAX_TOTAL_RECOMMENDED_ITEMS;

	
	protected boolean showDefaultLogin ;
	protected boolean ssoLogin ;
	protected String ssoSessionUrl ;
	protected String ssoLogoutUrl;

	protected String systemConfigJson = "{}";
	protected String contactTypeJson = "{}";

	/**
	 * to avoid browser caching for static files: HTML, CSS, JS in DEV mode
	 */
	protected String cacheBustingKey = StringUtil.isEmpty(SystemMetaData.MINIFY_SUFFIX)
			? String.valueOf(System.currentTimeMillis())
			: BUILD_KEY;

	protected int maxTotalDataQualityScore = ProfileModelUtil.getMaxTotalDataQualityScore();

	public WebData(String host, String templateFolder, String templateName) {
		this.setBaseData(host);
		this.host = host;
		this.templateFolder = templateFolder;
		this.templateName = templateName;
		this.httpStatusCode = HttpStatus.SC_OK;
	}

	public WebData(String host, String templateFolder, String templateName, int statusCode) {
		this.setBaseData(host);
		this.host = host;
		this.templateFolder = templateFolder;
		this.templateName = templateName;
		this.httpStatusCode = statusCode;
	}

	public WebData(String host, String templateFolder, String templateName, String pageTitle) {
		this.setBaseData(host);
		this.host = host;
		this.pageName = host;
		this.templateFolder = templateFolder;
		this.templateName = templateName;
		this.pageTitle = pageTitle;
	}

	public static WebData page404(String host, String templateFolder) {
		return new WebData(host, templateFolder, "404", HttpStatus.SC_NOT_FOUND);
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public String getDefaultTrackingMetric() {
		return defaultTrackingMetric;
	}

	public void setDefaultTrackingMetric(String defaultTrackingMetric) {
		this.defaultTrackingMetric = defaultTrackingMetric;
	}

	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
	}

	public String getPageImage() {
		return pageImage;
	}

	public void setPageImage(String pageImage) {
		this.pageImage = pageImage;
	}

	public String getPageDescription() {
		return pageDescription;
	}

	public void setPageDescription(String pageDescription) {
		this.pageDescription = pageDescription;
	}

	public String getPageKeywords() {
		return pageKeywords;
	}

	public void setPageKeywords(String pageKeywords) {
		this.pageKeywords = pageKeywords;
	}

	public void setPageKeywords(Set<String> listKeywords) {
		this.pageKeywords = StringUtil.join(", ", listKeywords);
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

	public String getDnsDomainLeoBot() {
		return dnsDomainLeoBot;
	}

	public void setDnsDomainLeoBot(String dnsDomainLeoBot) {
		this.dnsDomainLeoBot = dnsDomainLeoBot;
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

	public void setDefaultPersonalizationService(String defaultPersonalizationService) {
		this.defaultPersonalizationService = defaultPersonalizationService;
	}

	public void setSystemDbReady(boolean systemDbReady) {
		this.systemDbReady = systemDbReady;
	}

	public void setMaxTotalDataQualityScore(int maxTotalDataQualityScore) {
		this.maxTotalDataQualityScore = maxTotalDataQualityScore;
	}

	public int getMaxTotalRecommendedItems() {
		return maxTotalRecommendedItems;
	}

	public void setMaxTotalRecommendedItems(int maxTotalRecommendedItems) {
		this.maxTotalRecommendedItems = maxTotalRecommendedItems;
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

	public boolean isHasB2Bfeatures() {
		return hasB2Bfeatures;
	}

	public void setHasB2Bfeatures(boolean hasB2Bfeatures) {
		this.hasB2Bfeatures = hasB2Bfeatures;
	}

	public boolean isSystemDbReady() {
		return systemDbReady;
	}

	public String getIndustryDataModels() {
		return industryDataModels;
	}

	public void setIndustryDataModels(String industryDataModels) {
		this.industryDataModels = industryDataModels;
	}

	public int getMaxTotalDataQualityScore() {
		return maxTotalDataQualityScore;
	}

	public void setCacheBustingKey(String cacheBustingKey) {
		this.cacheBustingKey = cacheBustingKey;
	}

	public String getBaseLeoObserverUrl() {
		return baseLeoObserverUrl;
	}

	public void setBaseLeoObserverUrl(String baseLeoObserverUrl) {
		this.baseLeoObserverUrl = baseLeoObserverUrl;
	}

	public String getHost() {
		return host;
	}

	public String getTemplateFolder() {
		return templateFolder;
	}

	public String getTemplateName() {
		return templateName;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}

	public void setHttpStatusCode(int httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}

	public List<CategoryNavigator> getCategoryNavigators() {
		return categoryNavigators;
	}

	public void setCategoryNavigators(List<CategoryNavigator> categoryNavigators) {
		Collections.sort(categoryNavigators);
		this.categoryNavigators = categoryNavigators;
	}

	public void addCategoryNavigator(CategoryNavigator categoryNavigator) {
		this.categoryNavigators.add(categoryNavigator);
	}

	public List<AssetContent> getHeadlines() {
		return headlines;
	}

	public void setHeadlines(List<AssetContent> headlines) {
		this.headlines = headlines;
	}

	public List<ContentMediaBox> getContentMediaBoxs() {
		return contentMediaBoxs;
	}

	public void setContentMediaBoxs(List<ContentMediaBox> contentMediaBoxs) {
		this.contentMediaBoxs = contentMediaBoxs;
	}

	public List<PageNavigator> getTopPageNavigators() {
		return topPageNavigators;
	}

	public void setTopPageNavigators(List<PageNavigator> topPageNavigators) {
		Collections.sort(topPageNavigators);
		this.topPageNavigators = topPageNavigators;
	}

	public void setTopPageNavigators(PageNavigator topPageNavigator) {
		this.topPageNavigators.add(topPageNavigator);
	}

	public List<PageNavigator> getBottomPageNavigators() {
		return bottomPageNavigators;
	}

	public void setBottomPageNavigators(List<PageNavigator> bottomPageNavigators) {
		this.bottomPageNavigators = bottomPageNavigators;
	}

	public Map<String, Object> getCustomData() {
		return customData;
	}

	public void setCustomData(Map<String, Object> customData) {
		this.customData = customData;
	}

	public void setCustomData(String key, Object value) {
		this.customData.put(key, value);
	}

	public String getLeoCdpBuildVersion() {
		return leoCdpBuildVersion;
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

	
	
	public boolean isShowDefaultLogin() {
		return showDefaultLogin;
	}

	public void setShowDefaultLogin(boolean showDefaultLogin) {
		this.showDefaultLogin = showDefaultLogin;
	}

	public boolean isSsoLogin() {
		return ssoLogin;
	}

	public void setSsoLogin(boolean ssoLogin) {
		this.ssoLogin = ssoLogin;
	}

	public String getSsoSessionUrl() {
		return ssoSessionUrl;
	}

	public void setSsoSessionUrl(String ssoSessionUrl) {
		this.ssoSessionUrl = ssoSessionUrl;
	}

	public String getSsoLogoutUrl() {
		return ssoLogoutUrl;
	}

	public void setSsoLogoutUrl(String ssoLogoutUrl) {
		this.ssoLogoutUrl = ssoLogoutUrl;
	}

	public void setLeoCdpBuildVersion(String leoCdpBuildVersion) {
		this.leoCdpBuildVersion = leoCdpBuildVersion;
	}

	public String getBaseUploadedFilesUrl() {
		return baseUploadedFilesUrl;
	}

	public void setBaseUploadedFilesUrl(String baseUploadedFilesUrl) {
		this.baseUploadedFilesUrl = baseUploadedFilesUrl;
	}

	public String getMinifySuffix() {
		return minifySuffix;
	}

	public void setMinifySuffix(String minifySuffix) {
		this.minifySuffix = minifySuffix;
	}

	public String getSystemConfigJson() {
		return systemConfigJson;
	}

	public void setSystemConfigJson(String systemConfigJson) {
		this.systemConfigJson = systemConfigJson;
	}

	public String getContactTypeJson() {
		return contactTypeJson;
	}

	public void setContactTypeJson(String contactTypeJson) {
		this.contactTypeJson = contactTypeJson;
	}

	public String getCacheBustingKey() {
		return cacheBustingKey;
	}

	public String getDefaultPersonalizationService() {
		return defaultPersonalizationService;
	}

	public void setBaseData(String host) {
		AppMetadata app = AppMetadataUtil.getContentNetwork(host);
		this.pageTitle = app.getPageTitle();
		this.baseStaticUrl = app.getBaseStaticUrl();
		this.baseDeliveryApiUrl = app.getBaseDeliveryApiUrl();
		this.dnsDomainLeoAdmin = app.getDnsDomainLeoAdmin();
		this.dnsDomainWebSocket = app.getDnsDomainWebSocket();
		this.dnsDomainLeoBot = app.getDnsDomainLeoBot();
		this.baseUploaderUrl = app.getBaseUploaderUrl();
		this.baseUploadedFilesUrl = app.getBaseUploadedFilesUrl();
		this.baseLeoObserverUrl = app.getBaseLeoObserverUrl();
		this.webTemplateCache = app.isWebTemplateCache();
		this.dataApiCache = app.isDataApiCache();

		this.pageHeaderLogo = SystemMetaData.ADMIN_LOGO_URL;
		
		this.showDefaultLogin = SystemMetaData.SHOW_DEFAULT_LOGIN;
		this.ssoLogin = SystemMetaData.SSO_LOGIN;
		this.ssoSessionUrl = "https://" + dnsDomainLeoAdmin + "/_ssocdp/session";
		this.ssoLogoutUrl = "https://" + dnsDomainLeoAdmin + "/_ssocdp/logout";
	}

	/**
	 * @param model
	 * @return
	 */
	public static String renderHtml(WebData model) {
		StringBuilder tplPath = new StringBuilder();
		if (StringUtil.isEmpty(model.getTemplateName())) {
			tplPath.append(model.getTemplateFolder()).append("/index");
		} else {
			tplPath.append(model.getTemplateFolder()).append("/").append(model.getTemplateName());
		}
		String html = TemplateUtil.process(tplPath.toString(), model);
		if (html.equals(TemplateUtil._404)) {
			model.setHttpStatusCode(HttpStatus.SC_NOT_FOUND);
		}
		return html;
	}
}