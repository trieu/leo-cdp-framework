package leotech.cdp.model.asset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.annotations.Expose;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

public abstract class TaxonomyNode extends PersistentObject {

	@Key
	@Expose
	protected String id;

	@Expose
	protected String name;

	@Expose
	protected String description;

	@Expose
	protected String parentId;

	@Expose
	protected String headlineImageUrl = "";

	@Expose
	protected long creationTime;

	@Expose
	protected long modificationTime;
	
	@Expose
	protected Date createdAt;

	@Expose
	protected Date updatedAt;

	@Expose
	protected int navigationOrder = 0;// for the order of display in menu

	@Expose
	protected String slug = ""; // for SEO friendly

	@Expose
	protected long networkId = 0; // the content network ID

	protected int privacyStatus = 0;// 0: public, 1: protected or -1: private

	protected List<String> viewerIds = new ArrayList<>();

	Map<String, String> customData = new HashMap<>();

	protected TaxonomyNode() {
	}

	public TaxonomyNode(String name, long networkId) {
		super();
		this.description = "";
		this.parentId = "";
		initRequiredData(name, networkId);
	}

	public void initRequiredData(String name, long networkId) {
		this.name = name;
		this.networkId = networkId;
		this.slug = networkId + "-" + new Slugify().slugify(name);
		
		this.createdAt = new Date();
		this.updatedAt = new Date();
		this.creationTime = this.createdAt.getTime();
		this.modificationTime = this.updatedAt.getTime();
		
		buildHashedId();
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		this.id = createId(this.id, this.slug);
		return this.id; 
	}

	protected static ArangoCollection getCollection(ArangoCollection collection, String colName)
			throws ArangoDBException {
		if (collection == null) {
			ArangoDatabase arangoDatabase = AbstractCdpDatabaseUtil.getCdpDatabase();

			collection = arangoDatabase.collection(colName);

			// ensure indexing key fields
			collection.ensurePersistentIndex(Arrays.asList("name"), new PersistentIndexOptions().unique(true));
			collection.ensurePersistentIndex(Arrays.asList("slug"), new PersistentIndexOptions().unique(true));
		}
		return collection;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(name) && networkId > 0 && StringUtil.isNotEmpty(this.slug);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public Date getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}

	public void setNetworkId(long networkId) {
		this.networkId = networkId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getHeadlineImageUrl() {
		return headlineImageUrl;
	}

	public void setHeadlineImageUrl(String headlineImageUrl) {
		this.headlineImageUrl = headlineImageUrl;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public long getNetworkId() {
		return networkId;
	}

	public void setNetworkId(int networkId) {
		this.networkId = networkId;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public long getModificationTime() {
		return modificationTime;
	}

	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
		this.updatedAt = new Date(modificationTime);
	}

	public int getPrivacyStatus() {
		return privacyStatus;
	}

	public void setPrivacyStatus(int privacyStatus) {
		this.privacyStatus = privacyStatus;
	}

	public List<String> getViewerIds() {
		return viewerIds;
	}

	public void setViewerIds(List<String> viewerIds) {
		this.viewerIds = viewerIds;
	}

	public Map<String, String> getCustomData() {
		return customData;
	}

	public void setCustomData(Map<String, String> customData) {
		this.customData = customData;
	}

	public int getNavigationOrder() {
		return navigationOrder;
	}

	public void setNavigationOrder(int navigationOrder) {
		this.navigationOrder = navigationOrder;
	}

}
