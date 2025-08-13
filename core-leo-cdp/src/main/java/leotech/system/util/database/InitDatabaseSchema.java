package leotech.system.util.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import leotech.cdp.domain.AssetCategoryManagement;
import leotech.cdp.domain.DataServiceManagement;
import leotech.cdp.domain.DeviceManagement;
import leotech.cdp.domain.JourneyMapManagement;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.domain.schema.JourneyFlowSchema;
import leotech.cdp.model.activation.ActivationRule;
import leotech.cdp.model.activation.DataService;
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
import leotech.cdp.model.customer.BusinessAccount;
import leotech.cdp.model.customer.BusinessCase;
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
 * 
 * Tool to setup new default database with basic data for LeoCDP
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public class InitDatabaseSchema {

	static final List<String> sysCollections = new ArrayList<>();
	static final List<String> cdpCollections = new ArrayList<>();
	
	static final AtomicBoolean systemReady = new AtomicBoolean(false);
	
	static {
		// 32 core CDP data models
		
		// A
		cdpCollections.add(ActivationRule.COLLECTION_NAME); // 1
		cdpCollections.add(AssetCategory.COLLECTION_NAME); // 2
		cdpCollections.add(AssetContent.COLLECTION_NAME); // 3
		cdpCollections.add(AssetGroup.COLLECTION_NAME); // 4
		cdpCollections.add(AssetTemplate.COLLECTION_NAME); // 5
		
		// B
		cdpCollections.add(BusinessAccount.COLLECTION_NAME); // 6
		cdpCollections.add(BusinessCase.COLLECTION_NAME); // 7
		
		// C
		cdpCollections.add(Campaign.COLLECTION_NAME); // 8
		cdpCollections.add(ContextSession.COLLECTION_NAME); // 9 
		
		// D
		cdpCollections.add(DailyReportUnit.COLLECTION_NAME); // 10
		cdpCollections.add(DataService.COLLECTION_NAME); // 11
		cdpCollections.add(DataFlowStage.COLLECTION_NAME); // 12
		cdpCollections.add(Device.COLLECTION_NAME); // 13
		
		// E
		cdpCollections.add(EventMetric.COLLECTION_NAME); // 14
		cdpCollections.add(EventObserver.COLLECTION_NAME); // 15
		
		// F
		cdpCollections.add(FeedbackData.COLLECTION_NAME); // 16
		cdpCollections.add(FileMetadata.COLLECTION_NAME); // 17
		cdpCollections.add(FinanceEvent.COLLECTION_NAME); // 18
		
		// J
		cdpCollections.add(JourneyMap.COLLECTION_NAME); // 19
		
		// N
		cdpCollections.add(Notebook.COLLECTION_NAME); // 20
		
		// P
		cdpCollections.add(ProductItem.COLLECTION_NAME); // 21
		cdpCollections.add(Profile.COLLECTION_NAME); // 22
		
		// S
		cdpCollections.add(Segment.COLLECTION_NAME); // 23
		cdpCollections.add(SocialEvent.COLLECTION_NAME); // 24
		
		// T
		cdpCollections.add(TargetMediaUnit.COLLECTION_NAME); // 25
		cdpCollections.add(Touchpoint.COLLECTION_NAME);  // 26
		cdpCollections.add(TouchpointHub.COLLECTION_NAME); // 27
		cdpCollections.add(TrackingEvent.COLLECTION_NAME); // 28
		
		// W
		cdpCollections.add(WebhookDataEvent.COLLECTION_NAME);// 29
		
		// System 
		sysCollections.add(SystemService.COLLECTION_NAME); // 30
		sysCollections.add(SystemEvent.COLLECTION_NAME); // 31
		sysCollections.add(SystemUser.COLLECTION_NAME); // 32
	}
	
	
	/**
	 * @return
	 */
	public static boolean isSystemDbReady() {
		if(systemReady.get()) {
			return true;
		}
		else {
			ArangoDatabase dbInstance = ArangoDbUtil.getCdpDatabase();
			boolean check = dbInstance.getCollections().stream().filter(col->{
				return col.getName().equals(SystemUser.COLLECTION_NAME);
			}).count() == 1 ;
			systemReady.set(check);
			return check;
		}
	}
	

	/**
	 * @param dbInstance
	 * @param list
	 */
	public static void createNewCollections(ArangoDatabase dbInstance, List<String> list) {
		CollectionCreateOptions options = new CollectionCreateOptions();
		for (String colName : list) {
			try {
				dbInstance.createCollection(colName, options);
				System.out.println("=> Successfully createCollection " + colName);
			} catch (ArangoDBException e) {
				System.err.println("=> Failed to createCollection " + colName);
				e.printStackTrace();
			}
		}
	}
	

	/**
	 * 
	 * keep this method for clustering
	 * 
	 * @param dbKey
	 */
	static void setupCdpCollections(String dbKey) {
		ArangoDatabase dbInstance = ArangoDbUtil.initActiveArangoDatabase(dbKey);
		Collection<String> currentDbCollections = dbInstance.getCollections().stream().map(col -> {
			return col.getName();
		}).collect(Collectors.toList());

		List<String> cols = new ArrayList<>(cdpCollections.size());
		for (String colName : cdpCollections) {
			boolean isExisted = currentDbCollections.contains(colName);
			if (!isExisted) {
				cols.add(colName);
				System.out.println("=> [OK] to create collection: " + colName);
			} else {
				System.err.println("=> [SKIP] to create collection: " + colName);
			}
		}
		
		if(cols.size() > 0) {
			createNewCollections(dbInstance, cols);
		}
		
		// upgrade index
		Profile.initCollectionAndIndex();
		TrackingEvent.initCollectionAndIndex();
	}
	
	
	/**
	 * @param dbKey
	 */
	static void setupCdpGraph(String dbKey) {
		ArangoDatabase db = ArangoDbUtil.initActiveArangoDatabase(dbKey);
		
		// ############################## GRAPHS ##############################
		
		// init Graph Collection: Profile To Profile
		GraphProfile2Profile.initGraph(db);
		
		// init Graph Collection: Profile To Product
		GraphProfile2Product.initGraph(db);
				
		// init Graph Collection: Profile To Creative
		GraphProfile2Content.initGraph(db);
		
		// init Graph Collection: Profile To Purchased Product
		GraphProfile2Conversion.initGraph(db);
		
		// init Graph Collection: Profile To Touchpoint Hub
		GraphProfile2TouchpointHub.initGraph(db);
		
		System.out.println(" =====> OK, DONE INIT GRAPHS ===== ");
		// ############################## GRAPHS ##############################
	}
	

	/**
	 * @param dbKey
	 * @return
	 */
	public static boolean setupDefaultSystemConfigs(String dbKey) {
		ArangoDatabase dbInstance = ArangoDbUtil.initActiveArangoDatabase(dbKey);
		Collection<String> currentDbCollections = dbInstance.getCollections().stream().map(col -> {
			String name = col.getName();
			System.out.println(" dbKey " + dbKey + " has the collection " + name);
			return name;
		}).collect(Collectors.toList());

		// just create collections when SystemUser collection is not existed in the database
		List<String> cols = new ArrayList<>(sysCollections.size());
		boolean importDefaultData = false;
		for (String colName : sysCollections) {
			boolean isExisted = currentDbCollections.contains(colName);
			if (!isExisted) {
				cols.add(colName);
				// if collection "SystemUser" is not existed, we can make sure this is a new system setup
				if (colName.equalsIgnoreCase(SystemUser.COLLECTION_NAME)) {
					importDefaultData = true;
				}
			}
			else {
				System.err.println("=> [SKIP] to create collection: " + colName);
			}
		}
		System.out.println("importDefaultData " + importDefaultData);
		
		// setup core system collections
		createNewCollections(dbInstance, cols);

		return importDefaultData;
	}

	/**
	 * @param superAdminEmail
	 * @param superAdminPassword
	 * @throws IOException
	 */
	static void importingDefaultData(String superAdminEmail, String superAdminPassword) throws IOException {
		// default data importing
		SystemUser superAdmin = SystemUserDaoUtil.getByUserLogin(SystemUser.SUPER_ADMIN_LOGIN);

		// make sure do not override existing data
		boolean ok = true;
		if (superAdmin == null) {
			superAdmin = new SystemUser(SystemUser.SUPER_ADMIN_LOGIN, superAdminPassword, SystemUser.SUPER_ADMIN_NAME , superAdminEmail, AppMetadata.DEFAULT_ID);
			SystemUserDaoUtil.createNewSystemUser(superAdmin);
			ok = SystemUserDaoUtil.activateAsSuperAdmin(superAdmin.getUserLogin());
		}
		if (ok) {
			// ############################## INIT DEFAULT SYSTEM DATA ##############################
			SystemConfigsManagement.initDefaultSystemData(true);
			
			// Asset for Marketing and Communication
			AssetCategoryManagement.initDefaultSystemData(superAdmin);
			
			// init default Data Journey Map
			JourneyMapManagement.initDefaultSystemData();
			
			// Device
			DeviceManagement.initDefaultSystemData();
			
			// Journey Data Flow
			JourneyFlowSchema.initDefaultSystemData();
			
			// 4 default segments for customer data management
			SegmentDataManagement.initDefaultSystemData();
			
			// all data services and activation services
			DataServiceManagement.initDefaultSystemData();
		
			System.out.println(" =====> OK, DONE INIT DEFAULT SYSTEM DATA ===== ");
			
			// check and create folder qrcode
			File qrCodeHolder = new File("./public/qrcode");
			if(!qrCodeHolder.isDirectory()) {
				if(qrCodeHolder.mkdir()) {
					System.out.println(" =====> created folder " + qrCodeHolder.getAbsolutePath());
				}
			}
			
			// check and create folder exported-files 
			File exportedFileHolder = new File("./public/exported-files");
			if(!exportedFileHolder.isDirectory()) {
				if(exportedFileHolder.mkdir()) {
					System.out.println(" =====> created folder " + exportedFileHolder.getAbsolutePath());
				}
			}
			
			// check and create folder uploaded-files
			File uploadedFileHolder = new File("./public/uploaded-files");
			if(!uploadedFileHolder.isDirectory()) {
				if(uploadedFileHolder.mkdir()) {
					System.out.println(" =====> created folder " + uploadedFileHolder.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * @param leoPackage
	 * @param dbConfigKey
	 * @param superAdminEmail
	 * @param superAdminPassword
	 * @param industry
	 */
	public static void checkAndCreateDbCollections(String leoPackage, String dbConfigKey, String superAdminEmail, String superAdminPassword) {
		
		// 3 sys data collections
		boolean importDefaultData = setupDefaultSystemConfigs(dbConfigKey);
		
		// 31 cdp data collections
		setupCdpCollections(dbConfigKey); 
		
		// 3 edge collections
		setupCdpGraph(dbConfigKey);
		
		// check to import data in new setup only
		if (importDefaultData && StringUtil.isNotEmpty(superAdminEmail) && StringUtil.isNotEmpty(superAdminPassword) ) {
			try {
				importingDefaultData(superAdminEmail, superAdminPassword);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
