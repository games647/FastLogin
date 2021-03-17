package com.github.games647.fastlogin.core;

import com.github.games647.fastlogin.core.auth.RateLimiter;

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

        // run twice the size to fill it first and then test it
        RateLimiter rateLimiter = new RateLimiter(size, 0);
        for (int i = 0; i < size; i++) {
            assertTrue("Filling up", rateLimiter.tryAcquire());
        }

        for (int i = 0; i < size; i++) {
            Thread.sleep(1);
            assertTrue("Should be expired", rateLimiter.tryAcquire());
        }
    }

    /**
     * Too many requests
     */
    @Test
    public void shoudBlock() {
        int size = 3;

        // fill the size
        RateLimiter rateLimiter = new RateLimiter(size, TimeUnit.SECONDS.toMillis(30));
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
        // fill the size - 100ms should be reasonable high
        RateLimiter rateLimiter = new RateLimiter(1, 100);
        assertTrue("Filling up", rateLimiter.tryAcquire());

        Thread.sleep(50);

        // still is full - should fail
        assertFalse("Expired too early", rateLimiter.tryAcquire());

        // wait the remaining time and add a threshold, because
        Thread.sleep(50 + THRESHOLD_MILLI);
        assertTrue("Request not released", rateLimiter.tryAcquire());
    }
}
