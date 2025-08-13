package leotech.cdp.model.customer;

/**
 * the callback to update profile when import or save
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public abstract class UpdateProfileCallback implements Runnable {

	protected Profile profile;
	
	public UpdateProfileCallback() {
		
	}
	
	public UpdateProfileCallback(Profile profile) {
		super();
		this.profile = profile;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}
	
	abstract public void processProfile();

	@Override
	public void run() {
		if(this.profile != null) {
			processProfile();
		} else {
			System.err.println("UpdateProfileCallback.profile is NULL !");
		}
	}
	
	public final void setDataAndRun(Profile profile) {
		this.profile = profile;
		processProfile();
	}
}
