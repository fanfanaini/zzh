package com.recruit.agent.screening.application;

import com.recruit.agent.screening.domain.ScreeningResult;
import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.task.application.CreateAgentTaskCommand;
import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskType;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class BatchScreeningApplicationService {

    private final AgentTaskApplicationService taskService;
    private final Function<ScreenCandidateCommand, ScreeningResult> candidateScreening;
    private final BatchScreeningStateStore stateStore;
    private final BatchScreeningPublisher publisher;

    public BatchScreeningApplicationService(
            AgentTaskApplicationService taskService,
            Function<ScreenCandidateCommand, ScreeningResult> candidateScreening,
            Executor executor
    ) {
        this.taskService = taskService;
        this.candidateScreening = candidateScreening;
        this.stateStore = new com.recruit.agent.screening.infrastructure.InMemoryBatchScreeningStateStore();
        this.publisher = job -> executor.execute(() -> process(job));
    }

    public BatchScreeningApplicationService(AgentTaskApplicationService taskService,
            Function<ScreenCandidateCommand, ScreeningResult> candidateScreening,
            BatchScreeningStateStore stateStore, BatchScreeningPublisher publisher) {
        this.taskService = taskService;
        this.candidateScreening = candidateScreening;
        this.stateStore = stateStore;
        this.publisher = publisher;
    }

    public AgentTask submit(Long jobId, List<Long> candidateIds, String idempotencyKey) {
        if (jobId == null || candidateIds == null || candidateIds.isEmpty()) {
            throw new IllegalArgumentException("岗位和候选人列表不能为空");
        }
        List<Long> uniqueCandidateIds = candidateIds.stream().distinct().toList();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingTaskId = stateStore.findTaskByIdempotencyKey(idempotencyKey);
            if (existingTaskId.isPresent()) return taskService.get(existingTaskId.get());
        }
        AgentTask task = taskService.create(new CreateAgentTaskCommand(
                AgentTaskType.BATCH_SCREENING, jobId, uniqueCandidateIds.size()));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = stateStore.saveIdempotencyKey(idempotencyKey, task.id());
            if (existing.isPresent()) return taskService.get(existing.get());
        }
        BatchScreeningJob job = new BatchScreeningJob(task.id(), jobId, uniqueCandidateIds);
        stateStore.saveJob(job);
        publisher.publish(job);
        return task;
    }

    public AgentTask retry(Long taskId) {
        BatchScreeningJob job = stateStore.findJob(taskId)
                .orElseThrow(() -> new IllegalStateException("批量任务参数不存在，无法重新执行: " + taskId));
        AgentTask task = taskService.retry(taskId);
        publisher.publish(job);
        return task;
    }

    public void process(BatchScreeningJob job) {
        Long taskId = job.taskId();
        taskService.start(taskId);
        for (Long candidateId : job.candidateIds()) {
            try {
                candidateScreening.apply(new ScreenCandidateCommand(job.jobId(), candidateId));
                taskService.recordSuccess(taskId, "筛选候选人 " + candidateId, "筛选结果已保存");
            } catch (RuntimeException exception) {
                String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                taskService.recordFailure(taskId, "筛选候选人 " + candidateId, message);
            }
        }
        taskService.finish(taskId);
    }

}
