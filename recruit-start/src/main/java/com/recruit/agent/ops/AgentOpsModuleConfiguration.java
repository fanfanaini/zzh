package com.recruit.agent.ops;

import com.recruit.agent.ai.domain.ModelCallLogRepository;
import com.recruit.agent.trace.AgentTraceSink;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AgentOpsProperties.class)
public class AgentOpsModuleConfiguration {
    @Bean
    public AgentOpsService agentOpsService(ModelCallLogRepository modelLogs,
                                           AgentTraceSink traceSink,
                                           AgentOpsProperties properties) {
        return new AgentOpsService(modelLogs, traceSink, properties);
    }

    @Bean
    public AgentOpsMetrics agentOpsMetrics(AgentOpsService service) {
        return new AgentOpsMetrics(service);
    }

    @Bean
    @Profile("!mysql")
    public AlertEventRepository alertEventRepository() {
        return new InMemoryAlertEventRepository();
    }

    @Bean
    public AlertNotifier alertNotifier(AgentOpsProperties properties, RestClient.Builder builder) {
        if (!properties.isAlertNotificationEnabled()
                || properties.getAlertWebhookUrl() == null
                || properties.getAlertWebhookUrl().isBlank()) {
            return new NoopAlertNotifier();
        }
        return new WebhookAlertNotifier(builder, properties.getAlertWebhookUrl());
    }

    @Bean
    public AlertLifecycleService alertLifecycleService(AgentOpsService opsService,
                                                       AlertEventRepository repository,
                                                       AlertNotifier notifier,
                                                       AgentOpsProperties properties) {
        return new AlertLifecycleService(opsService, repository, notifier, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "recruit.ops", name = "alert-scheduler-enabled", havingValue = "true",
            matchIfMissing = true)
    public AlertEvaluationScheduler alertEvaluationScheduler(AlertLifecycleService service) {
        return new AlertEvaluationScheduler(service);
    }
}
