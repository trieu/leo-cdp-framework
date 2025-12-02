package leotech.system.config;

import static leotech.system.util.database.ArangoDbUtil.DEFAULT_ARANGO_DATABASE;
import static leotech.system.util.database.ArangoDbUtil.DEFAULT_ARANGO_HOST;
import static leotech.system.util.database.ArangoDbUtil.DEFAULT_ARANGO_PASSWORD;
import static leotech.system.util.database.ArangoDbUtil.DEFAULT_ARANGO_PORT;
import static leotech.system.util.database.ArangoDbUtil.DEFAULT_ARANGO_USERNAME;
import static leotech.system.util.database.ArangoDbUtil.ENV_ARANGO_DATABASE;
import static leotech.system.util.database.ArangoDbUtil.ENV_ARANGO_HOST;
import static leotech.system.util.database.ArangoDbUtil.ENV_ARANGO_PASSWORD;
import static leotech.system.util.database.ArangoDbUtil.ENV_ARANGO_PORT;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import leotech.system.util.database.ArangoDbUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.util.CommonUtil;
import rfx.core.util.FileUtils;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

public class DatabaseConfigs implements Serializable {

	private static final long serialVersionUID = 6185084071488833500L;

	public static final String SYSTEM_ENV_VARS = "SYSTEM_ENV_VARS";
	
	public static final int MAX_CONNECTIONS = 100;
	public static final int MIN_CONNECTIONS = 2;
	public static final long MAX_WAIT = 15000;

	public final static String MY_SQL = "mysql";
	public final static String SQL_SERVER = "sqlserver";
	public final static String ORACLE = "oracle";
	public final static String POSTGRESQL = "postgresql";

	public final static String ARANGODB = "arangodb";
	public final static String CLICKHOUSE = "clickhouse";

	public static class DbConfigsMap {
		private HashMap<String, DatabaseConfigs> configs;

		public DbConfigsMap() {
		}

		public HashMap<String, DatabaseConfigs> getConfigs() {
			if (configs == null) {
				configs = new HashMap<String, DatabaseConfigs>(0);
			}
			return configs;
		}

		public void setConfigs(HashMap<String, DatabaseConfigs> configs) {
			this.configs = configs;
		}
	}

	String dbId = StringPool.BLANK;
	private String username;
	private String password;
	private String database;
	private String host;
	private int port;
	private String dbdriver;
	private String dbdriverclasspath;
	private boolean enabled = true;
	private Map<String, Long> partitionMap;

	static final Map<String, DatabaseConfigs> dbConfigsPool = new HashMap<String, DatabaseConfigs>(6);
	static final Map<String, Driver> driversCache = new HashMap<String, Driver>();
	static String dbConfigsJson = null;

	public static DatabaseConfigs load(String dbId) {
		String databaseConfigFile = CommonUtil.getDatabaseConfigFile();
		if(StringUtil.isNotEmpty(SystemMetaData.RUNTIME_ENVIRONMENT)) {
			String replacement = "/"+SystemMetaData.RUNTIME_ENVIRONMENT +"-";
			databaseConfigFile = databaseConfigFile.replace("/", replacement);
		}
		return loadFromFile(databaseConfigFile, dbId.trim());
	}

	public static DatabaseConfigs loadConfigs(String sqlDbConfigsJson, String dbId) {
		String k = dbId;
		if (!dbConfigsPool.containsKey(k)) {
			DatabaseConfigs configs = null;
			try {
				DbConfigsMap map = new Gson().fromJson(sqlDbConfigsJson, DbConfigsMap.class);
				configs = map.getConfigs().get(dbId);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (configs == null) {
				throw new IllegalArgumentException("CAN NOT LOAD SqlDbConfigs from JSON: " + sqlDbConfigsJson);
			}
			configs.setDbId(dbId);
			dbConfigsPool.put(k, configs);
		}
		return dbConfigsPool.get(k);
	}

	public static DatabaseConfigs loadFromFile(String filePath, String dbId) {
		String k = dbId;
		if (dbConfigsJson == null) {
			try {
				dbConfigsJson = FileUtils.readFileAsString(filePath);
			} catch (IOException e) {
				throw new IllegalArgumentException("File is not found at " + filePath);
			}
		}
		if (!dbConfigsPool.containsKey(k)) {
			DatabaseConfigs configs = null;
			try {
				DbConfigsMap map = new Gson().fromJson(dbConfigsJson, DbConfigsMap.class);
				configs = map.getConfigs().get(dbId);
			} catch (Exception e) {
				if (e instanceof JsonSyntaxException) {
					System.err.println("Wrong JSON syntax in file " + filePath);
				} else {
					e.printStackTrace();
				}
			}
			
			// 
			if(DatabaseConfigs.SYSTEM_ENV_VARS.equalsIgnoreCase(dbId)) {
				String username = System.getenv().getOrDefault(ArangoDbUtil.ENV_ARANGO_USERNAME, DEFAULT_ARANGO_USERNAME);
		        String password = System.getenv().getOrDefault(ENV_ARANGO_PASSWORD, DEFAULT_ARANGO_PASSWORD);
		        String database = System.getenv().getOrDefault(ENV_ARANGO_DATABASE, DEFAULT_ARANGO_DATABASE);
		        String host     = System.getenv().getOrDefault(ENV_ARANGO_HOST, DEFAULT_ARANGO_HOST);
		        int port = StringUtil.safeParseInt(System.getenv().getOrDefault(ENV_ARANGO_PORT, DEFAULT_ARANGO_PORT));
		        configs = new DatabaseConfigs(username, password, database, host, port);
			}
			
			if (configs == null) {
				throw new IllegalArgumentException("CAN NOT LOAD DatabaseConfigs dbId " + dbId + " from " + filePath);
			}
			configs.setDbId(dbId);
			dbConfigsPool.put(k, configs);
		}
		return dbConfigsPool.get(k);
	}

	public String getConnectionUrl() {
		StringBuilder url = new StringBuilder();
		if (MY_SQL.equals(this.getDbdriver())) {
			
			url.append("jdbc:").append(MY_SQL).append("://");
			url.append(this.getHost());
			url.append(":").append(getPort()).append("/");
			url.append(this.getDatabase());
			url.append("?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&noAccessToProcedureBodies=true");
			
		} 
		else if (SQL_SERVER.equals(this.getDbdriver())) {
			
			url.append("jdbc:").append(SQL_SERVER).append("://");
			url.append(this.getHost());
			url.append(";databaseName=");
			url.append(this.getDatabase());
			
		} 
		else if (ORACLE.equals(this.getDbdriver())) {
			
			System.setProperty("java.security.egd", "file:///dev/urandom");
			// "jdbc:oracle:thin:@IP:PORT:ORADEV1"
			url.append("jdbc:").append(ORACLE).append(":thin:@");
			url.append(this.getHost());
			url.append(":").append(this.getPort()).append(":");
			url.append(this.getDatabase());
			
		} 
		else if (CLICKHOUSE.equals(this.getDbdriver())) {
			
			// "jdbc:clickhouse://127.0.0.1:8123"
			url.append("jdbc:").append(CLICKHOUSE);
			url.append(this.getHost());
			url.append(":").append(this.getPort());
			
		} 
		else {
			throw new IllegalArgumentException(
					"Currently, only support JDBC driver for MySQL, MSSQL Server and Oracle!");
		}
		return url.toString();
	}

	public DatabaseConfigs() {
	}
	
	public DatabaseConfigs(String username, String password, String database, String host, int port) {
		super();
		this.username = username;
		this.password = password;
		this.database = database;
		this.host = host;
		this.port = port;
		
		if (StringUtil.isEmpty(username) || StringUtil.isEmpty(password)
		  || StringUtil.isEmpty(database) || StringUtil.isEmpty(host) || port <= 0) {
		    throw new IllegalArgumentException(
	            "Invalid ArangoDB configuration. Required env variables: "
	            + ArangoDbUtil.ENV_ARANGO_USERNAME + ", "
	            + ArangoDbUtil.ENV_ARANGO_PASSWORD + ", "
	            + ArangoDbUtil.ENV_ARANGO_DATABASE + ", "
	            + ArangoDbUtil. ENV_ARANGO_HOST + ", "
	            + ArangoDbUtil.ENV_ARANGO_PORT
	            + ". Check your environment setup."
	        );
		}
		
		this.dbId = SYSTEM_ENV_VARS;
		this.dbdriver = ARANGODB;
		this.dbdriverclasspath = ArangoDbUtil.ARANGO_JAVA_DRIVER;
		this.enabled = true;
		this.partitionMap = Map.of("Profile", 1L);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getDbdriver() {
		return dbdriver;
	}

	public void setDbdriver(String dbdriver) {
		this.dbdriver = dbdriver;
	}

	public String getDbdriverclasspath() {
		return dbdriverclasspath;
	}

	public void setDbdriverclasspath(String dbdriverclasspath) {
		this.dbdriverclasspath = dbdriverclasspath;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDbId() {
		return dbId;
	}

	public void setDbId(String dbId) {
		this.dbId = dbId;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	

	public Map<String, Long> getPartitionMap() {
		if(this.partitionMap == null) {
			this.partitionMap = new HashMap<>(0);
		}
		return partitionMap;
	}

	public void setPartitionMap(Map<String, Long> partitionMap) {
		this.partitionMap = partitionMap;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(" ,Dbdriver: ").append(getDbdriver());
		s.append(" ,Dbdriverclasspath: ").append(getDbdriverclasspath());
		s.append(" ,Host: ").append(getHost());
		s.append(" ,Database: ").append(getDatabase());
		s.append(" ,Username: ").append(getUsername());
		s.append(" ,Password.length: ").append(getPassword().length());
		s.append(" ,ConnectionUrl: ").append(getConnectionUrl());
		return s.toString();
	}
}
