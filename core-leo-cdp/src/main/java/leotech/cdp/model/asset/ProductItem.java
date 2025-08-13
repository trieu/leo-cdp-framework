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
import leotech.system.util.UrlUtil;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 *
 */
public final class ProductItem extends MeasurableItem {

	public static final String USD = "USD";
	public static final String ITEM_ID = "item_ID";
	public static final String GENERAL_PRODUCT = "general_product";
	
	public static final String COLLECTION_NAME = getCdpCollectionName(ProductItem.class);
	static ArangoCollection collection;

	@Expose
	protected String productId;
	
	@Expose
	protected String productCode = "";
	
	@Expose
	protected String categoryName = "";
	
	@Expose
	protected String categoryId = "";
	
	@Expose
	protected String productIdType = ITEM_ID;
	
	@Expose
	protected String productType = GENERAL_PRODUCT;
	
	@Expose
	protected Set<String> inCampaigns = new HashSet<>(50);
	
	@Expose
	protected Set<String> inJourneyMaps = new HashSet<>(50);

	@Expose
	protected double originalPrice = 0;

	@Expose
	protected double salePrice = 0;
	
	@Expose
	protected int quantity = 0;

	@Expose
	protected String priceCurrency = "USD";

	@Expose
	protected String siteName;

	@Expose
	protected String siteDomain;

	@Expose
	protected String itemCondition;

	@Expose
	protected String availability;

	@Expose
	protected String brand = "";

	@Expose
	protected String sellerName;
	
	@Expose
	protected Set<String> storeIds = new HashSet<String>();
	
	@Expose
	protected Set<String> salesAgentIds = new HashSet<String>();;// a profile that has type = TYPE_INTERNAL_USER = 2;
	
	@Expose
	protected Set<String> warehouseIds = new HashSet<String>();;
	
	@Expose
	protected Set<String> touchpointIds = new HashSet<String>();;
	
	@Expose
	protected Map<String, Voucher> promoCodes = new HashMap<>(100);// codeID -> Voucher Code


	public static ArangoCollection getCollection() {
		if (collection == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			collection = arangoDatabase.collection(COLLECTION_NAME);

			// index data for this class
			// productID can be empty value, but fullUrl must be unique URL in this collection
			collection.ensurePersistentIndex(Arrays.asList("productId"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("productCode"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("productId","productIdType"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("fullUrl"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("brand"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("sellerName"), new PersistentIndexOptions().unique(false));
			
			collection.ensurePersistentIndex(Arrays.asList("touchpointIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("warehouseIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("salesAgentIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("storeIds[*]"), new PersistentIndexOptions().unique(false));

			// ensure indexing key fields
			collection.ensurePersistentIndex(Arrays.asList("slug"), new PersistentIndexOptions().unique(true));
			collection.ensurePersistentIndex(Arrays.asList("ownerId"), new PersistentIndexOptions().unique(false));
			collection.ensureFulltextIndex(Arrays.asList("title"), new FulltextIndexOptions().minLength(5));
			collection.ensurePersistentIndex(Arrays.asList("createdAt"), new PersistentIndexOptions().unique(false));

			collection.ensurePersistentIndex(Arrays.asList("networkId", "contentClass"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("contentClass", "groupIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("contentClass", "categoryIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("networkId", "topicIds[*]"), new PersistentIndexOptions().unique(false));

			// array fields
			collection.ensurePersistentIndex(Arrays.asList("groupIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("topicIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("categoryIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("keywords[*]"), new PersistentIndexOptions().unique(false));

			// for marketing marketing with targeting
			collection.ensurePersistentIndex(Arrays.asList("targetGeoLocations[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("targetSegmentIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("targetViewerIds[*]"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("inCampaigns[*]"), new PersistentIndexOptions().unique(false));
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
		
		// product ext data
		paramJson.iterator();
		try {
			this.priceCurrency = paramJson.getString("priceCurrency","USD");
			double p1 = paramJson.getDouble("originalPrice", 0.0);
			if(p1>=0) {
				this.originalPrice = p1;
			}
			else {
				throw new IllegalArgumentException("originalPrice can not be a negative number");
			}
			
			double p2 =  paramJson.getDouble("salePrice", 0.0);
			if(p2>=0) {
				this.salePrice = p2;
			}
			else {
				throw new IllegalArgumentException("salePrice can not be a negative number");
			}
			
		} catch (Exception e) {} 
		
		
		this.sellerName = paramJson.getString("sellerName","");
		this.brand = paramJson.getString("brand","");
		this.itemCondition = paramJson.getString("itemCondition","");
		this.availability = paramJson.getString("availability","");
		
		this.productType = paramJson.getString("productType",GENERAL_PRODUCT);
		this.productIdType = paramJson.getString("productIdType",ITEM_ID);
		this.productId = paramJson.getString("productId","");
		if(StringUtil.isEmpty(productId)) {
			throw new IllegalArgumentException("productId can not be empty");
		}
		
		this.fullUrl = paramJson.getString("fullUrl","");
		this.siteDomain = UrlUtil.getHostName(this.fullUrl);
		this.siteName = "";
	}


	@Override
	public String buildHashedId() {
		this.id = null;
		if ( (StringUtil.isNotEmpty(productId) || StringUtil.isNotEmpty(productCode)) && StringUtil.isNotEmpty(title)) {
			String keyHint = StringUtil.join("-", categoryId, productCode, productId, productIdType);
			this.id = createId(this.id, keyHint);
		} 
		else if(StringUtil.isNotEmpty(fullUrl) && StringUtil.isNotEmpty(title)) {
			String keyHint = StringUtil.join("-", this.categoryId,this.productCode, productId, productIdType, fullUrl);
			this.id = createId(this.id, keyHint);
		}
		else {
			System.err.println("title " + title);
			System.err.println("fullUrl " + fullUrl);
			System.err.println("productId " + productId);
			System.err.println("productCode " + productCode);
			newIllegalArgumentException("Product title, productId, productCode and fullUrl are required!");
		}
		
		this.slug = new Slugify().slugify(title + "-" + this.id);
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		this.creationTime = this.createdAt.getTime();
		this.modificationTime = this.creationTime; 
		
		return this.id;
	}
	
	public ProductItem() {
		this.fullUrl = "";
		this.type = ContentType.HTML_TEXT;
		this.assetType = AssetType.PRODUCT_ITEM_CATALOG;
		this.contentClass = "product";
	}
	
	public ProductItem(String fullUrl) {
		this.fullUrl = "";
		this.id =  createId(this.id, fullUrl);
	}

	public ProductItem(String fullUrl, String title, String siteDomain, String productId) {
		this.fullUrl = fullUrl;
		this.title = title;
		this.siteDomain = siteDomain;
		this.productId = productId;

		buildHashedId();
	}
	
	public ProductItem(OrderedItem item, String groupId) {
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
		this.assetType = AssetType.PRODUCT_ITEM_CATALOG;
		this.contentClass = "product";
		this.groupIds.add(groupId);
		
		buildHashedId();
	}

	public ProductItem(String groupId, String keywords, String storeId, String productId, String productIdType, String title, String description, String headlineImageUrl, double originalPrice,
			double salePrice, String priceCurrency, String fullUrl, String siteName, String siteDomain) {
		super();
		
		this.groupIds.add(groupId);
		this.type = ContentType.META_DATA;
		this.assetType = AssetType.PRODUCT_ITEM_CATALOG;
		this.contentClass = "product";
		
		// parsing keyword string into the list
		String[] kws = keywords.split(",");
		for (String kw : kws) {
			String keyword = kw.trim();
			if(StringUtil.isNotEmpty(keyword)) {
				this.keywords.add(keyword);
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
		buildHashedId();
		this.slug = new Slugify().slugify(title) + "-" + this.id;
	}
	
	/**
	 * @param groupId
	 * @param dataCsv
	 * @return ProductItem
	 */
	public static ProductItem initFromCsvData(String groupId, String[] dataCsv) {
		if(dataCsv.length > 10) {
			String productType = dataCsv[0];
			String keywords = StringUtil.safeString(dataCsv[1]);
			String storeId = StringUtil.safeString(dataCsv[2]);
			String productIdType = dataCsv[3];
			String productId = dataCsv[4];
			String title = dataCsv[5];
			String description = dataCsv[6];
			String image = dataCsv[7];
			double originalPrice = StringUtil.safeParseDouble(dataCsv[8]) ;
			double salePrice = StringUtil.safeParseDouble(dataCsv[9]) ;
			String priceCurrency = dataCsv[10];
			String fullUrl =  dataCsv[11];
			String siteDomain = UrlUtil.getHostName(fullUrl);
			String siteName = "";
			ProductItem item = new ProductItem(groupId, keywords, storeId, productId, productIdType, title, description, image, originalPrice, salePrice, priceCurrency, fullUrl, siteName, siteDomain);
			item.setProductType(productType);
			if(item.getSalePrice() > 0) {
				AssetProductItemDaoUtil.save(item);
			}
			return item;
		}
		return null;
	}

	@Override
	public boolean dataValidation() {
		boolean ok = this.groupIds.size() > 0 && StringUtil.isNotEmpty(this.title) 
				&& StringUtil.isNotEmpty(this.fullUrl) && StringUtil.isNotEmpty(this.id) 
				&& StringUtil.isNotEmpty(this.siteDomain);
		if (ok) {
			return true;
		}
		System.err.println(toString());
		throw new IllegalArgumentException("The data is not ready, title is empty ");
	}
	
	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

	public String getProductId() {
		if(productId == null) {
			productId = "";
		}
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductIdType() {
		if(productIdType == null) {
			productIdType = ITEM_ID;
		}
		return productIdType;
	}

	public void setProductIdType(String productIdType) {
		this.productIdType = productIdType;
	}
	

	public String getProductType() {
		if(productType == null) {
			productType = GENERAL_PRODUCT;
		}
		return productType;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}



	public double getOriginalPrice() {
		return originalPrice;
	}

	public void setOriginalPrice(double originalPrice) {
		if (originalPrice > 0) {
			this.originalPrice = originalPrice;
		}
	}

	public void setOriginalPrice(String originalPrice) {
		this.originalPrice = StringUtil.safeParseDouble(originalPrice);
	}

	public double getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(double salePrice) {
		this.salePrice = salePrice;

		if (this.originalPrice == 0) {
			this.originalPrice = this.salePrice;
		}
	}

	public void setSalePrice(String salePrice) {
		this.salePrice = StringUtil.safeParseDouble(salePrice);

		// because originalPrice can not be zero if sale price is larger than zero
		if (this.originalPrice == 0) {
			this.originalPrice = this.salePrice;
		}
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
		if (promoCodes == null) {
			promoCodes = new HashMap<>();
		}
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
