package com.recruit.agent.jobapplication.infrastructure;

import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.job.domain.JobRepository;
import com.recruit.agent.jobapplication.application.JobApplicationPipelineService;
import com.recruit.agent.jobapplication.domain.JobApplicationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class JobApplicationModuleConfiguration {

    @Bean
    @Profile("!mysql")
    public JobApplicationRepository jobApplicationRepository() {
        return new InMemoryJobApplicationRepository();
    }

    @Bean
    public JobApplicationPipelineService jobApplicationPipelineService(
            JobApplicationRepository repository,
            JobRepository jobRepository,
            CandidateRepository candidateRepository
    ) {
        return new JobApplicationPipelineService(repository, jobRepository, candidateRepository);
    }
}
