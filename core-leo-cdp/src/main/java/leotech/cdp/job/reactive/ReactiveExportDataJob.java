package leotech.cdp.job.reactive;

import java.util.Map;

import leotech.cdp.model.file.FileApiResponse;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public interface ReactiveExportDataJob {
    FileApiResponse processAndReturnData(Map<String, Object> data);
    void sendToExportJobToQueue(Map<String, Object> data);
}
