/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RateLimiterTest {

    private static final long THRESHOLD_MILLI = 10;

    /**
     * Always expired
     */
    @Test
    public void allowExpire() throws InterruptedException {
        int size = 3;

        FakeTicker ticker = new FakeTicker(5_000_000L);

        // run twice the size to fill it first and then test it
        TickingRateLimiter rateLimiter = new TickingRateLimiter(ticker, size, 0);
        for (int i = 0; i < size; i++) {
            assertTrue("Filling up", rateLimiter.tryAcquire());
        }

        for (int i = 0; i < size; i++) {
            ticker.add(Duration.ofSeconds(1));
            assertTrue("Should be expired", rateLimiter.tryAcquire());
        }
    }

    @Test
    public void allowExpireNegative() throws InterruptedException {
        int size = 3;

        FakeTicker ticker = new FakeTicker(-5_000_000L);

        // run twice the size to fill it first and then test it
        TickingRateLimiter rateLimiter = new TickingRateLimiter(ticker, size, 0);
        for (int i = 0; i < size; i++) {
            assertTrue("Filling up", rateLimiter.tryAcquire());
        }

        for (int i = 0; i < size; i++) {
            ticker.add(Duration.ofSeconds(1));
            assertTrue("Should be expired", rateLimiter.tryAcquire());
        }
    }

    /**
     * Too many requests
     */
    @Test
    public void shouldBlock() {
        int size = 3;

        FakeTicker ticker = new FakeTicker(5_000_000L);

        // fill the size
        TickingRateLimiter rateLimiter = new TickingRateLimiter(ticker, size, TimeUnit.SECONDS.toMillis(30));
        for (int i = 0; i < size; i++) {
            assertTrue("Filling up", rateLimiter.tryAcquire());
        }

        assertFalse("Should be full and no entry should be expired", rateLimiter.tryAcquire());
    }

    /**
     * Too many requests
     */
    @Test
    public void shouldBlockNegative() {
        int size = 3;

        FakeTicker ticker = new FakeTicker(-5_000_000L);

        // fill the size
        TickingRateLimiter rateLimiter = new TickingRateLimiter(ticker, size, TimeUnit.SECONDS.toMillis(30));
        for (int i = 0; i < size; i++) {
            assertTrue("Filling up", rateLimiter.tryAcquire());
        }

        assertFalse("Should be full and no entry should be expired", rateLimiter.tryAcquire());
    }

    /**
     * Blocked attempts shouldn't replace existing ones.
     */
    @Test
    public void blockedNotAdded() throws InterruptedException {
        FakeTicker ticker = new FakeTicker(5_000_000L);

        // fill the size - 100ms should be reasonable high
        TickingRateLimiter rateLimiter = new TickingRateLimiter(ticker, 1, 100);
        assertTrue("Filling up", rateLimiter.tryAcquire());

        ticker.add(Duration.ofMillis(50));

        // still is full - should fail
        assertFalse("Expired too early", rateLimiter.tryAcquire());

        // wait the remaining time and add a threshold, because
        ticker.add(Duration.ofMillis(50));
        assertTrue("Request not released", rateLimiter.tryAcquire());
    }

    /**
     * Blocked attempts shouldn't replace existing ones.
     */
    @Test
    public void blockedNotAddedNegative() throws InterruptedException {
        FakeTicker ticker = new FakeTicker(-5_000_000L);

        // fill the size - 100ms should be reasonable high
        TickingRateLimiter rateLimiter = new TickingRateLimiter(ticker, 1, 100);
        assertTrue("Filling up", rateLimiter.tryAcquire());

        ticker.add(Duration.ofMillis(50));

        // still is full - should fail
        assertFalse("Expired too early", rateLimiter.tryAcquire());

        // wait the remaining time and add a threshold, because
        ticker.add(Duration.ofMillis(50));
        assertTrue("Request not released", rateLimiter.tryAcquire());
    }
}
