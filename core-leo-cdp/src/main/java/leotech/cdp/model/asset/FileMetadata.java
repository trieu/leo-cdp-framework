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
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.system.model.AppMetadata;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * File Metadata for uploaded assets. <br>
 * Used for storing metadata of uploaded files within LEO CDP, tracking attributes such as 
 * path, name, uploaded time, revision, polymorphic relational references (object class/key), 
 * ownership, privacy statuses, and viewer permissions. <br>
 * 
 * ArangoDB collection: cdp_filemetadata
 * 
 * @author TrieuNT
 * @since 2020
 */
public class FileMetadata extends PersistentObject {

	public static final String COLLECTION_NAME = getCdpCollectionName(FileMetadata.class);
	
	// FIX: Volatile for thread-safe lazy initialization
	private static volatile ArangoCollection collection;

	@Key
	@Expose
	private String id;

	@Expose
	private String path;

	@Expose
	private String name;

	@Expose
	private long uploadedTime;
	
	@Expose
	private Date createdAt;

	@Expose
	private int revision = 1;

	@Expose
	private String refObjectClass;

	@Expose
	private String refObjectKey;

	@Expose
	private boolean downloadable = true;

	@Expose
	protected String ownerLogin = ""; // the userId or botId

	@Expose
	private int privacyStatus = 0; // 0: public, 1: protected, -1: private

	// FIX: Added @Expose so Gson correctly serializes/deserializes viewer permissions
	@Expose
	private List<Long> viewerIds = new ArrayList<>();

	@Expose
	private long networkId = AppMetadata.DEFAULT_ID;

	public FileMetadata() {
		// Default constructor for Gson serialization
	}

	public FileMetadata(String ownerLogin, String path, String name, String refObjectClass, String refObjectKey) {
		this.ownerLogin = ownerLogin;
		this.path = path;
		this.name = name;
		this.refObjectClass = refObjectClass;
		this.refObjectKey = refObjectKey;
		
		Date now = new Date();
		this.createdAt = now;
		this.uploadedTime = now.getTime();
		
		this.buildHashedId();
	}

	@Override
	public ArangoCollection getDbCollection() {
		if (collection == null) {
			// FIX: Double-checked locking to prevent race conditions on ArangoDB index initialization
			synchronized (FileMetadata.class) {
				if (collection == null) {
					ArangoDatabase arangoDatabase = ArangoDbUtil.getCdpDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);
					
					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);

					// --------------------------------------------------------------------------------
					// ARANGODB 3.11 INDEX OPTIMIZATION (RocksDB Engine)
					// Removed standalone indices for `networkId` and `ownerLogin`. 
					// Replaced them with a hierarchical composite index. Using RocksDB's Left-to-Right 
					// evaluation, `["networkId", "ownerLogin", "privacyStatus"]` efficiently covers:
					// 1. Fetch all files for a Tenant (networkId)
					// 2. Fetch all files for a User (networkId + ownerLogin)
					// 3. Filter User files by access level (networkId + ownerLogin + privacyStatus)
					// --------------------------------------------------------------------------------
					
					// Fast exact lookup
					col.ensurePersistentIndex(Arrays.asList("path"), new PersistentIndexOptions().unique(true));
					
					// Core Access Control & Tenant filtering
					col.ensurePersistentIndex(Arrays.asList("networkId", "ownerLogin", "privacyStatus"), pIdxOpts);
					
					// Polymorphic relational lookup (e.g., Fetching all attachments for a specific Campaign or Profile)
					col.ensurePersistentIndex(Arrays.asList("refObjectClass", "refObjectKey"), pIdxOpts);
					
					// Note: In ArangoDB 3.10+, ArangoSearch (Views) is heavily recommended over standard Fulltext. 
					// However, standard Fulltext is retained here for legacy compatibility.
					col.ensureFulltextIndex(Arrays.asList("name"), new FulltextIndexOptions().minLength(1));

					collection = col;
				}
			}
		}
		return collection;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(name) 
				&& StringUtil.isNotEmpty(path) 
				&& uploadedTime > 0;
	}
	
	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (StringUtil.isNotEmpty(this.path)) {
			this.id = createId(this.id, this.path);
			return this.id;
		} else {
			// FIX: Using standard Java throw pattern to preserve stack trace and execution flow
			throw new IllegalArgumentException("The physical/logical path of the uploaded file is required to generate an ID.");
		}
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public long getUploadedTime() { return uploadedTime; }
	public void setUploadedTime(long uploadedTime) { this.uploadedTime = uploadedTime; }

	public int getRevision() { return revision; }
	public void setRevision(int revision) { this.revision = revision; }

	public boolean isDownloadable() { return downloadable; }
	public void setDownloadable(boolean downloadable) { this.downloadable = downloadable; }

	public int getPrivacyStatus() { return privacyStatus; }
	public void setPrivacyStatus(int privacyStatus) { this.privacyStatus = privacyStatus; }

	public List<Long> getViewerIds() { return viewerIds; }
	public void setViewerIds(List<Long> viewerIds) { this.viewerIds = viewerIds; }

	public String getRefObjectClass() { return refObjectClass; }
	public void setRefObjectClass(String refObjectClass) { this.refObjectClass = refObjectClass; }

	public String getRefObjectKey() { return refObjectKey; }
	public void setRefObjectKey(String refObjectKey) { this.refObjectKey = refObjectKey; }

	public String getOwnerLogin() { return ownerLogin; }
	public void setOwnerLogin(String ownerLogin) { this.ownerLogin = ownerLogin; }

	public long getNetworkId() { return networkId; }
	public void setNetworkId(long networkId) { this.networkId = networkId; }

	@Override
	public Date getCreatedAt() { return createdAt; }
	@Override
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

	@Override
	public Date getUpdatedAt() { return this.createdAt; }
	@Override
	public void setUpdatedAt(Date updatedAt) { 
		// Skip due to immutable data nature of File records
	}
	
	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.createdAt);
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}