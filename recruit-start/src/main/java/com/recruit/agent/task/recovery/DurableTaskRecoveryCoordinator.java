package com.recruit.agent.task.recovery;

import com.recruit.agent.candidate.application.ResumeParsingTaskService;
import com.recruit.agent.screening.application.BatchScreeningApplicationService;
import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskStatus;
import com.recruit.agent.task.domain.AgentTaskType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("mysql")
public class DurableTaskRecoveryCoordinator {
    private final AgentTaskApplicationService tasks;
    private final ResumeParsingTaskService resumeParsing;
    private final ObjectProvider<BatchScreeningApplicationService> batchScreening;
    private final Environment environment;

    public DurableTaskRecoveryCoordinator(AgentTaskApplicationService tasks,
                                          ResumeParsingTaskService resumeParsing,
                                          ObjectProvider<BatchScreeningApplicationService> batchScreening,
                                          Environment environment) {
        this.tasks = tasks;
        this.resumeParsing = resumeParsing;
        this.batchScreening = batchScreening;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverIncompleteTasks() {
        tasks.list().stream()
                .filter(this::isRecoverable)
                .forEach(this::recover);
    }

    private boolean isRecoverable(AgentTask task) {
        return task.status() == AgentTaskStatus.PENDING || task.status() == AgentTaskStatus.RUNNING;
    }

    private void recover(AgentTask task) {
        if (task.taskType() == AgentTaskType.RESUME_PARSE) {
            resumeParsing.schedule(task);
            return;
        }
        if (task.taskType() == AgentTaskType.BATCH_SCREENING) {
            if (!environment.acceptsProfiles(Profiles.of("distributed"))) return;
            BatchScreeningApplicationService service = batchScreening.getIfAvailable();
            if (service != null) {
                try {
                    service.retry(task.id());
                } catch (RuntimeException exception) {
                    String message = exception.getMessage() == null
                            ? exception.getClass().getSimpleName() : exception.getMessage();
                    tasks.recordFailure(task.id(), "TASK_RECOVERY", message);
                    tasks.finish(task.id());
                }
            }
        }
    }
}
