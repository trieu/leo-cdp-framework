package leotech.cdp.model.activation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.quartz.Job;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.PersistentIndexOptions;
import com.github.slugify.Slugify;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.data.service.ExternalAgentService;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.SystemService;
import leotech.system.util.LogUtil;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * Agent is the computational object to represent the external service, which can be used for 
 * data synchronization, data enrichment, and personalization in marketing campaigns <br><br>
 * 
 * An agent can be a data connector to connect to external data source, and synchronize data to CDP <br>
 * An agent can be a data service to provide data enrichment for customer profiles, products, and contents in CDP <br>
 * An agent can be a personalization service to provide real-time recommendation for customer profiles in CDP <br><br>
 * 
 * ArangoDB Collection: cdp_agent <br>
 * 
 * @author tantrieuf31
 * @since 2026
 */
public final class Agent extends SystemService implements Serializable {
	
	private static final long serialVersionUID = -8291232649419081101L;
	
	// Action types
	public static final String ALL = "all";
	public static final String ACTIVE = "active";
	
	public static final String PURPOSE_PERSONALIZATION = "personalization";
	public static final String PURPOSE_DATA_ENRICHMENT = "data_enrichment";
	public static final String PURPOSE_SYNCHRONIZATION = "synchronization";

	public static final String NEW_SERVICE_PREFIX = "new_service_";
	public static final String DATA_SERVICE_JAVACLASS = "javaclass:";
	public static final String DATA_SERVICE_EXTERNAL = ExternalAgentService.class.getSimpleName();
	
	public static final String COLLECTION_NAME = PersistentObject.getCdpCollectionName(Agent.class);
	
	// FIX: Thread-safe lazy initialization
	private static volatile ArangoCollection instance;
	
	// FIX: Cache Slugify instance to prevent massive object creation overhead on every validation
	private static final Slugify SLUGIFY = new Slugify();

	@Expose
	private String serviceUri = "";
	
	@Expose
	private Date startedAt;
	
	@Expose
	private boolean forSynchronization = false;

	@Expose
	private boolean forDataEnrichment = false;

	@Expose
	private boolean forPersonalization = false;

	public Agent() {
		super();
		this.id = NEW_SERVICE_PREFIX + System.currentTimeMillis(); 
		initDefaultConfigs();
	}
	
	public Agent(String name) {
		super();
		this.name = name;
		initDefaultConfigs();
		buildHashedId();
	}

	public Agent(String name, Map<String, Object> configs) {
		super();
		this.name = name;
		this.configs = configs;
		buildHashedId();
	}

	private void initDefaultConfigs() {
		this.serviceUri = DATA_SERVICE_EXTERNAL;
		this.configs.put(SERVICE_PROVIDER, "");
		this.configs.put(SERVICE_API_URL, "");
		this.configs.put(SERVICE_API_KEY, "");
	}

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			// FIX: Double-checked locking for thread-safety during index creation
			synchronized (Agent.class) {
				if (instance == null) {
					ArangoDatabase arangoDatabase = ArangoDbUtil.getCdpDatabase();
					ArangoCollection col = arangoDatabase.collection(COLLECTION_NAME);

					PersistentIndexOptions uniqueOpts = new PersistentIndexOptions().unique(true);
					PersistentIndexOptions nonUniqueOpts = new PersistentIndexOptions().unique(false);

					// --------------------------------------------------------------------------------
					// ARANGODB 3.11 INDEX OPTIMIZATION
					// Previously, there was an index on ["name", "description", "updatedAt"].
					// Because "name" is unique, any lookup containing "name" will instantly return 1 doc.
					// The composite index was 100% redundant, wasting I/O and RAM. It has been removed.
					// Below are optimized indices tailored for System Service filtering & routing.
					// --------------------------------------------------------------------------------
					
					// Fast exact lookup
					col.ensurePersistentIndex(Arrays.asList("name"), uniqueOpts);
					
					// Core querying indices for the job orchestrator to find active agents by purpose
					col.ensurePersistentIndex(Arrays.asList("status", "forSynchronization"), nonUniqueOpts);
					col.ensurePersistentIndex(Arrays.asList("status", "forDataEnrichment"), nonUniqueOpts);
					col.ensurePersistentIndex(Arrays.asList("status", "forPersonalization"), nonUniqueOpts);
					
					// Used for UI sorting by most recently updated active agents
					col.ensurePersistentIndex(Arrays.asList("status", "updatedAt"), nonUniqueOpts);

					instance = col;
				}
			}
		}
		return instance;
	}
	
	public String buildHashedId() throws IllegalArgumentException {
		String safeId = StringUtil.safeString(this.id);
		
		// FIX: Utilized Java 11's .isBlank()
		if ((safeId.isBlank() || safeId.startsWith(NEW_SERVICE_PREFIX)) && StringUtil.isNotEmpty(this.name)) {
			String hint = this.name.substring(0, Math.min(name.length(), 20));
			this.id = SLUGIFY.slugify(hint); // Using static cached Slugify
		}
		return this.id;
	}
	
	@Override
	public boolean dataValidation() {
		buildHashedId();
		String url = StringUtil.safeString(this.configs.get(SERVICE_API_URL), "");
		String key = StringUtil.safeString(this.configs.get(SERVICE_API_KEY), "");
		
		if (StringUtil.isValidUrl(url) && StringUtil.isNotEmpty(key)) {
			this.status = 1;
		}		
		return StringUtil.isNotEmpty(id) && StringUtil.isNotEmpty(name) && configs != null;
	}
	
	@SuppressWarnings("unchecked")
	public Class<? extends Job> getClassForJobDetails() {
		if (StringUtil.isNotEmpty(serviceUri) && serviceUri.startsWith(DATA_SERVICE_JAVACLASS)) {
			String classpath = serviceUri.replace(DATA_SERVICE_JAVACLASS, "");
			try {
				Class<? extends Job> clazz = (Class<? extends Job>) Class.forName(classpath);		
				LogUtil.logInfo(Agent.class, "getClassForJobDetails using " + clazz.getName());
				return clazz;
			} catch (ClassNotFoundException | ClassCastException e) {
				// FIX: Never use e.printStackTrace() in production. Propagate properly.
				LogUtil.logError(Agent.class, "Failed to create/load class: " + classpath, e);
				throw new InvalidDataException("Failed to load Job class: " + classpath);
			}
		}
		
		LogUtil.logInfo(Agent.class, "getClassForJobDetails using " + ExternalAgentService.class.getName());
		return ExternalAgentService.class;
	}

	// ----------------------------------------------------------------------
	// GETTERS & SETTERS
	// ----------------------------------------------------------------------

	public boolean isForPersonalization() { return forPersonalization; }
	public void setForPersonalization(boolean forPersonalization) { this.forPersonalization = forPersonalization; }

	public boolean isForDataEnrichment() { return forDataEnrichment; }
	public void setForDataEnrichment(boolean forDataEnrichment) { this.forDataEnrichment = forDataEnrichment; }
	
	public boolean isForSynchronization() { return forSynchronization; }
	public void setForSynchronization(boolean forSynchronization) { this.forSynchronization = forSynchronization; }

	public String getServiceUri() { return serviceUri; }
	public void setServiceUri(String serviceUri) { this.serviceUri = serviceUri; }

	public Date getStartedAt() { return startedAt; }
	public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
	
	public boolean isStartedConnnector() {
		return startedAt != null;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}