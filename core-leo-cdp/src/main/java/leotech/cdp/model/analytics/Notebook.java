package leotech.cdp.model.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * 
 * Jupyter Notebook Model for doing analytics, data science tasks and building machine learning models
 * 
 * @author tantrieuf31
 * @since 2020/08/30
 *
 */
public final class Notebook extends PersistentObject  {
	
	public static final String COLLECTION_NAME = getCdpCollectionName(Notebook.class);
	static ArangoCollection instance;

	@Key
	@Expose
	String id;
	
	@Expose
	String type;
	
	@Expose
	int status;
	
	@Expose
	protected String name = "";
	
	@Expose
	protected String notebookFileUri = "";
	
	@Expose
	protected String notebookOutputFileUri = "";
	
	@Expose
	protected String htmlFileUri = "";
	
	@Expose
	protected String pythonFileUri = "";
	
	@Expose
	protected String description = "";
	
	@Expose
	protected List<String> dataSources = new ArrayList<String>();
	
	@Expose
	protected Map<String, Object> parameters = new HashMap<String, Object>();
	
	@Expose
	protected Date createdAt;
	
	@Expose
	protected Date updatedAt;
	
	@Expose
	protected int autoRunAfterMinute = 0;
	
	@Expose
	protected Date lastRunAt;
	
	@Expose
	protected String accessToken;
	
	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			instance = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields for fast lookup
			instance.ensureFulltextIndex(Arrays.asList("name"), new FulltextIndexOptions());
			instance.ensureFulltextIndex(Arrays.asList("description"), new FulltextIndexOptions());
			instance.ensurePersistentIndex(Arrays.asList("createdAt"), new PersistentIndexOptions().unique(false));
		}
		return instance;
	}
	
	public Notebook(String type, String name) {
		if(name.length() > 10) {
			this.type = type;
			this.name = name;
			
			this.createdAt = new Date();
			
			Slugify slg = new Slugify();
			String slugifiedName = slg.slugify(name);
			this.notebookFileUri = slugifiedName + ".ipynb";
			this.notebookOutputFileUri = slugifiedName + "-output.ipynb";
			this.htmlFileUri = slugifiedName + "-output.html";
			
			this.buildHashedId();
		} else {
			throw new IllegalArgumentException("Notebook's name must has more than 10 characters");
		}
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if(type != null && name != null) {
			String keyHint = type + name;
			this.id = createId(this.id, keyHint);
			return this.id;
		}
		else {
			newIllegalArgumentException("type and name is required");
		}
		return null;
	}
	
	
	public static ArangoCollection getInstance() {
		return instance;
	}

	public static void setInstance(ArangoCollection instance) {
		Notebook.instance = instance;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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
	
	public Date getLastRunAt() {
		return lastRunAt;
	}

	public void setLastRunAt(Date lastRunAt) {
		this.lastRunAt = lastRunAt;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getId() {
		return id;
	}

	public int getAutoRunAfterMinute() {
		return autoRunAfterMinute;
	}

	public void setAutoRunAfterMinute(int autoRunAfterMinute) {
		this.autoRunAfterMinute = autoRunAfterMinute;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.type) && StringUtil.isNotEmpty(this.name) && (this.createdAt != null) ;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
}
