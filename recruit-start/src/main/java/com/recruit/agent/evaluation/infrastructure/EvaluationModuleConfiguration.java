package com.recruit.agent.evaluation.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.evaluation.application.AgentCoreBaselineService;
import com.recruit.agent.evaluation.application.AgentEvaluationService;
import com.recruit.agent.evaluation.application.EvaluationJsonlParser;
import com.recruit.agent.evaluation.importing.AnonymizedConversationJsonlImporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvaluationModuleConfiguration {
    @Bean
    public AgentCoreBaselineService agentCoreBaselineService(ObjectMapper objectMapper) {
        return new AgentCoreBaselineService(new EvaluationJsonlParser(objectMapper), new AgentEvaluationService());
    }

    @Bean
    public AnonymizedConversationJsonlImporter anonymizedConversationJsonlImporter(ObjectMapper objectMapper) {
        return new AnonymizedConversationJsonlImporter(objectMapper);
    }
}
