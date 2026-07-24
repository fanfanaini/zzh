package com.recruit.agent.screening.application;

import com.recruit.agent.screening.domain.ScreeningResult;
import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.task.application.CreateAgentTaskCommand;
import com.recruit.agent.task.checkpoint.domain.TaskCheckpointRepository;
import com.recruit.agent.task.checkpoint.infrastructure.InMemoryTaskCheckpointRepository;
import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskType;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class BatchScreeningApplicationService {

    private final AgentTaskApplicationService taskService;
    private final Function<ScreenCandidateCommand, ScreeningResult> candidateScreening;
    private final BatchScreeningStateStore stateStore;
    private final BatchScreeningPublisher publisher;
    private final TaskCheckpointRepository checkpointRepository;
    private final TransactionOperations transactions;

    public BatchScreeningApplicationService(
            AgentTaskApplicationService taskService,
            Function<ScreenCandidateCommand, ScreeningResult> candidateScreening,
            Executor executor
    ) {
        this(taskService, candidateScreening, executor, new InMemoryTaskCheckpointRepository());
    }

    public BatchScreeningApplicationService(
            AgentTaskApplicationService taskService,
            Function<ScreenCandidateCommand, ScreeningResult> candidateScreening,
            Executor executor,
            TaskCheckpointRepository checkpointRepository
    ) {
        this(taskService, candidateScreening, executor, checkpointRepository, null);
    }

    public BatchScreeningApplicationService(
            AgentTaskApplicationService taskService,
            Function<ScreenCandidateCommand, ScreeningResult> candidateScreening,
            Executor executor,
            TaskCheckpointRepository checkpointRepository,
            TransactionOperations transactions
    ) {
        this.taskService = taskService;
        this.candidateScreening = candidateScreening;
        this.stateStore = new com.recruit.agent.screening.infrastructure.InMemoryBatchScreeningStateStore();
        this.publisher = job -> executor.execute(() -> process(job));
        this.checkpointRepository = checkpointRepository;
        this.transactions = transactions;
    }

    public BatchScreeningApplicationService(
            AgentTaskApplicationService taskService,
            Function<ScreenCandidateCommand, ScreeningResult> candidateScreening,
            BatchScreeningStateStore stateStore,
            BatchScreeningPublisher publisher
    ) {
        this(taskService, candidateScreening, stateStore, publisher, new InMemoryTaskCheckpointRepository());
    }

    public BatchScreeningApplicationService(
            AgentTaskApplicationService taskService,
            Function<ScreenCandidateCommand, ScreeningResult> candidateScreening,
            BatchScreeningStateStore stateStore,
            BatchScreeningPublisher publisher,
            TaskCheckpointRepository checkpointRepository
    ) {
        this(taskService, candidateScreening, stateStore, publisher, checkpointRepository, null);
    }

    public BatchScreeningApplicationService(
            AgentTaskApplicationService taskService,
            Function<ScreenCandidateCommand, ScreeningResult> candidateScreening,
            BatchScreeningStateStore stateStore,
            BatchScreeningPublisher publisher,
            TaskCheckpointRepository checkpointRepository,
            TransactionOperations transactions
    ) {
        this.taskService = taskService;
        this.candidateScreening = candidateScreening;
        this.stateStore = stateStore;
        this.publisher = publisher;
        this.checkpointRepository = checkpointRepository;
        this.transactions = transactions;
    }

    public AgentTask submit(Long jobId, List<Long> candidateIds, String idempotencyKey) {
        if (jobId == null || candidateIds == null || candidateIds.isEmpty()) {
            throw new IllegalArgumentException("岗位和候选人列表不能为空");
        }
        List<Long> uniqueCandidateIds = candidateIds.stream().distinct().toList();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingTaskId = stateStore.findTaskByIdempotencyKey(idempotencyKey);
            if (existingTaskId.isPresent()) {
                return taskService.get(existingTaskId.get());
            }
        }
        AgentTask task = taskService.create(new CreateAgentTaskCommand(
                AgentTaskType.BATCH_SCREENING, jobId, uniqueCandidateIds.size()));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = stateStore.saveIdempotencyKey(idempotencyKey, task.id());
            if (existing.isPresent()) {
                return taskService.get(existing.get());
            }
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
            String checkpointKey = "CANDIDATE:" + candidateId;
            if (checkpointRepository.isCompleted(taskId, checkpointKey)) {
                taskService.recordSuccess(taskId, "候选人 " + candidateId,
                        "持久化检查点已完成，本次跳过");
                continue;
            }
            try {
                Runnable operation = () -> {
                    candidateScreening.apply(new ScreenCandidateCommand(job.jobId(), candidateId));
                    checkpointRepository.markCompleted(taskId, checkpointKey);
                    taskService.recordSuccess(taskId, "筛选候选人 " + candidateId, "筛选结果已保存");
                };
                if (transactions == null) {
                    operation.run();
                } else {
                    transactions.executeWithoutResult(status -> operation.run());
                }
            } catch (RuntimeException exception) {
                String message = exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();
                taskService.recordFailure(taskId, "筛选候选人 " + candidateId, message);
            }
        }
        taskService.finish(taskId);
    }
}
