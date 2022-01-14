package com.github.games647.fastlogin.core;

import com.google.common.base.Ticker;

import java.time.Duration;

public class FakeTicker extends Ticker {

    private long timestamp;

    public FakeTicker(long initial) {
        timestamp = initial;
    }

    @Override
    public long read() {
        return timestamp;
    }

    public void add(Duration duration) {
        timestamp += duration.toNanos();
    }
}
