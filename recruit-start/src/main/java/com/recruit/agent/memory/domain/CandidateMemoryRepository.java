package com.recruit.agent.memory.domain;

import java.util.List;
import java.util.Optional;

public interface CandidateMemoryRepository {
    CandidateMemoryFact save(CandidateMemoryFact fact);

    Optional<CandidateMemoryFact> findById(Long id);

    Optional<CandidateMemoryFact> findActiveByCandidateIdAndKey(Long candidateId, String factKey);

    List<CandidateMemoryFact> findActiveByCandidateId(Long candidateId);

    List<CandidateMemoryFact> findAllByCandidateId(Long candidateId);

    long nextId();
}
