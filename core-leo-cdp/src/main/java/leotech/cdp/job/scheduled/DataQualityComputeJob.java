package leotech.cdp.job.scheduled;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.model.customer.ProfileSingleView;
import rfx.core.job.ScheduledJob;

/**
 * Data Quality Compute Job must be called by SuperAdmin only
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class DataQualityComputeJob extends ScheduledJob {


	@Override
	public void doTheJob() {
		
		int startIndex = 0;
		int numberResult = 200;
		
		List<ProfileSingleView> profiles = ProfileDaoUtil.listSingleViewAllWithPagination(startIndex, numberResult, true);
		AtomicInteger count = new AtomicInteger();
		while ( ! profiles.isEmpty() ) {
			profiles.parallelStream().forEach(profile->{
				// save database
				boolean ok = ProfileDaoUtil.saveProfile(profile) != null;
				if(ok) {
					int c = count.incrementAndGet();
					System.out.println(c+" DataQualityComputeJob profile.id: "+profile.getId());
				}
			});
			
			//loop to the end of database
			startIndex = startIndex + numberResult;
			profiles = ProfileDaoUtil.listSingleViewAllWithPagination(startIndex, numberResult, true);
		}
		System.out.println("Total processed profile: " + count);
	}
}
