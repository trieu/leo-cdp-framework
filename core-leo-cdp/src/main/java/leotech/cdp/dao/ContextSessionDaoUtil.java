package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.analytics.ContextSession;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.version.SystemMetaData;

/**
 * @author tantrieuf31
 *
 */
public final class ContextSessionDaoUtil extends AbstractCdpDatabaseUtil {

	static final String AQL_GET_CONTEXT_SESSIONS_BY_VISITOR_ID = AqlTemplate.get("AQL_GET_CONTEXT_SESSIONS_BY_VISITOR_ID");
	static final String AQL_GET_CONTEXT_SESSIONS_BY_PROFILE_ID = AqlTemplate.get("AQL_GET_CONTEXT_SESSIONS_BY_PROFILE_ID");
	static final String AQL_GET_CONTEXT_SESSION_BY_KEY = AqlTemplate.get("AQL_GET_CONTEXT_SESSION_BY_KEY");
	static final String AQL_UPDATE_CONTEXT_SESSION_AFTER_MERGE = AqlTemplate.get("AQL_UPDATE_CONTEXT_SESSION_AFTER_MERGE");
	
	static final String AQL_FIND_KEY_AQL = ArangoDbUtil.contentFindKeyAql(ContextSession.COLLECTION_NAME);
	
	static ExecutorService executorService = Executors.newFixedThreadPool(SystemMetaData.NUMBER_CORE_CPU);

	/**
	 * @param s
	 * @return
	 */
	public static ContextSession create(ContextSession s) {
		if (s.dataValidation()) {
			executorService.execute(() -> {
				ArangoCollection col = s.getDbCollection();
				if (col != null) {
					try {
						col.insertDocument(s, optionToUpsertInSilent());
					} catch (ArangoDBException e) {
						e.printStackTrace();
					}
				}
			});
			return s;
		}
		return null;
	}
	
	/**
	 * @param s
	 * @return
	 */
	public static ContextSession update(ContextSession s) {
		if (s.dataValidation()) {
			ArangoCollection col = s.getDbCollection();
			if (col != null) {
				executorService.execute(() -> {
					try {
						s.setUpdatedAt(new Date());
						col.updateDocument(s.getSessionKey(), s, getUpdateOptions());
					} catch (ArangoDBException e) {
						e.printStackTrace();
					}
				});
				return s;
			}
		}
		return null;
	}
	
	/**
	 * @param oldProfileId
	 * @param newProfileId
	 * @param newVisitorId
	 * @return
	 */
	public static void updateContextSessionAfterMerge(String oldProfileId, String newProfileId, String newVisitorId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("oldProfileId", oldProfileId);
		bindVars.put("newProfileId", newProfileId);
		bindVars.put("newVisitorId", newVisitorId);
		new ArangoDbCommand<ContextSession>(db, AQL_UPDATE_CONTEXT_SESSION_AFTER_MERGE, bindVars, ContextSession.class).update();
	}

	public static ContextSession getByKey(String sessionKey) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("sessionKey", sessionKey);
		ContextSession s = new ArangoDbCommand<ContextSession>(db, AQL_GET_CONTEXT_SESSION_BY_KEY, bindVars,
				ContextSession.class).getSingleResult();
		return s;
	}

	public static List<ContextSession> getSessionsByVisitorId(String visitorId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("visitorId", visitorId);
		List<ContextSession> list = new ArangoDbCommand<ContextSession>(db, AQL_GET_CONTEXT_SESSIONS_BY_VISITOR_ID,
				bindVars, ContextSession.class).getResultsAsList();
		return list;
	}

	public static List<ContextSession> getSessionsByProfileId(String profileId) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("profileId", profileId);
		List<ContextSession> list = new ArangoDbCommand<ContextSession>(db, AQL_GET_CONTEXT_SESSIONS_BY_PROFILE_ID, bindVars, ContextSession.class).getResultsAsList();
		return list;
	}

}
