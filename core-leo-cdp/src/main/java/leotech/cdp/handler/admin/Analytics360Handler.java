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
import leotech.cdp.model.analytics.StatisticCollector;
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
 *
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
		if (loginUser != null) {
			if (isAuthorized(loginUser, Notebook.class)) {
				switch (uri) {
					case URI_LIST_NOTEBOOKS : {
						int startIndex =   HttpWebParamUtil.getInteger(paramJson,"startIndex", 0);
						int numberResult = HttpWebParamUtil.getInteger(paramJson,"numberResult", 10);
						List<Notebook> list = NotebookManagement.getNotebooks(startIndex, numberResult);
						return JsonDataPayload.ok(uri, list, loginUser, Notebook.class);
					}
					case URI_RUN_NOTEBOOK_MANUALLY : {
						
						String id = paramJson.getString("id", "0");
						//TODO
						return JsonDataPayload.ok(uri, null, loginUser, Notebook.class);
					}
					case URI_GET_NOTEBOOK : {
						String id = paramJson.getString("id", "0");
						//TODO
						return JsonDataPayload.ok(uri, null, loginUser, Notebook.class);
					}
					case URI_UPDATE_NOTEBOOK : {
						String key = null;
						//TODO                                                                                                                                                                                                                                                                
						return JsonDataPayload.ok(uri, key, loginUser, Notebook.class);
					}
					case URI_REMOVE_NOTEBOOK : {
						// the data is not deleted, we need to remove it from valid data view, set status of object = -4
						//TODO
						boolean rs = false;
						return JsonDataPayload.ok(uri, rs, loginUser, Notebook.class);
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
			if (isAuthorized(loginUser, Notebook.class)) {
				switch (uri) {
					
					case URI_PROFILE_TOTAL_STATS : {
						List<StatisticCollector> stats = Analytics360Management.getProfileTotalStatistics();
						return JsonDataPayload.ok(uri, stats);
					}
					
					case URI_DASHBOARD_PRIMARY : {
						String beginFilterDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
						String endFilterDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
						String journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", "");
						String timeUnit = HttpWebParamUtil.getString(params,"timeUnit", Analytics360Management.MONTHS);
						if ( !beginFilterDate.isEmpty() && !endFilterDate.isEmpty() ) {
							DashboardReportCacheKey cacheKey = new DashboardReportCacheKey(journeyMapId, beginFilterDate, endFilterDate, timeUnit);
							DashboardReport dashboard = Analytics360Management.getDashboardReport(cacheKey);
							return JsonDataPayload.ok(uri, dashboard, loginUser, DashboardReport.class);
						} else {
							return JsonDataPayload.fail("beginFilterDate and endFilterDate must be a valid date", 500);
						}
					}
					
					case URI_EVENT_REPORT_SUMMARY : {
						String beginFilterDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
						String endFilterDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
						String journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", "");
						if ( !beginFilterDate.isEmpty() && !endFilterDate.isEmpty() ) {
							DashboardReportCacheKey cacheKey = new DashboardReportCacheKey(journeyMapId, beginFilterDate, endFilterDate);
							DashboardEventReport report = Analytics360Management.getSummaryEventDataReport(cacheKey);
							return JsonDataPayload.ok(uri, report, loginUser, DashboardEventReport.class);
						} 
						else {
							return JsonDataPayload.fail("beginFilterDate and endFilterDate must be a valid date", 500);
						}
					}
					
					case URI_EVENT_REPORT_PROFILE : {
						String beginFilterDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
						String endFilterDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
						String profileId = HttpWebParamUtil.getString(params,"profileId", "");
						String journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", "");
						if ( !beginFilterDate.isEmpty() && !endFilterDate.isEmpty() && !profileId.isEmpty() ) {
							DashboardEventReport report = Analytics360Management.getDashboardEventReportForProfile(journeyMapId, profileId, beginFilterDate, endFilterDate);
							return JsonDataPayload.ok(uri, report, loginUser, DashboardEventReport.class);
						} else {
							return JsonDataPayload.fail("beginFilterDate and endFilterDate must be a valid date, profileId must be a valid ID of profile", 500);
						}
					}
					
					case URI_EVENT_REPORT_JOURNEY_PROFILE : {
						String profileId = HttpWebParamUtil.getString(params,"profileId", "");
						String journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", "");
						if ( !profileId.isEmpty() ) {
							JourneyProfileReport report = Analytics360Management.getJourneyEventStatisticsForProfile(profileId, journeyMapId);
							return JsonDataPayload.ok(uri, report, loginUser, DashboardEventReport.class);
						} else {
							return JsonDataPayload.fail("profileId must be a valid ID", 500);
						}
					}
					
					case URI_TOUCHPOINT_REPORT : {
						String beginDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
						String endDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
						String journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", "");
						String profileId = HttpWebParamUtil.getString(params,"profileId", "");
						int touchpointType = HttpWebParamUtil.getInteger(params,"touchpointType", -1);
						int startIndex = HttpWebParamUtil.getInteger(params,"startIndex", 0);
						int numberResult = HttpWebParamUtil.getInteger(params,"numberResult", 20);
						List<TouchpointReport> reports;
						if ( !beginDate.isEmpty() && !endDate.isEmpty() ) {
							reports = TouchpointManagement.getTouchpointReport(journeyMapId, touchpointType, beginDate, endDate, startIndex, numberResult);
						} 
						else if( !profileId.isEmpty() ) {
							reports = TouchpointManagement.getTouchpointReportForProfile(profileId, journeyMapId, touchpointType, startIndex, numberResult);
						}
						else {
							reports = new ArrayList<>(0);
						}
						return JsonDataPayload.ok(uri, reports, loginUser, TouchpointReport.class);
					}
					
					case URI_TOUCHPOINT_FLOW_REPORT : {
						String beginFilterDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
						String endFilterDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
						String journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", "");
						String profileId = HttpWebParamUtil.getString(params,"profileId", "");
						int startIndex = HttpWebParamUtil.getInteger(params,"startIndex", 0);
						int numberFlow = HttpWebParamUtil.getInteger(params,"numberFlow", 200); //number of flow edges
						List<TouchpointFlowReport> reports;
						boolean reportForProfile = ! profileId.isBlank();
						if( reportForProfile ) {
							reports = TouchpointManagement.getTouchpointFlowReportForProfile(profileId, journeyMapId, beginFilterDate, endFilterDate, startIndex, numberFlow);
						}
						else {
							reports = TouchpointManagement.getTouchpointFlowReportForJourney(journeyMapId, beginFilterDate, endFilterDate, startIndex, numberFlow);
						}
						CytoscapeData data = new CytoscapeData(journeyMapId, reports, true);
						return JsonDataPayload.ok(uri, data, loginUser, CytoscapeData.class);
					}
					
					case URI_TOUCHPOINT_HUB_REPORT : {
						String beginDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
						String endDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
						String journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", "");
						String profileId = HttpWebParamUtil.getString(params,"profileId", "");
						
						List<TouchpointHubReport> reports;
						if( ! profileId.isEmpty() ) {
							reports = TouchpointHubManagement.getTouchpointHubReportForProfile(profileId, journeyMapId);
						}
						else {
							int maxTouchpointHubSize = HttpWebParamUtil.getInteger(params,"maxTouchpointHubSize", 100);
							reports = TouchpointHubManagement.getTouchpointHubReport(journeyMapId, beginDate, endDate, maxTouchpointHubSize);
						}
						return JsonDataPayload.ok(uri, reports, loginUser, TouchpointHubReport.class);
					}
					
					case URI_JOURNEY_FLOW_REPORT : {
						String profileId = HttpWebParamUtil.getString(params,"profileId", "");
						String journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", "");
						if (!profileId.isEmpty() && !journeyMapId.isEmpty()  ) {
							JourneyMap journeyReport = Analytics360Management.getJourneyMapReportForProfile(profileId, journeyMapId);
							return JsonDataPayload.ok(uri, journeyReport, loginUser, JourneyMap.class);
						} else {
							return JsonDataPayload.fail("profileId and journeyMapId must be a valid value", 500);
						}
					}
					
					case URI_EVENT_IN_SEGMENT_REPORT : {
						String beginFilterDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
						String endFilterDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
						String segmentId = HttpWebParamUtil.getString(params,"segmentId", "");
						
						if ( !beginFilterDate.isEmpty() && !endFilterDate.isEmpty() && !segmentId.isEmpty() ) {
							DashboardEventReport report = Analytics360Management.getDashboardEventReportForSegment(segmentId, beginFilterDate, endFilterDate);
							return JsonDataPayload.ok(uri, report, loginUser, DashboardEventReport.class);
						} else {
							return JsonDataPayload.fail("beginFilterDate and endFilterDate must be a valid date, profileId must be a valid ID of profile", 500);
						}
					}
					
					case URI_CX_REPORT : {
						String beginFilterDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
						String endFilterDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
						int feedbackDataTypeInt = HttpWebParamUtil.getInteger(params, "feedbackDataType", -1);
						String feedbackDataType = FeedbackType.getFeedbackTypeAsText(feedbackDataTypeInt);
						
						if ( !beginFilterDate.isEmpty() && !endFilterDate.isEmpty() && !feedbackDataType.isEmpty() ) {
							FeedbackRatingReport report = FeedbackDataManagement.getFeedbackReport(feedbackDataType, beginFilterDate, endFilterDate);
							return JsonDataPayload.ok(uri, report, loginUser, FeedbackRatingReport.class);
						} else {
							return JsonDataPayload.fail("beginFilterDate and endFilterDate must be a valid date", 500);
						}
					}
					
					case URI_CX_SURVEY_REPORT : {
						if(loginUser.canViewSurveyReport()) {
							String beginFilterDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
							String endFilterDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
							String refTemplateId = HttpWebParamUtil.getString(params,"refTemplateId", "");
							
							boolean includeGroup = HttpWebParamUtil.getBoolean(params,"includeGroup", true);
							boolean includeObject = HttpWebParamUtil.getBoolean(params,"includeObject", true);
							boolean includeItem = HttpWebParamUtil.getBoolean(params,"includeItem", true);
							boolean includePerson = HttpWebParamUtil.getBoolean(params,"includePerson", true);
							boolean rankingByScore = HttpWebParamUtil.getBoolean(params,"rankingByScore", false);
							
							if ( !beginFilterDate.isEmpty() &&  !endFilterDate.isEmpty() &&  !refTemplateId.isEmpty() ) {
								List<FeedbackSurveyReport> fbReports = FeedbackDataManagement.getSurveyFeedbackReport(refTemplateId, beginFilterDate, endFilterDate, 
										includeGroup, includeObject, includeItem, includePerson, rankingByScore);
								return JsonDataPayload.ok(uri, fbReports, loginUser, FeedbackSurveyReport.class);
							} 
							else {
								return JsonDataPayload.fail("beginFilterDate and endFilterDate must be a valid date", 500);
							}
						}
						else {
							return JsonErrorPayload.NO_AUTHORIZATION;
						}
					}
					
					case URI_CX_SURVEY_DATA_EXPORT : {
						if(loginUser.canViewSurveyReport()) {
							String beginFilterDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
							String endFilterDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
							String refTemplateId = HttpWebParamUtil.getString(params,"refTemplateId", "");
							
							boolean includeGroup = HttpWebParamUtil.getBoolean(params,"includeGroup", true);
							boolean includeObject = HttpWebParamUtil.getBoolean(params,"includeObject", true);
							boolean includeItem = HttpWebParamUtil.getBoolean(params,"includeItem", true);
							boolean includePerson = HttpWebParamUtil.getBoolean(params,"includePerson", true);
							boolean rankingByScore = HttpWebParamUtil.getBoolean(params,"rankingByScore", false);
							String systemUserId = loginUser.getKey();
							
							Map<String,String> results = null;
							if ( !beginFilterDate.isEmpty() &&  !endFilterDate.isEmpty() &&  !refTemplateId.isEmpty() ) {
								results = FeedbackDataManagement.exportSurveyFeedbackData(refTemplateId, beginFilterDate, endFilterDate, 
										includeGroup, includeObject, includeItem, includePerson, rankingByScore, systemUserId);
							}
							return JsonDataPayload.ok(uri, results, loginUser, FeedbackSurveyReport.class);
						}
						else {
							return JsonErrorPayload.NO_AUTHORIZATION;
						}
					}
					
					case URI_EVENT_MATRIX_REPORT : {
						
						String journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", "");
						String profileId = HttpWebParamUtil.getString(params,"profileId", "");
						String beginFilterDate = HttpWebParamUtil.getString(params,"beginFilterDate", "");
						String endFilterDate = HttpWebParamUtil.getString(params,"endFilterDate", "");
						
						Map<String, Object> model = Analytics360Management.getEventMatrixReportModel(journeyMapId, profileId, beginFilterDate, endFilterDate);
						return JsonDataPayload.ok(uri, model, loginUser, EventMatrixReport.class);
					}
					
					case URI_LIST_NOTEBOOKS : {
						int startIndex =   HttpWebParamUtil.getInteger(params,"startIndex", 0);
						int numberResult = HttpWebParamUtil.getInteger(params,"numberResult", 10);
						
						List<Notebook> list = NotebookManagement.getNotebooks(startIndex, numberResult);
						return JsonDataPayload.ok(uri, list, loginUser, Notebook.class);
					}
					
					case URI_GET_NOTEBOOK : {
						String id = HttpWebParamUtil.getString(params,"id", "");
						if (!id.isEmpty()) {
							//TODO
							return JsonDataPayload.ok(uri, null, loginUser, Notebook.class);
						}
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
