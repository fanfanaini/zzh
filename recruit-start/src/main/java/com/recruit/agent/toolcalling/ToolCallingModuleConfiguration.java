package com.recruit.agent.toolcalling;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.tool.ToolRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolCallingModuleConfiguration {
    @Bean
    public FunctionCallingToolPlanner functionCallingToolPlanner(LlmService llmService, ToolRegistry registry) {
        return new LlmFunctionCallingToolPlanner(llmService, registry);
    }
}
