package leotech.cdp.domain;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import leotech.cdp.dao.DataServiceDaoUtil;
import leotech.cdp.model.activation.ActivationRule;
import leotech.cdp.model.activation.ActivationRuleType;
import leotech.cdp.model.activation.DataService;
import leotech.cdp.model.customer.Segment;
import leotech.system.exception.InvalidDataException;
import leotech.system.model.SystemUser;
import rfx.core.util.FileUtils;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * data service management
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class DataServiceManagement {

	public static final String INIT_DATA_SERVICE_JSON = "./resources/data-for-new-setup/init-data-service-configs.json";


	/**
	 * @return List<DataService> from file
	 */
	static List<DataService> loadFromJsonFile() {
		List<DataService> list = null;
		try {
			Type listType = new TypeToken<ArrayList<DataService>>() {
			}.getType();
			String json = FileUtils.readFileAsString(INIT_DATA_SERVICE_JSON);
			list = new Gson().fromJson(json, listType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (list == null) {
			list = new ArrayList<>(0);
		}
		return list;
	}

	/**
	 * init default data for database
	 */
	public static void initDefaultSystemData(boolean overideOldData) {
		try {
			List<DataService> list = loadFromJsonFile();
			for (DataService service : list) {
				if (service.getStatus() >= 0) {
					DataServiceManagement.save(service, overideOldData);
					Utils.sleep(500);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * save the service
	 * 
	 * @param service
	 * @param overideOldData
	 * @return
	 */
	public static String save(DataService service, boolean overideOldData) {
		return DataServiceDaoUtil.save(service, overideOldData);
	}

	/**
	 * get the service
	 * 
	 * @param id
	 * @return DataService
	 */
	public static DataService getById(String id) {
		return DataServiceDaoUtil.getDataServiceById(id);
	}

	/**
	 * delete the service
	 * 
	 * @param id
	 * @return
	 */
	public static String deleteById(String id) {
		return DataServiceDaoUtil.deleteDataServiceById(id);
	}

	/**
	 * get all active dataServices in database
	 * 
	 * @return List<DataService>
	 */
	public static List<DataService> getAllActiveDataServices() {
		return DataServiceDaoUtil.getAllActiveDataServices();
	}

	/**
	 * @param keywords
	 * @return
	 */
	public static List<DataService> getDataServices(int startIndex, int numberResult, String keywords,
			String filterServiceValue, boolean forSynchronization, boolean forDataEnrichment,
			boolean forPersonalization) {
		return DataServiceDaoUtil.getDataServices(startIndex, numberResult, keywords, filterServiceValue,
				forSynchronization, forDataEnrichment, forPersonalization);
	}

	/**
	 * open the service, then activate segment data with an event
	 * 
	 * @param purpose
	 * @param dataServiceId
	 * @param delayMinutes
	 * @param schedulingTime
	 * @param segmentId
	 * @param eventName
	 * @return
	 */
	public static String activateDataService(SystemUser loginUser, String purpose, String dataServiceId,
			String timeToStart, int schedulingTime, String segmentId, String triggerEventName) {
		String userLogin = loginUser.getUserLogin();

		DataService service = DataServiceDaoUtil.getDataServiceById(dataServiceId);
		Segment segment = SegmentDataManagement.getSegmentWithActivationRulesById(segmentId);
		if (service != null && segment != null) {
			if (service.isReadyToRun()) {
				String activationType = ActivationRuleType.RUN_DATA_SERVICE;
				int priority = segment.getIndexScore();
				String defaultName = StringUtil.join("-", purpose, dataServiceId, segmentId);
				String description = "";
				ActivationRule activationRule = ActivationRule.create(userLogin, purpose, activationType, defaultName,
						description, priority, dataServiceId, segmentId, timeToStart, schedulingTime, triggerEventName);
				String activationRuleId = ActivationRuleManagement.save(activationRule);
				if (activationRuleId != null) {
					return activationRuleId;
				} else {
					throw new InvalidDataException(
							"Can not create DataServiceScheduler from serviceId: " + dataServiceId);
				}
			} else {
				throw new InvalidDataException(service.getId() + " is not ready to run, please set service_api_key");
			}
		} else {
			throw new InvalidDataException(" In-valid segmentId:" + segmentId + " or In-valid serviceId: " + dataServiceId);
		}
	}

}
