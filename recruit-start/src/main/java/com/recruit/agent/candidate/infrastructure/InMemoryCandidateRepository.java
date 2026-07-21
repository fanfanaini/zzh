package com.recruit.agent.candidate.infrastructure;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.CandidateRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCandidateRepository implements CandidateRepository {

    private final AtomicLong idGenerator = new AtomicLong(2000);
    private final ConcurrentHashMap<Long, Candidate> storage = new ConcurrentHashMap<>();

    @Override
    public Candidate save(Candidate candidate) {
        storage.put(candidate.id(), candidate);
        return candidate;
    }

    @Override
    public Optional<Candidate> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Candidate> findAll() {
        List<Candidate> candidates = new ArrayList<>(storage.values());
        candidates.sort(Comparator.comparing(Candidate::createdAt).reversed());
        return List.copyOf(candidates);
    }

    @Override
    public long nextCandidateId() {
        return idGenerator.incrementAndGet();
    }
}
