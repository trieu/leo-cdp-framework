package leotech.cdp.data.service;

import java.util.Map;

import leotech.cdp.data.DataServiceJob;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.Touchpoint;
import leotech.system.model.SystemService;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 * @since 2023
 *
 */
public class ComputeCLVServiceService extends DataServiceJob {

	String serviceProvider;
	
	public ComputeCLVServiceService() {
		super();
	}

	@Override
	protected void initConfigs() {
		Map<String, Object> configs = this.dataService.getConfigs();
		this.serviceProvider = StringUtil.safeString(configs.get(SystemService.SERVICE_PROVIDER),"");
	}
	
	@Override
	protected String doMyJob() {
		return this.processSegmentDataActivation();
	}

	@Override
	protected String processSegment(String segmentId, String segmentName, long segmentSize) {
		String remoteSegmentName = getSegmentNameForActivationList(segmentName);
		System.out.println("processSegmentData CDP remoteSegmentName " + remoteSegmentName + " segmentSize " + segmentSize);
		processProfilesInSegment(segmentId, segmentSize);
		return done(segmentId, segmentSize);
	}

	@Override
	protected String processProfileData(Profile profile) {
		
		System.out.println(" ComputeCLVServiceDataService.processProfileData id: " + profile.getId() + " FullName " + profile.getFullName());
		// TODO 
	    return StringPool.BLANK;
	}

	@Override
	protected String processTouchpointData(Touchpoint touchpoint) {
		// skip
		return StringPool.BLANK;
	}

	@Override
	protected String processTouchpointHub(String touchpointHubId, String touchpointHubName, long touchpointHubSize) {
		// skip
		return StringPool.BLANK;
	}

}