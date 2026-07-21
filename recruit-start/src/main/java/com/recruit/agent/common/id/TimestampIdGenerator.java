package com.recruit.agent.common.id;

import java.util.concurrent.atomic.AtomicLong;

public class TimestampIdGenerator {
    private final AtomicLong lastIssued = new AtomicLong();

    public long nextId() {
        long timeFloor = System.currentTimeMillis() * 1000;
        return lastIssued.updateAndGet(previous -> Math.max(timeFloor, previous + 1));
    }
}
