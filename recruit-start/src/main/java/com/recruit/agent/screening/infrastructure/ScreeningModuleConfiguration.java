package com.recruit.agent.screening.infrastructure;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.job.domain.JobRepository;
import com.recruit.agent.screening.application.EvaluationApplicationService;
import com.recruit.agent.screening.application.ScreeningApplicationService;
import com.recruit.agent.screening.application.BatchScreeningApplicationService;
import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.screening.domain.EvaluationRepository;
import com.recruit.agent.screening.domain.ResumeScreeningService;
import com.recruit.agent.screening.domain.ScreeningReviewService;
import com.recruit.agent.vector.application.VectorApplicationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.annotation.Profile;
import com.recruit.agent.screening.application.BatchScreeningPublisher;
import com.recruit.agent.screening.application.BatchScreeningStateStore;

@Configuration
public class ScreeningModuleConfiguration {

    @Bean
    public ResumeScreeningService resumeScreeningService() {
        return new ResumeScreeningService();
    }

    @Bean
    public ScreeningApplicationService screeningApplicationService(
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ResumeScreeningService resumeScreeningService,
            EvaluationRepository evaluationRepository,
            ObjectProvider<VectorApplicationService> vectorApplicationService,
            ObjectProvider<ScreeningReviewService> screeningReviewService
    ) {
        return new ScreeningApplicationService(
                jobRepository,
                candidateRepository,
                resumeScreeningService,
                evaluationRepository,
                java.util.Optional.ofNullable(vectorApplicationService.getIfAvailable()),
                java.util.Optional.ofNullable(screeningReviewService.getIfAvailable())
        );
    }

    @Bean
    public EvaluationApplicationService evaluationApplicationService(EvaluationRepository evaluationRepository) {
        return new EvaluationApplicationService(evaluationRepository);
    }

    @Bean
    public ScreeningReviewService screeningReviewService(LlmService llmService) {
        return new AiScreeningReviewService(llmService);
    }

    @Bean
    public ThreadPoolTaskExecutor screeningTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("batch-screening-");
        executor.initialize();
        return executor;
    }

    @Bean
    @Profile("!distributed")
    public BatchScreeningApplicationService batchScreeningApplicationService(
            AgentTaskApplicationService taskService,
            ScreeningApplicationService screeningService,
            @Qualifier("screeningTaskExecutor") ThreadPoolTaskExecutor screeningTaskExecutor
    ) {
        return new BatchScreeningApplicationService(taskService, screeningService::screen, screeningTaskExecutor);
    }

    @Bean
    @Profile("distributed")
    public BatchScreeningApplicationService distributedBatchScreeningApplicationService(
            AgentTaskApplicationService taskService,
            ScreeningApplicationService screeningService,
            BatchScreeningStateStore stateStore,
            BatchScreeningPublisher publisher
    ) {
        return new BatchScreeningApplicationService(taskService, screeningService::screen, stateStore, publisher);
    }
}
