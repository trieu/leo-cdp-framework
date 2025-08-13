package leotech.cdp.job.scheduled;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.model.customer.ProfileSingleView;
import rfx.core.job.ScheduledJob;
import rfx.core.util.Utils;

/**
 * 
 * recompute tracking event of profile, update ProfileSingleView score
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public class ProfileSingleViewAllDataJob  extends ScheduledJob {
	
	@Override
	public void doTheJob() {
		
		int startIndex = 0;
		int numberResult = 250;
		
		List<ProfileSingleView> profiles = ProfileDaoUtil.listSingleViewAllWithPagination(startIndex, numberResult);
		AtomicInteger count = new AtomicInteger();
		while ( ! profiles.isEmpty() ) {
			profiles.parallelStream().forEach( profile -> {
				boolean ok = ProfileDataManagement.updateProfileSingleDataView(profile, true, null);
				if(ok) {
					int c = count.incrementAndGet();
					System.out.println(c+" ProfileSingleViewAllDataJob id: "+profile.getId());
				}
			});
			
			//loop to the end of database
			startIndex = startIndex + numberResult;
			profiles = ProfileDaoUtil.listSingleViewAllWithPagination(startIndex, numberResult);
		}
		System.out.println("Total processed profile: " + count);
	}
	
	public static void main(String[] args) {
		new ProfileSingleViewAllDataJob().doTheJob();
		Utils.exitSystemAfterTimeout(3000);
	}

}
