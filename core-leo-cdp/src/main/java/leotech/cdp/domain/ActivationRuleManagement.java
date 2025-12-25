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
import leotech.cdp.model.activation.ActivationRuleJobRunner;
import leotech.cdp.model.activation.ActivationRuleType;
import leotech.cdp.model.activation.Agent;
import leotech.cdp.model.journey.EventObserver;
import leotech.system.domain.AirflowService;
import leotech.system.util.LogUtil;
import leotech.system.util.ReflectionUtil;

/**
 * the logic management of segment activation: SENDING_EMAIL,
 * SENDING_NOTIFICATION, SENDING_SMS, SYNCH_DATA and WRITE_DATA
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
		if (a != null) {
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
		if (a != null) {
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
		if (activationRule != null) {
			EventObserver eventObserver = EventObserverManagement.getDefaultDataObserver();
			Map<String, String> accessTokens = eventObserver.getAccessTokens();
			String dataServiceId = activationRule.getAgentId();
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
	public static List<ActivationRule> getAllActiveActivationRulesByAgentId(String serviceId) {
		return ActivationRuleDao.getAllActiveActivationRulesForDataService(serviceId);
	}

	/**
	 * @param segmentId
	 * @return
	 */
	public static List<ActivationRule> getAllActivationRulesForSegment(String segmentId) {
		return ActivationRuleDao.getAllActivationRulesForSegment(segmentId);
	}

	/**
	 * @param ar
	 * @param action
	 */
	protected static void processActionForActivationRules(ActivationRule ar, String action) {
		Agent service = AgentDaoUtil.getById(ar.getAgentId());

		// validation
		if (service == null) {
			System.err.println("DataService is null for ID: " + ar.getAgentId());
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
	public static void startDataServices() {
		try {
			if (jobScheduler == null) {
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
	static void loadActiveDataServicesAndTrigger(Scheduler scheduler) {
		try {
			List<Agent> dataServices = AgentManagement.getAllActiveAgents();
			LogUtil.logInfo(ActivationRuleManagement.class,
					"TimerTask.startAllActiveDataServices: " + dataServices.size() + " dataServices");
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
		if (jobScheduler != null) {
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
		if (jobDetail != null && jobScheduler != null) {
			try {
				LogUtil.logInfo(ActivationRuleManagement.class, "deleteScheduledJob scheduledJobId " + scheduledJobId);
				jobScheduler.deleteJob(jobDetail.getKey());
			} catch (SchedulerException e) {
				LogUtil.logError(ActivationRuleManagement.class, e.getMessage());
			}
		}
	}

	/**
	 * Start data service and schedule all activation rules
	 *
	 * @param scheduler Quartz scheduler
	 * @param agent     Data service agent
	 */
	static void triggerDataService(Scheduler scheduler, Agent agent) {

		try {
			// =========================
			// Mark agent as started
			// =========================
			agent.setStartedAt(new Date());
			AgentManagement.save(agent, false);

			String agentId = agent.getId();

			List<ActivationRule> activationRules = ActivationRuleManagement
					.getAllActiveActivationRulesByAgentId(agentId);

			LogUtil.logInfo(ActivationRule.class,
					"Load ActivationRules | agentId=" + agentId + " size=" + activationRules.size());

			// =========================
			// Schedule each rule
			// =========================
			for (ActivationRule rule : activationRules) {

				String activationJobId = agentId + "-" + rule.getId();

				// already scheduled?
				if (MAP_ACTIVATION_JOBS.containsKey(activationJobId)) {
					LogUtil.logInfo(ActivationRule.class, "Skip existing job | activationJobId=" + activationJobId
							+ " timeToStart=" + rule.getTimeToStart());
					continue;
				}

				LogUtil.logInfo(ActivationRule.class,
						"Scheduling rule | name=" + rule.getName() + " id=" + rule.getId());

				// =========================
				// Build Job + Trigger
				// =========================
				JobDetail jobDetail = ActivationRuleJobRunner.buildJobDetail(activationJobId, rule, agent);

				Trigger trigger = ActivationRuleJobRunner.buildTrigger(rule);

				// =========================
				// Schedule with Quartz
				// =========================
				Date scheduledDate = scheduler.scheduleJob(jobDetail, trigger);

				LogUtil.logInfo(ActivationRule.class,
						"Job scheduled | activationJobId=" + activationJobId + " scheduledAt=" + scheduledDate);

				// =========================
				// Cache locally
				// =========================
				MAP_ACTIVATION_JOBS.put(activationJobId, jobDetail);
			}

		} catch (Exception e) {
			LogUtil.logError(ActivationRule.class, "Failed to trigger data service | agentId=" + agent.getId(), e);
		}
	}

}
