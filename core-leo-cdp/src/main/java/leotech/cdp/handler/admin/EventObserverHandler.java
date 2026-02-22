package leotech.cdp.handler.admin;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;

import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.TouchpointType;

import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;

/**
 * Event Observer Admin Handler
 */
public final class EventObserverHandler extends SecuredHttpDataHandler {

    /* ===========================
     * Constants
     * =========================== */

    static final String FILTER_KEYWORDS = "filterKeywords";
    static final String JOURNEY_MAP_ID = "journeyMapId";

    static final String API_LIST = "/cdp/observers";
    static final String API_CREATE_NEW = "/cdp/observer/new";
    static final String API_UPDATE_MODEL = "/cdp/observer/update";
    static final String API_GET_MODEL = "/cdp/observer/get";
    static final String API_REMOVE = "/cdp/observer/remove";

    public EventObserverHandler(BaseHttpRouter baseHttpRouter) {
        super(baseHttpRouter);
    }

    /* =========================================================
     * POST HANDLER
     * ========================================================= */

    @Override
    public JsonDataPayload httpPostHandler(
            String userSession,
            String uri,
            JsonObject paramJson,
            Map<String, Cookie> cookieMap) throws Exception {

        SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
        if (loginUser == null) {
            return JsonErrorPayload.NO_AUTHENTICATION;
        }

        if (!isAuthorized(loginUser, EventObserver.class)) {
            return JsonErrorPayload.NO_AUTHORIZATION;
        }

        switch (uri) {

            case API_UPDATE_MODEL:
                return handleUpdate(uri, paramJson, loginUser);

            case API_REMOVE:
                return handleRemove(uri, paramJson, loginUser);

            default:
                return JsonErrorPayload.NO_HANDLER_FOUND;
        }
    }

    /* =========================================================
     * GET HANDLER
     * ========================================================= */

    @Override
    public JsonDataPayload httpGetHandler(
            String userSession,
            String uri,
            MultiMap params,
            Map<String, Cookie> cookieMap) throws Exception {

        SystemUser loginUser = initSystemUser(userSession, uri, params);
        if (loginUser == null) {
            return JsonErrorPayload.NO_AUTHENTICATION;
        }

        if (!isAuthorized(loginUser, EventObserver.class)) {
            return JsonErrorPayload.NO_AUTHORIZATION;
        }

        switch (uri) {

            case API_LIST:
                return handleList(uri, params, loginUser);

            case API_GET_MODEL:
                return handleGetModel(uri, params, loginUser);

            default:
                return JsonErrorPayload.NO_HANDLER_FOUND;
        }
    }

    /* =========================================================
     * HANDLER IMPLEMENTATIONS
     * ========================================================= */

    private JsonDataPayload handleList(
            String uri,
            MultiMap params,
            SystemUser loginUser) {

        String journeyMapId =
                HttpWebParamUtil.getString(params, JOURNEY_MAP_ID, "");

        String filterKeywords =
                HttpWebParamUtil.getString(params, FILTER_KEYWORDS, "");

        List<EventObserver> observers =
                EventObserverManagement.listAllByJourneyMap(
                        journeyMapId,
                        filterKeywords);

        @SuppressWarnings("null")
        Map<String, Object> data = ImmutableMap.of(
                "eventObservers", (Object) observers,
                "touchpointHubTypes", TouchpointType.getMapValueToName()
        );

        return JsonDataPayload.ok(uri, data, loginUser, EventObserver.class);
    }

    private JsonDataPayload handleGetModel(
            String uri,
            MultiMap params,
            SystemUser loginUser) {

        String id = HttpWebParamUtil.getString(params, "id", "");

        if (id.isEmpty()) {
            return JsonErrorPayload.NO_HANDLER_FOUND;
        }

        EventObserver observer =
                EventObserverManagement.getById(id);

        return JsonDataPayload.ok(uri, observer, loginUser, EventObserver.class);
    }

    private JsonDataPayload handleUpdate(
            String uri,
            JsonObject paramJson,
            SystemUser loginUser) {

        // TODO implement update logic
        String key = null;

        return JsonDataPayload.ok(uri, key, loginUser, EventObserver.class);
    }

    private JsonDataPayload handleRemove(
            String uri,
            JsonObject paramJson,
            SystemUser loginUser) {

        // Soft delete (status = -4)
        // TODO implement remove logic
        boolean result = false;

        return JsonDataPayload.ok(uri, result, loginUser, EventObserver.class);
    }
}