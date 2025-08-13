package leotech.cdp.dao;

import com.arangodb.ArangoDatabase;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.OverwriteMode;

import leotech.system.util.database.ArangoDbUtil;
import leotech.system.version.SystemMetaData;


/**
 * Abstract Cdp Database Util Class
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public abstract class AbstractCdpDatabaseUtil {

	public static final String LEO_CDP_DB_CONFIGS = SystemMetaData.MAIN_DATABASE_CONFIG;

	/**
	 * @return
	 */
	public static final ArangoDatabase getCdpDatabase() {
		ArangoDatabase db = ArangoDbUtil.initActiveArangoDatabase(LEO_CDP_DB_CONFIGS);
		return db;
	}
	
	/**
	 * @return
	 */
	public static final ArangoDatabase getSystemDatabase() {
		ArangoDatabase db = ArangoDbUtil.initActiveArangoDatabase(LEO_CDP_DB_CONFIGS);
		return db;
	}
	
	/**
	 * @return
	 */
	protected static final DocumentUpdateOptions getUpdateOptions() {
		DocumentUpdateOptions options = new DocumentUpdateOptions();
		options.mergeObjects(true);// override data
		return options;
	}
	
	/**
	 * @return
	 */
	protected static final DocumentUpdateOptions getMergeOptions() {
		DocumentUpdateOptions options = new DocumentUpdateOptions();
		options.silent(true);
		options.mergeObjects(true);// not override data
		options.keepNull(true);
		return options;
	}
	
	/**
	 * @return
	 */
	protected static final DocumentCreateOptions insertDocumentOverwriteModeReplace() {
		return new DocumentCreateOptions().overwriteMode(OverwriteMode.replace).returnNew(true);
	}
	
	/**
	 * @return
	 */
	protected static final DocumentCreateOptions optionToUpsertInSilent() {
		return new DocumentCreateOptions().overwriteMode(OverwriteMode.replace).silent(true);
	}
	
	/**
	 * @return
	 */
	protected static final DocumentCreateOptions insertDocumentOverwriteModeUpdate() {
		return new DocumentCreateOptions().overwriteMode(OverwriteMode.update).returnNew(true);
	}
	
}