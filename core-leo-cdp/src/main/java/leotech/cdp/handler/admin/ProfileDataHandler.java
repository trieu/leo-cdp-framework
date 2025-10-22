package leotech.cdp.handler.admin;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.EventDataManagement;
import leotech.cdp.domain.IdentityResolutionManagement.ResolutioResult;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.ProfileGraphManagement;
import leotech.cdp.domain.ProfileQueryManagement;
import leotech.cdp.domain.schema.FunnelMetaData;
import leotech.cdp.job.reactive.JobMergeDuplicatedProfiles;
import leotech.cdp.job.scheduled.MergeDuplicateProfilesJob;
import leotech.cdp.model.analytics.FeedbackEvent;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.customer.BusinessCase;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.model.customer.ProfileType;
import leotech.cdp.model.graph.Profile2Content;
import leotech.cdp.model.graph.Profile2Conversion;
import leotech.cdp.model.graph.Profile2Product;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.query.filters.ProfileFilter;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.CsvDataParser;
import leotech.system.model.ImportingResult;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.TaskRunner;
import rfx.core.util.StringUtil;

/**
 * Admin Web Handler
 * 
 * @author tantrieuf31
 *
 */
public final class ProfileDataHandler extends SecuredHttpDataHandler {
	
	private static final String VISITOR_ID = "visitorId";
	private static final String CRM_REF_ID = "crmRefId";
	private static final String PROFILE_ID = "profileId";
	private static final String INPUT_NEW_PROFILE = "new";
	
	// for list
	static final String PROFILE_LIST_ALL = "/cdp/profiles";
	static final String PROFILE_LIST_WITH_FILTER = "/cdp/profiles/filter";
	static final String PROFILE_IMPORT_PROFILE = "/cdp/profiles/import";
	static final String PROFILE_IMPORT_PREVIEW = "/cdp/profiles/import-preview";
	static final String PROFILE_BATCH_UPDATE = "/cdp/profiles/batch-update";
	static final String PROFILE_SEARCH_SUGGESTION = "/cdp/profiles/search-suggestion";
	static final String PROFILE_MERGE_DUPLICATES = "/cdp/profiles/merge-duplicates";
	
	// for profile
	static final String PROFILE_UPDATE = "/cdp/profile/update";
	static final String PROFILE_GET = "/cdp/profile/get";
	static final String PROFILE_GET_BY_ID = "/cdp/profile/get-by-id";
	
	// for identity resolution and profile data processing
	static final String PROFILE_DEDUPLICATE_FOR_ONE_PERSON = "/cdp/profile/deduplicate";
	
	static final String PROFILE_REMOVE = "/cdp/profile/remove";
	static final String PROFILE_DELETE_NOT_ACTIVE = "/cdp/profiles/delete-not-active";
	
	// event view
	static final String PROFILE_TRACKING_EVENTS = "/cdp/profile/tracking-events";
	static final String PROFILE_FEEDBACK_EVENTS = "/cdp/profile/feedback-events";
	static final String PROFILE_BUSINESS_CASES = "/cdp/profile/business-cases";
	static final String PROFILE_CONVERSION_EVENTS = "/cdp/profile/conversion-events";

	static final String PROFILE_UPDATE_ITEM_RANKING = "/cdp/profile/update-item-ranking";
	
	static final String PROFILE_RECOMMENDED_CONTENTS = "/cdp/profile/recommended-contents";
	static final String PROFILE_RECOMMENDED_PRODUCTS = "/cdp/profile/recommended-products";
	static final String PROFILE_PURCHASED_ITEMS = "/cdp/profile/get-purchased-items";
	
	static final String PROFILE_SET_EXT_ATTRIBUTES = "/cdp/profile/set-default-ext-attributes";
	


	public ProfileDataHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}

	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			switch (uri) {
				case PROFILE_LIST_WITH_FILTER : {
					// the list-view component at datatables.net needs AJAX POST method to avoid long URL 
					ProfileFilter filter = new ProfileFilter(loginUser, uri, paramJson);
					JsonDataTablePayload payload = ProfileQueryManagement.filter(filter);
					payload.setUserLoginPermission(loginUser, Profile.class);
					return payload;
				}
				
				case PROFILE_GET : {
					return getProfile(uri, loginUser, paramJson);
				}
				
				case PROFILE_MERGE_DUPLICATES : {
					if(isAdminRole(loginUser)) {
						// run data job manually on Admin InstanceContext
						TaskRunner.runJob(new MergeDuplicateProfilesJob(), 3000);
						return JsonDataPayload.ok(uri, true, loginUser, Profile.class);
					} else {
						return JsonDataPayload.fail("No authorization to merge duplicated profiles", 500);
					}
				}
				
				case PROFILE_DEDUPLICATE_FOR_ONE_PERSON : {
					if(isDataOperator(loginUser, Profile.class)) {
						String profileId = paramJson.getString(PROFILE_ID, "");
						if(! profileId.isBlank() ) {
							ResolutioResult c = JobMergeDuplicatedProfiles.doDeduplicationManually(loginUser, profileId);
							return JsonDataPayload.ok(uri, c, loginUser, Profile.class);
						} else {
							return JsonDataPayload.fail("Invalid profile.id " + profileId, 404);
						}
					} else {
						return JsonDataPayload.fail("No authorization to update profile", 500);
					}
				}
				
				case PROFILE_UPDATE : {
					if(isDataOperator(loginUser, Profile.class)) {
						String json = paramJson.getString("objectJson", "{}");
						try {
							String profileId = ProfileDataManagement.updateFromJson(loginUser, json);
							if(profileId != null) {
								return JsonDataPayload.ok(uri, profileId, loginUser, Profile.class);
							} else {
								return JsonDataPayload.fail("failed to save Profile data into database", 500);
							}
						} catch (Exception e) {
							return JsonDataPayload.fail(e.getMessage(), 500);
						}
					} else {
						return JsonDataPayload.fail("No authorization to update profile", 500);
					}
				}
				
				case PROFILE_REMOVE : {
					String id = paramJson.getString("id", "");
					if(isDataOperator(loginUser, Profile.class)) {
						
						try {
							boolean rs = ProfileDataManagement.checkAndRemove(loginUser, id);
							return JsonDataPayload.ok(uri, rs, loginUser, Profile.class);
						} catch (Exception e) {
							return JsonDataPayload.fail(e.getMessage(), 500);
						}
					} else {
						return JsonDataPayload.fail("No authorization to remove profile ID: " + id, 500);
					}
				}
				
				case PROFILE_DELETE_NOT_ACTIVE : {
					if(isAdminRole(loginUser)) {
						int status = paramJson.getInteger("status", Profile.STATUS_REMOVED);
						boolean deleteDeadVisitors = status == Profile.STATUS_DEAD;
						try {
							long deletedCount = 0;
							if(deleteDeadVisitors) {
								deletedCount = ProfileDataManagement.deleteAllDeadVisitors();								
							}
							else {
								deletedCount = ProfileDataManagement.deleteNotActiveProfile(status);								
							}
							return JsonDataPayload.ok(uri, deletedCount, loginUser, Profile.class);
						} catch (Exception e) {
							e.printStackTrace();
							return JsonDataPayload.fail(e.getMessage(), 500);
						}
					} else {
						return JsonDataPayload.fail("No authorization to delete non-active profiles", 500);
					}
				}
				
				case PROFILE_BATCH_UPDATE : {
					if(isAdminRole(loginUser)) {
						try {
							int updatedCount = ProfileDataManagement.batchUpdateProfiles(paramJson);
							return JsonDataPayload.ok(uri, updatedCount, loginUser, Profile.class);
						} catch (Exception e) {
							e.printStackTrace();
							return JsonDataPayload.fail(e.getMessage(), 500);
						}
					} else {
						return JsonDataPayload.fail("No authorization to delete non-active profiles", 500);
					}
				}
				
				case PROFILE_IMPORT_PROFILE : {
					if(loginUser.hasOperationRole()) {
						String importFileUrl = paramJson.getString(CsvDataParser.IMPORT_FILE_URL, "");
						if(StringUtil.isNotEmpty(importFileUrl)) {
							String userLogin = loginUser.getUserLogin();
							
							String dataLabelsStr = paramJson.getString("dataLabels","");
							String funnelStageId = paramJson.getString("funnelStageId",FunnelMetaData.STAGE_LEAD);
							String journeyMapId = paramJson.getString("journeyMapId",JourneyMap.DEFAULT_JOURNEY_MAP_ID);
							String touchpointHubId = paramJson.getString("touchpointHubId", TouchpointHub.DATA_OBSERVER.getId());
							
							int journeyStage = paramJson.getInteger("journeyStage",EventMetric.JOURNEY_STAGE_AWARENESS);
							int contactType = paramJson.getInteger("contactType", ProfileType.CRM_IMPORTED_CONTACT);
							boolean overwriteOldData = paramJson.getBoolean("overwriteOldData", false);							
							
							ImportingResult rs = ProfileDataManagement.importAndSave(userLogin, importFileUrl, dataLabelsStr, funnelStageId, journeyMapId, touchpointHubId, journeyStage, contactType, overwriteOldData);
							return JsonDataPayload.ok(uri, rs, loginUser, Profile.class);
						}
						return JsonDataPayload.fail(CsvDataParser.IMPORT_FILE_URL + " must not be empty", 500);
					} else {
						return JsonDataPayload.fail("No authorization to import data", 500);
					}
				}
				
				case PROFILE_UPDATE_ITEM_RANKING : {
					if(isDataOperator(loginUser, Profile.class)) {
						String profileId = paramJson.getString(PROFILE_ID);
						int recommendationModel = paramJson.getInteger("recommendationModel");
						String graphName = paramJson.getString("graphName");
						String rankingInfoListJson = paramJson.getString("rankingInfoList");
						if(StringUtil.isNotEmpty(rankingInfoListJson)) {
							Type type = new TypeToken<HashMap<String, Integer>>(){}.getType();
							Map<String, Integer> map = new Gson().fromJson(rankingInfoListJson, type);
							ProfileGraphManagement.updateItemRanking(profileId, recommendationModel, graphName, map);
							return JsonDataPayload.ok(uri, true, loginUser, Profile.class);
						}
					} else {
						return JsonDataPayload.fail("No authorization to update item ranking", 500);
					}
				}
				
				case PROFILE_SET_EXT_ATTRIBUTES : {
					if(isSuperAdminRole(loginUser)) {
						String extAttributesJson = paramJson.getString("extAttributes");
						if(StringUtil.isNotEmpty(extAttributesJson)) {
							Type type = new TypeToken<HashMap<String,Object>>(){}.getType();
							Map<String,Object> extAttributes = new Gson().fromJson(extAttributesJson, type);
							ProfileDataManagement.setProfileDefaultExtAttributes(extAttributes);
							return JsonDataPayload.ok(uri, true, loginUser, Profile.class);
						}
					} else {
						return JsonDataPayload.fail("No authorization to set ext-attributes for all profiles", 500);
					}
				}
				
				default : {
					return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			}
		} else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}
	
	JsonDataPayload getProfile(String uri, SystemUser loginUser, JsonObject paramJson) {
		String idType = paramJson.getString("idType");
		
		if(idType.equals(PROFILE_ID)) {
			String profileId = paramJson.getString(PROFILE_ID, INPUT_NEW_PROFILE);
			return getByProfileId(uri, loginUser, profileId);
		}
		else if(idType.equals(CRM_REF_ID)) {
			String crmRefId = paramJson.getString(CRM_REF_ID, "");
			return getByCrmId(uri, loginUser, crmRefId);
		}
		
		else if(idType.equals(VISITOR_ID)) {
			String visitorId = paramJson.getString(VISITOR_ID, "");
			return getByVisitorId(uri, loginUser, visitorId);
		}
		return JsonDataPayload.fail("Not found any profile with idType: " + idType, 500);
	}

	private JsonDataPayload getByVisitorId(String uri, SystemUser loginUser, String visitorId) {
		ProfileSingleView profile = ProfileQueryManagement.checkAndGetProfileByVisitorId(loginUser, visitorId);
		if(profile != null) {
			JsonDataPayload result = JsonDataPayload.ok(uri, profile, loginUser, Profile.class);
			boolean checkToEditProfile = loginUser.checkToEditProfile(profile);
			result.setCanEditData(checkToEditProfile);
			result.setCanDeleteData(checkToEditProfile);
			return result;
		} else {
			return JsonDataPayload.fail("No authorization or not found any profile with Web Visitor ID " + visitorId, 404);
		}
	}

	private JsonDataPayload getByCrmId(String uri, SystemUser loginUser, String crmRefId) {
		ProfileSingleView profile = ProfileQueryManagement.checkAndGetProfileByCrmId(loginUser, crmRefId);
		if(profile != null) {
			JsonDataPayload result = JsonDataPayload.ok(uri, profile, loginUser, Profile.class);
			boolean checkToEditProfile = loginUser.checkToEditProfile(profile);
			result.setCanEditData(checkToEditProfile);
			result.setCanDeleteData(checkToEditProfile);
			return result;
		} else {
			return JsonDataPayload.fail("No authorization or not found any profile with CRM ID " + crmRefId, 404);
		}
	}

	private JsonDataPayload getByProfileId(String uri, SystemUser loginUser, String profileId) {
		ProfileSingleView profile = null;
		boolean checkToUpdateData = false;
		if(INPUT_NEW_PROFILE.equals(profileId)) {
			// create new profile model
			profile = new ProfileSingleView(ProfileType.DIRECT_INPUT_CONTACT);
			checkToUpdateData = loginUser.canInsertData(Profile.class);
		} 
		else {
			profile = ProfileQueryManagement.checkAndGetProfileById(loginUser, profileId);
			checkToUpdateData = loginUser.checkToEditProfile(profile);
		}
		if(profile != null) {
			JsonDataPayload result = JsonDataPayload.ok(uri, profile, loginUser, Profile.class);
			result.setCanEditData(checkToUpdateData);
			result.setCanDeleteData(checkToUpdateData);
			return result;
		} 
		else {
			return JsonDataPayload.fail("No authorization or not found any profile with profileId " + profileId, 404);
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			switch (uri) {
				case PROFILE_LIST_ALL : {
					int startIndex =   HttpWebParamUtil.getInteger(params,"startIndex", 0);
					int numberResult = HttpWebParamUtil.getInteger(params,"numberResult", 20);
					List<Profile> list = ProfileQueryManagement.list(startIndex, numberResult);
					return JsonDataPayload.ok(uri, list, loginUser, Profile.class);
				}
				
				case PROFILE_GET_BY_ID : {
					String id = HttpWebParamUtil.getString(params,"id", "");
					ProfileSingleView pf = null;
					if(StringUtil.isNotEmpty(id)) {
						// create new profile model
						pf = ProfileQueryManagement.checkAndGetProfileById(loginUser, id);
					} else {
						pf = new ProfileSingleView();
					}
					return JsonDataPayload.ok(uri, pf, loginUser, Profile.class);
				}
				
				case PROFILE_TRACKING_EVENTS : {
					String profileId = HttpWebParamUtil.getString(params,PROFILE_ID, "");
					String journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", "");
					String searchValue = HttpWebParamUtil.getString(params,"searchValue", "");
					int startIndex =   HttpWebParamUtil.getInteger(params,"startIndex", 0);
					int numberResult = HttpWebParamUtil.getInteger(params,"numberResult", 20);
					List<TrackingEvent> list = EventDataManagement.getTrackingEventsOfProfile(profileId, journeyMapId, searchValue, startIndex, numberResult);
					return JsonDataPayload.ok(uri, list, loginUser, Profile.class);
				}
				
				case PROFILE_FEEDBACK_EVENTS : {
					String profileId = HttpWebParamUtil.getString(params,"id", "");
					int startIndex =   HttpWebParamUtil.getInteger(params,"startIndex", 0);
					int numberResult = HttpWebParamUtil.getInteger(params,"numberResult", 20);
					List<FeedbackEvent> list = EventDataManagement.getFeedbackEventFlowOfProfile(profileId, startIndex, numberResult);
					return JsonDataPayload.ok(uri, list, loginUser, Profile.class);
				}
				
				case PROFILE_BUSINESS_CASES : {
					//TODO
					List<BusinessCase> list = new ArrayList<>(0);
					return JsonDataPayload.ok(uri, list, loginUser, Profile.class);
				}
				
				case PROFILE_SEARCH_SUGGESTION : {
					//TODO
					List<String> keywords = new ArrayList<String>();
					return JsonDataPayload.ok(uri, keywords, loginUser, Profile.class);
				}
				
				case PROFILE_IMPORT_PREVIEW : {
					if(loginUser.hasOperationRole()) {
						String importFileUrl = HttpWebParamUtil.getString(params,"importFileUrl", "");
						if(StringUtil.isNotEmpty(importFileUrl)) {
							List<ProfileSingleView> profiles = ProfileDataManagement.parseToImportProfiles(importFileUrl, true);
							JsonDataTablePayload payload = JsonDataTablePayload.data(uri, profiles , CsvDataParser.TOP_RECORDS);
							return payload;
						}
						return JsonDataPayload.fail("importFileName must not be empty", 500);
					} else {
						return JsonDataPayload.fail("No Authorization To Update Data", 500);
					}
				}
				
				case PROFILE_RECOMMENDED_CONTENTS : {
					String profileId = HttpWebParamUtil.getString(params,PROFILE_ID, "");
					int startIndex =   HttpWebParamUtil.getInteger(params,"startIndex", 0);
					int numberResult = HttpWebParamUtil.getInteger(params,"numberResult", 20);
					List<Profile2Content> listCreatives = ProfileGraphManagement.getRecommendedContentItemsByIndexScore(profileId, startIndex, numberResult);
					return JsonDataPayload.ok(uri, listCreatives, loginUser, Profile.class);
				}
				
				case PROFILE_RECOMMENDED_PRODUCTS : {
					String profileId = HttpWebParamUtil.getString(params,PROFILE_ID, "");
					int startIndex =   HttpWebParamUtil.getInteger(params,"startIndex", 0);
					int numberResult = HttpWebParamUtil.getInteger(params,"numberResult", 20);
					List<Profile2Product> listProducts = ProfileGraphManagement.getRecommendedProductItemsByIndexScore(profileId, startIndex, numberResult);
					return JsonDataPayload.ok(uri, listProducts, loginUser, Profile.class);
				}
				
				case PROFILE_PURCHASED_ITEMS : {
					String profileId = HttpWebParamUtil.getString(params,PROFILE_ID, "");
					int startIndex =   HttpWebParamUtil.getInteger(params,"startIndex", 0);
					int numberResult = HttpWebParamUtil.getInteger(params,"numberResult", 20);
					
					List<Profile2Conversion> listProducts = ProfileGraphManagement.getPurchasedProductItems(profileId, startIndex, numberResult);
					return JsonDataPayload.ok(uri, listProducts, loginUser, Profile.class);
				}
				
				default :
					return JsonErrorPayload.NO_HANDLER_FOUND;
			}

		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}

}
