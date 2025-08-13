package leotech.cdp.query.filters;

import com.google.gson.Gson;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * the data model for datatables <br>
 * More at https://datatables.net
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public class DataFilter {

	private static final String SORT_DESC = "DESC";
	private static final String SORT_ASC = "ASC";
	protected String uri;
	protected int draw;

	protected int start;
	protected int length;

	protected String journeyMapId = StringPool.BLANK;
	protected String segmentId = StringPool.BLANK;
	
	protected String searchValue = StringPool.BLANK;
	protected String sortField = StringPool.BLANK;
	protected boolean sortAsc = true;
	protected boolean realtimeQuery = false;
	protected boolean withLastEvents = false;
	
	protected String authorizedViewer = StringPool.BLANK;
	protected String authorizedEditor = StringPool.BLANK;
	
	/**
	 * An user who can view and update details of segment
	 */
	protected String loginUsername;
	
	/**
	 * the web visitor ID
	 */
	protected String profileVisitorId;
	


	/**
	 * super admin can view and edit all data in system
	 */
	protected boolean isSuperAdmin = false;
	
	/**
	 *  admin can view and edit all data in a business unit
	 */
	protected boolean hasAdminRole = false;
	
	protected int status = PersistentObject.STATUS_ACTIVE;
	
	protected String beginFilterDate = StringPool.BLANK;
	protected String endFilterDate = StringPool.BLANK;
	protected JsonArray orderableFields = null;

	public DataFilter() {
		this.start = 0;
		this.length = 20;
	}

	public DataFilter(int start, int length) {
		super();
		this.start = start;
		this.length = length;
	}
	
	public DataFilter(SystemUser systemUser, int start, int length, boolean realtimeQuery) {
		super();
		checkSystemUser(systemUser);
		this.start = start;
		this.length = length;
		this.realtimeQuery = realtimeQuery;
	}
	
	public DataFilter(SystemUser systemUser, int start, int length, boolean realtimeQuery, boolean withLastEvents) {
		super();
		checkSystemUser(systemUser);
		this.start = start;
		this.length = length;
		this.realtimeQuery = realtimeQuery;
		this.withLastEvents = withLastEvents;
	}
	
	public DataFilter(String searchValue, int start, int length) {
		super();
		this.searchValue = searchValue;
		this.start = start;
		this.length = length;
	}
	
	public DataFilter(String journeyMapId, String searchValue, int start, int length) {
		super();
		this.journeyMapId = journeyMapId;
		this.searchValue = searchValue;
		this.start = start;
		this.length = length;
	}
	
	/**
	 * @param systemUser in SystemUser
	 * @param uri
	 * @param params in MultiMap
	 */
	public DataFilter(SystemUser systemUser, String uri, MultiMap params) {
		checkSystemUser(systemUser);
		
		this.uri = uri;

		this.start = HttpWebParamUtil.getInteger(params, "start", 0);
		this.length = HttpWebParamUtil.getInteger(params, "length", 20);
		this.draw = HttpWebParamUtil.getInteger(params, "draw", 1);

		this.status = HttpWebParamUtil.getInteger(params,"status", PersistentObject.STATUS_ACTIVE);
		this.searchValue = HttpWebParamUtil.getString(params,"searchValue", "");
		this.sortField = HttpWebParamUtil.getString(params,"sortField", "");
		this.sortAsc = HttpWebParamUtil.getBoolean(params,"sortAsc", true);
		this.realtimeQuery = HttpWebParamUtil.getBoolean(params,"realtimeQuery", true);
		this.withLastEvents = HttpWebParamUtil.getBoolean(params,"withLastEvents", false);
		
		
		this.authorizedViewer = StringUtil.safeString(params.get("authorizedViewer"), "");
		this.authorizedViewer = StringUtil.safeString(params.get("authorizedEditor"), "");
		
		this.beginFilterDate = HttpWebParamUtil.getString(params,"beginFilterDate", StringPool.BLANK);
		this.endFilterDate = HttpWebParamUtil.getString(params,"endFilterDate", StringPool.BLANK);
		
		this.journeyMapId = HttpWebParamUtil.getString(params,"journeyMapId", StringPool.BLANK);
		this.segmentId =  HttpWebParamUtil.getString(params,"segmentId", StringPool.BLANK);
	}




	/**
	 * @param systemUser
	 * @param uri
	 * @param paramJson
	 */
	public DataFilter(SystemUser systemUser, String uri, JsonObject paramJson) {
		checkSystemUser(systemUser);
		
		this.uri = uri;

		this.start = paramJson.getInteger("start", 0);
		this.length = paramJson.getInteger("length", 20);
		this.draw = paramJson.getInteger("draw", 1);

		this.status = paramJson.getInteger("status", PersistentObject.STATUS_ACTIVE);
		this.searchValue = paramJson.getString("searchValue", "");
		this.sortField = paramJson.getString("sortField", "");
		this.sortAsc = paramJson.getBoolean("sortAsc", true);
		this.realtimeQuery = paramJson.getBoolean("realtimeQuery", true);
		this.withLastEvents = paramJson.getBoolean("withLastEvents", false);
		
		this.authorizedViewer = paramJson.getString("authorizedViewer", "");
		this.authorizedEditor = paramJson.getString("authorizedEditor", "");
		
		this.beginFilterDate = paramJson.getString("beginFilterDate", StringPool.BLANK);
		this.endFilterDate = paramJson.getString("endFilterDate", StringPool.BLANK);
		
		this.journeyMapId = paramJson.getString("journeyMapId", StringPool.BLANK);
		this.segmentId = paramJson.getString("segmentId", StringPool.BLANK);
		
		// this is only for the Admin CDP
		this.orderableFields = paramJson.getJsonArray("order");
	}
	
	public void checkSystemUser(SystemUser systemUser) {
		// to check permission
		this.isSuperAdmin = systemUser.hasSuperAdminRole();
		this.hasAdminRole = systemUser.hasAdminRole();
		this.loginUsername = systemUser.getUserLogin();
		this.profileVisitorId = systemUser.getProfileVisitorId();
	}
	
	public JsonArray getOrderableFields() {
		if (orderableFields == null) {
			orderableFields = new JsonArray();
		}
		return orderableFields;
	}

	public void setOrderableFields(JsonArray orderableFields) {
		this.orderableFields = orderableFields;
	}

	public boolean isRealtimeQuery() {
		return realtimeQuery;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int getDraw() {
		return draw;
	}

	public void setDraw(int draw) {
		this.draw = draw;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}
	
	public void updateStart(int delta) {
		this.start += delta;
	}

	public String getSortField(String defaultField) {
		if (StringUtil.isEmpty(sortField)) {
			return defaultField;
		}
		return sortField;
	}

	public String getSortDirection() {
		return this.sortAsc ? SORT_ASC : SORT_DESC;
	}
	
	public String getSearchValue() {
		return StringUtil.safeString(searchValue);
	}

	public String getFormatedSearchValue() {
		return searchValue.isEmpty() ? "" : ("%" + searchValue.toLowerCase() + "%");
	}
	
	public String getLoginUsername() {
		return StringUtil.safeString(loginUsername);
	}

	public void setLoginUsername(String loginUsername) {
		if(StringUtil.isNotEmpty(loginUsername)) {
			this.loginUsername = loginUsername;
		}
	}

	public String getProfileVisitorId() {
		return StringUtil.safeString(profileVisitorId);
	}

	public void setProfileVisitorId(String profileVisitorId) {
		this.profileVisitorId = profileVisitorId;
	}

	public boolean isSuperAdmin() {
		return isSuperAdmin;
	}

	public void setSuperAdmin(boolean isSuperAdmin) {
		this.isSuperAdmin = isSuperAdmin;
	}

	public boolean hasAdminRole() {
		return hasAdminRole;
	}

	public void setHasAdminRole(boolean isAdmin) {
		this.hasAdminRole = isAdmin;
	}
	
	public String getAuthorizedViewer() {
		return StringUtil.safeString(authorizedViewer);
	}

	public void setAuthorizedViewer(String authorizedViewer) {
		this.authorizedViewer = authorizedViewer;
	}

	public String getAuthorizedEditor() {
		return StringUtil.safeString(authorizedEditor);
	}

	public void setAuthorizedEditor(String authorizedEditor) {
		this.authorizedEditor = authorizedEditor;
	}
	
	public boolean isFilteringAuthorization() {
		return StringUtil.isNotEmpty(this.authorizedViewer) || StringUtil.isNotEmpty(this.authorizedEditor);
	}
	
	public String getSortField() {
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public boolean isSortAsc() {
		return sortAsc;
	}

	public void setSortAsc(boolean sortAsc) {
		this.sortAsc = sortAsc;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setSearchValue(String searchValue) {
		this.searchValue = searchValue;
	}

	public void setRealtimeQuery(boolean realtimeQuery) {
		this.realtimeQuery = realtimeQuery;
	}
	
	public String getBeginFilterDate() {
		return StringUtil.safeString(beginFilterDate);
	}

	public void setBeginFilterDate(String beginFilterDate) {
		this.beginFilterDate = beginFilterDate;
	}

	public String getEndFilterDate() {
		return StringUtil.safeString(endFilterDate);
	}

	public void setEndFilterDate(String endFilterDate) {
		this.endFilterDate = endFilterDate;
	}

	public String getSegmentId() {
		return segmentId;
	}

	public void setSegmentId(String segmentId) {
		this.segmentId = segmentId;
	}

	public String getJourneyMapId() {
		return journeyMapId;
	}

	public void setJourneyMapId(String journeyMapId) {
		this.journeyMapId = journeyMapId;
	}

	public boolean isHasAdminRole() {
		return hasAdminRole;
	}
	
	

	public boolean isWithLastEvents() {
		return withLastEvents;
	}

	public void setWithLastEvents(boolean withLastEvents) {
		this.withLastEvents = withLastEvents;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
