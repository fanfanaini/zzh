package com.recruit.agent.evaluation.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.evaluation.application.AgentCoreBaselineService;
import com.recruit.agent.evaluation.application.AgentEvaluationService;
import com.recruit.agent.evaluation.application.EvaluationJsonlParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvaluationModuleConfiguration {
    @Bean
    public AgentCoreBaselineService agentCoreBaselineService(ObjectMapper objectMapper) {
        return new AgentCoreBaselineService(new EvaluationJsonlParser(objectMapper), new AgentEvaluationService());
    }
}
