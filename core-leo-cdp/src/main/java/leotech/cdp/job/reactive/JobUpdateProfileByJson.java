package leotech.cdp.job.reactive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.handler.api.DataApiPayload;
import leotech.system.version.SystemMetaData;

/**
 * single-view processing reactive job for API
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class JobUpdateProfileByJson extends ReactiveProfileDataJob<DataApiPayload> {

	static Logger logger = LoggerFactory.getLogger(JobUpdateProfileByJson.class);
	private static final int SCHEDULED_TIME = 2000; // milisecs

	private static final int SAVE_PROFILE_BATCH_SIZE = 40;

	private static ExecutorService executor = Executors.newFixedThreadPool(SystemMetaData.NUMBER_CORE_CPU);

	private static volatile JobUpdateProfileByJson instance = null;

	public static JobUpdateProfileByJson job() {
		if (instance == null) {
			instance = new JobUpdateProfileByJson();
		}
		return instance;
	}

	protected JobUpdateProfileByJson() {
		super(SAVE_PROFILE_BATCH_SIZE, SCHEDULED_TIME);
	}


	/**
	 * the job handler for observed event
	 */
	@Override
	public void processData(final DataApiPayload data) {
		// local queue and update directly
		executor.execute(new Runnable() {
			@Override
			public void run() {
				ProfileDataManagement.saveProfileFromApi(data);
			}
		});
	}

	/**
	 * 
	 */
	@Override
	public void processDataQueue(int batchSize) {
		for (int i = 0; i < batchSize; i++) {
			DataApiPayload data = dataQueue.poll();
			if (data == null) {
				break;
			} else {
				this.processData(data);
			}
		}
	}
}
