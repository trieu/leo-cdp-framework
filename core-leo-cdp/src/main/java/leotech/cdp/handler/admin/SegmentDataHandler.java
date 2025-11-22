package leotech.cdp.handler.admin;

import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.ProfileGraphManagement;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.handler.api.SegmentApiHandler;
import leotech.cdp.model.customer.Segment;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;

/**
 * segment data handler
 * 
 * @author tantrieuf31
 * @since 2020/09/02
 *
 */
public final class SegmentDataHandler extends SecuredHttpDataHandler {

	static final String IS_PRODUCT_GROUP = "isProductGroup";
	static final String ASSET_GROUP_ID = "assetGroupId";
	static final String SEGMENT_ID = "segmentId";
	
	static final String GET_SEGMENT_ALL_REFS = "/cdp/segments/all-refs";
	static final String LIST_WITH_FILTER = "/cdp/segments/filter";
	
	static final String REFRESH_DATA_ALL_SEGMENTS = "/cdp/segments/refresh";
	static final String REFRESH_DATA_SINGLE_SEGMENT = "/cdp/segment/refresh";
	
	static final String EXPORT_CSV_PROFILES = "/cdp/segment/export-csv";
	static final String EXPORT_JSON_PROFILES = "/cdp/segment/export-json";
	
	static final String SAVE_SEGMENT = "/cdp/segment/save";
	static final String LOAD_SINGLE_SEGMENT = "/cdp/segment/load";
	
	static final String LOAD_PROFILES_IN_SEGMENT = "/cdp/segment/profile-query-builder";
	static final String LOAD_SEGMENT_STATISTICS = "/cdp/segment/statistics";
	static final String DELETE_SEGMENT = "/cdp/segment/delete";
	
	static final String SET_RECOMMMENDED_ITEMS = "/cdp/segment/set-recommended-items";
	static final String REMOVE_RECOMMMENDED_ITEMS = "/cdp/segment/remove-recommended-items";
	
	public SegmentDataHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	/**
	 * HTTP POST handler
	 */
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			switch (uri) {
				// list of segments
				case LIST_WITH_FILTER: {
					return SegmentApiHandler.loadSegments(uri, paramJson, loginUser);
				}
				// load profiles in segment
				case LOAD_PROFILES_IN_SEGMENT: {
					return SegmentApiHandler.loadProfilesInSegment(uri, paramJson, loginUser);
				}
				// load the size of segment
				case LOAD_SEGMENT_STATISTICS: {
					return SegmentApiHandler.loadSegmentStatistics(uri, paramJson, loginUser);
				}
				// load metadata of a single segment 
				case LOAD_SINGLE_SEGMENT: {
					return SegmentApiHandler.loadSingleSegment(uri, paramJson, loginUser);
				}
				// save all metadata of a segment 
				case SAVE_SEGMENT: {
					return SegmentApiHandler.saveSegment(uri, paramJson, loginUser);
				}
				// delete a segment by ID
				case DELETE_SEGMENT: {
					return SegmentApiHandler.deleteSegment(uri, paramJson, loginUser);
				}
				// update segment ref ID of profile if match the rules in a specific segment
				case REFRESH_DATA_ALL_SEGMENTS: {
					return refreshDataAllSegments(uri, loginUser);
				}
				// refresh a segment
				case REFRESH_DATA_SINGLE_SEGMENT: {
					return refreshDataSegment(uri, paramJson, loginUser);
				}
				// set recommendation data
				case SET_RECOMMMENDED_ITEMS : {
					return setRecommendedItems(uri, paramJson, loginUser);
				}
				// unset recommendation data
				case REMOVE_RECOMMMENDED_ITEMS : {
					return removeRecommendedItems(uri, paramJson, loginUser);
				}
				
				default: {
					return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			}
		}
		else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}


	/**
	 * HTTP GET handler
	 */
	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			switch (uri) {
				case LOAD_SINGLE_SEGMENT: {
					return SegmentApiHandler.loadSingleSegment(uri, params, loginUser);
				}
				case EXPORT_CSV_PROFILES : {
					if(loginUser.hasOperationRole()) {
						return SegmentApiHandler.exportProfilesAsCsv(uri, params, loginUser);
					}
					else {
						return JsonErrorPayload.NO_AUTHORIZATION;
					}
				}
				case EXPORT_JSON_PROFILES : {
					if(loginUser.hasOperationRole()) {
						return SegmentApiHandler.exportProfilesAsJson(uri, params, loginUser);
					}
					else {
						return JsonErrorPayload.NO_AUTHORIZATION;
					}
				}
				case GET_SEGMENT_ALL_REFS : {
					if(loginUser.hasOperationRole()) {
						return SegmentApiHandler.getAllSegmentRefs(uri, params, loginUser);
					}
					else {
						return JsonErrorPayload.NO_AUTHORIZATION;
					}
				}
				default:
					return JsonErrorPayload.NO_HANDLER_FOUND;
				}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}


	private JsonDataPayload setRecommendedItems(String uri, JsonObject paramJson, SystemUser loginUser) {
		if(loginUser.hasOperationRole()) {
			String assetGroupId = paramJson.getString(ASSET_GROUP_ID, "");
			String segmentId = paramJson.getString(SEGMENT_ID, "");
			boolean isProductGroup = paramJson.getBoolean(IS_PRODUCT_GROUP, false);
			if( ! assetGroupId.isBlank() && ! segmentId.isBlank() ) {
				int c = ProfileGraphManagement.setRecommendationItems(isProductGroup, assetGroupId, segmentId);
				return JsonDataPayload.ok(uri, c, loginUser, Segment.class);
			} else {
				return JsonDataPayload.fail("assetGroupId and segmentId must not be empty", 500);
			}
		}
		else {
			return JsonErrorPayload.NO_AUTHORIZATION;
		}
	}
	
	private JsonDataPayload removeRecommendedItems(String uri, JsonObject paramJson, SystemUser loginUser) {
		if(loginUser.hasOperationRole()) {
			String assetGroupId = paramJson.getString(ASSET_GROUP_ID, "");
			String segmentId = paramJson.getString(SEGMENT_ID, "");
			boolean isProductGroup = paramJson.getBoolean(IS_PRODUCT_GROUP, false);
			if( ! assetGroupId.isBlank() && ! segmentId.isBlank() ) {
				ProfileGraphManagement.removeRecommendationItems(isProductGroup, assetGroupId, segmentId);
				return JsonDataPayload.ok(uri, true, loginUser, Segment.class);
			}
			else {
				return JsonDataPayload.fail("assetGroupId and segmentId must not be empty", 500);
			}
		}
		else {
			return JsonErrorPayload.NO_AUTHORIZATION;
		}
	}
	
	private JsonDataPayload refreshDataSegment(String uri, JsonObject paramJson, SystemUser loginUser) {
		String segmentId = paramJson.getString(SEGMENT_ID, "");
		boolean ok = SegmentDataManagement.refreshSegmentData(segmentId);
		return JsonDataPayload.ok(uri, ok, loginUser, Segment.class);
	}
	
	private JsonDataPayload refreshDataAllSegments(String uri, SystemUser loginUser) {
		int numberOfSegments = SegmentDataManagement.refreshDataOfAllSegments();
		return JsonDataPayload.ok(uri, numberOfSegments, loginUser, Segment.class);
	}

}