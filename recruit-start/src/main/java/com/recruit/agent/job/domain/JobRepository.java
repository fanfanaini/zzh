package com.recruit.agent.job.domain;

import java.util.List;
import java.util.Optional;

public interface JobRepository {

    Job save(Job job);

    Optional<Job> findById(Long id);

    List<Job> findAll();

    long nextId();
}
