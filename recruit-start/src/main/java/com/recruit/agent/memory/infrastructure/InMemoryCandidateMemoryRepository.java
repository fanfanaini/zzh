package com.recruit.agent.memory.infrastructure;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.memory.domain.CandidateMemoryFact;
import com.recruit.agent.memory.domain.CandidateMemoryRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCandidateMemoryRepository implements CandidateMemoryRepository {
    private final Map<Long, CandidateMemoryFact> facts = new ConcurrentHashMap<>();
    private final TimestampIdGenerator ids = new TimestampIdGenerator();

    @Override
    public CandidateMemoryFact save(CandidateMemoryFact fact) {
        facts.put(fact.id(), fact);
        return fact;
    }

    @Override
    public Optional<CandidateMemoryFact> findById(Long id) {
        return Optional.ofNullable(facts.get(id));
    }

    @Override
    public Optional<CandidateMemoryFact> findActiveByCandidateIdAndKey(Long candidateId, String factKey) {
        return facts.values().stream()
                .filter(CandidateMemoryFact::active)
                .filter(fact -> fact.candidateId().equals(candidateId))
                .filter(fact -> fact.factKey().equals(normalize(factKey)))
                .findFirst();
    }

    @Override
    public List<CandidateMemoryFact> findActiveByCandidateId(Long candidateId) {
        return findAllByCandidateId(candidateId).stream()
                .filter(CandidateMemoryFact::active)
                .toList();
    }

    @Override
    public List<CandidateMemoryFact> findAllByCandidateId(Long candidateId) {
        return facts.values().stream()
                .filter(fact -> fact.candidateId().equals(candidateId))
                .sorted(Comparator.comparing(CandidateMemoryFact::updatedAt).reversed())
                .toList();
    }

    @Override
    public long nextId() {
        return ids.nextId();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
    }
}
