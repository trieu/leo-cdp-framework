package test.cdp.activation;

import java.io.Serializable;
import java.util.Date;

import org.quartz.DateBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import rfx.core.util.StringUtil;

public class DailyTaskQuartz {

	public static class JobData implements Serializable {
		@JsonProperty("text")
		private String text;

		@JsonCreator
		public JobData(@JsonProperty("b") String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}

	public static void main(String[] args) throws SchedulerException, ClassNotFoundException {
		// Schedule the task to run every day at 9:00 AM
		String timeToStart = "01:17";

		String[] toks = timeToStart.split(":");

		int timeToRepeat = 1;

		int hourOfDay = 0;
		int minute = 0;
		if (toks.length == 2) {
			hourOfDay = StringUtil.safeParseInt(toks[0]);
			minute = StringUtil.safeParseInt(toks[1]);
		}

		try {
			Class<Job> clazz = (Class<Job>) Class.forName(MyScheduledJob.class.getName());

			// Create a Quartz Scheduler
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

			// Start the scheduler
			scheduler.start();

			// Define the job detail and tie it to our HelloJob class
			JobDataMap jobDataMap = new JobDataMap();
			jobDataMap.put("data", new JobData("One!"));
			JobDetail jobDetail = JobBuilder.newJob(clazz).usingJobData(jobDataMap)
					.withIdentity("myJob", "group1")
					.build();

			// Define the trigger to start at a specific time and repeat every 9 hours
			SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(timeToRepeat).repeatForever();
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity("myTrigger", "group1")
					.startAt(DateBuilder.todayAt(hourOfDay, minute, 0)) 
					.withSchedule(scheduleBuilder)
					.build();

			// Schedule the job with the trigger
			scheduler.scheduleJob(jobDetail, trigger);

			// Sleep for some time to allow the job to run
			Thread.sleep(120000); // Sleep for 2 minute

			// Shutdown the scheduler
			scheduler.shutdown();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class MyScheduledJob implements Job {

		JobData data;

		public void setData(JobData data) {
			this.data = data;
		}

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			// Your task logic here
			System.out.println(data.text + " do daily task executed at: " + new Date());
		}
	}
}
