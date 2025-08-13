package test.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;

public class TestQuantz {
// https://www.baeldung.com/quartz
	public static void main(String[] args) throws SchedulerException {
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();

		Scheduler sched = schedFact.getScheduler();

		sched.start();

	}

	public class SimpleJob implements Job {
		public void execute(JobExecutionContext arg0) throws JobExecutionException {
			System.out.println("This is a quartz job!");
		}
	}
}
