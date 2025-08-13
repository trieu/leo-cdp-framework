package leotech.cdp.job.reactive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.domain.IdentityResolutionManagement;
import leotech.cdp.domain.IdentityResolutionManagement.ResolutioResult;
import leotech.cdp.domain.ProfileQueryManagement;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.system.model.SystemUser;

/**
 * to merge duplicated profiles into a Unique Profile in database
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class JobMergeDuplicatedProfiles {
	
	static Logger logger = LoggerFactory.getLogger(JobMergeDuplicatedProfiles.class);
	static int CPU_CORES = Runtime.getRuntime().availableProcessors();

	private static ExecutorService executor = Executors.newFixedThreadPool(CPU_CORES);
	
	public static void execute(Profile primaryProfile) {
		execute(primaryProfile, true);
	}
	
	public static void execute(Profile primaryProfile, boolean compareDataQualityScore) {
		if(primaryProfile.checkToStartMergeDataJob()) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					logger.info("Run Identity Resolution profileId " + primaryProfile.getId());
					IdentityResolutionManagement.profileDeduplication(primaryProfile);
				}
			});
		}
	}
	
	/**
	 * @param loginUser
	 * @param profileId
	 * @return DuplicateProfileResult
	 */
	public static ResolutioResult doDeduplicationManually(SystemUser loginUser, String profileId) {
		ProfileSingleView profile = ProfileQueryManagement.checkAndGetProfileById(loginUser, profileId);
		ResolutioResult c = IdentityResolutionManagement.profileDeduplication(profile);
		return c;
	}
}
