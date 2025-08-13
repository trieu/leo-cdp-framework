package leotech.cdp.model.asset;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.arangodb.ArangoCollection;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonObject;

/**
 * 
 * digital asset group is the container that can have sorted items {post, product, service, template, creative}
 * 
 * @author tantrieuf31
 * @since 2018
 *
 */
public class AssetGroup extends AssetItem {
	
	public static final String COLLECTION_NAME = getCdpCollectionName(AssetGroup.class);
	static ArangoCollection collectionInstance;

	@Expose
	@com.arangodb.velocypack.annotations.Expose(serialize = false, deserialize = false)
	SortedSet<MeasurableItem> itemsOfGroup;

	// if true , then new Bot Subscriber is created for this page
	@Expose
	boolean isManagedByLeoBot = false;
	
	@Expose
	boolean defaultGroup = false;
	
	@Expose
	boolean mergeDataIntoTemplates = false;
								
	@Expose
	protected Map<String, List<String>> mapContentClassKeywords = new HashMap<>(100);
	
	@Expose
	protected Set<String> eventNamesForSegmentation = new HashSet<String>(20);

	public AssetGroup() {
		// for JSON decode from ArangoDB
	}
	
	public AssetGroup(String title, String categoryId, int assetType, String ownerId, String mediaInfo) {
		this.initNewGroup(categoryId, title, mediaInfo, type, ownerId);
		this.assetType = assetType;
		this.mediaInfo = mediaInfo;
		this.buildHashedId();
	}
	
	public AssetGroup(String title, String categoryId, int assetType, String ownerId) {
		this.initNewGroup(categoryId, title, mediaInfo, type, ownerId);
		this.assetType = assetType;
		this.mediaInfo = "";
		this.buildHashedId();
	}
	

	public boolean isDefaultGroup() {
		return defaultGroup;
	}

	public void setDefaultGroup(boolean defaultGroup) {
		this.defaultGroup = defaultGroup;
	}

	public boolean isMergeDataIntoTemplates() {
		return mergeDataIntoTemplates;
	}

	public void setMergeDataIntoTemplates(boolean mergeDataIntoTemplates) {
		this.mergeDataIntoTemplates = mergeDataIntoTemplates;
	}

	public void setContentItemsWithOrderByTime(List<AssetContent> posts) {
		this.itemsOfGroup = new TreeSet<>();
		itemsOfGroup.addAll(posts);
	}

	@Override
	public ArangoCollection getDbCollection() {
		ArangoCollection collection = getCollection(collectionInstance, COLLECTION_NAME);
		collection.ensurePersistentIndex(Arrays.asList("defaultGroup", "assetType"), new PersistentIndexOptions().unique(false));
		return collection;
	}

	
	public SortedSet<MeasurableItem> getItemsOfGroup() {
		if (itemsOfGroup == null) {
			itemsOfGroup = new TreeSet<>();
		}
		return itemsOfGroup;
	}

	public void setItemsOfGroup(SortedSet<MeasurableItem> itemsOfGroup) {
		this.itemsOfGroup = itemsOfGroup;
	}

	public boolean isManagedByLeoBot() {
		return isManagedByLeoBot;
	}
	
	public void setManagedByLeoBot(boolean isManagedByLeoBot) {
		this.isManagedByLeoBot = isManagedByLeoBot;
	}

	public Map<String, List<String>> getMapContentClassKeywords() {
		return mapContentClassKeywords;
	}

	public void clearMapContentClassKeywords() {
		this.mapContentClassKeywords.clear();
	}

	public void setMapContentClassKeywords(Map<String, List<String>> mapContentClassKeywords) {
		this.mapContentClassKeywords = mapContentClassKeywords;
	}

	public void setContentClassKeywords(String contentClass, List<String> keywords) {
		this.mapContentClassKeywords.put(contentClass, keywords);
	}
	
	public void clearEventNamesForSegmentation() {
		if(eventNamesForSegmentation != null) {
			eventNamesForSegmentation.clear();
		}
	}
	
	public Set<String> getEventNamesForSegmentation() {
		return eventNamesForSegmentation;
	}

	public void setEventNamesForSegmentation(Set<String> eventNamesForSegmentation) {
		this.eventNamesForSegmentation = eventNamesForSegmentation;
	}
	
	public void setEventNamesForSegmentation(String eventName) {
		this.eventNamesForSegmentation.add(eventName);
	}

	@Override
	public void initItemDataFromJson(JsonObject paramJson) {
		// skip
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		buildItemId(this);
		return this.id;
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
}
