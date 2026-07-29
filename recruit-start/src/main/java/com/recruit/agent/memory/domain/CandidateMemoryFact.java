package com.recruit.agent.memory.domain;

import java.time.Instant;
import java.util.Locale;

public record CandidateMemoryFact(
        Long id,
        Long candidateId,
        CandidateMemoryType type,
        String factKey,
        String factValue,
        double confidence,
        CandidateMemorySourceType sourceType,
        Long sourceId,
        String evidence,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) implements java.io.Serializable {

    public CandidateMemoryFact {
        if (id == null) {
            throw new IllegalArgumentException("memory id must not be null");
        }
        if (candidateId == null) {
            throw new IllegalArgumentException("candidate id must not be null");
        }
        if (type == null) {
            type = CandidateMemoryType.FACT;
        }
        if (factKey == null || factKey.isBlank()) {
            throw new IllegalArgumentException("memory fact key must not be blank");
        }
        if (factValue == null || factValue.isBlank()) {
            throw new IllegalArgumentException("memory fact value must not be blank");
        }
        factKey = normalizeKey(factKey);
        factValue = factValue.trim();
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        if (sourceType == null) {
            sourceType = CandidateMemorySourceType.SYSTEM;
        }
        evidence = evidence == null ? "" : evidence.trim();
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    public CandidateMemoryFact(Long id, Long candidateId, CandidateMemoryType type, String factKey,
                               String factValue, double confidence, CandidateMemorySourceType sourceType,
                               Long sourceId, String evidence) {
        this(id, candidateId, type, factKey, factValue, confidence, sourceType, sourceId, evidence,
                true, Instant.now(), Instant.now());
    }

    public CandidateMemoryFact withId(Long newId) {
        return new CandidateMemoryFact(newId, candidateId, type, factKey, factValue, confidence, sourceType,
                sourceId, evidence, active, createdAt, updatedAt);
    }

    public CandidateMemoryFact mergeFrom(CandidateMemoryFact incoming) {
        return new CandidateMemoryFact(id, candidateId, incoming.type(), factKey, incoming.factValue(),
                Math.max(confidence, incoming.confidence()), incoming.sourceType(), incoming.sourceId(),
                incoming.evidence(), true, createdAt, Instant.now());
    }

    public CandidateMemoryFact deactivate() {
        return new CandidateMemoryFact(id, candidateId, type, factKey, factValue, confidence, sourceType,
                sourceId, evidence, false, createdAt, Instant.now());
    }

    private static String normalizeKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
