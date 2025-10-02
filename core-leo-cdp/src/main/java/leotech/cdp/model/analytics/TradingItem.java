package leotech.cdp.model.analytics;

import java.util.Date;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonObject;
import leotech.cdp.model.asset.AssetType;
import leotech.cdp.model.customer.ProfileModelUtil;
import leotech.system.exception.InvalidDataException;
import leotech.system.util.IdGenerator;
import leotech.system.util.UrlUtil;
import leotech.system.util.XssFilterUtil;
import rfx.core.util.StringUtil;

/**
 * 
 * Trading Item, the data model to represent financial/trading assets (stocks, bonds, crypto, ETFs, etc.) in LEO CDP.
 * 
 * @author tantrieuf31
 * @since 2025
 *
 */
public class TradingItem implements Comparable<TradingItem> {

	private static final String USD = "USD";
	String sessionKey = "";
	String key = null;

	@Expose
	int assetType = AssetType.TRADING_ITEM_CATALOG;

	@Expose
	String fullUrl = "";

	@Expose
	String source = "";

	@Expose
	String name = "";

	@Expose
	String imageUrl = "";

	@Expose
	String videoUrl = "";

	@Expose
	String description = "";

	@Expose
	String tickerSymbol = ""; // e.g. AAPL, TSLA, BTC-USD

	@Expose
	String isinCode = ""; // International Securities Identifier

	@Expose
	String exchange = ""; // e.g. NASDAQ, NYSE, Binance

	@Expose
	String assetCategory = ""; // Stock, Bond, Crypto, ETF, Commodity

	@Expose
	String sector = ""; // Technology, Healthcare, Finance, etc.

	@Expose
	String currency = USD; // Default USD

	@Expose
	double price = 0.0; // Current or reference price

	@Expose
	double marketCap = 0.0; // Market capitalization

	@Expose
	double volume24h = 0.0; // 24-hour trading volume

	@Expose
	double circulatingSupply = 0.0; // For crypto assets

	@Expose
	String riskLevel = "Medium"; // Custom risk classification

	@Expose
	Date lastUpdated; // Last price refresh

	@Expose
	Date createdAt;

	@Expose
	Date expiredAt = null;

	public TradingItem() {

	}

	/**
	 * for API with JSON payload
	 * 
	 * @param createdAt
	 * @param obj
	 */
	public TradingItem(Date createdAt, JsonObject obj) {
		if (obj == null) {
			throw new InvalidDataException("Date createdAt or JsonObject obj is NULL");
		}

		System.out.println("TradingItem " + obj);

		// timestamps
		this.createdAt = createdAt == null ? new Date() : createdAt;
		this.expiredAt = ProfileModelUtil.parseDate(XssFilterUtil.safeGet(obj, "expiredAt"));
		this.lastUpdated = ProfileModelUtil.parseDate(XssFilterUtil.safeGet(obj, "lastUpdated"));

		// identifiers
		this.tickerSymbol = XssFilterUtil.safeGet(obj, "tickerSymbol");
		this.isinCode = XssFilterUtil.safeGet(obj, "isinCode");
		this.exchange = XssFilterUtil.safeGet(obj, "exchange");
		this.assetCategory = XssFilterUtil.safeGet(obj, "assetCategory");
		this.sector = XssFilterUtil.safeGet(obj, "sector");

		// trading / market data
		this.currency = StringUtil.isNotEmpty(XssFilterUtil.safeGet(obj, "currency"))
				? XssFilterUtil.safeGet(obj, "currency")
				: USD;

		this.price = obj.getDouble("price", 0.0);
		this.marketCap = obj.getDouble("marketCap", 0.0);
		this.volume24h = obj.getDouble("volume24h", 0.0);
		this.circulatingSupply = obj.getDouble("circulatingSupply", 0.0);
		this.riskLevel = XssFilterUtil.safeGet(obj, "riskLevel");

		// metadata / media
		this.source = XssFilterUtil.safeGet(obj, "source");
		this.fullUrl = XssFilterUtil.safeGet(obj, "fullUrl");
		this.name = XssFilterUtil.safeGet(obj, "name");
		this.description = XssFilterUtil.safeGet(obj, "description");
		this.imageUrl = XssFilterUtil.safeGet(obj, "imageUrl");
		this.videoUrl = XssFilterUtil.safeGet(obj, "videoUrl");

		// generate unique key
		buildKey();
	}

	public static TradingItem fromJson(JsonObject obj) {
		if (obj == null) {
			throw new InvalidDataException("JsonObject obj is NULL");
		}

		// try to parse createdAt from JSON, fallback to now
		Date createdAt = ProfileModelUtil.parseDate(XssFilterUtil.safeGet(obj, "createdAt"));
		if (createdAt == null) {
			createdAt = new Date();
		}

		return new TradingItem(createdAt, obj);
	}

	public void buildKey() {
		if (this.createdAt == null) {
			this.createdAt = new Date();
		}
		if (StringUtil.isNotEmpty(name) && StringUtil.isNotEmpty(tickerSymbol)) {
			this.key = IdGenerator.createHashedId(name + tickerSymbol + assetType + fullUrl + currency);
		} else {
			System.err.println("The name and tickerSymbol must not be empty for data: " + new Gson().toJson(this));
		}
	}

	public String getKey() {
		return key;
	}

	public int getAssetType() {
		return assetType;
	}

	public void setAssetType(int assetType) {
		this.assetType = assetType;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getTickerSymbol() {
		return tickerSymbol;
	}

	public void setTickerSymbol(String tickerSymbol) {
		this.tickerSymbol = tickerSymbol;
	}

	public String getIsinCode() {
		return isinCode;
	}

	public void setIsinCode(String isinCode) {
		this.isinCode = isinCode;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getAssetCategory() {
		return assetCategory;
	}

	public void setAssetCategory(String assetCategory) {
		this.assetCategory = assetCategory;
	}

	public String getSector() {
		return sector;
	}

	public void setSector(String sector) {
		this.sector = sector;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public double getMarketCap() {
		return marketCap;
	}

	public void setMarketCap(double marketCap) {
		this.marketCap = marketCap;
	}

	public double getVolume24h() {
		return volume24h;
	}

	public void setVolume24h(double volume24h) {
		this.volume24h = volume24h;
	}

	public double getCirculatingSupply() {
		return circulatingSupply;
	}

	public void setCirculatingSupply(double circulatingSupply) {
		this.circulatingSupply = circulatingSupply;
	}

	public String getRiskLevel() {
		return riskLevel;
	}

	public void setRiskLevel(String riskLevel) {
		this.riskLevel = riskLevel;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public Date getExpiredAt() {
		return expiredAt;
	}

	public void setExpiredAt(Date expiredAt) {
		this.expiredAt = expiredAt;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getVideoUrl() {
		return videoUrl;
	}

	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public String getFullUrl() {
		return fullUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setFullUrl(String fullUrl) {
		this.fullUrl = fullUrl;
	}

	public String getSource() {
		if (StringUtil.isEmpty(this.source)) {
			this.source = UrlUtil.getHostName(this.fullUrl);
		}
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @param assetType
	 * @return
	 */
	public boolean isTransactionalItem() {
		return this.assetType == AssetType.SERVICE_ITEM_CATALOG;
	}

	public String getSessionKey() {
		if (StringUtil.isEmpty(sessionKey)) {
			sessionKey = "";
		}
		return sessionKey;
	}

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public int hashCode() {
		if (key == null) {
			buildKey();
		}
		return Objects.hash(key);
	}

	@Override
	public boolean equals(Object obj) {
		return hashCode() == obj.hashCode();
	}

	@Override
	public int compareTo(TradingItem o) {
		int h1 = this.hashCode();
		int h2 = o.hashCode();
		if (h1 > h2) {
			return 1;
		} else if (h1 < h2) {
			return -1;
		}
		return 0;
	}

	public boolean isValid() {
		return StringUtil.isNotEmpty(this.key);
	}

}
