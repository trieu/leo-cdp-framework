package leotech.cdp.model.journey;

import com.google.gson.Gson;

import leotech.cdp.model.analytics.TouchpointHubReport;

public final class JourneyNodeLink {
	int source;
	int target;
	long value = 1;
	
	long visitor = 0, lead = 0, prospect = 0, newCustomer = 0, engagedCustomer = 0, churnedProfile = 0;

	public JourneyNodeLink() {
	}

	public JourneyNodeLink(int source, int target, TouchpointHubReport report, long defaultValue) {
		super();
		
		this.visitor = report.getVisitor();
		this.lead = report.getLead();
		this.prospect = report.getProspect();
		
		this.newCustomer = report.getNewCustomer();
		this.engagedCustomer = report.getEngagedCustomer();
		this.churnedProfile = report.getChurnedProfile();
		
		this.source = source;
		this.target = target;
		
		this.value = report.getEventCount(defaultValue);
	}
	
	public JourneyNodeLink(int source, int target, long value) {
		super();
		this.source = source;
		this.target = target;
		//this.value = value;
	}

	public int getSource() {
		return source;
	}

	public void setSource(int source) {
		this.source = source;
	}

	public int getTarget() {
		return target;
	}

	public void setTarget(int target) {
		this.target = target;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}