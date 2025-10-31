package leotech.cdp.utils;

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.Gson;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.DateTimeUtil;
import rfx.core.util.StringUtil;

public class EventTrackingUtil {

	private static final String EVUT = "evut-";
	private static final String EVENT_STATS = "event-stats";
	private static final String SUMMARY = "summary";
	private static final String PREFIX_MONITOR = "m:";
	private static final String PREFIX_VIEW = "v:";

	public static final String DATE_HOUR_MINUTE_FORMAT_PATTERN = "yyyy-MM-dd-HH-mm";
	public static final String DATE_HOUR_MINUTE_SECOND_FORMAT_PATTERN = "yyyy-MM-dd-HH-mm-ss";

	public static final int TIME_TO_PUSH = 2000;
	public static final int ONE_DAY = 86400;
	public static final int AFTER_3_DAYS = ONE_DAY * 3;
	public static final int AFTER_7_DAYS = ONE_DAY * 7;
	public static final int AFTER_10_DAYS = ONE_DAY * 10;
	public static final int AFTER_15_DAYS = ONE_DAY * 15;
	public static final int AFTER_30_DAYS = ONE_DAY * 30;
	public static final int AFTER_60_DAYS = ONE_DAY * 60;

	static JedisPooled jedisPool = RedisConfigs.load().get("realtimeDataStats").getJedisClient();

	public static boolean updateEvent(long unixtime, final String event) {
		return updateEvent(unixtime, event, false);
	}

	public static boolean updateEvent(String prefix, long unixtime, final String event, boolean withSummary) {
		return updateEvent(prefix, unixtime, new String[] { event }, withSummary);
	}

	public static boolean updateEvent(long unixtime, final String event, boolean withSummary) {
		return updateEvent(PREFIX_MONITOR, unixtime, new String[] { event }, withSummary);
	}

	public static boolean updateEvent(long unixtime, final String[] events, boolean withSummary) {
		return updateEvent(PREFIX_MONITOR, unixtime, events, withSummary);
	}

	public static boolean updateEvent(String prefix, long unixtime, final String[] events, boolean withSummary) {
		boolean commited = eventBufferQueue.add(new EventLog(prefix, unixtime, events, withSummary));
		return commited;
	}

	public static boolean updateEventPageView(final String uuid, final String event, final String hostReferer,
			long delta) {
		boolean commited = false;
		try {
			Date date = new Date();
			final String dateStr = DateTimeUtil.formatDate(date, DateTimeUtil.DATE_FORMAT_PATTERN);
			final String dateHourStr = DateTimeUtil.formatDate(date, DateTimeUtil.DATE_HOUR_FORMAT_PATTERN);

			RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
				@Override
				protected Boolean build() throws JedisException {
					Pipeline p = jedis.pipelined();

					p.hincrBy(EVENT_STATS, event, delta);
					p.pfadd(EVUT + event, uuid);

					String keyH = PREFIX_VIEW + dateHourStr;
					p.hincrBy(keyH, event, delta);
					p.expire(keyH, AFTER_7_DAYS);

					String keyD = PREFIX_VIEW + dateStr;
					p.hincrBy(keyD, event, delta);
					p.expire(keyD, AFTER_15_DAYS);

					if (StringUtil.isNotEmpty(uuid)) {
						String keyHU = PREFIX_VIEW + dateHourStr + ":u";
						p.pfadd(keyHU, uuid);
						p.expire(keyHU, AFTER_3_DAYS);

						String keyDU = PREFIX_VIEW + dateStr + ":u";
						p.pfadd(keyDU, uuid);
						p.expire(keyDU, AFTER_15_DAYS);
					}

					if (StringUtil.isNotEmpty(event)) {
						String keyHUT = PREFIX_VIEW + dateHourStr + ":u:" + event + ":" + hostReferer;
						p.pfadd(keyHUT, uuid);
						p.expire(keyHUT, AFTER_3_DAYS);

						String keyDUT = PREFIX_VIEW + dateStr + ":u:" + event + ":" + hostReferer;
						p.pfadd(keyDUT, uuid);
						p.expire(keyDUT, AFTER_15_DAYS);
					}

					p.sync();
					return true;
				}
			};
			if (cmd != null) {
				commited = cmd.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return commited;
	}

	public static boolean recordTrackingEvent(final String host, final String uuid, final String tag) {
		boolean commited = false;
		try {
			Date date = new Date();
			final String dateStr = DateTimeUtil.formatDate(date, DateTimeUtil.DATE_FORMAT_PATTERN);
			final String dateHourStr = DateTimeUtil.formatDate(date, DateTimeUtil.DATE_HOUR_FORMAT_PATTERN);
			RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
				@Override
				protected Boolean build() throws JedisException {
					Pipeline p = jedis.pipelined();

					String keyH = PREFIX_VIEW + dateHourStr;
					p.hincrBy(keyH, host, 1L);
					p.expire(keyH, AFTER_15_DAYS);

					String keyD = PREFIX_VIEW + dateStr;
					p.hincrBy(keyD, host, 1L);
					p.expire(keyD, AFTER_60_DAYS);

					if (StringUtil.isNotEmpty(uuid)) {
						String keyHU = PREFIX_VIEW + dateHourStr + ":u";
						p.pfadd(keyHU, uuid);
						p.expire(keyHU, AFTER_3_DAYS);

						String keyDU = PREFIX_VIEW + dateStr + ":u";
						p.pfadd(keyDU, uuid);
						p.expire(keyDU, AFTER_7_DAYS);
					}

					if (StringUtil.isNotEmpty(tag)) {
						String keyHUT = PREFIX_VIEW + dateHourStr + ":u:" + host + ":" + tag;
						p.pfadd(keyHUT, uuid);
						p.expire(keyHUT, AFTER_3_DAYS);

						String keyDUT = PREFIX_VIEW + dateStr + ":u:" + host + ":" + tag;
						p.pfadd(keyDUT, uuid);
						p.expire(keyDUT, AFTER_7_DAYS);
					}

					p.sync();
					return true;
				}
			};
			if (cmd != null) {
				commited = cmd.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return commited;
	}

	public static boolean updateLogEvent(int unixtime, final String event) {
		return updateEvent(unixtime, "kk:" + event);
	}

	public static String getAllLogEvents(final String pkey) {
		final String key;
		if (pkey == null) {
			key = PREFIX_MONITOR + DateTimeUtil.formatDate(new Date(), DateTimeUtil.DATE_HOUR_FORMAT_PATTERN);
		} else {
			key = pkey;
		}
		if (key.startsWith(PREFIX_MONITOR)) {
			String s = new RedisCommand<String>(jedisPool) {
				@Override
				protected String build() throws JedisException {
					Map<String, String> map = jedis.hgetAll(key);
					return new Gson().toJson(map);
				}
			}.execute();
			return s;
		}
		return "No data";
	}

	public static final String LOCATION_METRIC_PLAYVIEW = "lplv";
	public static final String LOCATION_METRIC_IMPRESSION = "limp";
	public static final String LOCATION_METRIC_CLICK = "lclk";

	final static class EventLog {
		String prefix;
		long unixtime;
		String[] events;
		boolean withSummary;

		public EventLog(String prefix, long unixtime, String[] events, boolean withSummary) {
			super();
			this.prefix = prefix;
			this.unixtime = unixtime;
			this.events = events;
			this.withSummary = withSummary;
		}

		public String getPrefix() {
			return prefix;
		}

		public long getUnixtime() {
			return unixtime;
		}

		public String[] getEvents() {
			return events;
		}

		public boolean isWithSummary() {
			return withSummary;
		}
	}

	private static Queue<EventLog> eventBufferQueue = new ConcurrentLinkedQueue<>();
	private static Timer timer = new Timer(true);

	static {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				for (int i = 0; i < 1000; i++) {
					EventLog log = eventBufferQueue.poll();
					if (log == null) {
						break;
					}
					String prefix = log.getPrefix();
					long unixtime = log.getUnixtime();
					String[] events = log.getEvents();
					boolean withSummary = log.isWithSummary();
					try {
						Date date = new Date(unixtime * 1000L);
						final String dateStr = DateTimeUtil.formatDate(date, DateTimeUtil.DATE_FORMAT_PATTERN);
						final String dateHourStr = DateTimeUtil.formatDate(date, DateTimeUtil.DATE_HOUR_FORMAT_PATTERN);
						RedisCommand<Boolean> cmd = new RedisCommand<Boolean>(jedisPool) {
							@Override
							protected Boolean build() throws JedisException {

								String keyD = prefix + dateStr;
								String keyH = prefix + dateHourStr;

								Pipeline p = jedis.pipelined();
								for (String event : events) {
									if (withSummary) {
										p.hincrBy(SUMMARY, event, 1L);
									}

									// hourly
									p.hincrBy(keyH, event, 1L);
									p.expire(keyH, AFTER_7_DAYS);

									// daily
									p.hincrBy(keyD, event, 1L);
									p.expire(keyD, AFTER_10_DAYS);
								}

								p.sync();
								return true;
							}
						};
						cmd.execute();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		}, TIME_TO_PUSH, TIME_TO_PUSH);
	}

}
