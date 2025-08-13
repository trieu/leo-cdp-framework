package leotech.cdp.domain;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.job.factory.ReactiveExportDataJobFactory;
import leotech.cdp.job.reactive.ReactiveExportDataJob;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileExportingDataUtil;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.query.SegmentQuery;
import leotech.cdp.query.filters.DataFilter;
import leotech.cdp.utils.VngCloudUtil;
import leotech.system.common.PublicFileHttpRouter;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.util.IdGenerator;
import leotech.system.version.SystemMetaData;
import rfx.core.util.FileUtils;

/**
 * Segment Query Management for matched profiles and size
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class SegmentQueryManagement {
	
	private static final int MAX_SIZE_CACHE = 100000;

	private static final int MAX_SEGMENT_SIZE_TO_RUN_IN_QUEUE = SystemMetaData.MAX_SEGMENT_SIZE_TO_RUN_IN_QUEUE;

	private static final String IN_QUEUE = "in_queue";

	static Logger logger = LoggerFactory.getLogger(SegmentQueryManagement.class);

	private static final int CACHE_TIME_QUERY_PROFILE = 60;
	private static final int CACHE_TIME_QUERY_SIZE = 60;
	
	private static final int BATCH_SIZE_PROFILE_ID_QUERY = 1000;
	private static final int DATA_EXPORT_BATCH_SIZE = 1000;
	
	// ------- BEGIN Cache Segment Query for profile
	
	static CacheLoader<SegmentQuery, List<Profile>> cacheLoaderSegmentQuery = new CacheLoader<>() {
		@Override
		public List<Profile> load(SegmentQuery segmentQuery) {
			List<Profile> profiles = null;
			if (segmentQuery.isQueryReadyToRun()) {
				System.out.println("===> ProfileDaoUtil.getProfilesBySegmentQuery ");
				profiles = ProfileDaoUtil.getProfilesBySegmentQuery(segmentQuery);
			} 
			profiles = profiles == null ? new ArrayList<>(0) : profiles;
			return profiles;
		}
	};

	static final LoadingCache<SegmentQuery, List<Profile>> cacheSegmentQuery = CacheBuilder.newBuilder().maximumSize(MAX_SIZE_CACHE)
			.expireAfterWrite(CACHE_TIME_QUERY_PROFILE, TimeUnit.SECONDS).build(cacheLoaderSegmentQuery);
	
	public static void refreshCacheSegmentQuery(SegmentQuery segmentQuery) {
		cacheSegmentQuery.refresh(segmentQuery);
	}
	
	public static void clearAllSegmentQuery() {
		cacheSegmentQuery.invalidateAll();
	}
	// ------ END Cache Segment Query for profile
	
	
	// ------- BEGIN Cache Segment Size
	
	static CacheLoader<SegmentQuery, Long> cacheLoaderSegmentSize = new CacheLoader<>() {
		@Override
		public Long load(SegmentQuery segmentQuery) {
			long size = 0;
			if (segmentQuery.isQueryReadyToRun()) {
				System.out.println("===> SegmentDaoUtil.getSegmentSizeByQuery ");
				size = SegmentDaoUtil.getSegmentSizeByQuery(segmentQuery);
			} 
			return size;
		}
	};

	static final LoadingCache<SegmentQuery, Long> cacheSegmentSize = CacheBuilder.newBuilder().maximumSize(MAX_SIZE_CACHE)
			.expireAfterWrite(CACHE_TIME_QUERY_SIZE, TimeUnit.SECONDS).build(cacheLoaderSegmentSize);
	
	public static void refreshCacheSegmentSize(SegmentQuery segmentQuery) {
		cacheSegmentSize.refresh(segmentQuery);
	}
	
	public static void clearAllSegmentSize() {
		cacheSegmentSize.invalidateAll();
	}
	// ------ END Cache Segment Size
	
	/**
	 * @param Segment segment
	 * @param DataFilter filtercacheSegmentSize
	 * @return
	 */
	public static JsonDataTablePayload getProfilesInSegment(Segment segment, DataFilter filter) {
		int startIndex = filter.getStart();
		int numberResult = filter.getLength();
		boolean realtimeQuery = filter.isRealtimeQuery() || segment.isRealtimeQuery();
		boolean withLastEvents = filter.isWithLastEvents();
		long total = segment.getTotalCount();
		
		SegmentQuery segmentQuery = segment.buildQuery(startIndex, numberResult, realtimeQuery, withLastEvents);
		List<Profile> profiles = null;
		try {
			if(realtimeQuery) {
				// Create an ExecutorService with a fixed thread pool size of 2 (since we have 2 tasks)
		        ExecutorService executor = Executors.newFixedThreadPool(2);

		        // Task 1: Get profiles
		        Callable<List<Profile>> getProfilesTask = () -> ProfileDaoUtil.getProfilesBySegmentQuery(segmentQuery);

		        // Task 2: Compute segment size
		        Callable<Long> computeSizeTask = () -> computeSegmentSize(segmentQuery);

		        try {
		            // Submit both tasks to the executor
		            Future<List<Profile>> profilesFuture = executor.submit(getProfilesTask);
		            Future<Long> sizeFuture = executor.submit(computeSizeTask);

		            // Retrieve results
		            profiles = profilesFuture.get(); // This will block until the task is complete
		            total = sizeFuture.get();   // This will also block until the task is complete

		        } catch (Exception e) {
		            e.printStackTrace();
		        } finally {
		            // Shut down the executor
		            executor.shutdown();
		        }
			}
			else {
				profiles = cacheSegmentQuery.get(segmentQuery);
			}
			
		} catch (ExecutionException e) {
			logger.debug("getProfilesInSegment",e);
		}
		profiles = profiles == null ? new ArrayList<>(0) : profiles;
		return JsonDataTablePayload.data(filter.getUri(), profiles, total, total, filter.getDraw());
	}
	
	/**
	 * @param jsonQueryRules
	 * @return
	 */
	public static long computeSegmentSize(String jsonQueryRules, String customQueryFilter, boolean realtimeQuery) {
		SegmentQuery segmentQuery = new Segment(jsonQueryRules, customQueryFilter, realtimeQuery).buildQuery();
		return computeSegmentSize(segmentQuery);
	}
	
	/**
	 * @param segmentQuery
	 * @return
	 */
	public static long computeSegmentSize(SegmentQuery segmentQuery) {
		long size = 0;
		try {
			if(segmentQuery.isRealtimeQuery()) {
				size = SegmentDaoUtil.getSegmentSizeByQuery(segmentQuery);
			}
			else {
				size = cacheSegmentSize.get(segmentQuery);
			}
		} catch (ExecutionException e) {
			logger.debug("computeSegmentSize",e);
		}
		return size;
	}

	/**
	 * @param segmentId
	 * @param systemUserId
	 * @return
	 */
	public static String exportAllProfilesAsJSON(String segmentId, String systemUserId) {
		Segment sm = SegmentDaoUtil.getSegmentById(segmentId);
		if (sm != null) {
			long totalCount = sm.getTotalCount();
			List<String> rows = new ArrayList<String>((int) totalCount);

			int startIndex = 0;
			int numberResult = DATA_EXPORT_BATCH_SIZE;
			SegmentQuery profileQuery = sm.buildQuery(startIndex, numberResult, false);
			List<Profile> tempList = ProfileDaoUtil.getProfilesBySegmentQuery(profileQuery);
			for (Profile p : tempList) {
				String jsonData = p.toJson();

				// logger.info("csvData " + csvData);
				rows.add(jsonData);
			}

			while (!tempList.isEmpty()) {
				startIndex = startIndex + numberResult;
				profileQuery = sm.buildQuery(startIndex, numberResult, false);
				tempList = ProfileDaoUtil.getProfilesBySegmentQuery(profileQuery);
				for (Profile p : tempList) {
					String jsonData = p.toJson();
					rows.add(jsonData);
				}
			}

			StringBuffer exportedStr = new StringBuffer();

			exportedStr.append("[");
			int c = 0, lastIndex = rows.size() - 1;
			for (String row : rows) {
				if (c < lastIndex) {
					exportedStr.append(row).append(",");
				} else {
					exportedStr.append(row);
				}
				c++;
			}
			exportedStr.append("]");

			String dataAccessKey = IdGenerator.generateDataAccessKey(segmentId, systemUserId);
			String fullPath = PublicFileHttpRouter.PUBLIC_EXPORTED_FILES_SEGMENT + segmentId + ".json";
			FileUtils.writeStringToFile("." + fullPath, exportedStr.toString());
			String accessUri = fullPath + "?dataAccessKey=" + dataAccessKey;
			return accessUri;
		}
		return "";
	}
	

	
	/**
	 * @param segmentId
	 * @param systemUserId
	 * @param csvType
	 * @return the downloading URL file in cloud 
	 */
	public static String createJobToExportCsvData(int csvType, String segmentId, String systemUserId, String dataFor, String exportType) {
		try {
			Map<String, Object> data = new HashMap<>();
			data.put("csvType", csvType);
			data.put("segmentId", segmentId);
			data.put("systemUserId", systemUserId);
			data.put("dataFor", dataFor);
			data.put("exportType", exportType);
			
			System.out.println("createJobToExportCsvData data: \n" + data);

			ReactiveExportDataJob exportDataJob = ReactiveExportDataJobFactory.getReactiveExportDataJob(exportType);
			if(exportDataJob != null) {
				exportDataJob.sendToExportJobToQueue(data);
				return IN_QUEUE;
			}
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}


	/**
	 * @param csvType
	 * @param segmentId
	 * @param systemUserId
	 * @return the downloading URL file in local of same server
	 */
	public static String exportAllProfilesInAsCSV(int csvType, String segmentId, String systemUserId, String dataFor, String exportType) {
		try {
			Segment segment = SegmentDaoUtil.getSegmentById(segmentId);
			if (segment != null) {
				int totalCount = (int) segment.getTotalCount();
				
				// too big, put into background jobs 
				boolean inQueue = totalCount > MAX_SEGMENT_SIZE_TO_RUN_IN_QUEUE && VngCloudUtil.isReadyToUpload();
				if(inQueue) {
					return createJobToExportCsvData(csvType, segmentId, systemUserId, dataFor, exportType);
					// return
				}
				else {
					List<String> rows = new ArrayList<String>(totalCount);
					int startIndex = 0;
					List<Profile> tempList;
					ExecutorService executor = Executors.newFixedThreadPool(SystemMetaData.NUMBER_CORE_CPU);
					List<Future<String>> futures = new ArrayList<>();

					while (true) {
						SegmentQuery profileQuery = segment.buildQuery(startIndex, DATA_EXPORT_BATCH_SIZE, true);
						tempList = ProfileDaoUtil.getProfilesBySegmentQuery(profileQuery);
						if(tempList.isEmpty()) {
							break;
						}
						else {
							for (Profile p : tempList) {
								Future<String> future = executor.submit(() -> p.toCSV(csvType));
								futures.add(future);
							}
							startIndex = startIndex + DATA_EXPORT_BATCH_SIZE;
						}
					}

					for (Future<String> future : futures) {
						rows.add(future.get());
					}
					executor.shutdown();

					StringBuffer exportedStr = new StringBuffer();
					String csvHeader = ProfileExportingDataUtil.CSV_LEO_CDP_HEADER;

					if(csvType == ProfileExportingDataUtil.CSV_TYPE_FACEBOOK) {
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

					String dataAccessKey = IdGenerator.generateDataAccessKey(segmentId, systemUserId);
					LocalDateTime currentDateTime = LocalDateTime.now();
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
					String fullPath = PublicFileHttpRouter.PUBLIC_EXPORTED_FILES_SEGMENT + segmentId + "_" + currentDateTime.format(formatter) + ".csv";
					
					String content =  exportedStr.toString();
					String uriFile = "." + fullPath;
					FileUtils.writeStringToFile(uriFile, content);
					
					// to fix UTF8 in Excel Windows
					try (FileOutputStream fos = new FileOutputStream(uriFile)) {
						fos.write(0xEF);
						fos.write(0xBB); 
						fos.write(0xBF);
						fos.write(content.getBytes(StandardCharsets.UTF_8));
					}

					String csvDownloadUrl = fullPath + "?dataAccessKey=" + dataAccessKey;
					
					SegmentDataManagement.saveExportedFileUrlCsvForSegment(csvType, segment, csvDownloadUrl);
					
					return csvDownloadUrl;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * @param segmentId
	 * @param consumerProfileIdentity
	 * @return
	 */
	public static int applyConsumerForAllProfilesInSegment(String segmentId, Consumer<? super ProfileIdentity> consumerProfileIdentity) {
		//FIXME improve performance for big database
		Segment segment = SegmentDaoUtil.getSegmentById(segmentId);
		int count = 0;	
		if(segment != null) {
			int startIndex = 0;
			int numberResult = BATCH_SIZE_PROFILE_ID_QUERY;
			SegmentQuery profileQuery = segment.buildQuery(startIndex, numberResult, false);
			List<ProfileIdentity> profileIdentities;
			if(profileQuery.isQueryReadyToRun()) {
				profileIdentities = ProfileDaoUtil.getProfileIdentitiesByQuery(profileQuery);
			}
			else {
				profileIdentities = new ArrayList<>(0);
			}
			while ( ! profileIdentities.isEmpty() ) {
				profileIdentities.parallelStream().forEach(consumerProfileIdentity);
				count = count + profileIdentities.size();
				
				//loop to the end of segment
				startIndex = startIndex + numberResult;
				profileQuery = segment.buildQuery(startIndex, numberResult, false);
				if(profileQuery.isQueryReadyToRun()) {
					profileIdentities = ProfileDaoUtil.getProfileIdentitiesByQuery(profileQuery);
				}
				else {
					profileIdentities = new ArrayList<>(0);
				}
			}
		}
		return count;
	}
}
