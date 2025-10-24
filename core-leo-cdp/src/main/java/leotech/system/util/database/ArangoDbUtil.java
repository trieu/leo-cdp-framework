package leotech.system.util.database;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;

import leotech.system.config.DatabaseConfigs;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * Core ArangoDb Utility class
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public final class ArangoDbUtil {
	
	static Logger logger = LoggerFactory.getLogger(ArangoDbUtil.class);
	
	public static final String ARANGO_JAVA_DRIVER = "com.arangodb.ArangoDB";

	 // Environment variable keys
    public static final String ENV_ARANGO_USERNAME = "ARANGODB_USERNAME";
    public static final String ENV_ARANGO_PASSWORD = "ARANGODB_PASSWORD";
    public static final String ENV_ARANGO_DATABASE = "ARANGODB_DATABASE";
    public static final String ENV_ARANGO_HOST     = "ARANGODB_HOST";
    public static final String ENV_ARANGO_PORT     = "ARANGODB_PORT";

    // Default values
    public static final String DEFAULT_ARANGO_USERNAME = "root";
    public static final String DEFAULT_ARANGO_PASSWORD = "";
    public static final String DEFAULT_ARANGO_DATABASE = "leo_cdp_test";
    public static final String DEFAULT_ARANGO_HOST     = "localhost";
    public static final String DEFAULT_ARANGO_PORT     = "8529";
	
	static final int CHUNK_SIZE = 50000;
	static final long CONNECTION_TTL = 5 * 60 * 1000;
	static final int MAX_CONNECTIONS = 500;

	static DatabaseConfigs dbConfigs = null;
	static ArangoDB arangoDB = null;
	static ArangoDatabase activeArangoDbInstance = null;
	static Map<String, ArangoDatabase> arangoDbInstances = new HashMap<>();

	/**
	 * to init and get DatabaseConfigs
	 * 
	 * @return DatabaseConfigs
	 */
	public final static DatabaseConfigs initDbConfigs() {
		if (dbConfigs == null) {
			String mainDatabaseConfig = SystemMetaData.MAIN_DATABASE_CONFIG;
			dbConfigs = DatabaseConfigs.load(mainDatabaseConfig);
			if(dbConfigs == null) {
				Utils.exitSystemAfterTimeout(2000);
				throw new IllegalArgumentException(mainDatabaseConfig + " in ./configs/database-configs.json is not found ! ");
			}
			
		}
		return dbConfigs;
	}
	
	/**
	 * @return connectionUrl
	 */
	public final static String getDatabaseConnectionUrl() {
		if (dbConfigs != null) {
			return dbConfigs.getConnectionUrl();
		}
		return "ArangoDbUtil.dbConfigs is NULL";
	}

	/**
	 * set DatabaseConfigs
	 * 
	 * @param dbConfigs
	 */
	public final static void setDbConfigs(DatabaseConfigs configs) {
		if (configs != null) {
			ArangoDbUtil.dbConfigs = configs;
		}
	}

	/**
	 * 
	 * get Active ArangoDb Instance from mainDatabaseConfig in the file "leocdp-metadata.properties"
	 * 
	 * @return ArangoDatabase of CDP
	 */
	public final synchronized static ArangoDatabase getCdpDatabase() {
		initDbConfigs();
		if (activeArangoDbInstance == null) {
			arangoDB = buildDbInstance(dbConfigs.getHost(), dbConfigs.getPort(), dbConfigs.getUsername(), dbConfigs.getPassword());
			activeArangoDbInstance = arangoDB.db(dbConfigs.getDatabase());
		}
		return activeArangoDbInstance;
	}
	
	/**
	 * @return ArangoDatabase of system
	 */
	public final synchronized static ArangoDatabase getSystemDatabase() {
		initDbConfigs();
		if (activeArangoDbInstance == null) {
			arangoDB = buildDbInstance(dbConfigs.getHost(), dbConfigs.getPort(), dbConfigs.getUsername(), dbConfigs.getPassword());
			activeArangoDbInstance = arangoDB.db(dbConfigs.getDatabase());
		}
		return activeArangoDbInstance;
	}
	
	

	/**
	 * @param dbConfigKey
	 * @return
	 */
	public final synchronized static ArangoDatabase initActiveArangoDatabase(String dbConfigKey) {
		ArangoDatabase dbInstance = arangoDbInstances.get(dbConfigKey);
		if (dbInstance == null) {
			DatabaseConfigs dbconfig = DatabaseConfigs.load(dbConfigKey);
			
			logger.info("--------------------------------");
			logger.info("[DbConfigs] load dbConfigKey : " + dbConfigKey );
			logger.info("Host: "+dbconfig.getHost() + " Database: " + dbconfig.getDatabase() + " Port: " + dbconfig.getPort());
			logger.info("--------------------------------");
			
			String dbName = dbconfig.getDatabase();
			String host = dbconfig.getHost();
			int port = dbconfig.getPort();
			String username = dbconfig.getUsername();
			String pass = dbconfig.getPassword();
			ArangoDB arangoDB = buildDbInstance(host, port, username, pass);
			
			Collection<String> dbNames = arangoDB.getDatabases();
			boolean checkToCreateNewDatabase = false;
			for (String name : dbNames) {
				// make sure the database is existed 
				if(StringUtil.isNotEmpty(name) && name.equals(dbName)) {
					checkToCreateNewDatabase = true;
				}
			}
			
			// try to create a new db
			if( ! checkToCreateNewDatabase ) {
				// new system, create new database for CDP
				checkToCreateNewDatabase = arangoDB.createDatabase(dbName);
			}
			
			// ok to get dbInstance
			if(checkToCreateNewDatabase) {
				// ready to connect
				dbInstance = arangoDB.db(dbName);	
			}
			
			arangoDbInstances.put(dbConfigKey, dbInstance);
			
			// if active instance is NULL then set default ArangoDB instance for current active system
			if(activeArangoDbInstance == null) {
				activeArangoDbInstance = dbInstance;
			}
		}
		return dbInstance;
	}

	/**
	 * @param host
	 * @param port
	 * @param username
	 * @param pass
	 * @return
	 */
	public final static ArangoDB buildDbInstance(String host, int port, String username, String pass) {
		ArangoDB arangoDB = new ArangoDB.Builder().host(host, port).user(username).password(pass)
				.chunksize(CHUNK_SIZE).connectionTtl(CONNECTION_TTL).maxConnections(MAX_CONNECTIONS).build();
		return arangoDB;
	}

	/**
	 * @param collectionName
	 * @return
	 */
	public final static  String contentFindKeyAql(String collectionName) {
		return "FOR e in " + collectionName + " FILTER e._key == @id RETURN e._key";
	}

	/**
	 * @param aql
	 * @param fieldName
	 * @param indexedId
	 * @return
	 */
	public final static String findKey(String aql, String fieldName, Object indexedId) {
		Map<String, Object> bindKeys = new HashMap<>();
		bindKeys.put(fieldName, indexedId);
		ArangoCursor<String> cursor2 = ArangoDbUtil.getCdpDatabase().query(aql, bindKeys, String.class);
		while (cursor2.hasNext()) {
			String key = cursor2.next();
			return key;
		}
		return null;
	}

	/**
	 * @param collectionName
	 * @param id
	 * @return
	 */
	public final static boolean isExistedDocument(String collectionName, String id) {	
		return isExistedDocument(ArangoDbUtil.getCdpDatabase(), collectionName, id);
	}
	
	/**
	 * @param db
	 * @param collectionName
	 * @param id
	 * @return
	 */
	public final static boolean isExistedDocument(ArangoDatabase db, String collectionName, String id) {
		String aql = "RETURN LENGTH(FOR d IN " + collectionName + " FILTER d._key == @id LIMIT 1 RETURN true) > 0";
		Map<String, Object> bindKeys = new HashMap<>(1);
		bindKeys.put("id", id);
		ArangoCursor<Boolean> cursor = db.query(aql, bindKeys, Boolean.class);
		while (cursor.hasNext()) {
			return cursor.next();
		}
		return false;
	}

}
