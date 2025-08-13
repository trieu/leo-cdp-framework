package leotech.cdp.job.reactive;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.model.customer.ProfileSingleView;

/**
 * single-view processing reactive job (triggered by external event) <br>
 * used by MergeDuplicateProfilesJob to update and recompute all event data for profile 
 * 
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class JobUpdateProfileSingleView extends ReactiveProfileDataJob<String> {

	private static ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private static volatile JobUpdateProfileSingleView instance = null;
	static ConcurrentHashMap<String, Boolean> profileIdMap = new ConcurrentHashMap<>();
	
	public static JobUpdateProfileSingleView job() {
		if(instance == null) {
			instance = new JobUpdateProfileSingleView();
		}
		return instance;
	}

	protected JobUpdateProfileSingleView() {
		super();
	}
	
	@Override
	public void processData(final String profileId) {
		// to avoid duplicated job, only process a single profile
		if( ! profileIdMap.containsKey(profileId)) {
			profileIdMap.put(profileId, true);
			executor.execute(new Runnable() {
				@Override
				public void run() {
					ProfileSingleView profile = ProfileDaoUtil.getProfileById(profileId);
					if (profile != null) {
						ProfileDataManagement.updateProfileSingleDataView(profile, true, new ArrayList<>(0));
						profileIdMap.remove(profileId);
					}
				}
			});
		}
	}

	@Override
	public void processDataQueue(int batchSize) {
		for (int i = 0; i < batchSize; i++) {
			String profileId = dataQueue.poll();
			if (profileId == null) {
				break;
			} 
			else {
				processData(profileId);
			}
		}
	}
}
