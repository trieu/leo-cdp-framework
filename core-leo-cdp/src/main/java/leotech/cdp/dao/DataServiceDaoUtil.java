package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.activation.DataService;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;
import rfx.core.util.StringUtil;

/**
 * Data Service DAO
 * 
 * @author tantrieuf31
 * @since 2023
 */
public final class DataServiceDaoUtil extends AbstractCdpDatabaseUtil {

	static final String AQL_FIND_KEY_AQL = ArangoDbUtil.contentFindKeyAql(DataService.COLLECTION_NAME);
	static final String AQL_GET_DATA_SERVICE_BY_ID = AqlTemplate.get("AQL_GET_DATA_SERVICE_BY_ID");
	static final String AQL_LIST_DATA_SERVICES = AqlTemplate.get("AQL_LIST_DATA_SERVICES");
	static final String AQL_LIST_DATA_SERVICES_WITH_FILTER = AqlTemplate.get("AQL_LIST_DATA_SERVICES_WITH_FILTER");
	static final String AQL_LIST_ALL_ACTIVE_DATA_SERVICES = AqlTemplate.get("AQL_LIST_ALL_ACTIVE_DATA_SERVICES");

	/**
	 * save a service
	 * 
	 * @param service
	 * @return the ID of service
	 */
	public static String save(DataService service, boolean overideOldData) {
		if (service.dataValidation()) {
			ArangoCollection col = service.getDbCollection();
			if (col != null) {
				String id = service.getId();
				service.setUpdatedAt(new Date());
				
				String _key = ArangoDbUtil.findKey(AQL_FIND_KEY_AQL, "id", id);
				if(overideOldData || _key == null) {
					col.insertDocument(service, optionToUpsertInSilent());
				}
				else {
					col.updateDocument(id, service, getMergeOptions());
				}
				return id;
			}
		}
		else {
			throw new IllegalArgumentException("service.dataValidation is false \n " + service.toString() );
		}
		return null;
	}

	/**
	 * @param id
	 * @return
	 */
	public static DataService getDataServiceById(String id) {
		DataService cat = null;
		if(StringUtil.isNotEmpty(id)) {
			ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("id", id);
			cat = new ArangoDbCommand<DataService>(db, AQL_GET_DATA_SERVICE_BY_ID, bindVars, DataService.class).getSingleResult();
		}
		return cat;
	}

	/**
	 * @param id
	 * @return
	 */
	public static String deleteDataServiceById(String id) throws ArangoDBException {
		ArangoCollection col = new DataService().getDbCollection();
		if (col != null) {
			col.deleteDocument(id);
			return id;
		}
		return "";
	}

	
	/**
	 * @return
	 */
	public static List<DataService> getAllActiveDataServices() {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(0);
		List<DataService> list = new ArangoDbCommand<DataService>(db, AQL_LIST_ALL_ACTIVE_DATA_SERVICES, bindVars, DataService.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param keywords
	 * @param onlyActive
	 * @return
	 */
	public static List<DataService> getDataServices(int startIndex, int numberResult, String keywords, String filterServiceValue, boolean forSynchronization, boolean forDataEnrichment, boolean forPersonalization) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(6);
		bindVars.put("keywords", "%"+keywords+"%");
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		String aql;
		if(DataService.ALL.equals(filterServiceValue) || StringUtil.isEmpty(filterServiceValue)) {
			aql = AQL_LIST_DATA_SERVICES;
		}
		else {
			aql = AQL_LIST_DATA_SERVICES_WITH_FILTER;
			bindVars.put("forSynchronization", forSynchronization);
			bindVars.put("forDataEnrichment", forDataEnrichment);
			bindVars.put("forPersonalization", forPersonalization);
		}
		
		System.out.println("keywords "+keywords);
		System.out.println("filterServiceValue "+filterServiceValue);
		System.out.println("forSynchronization "+forSynchronization);
		System.out.println("forDataEnrichment "+forDataEnrichment);
		System.out.println("forPersonalization "+forPersonalization);
		System.out.println(aql);
		List<DataService> list = new ArangoDbCommand<DataService>(db, aql, bindVars, DataService.class).getResultsAsList();
		return list;
	}

}
