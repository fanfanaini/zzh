package com.recruit.agent.tool;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryToolIdempotencyStore implements ToolIdempotencyStore {
    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<ToolResult> find(String key) {
        Entry entry = entries.get(key);
        return entry == null || !entry.result().isDone()
                ? Optional.empty()
                : entry.result().join();
    }

    @Override
    public ToolResult saveIfAbsent(String key, ToolResult result) {
        Claim claim = claim(key, Duration.ZERO);
        return claim.acquired() ? complete(claim, result) : claim.completedResult();
    }

    @Override
    public Claim claim(String key, Duration leaseDuration) {
        while (true) {
            Entry candidate = new Entry(UUID.randomUUID().toString(), new CompletableFuture<>());
            Entry existing = entries.putIfAbsent(key, candidate);
            if (existing == null) {
                return Claim.acquired(key, candidate.ownerToken());
            }
            Optional<ToolResult> result = existing.result().join();
            if (result.isPresent()) {
                return Claim.completed(key, result.orElseThrow());
            }
        }
    }

    @Override
    public ToolResult complete(Claim claim, ToolResult result) {
        Entry entry = ownedEntry(claim);
        entry.result().complete(Optional.of(result));
        return result;
    }

    @Override
    public void release(Claim claim) {
        Entry entry = entries.get(claim.key());
        if (entry != null && entry.ownerToken().equals(claim.ownerToken())
                && entries.remove(claim.key(), entry)) {
            entry.result().complete(Optional.empty());
        }
    }

    private Entry ownedEntry(Claim claim) {
        Entry entry = entries.get(claim.key());
        if (entry == null || !entry.ownerToken().equals(claim.ownerToken())) {
            throw new IllegalStateException("Idempotency claim is no longer owned");
        }
        return entry;
    }

    private record Entry(String ownerToken, CompletableFuture<Optional<ToolResult>> result) {
    }
}
