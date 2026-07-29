package com.recruit.agent.toolcalling;

import java.util.Map;

public record ToolCallDecision(
        boolean callTool,
        String toolName,
        Map<String, Object> arguments,
        double confidence,
        String reason
) {
    public ToolCallDecision {
        toolName = toolName == null ? "" : toolName.trim();
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        reason = reason == null ? "" : reason.trim();
    }

    public static ToolCallDecision none(String reason) {
        return new ToolCallDecision(false, "", Map.of(), 0.0, reason);
    }
}
