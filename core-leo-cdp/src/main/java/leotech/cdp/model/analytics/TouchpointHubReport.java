package leotech.cdp.model.analytics;

import com.google.gson.Gson;

import leotech.cdp.model.journey.TouchpointHub;
import rfx.core.util.StringUtil;

/**
 * The report object for touchpoint hub
 * 
 * @author tantrieuf31
 * @since 2022
 */
public final class TouchpointHubReport extends ReportProfileEvent {

	String touchpointHubId;
	String name = "Unknown Source";
	long visitor = 0, lead = 0, prospect = 0, newCustomer = 0, engagedCustomer = 0, churnedProfile = 0;
	
	public TouchpointHubReport() {
	}
	
	public TouchpointHubReport(long totalProfile) {
		this.profileCount = totalProfile;
	}
	
	public TouchpointHubReport(TouchpointHub touchpointHub, long profileCount) {
		this.touchpointHubId = touchpointHub.getId();
		this.name = touchpointHub.getName();
		this.profileCount = profileCount;
	}

	public String getTouchpointHubId() {
		return StringUtil.safeString(touchpointHubId);
	}

	public void setTouchpointHubId(String touchpointHubId) {
		this.touchpointHubId = touchpointHubId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getVisitor() {
		return visitor;
	}

	public long getLead() {
		return lead;
	}

	public long getProspect() {
		return prospect;
	}

	public long getNewCustomer() {
		return newCustomer;
	}

	public long getEngagedCustomer() {
		return engagedCustomer;
	}

	public long getChurnedProfile() {
		return churnedProfile;
	}
	
	
	
	public void setVisitor(long visitor) {
		if(visitor > 0) {
			this.visitor += visitor;
		}
	}

	public void setLead(long lead) {
		if(lead > 0) {
			this.lead += lead;
		}
	}

	public void setProspect(long prospect) {
		if(prospect > 0) {
			this.prospect += prospect;
		}
	}

	public void setNewCustomer(long newCustomer) {
		if(newCustomer > 0) {
			this.newCustomer += newCustomer;
		}
	}

	public void setEngagedCustomer(long engagedCustomer) {
		if(engagedCustomer > 0) {
			this.engagedCustomer += engagedCustomer;
		}
	}

	public void setChurnedProfile(long churnedProfile) {
		if(churnedProfile > 0) {
			this.churnedProfile += churnedProfile;
		}
		
	}

	/**
	 * to update report data (visitor,lead,prospect,newCustomer,engagedCustomer,churnedProfile) and compute profileCount
	 * 
	 * @param touchpointHubReport
	 */
	public final void updateReport(TouchpointHubReport rp) {
		setVisitor(rp.getVisitor());
		setLead(rp.getLead());
		setProspect(rp.getProspect());
		
		setNewCustomer(rp.getNewCustomer());
		setEngagedCustomer(rp.getEngagedCustomer());
		setChurnedProfile(rp.getChurnedProfile());
		
		this.profileCount = this.visitor + this.lead + this.prospect + this.newCustomer + this.engagedCustomer + this.churnedProfile;
	}
	
	
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
