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
 * Data service to enrich and activate profile, segment and touchpoint
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class DataService extends SystemService implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8291232649419081101L;
	
	//action types
	public static final String ALL = "all";
	public static final String ACTIVE = "active";
	
	public static final String PURPOSE_PERSONALIZATION = "personalization";
	public static final String PURPOSE_DATA_ENRICHMENT = "data_enrichment";
	public static final String PURPOSE_SYNCHRONIZATION = "synchronization";

	public static final String NEW_SERVICE_PREFIX = "new_service_";
	public static final String DATA_SERVICE_JAVACLASS = "javaclass:";
	public static final String DATA_SERVICE_EXTERNAL = ExternalAgentService.class.getSimpleName();
	
	public static final String COLLECTION_NAME = PersistentObject.getCdpCollectionName(DataService.class);
	private static ArangoCollection instance;

	@Override
	public ArangoCollection getDbCollection() {
		if (instance == null) {
			ArangoDatabase arangoDatabase = ArangoDbUtil.getCdpDatabase();
			
			instance = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields
			instance.ensurePersistentIndex(Arrays.asList("name"), new PersistentIndexOptions().unique(true));
			instance.ensurePersistentIndex(Arrays.asList("name","description","updatedAt"), new PersistentIndexOptions().unique(false));
		}
		return instance;
	}
	
	@Expose
	String serviceUri = "";
	
	@Expose
	Date startedAt;
	
	// BEGIN 3 purposes
	
	@Expose
	boolean forSynchronization = false; // 1

	@Expose
	boolean forDataEnrichment = false; // 2

	@Expose
	boolean forPersonalization = false; // 3
	
	// END 3 purposes

	
	public String buildHashedId() throws IllegalArgumentException {
		String safeId = StringUtil.safeString(this.id);
		if ( (safeId.isBlank() || safeId.startsWith(NEW_SERVICE_PREFIX)) && StringUtil.isNotEmpty(this.name)) {
			String hint = this.name.substring(0, Math.min(name.length(), 20));
			this.id = new Slugify().slugify(hint);
		}
		return id;
	}
	
	@Override
	public boolean dataValidation() {
		buildHashedId();
		String url = StringUtil.safeString(this.configs.get(SERVICE_API_URL),"") ;
		String key = StringUtil.safeString(this.configs.get(SERVICE_API_KEY),"") ;
		if( StringUtil.isValidUrl(url) && StringUtil.isNotEmpty(key)) {
			this.status = 1;
		}		
		return StringUtil.isNotEmpty(id) && StringUtil.isNotEmpty(name) && configs != null;
	}

	public DataService() {
		// default
		super();
		this.id = NEW_SERVICE_PREFIX + System.currentTimeMillis(); 
		this.serviceUri = DATA_SERVICE_EXTERNAL;
		this.configs.put(SERVICE_PROVIDER, "");
		this.configs.put(SERVICE_API_URL, "");
		this.configs.put(SERVICE_API_KEY, "");
	}
	
	public DataService(String name) {
		super();
		this.name = name;
		this.serviceUri = DATA_SERVICE_EXTERNAL;
		this.configs.put(SERVICE_PROVIDER, "");
		this.configs.put(SERVICE_API_URL, "");
		this.configs.put(SERVICE_API_KEY, "");
		buildHashedId();
	}

	public DataService(String name, Map<String, Object> configs) {
		super();
		this.name = name;
		this.configs = configs;
		buildHashedId();
	}

	public boolean isForPersonalization() {
		return forPersonalization;
	}

	public void setForPersonalization(boolean forPersonalization) {
		this.forPersonalization = forPersonalization;
	}

	public boolean isForDataEnrichment() {
		return forDataEnrichment;
	}

	public void setForDataEnrichment(boolean forDataEnrichment) {
		this.forDataEnrichment = forDataEnrichment;
	}
	
	public boolean isForSynchronization() {
		return forSynchronization;
	}

	public void setForSynchronization(boolean forSynchronization) {
		this.forSynchronization = forSynchronization;
	}

	public String getServiceUri() {
		return serviceUri;
	}

	public void setServiceUri(String serviceUri) {
		this.serviceUri = serviceUri;
	}
	
	@SuppressWarnings("unchecked")
	public Class<? extends Job> getClassForJobDetails(){
		String serviceUri = this.getServiceUri();
		Class<? extends Job> clazz = null;
		if(StringUtil.isNotEmpty(serviceUri)) {
			if(serviceUri.startsWith(DataService.DATA_SERVICE_JAVACLASS)) {
				String classpath = serviceUri.replace(DataService.DATA_SERVICE_JAVACLASS, "");
				try {
					clazz = (Class<Job>) Class.forName(classpath);		
					LogUtil.logInfo(DataService.class, "getClassForJobDetails using " + clazz.getName());
				} catch (Exception e) {
					e.printStackTrace();
					String s = "Failed to create " + classpath;
					throw new InvalidDataException(s);
				}
			}
			else {				
				clazz = ExternalAgentService.class;
				LogUtil.logInfo(DataService.class, "getClassForJobDetails using " + clazz.getName());
			}
		}
		return clazz;
	}

	public Date getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Date startedAt) {
		this.startedAt = startedAt;
	}
	
    public boolean isStartedConnnector() {
    	return startedAt != null;
    }

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
