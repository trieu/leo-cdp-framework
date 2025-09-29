package leotech.cdp.data.service;

import leotech.cdp.data.DataServiceJob;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.Touchpoint;
import rfx.core.util.StringPool;

/**
 * read event data, profile data, update cdp_profile2content and cdp_profile2product
 * 
 * @author tantrieuf31
 * @since 2023
 *
 */
public class InternalAgentActivation extends DataServiceJob {

	
	public InternalAgentActivation() {
		super();
	}

	@Override
	protected void initConfigs() {
		
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
	protected String processProfile(Profile profile) {
		
		System.out.println(" InternalAgentActivation.processProfileData id: " + profile.getId() + " FullName " + profile.getFullName());
		// TODO 
	    return StringPool.BLANK;
	}


	@Override
	protected String processTouchpoint(Touchpoint touchpoint) {
		// skip
		return StringPool.BLANK;
	}

	@Override
	protected String processTouchpointHub(String touchpointHubId, String touchpointHubName, long touchpointHubSize) {
		// skip
		return StringPool.BLANK;
	}

}