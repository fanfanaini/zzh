package com.recruit.agent.jobapplication.infrastructure;

import com.recruit.agent.jobapplication.domain.JobApplication;
import com.recruit.agent.jobapplication.domain.JobApplicationHistory;
import com.recruit.agent.jobapplication.domain.JobApplicationRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryJobApplicationRepository implements JobApplicationRepository {

    private final AtomicLong applicationIdGenerator = new AtomicLong(3000);
    private final AtomicLong historyIdGenerator = new AtomicLong(4000);
    private final ConcurrentHashMap<Long, JobApplication> applications = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, JobApplicationHistory> histories = new ConcurrentHashMap<>();

    @Override
    public JobApplication save(JobApplication application) {
        applications.put(application.id(), application);
        return application;
    }

    @Override
    public Optional<JobApplication> findById(Long id) {
        return Optional.ofNullable(applications.get(id));
    }

    @Override
    public Optional<JobApplication> findByJobIdAndCandidateId(Long jobId, Long candidateId) {
        return applications.values().stream()
                .filter(application -> application.jobId().equals(jobId)
                        && application.candidateId().equals(candidateId))
                .findFirst();
    }

    @Override
    public List<JobApplication> findAll() {
        ArrayList<JobApplication> values = new ArrayList<>(applications.values());
        values.sort(Comparator.comparing(JobApplication::updatedAt).reversed());
        return List.copyOf(values);
    }

    @Override
    public JobApplicationHistory saveHistory(JobApplicationHistory history) {
        histories.put(history.id(), history);
        return history;
    }

    @Override
    public List<JobApplicationHistory> findHistoryByApplicationId(Long applicationId) {
        return histories.values().stream()
                .filter(history -> history.applicationId().equals(applicationId))
                .sorted(Comparator.comparing(JobApplicationHistory::createdAt))
                .toList();
    }

    @Override
    public long nextApplicationId() {
        return applicationIdGenerator.incrementAndGet();
    }

    @Override
    public long nextHistoryId() {
        return historyIdGenerator.incrementAndGet();
    }
}
