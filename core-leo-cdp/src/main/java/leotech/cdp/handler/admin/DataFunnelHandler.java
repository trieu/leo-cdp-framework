package leotech.cdp.handler.admin;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.DataFlowManagement;
import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.domain.schema.JourneyFlowSchema;
import leotech.cdp.model.journey.DataFlowStage;
import leotech.cdp.model.journey.EventMetric;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;

/**
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class DataFunnelHandler extends SecuredHttpDataHandler {
	
	// for dataList view

	static final String URI_GET_METADATA_FUNNEL = "/cdp/funnel/metadata";
	static final String URI_EVENT_METRICS = "/cdp/funnel/event-metrics";
	
	static final String URI_EVENT_METRIC_SAVE = "/cdp/funnel/event-metric/save";
	static final String URI_EVENT_METRIC_DELETE = "/cdp/funnel/event-metric/delete";
	
	public DataFunnelHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}

	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap) throws Exception {
		System.out.println("DataFunnelHandler uri " + uri);
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			if (isAuthorized(loginUser, DataFlowStage.class)) {
				switch (uri) {
					
					case URI_EVENT_METRIC_SAVE : {
						String eventMetricJson = paramJson.getString("eventMetricJson", ""); 
						EventMetric eventMetric = new Gson().fromJson(eventMetricJson, EventMetric.class);
						String id = EventMetricManagement.save(eventMetric);
						return JsonDataPayload.ok(uri, id, loginUser, EventMetric.class);
					}
					
					case URI_EVENT_METRIC_DELETE : {
						String eventName = paramJson.getString("eventName", ""); 
						
						String id = EventMetricManagement.delete(eventName);
						return JsonDataPayload.ok(uri, id, loginUser, EventMetric.class);
					}
				
					default : {
						return JsonErrorPayload.NO_HANDLER_FOUND;
					}
				}
			}
			return JsonErrorPayload.NO_AUTHORIZATION;

		} else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			if (isAuthorized(loginUser, DataFlowStage.class)) {
				switch (uri) {
					case URI_GET_METADATA_FUNNEL : {
						
						Map<String, Object> map = new HashMap<>(2);
							
						List<DataFlowStage> funnelFlow = DataFlowManagement.getCustomerFunnelStages();
						map.put(JourneyFlowSchema.GENERAL_DATA_FUNNEL, funnelFlow);
						
						// customer flow event metrics
						List<EventMetric> metrics = EventMetricManagement.getAllSortedEventMetrics(true);
						map.put(JourneyFlowSchema.BEHAVIORAL_METRICS, metrics);
						
						return JsonDataPayload.ok(uri, map, loginUser, DataFlowStage.class);
					}
					
					case URI_EVENT_METRICS : {
						Collection<EventMetric> metrics = EventMetricManagement.getAllSortedEventMetrics(true);
						return JsonDataPayload.ok(uri, metrics, loginUser, DataFlowStage.class);
					}

					default :
						return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			} else {
				return JsonErrorPayload.NO_AUTHORIZATION;
			}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}

}
