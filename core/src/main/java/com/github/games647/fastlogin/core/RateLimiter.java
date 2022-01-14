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

import java.util.Arrays;

/**
 * Limit the number of requests with a maximum size. Each requests expire after the specified time making it available
 * for another request.
 */
public class RateLimiter {

    private final long[] requests;
    private final long expireTime;
    private int position;

    public RateLimiter(int maxLimit, long expireTime) {
        this.requests = new long[maxLimit];
        this.expireTime = expireTime;

        // fill the array with the lowest values, so that the first uninitialized values will always expire
        Arrays.fill(requests, Long.MIN_VALUE);
    }

    /**
     * Ask if access is allowed. If so register the request.
     *
     * @return true if allowed - false otherwise without any side effects
     */
    public boolean tryAcquire() {
        // current time millis is not monotonic - it can jump back depending on user choice or NTP
        long now = System.nanoTime() / 1_000_000;

        // after this the request should be expired
        long toBeExpired = now - expireTime;
        synchronized (this) {
            // having synchronized will limit the amount of concurrency a lot
            long oldest = requests[position];
            if (oldest < toBeExpired) {
                requests[position] = now;
                position = (position + 1) % requests.length;
                return true;
            }

            return false;
        }
    }
}
