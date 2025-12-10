package leotech.cdp.job.reactive;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileExportingDataUtil;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.file.FileApiResponse;
import leotech.cdp.query.SegmentQuery;
import leotech.system.common.PublicFileHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.domain.SystemUserManagement;
import leotech.system.model.Notification;
import leotech.system.util.TaskRunner;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * Segment Export as CSV data
 * 
 * @author SangNguyen, Trieu Nguyen
 * @since 2024
 */
public class JobCsvExportForSegment extends ReactiveExportDataJob {
	public static final String className = JobCsvExportForSegment.class.getSimpleName();
	public static final String FILE_EXTENSION = ".csv";
	public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String SEGMENT_DATA_EXPORTING = "segment-data-exporting";
	public static final String SEGMENT_PROFILE_PERCENTAGE = "segment-profile-percentage";
	public static final String SEGMENT_DATA_EXPORTING_PERCENTAGE = "segment-data-exporting-percentage";
	public static final String SEGMENT_DATA_CLOUD_UPLOADING = "segment-data-cloud-uploading";
	
	public static final int GET_SEGMENT_DATA_FOR_CSV_THREAD_POOL_SIZE = SystemMetaData.NUMBER_CORE_CPU - 1;
	public static final int FILE_CLOUD_UPLOADING_NOTIFICATION_DELAY_SECOND = 4;
	public static final int DATA_EXPORT_BATCH_SIZE = SystemMetaData.BATCH_SIZE_OF_SEGMENT_DATA_EXPORT;
	public static final int MIN_PERCENTAGE_TO_NOTIFY = 5;

	private static volatile JobCsvExportForSegment instance = null;

	public static JobCsvExportForSegment job() {
		if (instance == null) {
			instance = new JobCsvExportForSegment();
		}
		return instance;
	}


	/**
	 * @param data Map of data you want to export in queue
	 */
	@Override
	public void sendToExportJobToQueue(Map<String, Object> data) {
		String segmentId = data.get("segmentId").toString();
		String systemUserId = data.get("systemUserId").toString();
		String dataFor = data.get("dataFor").toString();

		System.out.println(className + " sendToExportingDataQueue segmentId: " + segmentId);
		
		TaskRunner.run(()->{
			FileApiResponse response = processAndReturnData(data);
			String fileUrl = response.getFileUrl();
			String message = response.getMessage();

			if (StringUtil.isNotEmpty(fileUrl)) {
				// send notification when finishing
				Notification notification = new Notification(
						className,
						SEGMENT_DATA_EXPORTING_PERCENTAGE,
						"100%",
						"",
						""
				);
				SystemUserManagement.sendNotification(systemUserId, notification);

				notification.setType(SEGMENT_DATA_EXPORTING);
				notification.setUrl(fileUrl);
				notification.setMessage(message);
				notification.setDataFor(dataFor);

				SystemUserManagement.sendNotification(systemUserId, notification);
			} else {
				System.err.println(fileUrl);
			}
		});
	}


	/**
	 * @param data the data map from request containing csvType, segmentId and systemUserId
	 * @return response after uploading all Segment profiles to cloud storage
	 */
	@Override
	public FileApiResponse processAndReturnData(final Map<String, Object> data) {
		// Use all thread for exporting and 1 thread for websocket notification
		ExecutorService createFileExecutor = Executors.newFixedThreadPool(GET_SEGMENT_DATA_FOR_CSV_THREAD_POOL_SIZE);

		try {
			String segmentId = data.get("segmentId").toString();
			int csvType = StringUtil.safeParseInt(data.get("csvType"));
			String systemUserId = data.get("systemUserId").toString();
			Segment segment = SegmentDaoUtil.getSegmentById(segmentId);

			if (segment != null) {
				long totalCount = segment.getTotalCount();
				int startIndex = 0;
				int submittedTasks = 0;

				CompletionService<List<String>> completionService = new ExecutorCompletionService<>(createFileExecutor);

				while (startIndex < totalCount) {
					int start = startIndex;
					completionService.submit(() -> getProfilesAndConvertToCsvContentRows(segment, start, csvType));
					startIndex += DATA_EXPORT_BATCH_SIZE;
					submittedTasks++;
				}

				List<String> rows = getAllDataRowsAndSendProcessNotificationToUser(submittedTasks, segment, systemUserId, completionService);

				// stop create file thread
				createFileExecutor.shutdown();

				// send websocket notification task
				Runnable task = () -> {
					// send websocket notification for proceeded percentage of rows
					Notification notification = new Notification(
							className,
							SEGMENT_DATA_CLOUD_UPLOADING,
							"Uploading file to cloud storage",
							"",
							""
					);
					SystemUserManagement.sendNotification(systemUserId, notification);
				};
				TaskRunner.run(task);

				StringBuffer exportedDataStr = buildCsvContent(csvType, rows);
				String fullFilePath = getLocalFilePath(segmentId);
				String fullLocalFilePath = "." + fullFilePath;

				// add BOM to FILE_EXTENSION file to avoid utf-8 decode error
				String content = new String(exportedDataStr.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

				// to fix UTF8 in Excel Windows
				try (FileOutputStream fos = new FileOutputStream(fullLocalFilePath)) {
					fos.write(0xEF);
					fos.write(0xBB);
					fos.write(0xBF);
					fos.write(content.getBytes(StandardCharsets.UTF_8));
				}

				

                
                String securedAccessUrl = SecuredHttpDataHandler.createAccessUriForExportedFile(segmentId, systemUserId, fullLocalFilePath);
            
                SegmentDataManagement.saveExportedFileUrlCsvForSegment(csvType, segment, securedAccessUrl);
                
       
				FileApiResponse fileApiResponse = new FileApiResponse(200, securedAccessUrl, "CSV file is ready");
                fileApiResponse.setMessage(" Data of segment '" + segment.getName() + "' is ready.");
				return fileApiResponse;
			}

			return new FileApiResponse(HttpStatus.SC_BAD_REQUEST, null, "Segment ID does not exist");
		}
		catch (InterruptedException e) {
			createFileExecutor.shutdownNow();
			Thread.currentThread().interrupt();

			return new FileApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Error to upload file to Vstorage");
		}
		catch (Exception e) {
			e.printStackTrace();

			return new FileApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Error to upload file to Vstorage");
		}
		finally {
			

			try {
				if (!createFileExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
					createFileExecutor.shutdownNow();
				}
			} catch (InterruptedException e) {
				createFileExecutor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}


	/**
	 * @param segment selected Segment to export
	 * @param startIndex starting index to query data from database (...LIMIT startIndex, batchSize...)
	 * @param csvType type of csv file
	 * @return string list of profiles after being converted to CSV content row format
	 */
	private List<String> getProfilesAndConvertToCsvContentRows(Segment segment, int startIndex, int csvType) {
		SegmentQuery sQuery = segment.buildQuery(startIndex, DATA_EXPORT_BATCH_SIZE, segment.isRealtimeQuery());
		List<Profile> tempList = ProfileDaoUtil.getProfilesBySegmentQuery(sQuery);
		List<String> rows = new ArrayList<>(DATA_EXPORT_BATCH_SIZE);

		if (! tempList.isEmpty() ) {
			for (Profile p : tempList) {
				String row = p.toCSV(csvType);
				rows.add(row);
			}
		}

		return rows;
	}






	/**
	 * @param segmentId selected segment ID to export
	 * @return local file path to the local CSV file
	 */
	private String getLocalFilePath(String segmentId) {
		LocalDateTime currentDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
		String dateTimeStr = currentDateTime.format(formatter);
		return PublicFileHttpRouter.PUBLIC_EXPORTED_FILES_SEGMENT + segmentId + StringPool.UNDERLINE + dateTimeStr + FILE_EXTENSION;
	}


	/**
	 * @param submittedTasks number of submitted tasks to the multiple thread process
	 * @param segment selected segment to export
	 * @param userId user ID
	 * @param completionService CompletionService
	 * @return string list of all data rows of segment's profiles to export to CSV
	 */
	private List<String> getAllDataRowsAndSendProcessNotificationToUser(int submittedTasks, Segment segment, String userId, CompletionService<List<String>> completionService) throws Exception {
		int flagPercentage = MIN_PERCENTAGE_TO_NOTIFY;
		long totalCount = segment.getTotalCount();
		String segmentId = segment.getId();
		List<String> dataRows = new ArrayList<>((int) totalCount);

		for (int i = 0; i < submittedTasks; i++) {
			Future<List<String>> future = completionService.take();
			List<String> completedCsvRows = future.get();
			dataRows.addAll(completedCsvRows);

			// get completed flagPercentage
			int completedCount = completedCsvRows.size() * (i + 1);
			int completedPercentage = (int) calculateUploadingPercentage(
					completedCount,
					totalCount,
					true
			);

			// reduce notification amount to send in short period of time
			if(flagPercentage < completedPercentage) {
				sendProcessNotificationToUser(segmentId, userId, completedPercentage);
				flagPercentage += MIN_PERCENTAGE_TO_NOTIFY;
			}
		}

		return dataRows;
	}


	/**
	 * @param segmentId selected segment ID to export
	 * @param userId user ID
	 * @param completedPercentage exported percentage
	 */
	private void sendProcessNotificationToUser(String segmentId, String userId, int completedPercentage) {
		System.out.println("Uploaded Percentage of " + segmentId + ": " + completedPercentage);

		// send websocket notification for proceeded percentage of rows
		Notification notification = new Notification(
				className,
				SEGMENT_DATA_EXPORTING_PERCENTAGE,
				segmentId + " - " + completedPercentage + "%",
				"",
				""
		);
		SystemUserManagement.sendNotification(userId, notification);
	}


	/**
	 * @param csvType type of CSV file
	 * @param rows string list of all data rows of segment's profiles to export to CSV
	 * @return a string buffer containing all content of the CSV file
	 */
	private StringBuffer buildCsvContent(int csvType, List<String> rows) {
		StringBuffer exportedStr = new StringBuffer();
		String csvHeader = ProfileExportingDataUtil.CSV_LEO_CDP_HEADER;

		if (csvType == ProfileExportingDataUtil.CSV_TYPE_FACEBOOK) {
			csvHeader = ProfileExportingDataUtil.CSV_FACEBOOK_HEADER;
		}
		exportedStr.append(csvHeader).append("\n");

		int c = 0, lastIndex = rows.size() - 1;
		for (String row : rows) {
			if (c < lastIndex) {
				exportedStr.append(row).append("\n");
			} else {
				exportedStr.append(row);
			}
			c++;
		}

		return exportedStr;
	}
}
