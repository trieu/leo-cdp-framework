package leotech.system.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.system.config.AqlTemplate;
import leotech.system.model.SystemService;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;

/**
 * System Configuration DAO
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class SystemServiceDaoUtil extends AbstractCdpDatabaseUtil {

	static final String AQL_GET_SYSTEM_SERVICE_BY_ID = AqlTemplate.get("AQL_GET_SYSTEM_SERVICE_BY_ID");
	static final String AQL_GET_PUBLIC_SYSTEM_SERVICES = AqlTemplate.get("AQL_GET_PUBLIC_SYSTEM_SERVICES");
	static final String AQL_GET_ALL_SYSTEM_SERVICES = AqlTemplate.get("AQL_GET_ALL_SYSTEM_SERVICES");
	static final String AQL_REMOVE_ALL_SYSTEM_SERVICES = AqlTemplate.get("AQL_REMOVE_ALL_SYSTEM_SERVICES");

	/**
	 * @param id
	 * @return
	 */
	public static SystemService getById(String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		SystemService config = new ArangoDbCommand<SystemService>(db, AQL_GET_SYSTEM_SERVICE_BY_ID, bindVars, SystemService.class).getSingleResult();
		return config;
	}

	/**
	 * @return
	 */
	public static List<SystemService> getPublicSystemConfigs() {
		ArangoDatabase db = getCdpDatabase();
		String aql = AQL_GET_PUBLIC_SYSTEM_SERVICES;
		List<SystemService> list = new ArangoDbCommand<SystemService>(db, aql, SystemService.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @return
	 */
	public static List<SystemService> getAllSystemConfigs() {
		ArangoDatabase db = getCdpDatabase();
		List<SystemService> list = new ArangoDbCommand<SystemService>(db, AQL_GET_ALL_SYSTEM_SERVICES, SystemService.class)
				.getResultsAsList();
		return list;
	}

	/**
	 * @param config
	 * @return
	 */
	public static String save(SystemService config) {
		if (config.dataValidation()) {
			ArangoCollection col = config.getDbCollection();
			if (col != null) {
				String id = config.getId();
				System.out.println(config);
				boolean isExisted = ArangoDbUtil.isExistedDocument(SystemService.COLLECTION_NAME, id);
				if (isExisted) {
					config.setUpdatedAt(new Date());
					col.updateDocument(id, config, getUpdateOptions());
				} else {
					col.insertDocument(config, optionToUpsertInSilent());
				}
				return id;
			}
		}
		return null;
	}
	
	public static String insert(SystemService config) {
		if (config.dataValidation()) {
			ArangoCollection col = config.getDbCollection();
			if (col != null) {
				String id = config.getId();
				boolean isExisted = ArangoDbUtil.isExistedDocument(SystemService.COLLECTION_NAME, id);
				System.out.println(isExisted + " SystemConfig " +config);
				if (! isExisted) {
					col.insertDocument(config);
				} 
				return id;
			}
		}
		return null;
	}
	
	public static void removeAllSystemConfigs() {
		ArangoDatabase db = getCdpDatabase();
		new ArangoDbCommand<SystemService>(db, AQL_REMOVE_ALL_SYSTEM_SERVICES, SystemService.class).update();
	}

	public static void main(String[] args) {
	//	SystemConfigsManagement.initDefaultSystemData(true);
		System.out.println(getPublicSystemConfigs());
	}

}
