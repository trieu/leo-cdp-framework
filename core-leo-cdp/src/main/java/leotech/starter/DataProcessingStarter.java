package leotech.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import leotech.cdp.data.service.profile.ProfileDataPipelineJob;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.domain.schema.CustomerFunnel;
import leotech.cdp.job.scheduled.CdpEventProcessingJob;
import leotech.cdp.job.scheduled.CdpProductImportingJob;
import leotech.cdp.job.scheduled.CdpProductImportingJob.ProductImportingEvent;
import leotech.cdp.job.scheduled.CdpProfileProcessingJob;
import leotech.cdp.job.scheduled.CdpSegmentProcessingJob;
import leotech.cdp.job.scheduled.RefreshAllSegmentsJob;
import leotech.system.model.ImportingResult;
import leotech.system.util.LogUtil;
import leotech.system.util.TaskRunner;
import leotech.system.version.SystemMetaData;
import rfx.core.job.ScheduledJob;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * the starter instance, to start Apache Kafka consumers
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class DataProcessingStarter {

	public static final String PROFILE_DATA_PIPELINE_JOB = "ProfileDataPipelineJob";
	
	public static final String CDP_PROFILE_PROCESSING_JOB = "CdpProfileProcessingJob";
	public static final String CDP_EVENT_PROCESSING_JOB = "CdpEventProcessingJob";
	public static final String CDP_SEGMENT_PROCESSING_JOB = "CdpSegmentProcessingJob";
	
	public static final String CDP_PRODUCT_IMPORTING_JOB = "CdpProductImportingJob";
	public static final String CDP_PROFILE_IMPORTING_JOB = "CdpProfileImportingJob";
	
	public static final String CDP_REFRESH_SEGMENT_DATA_JOB = "CdpRefreshSegmentDataJob";
	public static final String CDP_REFRESH_ALL_SEGMENTS_JOB = "CdpRefreshAllSegmentsJob";
	
	
	static Logger logger = LoggerFactory.getLogger(DataProcessingStarter.class);
	static final Marker IMPORTANT = MarkerFactory.getMarker("IMPORTANT");
	static String className = DataProcessingStarter.class.getSimpleName();


	public static void main(String[] args) {
		SystemMetaData.initTimeZoneGMT();

		logger.info(IMPORTANT, className + " is started! ");

		int length = args.length;
		if (length >= 1) {
			String className = args[0];
			int partitionId = -1;
			int period = 0;

			logger.info("className " + className);
			
			ScheduledJob job = null;

			if(length == 5) {
				if(className.startsWith(CDP_PROFILE_IMPORTING_JOB)) {
					String importFileUrl = args[1];
					String observerId =  args[2];
					String dataLabelsStr =  args[3];
					boolean overwriteOldData =  args[4] == "true";
					String funnelId =  CustomerFunnel.FUNNEL_STAGE_NEW_CUSTOMER.getId();

					ImportingResult rs = ProfileDataManagement.importCsvAndSaveProfile(importFileUrl, observerId, dataLabelsStr, funnelId, overwriteOldData);
					LogUtil.println(rs);
					Utils.sleep(360000);
				}
			}
			else if(length == 4) {
				if (className.startsWith(PROFILE_DATA_PIPELINE_JOB)) {
					partitionId = StringUtil.safeParseInt(args[1]);
					String topic = args[2];
					String bootstrapServers = args[3];
					period = TaskRunner.DEFAULT_PERIOD;
					job = new ProfileDataPipelineJob(bootstrapServers, topic, partitionId);
				} 
			}
			else if( length == 3) {
				if(className.startsWith(CDP_PRODUCT_IMPORTING_JOB)) {
					ProductImportingEvent e = new ProductImportingEvent(args[1], args[2]);
					job = new CdpProductImportingJob(e);
				}
			}
			else if (length == 2) {
				period = StringUtil.safeParseInt(args[1]);
				if(period == 0) {
					period = TaskRunner.DEFAULT_PERIOD;
				}
				
				if (className.startsWith(CDP_EVENT_PROCESSING_JOB)) {
					partitionId = StringUtil.safeParseInt(args[1]);
					job = new CdpEventProcessingJob(partitionId);
				}
				else if(className.startsWith(CDP_PROFILE_PROCESSING_JOB)) {
					partitionId = StringUtil.safeParseInt(args[1]);
					job = new CdpProfileProcessingJob(partitionId);
				}
				else if(className.startsWith(CDP_SEGMENT_PROCESSING_JOB)) {
					job = new CdpSegmentProcessingJob();
				}
				else if(className.startsWith(CDP_REFRESH_SEGMENT_DATA_JOB)) {
					String segmentId = args[1];
					SegmentDataManagement.refreshSegmentData(segmentId);
				}
			} 
			else {
				if(className.startsWith(CDP_REFRESH_ALL_SEGMENTS_JOB)) {
					job = new RefreshAllSegmentsJob();
				}
				else if (className.startsWith(CDP_EVENT_PROCESSING_JOB)) {
					job = new CdpEventProcessingJob();
				}
				else if(className.startsWith(CDP_PROFILE_PROCESSING_JOB)) {
					job = new CdpProfileProcessingJob();
				}
				else if(className.startsWith(CDP_SEGMENT_PROCESSING_JOB)) {
					job = new CdpSegmentProcessingJob();
				}
			}

			if (job != null) {
				if(period <= 0) {
					job.doTheJob();
				}
				else {
					TaskRunner.timerSetScheduledJob(job, period);
				}
				
				Utils.foreverLoop();
			}
			else {
				System.err.println("No class for Job is found, exit DataProcessingStarter !");
			}
			
		}
		else {
			logger.error(className + " need a parameter LeoCdpEventProcessingJob or LeoCdpProfileProcessingJob");
		}

	}

	

}
