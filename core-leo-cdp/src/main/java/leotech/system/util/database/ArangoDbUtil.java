package leotech.system.util.database;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
			dbConfigs = DatabaseConfigs.load(SystemMetaData.MAIN_DATABASE_CONFIG);
			if(dbConfigs == null) {
				Utils.exitSystemAfterTimeout(2000);
				throw new IllegalArgumentException(SystemMetaData.MAIN_DATABASE_CONFIG + " in ./configs/database-configs.json is not found ! ");
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
			DatabaseConfigs dbConfig = DatabaseConfigs.load(dbConfigKey);
			
			System.out.println("--------------------------------");
			System.out.println("[DbConfigs] load dbConfigKey : " + dbConfigKey );
			System.out.println("Host: "+dbConfig.getHost() + " Database: " + dbConfig.getDatabase() + " Port: " + dbConfig.getPort());
			System.out.println("--------------------------------");
			
			String dbName = dbConfig.getDatabase();
			String host = dbConfig.getHost();
			int port = dbConfig.getPort();
			String username = dbConfig.getUsername();
			String pass = dbConfig.getPassword();
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
