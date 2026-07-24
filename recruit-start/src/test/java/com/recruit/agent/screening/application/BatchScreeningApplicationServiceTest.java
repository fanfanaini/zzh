package com.recruit.agent.screening.application;

import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.task.checkpoint.infrastructure.InMemoryTaskCheckpointRepository;
import com.recruit.agent.task.domain.AgentTaskStatus;
import com.recruit.agent.task.infrastructure.InMemoryAgentStepRepository;
import com.recruit.agent.task.infrastructure.InMemoryAgentTaskRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    @Test
    void retrySkipsCandidatesWithCompletedCheckpoints() {
        AgentTaskApplicationService tasks = new AgentTaskApplicationService(
                new InMemoryAgentTaskRepository(), new InMemoryAgentStepRepository());
        var stateStore = new com.recruit.agent.screening.infrastructure.InMemoryBatchScreeningStateStore();
        var checkpoints = new InMemoryTaskCheckpointRepository();
        Map<Long, AtomicInteger> calls = new ConcurrentHashMap<>();
        BatchScreeningApplicationService service = new BatchScreeningApplicationService(
                tasks,
                command -> {
                    int attempt = calls.computeIfAbsent(command.candidateId(), ignored -> new AtomicInteger())
                            .incrementAndGet();
                    if (command.candidateId() == 2L && attempt == 1) {
                        throw new IllegalStateException("model timeout");
                    }
                    return null;
                },
                stateStore,
                job -> { },
                checkpoints
        );

        var task = service.submit(10L, List.of(1L, 2L, 3L), "retry-key");
        service.process(stateStore.findJob(task.id()).orElseThrow());
        service.retry(task.id());
        service.process(stateStore.findJob(task.id()).orElseThrow());

        assertThat(calls.get(1L)).hasValue(1);
        assertThat(calls.get(2L)).hasValue(2);
        assertThat(calls.get(3L)).hasValue(1);
        assertThat(checkpoints.isCompleted(task.id(), "CANDIDATE:1")).isTrue();
        assertThat(checkpoints.isCompleted(task.id(), "CANDIDATE:2")).isTrue();
        assertThat(checkpoints.isCompleted(task.id(), "CANDIDATE:3")).isTrue();
        assertThat(tasks.get(task.id()).status()).isEqualTo(AgentTaskStatus.SUCCEEDED);
        assertThat(tasks.get(task.id()).successCount()).isEqualTo(3);
    }
}
