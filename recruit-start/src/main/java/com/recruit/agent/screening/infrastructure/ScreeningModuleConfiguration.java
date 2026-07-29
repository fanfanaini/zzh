package com.recruit.agent.screening.infrastructure;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.job.domain.JobRepository;
import com.recruit.agent.jobapplication.application.JobApplicationPipelineService;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import com.recruit.agent.screening.application.BatchScreeningPublisher;
import com.recruit.agent.screening.application.BatchScreeningStateStore;
import com.recruit.agent.task.checkpoint.domain.TaskCheckpointRepository;

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
            ObjectProvider<ScreeningReviewService> screeningReviewService,
            ObjectProvider<JobApplicationPipelineService> jobApplicationPipelineService
    ) {
        return new ScreeningApplicationService(
                jobRepository,
                candidateRepository,
                resumeScreeningService,
                evaluationRepository,
                java.util.Optional.ofNullable(vectorApplicationService.getIfAvailable()),
                java.util.Optional.ofNullable(screeningReviewService.getIfAvailable()),
                java.util.Optional.ofNullable(jobApplicationPipelineService.getIfAvailable())
        );
    }

    @Bean
    public EvaluationApplicationService evaluationApplicationService(
            EvaluationRepository evaluationRepository,
            ObjectProvider<JobApplicationPipelineService> jobApplicationPipelineService
    ) {
        return new EvaluationApplicationService(evaluationRepository,
                java.util.Optional.ofNullable(jobApplicationPipelineService.getIfAvailable()));
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
            TaskCheckpointRepository checkpointRepository,
            ObjectProvider<PlatformTransactionManager> transactionManager,
            @Qualifier("screeningTaskExecutor") ThreadPoolTaskExecutor screeningTaskExecutor
    ) {
        return new BatchScreeningApplicationService(taskService, screeningService::screen, screeningTaskExecutor,
                checkpointRepository, transactionOperations(transactionManager));
    }

    @Bean
    @Profile("distributed")
    public BatchScreeningApplicationService distributedBatchScreeningApplicationService(
            AgentTaskApplicationService taskService,
            ScreeningApplicationService screeningService,
            BatchScreeningStateStore stateStore,
            BatchScreeningPublisher publisher,
            TaskCheckpointRepository checkpointRepository,
            ObjectProvider<PlatformTransactionManager> transactionManager
    ) {
        return new BatchScreeningApplicationService(taskService, screeningService::screen, stateStore, publisher,
                checkpointRepository, transactionOperations(transactionManager));
    }

    private TransactionOperations transactionOperations(
            ObjectProvider<PlatformTransactionManager> transactionManager
    ) {
        PlatformTransactionManager manager = transactionManager.getIfAvailable();
        return manager == null ? null : new TransactionTemplate(manager);
    }
}
