package leotech.cdp.query;

import java.util.ArrayList;
import java.util.List;

import leotech.cdp.model.customer.ProfileSingleView;

public class ProfileMatchingResult {

	List<ProfileSingleView> deterministic;
	List<ProfileSingleView> probabilistic;

	public ProfileMatchingResult() {
		
	}

	public List<ProfileSingleView> getDeterministic() {
		if (deterministic == null) {
			deterministic = new ArrayList<ProfileSingleView>(0);
		}
		return deterministic;
	}
	public void setDeterministic(List<ProfileSingleView> deterministic) {
		this.deterministic = deterministic;
	}
	public List<ProfileSingleView> getProbabilistic() {
		if (probabilistic == null) {
			probabilistic = new ArrayList<ProfileSingleView>(0);
		}
		return probabilistic;
	}
	public void setProbabilistic(List<ProfileSingleView> probabilistic) {
		this.probabilistic = probabilistic;
	}

	public ProfileSingleView getProfileByDeterministicProcessing() {
		List<ProfileSingleView> deterministicProfiles = getDeterministic();

		if (deterministicProfiles.size() > 0) {
			return deterministicProfiles.get(0);
		}

		return null;
	}

	/**
	 * choosing the best between deterministic Profiles and probabilistic Profiles
	 * 
	 * @return Profile
	 */
	public ProfileSingleView getBestMatchingProfile() {
		List<ProfileSingleView> deterministicProfiles = getDeterministic();
		List<ProfileSingleView> probabilisticProfiles = getProbabilistic();
		ProfileSingleView profileSingleView = null;;
		if (deterministicProfiles.size() > 0) {
			profileSingleView = deterministicProfiles.get(0);
		} 
		else if (probabilisticProfiles.size() > 0) {
			profileSingleView = probabilisticProfiles.get(0);
		}
		if(profileSingleView != null) {
			profileSingleView.unifyData();
		}
		return profileSingleView;
	}

}
