package leotech.cdp.job.reactive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Segment Export as Excel file (.xlsx) using fastexcel-writer
 *
 * @author
 * @since 2025
 */
public class JobExcelExportForSegment extends ReactiveExportDataJob {

	static Logger logger = LoggerFactory.getLogger(JobExcelExportForSegment.class);

	public static final String className = JobExcelExportForSegment.class.getSimpleName();
	public static final String FILE_EXTENSION = ".xlsx";
	public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String SEGMENT_DATA_EXPORTING = "segment-data-exporting";
	public static final String SEGMENT_PROFILE_PERCENTAGE = "segment-profile-percentage";
	public static final String SEGMENT_DATA_EXPORTING_PERCENTAGE = "segment-data-exporting-percentage";
	public static final String SEGMENT_DATA_CLOUD_UPLOADING = "segment-data-cloud-uploading";

	public static final int GET_SEGMENT_DATA_FOR_EXCEL_THREAD_POOL_SIZE = Math.max(SystemMetaData.NUMBER_CORE_CPU - 1,
			1);
	public static final int FILE_CLOUD_UPLOADING_NOTIFICATION_DELAY_SECOND = 4;
	public static final int DATA_EXPORT_BATCH_SIZE = SystemMetaData.BATCH_SIZE_OF_SEGMENT_DATA_EXPORT;
	public static final int MIN_PERCENTAGE_TO_NOTIFY = 5;

	private static volatile JobExcelExportForSegment instance = null;

	public static JobExcelExportForSegment job() {
		if (instance == null) {
			instance = new JobExcelExportForSegment();
		}
		return instance;
	}

	@Override
	public void sendToExportJobToQueue(Map<String, Object> data) {
		String segmentId = data.get("segmentId").toString();
		String systemUserId = data.get("systemUserId").toString();
		String dataFor = data.get("dataFor").toString();

		System.out.println(className + " sendToExportingDataQueue segmentId: " + segmentId);

		TaskRunner.run(() -> {
			FileApiResponse response = processAndReturnData(data);
			String fileUrl = response.getFileUrl();
			String message = response.getMessage();

			if (StringUtil.isNotEmpty(fileUrl)) {
				Notification done = new Notification(className, SEGMENT_DATA_EXPORTING_PERCENTAGE, "100%", "", "");
				SystemUserManagement.sendNotification(systemUserId, done);

				done.setType(SEGMENT_DATA_EXPORTING);
				done.setUrl(fileUrl);
				done.setMessage(message);
				done.setDataFor(dataFor);
				SystemUserManagement.sendNotification(systemUserId, done);
			} else {
				System.err.println("File URL empty for segmentId=" + segmentId);
			}
		});
	}

	@Override
	public FileApiResponse processAndReturnData(final Map<String, Object> data) {
		ExecutorService executor = Executors.newFixedThreadPool(GET_SEGMENT_DATA_FOR_EXCEL_THREAD_POOL_SIZE);

		try {
			String segmentId = data.get("segmentId").toString();
			int csvType = StringUtil.safeParseInt(data.get("csvType"));
			String systemUserId = data.get("systemUserId").toString();
			Segment segment = SegmentDaoUtil.getSegmentById(segmentId);

			if (segment == null) {
				return new FileApiResponse(HttpStatus.SC_BAD_REQUEST, null, "Segment ID does not exist");
			}

			long totalCount = segment.getTotalCount();
			int startIndex = 0;
			int submittedTasks = 0;

			CompletionService<List<String>> completionService = new ExecutorCompletionService<>(executor);

			while (startIndex < totalCount) {
				int start = startIndex;
				completionService.submit(() -> getProfilesAndConvertToExcelRows(segment, start, csvType));
				startIndex += DATA_EXPORT_BATCH_SIZE;
				submittedTasks++;
			}

			// Prepare Excel file
			String fullFilePath = getLocalFilePath(segmentId);
			String localPath = "." + fullFilePath;

			Path path = Paths.get(localPath);
			File file = path.toFile();
			file.getParentFile().mkdirs();

			try (OutputStream os = new FileOutputStream(file);
					Workbook wb = new Workbook(os, "LEO CDP Export", "1.0")) {

				Worksheet sheet = wb.newWorksheet("Segment Export");
				writeExcelHeader(sheet, csvType);

				long[] processedRows = { 0 };

				for (int i = 0; i < submittedTasks; i++) {
					Future<List<String>> future = completionService.take();
					List<String> completedRows = future.get();

					writeExcelRows(sheet, completedRows, processedRows[0] + 1);
					processedRows[0] += completedRows.size();

					int completedPercentage = (int) calculateUploadingPercentage(processedRows[0], totalCount, true);

					if (completedPercentage % MIN_PERCENTAGE_TO_NOTIFY == 0) {
						sendProcessNotificationToUser(segmentId, systemUserId, completedPercentage);
					}
				}

				wb.finish();
			}

			executor.shutdown();

			logger.info("Data of segment '" + segment.getName() + "' is ready at " + localPath);
			
			
			 String securedAccessUrl = SecuredHttpDataHandler.createAccessUriForExportedFile(segmentId, systemUserId, localPath);
	            
             SegmentDataManagement.saveExportedFileUrlCsvForSegment(csvType, segment, securedAccessUrl);

			return new FileApiResponse(200, securedAccessUrl, "OK");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			executor.shutdownNow();
			return new FileApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Export interrupted");
		} catch (Exception e) {
			e.printStackTrace();
			return new FileApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Error exporting Excel");
		} finally {
			try {
				executor.awaitTermination(60, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

	private List<String> getProfilesAndConvertToExcelRows(Segment segment, int startIndex, int csvType) {
		SegmentQuery query = segment.buildQuery(startIndex, DATA_EXPORT_BATCH_SIZE, segment.isRealtimeQuery());
		List<Profile> profiles = ProfileDaoUtil.getProfilesBySegmentQuery(query);
		List<String> rows = new ArrayList<>(DATA_EXPORT_BATCH_SIZE);

		for (Profile p : profiles) {
			rows.add(p.toCSV(csvType)); // still using CSV row string, reuse structure
		}
		return rows;
	}

	private void writeExcelHeader(Worksheet sheet, int csvType) {
		String header = (csvType == ProfileExportingDataUtil.CSV_TYPE_FACEBOOK)
				? ProfileExportingDataUtil.CSV_FACEBOOK_HEADER
				: ProfileExportingDataUtil.CSV_LEO_CDP_HEADER;

		String[] columns = header.split(",");
		for (int i = 0; i < columns.length; i++) {
			sheet.value(0, i, columns[i]);
		}
	}

	private void writeExcelRows(Worksheet sheet, List<String> rows, long startRowIndex) {
		long current = startRowIndex;
		for (String row : rows) {
			String[] cols = row.split(",", -1);
			for (int c = 0; c < cols.length; c++) {
				sheet.value((int) current, c, cols[c]);
			}
			current++;
		}
	}

	private String getLocalFilePath(String segmentId) {
		LocalDateTime now = LocalDateTime.now();
		String dateTime = now.format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN));
		return PublicFileHttpRouter.PUBLIC_EXPORTED_FILES_SEGMENT + segmentId + StringPool.UNDERLINE + dateTime
				+ FILE_EXTENSION;
	}

	private void sendProcessNotificationToUser(String segmentId, String userId, int completedPercentage) {
		System.out.println("Uploaded Percentage of " + segmentId + ": " + completedPercentage);
		Notification notification = new Notification(className, SEGMENT_DATA_EXPORTING_PERCENTAGE,
				segmentId + " - " + completedPercentage + "%", "", "");
		SystemUserManagement.sendNotification(userId, notification);
	}
}
