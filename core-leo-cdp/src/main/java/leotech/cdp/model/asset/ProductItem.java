package leotech.cdp.model.asset;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.FulltextIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.model.analytics.OrderedItem;
import leotech.system.util.LogUtil;
import leotech.system.util.UrlUtil;
import rfx.core.util.StringUtil;

/**
 * Product Item for product catalog. <br>
 * Used for storing product data for recommendations, syncs, scraping, and
 * campaigns. <br>
 * Contains fields like product ID, category name, type, price, domain,
 * condition, availability, etc. <br>
 * 
 * ArangoDB collection: cdp_productitem
 * 
 * @author tantrieuf31
 * @since 2021
 */
public final class ProductItem extends MeasurableItem {

	public static final String USD = "USD";
	public static final String ITEM_ID = "item_ID";
	public static final String GENERAL_PRODUCT = "general_product";

	public static final String COLLECTION_NAME = getCdpCollectionName(ProductItem.class);

	// Volatile for thread-safe lazy initialization
	private static volatile ArangoCollection collection;

	// Reused constant Slugify instance to prevent regex memory thrashing on
	// mass catalog imports
	private static final Slugify SLUGIFY = new Slugify();

	@Expose
	private String productId;

	@Expose
	private String productCode = "";

	@Expose
	private String categoryName = "";

	@Expose
	private String categoryId = "";

	@Expose
	private String productIdType = ITEM_ID;

	@Expose
	private String productType = GENERAL_PRODUCT;

	@Expose
	private Set<String> inCampaigns = new HashSet<>(50);

	@Expose
	private Set<String> inJourneyMaps = new HashSet<>(50);

	@Expose
	private double originalPrice = 0;

	@Expose
	private double salePrice = 0;

	@Expose
	private int quantity = 0;

	@Expose
	private String priceCurrency = "USD";

	@Expose
	private String siteName;

	@Expose
	private String siteDomain;

	@Expose
	private String itemCondition;

	@Expose
	private String availability;

	@Expose
	private String brand = "";

	@Expose
	private String sellerName;

	@Expose
	private Set<String> storeIds = new HashSet<>();

	@Expose
	private Set<String> salesAgentIds = new HashSet<>(); // Internal User ID (Profile Type = 2)

	@Expose
	private Set<String> warehouseIds = new HashSet<>();

	@Expose
	private Set<String> touchpointIds = new HashSet<>();

	@Expose
	private Map<String, Voucher> promoCodes = new HashMap<>(100); // codeID -> Voucher Code

	public ProductItem() {
		this.fullUrl = "";
		this.type = ContentType.HTML_TEXT;
		this.assetType = AssetType.PRODUCT_ITEM_CATALOG;
		this.contentClass = "product";
	}

	public ProductItem(String fullUrl) {
		this(); // Calls default constructor to maintain base state setup
		this.fullUrl = fullUrl;
		this.id = createId(this.id, fullUrl);
	}

	public ProductItem(String fullUrl, String title, String siteDomain, String productId) {
		this();
		this.fullUrl = fullUrl;
		this.title = title;
		this.siteDomain = siteDomain;
		this.productId = productId;
		this.buildHashedId();
	}

	public ProductItem(OrderedItem item, String groupId) {
		this();
		this.productId = item.getItemId();
		this.productCode = item.getProductCode();
		this.categoryName = item.getCategoryName();
		this.categoryId = item.getCategoryId();

		this.fullUrl = item.getFullUrl();
		this.title = item.getName();
		this.description = item.getDescription();

		this.siteDomain = item.getSource();
		this.productIdType = item.getIdType();
		this.salePrice = item.getSalePrice();
		this.originalPrice = item.getOriginalPrice();
		this.priceCurrency = item.getCurrency();
		this.setHeadlineImageUrl(item.getImageUrl());
		this.setHeadlineVideoUrl(item.getVideoUrl());

		this.type = ContentType.META_DATA;
		this.groupIds.add(groupId);

		this.buildHashedId();
	}

	public ProductItem(String groupId, String keywords, String storeId, String productId, String productIdType,
			String title, String description, String headlineImageUrl, double originalPrice, double salePrice,
			String priceCurrency, String fullUrl, String siteName, String siteDomain) {
		this();
		this.groupIds.add(groupId);
		this.type = ContentType.META_DATA;

		if (StringUtil.isNotEmpty(keywords)) {
			// Optimized parsing utilizing stream/map concepts implicitly
			String[] kws = keywords.split(",");
			for (String kw : kws) {
				String keyword = kw.trim();
				if (!keyword.isEmpty()) {
					this.keywords.add(keyword);
				}
			}
		}

		this.storeIds.add(storeId);
		this.productId = productId;
		this.productIdType = productIdType;
		this.title = title;
		this.description = description;
		this.headlineImageUrl = headlineImageUrl;
		this.originalPrice = originalPrice;
		this.salePrice = salePrice;
		this.priceCurrency = priceCurrency;
		this.fullUrl = fullUrl;
		this.siteName = siteName;
		this.siteDomain = siteDomain;

		this.buildHashedId();
	}

	public static ArangoCollection getCollection() {
		if (collection == null) {
			// Thread-safe double-checked locking
			synchronized (ProductItem.class) {
				if (collection == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);
					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);

					// Core System Indices
					col.ensurePersistentIndex(Arrays.asList("slug"), new PersistentIndexOptions().unique(true));
					col.ensurePersistentIndex(Arrays.asList("productId", "productIdType"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("productCode", "brand"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("fullUrl"), pIdxOpts);

					// Core Filtering/Sorting Indices (Used by Admin Catalog UI)
					col.ensurePersistentIndex(Arrays.asList("sellerName", "createdAt"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("networkId", "contentClass"), pIdxOpts);

					// Array Mapping Indices (RocksDB handles array unpacking natively here)
					col.ensurePersistentIndex(Arrays.asList("groupIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("topicIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("categoryIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("keywords[*]"), pIdxOpts);

					// Relation & Location Matrix Arrays
					col.ensurePersistentIndex(Arrays.asList("touchpointIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("warehouseIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("storeIds[*]"), pIdxOpts);

					// Marketing & Targeting Arrays
					col.ensurePersistentIndex(Arrays.asList("targetGeoLocations[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("targetSegmentIds[*]"), pIdxOpts);
					col.ensurePersistentIndex(Arrays.asList("inCampaigns[*]"), pIdxOpts);

					// Full-text Engine for UI searches
					col.ensureFulltextIndex(Arrays.asList("title"), new FulltextIndexOptions().minLength(5));

					collection = col;
				}
			}
		}
		return collection;
	}

	@Override
	public ArangoCollection getDbCollection() {
		return getCollection();
	}

	@Override
	public void initItemDataFromJson(JsonObject paramJson) {
		setItemDataFromJson(this, paramJson);

		try {
			this.priceCurrency = paramJson.getString("priceCurrency", USD);

			double p1 = paramJson.getDouble("originalPrice", 0.0);
			if (p1 >= 0) {
				this.originalPrice = p1;
			} else {
				throw new IllegalArgumentException("originalPrice cannot be a negative number");
			}

			double p2 = paramJson.getDouble("salePrice", 0.0);
			if (p2 >= 0) {
				this.salePrice = p2;
			} else {
				throw new IllegalArgumentException("salePrice cannot be a negative number");
			}
		} catch (Exception e) {
			LogUtil.logError(ProductItem.class, "Failed to parse prices from JSON payload: " + e.getMessage());
		}

		this.sellerName = paramJson.getString("sellerName", "");
		this.brand = paramJson.getString("brand", "");
		this.itemCondition = paramJson.getString("itemCondition", "");
		this.availability = paramJson.getString("availability", "");

		this.productType = paramJson.getString("productType", GENERAL_PRODUCT);
		this.productIdType = paramJson.getString("productIdType", ITEM_ID);
		this.productId = paramJson.getString("productId", "");

		if (StringUtil.isEmpty(productId)) {
			throw new IllegalArgumentException("productId cannot be empty");
		}

		this.fullUrl = paramJson.getString("fullUrl", "");
		this.siteDomain = UrlUtil.getHostName(this.fullUrl);
		this.siteName = "";
	}

	@Override
	public String buildHashedId() {
		this.id = null;
		if ((StringUtil.isNotEmpty(productId) || StringUtil.isNotEmpty(productCode)) && StringUtil.isNotEmpty(title)) {
			String keyHint = StringUtil.join("-", categoryId, productCode, productId, productIdType);
			this.id = createId(this.id, keyHint);
		} else if (StringUtil.isNotEmpty(fullUrl) && StringUtil.isNotEmpty(title)) {
			String keyHint = StringUtil.join("-", categoryId, productCode, productId, productIdType, fullUrl);
			this.id = createId(this.id, keyHint);
		} else {
			// Replace System.err logging with true exception throwing
			throw new IllegalArgumentException(String.format(
					"Data incomplete. Title: '%s', FullUrl: '%s', ProductId: '%s', ProductCode: '%s'. Product title, productId/productCode or fullUrl are required!",
					title, fullUrl, productId, productCode));
		}

		Date now = new Date();
		this.createdAt = now;
		this.updatedAt = now;
		this.creationTime = now.getTime();
		this.modificationTime = this.creationTime;

		// Use cached Slugify to avoid severe memory leaks
		this.slug = SLUGIFY.slugify(title + "-" + this.id);
		return this.id;
	}

	public static ProductItem initFromCsvData(String groupId, String[] dataCsv) {
		if (dataCsv != null && dataCsv.length > 10) {
			String productType = dataCsv[0];
			String keywords = StringUtil.safeString(dataCsv[1]);
			String storeId = StringUtil.safeString(dataCsv[2]);
			String productIdType = dataCsv[3];
			String productId = dataCsv[4];
			String title = dataCsv[5];
			String description = dataCsv[6];
			String image = dataCsv[7];
			double originalPrice = StringUtil.safeParseDouble(dataCsv[8]);
			double salePrice = StringUtil.safeParseDouble(dataCsv[9]);
			String priceCurrency = dataCsv[10];
			String fullUrl = dataCsv[11];
			String siteDomain = UrlUtil.getHostName(fullUrl);

			ProductItem item = new ProductItem(groupId, keywords, storeId, productId, productIdType, title, description,
					image, originalPrice, salePrice, priceCurrency, fullUrl, "", siteDomain);
			item.setProductType(productType);

			if (item.getSalePrice() > 0) {
				AssetProductItemDaoUtil.save(item);
			}
			return item;
		}
		return null;
	}

	@Override
	public boolean dataValidation() {
		if (!this.groupIds.isEmpty() && StringUtil.isNotEmpty(this.title) && StringUtil.isNotEmpty(this.fullUrl)
				&& StringUtil.isNotEmpty(this.id) && StringUtil.isNotEmpty(this.siteDomain)) {
			return true;
		}
		throw new IllegalArgumentException("The data is not ready, mandatory fields are empty: " + this.toString());
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	public String getProductId() {
		return (productId == null) ? "" : productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductIdType() {
		return (productIdType == null) ? ITEM_ID : productIdType;
	}

	public void setProductIdType(String productIdType) {
		this.productIdType = productIdType;
	}

	public String getProductType() {
		return (productType == null) ? GENERAL_PRODUCT : productType;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}

	public double getOriginalPrice() {
		return originalPrice;
	}

	public void setOriginalPrice(double originalPrice) {
		if (originalPrice > 0)
			this.originalPrice = originalPrice;
	}

	public void setOriginalPrice(String originalPrice) {
		this.originalPrice = StringUtil.safeParseDouble(originalPrice);
	}

	public double getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(double salePrice) {
		this.salePrice = salePrice;
		if (this.originalPrice == 0)
			this.originalPrice = this.salePrice;
	}

	public void setSalePrice(String salePrice) {
		this.salePrice = StringUtil.safeParseDouble(salePrice);
		if (this.originalPrice == 0)
			this.originalPrice = this.salePrice;
	}

	public String getPriceCurrency() {
		return priceCurrency;
	}

	public void setPriceCurrency(String priceCurrency) {
		this.priceCurrency = priceCurrency;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public String getSiteDomain() {
		return siteDomain;
	}

	public void setSiteDomain(String siteDomain) {
		this.siteDomain = siteDomain;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public Map<String, Voucher> getPromoCodes() {
		if (promoCodes == null)
			promoCodes = new HashMap<>();
		return promoCodes;
	}

	public void setPromoCodes(Map<String, Voucher> promoCodes) {
		this.promoCodes = promoCodes;
	}

	public void setPromoCodes(List<Voucher> promoCodeList) {
		this.promoCodes = new HashMap<>(promoCodeList.size());
		for (Voucher code : promoCodeList) {
			this.promoCodes.put(code.getId(), code);
		}
	}

	public String getItemCondition() {
		return itemCondition;
	}

	public void setItemCondition(String itemCondition) {
		this.itemCondition = itemCondition;
	}

	public String getAvailability() {
		return availability;
	}

	public void setAvailability(String availability) {
		this.availability = availability;
	}

	public boolean isEmpty() {
		return StringUtil.isEmpty(this.id);
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
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

	@Override
	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public Set<String> getInJourneyMaps() {
		return inJourneyMaps;
	}

	public void setInJourneyMaps(Set<String> inJourneyMaps) {
		this.inJourneyMaps = inJourneyMaps;
	}

	public String getSellerName() {
		return sellerName;
	}

	public void setSellerName(String sellerName) {
		this.sellerName = sellerName;
	}

	public Set<String> getInCampaigns() {
		return inCampaigns;
	}

	public void setInCampaigns(Set<String> inCampaigns) {
		this.inCampaigns = inCampaigns;
	}

	public void addInCampaigns(String campaignId) {
		this.inCampaigns.add(campaignId);
	}

	public void removeInCampaigns(String campaignId) {
		this.inCampaigns.remove(campaignId);
	}

	public void updateData(OrderedItem orderedItem) {
		this.setTitle(orderedItem.getName());
		this.setFullUrl(orderedItem.getFullUrl());
		this.setHeadlineImageUrl(orderedItem.getImageUrl());
		this.setHeadlineVideoUrl(orderedItem.getVideoUrl());
		this.setOriginalPrice(orderedItem.getOriginalPrice());
		this.setSalePrice(orderedItem.getSalePrice());
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public Set<String> getStoreIds() {
		return this.storeIds;
	}

	public void setStoreIds(String storeId) {
		this.storeIds.add(storeId);
	}

	public void setStoreIds(Set<String> storeIds) {
		this.storeIds.addAll(storeIds);
	}

	public Set<String> getSalesAgentIds() {
		return salesAgentIds;
	}

	public void setSalesAgentIds(Set<String> salesAgentIds) {
		this.salesAgentIds.addAll(salesAgentIds);
	}

	public Set<String> getTouchpointIds() {
		return touchpointIds;
	}

	public void setTouchpointIds(Set<String> touchpointIds) {
		this.touchpointIds.addAll(touchpointIds);
	}

	public Set<String> getWarehouseIds() {
		return warehouseIds;
	}

	public void setWarehouseIds(Set<String> warehouseIds) {
		this.warehouseIds.addAll(warehouseIds);
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}