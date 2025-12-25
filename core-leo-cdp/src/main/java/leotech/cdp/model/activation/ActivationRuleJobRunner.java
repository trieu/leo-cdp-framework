package leotech.cdp.model.activation;

import java.util.Date;

import org.quartz.DateBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import leotech.system.util.LogUtil;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * ActivationRuleJobRunner
 *
 * Interprets ActivationRule scheduling metadata and converts it into Quartz
 * JobDetail + Trigger.
 */
public final class ActivationRuleJobRunner {

	private static final int DEFAULT_POLL_INTERVAL_SECONDS = 300;

	private ActivationRuleJobRunner() {
	}

	// =========================
	// JobDetail
	// =========================
	public static JobDetail buildJobDetail(String activationJobId, ActivationRule rule, Agent dataService) {

		JobDataMap dataMap = new JobDataMap();
		dataMap.put("activationRule", rule);
		dataMap.put("dataService", dataService);
		dataMap.put("activationJobId", activationJobId);

		Class<? extends Job> jobClass = dataService.getClassForJobDetails();

		return JobBuilder.newJob(jobClass).withIdentity("ActivationJob-" + activationJobId).usingJobData(dataMap)
				.build();
	}

	// =========================
	// Trigger
	// =========================
	public static Trigger buildTrigger(ActivationRule rule) {

		TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger().withIdentity("ActivationRule-" + rule.getId())
				.startAt(resolveStartTime(rule));

		if (rule.isManualTrigger()) {
			LogUtil.logInfo(ActivationRuleJobRunner.class, "Manual trigger for rule " + rule.getId());
			return builder.build();
		}

		SimpleScheduleBuilder schedule;

		if (rule.isPollingLoop()) {
			schedule = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(DEFAULT_POLL_INTERVAL_SECONDS)
					.repeatForever();
		} else {
			schedule = buildFixedRateSchedule(rule.getSchedulingTime(), rule.getTimeUnit());
		}

		return builder.withSchedule(schedule).build();
	}

	// =========================
	// Internals
	// =========================
	private static SimpleScheduleBuilder buildFixedRateSchedule(int value, int unit) {
		SimpleScheduleBuilder schedule = SimpleScheduleBuilder.simpleSchedule().repeatForever();

		switch (unit) {
		case 0:
			return schedule.withIntervalInSeconds(value);
		case 1:
			return schedule.withIntervalInMinutes(value);
		case 2:
			return schedule.withIntervalInHours(value);
		case 4:
			return schedule.withIntervalInHours(value * 24);
		case 5:
			return schedule.withIntervalInHours(value * 24 * 7);
		default:
			LogUtil.logError(ActivationRuleJobRunner.class, "Unknown timeUnit=" + unit + ", fallback to minutes");
			return schedule.withIntervalInMinutes(value);
		}
	}

	private static Date resolveStartTime(ActivationRule rule) {
		try {
			String[] toks = rule.getTimeToStart().split(StringPool.COLON);
			if (toks.length == 2) {
				return DateBuilder.todayAt(StringUtil.safeParseInt(toks[0]), StringUtil.safeParseInt(toks[1]), 0);
			}
		} catch (Exception ignored) {
		}
		return new Date();
	}
}
