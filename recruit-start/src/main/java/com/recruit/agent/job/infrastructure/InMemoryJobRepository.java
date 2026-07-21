package com.recruit.agent.job.infrastructure;

import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.domain.JobRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryJobRepository implements JobRepository {

    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final ConcurrentHashMap<Long, Job> storage = new ConcurrentHashMap<>();

    @Override
    public Job save(Job job) {
        storage.put(job.id(), job);
        return job;
    }

    @Override
    public Optional<Job> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Job> findAll() {
        List<Job> jobs = new ArrayList<>(storage.values());
        jobs.sort(Comparator.comparing(Job::createdAt).reversed());
        return List.copyOf(jobs);
    }

    @Override
    public long nextId() {
        return idGenerator.incrementAndGet();
    }
}
