package leotech.system.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public final class LevelLogger {
	private static final Logger LOGGER = LoggerFactory.getLogger(LevelLogger.class);
	private static final Map<Level, LoggingFunction> map;

	static {
		map = new HashMap<>();
		map.put(Level.TRACE, (o) -> LOGGER.trace(o));
		map.put(Level.DEBUG, (o) -> LOGGER.debug(o));
		map.put(Level.INFO, (o) -> LOGGER.info(o));
		map.put(Level.WARN, (o) -> LOGGER.warn(o));
		map.put(Level.ERROR, (o) -> LOGGER.error(o));
	}

	public static void log(Level level, String s) {
		map.get(level).log(s);
	}

	@FunctionalInterface
	private interface LoggingFunction {
		public void log(String arg);
	}
}
