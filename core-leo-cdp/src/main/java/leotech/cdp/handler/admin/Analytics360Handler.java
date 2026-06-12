package leotech.cdp.handler.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.Analytics360Management;
import leotech.cdp.domain.Analytics360Management.DashboardReportCacheKey;
import leotech.cdp.domain.FeedbackDataManagement;
import leotech.cdp.domain.NotebookManagement;
import leotech.cdp.domain.TouchpointHubManagement;
import leotech.cdp.domain.TouchpointManagement;
import leotech.cdp.model.analytics.CytoscapeData;
import leotech.cdp.model.analytics.DashboardEventReport;
import leotech.cdp.model.analytics.DashboardReport;
import leotech.cdp.model.analytics.EventMatrixReport;
import leotech.cdp.model.analytics.FeedbackRatingReport;
import leotech.cdp.model.analytics.FeedbackSurveyReport;
import leotech.cdp.model.analytics.JourneyProfileReport;
import leotech.cdp.model.analytics.Notebook;
import leotech.cdp.model.analytics.TouchpointFlowReport;
import leotech.cdp.model.analytics.TouchpointHubReport;
import leotech.cdp.model.analytics.TouchpointReport;
import leotech.cdp.model.customer.FeedbackType;
import leotech.cdp.model.journey.JourneyMap;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;

/**
 * @author tantrieuf31
 * @since 2020
 */
public final class Analytics360Handler extends SecuredHttpDataHandler {
    
    // for dashboard view
    static final String URI_DASHBOARD_PRIMARY = "/cdp/analytics360/dashboard-primary";
    static final String URI_PROFILE_TOTAL_STATS = "/cdp/analytics360/profile-total-stats";
    
    static final String URI_CX_REPORT = "/cdp/analytics360/cx-report";
    static final String URI_CX_SURVEY_REPORT = "/cdp/analytics360/cx-survey-report";
    static final String URI_CX_SURVEY_DATA_EXPORT = "/cdp/analytics360/cx-survey-data-export";
    
    static final String URI_EVENT_REPORT_SUMMARY  = "/cdp/analytics360/event-report/summary";
    static final String URI_EVENT_REPORT_PROFILE  = "/cdp/analytics360/event-report/profile";
    static final String URI_EVENT_REPORT_JOURNEY_PROFILE  = "/cdp/analytics360/event-report/journey-profile";
    
    static final String URI_TOUCHPOINT_REPORT  = "/cdp/analytics360/touchpoint-report";
    static final String URI_TOUCHPOINT_FLOW_REPORT  = "/cdp/analytics360/touchpoint-flow-report";
    static final String URI_TOUCHPOINT_HUB_REPORT  = "/cdp/analytics360/touchpoint-hub-report";
    static final String URI_JOURNEY_FLOW_REPORT  = "/cdp/analytics360/journey-flow-report";
    
    static final String URI_EVENT_IN_SEGMENT_REPORT  = "/cdp/analytics360/event-report/segment";
    static final String URI_EVENT_MATRIX_REPORT  = "/cdp/analytics360/event-matrix";
    
    // for dataList view
    static final String URI_LIST_NOTEBOOKS = "/cdp/analytics360/notebooks";
    
    // for dataList view
    static final String URI_CREATE_NEW = "/cdp/analytics360/notebook/new";
    static final String URI_UPDATE_NOTEBOOK = "/cdp/analytics360/notebook/update";
    static final String URI_GET_NOTEBOOK = "/cdp/analytics360/notebook/get";
    static final String URI_REMOVE_NOTEBOOK = "/cdp/analytics360/notebook/remove";
    static final String URI_RUN_NOTEBOOK_MANUALLY = "/cdp/analytics360/notebook/run";

    public Analytics360Handler(BaseHttpRouter baseHttpRouter) {
        super(baseHttpRouter);
    }

    @Override
    public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap) throws Exception {
        SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
        
        // Guard clauses to reduce nesting
        if (loginUser == null) {
            return JsonErrorPayload.NO_AUTHENTICATION;
        }
        if (!isAuthorized(loginUser, Notebook.class)) {
            return JsonErrorPayload.NO_AUTHORIZATION;
        }

        switch (uri) {
            case URI_LIST_NOTEBOOKS:
                return handlePostListNotebooks(uri, paramJson, loginUser);
            case URI_RUN_NOTEBOOK_MANUALLY:
            case URI_GET_NOTEBOOK:
                // TODO: Implement logic
                return JsonDataPayload.ok(uri, null, loginUser, Notebook.class);
            case URI_UPDATE_NOTEBOOK:
                // TODO: Implement logic
                String key = null; 
                return JsonDataPayload.ok(uri, key, loginUser, Notebook.class);
            case URI_REMOVE_NOTEBOOK:
                // the data is not deleted, we need to remove it from valid data view, set status of object = -4
                // TODO: Implement logic
                boolean rs = false;
                return JsonDataPayload.ok(uri, rs, loginUser, Notebook.class);
            default:
                return JsonErrorPayload.NO_HANDLER_FOUND;
        }
    }

    @Override
    public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
        SystemUser loginUser = initSystemUser(userSession, uri, params);
        
        // Guard clauses to reduce nesting
        if (loginUser == null) {
            return JsonErrorPayload.NO_AUTHENTICATION;
        }
        if (!isAuthorized(loginUser, Notebook.class)) {
            return JsonErrorPayload.NO_AUTHORIZATION;
        }

        switch (uri) {
            case URI_PROFILE_TOTAL_STATS:
                return JsonDataPayload.ok(uri, Analytics360Management.getProfileTotalStatistics());
            case URI_DASHBOARD_PRIMARY:
                return handleDashboardPrimary(uri, params, loginUser);
            case URI_EVENT_REPORT_SUMMARY:
                return handleEventReportSummary(uri, params, loginUser);
            case URI_EVENT_REPORT_PROFILE:
                return handleEventReportProfile(uri, params, loginUser);
            case URI_EVENT_REPORT_JOURNEY_PROFILE:
                return handleEventReportJourneyProfile(uri, params, loginUser);
            case URI_TOUCHPOINT_REPORT:
                return handleTouchpointReport(uri, params, loginUser);
            case URI_TOUCHPOINT_FLOW_REPORT:
                return handleTouchpointFlowReport(uri, params, loginUser);
            case URI_TOUCHPOINT_HUB_REPORT:
                return handleTouchpointHubReport(uri, params, loginUser);
            case URI_JOURNEY_FLOW_REPORT:
                return handleJourneyFlowReport(uri, params, loginUser);
            case URI_EVENT_IN_SEGMENT_REPORT:
                return handleEventInSegmentReport(uri, params, loginUser);
            case URI_CX_REPORT:
                return handleCxReport(uri, params, loginUser);
            case URI_CX_SURVEY_REPORT:
                return handleCxSurveyReport(uri, params, loginUser);
            case URI_CX_SURVEY_DATA_EXPORT:
                return handleCxSurveyDataExport(uri, params, loginUser);
            case URI_EVENT_MATRIX_REPORT:
                return handleEventMatrixReport(uri, params, loginUser);
            case URI_LIST_NOTEBOOKS:
                return handleGetListNotebooks(uri, params, loginUser);
            case URI_GET_NOTEBOOK:
                return handleGetNotebook(uri, params, loginUser);
            default:
                return JsonErrorPayload.NO_HANDLER_FOUND;
        }
    }

    // --- Extracted Private Methods for POST logic ---

    private JsonDataPayload handlePostListNotebooks(String uri, JsonObject paramJson, SystemUser loginUser) {
        int startIndex = HttpWebParamUtil.getInteger(paramJson, "startIndex", 0);
        int numberResult = HttpWebParamUtil.getInteger(paramJson, "numberResult", 10);
        List<Notebook> list = NotebookManagement.getNotebooks(startIndex, numberResult);
        return JsonDataPayload.ok(uri, list, loginUser, Notebook.class);
    }

    // --- Extracted Private Methods for GET logic ---

    private JsonDataPayload handleDashboardPrimary(String uri, MultiMap params, SystemUser loginUser) {
        String beginFilterDate = HttpWebParamUtil.getString(params, "beginFilterDate", "");
        String endFilterDate = HttpWebParamUtil.getString(params, "endFilterDate", "");
        String journeyMapId = HttpWebParamUtil.getString(params, "journeyMapId", "");
        String timeUnit = HttpWebParamUtil.getString(params, "timeUnit", Analytics360Management.MONTHS);
        
        if (!beginFilterDate.isBlank() && !endFilterDate.isBlank()) {
            DashboardReportCacheKey cacheKey = new DashboardReportCacheKey(journeyMapId, beginFilterDate, endFilterDate, timeUnit);
            DashboardReport dashboard = Analytics360Management.getDashboardReport(cacheKey);
            return JsonDataPayload.ok(uri, dashboard, loginUser, DashboardReport.class);
        }
        return JsonDataPayload.fail("beginFilterDate and endFilterDate must be valid dates", 500);
    }

    private JsonDataPayload handleEventReportSummary(String uri, MultiMap params, SystemUser loginUser) {
        String beginFilterDate = HttpWebParamUtil.getString(params, "beginFilterDate", "");
        String endFilterDate = HttpWebParamUtil.getString(params, "endFilterDate", "");
        String journeyMapId = HttpWebParamUtil.getString(params, "journeyMapId", "");
        
        if (!beginFilterDate.isBlank() && !endFilterDate.isBlank()) {
            DashboardReportCacheKey cacheKey = new DashboardReportCacheKey(journeyMapId, beginFilterDate, endFilterDate);
            DashboardEventReport report = Analytics360Management.getSummaryEventDataReport(cacheKey);
            return JsonDataPayload.ok(uri, report, loginUser, DashboardEventReport.class);
        }
        return JsonDataPayload.fail("beginFilterDate and endFilterDate must be valid dates", 500);
    }

    private JsonDataPayload handleEventReportProfile(String uri, MultiMap params, SystemUser loginUser) {
        String beginFilterDate = HttpWebParamUtil.getString(params, "beginFilterDate", "");
        String endFilterDate = HttpWebParamUtil.getString(params, "endFilterDate", "");
        String profileId = HttpWebParamUtil.getString(params, "profileId", "");
        String journeyMapId = HttpWebParamUtil.getString(params, "journeyMapId", "");
        
        if (!beginFilterDate.isBlank() && !endFilterDate.isBlank() && !profileId.isBlank()) {
            DashboardEventReport report = Analytics360Management.getDashboardEventReportForProfile(journeyMapId, profileId, beginFilterDate, endFilterDate);
            return JsonDataPayload.ok(uri, report, loginUser, DashboardEventReport.class);
        }
        return JsonDataPayload.fail("beginFilterDate, endFilterDate, and profileId must be valid", 500);
    }

    private JsonDataPayload handleEventReportJourneyProfile(String uri, MultiMap params, SystemUser loginUser) {
        String profileId = HttpWebParamUtil.getString(params, "profileId", "");
        String journeyMapId = HttpWebParamUtil.getString(params, "journeyMapId", "");
        
        if (!profileId.isBlank()) {
            JourneyProfileReport report = Analytics360Management.getJourneyEventStatisticsForProfile(profileId, journeyMapId);
            return JsonDataPayload.ok(uri, report, loginUser, DashboardEventReport.class);
        }
        return JsonDataPayload.fail("profileId must be a valid ID", 500);
    }

    private JsonDataPayload handleTouchpointReport(String uri, MultiMap params, SystemUser loginUser) {
        String beginDate = HttpWebParamUtil.getString(params, "beginFilterDate", "");
        String endDate = HttpWebParamUtil.getString(params, "endFilterDate", "");
        String journeyMapId = HttpWebParamUtil.getString(params, "journeyMapId", "");
        String profileId = HttpWebParamUtil.getString(params, "profileId", "");
        int touchpointType = HttpWebParamUtil.getInteger(params, "touchpointType", -1);
        int startIndex = HttpWebParamUtil.getInteger(params, "startIndex", 0);
        int numberResult = HttpWebParamUtil.getInteger(params, "numberResult", 20);
        
        List<TouchpointReport> reports;
        if (!beginDate.isBlank() && !endDate.isBlank()) {
            reports = TouchpointManagement.getTouchpointReport(journeyMapId, touchpointType, beginDate, endDate, startIndex, numberResult);
        } else if (!profileId.isBlank()) {
            reports = TouchpointManagement.getTouchpointReportForProfile(profileId, journeyMapId, touchpointType, startIndex, numberResult);
        } else {
            reports = new ArrayList<>(0);
        }
        return JsonDataPayload.ok(uri, reports, loginUser, TouchpointReport.class);
    }

    private JsonDataPayload handleTouchpointFlowReport(String uri, MultiMap params, SystemUser loginUser) {
        String beginFilterDate = HttpWebParamUtil.getString(params, "beginFilterDate", "");
        String endFilterDate = HttpWebParamUtil.getString(params, "endFilterDate", "");
        String journeyMapId = HttpWebParamUtil.getString(params, "journeyMapId", "");
        String profileId = HttpWebParamUtil.getString(params, "profileId", "");
        int startIndex = HttpWebParamUtil.getInteger(params, "startIndex", 0);
        int numberFlow = HttpWebParamUtil.getInteger(params, "numberFlow", 200); 
        
        List<TouchpointFlowReport> reports;
        if (!profileId.isBlank()) {
            reports = TouchpointManagement.getTouchpointFlowReportForProfile(profileId, journeyMapId, beginFilterDate, endFilterDate, startIndex, numberFlow);
        } else {
            reports = TouchpointManagement.getTouchpointFlowReportForJourney(journeyMapId, beginFilterDate, endFilterDate, startIndex, numberFlow);
        }
        CytoscapeData data = new CytoscapeData(journeyMapId, reports, true);
        return JsonDataPayload.ok(uri, data, loginUser, CytoscapeData.class);
    }

    private JsonDataPayload handleTouchpointHubReport(String uri, MultiMap params, SystemUser loginUser) {
        String beginDate = HttpWebParamUtil.getString(params, "beginFilterDate", "");
        String endDate = HttpWebParamUtil.getString(params, "endFilterDate", "");
        String journeyMapId = HttpWebParamUtil.getString(params, "journeyMapId", "");
        String profileId = HttpWebParamUtil.getString(params, "profileId", "");
        
        List<TouchpointHubReport> reports;
        if (!profileId.isBlank()) {
            reports = TouchpointHubManagement.getTouchpointHubReportForProfile(profileId, journeyMapId);
        } else {
            int maxTouchpointHubSize = HttpWebParamUtil.getInteger(params, "maxTouchpointHubSize", 100);
            reports = TouchpointHubManagement.getTouchpointHubReport(journeyMapId, beginDate, endDate, maxTouchpointHubSize);
        }
        return JsonDataPayload.ok(uri, reports, loginUser, TouchpointHubReport.class);
    }

    private JsonDataPayload handleJourneyFlowReport(String uri, MultiMap params, SystemUser loginUser) {
        String profileId = HttpWebParamUtil.getString(params, "profileId", "");
        String journeyMapId = HttpWebParamUtil.getString(params, "journeyMapId", "");
        
        if (!profileId.isBlank() && !journeyMapId.isBlank()) {
            JourneyMap journeyReport = Analytics360Management.getJourneyMapReportForProfile(profileId, journeyMapId);
            return JsonDataPayload.ok(uri, journeyReport, loginUser, JourneyMap.class);
        }
        return JsonDataPayload.fail("profileId and journeyMapId must be valid values", 500);
    }

    private JsonDataPayload handleEventInSegmentReport(String uri, MultiMap params, SystemUser loginUser) {
        String beginFilterDate = HttpWebParamUtil.getString(params, "beginFilterDate", "");
        String endFilterDate = HttpWebParamUtil.getString(params, "endFilterDate", "");
        String segmentId = HttpWebParamUtil.getString(params, "segmentId", "");
        
        if (!beginFilterDate.isBlank() && !endFilterDate.isBlank() && !segmentId.isBlank()) {
            DashboardEventReport report = Analytics360Management.getDashboardEventReportForSegment(segmentId, beginFilterDate, endFilterDate);
            return JsonDataPayload.ok(uri, report, loginUser, DashboardEventReport.class);
        }
        return JsonDataPayload.fail("beginFilterDate, endFilterDate, and segmentId must be valid", 500);
    }

    private JsonDataPayload handleCxReport(String uri, MultiMap params, SystemUser loginUser) {
        String beginFilterDate = HttpWebParamUtil.getString(params, "beginFilterDate", "");
        String endFilterDate = HttpWebParamUtil.getString(params, "endFilterDate", "");
        int feedbackDataTypeInt = HttpWebParamUtil.getInteger(params, "feedbackDataType", -1);
        String feedbackDataType = FeedbackType.getFeedbackTypeAsText(feedbackDataTypeInt);
        
        if (!beginFilterDate.isBlank() && !endFilterDate.isBlank() && !feedbackDataType.isBlank()) {
            FeedbackRatingReport report = FeedbackDataManagement.getFeedbackReport(feedbackDataType, beginFilterDate, endFilterDate);
            return JsonDataPayload.ok(uri, report, loginUser, FeedbackRatingReport.class);
        }
        return JsonDataPayload.fail("beginFilterDate and endFilterDate must be valid dates", 500);
    }

    private JsonDataPayload handleCxSurveyReport(String uri, MultiMap params, SystemUser loginUser) {
        if (!loginUser.canViewSurveyReport()) {
            return JsonErrorPayload.NO_AUTHORIZATION;
        }
        
        SurveyParams sp = getSurveyParams(params);
        if (!sp.beginFilterDate.isBlank() && !sp.endFilterDate.isBlank() && !sp.refTemplateId.isBlank()) {
            List<FeedbackSurveyReport> fbReports = FeedbackDataManagement.getSurveyFeedbackReport(
                sp.refTemplateId, sp.beginFilterDate, sp.endFilterDate, 
                sp.includeGroup, sp.includeObject, sp.includeItem, sp.includePerson, sp.rankingByScore);
            return JsonDataPayload.ok(uri, fbReports, loginUser, FeedbackSurveyReport.class);
        }
        return JsonDataPayload.fail("beginFilterDate, endFilterDate, and refTemplateId must be valid", 500);
    }

    private JsonDataPayload handleCxSurveyDataExport(String uri, MultiMap params, SystemUser loginUser) {
        if (!loginUser.canViewSurveyReport()) {
            return JsonErrorPayload.NO_AUTHORIZATION;
        }

        SurveyParams sp = getSurveyParams(params);
        Map<String, String> results = null;
        if (!sp.beginFilterDate.isBlank() && !sp.endFilterDate.isBlank() && !sp.refTemplateId.isBlank()) {
            results = FeedbackDataManagement.exportSurveyFeedbackData(
                sp.refTemplateId, sp.beginFilterDate, sp.endFilterDate, 
                sp.includeGroup, sp.includeObject, sp.includeItem, sp.includePerson, sp.rankingByScore, loginUser.getKey());
        }
        return JsonDataPayload.ok(uri, results, loginUser, FeedbackSurveyReport.class);
    }

    private JsonDataPayload handleEventMatrixReport(String uri, MultiMap params, SystemUser loginUser) {
        String journeyMapId = HttpWebParamUtil.getString(params, "journeyMapId", "");
        String profileId = HttpWebParamUtil.getString(params, "profileId", "");
        String beginFilterDate = HttpWebParamUtil.getString(params, "beginFilterDate", "");
        String endFilterDate = HttpWebParamUtil.getString(params, "endFilterDate", "");
        
        Map<String, Object> model = Analytics360Management.getEventMatrixReportModel(journeyMapId, profileId, beginFilterDate, endFilterDate);
        return JsonDataPayload.ok(uri, model, loginUser, EventMatrixReport.class);
    }

    private JsonDataPayload handleGetListNotebooks(String uri, MultiMap params, SystemUser loginUser) {
        int startIndex = HttpWebParamUtil.getInteger(params, "startIndex", 0);
        int numberResult = HttpWebParamUtil.getInteger(params, "numberResult", 10);
        
        List<Notebook> list = NotebookManagement.getNotebooks(startIndex, numberResult);
        return JsonDataPayload.ok(uri, list, loginUser, Notebook.class);
    }

    private JsonDataPayload handleGetNotebook(String uri, MultiMap params, SystemUser loginUser) {
        String id = HttpWebParamUtil.getString(params, "id", "");
        if (!id.isBlank()) {
            // TODO: Implement logic
            return JsonDataPayload.ok(uri, null, loginUser, Notebook.class);
        }
        return JsonDataPayload.fail("Invalid ID", 500); // Handled empty state gracefully
    }

    // --- Helper Class / Method for common Survey Parameters ---
    
    private SurveyParams getSurveyParams(MultiMap params) {
        SurveyParams sp = new SurveyParams();
        sp.beginFilterDate = HttpWebParamUtil.getString(params, "beginFilterDate", "");
        sp.endFilterDate = HttpWebParamUtil.getString(params, "endFilterDate", "");
        sp.refTemplateId = HttpWebParamUtil.getString(params, "refTemplateId", "");
        sp.includeGroup = HttpWebParamUtil.getBoolean(params, "includeGroup", true);
        sp.includeObject = HttpWebParamUtil.getBoolean(params, "includeObject", true);
        sp.includeItem = HttpWebParamUtil.getBoolean(params, "includeItem", true);
        sp.includePerson = HttpWebParamUtil.getBoolean(params, "includePerson", true);
        sp.rankingByScore = HttpWebParamUtil.getBoolean(params, "rankingByScore", false);
        return sp;
    }

    private static class SurveyParams {
        String beginFilterDate;
        String endFilterDate;
        String refTemplateId;
        boolean includeGroup;
        boolean includeObject;
        boolean includeItem;
        boolean includePerson;
        boolean rankingByScore;
    }
}