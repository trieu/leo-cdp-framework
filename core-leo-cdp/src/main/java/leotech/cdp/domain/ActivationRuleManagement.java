package leotech.cdp.domain;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import leotech.cdp.dao.ActivationRuleDao;
import leotech.cdp.dao.AgentDaoUtil;
import leotech.cdp.model.activation.ActivationRule;
import leotech.cdp.model.activation.ActivationRuleType;
import leotech.cdp.model.activation.Agent;
import leotech.cdp.model.journey.EventObserver;
import leotech.system.domain.AirflowService;
import leotech.system.util.LogUtil;
import leotech.system.util.ReflectionUtil;

/**
 *  the logic management of segment activation: SENDING_EMAIL, SENDING_NOTIFICATION, SENDING_SMS, SYNCH_DATA and WRITE_DATA
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public final class ActivationRuleManagement {
	
	private static final int TIME_REFRESH_AGENTS = 10000;
	static final String WRITE_DATA_FOLDER = "write_data_folder";
	static final int BASE_1_SECOND = 1000;
	static final int BASE_1_HOUR = 3600 * BASE_1_SECOND;
	
	final static Map<String, JobDetail> MAP_ACTIVATION_JOBS = new HashMap<>();
	final static Map<String, String> ACTIVATION_TYPES = new HashMap<>(20);
	
	static {
		ACTIVATION_TYPES.putAll(ReflectionUtil.getConstantStringMap(ActivationRuleType.class));
	}
	
	public static Map<String, String> getActivationTypeValues() {
		return ACTIVATION_TYPES;
	}
	
	private static Scheduler jobScheduler = null;
	private static Timer refreshDataTimer = null;

	
	/**
	 * save ActivationRule
	 * 
	 * @param rule
	 * @return saved ID
	 */
	public static String save(ActivationRule rule) {
		return ActivationRuleDao.save(rule);
	}
	
	/**
	 * @param activationRuleId
	 * @return
	 */
	public static boolean stopActivation(String activationRuleId) {
		ActivationRule a = ActivationRuleDao.getActivationRuleById(activationRuleId);
		if(a != null) {
			a.setActive(false);
			return save(a) != null;
		}
		return false;
	}
	
	/**
	 * @param activationRuleId
	 * @return
	 */
	public static boolean startActivation(String activationRuleId) {
		ActivationRule a = ActivationRuleDao.getActivationRuleById(activationRuleId);
		if(a != null) {
			a.setActive(true);
			return save(a) != null;
		}
		return false;
	}
	
	
	/**
	 * @param activationRuleId
	 * @return
	 */
	public static boolean manuallyRunActivation(String activationRuleId) {
		ActivationRule activationRule = ActivationRuleDao.getActivationRuleById(activationRuleId);
		if(activationRule != null) {
			EventObserver eventObserver = EventObserverManagement.getDefaultDataObserver();
			Map<String, String> accessTokens = eventObserver.getAccessTokens();
			String dataServiceId = activationRule.getDataServiceId();
			String segmentId = activationRule.getSegmentId();
			Agent service = AgentManagement.getById(dataServiceId);
			String dagId = service.getDagId();
			Map<String, Object> params = service.buildConfParamsAirflowDagForSegment(segmentId, accessTokens);
			boolean ok = AirflowService.triggerDagJob(dagId, params);
			return ok;
		}
		return false;
	}
	
	
	/**
	 * @param activationRuleId
	 * @return
	 */
	public static boolean removeActivation(String id) {
		return ActivationRuleDao.removeActivationRuleById(id);
	}
	
	/**
	 * delete ActivationRule
	 * 
	 * @param rule
	 * @return deleted ID
	 */
	public static void delete(ActivationRule rule) {
		ActivationRuleDao.delete(rule);
	}

	/**
	 * get all active activation rules to start data service service
	 * 
	 * @param serviceId
	 * @return
	 */
	public static List<ActivationRule> getAllActiveActivationRulesByServiceId(String serviceId){
		return ActivationRuleDao.getAllActiveActivationRulesForDataService(serviceId);
	}
	
	/**
	 * @param segmentId
	 * @return
	 */
	public static List<ActivationRule> getAllActivationRulesForSegment(String segmentId){
		return ActivationRuleDao.getAllActivationRulesForSegment(segmentId);
	}
	
	
	
	/**
	 * @param ar
	 * @param action
	 */
	protected static void processActionForActivationRules(ActivationRule ar, String action) {
		Agent service = AgentDaoUtil.getById(ar.getDataServiceId());
		
		// validation
		if (service == null) {
			System.err.println("DataService is null for ID: " + ar.getDataServiceId());
			return;
		}
		if (!service.isReadyToRun()) {
			System.err.println("DataService is not ready to run, name: " + service.getName());
			return;
		}

		// TODO check and run
		System.out.println("==> WRITE_DATA " + ar.getName());
	}
	
	/**
	 * start Data Services as job scheduler
	 * 
	 * @return
	 * @throws SchedulerException 
	 */
	public static void startScheduledJobsForAllDataServices()  {	
		try {
			if(jobScheduler == null) {
				jobScheduler = StdSchedulerFactory.getDefaultScheduler();
			}
			refreshDataTimer = new Timer(true);
			refreshDataTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					loadActiveDataServicesAndTrigger(jobScheduler);
				}
			}, 3000, TIME_REFRESH_AGENTS);
		} catch (SchedulerException e) {
			LogUtil.logError(ActivationRuleManagement.class, e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	static void loadActiveDataServicesAndTrigger(Scheduler scheduler)  {
		try {
			List<Agent> dataServices = AgentManagement.getAllActiveAgents();
			LogUtil.logInfo(ActivationRuleManagement.class, "TimerTask.startAllActiveDataServices: " + dataServices.size() + " dataServices");
			for (Agent service : dataServices) {					
				triggerDataService(scheduler, service);
			}
			scheduler.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * shutdownScheduler of 
	 */
	public static void shutdownScheduler() {
		if(jobScheduler != null) {
			try {
				MAP_ACTIVATION_JOBS.clear();
				jobScheduler.shutdown();
			} catch (SchedulerException e) {
				LogUtil.logError(ActivationRuleManagement.class, e.getMessage());
			}
		}
	}
	
	/**
	 * @param activationJobId
	 */
	public static void deleteScheduledJob(String scheduledJobId) {
		JobDetail jobDetail = MAP_ACTIVATION_JOBS.remove(scheduledJobId);
		if(jobDetail != null && jobScheduler != null) {
			try {
				LogUtil.logInfo(ActivationRuleManagement.class, "deleteScheduledJob scheduledJobId " + scheduledJobId);
				jobScheduler.deleteJob(jobDetail.getKey());
			} catch (SchedulerException e) {
				LogUtil.logError(ActivationRuleManagement.class, e.getMessage());
			}
		}
	}
	
	


	/**
	 * to start data service
	 * 
	 * @param service
	 */
	static void triggerDataService(Scheduler scheduler, Agent service) {
		// ready to run job
		try {
			// the datetime to get all active activation rules and start
			service.setStartedAt(new Date());
			AgentManagement.save(service, false);
			
			String dataServiceId = service.getId();
			List<ActivationRule> activationRules = ActivationRuleManagement.getAllActiveActivationRulesByServiceId(dataServiceId);
			System.out.println(" getAllActiveActivationRulesByServiceId " + dataServiceId + " activations.size " + activationRules.size());
			
			for (ActivationRule activationRule : activationRules) {
				String activationJobId = service.getId() + "-" + activationRule.getId();
				
				JobDetail jobDetail = MAP_ACTIVATION_JOBS.get(activationJobId);
				if(jobDetail == null) {
					LogUtil.logInfo(ActivationRule.class, "rule name: " + activationRule.getName());
					
					// Job
					jobDetail = activationRule.getJobDetail(activationJobId, service);
					
					// Trigger
					Trigger trigger = activationRule.getJobTrigger();
					
					// Scheduler
					Date scheduledDate = scheduler.scheduleJob(jobDetail, trigger);
					LogUtil.logInfo(ActivationRule.class, "now: " + new Date() + " scheduledDate: " + scheduledDate);
					
					// save local Map
					MAP_ACTIVATION_JOBS.put(activationJobId, jobDetail);
				}
				else {
					LogUtil.logInfo(ActivationRule.class, "The job is not ready for Scheduler, skip activationJobId: " + activationJobId + " getTimeToStart: " +activationRule.getTimeToStart());
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
