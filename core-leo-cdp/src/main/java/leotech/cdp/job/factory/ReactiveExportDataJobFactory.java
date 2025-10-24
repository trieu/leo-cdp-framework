package leotech.cdp.job.factory;

import java.util.Objects;

import leotech.cdp.job.reactive.JobCsvExportForSegment;
import leotech.cdp.job.reactive.JobExcelExportForSegment;
import leotech.cdp.job.reactive.JobGoogleSheetExportForSegment;
import leotech.cdp.job.reactive.ReactiveExportDataJob;

/**
 * @author Trieu Nguyen, Sang Nguyen
 * @since 2024
 *
 */
public class ReactiveExportDataJobFactory {
    private ReactiveExportDataJobFactory() {}

    /**
     * check ExportDataType
     * 
     * @param type
     * @return ReactiveExportDataJob
     */
    public final static ReactiveExportDataJob getReactiveExportDataJob(String type) {
    	type = type.toUpperCase();
        if(Objects.equals(type, ExportDataType.CSV_FILE.name())) {
            return JobCsvExportForSegment.job();
        }
        else  if(Objects.equals(type, ExportDataType.EXCEL_FILE.name())) {
            return JobExcelExportForSegment.job();
        }
        else  if(Objects.equals(type, ExportDataType.GOOGLE_SHEETS.name()) ) {
            return JobGoogleSheetExportForSegment.job();
        }
        else throw new IllegalArgumentException("This exporting type is unsupported");
    }
}
