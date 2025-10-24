package leotech.cdp.job.reactive;

import java.util.Map;

import leotech.cdp.model.file.FileApiResponse;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public abstract class ReactiveExportDataJob {
	abstract FileApiResponse processAndReturnData(Map<String, Object> data);
	abstract public void sendToExportJobToQueue(Map<String, Object> data);
    
    /**
     * @param completedCount the number of completed rows
     * @param total the total number of rows
     * @return the percentage of completed rows - max 99%
     */
    public static double calculateUploadingPercentage(long completedCount, long total, boolean needCeiling) {
        double percentage = (double) completedCount / (double) total * 100;
        return percentage <= 99
                    ? needCeiling
                        ? Math.round(percentage)
                        : Math.round(percentage * 10.0) / 10.0
                    : 99;
    }
}
