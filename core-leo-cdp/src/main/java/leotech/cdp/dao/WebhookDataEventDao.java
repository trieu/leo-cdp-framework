package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.analytics.WebhookDataEvent;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;

/**
 * WebhookDataEvent Database Access
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class WebhookDataEventDao extends AbstractCdpDatabaseUtil {
	
	static final String AQL_GET_WEBHOOK_EVENTS_BY_SOURCE = AqlTemplate.get("AQL_GET_WEBHOOK_EVENTS_BY_SOURCE");

	/**
	 * @param e
	 * @return id of WebhookDataEvent
	 */
	public static String save(WebhookDataEvent e) {
		if (e.dataValidation()) {
			ArangoCollection col = e.getDbCollection();
			String id = e.getId();
			
			if (id != null && col != null) {
				e.setUpdatedAt(new Date());
				col.insertDocument(e, optionToUpsertInSilent());
				return id;
			}
		}
		return null;
	}
	
	/**
	 * @param source
	 * @return
	 */
	public static List<WebhookDataEvent> listAllBySource(String source) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("source", source);
		List<WebhookDataEvent> list = new ArangoDbCommand<WebhookDataEvent>(db, AQL_GET_WEBHOOK_EVENTS_BY_SOURCE, bindVars, WebhookDataEvent.class).getResultsAsList();
		return list;
	}
}
