package com.recruit.agent.toolcalling;

import java.util.Map;

public record LlmToolCallDecisionResult(
        boolean callTool,
        String toolName,
        Map<String, Object> arguments,
        double confidence,
        String reason
) {
}
