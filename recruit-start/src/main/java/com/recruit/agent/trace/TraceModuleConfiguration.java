package com.recruit.agent.trace;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TraceModuleConfiguration {

    @Bean
    public UnifiedTraceRecorder unifiedTraceRecorder(AgentTraceSink sink) {
        return new UnifiedTraceRecorder(sink);
    }
}
