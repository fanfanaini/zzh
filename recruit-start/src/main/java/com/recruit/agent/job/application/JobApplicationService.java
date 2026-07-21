package com.recruit.agent.job.application;

import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.domain.JobRepository;
import com.recruit.agent.job.domain.JobStatus;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

public class JobApplicationService {

    private final JobRepository repository;

    public JobApplicationService(JobRepository repository) {
        this.repository = repository;
    }

    public Job create(CreateJobCommand command) {
        Job job = new Job(
                repository.nextId(),
                command.name(),
                command.description(),
                command.requiredSkills(),
                command.preferredSkills(),
                command.educationRequirement(),
                command.minExperienceYears(),
                command.maxExperienceYears(),
                Instant.now()
        );
        return repository.save(job);
    }

    public Job get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("岗位不存在: " + id));
    }

    public List<Job> list() {
        return search(null, null);
    }

    public List<Job> search(String keyword, JobStatus status) {
        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase();
        return repository.findAll().stream()
                .filter(job -> job.status() != JobStatus.DELETED)
                .filter(job -> status == null || job.status() == status)
                .filter(job -> normalizedKeyword == null || normalizedKeyword.isBlank()
                        || job.name().toLowerCase().contains(normalizedKeyword)
                        || job.description().toLowerCase().contains(normalizedKeyword))
                .toList();
    }

    public Job update(Long id, CreateJobCommand command) {
        Job existing = get(id);
        return repository.save(existing.update(command.name(), command.description(), command.requiredSkills(),
                command.preferredSkills(), command.educationRequirement(), command.minExperienceYears(),
                command.maxExperienceYears()));
    }

    public Job close(Long id) {
        return repository.save(get(id).close());
    }

    public Job delete(Long id) {
        return repository.save(get(id).markDeleted());
    }
}
