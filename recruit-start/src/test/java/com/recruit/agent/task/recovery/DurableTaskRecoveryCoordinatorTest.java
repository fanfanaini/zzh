package com.recruit.agent.task.recovery;

import com.recruit.agent.candidate.application.ResumeParsingTaskService;
import com.recruit.agent.screening.application.BatchScreeningApplicationService;
import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskStatus;
import com.recruit.agent.task.domain.AgentTaskType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DurableTaskRecoveryCoordinatorTest {
    @Test
    void resumesOnlyPendingOrRunningSupportedTasks() {
        AgentTaskApplicationService tasks = mock(AgentTaskApplicationService.class);
        ResumeParsingTaskService resume = mock(ResumeParsingTaskService.class);
        BatchScreeningApplicationService batch = mock(BatchScreeningApplicationService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BatchScreeningApplicationService> provider = mock(ObjectProvider.class);
        Environment environment = mock(Environment.class);
        when(environment.acceptsProfiles(org.mockito.ArgumentMatchers.any(Profiles.class))).thenReturn(true);
        when(provider.getIfAvailable()).thenReturn(batch);
        AgentTask pendingResume = task(1L, AgentTaskType.RESUME_PARSE, AgentTaskStatus.PENDING);
        AgentTask runningBatch = task(2L, AgentTaskType.BATCH_SCREENING, AgentTaskStatus.RUNNING);
        AgentTask completed = task(3L, AgentTaskType.RESUME_PARSE, AgentTaskStatus.SUCCEEDED);
        when(tasks.list()).thenReturn(List.of(pendingResume, runningBatch, completed));

        new DurableTaskRecoveryCoordinator(tasks, resume, provider, environment).recoverIncompleteTasks();

        verify(resume).schedule(pendingResume);
        verify(batch).retry(2L);
        verify(resume, never()).schedule(completed);
    }

    private AgentTask task(Long id, AgentTaskType type, AgentTaskStatus status) {
        return new AgentTask(id, type, status, id, 1, 0, 0, Instant.EPOCH, Instant.EPOCH);
    }
}
