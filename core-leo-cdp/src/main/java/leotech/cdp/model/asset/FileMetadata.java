package leotech.cdp.model.asset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.FulltextIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.annotations.Expose;

import leotech.system.model.AppMetadata;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * @author TrieuNT
 *
 */
public class FileMetadata extends PersistentObject {

	@Key
	@Expose
	String id;

	@Expose
	String path;

	@Expose
	String name;

	@Expose
	long uploadedTime;
	
	@Expose
	Date createdAt;

	@Expose
	int revision;

	@Expose
	String refObjectClass;

	@Expose
	String refObjectKey;

	@Expose
	boolean downloadable = true;

	@Expose
	protected String ownerLogin = ""; // the userId or botId

	@Expose
	int privacyStatus = 0;// 0: public, 1: protected or -1: private

	List<Long> viewerIds = new ArrayList<>();

	@Expose
	long networkId = AppMetadata.DEFAULT_ID;

	public FileMetadata() {
	}

	public FileMetadata(String ownerLogin, String path, String name, String refObjectClass, String refObjectKey) {
		super();
		this.ownerLogin = ownerLogin;
		this.path = path;
		this.name = name;
		this.createdAt = new Date();
		this.uploadedTime = createdAt.getTime();
		this.refObjectClass = refObjectClass;
		this.refObjectKey = refObjectKey;
	}

	public static final String COLLECTION_NAME = getCdpCollectionName(FileMetadata.class);
	static ArangoCollection collection;

	@Override
	public ArangoCollection getDbCollection() {
		if (collection == null) {
			ArangoDatabase arangoDatabase = ArangoDbUtil.getCdpDatabase();
			collection = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields
			collection.ensurePersistentIndex(Arrays.asList("path"), new PersistentIndexOptions().unique(true));
			collection.ensurePersistentIndex(Arrays.asList("refObjectClass", "refObjectKey"), new PersistentIndexOptions().unique(false));
			collection.ensureFulltextIndex(Arrays.asList("name"), new FulltextIndexOptions().minLength(1));
			collection.ensurePersistentIndex(Arrays.asList("networkId"), new PersistentIndexOptions().unique(false));
			collection.ensurePersistentIndex(Arrays.asList("ownerLogin"), new PersistentIndexOptions().unique(false));
		}
		return collection;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(name) && uploadedTime > 0 && StringUtil.isNotEmpty(this.path);
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if( StringUtil.isNotEmpty(this.path) ) {
			this.id = createId(this.id, this.path);
			return this.id;
		}
		else {
			newIllegalArgumentException("The path of uploaded file is required ");
		}
		return null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getUploadedTime() {
		return uploadedTime;
	}

	public void setUploadedTime(long uploadedTime) {
		this.uploadedTime = uploadedTime;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	public boolean isDownloadable() {
		return downloadable;
	}

	public void setDownloadable(boolean downloadable) {
		this.downloadable = downloadable;
	}

	public int getPrivacyStatus() {
		return privacyStatus;
	}

	public void setPrivacyStatus(int privacyStatus) {
		this.privacyStatus = privacyStatus;
	}

	public List<Long> getViewerIds() {
		return viewerIds;
	}

	public void setViewerIds(List<Long> viewerIds) {
		this.viewerIds = viewerIds;
	}

	public String getRefObjectClass() {
		return refObjectClass;
	}

	public void setRefObjectClass(String refObjectClass) {
		this.refObjectClass = refObjectClass;
	}

	public String getRefObjectKey() {
		return refObjectKey;
	}

	public void setRefObjectKey(String refObjectKey) {
		this.refObjectKey = refObjectKey;
	}

	public String getOwnerLogin() {
		return ownerLogin;
	}

	public void setOwnerLogin(String ownerLogin) {
		this.ownerLogin = ownerLogin;
	}

	public long getNetworkId() {
		return networkId;
	}

	public void setNetworkId(long networkId) {
		this.networkId = networkId;
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
		return this.createdAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		//skip due to immutable data
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.createdAt);
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

}
