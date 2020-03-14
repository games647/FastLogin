package com.github.games647.fastlogin.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This limits the number of threads that are used at maximum. Thread creation can be very heavy for the CPU and
 * context switching between threads too. However we need many threads for blocking HTTP and database calls.
 * Nevertheless this number can be further limited, because the number of actually working database threads
 * is limited by the size of our database pool. The goal is to separate concerns into processing and blocking only
 * threads.
 */
public class AsyncScheduler {

    // 30 threads are still too many - the optimal solution is to separate into processing and blocking threads
    // where processing threads could only be max number of cores while blocking threads could be minimized using
    // non-blocking I/O and a single event executor
    private final ThreadPoolExecutor executorService;

    public AsyncScheduler(ThreadFactory threadFactory) {
        executorService = new ThreadPoolExecutor(5, 30,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024), threadFactory);
    }

    public void runAsync(Runnable task) {
        executorService.execute(task);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException interruptEx) {
            Thread.currentThread().interrupt();
        }
    }
}
