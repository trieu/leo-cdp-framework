package leotech.system.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import leotech.system.version.SystemMetaData;

/**
 * Thread pool tuned for high-throughput I/O workloads (Redis, HTTP, Kafka, etc.)
 * - Core threads: number of CPU cores
 * - Max threads: 5x cores for burst traffic
 * - Queue: bounded to provide backpressure without blowing RAM
 * - Rejection: CallerRunsPolicy to slow callers instead of dropping tasks
 * - Threads: named & daemon-safe for observability
 */
public final class IoProcessors {

    public static final ExecutorService EXECUTOR;

    static {
        int cores = SystemMetaData.NUMBER_CORE_CPU;
        int maxThreads = cores * 5;
        int queueSize = 5_000 * cores;

        EXECUTOR = new ThreadPoolExecutor(
                cores,
                maxThreads,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                new IoThreadFactory("leo-http"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // optional but wise: prestart core threads for lower latency spikes
        ((ThreadPoolExecutor) EXECUTOR).prestartAllCoreThreads();
    }

    private IoProcessors() {
        // utility class
    }

    /**
     * Clean thread factory that names threads predictably.
     */
    private static class IoThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(1);
        private final String prefix;

        private IoThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);  // daemon ensures shutdown doesn't hang JVM
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}

