package leotech.system.domain;

import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.query.filters.SystemEventFilter;
import leotech.system.common.BaseWebRouter;
import leotech.system.dao.SystemEventDaoUtil;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemEvent;
import leotech.system.model.SystemUser;
import leotech.system.util.TaskRunner;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * System Event Management
 * 
 * @author tantrieuf31
 * @since 2023
 *
 */
/**
 * @author thomas
 *
 */
public class SystemEventManagement {

	static final int BATCH_SIZE = 5;
	static final boolean BATCH_ENABLED = false;
	public static final String LEOCDP = "leocdp";


	/**
	 * @param title
	 * @param uri
	 * @param userId
	 * @param userIp
	 * @param userAgent
	 */
	public static void contentView(String title, String uri, String userId, String userIp, String userAgent) {
		// TODO
	}
	
	/**
	 * @param userLogin
	 * @param clazz
	 * @param action
	 * @param paramJson
	 */
	public static void log(String userLogin, Class<?> clazz, String action, JsonObject paramJson) {
		if(userLogin != null) {
			String obj = clazz.getName();
			String ip = StringUtil.safeString(paramJson.remove(BaseWebRouter.P_USER_IP),"");
			String ua = StringUtil.safeString(paramJson.remove(BaseWebRouter.P_USER_AGENT),"");
			String data = paramJson.toString();
			
			SystemEvent log = new SystemEvent(userLogin , obj,"", action, data, ip, ua);			
			System.out.println(log.toString());
			SystemEventManagement.save(log);
		}
	}


	/**
	 * @param systemUser
	 * @param clazz
	 * @param uri
	 * @param paramJson
	 */
	public static void log(SystemUser systemUser, Class<?> clazz, String uri, JsonObject paramJson) {
		if(systemUser != null) {	
			TaskRunner.run(()->{
				String ip = StringUtil.safeString(paramJson.remove(BaseWebRouter.P_USER_IP),"");
				String ua = StringUtil.safeString(paramJson.remove(BaseWebRouter.P_USER_AGENT),"");
				String data = paramJson.toString();
				String userLogin = systemUser.getUserLogin();
				String obj = clazz.getSimpleName();
				
				SystemEvent log = new SystemEvent(userLogin , obj,"", uri, data, ip, ua);
				SystemEventManagement.save(log);
			});			
		}
	}
	
	/**
	 * @param systemUser
	 * @param clazz
	 * @param uri
	 * @param params
	 */
	public static void log(SystemUser systemUser, Class<?> clazz, String uri, MultiMap params) {
		if(systemUser != null) {
			TaskRunner.run(()->{
				String ip = StringUtil.safeString(params.get(BaseWebRouter.P_USER_IP),"");
				String ua = StringUtil.safeString(params.get(BaseWebRouter.P_USER_AGENT),"");
				params.remove(BaseWebRouter.P_USER_IP).remove(BaseWebRouter.P_USER_AGENT);
				
				String data = params.toString();
				String userLogin = systemUser.getUserLogin();
				String obj = clazz.getSimpleName();
				
				SystemEvent log = new SystemEvent(userLogin , obj, "", uri, data, ip, ua);
				SystemEventManagement.save(log);
			});
		}
	}
	

	/**
	 * @param sourceIp
	 * @param objectName
	 * @param objectId
	 * @param uri
	 * @param data
	 */
	public static void dataJobLog(String sourceIp, String objectName, String objectId, String uri, String data ) {
		String userAgent = SystemMetaData.BUILD_ID + SystemMetaData.BUILD_VERSION;
		SystemEvent log = new SystemEvent(LEOCDP, objectName, objectId, uri, data, sourceIp, userAgent);
		System.out.println("=> [dataJobLog] "+log);
		
		TaskRunner.run(()->{	
			SystemEventManagement.save(log);
		});	
	}
	
	/**
	 * @param systemEvent
	 * @return
	 */
	public static String save(SystemEvent systemEvent) {
		return SystemEventDaoUtil.save(systemEvent);
	}
	
	/**
	 * @param userLogin
	 */
	public static void deleteByUserLogin(String userLogin) {
		SystemEventDaoUtil.deleteByUserLogin(userLogin);
	}
	
	/**
	 * @param userLogin
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<SystemEvent> getByUserLogin(String userLogin, int startIndex, int numberResult) {
		return SystemEventDaoUtil.getByUserLogin(userLogin, startIndex, numberResult);
	}
	
	/**
	 * @param objectName
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<SystemEvent> getByObjectName(String objectName, int startIndex, int numberResult) {
		return SystemEventDaoUtil.getByObjectName(objectName, startIndex, numberResult);
	}
	
	/**
	 * @param objectName
	 * @param objectId
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<SystemEvent> getByObjectNameAndId(String objectName, String objectId, int startIndex, int numberResult)  {
		return SystemEventDaoUtil.getByObjectNameAndId(objectName, objectId, startIndex, numberResult);
	}
	
	public static List<SystemEvent> getByClassNameAndId(Class<?> clazz, String objectId, int startIndex, int numberResult)  {
		return SystemEventDaoUtil.getByObjectNameAndId(clazz.getSimpleName(), objectId, startIndex, numberResult);
	}

	/**
	 * @param filter
	 * @return
	 */
	public static JsonDataTablePayload filter(SystemEventFilter filter) {
		return SystemEventDaoUtil.listByFilter(filter);
	}
}
