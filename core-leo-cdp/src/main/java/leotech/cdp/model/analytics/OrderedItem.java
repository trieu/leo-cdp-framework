package leotech.cdp.model.analytics;

import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonObject;
import leotech.cdp.model.asset.AssetType;
import leotech.cdp.model.asset.ProductItem;
import leotech.system.exception.InvalidDataException;
import leotech.system.util.IdGenerator;
import leotech.system.util.UrlUtil;
import leotech.system.util.XssFilterUtil;
import rfx.core.util.StringUtil;

/**
 * 
 * Sales Item (Product or Service or Event Ticket or Subscribed Internet
 * service)
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class OrderedItem implements Comparable<OrderedItem> {

	private static final String DEFAULT_ID_TYPE = "item_ID";
	private static final String USD = "USD";
	String sessionKey = "";
	String key = null;

	@Expose
	int assetType = AssetType.PRODUCT_ITEM_CATALOG;

	@Expose
	String storeId = "";

	@Expose
	String itemId = "";

	@Expose
	String idType = DEFAULT_ID_TYPE;

	@Expose
	String productCode = "";

	@Expose
	String categoryName = "";

	@Expose
	String categoryId = "";

	@Expose
	int quantity = 0;

	@Expose
	double originalPrice = 0;

	@Expose
	double discount = 0;

	@Expose
	double salePrice = 0; // discountedPrice

	@Expose
	int serviceAvailable = 0;
	
	@Expose
	int useService = 0;
	
	@Expose
	int inPackage = 0;

	@Expose
	String couponCode = "";

	@Expose
	String currency = USD;

	@Expose
	String imageUrl = "";

	@Expose
	String videoUrl = "";

	@Expose
	String fullUrl = "";

	@Expose
	String source = "";

	@Expose
	String name = "";

	@Expose
	String description = "";

	@Expose
	Date createdAt;

	public OrderedItem() {

	}

	/**
	 * for API with JSON payload
	 * 
	 * @param createdAt
	 * @param obj
	 */
	public OrderedItem(Date createdAt, JsonObject obj) {
		if (obj == null) {
			throw new InvalidDataException("Date createdAt or JsonObject obj is NULL");
		}
		System.out.println("OrderedItem " + obj);
		this.createdAt = createdAt == null ? new Date() : createdAt;

		// item ID
		this.idType = XssFilterUtil.safeGet(obj, "idtype", DEFAULT_ID_TYPE);
		this.source = XssFilterUtil.safeGet(obj, "source");
		this.itemId = XssFilterUtil.safeGet(obj, "itemid");
		
		this.productCode = XssFilterUtil.safeGet(obj, "productcode");
		this.categoryId = XssFilterUtil.safeGet(obj, "categoryid");
		this.categoryName = XssFilterUtil.safeGet(obj, "categoryname");

		// transaction
		this.salePrice = XssFilterUtil.safeGetDouble(obj, "saleprice", 0);
		this.originalPrice = XssFilterUtil.safeGetDouble(obj, "originalprice", 0);
		
		this.discount = XssFilterUtil.safeGetDouble(obj, "discount", 0);
		this.currency = XssFilterUtil.safeGet(obj, "currency", USD);
		this.quantity = XssFilterUtil.safeGetInteger(obj, "quantity", 1);
		
		this.inPackage = XssFilterUtil.safeGetInteger(obj, "inpackage", 0);
		this.useService = XssFilterUtil.safeGetInteger(obj, "useservice", 0);
		this.serviceAvailable = XssFilterUtil.safeGetInteger(obj, "serviceavailable", 0);

		// metadata
		this.fullUrl = XssFilterUtil.safeGet(obj, "fullurl");
		this.name = XssFilterUtil.safeGet(obj, "name");
		this.description = XssFilterUtil.safeGet(obj, "description");
		this.imageUrl = XssFilterUtil.safeGet(obj, "imageurl");
		this.videoUrl = XssFilterUtil.safeGet(obj, "videourl");
		
		this.couponCode = XssFilterUtil.safeGet(obj, "couponcode");
		

		buildKey();
	}

	public OrderedItem(Date createdAt, ProductItem item, int quantity) {
		this.createdAt = createdAt;
		this.itemId = item.getProductId();
		this.idType = item.getProductIdType();
		this.salePrice = item.getSalePrice();
		this.originalPrice = item.getOriginalPrice();
		this.currency = item.getPriceCurrency();
		this.fullUrl = item.getFullUrl();
		this.name = item.getTitle();
		this.imageUrl = item.getHeadlineImageUrl();
		this.videoUrl = item.getHeadlineVideoUrl();
		this.quantity = quantity;

		buildKey();
	}

	public OrderedItem(Date createdAt, String sessionKey, String name, String itemId, String idType, int quantity) {
		super();
		this.createdAt = createdAt;
		this.sessionKey = sessionKey;
		this.name = name;
		this.itemId = itemId;
		this.idType = idType;
		this.quantity = quantity;

		buildKey();
	}

	public void buildKey() {
		if (this.createdAt == null) {
			this.createdAt = new Date();
		}
		if (StringUtil.isNotEmpty(name) && (StringUtil.isNotEmpty(itemId) || StringUtil.isNotEmpty(productCode)) ) {
			this.key = IdGenerator.createHashedId(name + itemId + idType + productCode + assetType + fullUrl + currency);
		} else {
			System.err.println("The name and itemId must not be empty for data: " + new Gson().toJson(this));
		}
	}

	public String getStoreId() {
		return storeId;
	}

	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}

	public void setStoreId(Set<String> storeIds) {
		Iterator<String> iterator = storeIds.iterator();
		if (iterator.hasNext()) {
			this.storeId = iterator.next();
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

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getIdType() {
		return idType;
	}

	public void setIdType(String idType) {
		this.idType = idType;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public double getOriginalPrice() {
		return originalPrice;
	}

	public void setOriginalPrice(double originalPrice) {
		this.originalPrice = originalPrice;
	}

	public double getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(double salePrice) {
		this.salePrice = salePrice;
	}

	public String getCouponCode() {
		return couponCode;
	}

	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}

	public String getCurrency() {
		if (StringUtil.isEmpty(currency)) {
			currency = USD;
		}
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public double getTransactionValue() {
		double t = this.salePrice * this.quantity;
		return t > this.discount ? t - this.discount : 0;
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

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}
	
	

	public int getServiceAvailable() {
		return serviceAvailable;
	}

	public void setServiceAvailable(int serviceAvailable) {
		this.serviceAvailable = serviceAvailable;
	}

	public int getUseService() {
		return useService;
	}

	public void setUseService(int useService) {
		this.useService = useService;
	}

	public int getInPackage() {
		return inPackage;
	}

	public void setInPackage(int inPackage) {
		this.inPackage = inPackage;
	}

	public double getDiscount() {
		return discount;
	}

	public void setDiscount(double discount) {
		this.discount = discount;
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
		return (this.assetType == AssetType.PRODUCT_ITEM_CATALOG || this.assetType == AssetType.SERVICE_ITEM_CATALOG)
				&& StringUtil.isNotEmpty(this.itemId) && StringUtil.isNotEmpty(this.idType);
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
	public int compareTo(OrderedItem o) {
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
