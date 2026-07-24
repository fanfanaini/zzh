package com.recruit.agent.candidate.application;

import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.task.domain.AgentTaskStatus;
import com.recruit.agent.task.checkpoint.infrastructure.InMemoryTaskCheckpointRepository;
import com.recruit.agent.task.infrastructure.InMemoryAgentStepRepository;
import com.recruit.agent.task.infrastructure.InMemoryAgentTaskRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeParsingTaskServiceTest {

    @Test
    void executesResumeParsingAsBackgroundTask() {
        var taskRepository = new InMemoryAgentTaskRepository();
        var stepRepository = new InMemoryAgentStepRepository();
        var taskService = new AgentTaskApplicationService(taskRepository, stepRepository);
        var textService = mock(CandidateResumeTextExtractionService.class);
        var profileService = mock(CandidateProfileExtractionService.class);
        var service = new ResumeParsingTaskService(taskService, textService, profileService, Runnable::run);

        var submitted = service.submit(3001L);

        verify(textService).extractText(3001L);
        verify(profileService).extractProfile(3001L);
        assertThat(taskService.get(submitted.id()).status()).isEqualTo(AgentTaskStatus.SUCCEEDED);
        assertThat(taskService.get(submitted.id()).successCount()).isEqualTo(1);
        assertThat(taskService.listSteps(submitted.id()))
                .extracting(step -> step.stepName())
                .contains("任务已创建", "任务开始执行", "简历解析");
    }

    @Test
    void marksTaskFailedWhenDocumentParserHasLinkageError() {
        var taskRepository = new InMemoryAgentTaskRepository();
        var stepRepository = new InMemoryAgentStepRepository();
        var taskService = new AgentTaskApplicationService(taskRepository, stepRepository);
        var textService = mock(CandidateResumeTextExtractionService.class);
        var profileService = mock(CandidateProfileExtractionService.class);
        var service = new ResumeParsingTaskService(taskService, textService, profileService, Runnable::run);
        doThrow(new NoClassDefFoundError("missing-parser-class")).when(textService).extractText(3001L);

        var submitted = service.submit(3001L);

        assertThat(taskService.get(submitted.id()).status()).isEqualTo(AgentTaskStatus.FAILED);
        assertThat(taskService.get(submitted.id()).failureCount()).isEqualTo(1);
        assertThat(taskService.listSteps(submitted.id()))
                .anySatisfy(step -> {
                    assertThat(step.status()).isEqualTo(AgentTaskStatus.FAILED);
                    assertThat(step.message()).contains("missing-parser-class");
                });
    }

    @Test
    void retriesFromLastCompletedBusinessCheckpoint() {
        var taskRepository = new InMemoryAgentTaskRepository();
        var stepRepository = new InMemoryAgentStepRepository();
        var checkpointRepository = new InMemoryTaskCheckpointRepository();
        var taskService = new AgentTaskApplicationService(taskRepository, stepRepository);
        var textService = mock(CandidateResumeTextExtractionService.class);
        var profileService = mock(CandidateProfileExtractionService.class);
        var service = new ResumeParsingTaskService(
                taskService, textService, profileService, checkpointRepository, Runnable::run);
        when(profileService.extractProfile(3001L))
                .thenThrow(new IllegalStateException("profile timeout"))
                .thenReturn(mock(com.recruit.agent.candidate.domain.Candidate.class));

        var submitted = service.submit(3001L);
        var retried = service.retry(submitted.id());

        verify(textService, times(1)).extractText(3001L);
        verify(profileService, times(2)).extractProfile(3001L);
        assertThat(checkpointRepository.isCompleted(submitted.id(), "TEXT_EXTRACTED")).isTrue();
        assertThat(checkpointRepository.isCompleted(submitted.id(), "PROFILE_EXTRACTED")).isTrue();
        assertThat(taskService.get(retried.id()).status()).isEqualTo(AgentTaskStatus.SUCCEEDED);
        assertThat(taskService.get(retried.id()).successCount()).isEqualTo(1);
    }
}
