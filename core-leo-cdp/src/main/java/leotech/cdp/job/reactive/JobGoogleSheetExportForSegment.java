package leotech.cdp.job.reactive;

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

import org.apache.http.HttpStatus;

import com.google.auth.oauth2.GoogleCredentials;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileExportingDataUtil;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.file.FileApiResponse;
import leotech.cdp.query.SegmentQuery;
import leotech.cdp.utils.GoogleSheetUtils;
import leotech.system.domain.SystemUserManagement;
import leotech.system.model.Notification;
import leotech.system.util.LogUtil;
import leotech.system.util.TaskRunner;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;

/**
 * @author SangNguyen, Trieu Nguyen
 * @since 2024
 */
public class JobGoogleSheetExportForSegment extends ReactiveExportDataJob {
	
	static final String className = JobGoogleSheetExportForSegment.class.getSimpleName();
	static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
	static final String VALUE_INPUT_OPTION = "RAW";
	static final String DEFAULT_SHEET_TAB_NAME = "Sheet";
	
	public static final String SEGMENT_DATA_EXPORTING = "segment-data-exporting";
	public static final String SEGMENT_PROFILE_PERCENTAGE = "segment-profile-percentage";
	public static final String SEGMENT_DATA_EXPORTING_PERCENTAGE = "segment-data-exporting-percentage";

	static final int GET_SEGMENT_DATA_FOR_GG_SHEET_THREAD_POOL_SIZE = SystemMetaData.NUMBER_CORE_CPU - 1;
	static final int DATA_EXPORT_BATCH_SIZE = SystemMetaData.BATCH_SIZE_OF_SEGMENT_DATA_EXPORT;
	static final int GOOGLE_SHEET_EXTRA_ROWS = 50;
	static final int GOOGLE_SHEET_MAX_ROWS_PER_REQUEST = 10000;
	static final int MIN_PERCENTAGE_TO_NOTIFY = 10;
	static final boolean ALLOW_PUBLIC = true;
	
	private static GoogleSheetUtils googleSheetUtils = new GoogleSheetUtils();
	private static JobGoogleSheetExportForSegment instance;
	private GoogleCredentials googleCredentials;

	public JobGoogleSheetExportForSegment(GoogleCredentials googleCredentials) {
		this.googleCredentials = googleCredentials;
	}

	public static JobGoogleSheetExportForSegment job() {
		try {
			
			if (instance == null) {
				GoogleCredentials credentials = googleSheetUtils.getCredentials();
				if(credentials != null) {
					instance = new JobGoogleSheetExportForSegment(credentials);
				}
			}
			return instance;
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.logError(JobGoogleSheetExportForSegment.class, e.toString());
		}
		return null;
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

		TaskRunner.run(() -> {
			FileApiResponse response = processAndReturnData(data);
			String fileUrl = response.getFileUrl();
			String message = response.getMessage();

			if (StringUtil.isNotEmpty(fileUrl)) {
				// send notification when finishing
				Notification notification = new Notification(className, SEGMENT_DATA_EXPORTING_PERCENTAGE, "100%", "","");
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
	 * @param data the data map from request containing csvType, segmentId and
	 *             systemUserId
	 * @return response after uploading all Segment profiles to Google Sheet
	 */
	@Override
	public FileApiResponse processAndReturnData(Map<String, Object> data) {
		ExecutorService writeSheetService = Executors
				.newFixedThreadPool(GET_SEGMENT_DATA_FOR_GG_SHEET_THREAD_POOL_SIZE);
		CompletionService<List<List<Object>>> completionService = new ExecutorCompletionService<>(writeSheetService);

		try {
			String segmentId = (String) data.get("segmentId");
			Integer csvType = (Integer) data.get("csvType");
			String systemUserId = data.get("systemUserId").toString();
			Segment segment = SegmentDaoUtil.getSegmentById(segmentId);

			if (segment != null) {
				long totalCount = segment.getTotalCount();
				
				String sheetTitle = generateSegmentGoogleSheetTitle(segment);

				// get header values basing on csvType
				List<List<Object>> headerValues = List.of(ProfileExportingDataUtil.getGoogleSheetHeader(csvType));

				// create a new spreadsheet
				FileApiResponse createSheetResponse = googleSheetUtils.createNewSheet(sheetTitle,
						totalCount + 1 + GOOGLE_SHEET_EXTRA_ROWS, headerValues.size(), DEFAULT_SHEET_TAB_NAME,
						this.googleCredentials, ALLOW_PUBLIC);

				// when create new spreadsheet successfully
				if (createSheetResponse.getFileUrl() != null) {
					String sheetUrl = createSheetResponse.getFileUrl();
					
					// get sheet ID after creating
					String sheetId = createSheetResponse.getMessage();
					
					TaskRunner.runJob(()->{
						int startIndex = 0;
						int submittedTasks = 0;
						
						// get range of header (Ex: A0:A<header size>)
						String range = googleSheetUtils.getRange(DEFAULT_SHEET_TAB_NAME, startIndex + 1, headerValues);

						// add header first
						FileApiResponse writeResponse = googleSheetUtils.writeToSheet(sheetId, range, VALUE_INPUT_OPTION,
								headerValues, this.googleCredentials);

						if (writeResponse.getStatusCode() != HttpStatus.SC_OK) {
							LogUtil.logError(JobGoogleSheetExportForSegment.class, writeResponse.toString());
						}
						
						// submit event to write each data batch to Google Sheet
						while (startIndex < totalCount) {
							final int start = startIndex;
							completionService.submit(() -> getProfilesAndConvertToGoogleSheetRow(segment, start, csvType));
							startIndex += DATA_EXPORT_BATCH_SIZE;
							submittedTasks++;
						}

						// after submitting, wait for the response and notify the process to the user
						try {
							FileApiResponse response = writeDataToGoogleSheetAndSendProcessNotificationToUser(submittedTasks, segment,
									systemUserId, sheetId, completionService);
							if (response.getStatusCode() != HttpStatus.SC_OK) {
								LogUtil.logError(JobGoogleSheetExportForSegment.class, response.toString());
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					});


					// SAVE CSV DOWNLOAD URL into Segment
					SegmentDataManagement.saveExportedFileUrlCsvForSegment(csvType, segment, sheetUrl);
					System.out.println("**Finished exporting data to Google Sheet!**\nURL: " + sheetUrl);

					return new FileApiResponse(HttpStatus.SC_OK, sheetUrl,
							"Export segment to Google Sheet successfully");
				} else {
					return new FileApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null,
							createSheetResponse.getMessage());
				}
			}

			return new FileApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, "Segment ID does not exist");
		} catch (Exception e) {
			e.printStackTrace();
			writeSheetService.shutdownNow();

			return new FileApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null,
					"Error to export segment data to Google Sheet");
		}
	}

	/**
	 * @param spreadSheetId spreadsheet ID
	 * @param values        two-sided list for profiles
	 * @return response after getting all profiles of selected Segment and write
	 *         them to Google Sheet
	 */
	private FileApiResponse writeProfilesToGoogleSheet(String spreadSheetId, List<List<Object>> values, int startRow) {
		// get range to write from values
		String range = googleSheetUtils.getRange(DEFAULT_SHEET_TAB_NAME, startRow, values);

		FileApiResponse res = googleSheetUtils.writeToSheet(spreadSheetId, range, VALUE_INPUT_OPTION, values,
				this.googleCredentials);

		if (res.getStatusCode() == HttpStatus.SC_OK) {
			return new FileApiResponse(HttpStatus.SC_OK, googleSheetUtils.getGoogleSheetUrl(spreadSheetId),
					"Added new " + values.size() + " rows in " + res.getFileUrl());
		} else {
			return new FileApiResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR,
					googleSheetUtils.getGoogleSheetUrl(spreadSheetId), "Error to export segment data to Google Sheet");
		}
	}

	/**
	 * @param startIndex starting index to query data from database (...LIMIT
	 *                   startIndex, batchSize...)
	 * @param segment    selected Segment to export
	 * @param csvType    type of csv file
	 * @return two-sided list containing all profiles of selected Segment after
	 *         querying from database
	 */
	private List<List<Object>> getProfilesAndConvertToGoogleSheetRow(Segment segment, int startIndex, int csvType) {
		SegmentQuery profileQuery = segment.buildQuery(startIndex, DATA_EXPORT_BATCH_SIZE, segment.isRealtimeQuery());
		List<Profile> tempList = ProfileDaoUtil.getProfilesBySegmentQuery(profileQuery);
		List<List<Object>> values = new ArrayList<>(DATA_EXPORT_BATCH_SIZE);

		if (!tempList.isEmpty()) {
			for (Profile p : tempList) {
				values.add(p.toGoogleSheet(csvType));
			}
		}

		return values;
	}

	/**
	 * @param segment selected Segment to export
	 * @return the auto-generated name from a Segment
	 */
	private String generateSegmentGoogleSheetTitle(Segment segment) {
		LocalDateTime currentDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
		String dateTimeStr = currentDateTime.format(formatter);

		return segment.getName() + "(" + segment.getId() + ") (Exported Date: " + dateTimeStr + ")";
	}

	/**
	 * @param submittedTasks    submitted task number
	 * @param segment           selected Segment to export
	 * @param systemUserId      user ID
	 * @param spreadsheetId     selected spreadsheet ID to write on
	 * @param completionService CompletionService
	 * @return FileApiResponse after writing data on Google Sheet
	 */
	private FileApiResponse writeDataToGoogleSheetAndSendProcessNotificationToUser(int submittedTasks, Segment segment,
			String systemUserId, String spreadsheetId, CompletionService<List<List<Object>>> completionService)
			throws Exception {
		int startRow = 2;
		List<List<Object>> profileValues = new ArrayList<>();
		long totalCount = segment.getTotalCount();
		String segmentId = segment.getId();
		int currentPercentage = MIN_PERCENTAGE_TO_NOTIFY;

		for (int taskCount = 1; taskCount <= submittedTasks; taskCount++) {
			Future<List<List<Object>>> profilesFuture = completionService.take();
			List<List<Object>> profiles = profilesFuture.get();
			profileValues.addAll(profiles);

			// send process notification
			int completedPercentage = (int) GoogleSheetUtils.calculateUploadingPercentage(profileValues.size(),
					totalCount, true);
			if (currentPercentage < completedPercentage) {
				sendProcessNotificationToUser(segmentId, systemUserId, completedPercentage);
				currentPercentage += MIN_PERCENTAGE_TO_NOTIFY;
			}

			// write data to Google Sheet
			if (profileValues.size() > GOOGLE_SHEET_MAX_ROWS_PER_REQUEST || taskCount == submittedTasks) {
				System.out.println("**Reached the maximum rows, proceeding writing data batch to Google Sheet...**");

				FileApiResponse response = writeProfilesToGoogleSheet(spreadsheetId, profileValues, startRow);

				if (response.getStatusCode() == HttpStatus.SC_OK) {
					System.out.println(response.getMessage());
					startRow += profileValues.size();
					profileValues.clear();
				} else {
					return response;
				}
			}
		}

		return new FileApiResponse(HttpStatus.SC_OK, googleSheetUtils.getGoogleSheetUrl(spreadsheetId),
				"Exported data to CSV file successfully");
	}

	/**
	 * @param segmentId           exporting Segment ID
	 * @param userId              current user ID
	 * @param completedPercentage exported percentage
	 */
	private void sendProcessNotificationToUser(String segmentId, String userId, int completedPercentage) {
		System.out.println("Uploaded Percentage of " + segmentId + ": " + completedPercentage);

		// send websocket notification for proceeded percentage of rows
		Notification notification = new Notification(className, SEGMENT_DATA_EXPORTING_PERCENTAGE,
				segmentId + " - " + completedPercentage + "%", "", "");
		SystemUserManagement.sendNotification(userId, notification);
	}
}
