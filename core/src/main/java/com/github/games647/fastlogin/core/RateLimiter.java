package com.github.games647.fastlogin.core;

@FunctionalInterface
public interface RateLimiter {

    boolean tryAcquire();
}
