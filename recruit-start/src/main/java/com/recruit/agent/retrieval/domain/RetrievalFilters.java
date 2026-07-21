package com.recruit.agent.retrieval.domain;

import java.util.LinkedHashSet;
import java.util.Set;

public record RetrievalFilters(Set<Long> documentIds, Set<String> sourceNames) {

    public RetrievalFilters {
        documentIds = immutableDocumentIds(documentIds);
        sourceNames = immutableSourceNames(sourceNames);
    }

    public static RetrievalFilters none() {
        return new RetrievalFilters(Set.of(), Set.of());
    }

    public boolean hasDocumentIds() {
        return !documentIds.isEmpty();
    }

    public boolean hasSourceNames() {
        return !sourceNames.isEmpty();
    }

    private static Set<Long> immutableDocumentIds(Set<Long> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (Long value : values) {
            if (value == null) {
                throw new IllegalArgumentException("documentIds must not contain null");
            }
            result.add(value);
        }
        return Set.copyOf(result);
    }

    private static Set<String> immutableSourceNames(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("sourceNames must not contain blank values");
            }
            result.add(value.trim());
        }
        return Set.copyOf(result);
    }
}
