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

import com.github.games647.fastlogin.core.antibot.TickingRateLimiter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TickingRateLimiterTest {

    private static final long THRESHOLD_MILLI = 10;

    /**
     * Always expired
     */
    @Test
    public void allowExpire() {
        int size = 3;

        FakeTicker ticker = new FakeTicker(5_000_000L);

        // run twice the size to fill it first and then test it
        TickingRateLimiter rateLimiter = new TickingRateLimiter(ticker, size, 0);
        for (int i = 0; i < size; i++) {
            assertThat("Filling up", rateLimiter.tryAcquire(), is(true));
        }

        for (int i = 0; i < size; i++) {
            ticker.add(Duration.ofSeconds(1));
            assertThat("Should be expired", rateLimiter.tryAcquire(), is(true));
        }
    }

    @Test
    public void allowExpireNegative() {
        int size = 3;

        FakeTicker ticker = new FakeTicker(-5_000_000L);

        // run twice the size to fill it first and then test it
        TickingRateLimiter rateLimiter = new TickingRateLimiter(ticker, size, 0);
        for (int i = 0; i < size; i++) {
            assertThat("Filling up", rateLimiter.tryAcquire(), is(true));
        }

        for (int i = 0; i < size; i++) {
            ticker.add(Duration.ofSeconds(1));
            assertThat("Should be expired", rateLimiter.tryAcquire(), is(true));
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
            assertThat("Filling up", rateLimiter.tryAcquire(), is(true));
        }

        assertThat("Should be full and no entry should be expired", rateLimiter.tryAcquire(), is(false));
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
            assertThat("Filling up", rateLimiter.tryAcquire(), is(true));
        }

        assertThat("Should be full and no entry should be expired", rateLimiter.tryAcquire(), is(false));
    }

    /**
     * Blocked attempts shouldn't replace existing ones.
     */
    @Test
    public void blockedNotAdded() {
        FakeTicker ticker = new FakeTicker(5_000_000L);

        // fill the size - 100ms should be reasonable high
        TickingRateLimiter rateLimiter = new TickingRateLimiter(ticker, 1, 100);
        assertThat("Filling up", rateLimiter.tryAcquire(), is(true));

        ticker.add(Duration.ofMillis(50));

        // still is full - should fail
        assertThat("Expired too early", rateLimiter.tryAcquire(), is(false));

        // wait the remaining time and add a threshold, because
        ticker.add(Duration.ofMillis(50));
        assertThat("Request not released", rateLimiter.tryAcquire(), is(true));
    }

    /**
     * Blocked attempts shouldn't replace existing ones.
     */
    @Test
    public void blockedNotAddedNegative() {
        FakeTicker ticker = new FakeTicker(-5_000_000L);

        // fill the size - 100ms should be reasonable high
        TickingRateLimiter rateLimiter = new TickingRateLimiter(ticker, 1, 100);
        assertThat("Filling up", rateLimiter.tryAcquire(), is(true));

        ticker.add(Duration.ofMillis(50));

        // still is full - should fail
        assertThat("Expired too early", rateLimiter.tryAcquire(), is(false));

        // wait the remaining time and add a threshold, because
        ticker.add(Duration.ofMillis(50));
        assertThat("Request not released", rateLimiter.tryAcquire(), is(true));
    }
}
