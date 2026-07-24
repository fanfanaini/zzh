package com.recruit.agent.ai.infrastructure;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.domain.ModelCallLogRepository;
import com.recruit.agent.ai.domain.RawLlmClient;
import com.recruit.agent.ai.domain.PromptVersionRepository;
import com.recruit.agent.ai.application.PromptApplicationService;
import com.recruit.agent.ai.application.ModelCallMonitoringService;
import com.recruit.agent.trace.UnifiedTraceRecorder;
import com.recruit.agent.privacy.SensitiveDataRedactor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AiProperties.class, LlmRecoveryProperties.class})
public class AiModuleConfiguration {

    @Bean
    public RawLlmClient rawLlmClient(AiProperties properties) {
        if (properties.isEnabled()) {
            return new OpenAiCompatibleRawLlmClient(properties);
        }
        return new DisabledRawLlmClient();
    }

    @Bean
    public LlmService llmService(RawLlmClient rawLlmClient,
                                 ModelCallLogRepository logRepository,
                                 LlmRecoveryProperties recovery,
                                 UnifiedTraceRecorder traceRecorder,
                                 SensitiveDataRedactor redactor) {
        return new DefaultLlmService(rawLlmClient, logRepository, recovery.getMaxAttempts(),
                recovery.getInitialBackoff().toMillis(), recovery.getMaximumBackoff().toMillis(),
                millis -> {
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("LLM retry backoff was interrupted", exception);
                    }
                }, () -> java.util.concurrent.ThreadLocalRandom.current().nextDouble(), traceRecorder, redactor);
    }

    @Bean
    public PromptApplicationService promptApplicationService(PromptVersionRepository repository) {
        return new PromptApplicationService(repository);
    }

    @Bean
    public ModelCallMonitoringService modelCallMonitoringService(ModelCallLogRepository repository) {
        return new ModelCallMonitoringService(repository);
    }
}
