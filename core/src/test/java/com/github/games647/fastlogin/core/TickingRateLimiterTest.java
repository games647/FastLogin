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

import com.github.games647.fastlogin.core.antibot.RateLimiter;
import com.github.games647.fastlogin.core.antibot.TickingRateLimiter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickingRateLimiterTest {

    /**
     * Always expired
     */
    @ParameterizedTest
    @ValueSource(longs = {5_000_000L, -5_000_000L})
    void allowExpire(long initial) {
        int size = 3;

        FakeTicker ticker = new FakeTicker(initial);

        // run twice the size to fill it first and then test it
        RateLimiter rateLimiter = new TickingRateLimiter(ticker, size, 0);
        for (int i = 0; i < size; i++) {
            assertTrue(rateLimiter.tryAcquire(), "Filling up");
        }

        for (int i = 0; i < size; i++) {
            ticker.add(Duration.ofSeconds(1));
            assertTrue(rateLimiter.tryAcquire(), "Should be expired");
        }
    }

    /**
     * Too many requests
     */
    @ParameterizedTest
    @ValueSource(longs = {5_000_000L, -5_000_000L})
    void shouldBlock(long initial) {
        int size = 3;

        FakeTicker ticker = new FakeTicker(initial);

        // fill the size
        RateLimiter rateLimiter = new TickingRateLimiter(ticker, size, TimeUnit.SECONDS.toMillis(30));
        for (int i = 0; i < size; i++) {
            assertTrue(rateLimiter.tryAcquire(), "Filling up");
        }

        assertFalse(rateLimiter.tryAcquire(), "Should be full and no entry should be expired");
    }
    
    /**
     * Blocked attempts shouldn't replace existing ones.
     */
    @ParameterizedTest
    @ValueSource(longs = {5_000_000L, -5_000_000L})
    void blockedNotAdded(long initial) {
        FakeTicker ticker = new FakeTicker(initial);

        // fill the size - 100ms should be reasonable high
        RateLimiter rateLimiter = new TickingRateLimiter(ticker, 1, 100);
        assertTrue(rateLimiter.tryAcquire(), "Filling up");

        ticker.add(Duration.ofMillis(50));

        // still is full - should fail
        assertFalse(rateLimiter.tryAcquire(), "Expired too early");

        // wait the remaining time and add a threshold, because
        ticker.add(Duration.ofMillis(50));
        assertTrue(rateLimiter.tryAcquire(), "Request not released");
    }
}
