package com.recruit.agent.candidate.domain;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository {

    ResumeDocument save(ResumeDocument resume);

    Optional<ResumeDocument> findById(Long id);

    List<ResumeDocument> findByCandidateId(Long candidateId);

    void deleteById(Long id);

    long nextResumeId();
}
