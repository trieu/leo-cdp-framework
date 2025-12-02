package leotech.system.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import leotech.system.version.SystemMetaData;
import rfx.core.util.Utils;

/**
 * Log Util
 * 
 * @author tantrieuf31
 * @since 2022
 */
public class LogUtil {
	static Class<?> clazz = LogUtil.class;
	static Logger logger = LoggerFactory.getLogger(clazz);
	

    /**
     * Change root log level to INFO at runtime (Log4j2)
     */
    public static void loadLoggerConfigs() {
    	LogUtil.reloadConfig("./configs/log4j.xml");
    	
    	 Utils.sleep(200);
    	if(SystemMetaData.isDevMode()) {
    		 setLogLevel("INFO");
    	}       
    }

    /**
     * Change root log level at runtime using string input: DEBUG, INFO, WARN, ERROR, FATAL
     */
    public static void setLogLevel(String levelName) {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();

            Level level = Level.getLevel(levelName.toUpperCase());
            if (level == null) {
                logger.error("Invalid log level: {}", levelName);
                return;
            }

            config.getRootLogger().setLevel(level);
            context.updateLoggers(); // propagate changes

            logger.info("Root log level changed to {}", level);
        } catch (Exception e) {
            logger.error("Failed to change log level at runtime", e);
        }
    }

    /**
     * Reload Log4j2 config file at runtime (advanced)
     */
    public static void reloadConfig(String configFilePath) {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.setConfigLocation(new URI(configFilePath));
            context.reconfigure();
            logger.info("Log4j2 config reloaded from {}", configFilePath);
        } catch (Exception e) {
            logger.error("Failed to reload Log4j2 config", e);
        }
    }
	
	public static void debug(Object obj) {
		logger.debug(obj.toString());
	}
	
	public static void println(Object obj) {
		System.out.println(obj.toString());
	}
	
	private static final ThreadLocal<DateFormat> FORMATTER =
	        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

	
	static String now() {
		return "[" + FORMATTER.get().format(new Date()) + "] "; 
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


	

	public static String readLastRowsLog(String logFileUri, int maxRows) throws IOException {
		StringBuilder log = new StringBuilder();

		File file = new File(logFileUri);
		int counter = 0;

		ReversedFileReader reader = new ReversedFileReader(file);
		while (counter < maxRows) {
			log.append(reader.readLine()).append("\n");
			counter++;
		}
		if (reader != null) {
			reader.close();
		}

		return log.toString();
	}
	
	public static final String toJson(Object obj) {
		return new GsonBuilder().disableHtmlEscaping().create().toJson(obj);
	}
	
	public static String toPrettyJson(Object obj) {
		return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(obj);
	}
	
	

}
