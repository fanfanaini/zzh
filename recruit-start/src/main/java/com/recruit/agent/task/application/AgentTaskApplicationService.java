package com.recruit.agent.task.application;

import com.recruit.agent.task.domain.AgentStep;
import com.recruit.agent.task.domain.AgentStepRepository;
import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskRepository;
import com.recruit.agent.task.domain.AgentTaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

public class AgentTaskApplicationService {

    private final AgentTaskRepository taskRepository;
    private final AgentStepRepository stepRepository;

    public AgentTaskApplicationService(AgentTaskRepository taskRepository, AgentStepRepository stepRepository) {
        this.taskRepository = taskRepository;
        this.stepRepository = stepRepository;
    }

    public AgentTask create(CreateAgentTaskCommand command) {
        AgentTask task = taskRepository.save(new AgentTask(
                taskRepository.nextTaskId(),
                command.taskType(),
                AgentTaskStatus.PENDING,
                command.targetId(),
                command.totalCount(),
                0,
                0,
                Instant.now(),
                Instant.now()
        ));
        stepRepository.save(new AgentStep(
                stepRepository.nextStepId(),
                task.id(),
                "任务已创建",
                AgentTaskStatus.PENDING,
                "等待调度执行",
                Instant.now()
        ));
        return task;
    }

    public AgentTask get(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Agent 任务不存在: " + id));
    }

    public List<AgentTask> list() {
        return taskRepository.findAll();
    }

    public List<AgentStep> listSteps(Long taskId) {
        get(taskId);
        return stepRepository.findByTaskId(taskId);
    }

    public AgentTask retry(Long id) {
        AgentTask task = get(id).retry();
        AgentTask saved = taskRepository.save(task);
        stepRepository.save(new AgentStep(
                stepRepository.nextStepId(),
                saved.id(),
                "任务已重试",
                AgentTaskStatus.PENDING,
                "已重置任务进度，等待重新执行",
                Instant.now()
        ));
        return saved;
    }

    public AgentTask start(Long id) {
        AgentTask task = taskRepository.save(get(id).start());
        addStep(id, "任务开始执行", AgentTaskStatus.RUNNING, "后台工作流已启动");
        return task;
    }

    public AgentTask recordSuccess(Long id, String stepName, String message) {
        AgentTask task = taskRepository.save(get(id).recordSuccess());
        addStep(id, stepName, AgentTaskStatus.SUCCEEDED, message);
        return task;
    }

    public AgentTask recordFailure(Long id, String stepName, String message) {
        AgentTask task = taskRepository.save(get(id).recordFailure());
        addStep(id, stepName, AgentTaskStatus.FAILED, message);
        return task;
    }

    public AgentTask finish(Long id) {
        return taskRepository.save(get(id).finish());
    }

    private void addStep(Long taskId, String stepName, AgentTaskStatus status, String message) {
        stepRepository.save(new AgentStep(stepRepository.nextStepId(), taskId, stepName, status, message, Instant.now()));
    }
}
