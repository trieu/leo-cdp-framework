package leotech.cdp.model.analytics;

public class ProfileStatisticsView {

	long totalProfile = 0, totalVisitor = 0, totalInactiveVisitor = 0,
			totalCustomerProfile = 0, totalContactProfile = 0, totalSegments = 0;

	public ProfileStatisticsView() {
		
	}

	public long getTotalProfile() {
		return totalProfile;
	}

	public void setTotalProfile(long totalProfile) {
		this.totalProfile = totalProfile;
	}

	public long getTotalVisitor() {
		return totalVisitor;
	}

	public void setTotalVisitor(long totalVisitor) {
		this.totalVisitor = totalVisitor;
	}

	public long getTotalInactiveVisitor() {
		return totalInactiveVisitor;
	}

	public void setTotalInactiveVisitor(long totalInactiveVisitor) {
		this.totalInactiveVisitor = totalInactiveVisitor;
	}

	public long getTotalCustomerProfile() {
		return totalCustomerProfile;
	}

	public void setTotalCustomerProfile(long totalCustomerProfile) {
		this.totalCustomerProfile = totalCustomerProfile;
	}

	public long getTotalContactProfile() {
		return totalContactProfile;
	}

	public void setTotalContactProfile(long totalContactProfile) {
		this.totalContactProfile = totalContactProfile;
	}

	public long getTotalSegments() {
		return totalSegments;
	}

	public void setTotalSegments(long totalSegments) {
		this.totalSegments = totalSegments;
	}
	
	
	
}
