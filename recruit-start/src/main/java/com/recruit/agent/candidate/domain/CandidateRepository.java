package com.recruit.agent.candidate.domain;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository {

    Candidate save(Candidate candidate);

    Optional<Candidate> findById(Long id);

    List<Candidate> findAll();

    long nextCandidateId();
}
