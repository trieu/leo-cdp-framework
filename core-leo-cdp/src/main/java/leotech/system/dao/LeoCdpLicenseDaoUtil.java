package leotech.system.dao;

import java.util.HashMap;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.system.model.LeoCdpLicense;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;

/**
 * LeoCdpLicense DAO
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class LeoCdpLicenseDaoUtil extends AbstractCdpDatabaseUtil {

	static final String AQL_GET_BY_EMAIL = "FOR d in system_leocdp_license FILTER d.adminEmail == @adminEmail RETURN d";
	static final String AQL_GET_BY_DOMAIN = "FOR d in system_leocdp_license FILTER d.adminDomain == @adminDomain RETURN d";
	
	
	public static boolean save(LeoCdpLicense d) {
		if (d.dataValidation()) {
			ArangoCollection col = d.getDbCollection();
			String id = d.getId();
			if (col != null) {
				ArangoDatabase db = getCdpDatabase();
				boolean isExisted = ArangoDbUtil.isExistedDocument(db, LeoCdpLicense.COLLECTION_NAME, id);
				if(!isExisted) {
					col.insertDocument(d);
				} else {
					col.updateDocument(id, d, getUpdateOptions());
				}
				return true;
			}
		} else {
			System.err.println("LeoCdpLicense.dataValidation is failed");
		}
		return false;
	}

	public static LeoCdpLicense getByEmail(String adminEmail) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("adminEmail", adminEmail);
		LeoCdpLicense p = new ArangoDbCommand<LeoCdpLicense>(db, AQL_GET_BY_EMAIL, bindVars, LeoCdpLicense.class).getSingleResult();
		return p;
	}
	
	public static LeoCdpLicense getByDomain(String adminDomain) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("adminDomain", adminDomain);
		LeoCdpLicense p = new ArangoDbCommand<LeoCdpLicense>(db, AQL_GET_BY_DOMAIN, bindVars, LeoCdpLicense.class).getSingleResult();
		return p;
	}

}
