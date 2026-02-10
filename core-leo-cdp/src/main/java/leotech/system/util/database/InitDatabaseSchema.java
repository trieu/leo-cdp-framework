package leotech.system.util.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.CollectionCreateOptions;

import leotech.cdp.dao.graph.GraphProfile2Content;
import leotech.cdp.dao.graph.GraphProfile2Conversion;
import leotech.cdp.dao.graph.GraphProfile2Product;
import leotech.cdp.dao.graph.GraphProfile2Profile;
import leotech.cdp.dao.graph.GraphProfile2TouchpointHub;
import leotech.cdp.domain.AgentManagement;
import leotech.cdp.domain.AssetCategoryManagement;
import leotech.cdp.domain.DeviceManagement;
import leotech.cdp.domain.JourneyMapManagement;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.domain.schema.JourneyFlowSchema;
import leotech.cdp.model.activation.ActivationRule;
import leotech.cdp.model.activation.Agent;
import leotech.cdp.model.analytics.ContextSession;
import leotech.cdp.model.analytics.DailyReportUnit;
import leotech.cdp.model.analytics.FeedbackData;
import leotech.cdp.model.analytics.FinanceEvent;
import leotech.cdp.model.analytics.Notebook;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.analytics.WebhookDataEvent;
import leotech.cdp.model.asset.AssetCategory;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.FileMetadata;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.asset.SocialEvent;
import leotech.cdp.model.customer.Device;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.journey.DataFlowStage;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.model.marketing.Campaign;
import leotech.cdp.model.marketing.TargetMediaUnit;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.domain.SystemConfigsManagement;
import leotech.system.model.AppMetadata;
import leotech.system.model.SystemEvent;
import leotech.system.model.SystemService;
import leotech.system.model.SystemUser;
import rfx.core.util.StringUtil;

/**
 * Core utility to initialize and synchronize the ArangoDB schema for LeoCDP.
 * Handles collection creation, graph setup, directory environment, and data
 * bootstrapping. 
 * 
 * @author tantrieuf31
 * @since 2019
 */
public class InitDatabaseSchema {

	private static final List<String> REQUIRED_PATHS = List.of("./public/qrcode", "./public/exported-files",
			"./public/uploaded-files");

	private static final List<String> SYS_COLLECTIONS = List.of(SystemService.COLLECTION_NAME,
			SystemEvent.COLLECTION_NAME, SystemUser.COLLECTION_NAME);

	private static final List<String> CDP_COLLECTIONS = List.of(ActivationRule.COLLECTION_NAME,
			AssetCategory.COLLECTION_NAME, AssetContent.COLLECTION_NAME, AssetGroup.COLLECTION_NAME,
			AssetTemplate.COLLECTION_NAME, Campaign.COLLECTION_NAME, ContextSession.COLLECTION_NAME,
			DailyReportUnit.COLLECTION_NAME, Agent.COLLECTION_NAME, DataFlowStage.COLLECTION_NAME,
			Device.COLLECTION_NAME, EventMetric.COLLECTION_NAME, EventObserver.COLLECTION_NAME,
			FeedbackData.COLLECTION_NAME, FileMetadata.COLLECTION_NAME, FinanceEvent.COLLECTION_NAME,
			JourneyMap.COLLECTION_NAME, Notebook.COLLECTION_NAME, ProductItem.COLLECTION_NAME, Profile.COLLECTION_NAME,
			Segment.COLLECTION_NAME, SocialEvent.COLLECTION_NAME, TargetMediaUnit.COLLECTION_NAME,
			Touchpoint.COLLECTION_NAME, TouchpointHub.COLLECTION_NAME, TrackingEvent.COLLECTION_NAME,
			WebhookDataEvent.COLLECTION_NAME);

	private static final AtomicBoolean systemReady = new AtomicBoolean(false);

	/**
	 * Verifies if the system database and local filesystem environment are ready.
	 * Checks for the existence of the SystemUser collection and required public
	 * folders. * @return true if both DB and Filesystem environments are
	 * initialized.
	 */
	public static boolean isSystemDbReady() {
		// Ensure required local directories exist before checking DB
		ensureRequiredDirectoriesExist();

		if (systemReady.get()) {
			return true;
		}

		ArangoDatabase dbInstance = ArangoDbUtil.getCdpDatabase();
		boolean dbExists = dbInstance.getCollections().stream()
				.anyMatch(col -> col.getName().equals(SystemUser.COLLECTION_NAME));

		systemReady.set(dbExists);
		return dbExists;
	}

	/**
	 * Synchronizes all DB collections and graphs, and imports data if necessary.
	 */
	public static void checkAndCreateDbCollections(String leoPackage, String dbConfigKey, String adminEmail,
			String adminPass) {
		boolean isNewSetup = setupDefaultSystemConfigs(dbConfigKey);

		setupCdpCollections(dbConfigKey);
		setupCdpGraph(dbConfigKey);

		if (isNewSetup && StringUtil.isNotEmpty(adminEmail) && StringUtil.isNotEmpty(adminPass)) {
			try {
				importingDefaultData(adminEmail, adminPass);
			} catch (IOException e) {
				System.err.println("[CRITICAL] Data import failed: " + e.getMessage());
			}
		}
	}

	/**
	 * Validates and creates required application directories on the local
	 * filesystem.
	 */
	private static void ensureRequiredDirectoriesExist() {
		for (String pathStr : REQUIRED_PATHS) {
			try {
				Path path = Paths.get(pathStr);
				if (Files.notExists(path)) {
					Files.createDirectories(path);
					System.out.println("[INFO] Environment setup: Created directory " + path.toAbsolutePath());
				}
			} catch (IOException e) {
				System.err.println("[ERROR] Failed to initialize directory: " + pathStr + " -> " + e.getMessage());
			}
		}
	}

	/**
	 * Batch creates new collections with standard options.
	 */
	public static void createNewCollections(ArangoDatabase db, List<String> collectionNames) {
		CollectionCreateOptions options = new CollectionCreateOptions();
		for (String name : collectionNames) {
			try {
				db.createCollection(name, options);
				System.out.println("[INFO] Database setup: Created collection " + name);
			} catch (ArangoDBException e) {
				System.err.println("[ERROR] ArangoDB Error: " + name + " -> " + e.getMessage());
			}
		}
	}

	/**
	 * Initializes CDP collections and sets up indices for high-volume entities.
	 */
	static void setupCdpCollections(String dbKey) {
		ArangoDatabase db = ArangoDbUtil.initActiveArangoDatabase(dbKey);
		Set<String> existingNames = db.getCollections().stream().map(col -> col.getName()).collect(Collectors.toSet());

		List<String> toCreate = CDP_COLLECTIONS.stream().filter(name -> !existingNames.contains(name))
				.collect(Collectors.toList());

		if (!toCreate.isEmpty()) {
			createNewCollections(db, toCreate);
		}

		// Ensure performance indices exist
		Profile.initCollectionAndIndex();
		TrackingEvent.initCollectionAndIndex();
	}

	/**
	 * Initializes Graph schemas for entity relationships.
	 */
	static void setupCdpGraph(String dbKey) {
		ArangoDatabase db = ArangoDbUtil.initActiveArangoDatabase(dbKey);
		GraphProfile2Profile.initGraph(db);
		GraphProfile2Product.initGraph(db);
		GraphProfile2Content.initGraph(db);
		GraphProfile2Conversion.initGraph(db);
		GraphProfile2TouchpointHub.initGraph(db);
		System.out.println("[INFO] Database setup: Graphs initialized.");
	}

	/**
	 * Verifies system configuration status. * @return true if a fresh installation
	 * is detected.
	 */
	public static boolean setupDefaultSystemConfigs(String dbKey) {
		ArangoDatabase db = ArangoDbUtil.initActiveArangoDatabase(dbKey);
		Set<String> existingNames = db.getCollections().stream().map(col -> col.getName()).collect(Collectors.toSet());

		List<String> toCreate = SYS_COLLECTIONS.stream().filter(name -> !existingNames.contains(name))
				.collect(Collectors.toList());

		boolean isFreshInstall = toCreate.contains(SystemUser.COLLECTION_NAME);

		if (!toCreate.isEmpty()) {
			createNewCollections(db, toCreate);
		}

		return isFreshInstall;
	}

	/**
	 * Populates a fresh database with default administrative and business data.
	 */
	static void importingDefaultData(String adminEmail, String adminPassword) throws IOException {
		SystemUser admin = SystemUserDaoUtil.getByUserLogin(SystemUser.SUPER_ADMIN_LOGIN);

		if (admin == null) {
			admin = new SystemUser(SystemUser.SUPER_ADMIN_LOGIN, adminPassword, SystemUser.SUPER_ADMIN_NAME, adminEmail,
					AppMetadata.DEFAULT_ID);
			SystemUserDaoUtil.createNewSystemUser(admin);
			SystemUserDaoUtil.activateAsSuperAdmin(admin.getUserLogin());
		}

		// Initialize business domain defaults
		SystemConfigsManagement.initDefaultSystemData(true);
		AssetCategoryManagement.initDefaultSystemData(admin);
		JourneyMapManagement.initDefaultSystemData();
		DeviceManagement.initDefaultSystemData();
		JourneyFlowSchema.initDefaultSystemData();
		SegmentDataManagement.initDefaultSystemData();
		AgentManagement.initDefaultData(true);

		System.out.println("[SUCCESS] Initial data bootstrapping completed.");
	}
}