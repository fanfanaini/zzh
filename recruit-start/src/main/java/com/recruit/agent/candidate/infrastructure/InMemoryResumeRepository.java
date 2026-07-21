package com.recruit.agent.candidate.infrastructure;

import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryResumeRepository implements ResumeRepository {

    private final AtomicLong idGenerator = new AtomicLong(3000);
    private final ConcurrentHashMap<Long, ResumeDocument> storage = new ConcurrentHashMap<>();

    @Override
    public ResumeDocument save(ResumeDocument resume) {
        storage.put(resume.id(), resume);
        return resume;
    }

    @Override
    public Optional<ResumeDocument> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<ResumeDocument> findByCandidateId(Long candidateId) {
        List<ResumeDocument> resumes = new ArrayList<>();
        for (ResumeDocument resume : storage.values()) {
            if (resume.candidateId().equals(candidateId)) {
                resumes.add(resume);
            }
        }
        resumes.sort(Comparator.comparing(ResumeDocument::uploadedAt).reversed());
        return List.copyOf(resumes);
    }

    @Override
    public void deleteById(Long id) {
        storage.remove(id);
    }

    @Override
    public long nextResumeId() {
        return idGenerator.incrementAndGet();
    }
}
