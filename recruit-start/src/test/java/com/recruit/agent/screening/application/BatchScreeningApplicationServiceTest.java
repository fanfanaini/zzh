package com.recruit.agent.screening.application;

import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.task.domain.AgentTaskStatus;
import com.recruit.agent.task.infrastructure.InMemoryAgentStepRepository;
import com.recruit.agent.task.infrastructure.InMemoryAgentTaskRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BatchScreeningApplicationServiceTest {

    @Test
    void shouldRunEveryCandidateAndRecordProgress() {
        AgentTaskApplicationService tasks = new AgentTaskApplicationService(
                new InMemoryAgentTaskRepository(), new InMemoryAgentStepRepository());
        AtomicInteger calls = new AtomicInteger();
        BatchScreeningApplicationService service = new BatchScreeningApplicationService(
                tasks,
                command -> {
                    calls.incrementAndGet();
                    if (command.candidateId() == 2L) {
                        throw new IllegalStateException("model timeout");
                    }
                    return null;
                },
                Runnable::run
        );

        var task = service.submit(10L, List.of(1L, 2L, 3L), "request-1");

        var completed = tasks.get(task.id());
        assertThat(calls).hasValue(3);
        assertThat(completed.status()).isEqualTo(AgentTaskStatus.FAILED);
        assertThat(completed.successCount()).isEqualTo(2);
        assertThat(completed.failureCount()).isEqualTo(1);
        assertThat(tasks.listSteps(task.id())).hasSize(5);
    }

    @Test
    void shouldReturnSameTaskForSameIdempotencyKey() {
        AgentTaskApplicationService tasks = new AgentTaskApplicationService(
                new InMemoryAgentTaskRepository(), new InMemoryAgentStepRepository());
        BatchScreeningApplicationService service = new BatchScreeningApplicationService(
                tasks, command -> null, Runnable::run);

        var first = service.submit(10L, List.of(1L), "same-key");
        var second = service.submit(10L, List.of(1L), "same-key");

        assertThat(second.id()).isEqualTo(first.id());
    }
}
