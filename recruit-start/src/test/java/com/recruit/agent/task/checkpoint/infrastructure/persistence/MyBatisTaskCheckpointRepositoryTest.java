package com.recruit.agent.task.checkpoint.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MyBatisTaskCheckpointRepositoryTest {

    private final TaskCheckpointMapper mapper = mock(TaskCheckpointMapper.class);
    private final MyBatisTaskCheckpointRepository repository = new MyBatisTaskCheckpointRepository(mapper);

    @Test
    void storesAndFindsCompletedBusinessNode() {
        repository.markCompleted(41L, "CANDIDATE:9");

        verify(mapper).markCompleted(
                org.mockito.ArgumentMatchers.eq(41L),
                org.mockito.ArgumentMatchers.eq("CANDIDATE:9"),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));

        when(mapper.countCompleted(41L, "CANDIDATE:9")).thenReturn(1);
        assertThat(repository.isCompleted(41L, "CANDIDATE:9")).isTrue();
    }
}
