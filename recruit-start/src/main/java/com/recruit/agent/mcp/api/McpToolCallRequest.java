package com.recruit.agent.mcp.api;

import java.util.Map;

public record McpToolCallRequest(
        Map<String, Object> arguments,
        String idempotencyKey,
        Long conversationId
) {
}
