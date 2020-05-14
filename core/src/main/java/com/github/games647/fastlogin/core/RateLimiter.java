package com.github.games647.fastlogin.core;

/**
 * Limit the number of requests with a maximum size. Each requests expires after the specified time making it available
 * for another request.
 */
public class RateLimiter {

    private final long[] requests;
    private final long expireTime;
    private int position;

    public RateLimiter(int maxLimit, long expireTime) {
        this.requests = new long[maxLimit];
        this.expireTime = expireTime;
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
