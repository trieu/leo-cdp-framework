package leotech.system.util.database;

import com.arangodb.ArangoCollection;

/**
 * 
 * the interface for data persistent model
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public interface PersistentArangoObject {


	/**
	 * @return ArangoCollection, the database collection for storing
	 */
	public ArangoCollection getDbCollection();


	/**
	 * @return check true or false for data validation
	 */
	public boolean dataValidation();

}
