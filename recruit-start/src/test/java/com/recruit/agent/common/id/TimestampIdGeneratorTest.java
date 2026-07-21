package com.recruit.agent.common.id;

import org.junit.jupiter.api.Test;

import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class TimestampIdGeneratorTest {

    @Test
    void issuesStrictlyIncreasingIdsWithinTheSameProcess() {
        TimestampIdGenerator generator = new TimestampIdGenerator();

        long[] ids = LongStream.range(0, 10_000).map(ignored -> generator.nextId()).toArray();

        assertThat(ids).doesNotHaveDuplicates();
        for (int index = 1; index < ids.length; index++) {
            assertThat(ids[index]).isGreaterThan(ids[index - 1]);
        }
    }
}
