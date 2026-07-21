package com.recruit.agent.common.id;

import java.util.concurrent.ThreadLocalRandom;

public class TimestampIdGenerator {

    public long nextId() {
        long timestamp = System.currentTimeMillis();
        long random = ThreadLocalRandom.current().nextLong(1000);
        return timestamp * 1000 + random;
    }
}
