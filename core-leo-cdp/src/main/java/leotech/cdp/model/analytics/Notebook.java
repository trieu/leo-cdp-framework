package leotech.cdp.model.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.FulltextIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * Jupyter Notebook Model for Data Science, Analytics, and Machine Learning
 * inside LEO CDP. <br>
 * Used for storing metadata of Jupyter Notebooks, orchestrating scheduled
 * executions, passing dynamic parameters to Python scripts, and tracking ML
 * model outputs. <br>
 * 
 * ArangoDB collection: cdp_notebook
 * 
 * @author tantrieuf31
 * @since 2020/08/30
 */
public final class Notebook extends PersistentObject {

	public static final String COLLECTION_NAME = getCdpCollectionName(Notebook.class);

	// Thread-safe lazy initialization
	private static volatile ArangoCollection instance;

	// Cached Slugify to prevent severe regex memory leaks on instantiation
	private static final Slugify SLUGIFY = new Slugify();

	// Notebook Status Constants
	public static final int STATUS_DRAFT = 0;
	public static final int STATUS_ACTIVE = 1;
	public static final int STATUS_ARCHIVED = -1;

	// Execution Status Constants
	public static final int RUN_STATUS_PENDING = 0;
	public static final int RUN_STATUS_SUCCESS = 1;
	public static final int RUN_STATUS_FAILED = -1;
	public static final int RUN_STATUS_RUNNING = 2;

	@Key
	@Expose
	private String id;

	@Expose
	private String type;

	@Expose
	private int status = STATUS_ACTIVE;

	@Expose
	private String name = "";

	@Expose
	private String description = "";

	// To classify ML models (e.g., ["churn-prediction", "clv", "recommendation"])
	@Expose
	private List<String> tags = new ArrayList<>();

	/**
	 * A list of login users who can view data, default only admin and super admin
	 * can view data
	 */
	@Expose
	protected Set<String> authorizedViewers = new HashSet<String>();

	/**
	 * A list of login users who can update data, default only admin and super admin
	 * can update data
	 */
	@Expose
	protected Set<String> authorizedEditors = new HashSet<String>();

	@Expose
	private String notebookFileUri = "";

	@Expose
	private String notebookOutputFileUri = "";

	@Expose
	private String htmlFileUri = "";

	@Expose
	private String pythonFileUri = "";

	@Expose
	private List<String> dataSources = new ArrayList<>();

	@Expose
	private Map<String, Object> parameters = new HashMap<>();

	@Expose
	private Date createdAt;

	@Expose
	private Date updatedAt;

	// Scheduling & Execution
	@Expose
	private int autoRunAfterMinute = 0; // 0 means manual execution only

	@Expose
	private Date lastRunAt;

	// Operational tracking for ML Jobs
	@Expose
	private int lastRunStatus = RUN_STATUS_PENDING;

	// Captures Python stack traces if the notebook fails
	@Expose
	private String lastErrorMessage = "";

	@Expose
	private String accessToken;

	public Notebook() {
		// Default constructor for Gson
	}

	public Notebook(String type, String name) {
		if (StringUtil.isNotEmpty(name) && name.length() > 10) {
			this.type = type;
			this.name = name;

			Date now = new Date();
			this.createdAt = now;
			this.updatedAt = now;

			//  Uses cached static SLUGIFY engine
			String slugifiedName = SLUGIFY.slugify(name);
			this.notebookFileUri = slugifiedName + ".ipynb";
			this.notebookOutputFileUri = slugifiedName + "-output.ipynb";
			this.htmlFileUri = slugifiedName + "-output.html";

			this.buildHashedId();
		} else {
			throw new IllegalArgumentException("Notebook's name must contain more than 10 characters");
		}
	}

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			// Double-checked locking to prevent index race conditions
			synchronized (Notebook.class) {
				if (instance == null) {
					ArangoDatabase arangoDatabase = getArangoDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);

					PersistentIndexOptions pIdxOpts = new PersistentIndexOptions().unique(false);

					// --------------------------------------------------------------------------------
					// ARANGODB 3.11 INDEX OPTIMIZATION (RocksDB Engine)
					// Notebooks act as Cron Jobs. The system needs to query ArangoDB constantly to
					// see
					// which active notebooks are due for execution. We also need UI filtering
					// indices.
					// --------------------------------------------------------------------------------

					// Core UI Lookup: Find active notebooks by Type
					col.ensurePersistentIndex(Arrays.asList("status", "type"), pIdxOpts);

					// Job Orchestrator Index: Instantly finds Active scheduled notebooks
					col.ensurePersistentIndex(Arrays.asList("status", "autoRunAfterMinute"), pIdxOpts);

					// Observability Index: Find failing or running models
					col.ensurePersistentIndex(Arrays.asList("status", "lastRunStatus"), pIdxOpts);

					// Tag filtering for sorting ML Models
					col.ensurePersistentIndex(Arrays.asList("tags[*]"), pIdxOpts);

					// Legacy Fulltext indexing for UI search functionality
					col.ensureFulltextIndex(Arrays.asList("name"), new FulltextIndexOptions().minLength(2));

					instance = col;
				}
			}
		}
		return instance;
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (StringUtil.isNotEmpty(this.type) && StringUtil.isNotEmpty(this.name)) {
			String keyHint = this.type + this.name;
			this.id = createId(this.id, keyHint);
			return this.id;
		} else {
			throw new IllegalArgumentException("Type and Name are required to build a Notebook ID");
		}
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.type) && StringUtil.isNotEmpty(this.name) && this.createdAt != null;
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
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

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public Set<String> getAuthorizedViewers() {
		return authorizedViewers;
	}

	public void setAuthorizedViewers(Set<String> authorizedViewers) {
		this.authorizedViewers = authorizedViewers;
	}

	public Set<String> getAuthorizedEditors() {
		return authorizedEditors;
	}

	public void setAuthorizedEditors(Set<String> authorizedEditors) {
		this.authorizedEditors = authorizedEditors;
	}

	public String getNotebookFileUri() {
		return notebookFileUri;
	}

	public void setNotebookFileUri(String notebookFileUri) {
		this.notebookFileUri = notebookFileUri;
	}

	public String getNotebookOutputFileUri() {
		return notebookOutputFileUri;
	}

	public void setNotebookOutputFileUri(String notebookOutputFileUri) {
		this.notebookOutputFileUri = notebookOutputFileUri;
	}

	public String getPythonFileUri() {
		return pythonFileUri;
	}

	public void setPythonFileUri(String pythonFileUri) {
		this.pythonFileUri = pythonFileUri;
	}

	public String getHtmlFileUri() {
		return htmlFileUri;
	}

	public void setHtmlFileUri(String htmlFileUri) {
		this.htmlFileUri = htmlFileUri;
	}

	public List<String> getDataSources() {
		return dataSources;
	}

	public void setDataSources(List<String> dataSources) {
		this.dataSources = dataSources;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
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

	public int getAutoRunAfterMinute() {
		return autoRunAfterMinute;
	}

	public void setAutoRunAfterMinute(int autoRunAfterMinute) {
		this.autoRunAfterMinute = autoRunAfterMinute;
	}

	public Date getLastRunAt() {
		return lastRunAt;
	}

	public void setLastRunAt(Date lastRunAt) {
		this.lastRunAt = lastRunAt;
	}

	public int getLastRunStatus() {
		return lastRunStatus;
	}

	public void setLastRunStatus(int lastRunStatus) {
		this.lastRunStatus = lastRunStatus;
	}

	public String getLastErrorMessage() {
		return lastErrorMessage;
	}

	public void setLastErrorMessage(String lastErrorMessage) {
		this.lastErrorMessage = lastErrorMessage;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
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