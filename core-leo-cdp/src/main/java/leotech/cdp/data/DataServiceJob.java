package leotech.cdp.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import leotech.cdp.dao.ActivationRuleDao;
import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.domain.ActivationRuleManagement;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.model.activation.ActivationRule;
import leotech.cdp.model.activation.DataService;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.journey.Touchpoint;
import leotech.system.domain.SystemEventManagement;
import leotech.system.model.SystemService;
import leotech.system.util.LogUtil;
import rfx.core.util.StringUtil;

/**
 * abstract class for data service job, all implementation should extend this class
 * 
 * @author tantrieuf31
 * @since 2023
 *
 */
public abstract class DataServiceJob implements Job {

	private static final String NOTHING = "nothing";
	public static final String EVENT_RUN_DEFAULT_JOB = "run_default_job";
	protected final static int BATCH_SIZE = 200;
	public final static String CDP_SEGMENT_PREFIX = "[CDP] ";
	
	public final static int DELAY_TO_START = 3000;
	
	private AtomicBoolean working = new AtomicBoolean(false);
	
	protected String activationJobId;
	protected ActivationRule activationRule;
	protected DataService dataService;
	
	protected String name = NOTHING;
	protected String apiKey = "";
	protected List<String> synchronizedFields = new ArrayList<String>(300);
	
	public DataServiceJob() {
	}
	
	public void validateAndInit() throws IllegalArgumentException {		 
		validate();
		if(NOTHING.equals(this.name)){			
			this.name = dataService.getName();
			this.initApiKey();	
		}
	}

	protected void validate() {
		if(this.activationJobId == null) {
			throw new IllegalArgumentException("activationJobId can not be null");
		}
		
		if(this.dataService == null) {
			throw new IllegalArgumentException("dataService can not be null");
		}
		if( ! this.dataService.isReadyToRun()) {
			throw new IllegalArgumentException("dataService is not active, ID: " + dataService.getId());
		}
		
		if(this.activationRule == null) {
			throw new IllegalArgumentException("activationRule can not be null");
		}
		
		String activationRuleId = activationRule.getId();
		if( ! this.activationRule.isActive()) {
			
			throw new IllegalArgumentException("activationRule is not active, ID: " + activationRuleId);
		}
		boolean check = ActivationRuleDao.isActivationRuleReadyToRun(activationRuleId);
		if(!check) {
			throw new IllegalArgumentException("activationRule is not ready to run " + activationRule.getName());
		}
	}
	
	/**
	 * load apiKey from dataService.getConfigs
	 */
	private final void initApiKey() {
		this.apiKey = this.dataService.getConfigs().getOrDefault(SystemService.SERVICE_API_KEY, "").toString();
		if(StringUtil.isNotEmpty(apiKey)) {
			this.initConfigs();
		}
		else {
			throw new IllegalArgumentException(SystemService.SERVICE_API_KEY + " must be setup at "+ this.dataService.getId());
		}
	}
	
	public final static String getSegmentNameForActivationList(String name) {
		return CDP_SEGMENT_PREFIX + name;
	}
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			// init data to execute job
			this.validateAndInit();
			
			if( ! isWorking() ) {
				// beginning
				this.startUp();
				
				// doing
				LogUtil.logInfo(this.getClass(), this.getClass().getName() + " run at: " + new Date());
				String rs = this.doMyJob();
				log("Result: " + rs);
				
				// ending
				this.shutDown();
			}
		} catch (Exception e) {
			if(e instanceof IllegalArgumentException) {
				ActivationRuleManagement.deleteScheduledJob(activationJobId);
			}
			else {
				e.printStackTrace();
			}
		}
	}
	
	protected void startUp() {
		dataValidation();
		this.working.set(true);
		System.out.println("startUp DataServiceScheduler.name " + name);
		log("startUp");
	}

	protected void shutDown() {
		this.working.set(false);
		System.out.println("shutDown DataServiceScheduler.name " + name);
		log("shutDown");
	}
	
	// init for sub class
	abstract protected void initConfigs();
	abstract protected String doMyJob();
	
	// processing logic for sub class
	abstract protected String processSegment(String segmentId, String segmentName, long segmentSize);
	abstract protected String processProfileData(Profile profile);
	
	abstract protected String processTouchpointHub(String touchpointHubId, String touchpointHubName, long touchpointHubSize);
	abstract protected String processTouchpointData(Touchpoint touchpoint);
	
	/**
	 * @return
	 */
	protected final String processSegmentDataActivation() {
		long size = 0;
		long beginTime = System.currentTimeMillis();
		List<ActivationRule> rules = ActivationRuleManagement.getAllActiveActivationRulesByServiceId(this.dataService.getId());
		for (ActivationRule rule : rules) {
			String segmentId = rule.getSegmentId();
			Segment segment = SegmentDataManagement.getSegmentById(segmentId);
			String segmentName = segment.getName();
			size = segment.getTotalCount();
			if (size > 0) {
				try {
					String rs = this.processSegment(segmentId, segmentName, size);
					LogUtil.logInfo(this.getClass(), rs);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		long processedTime = System.currentTimeMillis() - beginTime;
		String rs = "processed segment size " + size + " in millisecond: " + processedTime;
		return rs;
	}
	
	/**
	 * @param segmentId
	 * @param segmentSize
	 */
	protected final void processProfilesInSegment(String segmentId, long segmentSize) {
		Segment segment = SegmentDaoUtil.getSegmentById(segmentId);
		SegmentDataManagement.processProfilesInSegment(segment, BATCH_SIZE, (Profile profile)->{
			processProfileData(profile);
		});
	}

	/**
	 * @return
	 */
	public final DataService getDataService() {
		dataValidation();
		return dataService;
	}
	
	/**
	 * 
	 */
	protected final void dataValidation() {
		if(dataService == null) {
			throw new IllegalArgumentException("dataService is NULL");
		}
		if(activationRule == null) {
			throw new IllegalArgumentException("activationRule is NULL");
		}
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isWorking() {
		return working.get();
	}

	public void setWorking(boolean working) {
		this.working.set(working);
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setDataService(DataService dataService) {
		this.dataService = dataService;
	}
	
	public void setActivationRule(ActivationRule activationRule) {
		this.activationRule = activationRule;
	}

	public String getActivationJobId() {
		return activationJobId;
	}

	public void setActivationJobId(String activationJobId) {
		this.activationJobId = activationJobId;
	}

	public AtomicBoolean getWorking() {
		return working;
	}

	public List<String> getSynchronizedFields() {
		return synchronizedFields;
	}

	public void setSynchronizedFields(List<String> synchronizedFields) {
		if(synchronizedFields != null) {
			this.synchronizedFields = synchronizedFields;
		}
	}

	public void log(String data) {
		String objectName = ActivationRule.class.getSimpleName();
		String objectId = this.activationRule.getId();
		String actionUri = this.dataService.getId();
		SystemEventManagement.dataJobLog("127.0.0.1",objectName, objectId, actionUri, data );
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+".dataService " +this.name;
	}
	
	protected String done(String segmentId, long segmentSize) {
		return "process segmentId: "+segmentId + " segmentSize: " + segmentSize;
	}
}
