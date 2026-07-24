package com.recruit.agent.candidate.application;

import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.task.application.CreateAgentTaskCommand;
import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskType;
import com.recruit.agent.task.checkpoint.domain.TaskCheckpointRepository;
import com.recruit.agent.task.checkpoint.infrastructure.InMemoryTaskCheckpointRepository;

import java.util.concurrent.Executor;

public class ResumeParsingTaskService {

    private final AgentTaskApplicationService taskService;
    private final CandidateResumeTextExtractionService textExtractionService;
    private final CandidateProfileExtractionService profileExtractionService;
    private final TaskCheckpointRepository checkpointRepository;
    private final Executor executor;

    public ResumeParsingTaskService(
            AgentTaskApplicationService taskService,
            CandidateResumeTextExtractionService textExtractionService,
            CandidateProfileExtractionService profileExtractionService,
            Executor executor
    ) {
        this(taskService, textExtractionService, profileExtractionService,
                new InMemoryTaskCheckpointRepository(), executor);
    }

    public ResumeParsingTaskService(
            AgentTaskApplicationService taskService,
            CandidateResumeTextExtractionService textExtractionService,
            CandidateProfileExtractionService profileExtractionService,
            TaskCheckpointRepository checkpointRepository,
            Executor executor
    ) {
        this.taskService = taskService;
        this.textExtractionService = textExtractionService;
        this.profileExtractionService = profileExtractionService;
        this.checkpointRepository = checkpointRepository;
        this.executor = executor;
    }

    public AgentTask submit(Long resumeId) {
        if (resumeId == null) {
            throw new IllegalArgumentException("简历 ID 不能为空");
        }
        AgentTask task = taskService.create(new CreateAgentTaskCommand(
                AgentTaskType.RESUME_PARSE,
                resumeId,
                1
        ));
        schedule(task);
        return task;
    }

    public AgentTask retry(Long taskId) {
        AgentTask task = taskService.get(taskId);
        if (task.taskType() != AgentTaskType.RESUME_PARSE) {
            throw new IllegalArgumentException("该任务不是简历解析任务");
        }
        AgentTask retried = taskService.retry(taskId);
        schedule(retried);
        return retried;
    }

    public void schedule(AgentTask task) {
        if (task.targetId() == null) {
            taskService.recordFailure(task.id(), "参数校验", "RESUME_PARSE 需要 targetId=resumeId");
            taskService.finish(task.id());
            return;
        }
        executor.execute(() -> execute(task.id(), task.targetId()));
    }

    private void execute(Long taskId, Long resumeId) {
        try {
            taskService.start(taskId);
            if (!checkpointRepository.isCompleted(taskId, "TEXT_EXTRACTED")) {
                textExtractionService.extractText(resumeId);
                checkpointRepository.markCompleted(taskId, "TEXT_EXTRACTED");
            }
            if (!checkpointRepository.isCompleted(taskId, "PROFILE_EXTRACTED")) {
                profileExtractionService.extractProfile(resumeId);
                checkpointRepository.markCompleted(taskId, "PROFILE_EXTRACTED");
            }
            taskService.recordSuccess(taskId, "简历解析", "文本提取、候选人画像和结构化详情已更新");
        } catch (RuntimeException | LinkageError exception) {
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            taskService.recordFailure(taskId, "简历解析", message);
        } finally {
            taskService.finish(taskId);
        }
    }
}
