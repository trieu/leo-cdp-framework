package test.segment_export;

import java.util.HashMap;
import java.util.Map;

import leotech.cdp.job.factory.ReactiveExportDataJobFactory;
import leotech.cdp.job.reactive.ReactiveExportDataJob;

public class SegmentDataExport {
    public static void main(String[] args) {
        Map<String, Object> data = new HashMap<>();
        data.put("csvType", 0);
        data.put("segmentId", "");
        data.put("systemUserId", "");
        data.put("dataFor", "txt_exported_excel_csv");
//        data.put("dataFor", "txt_exported_facebook_csv");
        data.put("exportType", "GOOGLE_SHEETS");
//        data.put("exportType", "CSV_FILE");

        ReactiveExportDataJob exportDataJob = ReactiveExportDataJobFactory.getReactiveExportDataJob(data.get("exportType").toString());
        exportDataJob.sendToExportJobToQueue(data);
    }
}
