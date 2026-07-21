package com.recruit.agent.task.application;

import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskStatus;
import com.recruit.agent.task.domain.AgentTaskType;
import com.recruit.agent.task.infrastructure.InMemoryAgentStepRepository;
import com.recruit.agent.task.infrastructure.InMemoryAgentTaskRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTaskApplicationServiceTest {

    @Test
    void createsTaskAndRetryAddsStep() {
        AgentTaskApplicationService service = new AgentTaskApplicationService(
                new InMemoryAgentTaskRepository(),
                new InMemoryAgentStepRepository()
        );

        AgentTask task = service.create(new CreateAgentTaskCommand(AgentTaskType.BATCH_SCREENING, 1001L, 5));
        AgentTask retried = service.retry(task.id());

        assertThat(task.status()).isEqualTo(AgentTaskStatus.PENDING);
        assertThat(retried.successCount()).isZero();
        assertThat(service.listSteps(task.id())).hasSize(2);
    }
}
