package leotech.system.util.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;

import leotech.system.exception.InvalidSystemException;
import leotech.system.util.LogUtil;
import rfx.core.util.StringUtil;

/**
 * utility class For Database query data with ArangoDB
 * 
 * @author tantrieuf31
 * @since 2019
 *
 * @param <T>
 */
public class ArangoDbCommand<T> {

	public static abstract class CallbackQuery<T> {
		abstract public T apply(T obj);
	}

	ArangoDatabase arangoDb;
	String aql;
	Map<String, Object> bindVars;
	Class<T> type;
	CallbackQuery<T> callback = null;

	public ArangoDbCommand(ArangoDatabase db, String aql, Class<T> type) {
		setRequiredData(db, aql, new HashMap<>(0), type);
	}

	public ArangoDbCommand(ArangoDatabase db, String aql, Map<String, Object> bindVars) {
		setRequiredData(db, aql, bindVars, null);
	}

	public ArangoDbCommand(ArangoDatabase db, String aql, Class<T> type, CallbackQuery<T> callback) {
		setRequiredData(db, aql, new HashMap<>(0), type);
		this.callback = callback;
	}

	public ArangoDbCommand(ArangoDatabase db, String aql, Map<String, Object> bindVars, Class<T> type) {
		setRequiredData(db, aql, bindVars, type);
	}

	public ArangoDbCommand(ArangoDatabase db, String aql, Map<String, Object> bindVars, Class<T> type,
			CallbackQuery<T> callback) {
		setRequiredData(db, aql, bindVars, type);
		this.callback = callback;
	}

	
	/**
	 * @param db
	 * @param aql
	 * @param bindVars
	 * @param type
	 */
	private void setRequiredData(ArangoDatabase db, String aql, Map<String, Object> bindVars, Class<T> type) {
		this.arangoDb = db;
		this.aql = aql;
		this.bindVars = bindVars;
		this.type = type;
	}
	


	/**
	 * @return
	 */
	public List<T> getResultsAsList() {
		ArangoCursor<T> cursor = null;
		List<T> list = new ArrayList<>();
		try {
			cursor = arangoDb.query(aql, bindVars, null, type);
			while (cursor.hasNext()) {
				T obj = cursor.next();
				if (callback != null && obj != null) {
					T appliedObj = callback.apply(obj);
					if (appliedObj != null) {
						list.add(appliedObj);
					}
				} else {
					list.add(obj);
				}
			}
		} catch (Exception e) {
			System.out.println("DEBUG ==> {AQL} "+aql);
			handleException(e);
		} finally {
			closeArangoCursor(cursor);
		}
		return list;
	}
	
	void handleException(Exception e) {
		// TODO improve error handler
		e.printStackTrace();
		LogUtil.logError(e.getClass(), e.toString());
		
		if(e instanceof java.net.ConnectException) {
			String err = "Can not connect to ArangoDb at " + ArangoDbUtil.getDatabaseConnectionUrl();
			throw new InvalidSystemException(err);
		}
		else if(e instanceof ArangoDBException) {
			ArangoDBException ea = (ArangoDBException)e;
			int errorNum = StringUtil.safeParseInt(ea.getErrorNum());
			if (errorNum == 1200) {
				System.err.println(ea.getErrorMessage());
			}
			if (InitDatabaseSchema.isSystemDbReady()) {
				ea.printStackTrace();
			}
		}
	}

	void closeArangoCursor(ArangoCursor<T> cursor) {
		if (cursor != null) {
			try {
				cursor.close();
			} 
			catch (Exception e) {}
		}
	}

	/**
	 * 
	 */
	public void applyCallback() {
		ArangoCursor<T> cursor = null;
		try {
			cursor = arangoDb.query(aql, bindVars, null, type);
			while (cursor.hasNext()) {
				T obj = cursor.next();
				if (callback != null && obj != null) {
					callback.apply(obj);
				}
			}
		} catch (Exception e) {
			System.out.println("DEBUG ==> {AQL} "+aql);
			handleException(e);
		} finally {
			closeArangoCursor(cursor);
		}
	}

	/**
	 * @return
	 */
	public Set<T> getResultsAsSet() {
		ArangoCursor<T> cursor = null;
		Set<T> set = new HashSet<T>();
		try {
			cursor = arangoDb.query(aql, bindVars, null, type);
			while (cursor.hasNext()) {
				T obj = cursor.next();
				if (callback != null && obj != null) {
					T appliedObj = callback.apply(obj);
					if (appliedObj != null) {
						set.add(appliedObj);
					}
				} else {
					set.add(obj);
				}
			}
		} catch (Exception e) {
			System.out.println("DEBUG ==> {AQL} "+aql);
			handleException(e);
		} finally {
			closeArangoCursor(cursor);
		}
		return set;
	}

	/**
	 * get a single result
	 * 
	 * @return
	 */
	public T getSingleResult() {
	    ArangoCursor<T> cursor = null;
	    T obj = null;

	    try {
	        cursor = arangoDb.query(aql, bindVars, null, type);
	        if (cursor != null && cursor.hasNext()) {
	            // Filter out null values to avoid Optional.of(null)
	            Optional<T> findFirst = cursor.stream()
	                                          .filter(Objects::nonNull)
	                                          .findFirst();
	            if (callback != null && findFirst.isPresent()) {
	                obj = callback.apply(findFirst.get());
	            } else if (findFirst.isPresent()) {
	                obj = findFirst.get();
	            }
	        }
	    } catch (Exception e) {
	        System.out.println("DEBUG ==> {AQL} " + aql);
	        handleException(e);
	    } finally {
	        closeArangoCursor(cursor);
	    }
	    return obj;
	}


	public void update() {
		try {
			arangoDb.query(aql, bindVars, type);
		} catch (Exception e) {
			System.out.println("DEBUG ==> {AQL} "+aql);
			System.out.println(" bindVars " + bindVars);
			handleException(e);
		} 
	}
	
	/**
	 * 
	 * @return the _key of removed document
	 */
	public String remove() {
		ArangoCursor<String> cursor = null;
		try {
			cursor = arangoDb.query(aql, bindVars, String.class);
			if (cursor.hasNext()) {
				String key = cursor.stream().findFirst().get();
				return key;
			}
		} catch (Exception e) {
			handleException(e);
		} finally {
			if (cursor != null) {
				try {
					cursor.close();
				} 
				catch (Exception e) {}
			}
		}
		return null;
	}

	/**
	 * @param AqlQueryOptions options
	 */
	public void update(AqlQueryOptions options) {
		try {
			arangoDb.query(aql, bindVars, options, type);
		} catch (Exception e) {
			handleException(e);
		} 
	}

}
