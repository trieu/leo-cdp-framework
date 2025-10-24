package leotech.cdp.model.journey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.PersistentIndexOptions;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.analytics.TouchpointHubReport;
import leotech.system.exception.InvalidDataException;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringUtil;

/**
 * The journey map is a data entity that contains profile, touchpoints, products, contents and services
 * In free version 1.x, we give them only 1 journey map in database
 * 
 * @author tantrieuf31 
 * @since 2020
 */
public final class JourneyMap extends PersistentObject {
	
	public static final String DEFAULT_JOURNEY_MAP_ID = "id_default_journey";
	public static final String DEFAULT_JOURNEY_MAP_NAME = "DEFAULT JOURNEY MAP";
	public static final String DEFAULT_EVENT_METRIC = "page-view";

	public static final String COLLECTION_NAME = getCdpCollectionName(JourneyMap.class);
	static ArangoCollection dbCol;

	@Override
	public ArangoCollection getDbCollection() {
		if (dbCol == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();

			dbCol = arangoDatabase.collection(COLLECTION_NAME);

			// ensure indexing key fields for fast lookup
			dbCol.ensurePersistentIndex(Arrays.asList("updatedAt"),new PersistentIndexOptions().unique(false));
			
			// to check permission
			dbCol.ensurePersistentIndex(Arrays.asList("authorizedEditors[*]"),new PersistentIndexOptions().inBackground(true).unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("authorizedViewers[*]"),new PersistentIndexOptions().inBackground(true).unique(false));
		}
		return dbCol;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.name) && this.journeyStages.size() > 0 && touchpointHubMap != null;
	}


	@Key
	@Expose
	protected String id;

	@Expose
	protected String name;
	
	@Expose
	protected int status = STATUS_ACTIVE; 

	@Expose
	protected Date createdAt;

	@Expose
	protected Date updatedAt;

	@Expose
	protected String defaultMetricName = DEFAULT_EVENT_METRIC;
	
	/**
	 * A list of login users who can view data, default only admin and super admin can view data
	 */
	@Expose
	protected Set<String> authorizedViewers = new HashSet<String>();
	
	/**
	 * A list of login users who can update data, default only admin and super admin can update data
	 */
	@Expose
	protected Set<String> authorizedEditors = new HashSet<String>();
	

	// list of ordered nodes for journey map visualization in JS
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected List<String> journeyStages = new ArrayList<String>();

	// list of ordered nodes for journey map visualization in JS
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected List<JourneyNode> journeyNodes = new ArrayList<>(); 

	// list of node connections for journey map visualization in JS
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected List<JourneyNodeLink> journeyLinks = new ArrayList<>(); 

	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected Map<String, TouchpointHub> touchpointHubMap;

	// list of ordered nodes for data persistence with reversed indexing
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected Map<String, Integer> touchpointHubIndex; 
	
	@Expose
	@com.arangodb.velocypack.annotations.Expose(deserialize = false, serialize = false)
	protected List<TouchpointHub> sortedTouchpointHubs;


	public JourneyMap() {
		// gson
		if(authorizedViewers == null) {
			authorizedViewers = new HashSet<String>();	
		}
		if(authorizedEditors == null) {
			authorizedEditors = new HashSet<String>();	
		}
	}
	
	public JourneyMap(String name) {
		super(); 
		initJourneyMap(name);
	}
		
	protected void initJourneyMap(String name) {
		this.name = name;
		this.createdAt = new Date();
		this.updatedAt = this.createdAt;
		this.buildHashedId();
	}

	public void setTouchpointHubs(List<TouchpointHub> sortedTouchpointHubs, Map<String, TouchpointHub> touchpointHubMap, Map<String, Integer> touchpointHubIndex) {
		this.sortedTouchpointHubs = sortedTouchpointHubs;
		this.touchpointHubMap = touchpointHubMap;
		this.touchpointHubIndex = touchpointHubIndex;

		Set<String> hubNames = touchpointHubMap.keySet();
		for (String hubName : hubNames) {
			TouchpointHub hub = touchpointHubMap.get(hubName);
			int index = touchpointHubIndex.get(hubName);
			journeyStages.add(hubName);
			
			String label =  "[" + hub.getJourneyLevel() + "] " + hubName ;
			journeyNodes.add(new JourneyNode(index, hubName, label));
		}
		Collections.sort(journeyNodes);
	}
	
	/**
	 * create id from the name of journey map
	 * 
	 * @param name
	 * @return
	 */
	public static String getId(String name) {
		return createHashedId(name);
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if( StringUtil.isNotEmpty(this.name) ) {
			if(DEFAULT_JOURNEY_MAP_NAME.equals(this.name)) {
				this.id = DEFAULT_JOURNEY_MAP_ID;
			}
			else if(this.name.length() > 5) {
				this.id = createId(this.id, this.name);
			} 
			else {
				newIllegalArgumentException("JourneyMap.name.length must be larger than 5");
			}
		}
		else {
			newIllegalArgumentException("JourneyMap.name is required");
		}
		return this.id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public List<String> getJourneyStages() {
		return journeyStages;
	}

	public void setJourneyStages(List<String> journeyStages) {
		this.journeyStages = journeyStages;
	}

	public List<JourneyNodeLink> getJourneyLinks() {
		return journeyLinks;
	}

	public void setJourneyLinks(List<JourneyNodeLink> journeyLinks) {
		this.journeyLinks = journeyLinks;
	}

	public void addJourneyLink(TouchpointHub mediaSource, TouchpointHub mediaTarget, TouchpointHubReport report, long defaultValue) {
		int mediaSourceIndex = touchpointHubIndex.getOrDefault(mediaSource.getName(), -1);
		int mediaTargetIndex = touchpointHubIndex.getOrDefault(mediaTarget.getName(), -1);
		if (mediaSourceIndex >= 0 && mediaTargetIndex >= 0) {
			this.journeyLinks.add(new JourneyNodeLink(mediaSourceIndex, mediaTargetIndex, report, defaultValue));
		}
	}
	
	public void addJourneyLink(TouchpointHub mediaSource, TouchpointHub mediaTarget, long reportedValue) {
		int mediaSourceIndex = touchpointHubIndex.getOrDefault(mediaSource.getName(), -1);
		int mediaTargetIndex = touchpointHubIndex.getOrDefault(mediaTarget.getName(), -1);
		if (mediaSourceIndex >= 0 && mediaTargetIndex >= 0) {
			this.journeyLinks.add(new JourneyNodeLink(mediaSourceIndex, mediaTargetIndex, reportedValue));
		}
	}

	public List<JourneyNode> getJourneyNodes() {
		return journeyNodes;
	}

	public void setJourneyNodes(List<JourneyNode> journeyNodes) {
		this.journeyNodes = journeyNodes;
	}
	
	public List<TouchpointHub> getSortedTouchpointHubs() {
		if(this.sortedTouchpointHubs != null) {
			return this.sortedTouchpointHubs;	
		}
		return new ArrayList<TouchpointHub>(0);
	}

	public Map<String, TouchpointHub> getTouchpointHubMap() {
		return touchpointHubMap;
	}

	public void setTouchpointHubMap(Map<String, TouchpointHub> touchpointHubMap) {
		this.touchpointHubMap = touchpointHubMap;
	}

	public Map<String, Integer> getTouchpointHubIndex() {
		return touchpointHubIndex;
	}

	public void setTouchpointHubIndex(Map<String, Integer> touchpointHubIndex) {
		this.touchpointHubIndex = touchpointHubIndex;
	}


	public String getDefaultMetricName() {
		return defaultMetricName;
	}

	public void setDefaultMetricName(String defaultMetricName) {
		this.defaultMetricName = defaultMetricName;
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
	
	public Set<String> getAuthorizedEditors() {
		if(authorizedEditors == null) {
			authorizedEditors = new HashSet<>();
		}
		return authorizedEditors;
	}

	public void setAuthorizedEditors(Set<String> authorizedEditors) {
		if(authorizedEditors != null) {
			this.authorizedEditors = authorizedEditors;
		}
	}
	
	public Set<String> updateAuthorizedEditorsAndReturnRemovedEditors(Set<String> authorizedEditors) {
		if(authorizedEditors != null) {
			Set<String> removedEditors = Sets.difference(this.authorizedEditors, authorizedEditors);
			this.authorizedEditors = authorizedEditors;
			return removedEditors;
		}
		return new HashSet<>(0);
	}
	
	public Set<String> setAuthorizedEditor(String authorizedEditor) {
		if(StringUtil.isNotEmpty(authorizedEditor)) {
			getAuthorizedEditors().add(authorizedEditor);
		}
		return authorizedEditors;
	}
	
	public Set<String> removeAuthorizedEditor(String authorizedEditor) {
		if(StringUtil.isNotEmpty(authorizedEditor)) {
			getAuthorizedEditors().remove(authorizedEditor);
		}
		return Sets.newHashSet(authorizedEditor);
	}
	
	public Set<String> getAuthorizedViewers() {
		if(authorizedViewers == null) {
			authorizedViewers = new HashSet<>();
		}
		return authorizedViewers;
	}

	public void setAuthorizedViewers(Set<String> authorizedViewers) {
		if(authorizedViewers != null) {
			this.authorizedViewers = authorizedViewers;
		}
	}
	
	public Set<String> updateAuthorizedViewersAndReturnRemovedViewers(Set<String> authorizedViewers) {
		if(authorizedViewers != null) {
			Set<String> removedViewers = Sets.difference(this.authorizedViewers, authorizedViewers);
			this.authorizedViewers = authorizedViewers;
			return removedViewers;
		}
		return new HashSet<>(0);
	}
	
	public Set<String> setAuthorizedViewer(String authorizedViewer) {
		if(StringUtil.isNotEmpty(authorizedViewer)) {
			getAuthorizedViewers().add(authorizedViewer);
		}
		return this.authorizedViewers;
	}
	
	public Set<String> removeAuthorizedViewer(String authorizedViewer) {
		if(StringUtil.isNotEmpty(authorizedViewer)) {
			getAuthorizedViewers().remove(authorizedViewer);
		}
		return Sets.newHashSet(authorizedViewer);
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public String getDocumentUUID() {
		return COLLECTION_NAME + "/" + id;
	}
	
	public JourneyMapRefKey getJourneyMapRefKey() {
		return new JourneyMapRefKey(this.id, this.name,"");
	}
	
	public JourneyMapRefKey getJourneyMapRefKey(String profileType) {
		return new JourneyMapRefKey(this.id, this.name,profileType);
	}
	
	/**
	 * build default Journey Map from touchpointHubs
	 * 
	 * @param touchpointHubs
	 * @return default JourneyMap 
	 */
	public static JourneyMap buildDefaultJourneyMap(List<TouchpointHub> touchpointHubs) {
		return buildJourneyMap(DEFAULT_JOURNEY_MAP_NAME, touchpointHubs);
	}
	
	/**
	 * @param map
	 * @param touchpointHubs
	 * @param reports
	 * @return
	 */
	public static JourneyMap setTouchpointHubsForJourneyMap(JourneyMap map, List<TouchpointHub> touchpointHubs, List<TouchpointHubReport> reports) {
		return setTouchpointHubsForJourneyMap(map, touchpointHubs, reports, false);
	}
	
	/**
	 * build Journey Map from name and touchpointHubs
	 * 
	 * @param journeyMapName as string
	 * @param touchpointHubs of TouchpointHub
	 * @return processed JourneyMap
	 */
	public static JourneyMap buildJourneyMap(String journeyMapName, List<TouchpointHub> touchpointHubs) {
		return setTouchpointHubsForJourneyMap(new JourneyMap(journeyMapName), touchpointHubs, null, false);
	}
	
	/**
	 * build Journey Map from name and touchpointHubs
	 * 
	 * @param journeyMapName as string
	 * @param touchpointHubs of TouchpointHub
	 * @param map from database
	 * @return processed JourneyMap
	 */
	public static JourneyMap setTouchpointHubsForJourneyMap(JourneyMap map, List<TouchpointHub> touchpointHubs, boolean validationInput) {
		return setTouchpointHubsForJourneyMap(map, touchpointHubs, null, validationInput);
	}

	/**
	 * @param map
	 * @param touchpointHubs
	 * @param reports
	 * @param validationInput
	 * @return
	 */
	public static JourneyMap setTouchpointHubsForJourneyMap(JourneyMap map, List<TouchpointHub> touchpointHubs, List<TouchpointHubReport> reports, boolean validationInput) {
		int size = touchpointHubs.size();
		if(size >= 2 && map != null) {
			String journeyMapId = map.getId();
			
			// core elements of Journey Map
			Map<String, TouchpointHub> touchpointHubMap = new HashMap<>(size);
			Map<String, Integer> touchpointHubIndex = new HashMap<>(size);
			
			TouchpointHub cdpDatabase = TouchpointHub.DATA_OBSERVER;
			cdpDatabase.setJourneyMapId(journeyMapId);
			
			// find and update the last Level Index for LEO_DATA_OBSERVER
			int maxLevel = 1;
			boolean foundDataHub = false;
			for (int i = 0; i < size; i++) {
				TouchpointHub touchpointHub = touchpointHubs.get(i);
				touchpointHub.setJourneyMapId(journeyMapId);
				
				int journeyLevel = touchpointHub.getJourneyLevel();
				int touchpointType = touchpointHub.getType();
				if(touchpointType == TouchpointType.DATA_OBSERVER) {
					cdpDatabase = touchpointHub;
					foundDataHub = true;
				} 
				else if(journeyLevel >= maxLevel){
					maxLevel = journeyLevel + 1;
				}
				
				if(validationInput) {
					boolean ok = touchpointHub.dataValidation();
					if( ! ok ) {
						// skip invalid data
						System.err.println("FAIL touchpointHub.dataValidation " + touchpointHub.getName());
						continue;
					}
				}
				String hubName = touchpointHub.getName();
				touchpointHubMap.put(hubName, touchpointHub);
				touchpointHubIndex.put(hubName, i);
			}
			
			// the CDP Database must be the end position in the Data Journey Map
			cdpDatabase.setJourneyLevel(maxLevel);

			if(! foundDataHub) {
				TouchpointHub.DATA_OBSERVER.setJourneyLevel(maxLevel+1);
				touchpointHubs.add(TouchpointHub.DATA_OBSERVER);
			}
			
			// sorting TouchpointHub to make sure the order of level
			Collections.sort(touchpointHubs, new Comparator<TouchpointHub>() {
				@Override
				public int compare(TouchpointHub o1, TouchpointHub o2) {
					int journeyLevel1 = o1.getJourneyLevel();
					int journeyLevel2 = o2.getJourneyLevel();
					if (journeyLevel1 > journeyLevel2) {
						return 1;
					} else if (journeyLevel1 < journeyLevel2) {
						return -1;
					}
					return 0;
				}
			});
			
			// update to the map
			map.setTouchpointHubs(touchpointHubs, touchpointHubMap, touchpointHubIndex);
			
			TouchpointHubReport defaultReport = new TouchpointHubReport(1);
			if(reports == null) {
				for (TouchpointHub touchpointHub1 : touchpointHubs) {
					for (TouchpointHub touchpointHub2 : touchpointHubs) {
						int journeyLevel2 = touchpointHub2.getJourneyLevel();
						int journeyLevel1 = touchpointHub1.getJourneyLevel();
						if (journeyLevel2 > journeyLevel1) {
							map.addJourneyLink(touchpointHub1, touchpointHub2, defaultReport, 1);
						}
					}
				}
			}
			else {
				// set report data for link, FIXME 
				Map<String, TouchpointHubReport> reportMap = new HashMap<>(reports.size());
				
				// count profile report
				long totalProfile = 0;
				for (TouchpointHubReport tpReport : reports) {
					reportMap.put(tpReport.getTouchpointHubId(), tpReport);
					totalProfile += tpReport.getProfileCount();
				}
				
				// set report into TouchpointHub
				for (TouchpointHub touchpointHub1 : touchpointHubs) {
					TouchpointHubReport report = reportMap.getOrDefault(touchpointHub1.getId(), defaultReport);
					touchpointHub1.setTotalProfile(report.getProfileCount());
					touchpointHub1.setReport(report);
					
					for (TouchpointHub touchpointHub2 : touchpointHubs) {
						int journeyLevel2 = touchpointHub2.getJourneyLevel();
						int journeyLevel1 = touchpointHub1.getJourneyLevel();
						if (journeyLevel2 > journeyLevel1) {
							map.addJourneyLink(touchpointHub1, touchpointHub2, report, 1);
						}
					}
				}
				cdpDatabase.setTotalProfile(totalProfile);
			}
			return map;
		}
		else {
			throw new InvalidDataException("JourneyMap must not be NULL and touchpointHubs.size must be larger or equals 2 !");
		}
	}
}
