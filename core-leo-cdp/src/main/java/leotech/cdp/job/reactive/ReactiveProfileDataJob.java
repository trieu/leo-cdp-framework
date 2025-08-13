package leotech.cdp.job.reactive;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author tantrieuf31
 * @since 2021
 *
 * @param <T>
 */
public abstract class ReactiveProfileDataJob<T> {
	
	public static final int BATCH_PROCESSING_SIZE = 50;
	public static final int TIME_TO_PROCESS = 3000;//milisecs
	
	protected final Timer timer = new Timer(true);
	protected final Queue<T> dataQueue = new ConcurrentLinkedQueue<>();

	public ReactiveProfileDataJob() {
		this.initTimer(BATCH_PROCESSING_SIZE, TIME_TO_PROCESS);
	}

	public ReactiveProfileDataJob(int batchSize, int timeToProcess) {
		this.initTimer(batchSize, timeToProcess);
	}
	
	private void initTimer(int batchSize, int timeToProcess) {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				processDataQueue(batchSize);
			}
		}, timeToProcess, timeToProcess);	
	}
	
	public final void enqueue(T obj) {
		dataQueue.add(obj);
	}
	
	abstract public void processData(T data);	
	abstract public void processDataQueue(int batchSize);
}