/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2021 <Your name and contributors>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.core;

import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

/**
 * This limits the number of threads that are used at maximum. Thread creation can be very heavy for the CPU and
 * context switching between threads too. However, we need many threads for blocking HTTP and database calls.
 * Nevertheless, this number can be further limited, because the number of actually working database threads
 * is limited by the size of our database pool. The goal is to separate concerns into processing and blocking only
 * threads.
 */
public class AsyncScheduler {

    private static final int MAX_CAPACITY = 1024;

    //todo: single thread for delaying and scheduling tasks
    private final Logger logger;

    // 30 threads are still too many - the optimal solution is to separate into processing and blocking threads
    // where processing threads could only be max number of cores while blocking threads could be minimized using
    // non-blocking I/O and a single event executor
    private final ExecutorService processingPool;

    /*
    private final ExecutorService databaseExecutor = new ThreadPoolExecutor(1, 10,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(MAX_CAPACITY));
     */

    public AsyncScheduler(Logger logger, ThreadFactory threadFactory) {
        this.logger = logger;
        processingPool = new ThreadPoolExecutor(6, 32,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(MAX_CAPACITY), threadFactory);
    }

    /*
    public <R> CompletableFuture<R> runDatabaseTask(Supplier<R> databaseTask) {
        return CompletableFuture.supplyAsync(databaseTask, databaseExecutor)
                .exceptionally(error -> {
                    logger.warn("Error occurred on thread pool", error);
                    return null;
                })
                // change context to the processing pool
                .thenApplyAsync(r -> r, processingPool);
    }
     */

    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, processingPool).exceptionally(error -> {
            logger.warn("Error occurred on thread pool", error);
            return null;
        });
    }

    public void shutdown() {
        MoreExecutors.shutdownAndAwaitTermination(processingPool, 1, TimeUnit.MINUTES);
        //MoreExecutors.shutdownAndAwaitTermination(databaseExecutor, 1, TimeUnit.MINUTES);
    }
}
