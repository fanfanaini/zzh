package com.recruit.agent.tool;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryToolIdempotencyStore implements ToolIdempotencyStore {
    private final ConcurrentMap<String, ToolResult> results = new ConcurrentHashMap<>();

    @Override
    public Optional<ToolResult> find(String key) {
        return Optional.ofNullable(results.get(key));
    }

    @Override
    public ToolResult saveIfAbsent(String key, ToolResult result) {
        ToolResult existing = results.putIfAbsent(key, result);
        return existing == null ? result : existing;
    }
}
