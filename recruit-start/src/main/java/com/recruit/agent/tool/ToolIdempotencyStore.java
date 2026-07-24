package com.recruit.agent.tool;

import java.time.Duration;
import java.util.Optional;

public interface ToolIdempotencyStore {
    Optional<ToolResult> find(String key);

    ToolResult saveIfAbsent(String key, ToolResult result);

    Claim claim(String key, Duration leaseDuration);

    ToolResult complete(Claim claim, ToolResult result);

    void release(Claim claim);

    record Claim(String key, String ownerToken, ToolResult completedResult) {
        public static Claim acquired(String key, String ownerToken) {
            return new Claim(key, ownerToken, null);
        }

        public static Claim completed(String key, ToolResult result) {
            return new Claim(key, null, result);
        }

        public boolean acquired() {
            return ownerToken != null;
        }
    }
}
