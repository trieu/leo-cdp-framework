package leotech.system.model;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import leotech.starter.router.UploaderHttpRouter;
import rfx.core.util.StringUtil;

/**
 * the class to parse CSV data for processing and importing
 * 
 * @author Trieu Nguyen (Thomas)
 * @since 2022
 *
 * @param <T> type of Record Item
 */
public abstract class CsvDataParser<T> {

	static Logger logger = LoggerFactory.getLogger(CsvDataParser.class);
	public static final String IMPORT_FILE_URL = "importFileUrl";
	public static final int TOP_RECORDS = 20;
	
	String importFileUrl;
	boolean previewTopRecords;

	public CsvDataParser(String importFileUrl, boolean previewTopRecords) {
		super();
		this.importFileUrl = importFileUrl;
		this.previewTopRecords = previewTopRecords;
	}

	/**
	 * parse the imported CSV File
	 * 
	 * @param importFileUrl
	 * @param previewTopRecords
	 * @return
	 */
	public List<T> parseImportedCsvFile() {
		if(StringUtil.isEmpty(importFileUrl)) {
			return new ArrayList<T>(0);
		}
		
		
		CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(false);
		if (previewTopRecords) {
			settings.setNumberOfRecordsToRead(TOP_RECORDS);
		}
		
		CsvParser csvParser = new CsvParser(settings);		
		List<T> records = null;
		try {
			String pathname;
			if(importFileUrl.startsWith(UploaderHttpRouter.UPLOADED_FILES_LOCATION)) {
				pathname = "." + importFileUrl;
			}
			else if(importFileUrl.startsWith("/")) {
				pathname = importFileUrl;// absolute URL
			}
			else {
				pathname = "./" + importFileUrl;// relative URL
			}
			
			File file = new File(pathname);
			if(file.isFile()) {
				List<String[]> allRows = csvParser.parseAll(new FileReader(file));
				int len = allRows.size();
				if(len > 0) {
					String[] headers = allRows.get(0);
					records = new ArrayList<>(len);				
					for (int i = 1; i < len; i++) {
						String[] csvDataRow = allRows.get(i);	
						T obj = createObjectFromCsvRow(headers , csvDataRow);
						if(obj != null) {
							records.add(obj);
						}
					}
				}
			}
			else {
				logger.error("Not found any file at " + pathname);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
		if (records == null) {
			records = new ArrayList<T>(0);
		}		
		return records;
	}
	
	/**
	 * to create data entity from CSV data
	 * 
	 * @param headers
	 * @param csvDataRow
	 * @return
	 */
	abstract public T createObjectFromCsvRow(String[] headers, String[] csvDataRow);
	
}
