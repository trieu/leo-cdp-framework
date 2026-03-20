package leotech.cdp.model.asset;

import com.arangodb.ArangoCollection;
import com.google.gson.annotations.Expose;

import leotech.system.model.AppMetadata;

/**
 * Digital asset category, used to categorize digital assets in the system. 
 * Each category is associated with an asset type, such as content item, campaign, etc.
 * 
 * ArangoDB collection: cdp_assetcategory
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class AssetCategory extends TaxonomyNode {
	
	public static final String COLLECTION_NAME = getCdpCollectionName(AssetCategory.class);
	static ArangoCollection collectionInstance;

	@Override
	public ArangoCollection getDbCollection() {
		return getCollection(collectionInstance, COLLECTION_NAME);
	}

	@Expose
	int assetType = AssetType.CONTENT_ITEM_CATALOG;

	public AssetCategory() {
	}

	public AssetCategory(String name, int assetType) {
		super(name, AppMetadata.DEFAULT_ID);
		this.assetType = assetType;
	}
	
	public AssetCategory(String name, int assetType, String description, int order) {
		super(name, AppMetadata.DEFAULT_ID);
		this.assetType = assetType;
		this.description = description;
		this.navigationOrder = order;
	}

	public AssetCategory(String name, long networkId, int assetType) {
		super(name, networkId);
		this.assetType = assetType;
	}

	public int getAssetType() {
		return assetType;
	}

	public void setAssetType(int assetType) {
		this.assetType = assetType;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + this.id;
	}

}
