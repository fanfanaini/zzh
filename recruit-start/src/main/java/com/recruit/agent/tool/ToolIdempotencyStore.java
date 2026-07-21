package com.recruit.agent.tool;

import java.util.Optional;

public interface ToolIdempotencyStore {
    Optional<ToolResult> find(String key);

    ToolResult saveIfAbsent(String key, ToolResult result);
}
