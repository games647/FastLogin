/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2023 games647 and contributors
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
package com.github.games647.fastlogin.core.antibot;

import com.google.common.base.Ticker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 * Limit the number of requests with a maximum size. Each requests expire after the specified time making it available
 * for another request.
 */
public class TickingRateLimiter implements RateLimiter {

    private final Ticker ticker;

    // amount of milliseconds to expire
    private final long expireTime;

    // total request limit
    private final int requestLimit;

    private final Deque<TimeRecord> records;
    private int totalRequests;

    public TickingRateLimiter(Ticker ticker, int maxLimit, long expireTime) {
        this.ticker = ticker;

        this.requestLimit = maxLimit;
        this.expireTime = expireTime;

        records = new ArrayDeque<>(10);
    }

    /**
     * Ask if access is allowed. If so register the request.
     *
     * @return true if allowed - false otherwise without any side effects
     */
    @Override
    public boolean tryAcquire() {
        // current time millis is not monotonic - it can jump back depending on user choice or NTP
        long nowMilli = ticker.read() / 1_000_000;
        synchronized (this) {
            // having synchronized will limit the amount of concurrency a lot
            TimeRecord oldest = records.peekFirst();
            if (oldest != null && oldest.hasExpired(nowMilli)) {
                records.pop();
                totalRequests -= oldest.getRequestCount();
            }

            // total requests reached block any further requests
            if (totalRequests >= requestLimit) {
                return false;
            }

            TimeRecord latest = records.peekLast();
            if (latest == null) {
                // empty list - add new record
                records.add(new TimeRecord(nowMilli, expireTime));
                totalRequests++;
                return true;
            }

            int res = latest.compareTo(nowMilli);
            if (res < 0) {
                // now is before than the record means time jumps
                throw new IllegalStateException("Time jumped back");
            }

            if (res == 0) {
                // same minute record
                latest.hit();
                totalRequests++;
                return true;
            }

            // now is one minute newer
            records.add(new TimeRecord(nowMilli, expireTime));
            totalRequests++;
            return true;
        }
    }

    private static class TimeRecord implements Comparable<Long> {

        private final long firstMinuteRecord;
        private final long expireTime;
        private int count;

        TimeRecord(long firstMinuteRecord, long expireTime) {
            this.firstMinuteRecord = firstMinuteRecord;
            this.expireTime = expireTime;
            this.count = 1;
        }

        public void hit() {
            count++;
        }

        public int getRequestCount() {
            return count;
        }

        public boolean hasExpired(long now) {
            return firstMinuteRecord + expireTime <= now;
        }

        @Override
        public int compareTo(Long other) {
            if (other < firstMinuteRecord) {
                return -1;
            }

            if (other > firstMinuteRecord + TimeUnit.MINUTES.toMillis(1)) {
                return +1;
            }

            return 0;
        }
    }
}
