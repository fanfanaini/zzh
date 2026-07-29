package com.recruit.agent.jobapplication.domain;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository {

    JobApplication save(JobApplication application);

    Optional<JobApplication> findById(Long id);

    Optional<JobApplication> findByJobIdAndCandidateId(Long jobId, Long candidateId);

    List<JobApplication> findAll();

    JobApplicationHistory saveHistory(JobApplicationHistory history);

    List<JobApplicationHistory> findHistoryByApplicationId(Long applicationId);

    long nextApplicationId();

    long nextHistoryId();
}
