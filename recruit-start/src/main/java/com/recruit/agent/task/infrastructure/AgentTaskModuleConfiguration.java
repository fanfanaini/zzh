package com.recruit.agent.task.infrastructure;

import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.task.domain.AgentStepRepository;
import com.recruit.agent.task.domain.AgentTaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentTaskModuleConfiguration {

    @Bean
    public AgentTaskApplicationService agentTaskApplicationService(
            AgentTaskRepository taskRepository,
            AgentStepRepository stepRepository
    ) {
        return new AgentTaskApplicationService(taskRepository, stepRepository);
    }
}
