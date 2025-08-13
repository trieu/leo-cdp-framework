package leotech.system.util;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import leotech.system.version.SystemMetaData;
import rfx.core.job.ScheduledJob;
import rfx.core.util.Utils;

/**
 * background task runner
 * 
 * @author tantrieuf31
 *
 */
public final class TaskRunner {

	public static final int DEFAULT_DELAY = 4000;
	public static final int DEFAULT_PERIOD = 6000;
	
	static ExecutorService runnerService = Executors.newFixedThreadPool(4);
	static ExecutorService executorService = Executors.newFixedThreadPool(SystemMetaData.NUMBER_CORE_CPU);
	static Timer timer = new Timer(true);
	
	public static void run(Runnable task) {
		runnerService.execute(task);
	}
	
	public static void runInThreadPools(Runnable task) {
		executorService.execute(task);
	}
	
	public static void timerSetScheduledJob(ScheduledJob job) {
		timer.schedule(job, DEFAULT_DELAY, DEFAULT_PERIOD);
	}
	
	public static void timerSetScheduledJob(ScheduledJob job, int period) {
		timer.schedule(job, DEFAULT_DELAY, period);
	}
	public static void runJob(Runnable task) {
		runJob(task, 200);
	}
	public static void runJob(Runnable task, int delay) {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				task.run();
			}
		}, delay);
	}
	
	public static void main(String[] args) {
		runJob(()->{
			System.out.println("run run " + new Date());
		});
		runJob(()->{
			System.out.println("run run "+ new Date());
		});
		runJob(()->{
			System.out.println("run run "+ new Date());
		});
		Utils.sleep(5000);
	}
	
}
