package leotech.system.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

/**
 * Log Util
 * 
 * @author tantrieuf31
 * @since 2022
 */
public class LogUtil {
	static Class<?> clazz = LogUtil.class;
	static Logger logger = LoggerFactory.getLogger(clazz);
	
	public static void debug(Object obj) {
		logger.debug(obj.toString());
	}
	
	public static void println(Object obj) {
		System.out.println(obj.toString());
	}
	
	private static DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	static String now() {
		return "[" + formatter.format(new Date()) + "] "; 
	}

	public static void logInfo(Class<?> clazz, Object message) {
		Logger logger = LoggerFactory.getLogger(clazz.getClass());
		logger.info(now() + message.toString());
	}

	public static void logInfo(Class<?> clazz, String message) {
		Logger logger = LoggerFactory.getLogger(clazz.getClass());
		logger.info(now() + message);
	}
	
	public static void logError(Class<?> clazz, String message) {
		Logger logger = LoggerFactory.getLogger(clazz.getClass());
		logger.error(now() + message);
	}


	public static String toPrettyJson(Object obj) {
		return new GsonBuilder().setPrettyPrinting().create().toJson(obj);
	}

	public static String readLastRowsLog(String logFileUri, int maxRows) throws IOException {
		StringBuilder log = new StringBuilder();

		File file = new File(logFileUri);
		int counter = 0;
		Charset charset = Charset.forName("UTF-8");

		ReversedFileReader reader = new ReversedFileReader(file, charset);
		while (counter < maxRows) {
			log.append(reader.readLine()).append("\n");
			counter++;
		}
		if (reader != null) {
			reader.close();
		}

		return log.toString();
	}
	

	public static void setLogLevelToInfo() {
		// TODO Auto-generated method stub
	}

}
