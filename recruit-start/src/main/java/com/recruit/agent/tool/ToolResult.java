package com.recruit.agent.tool;

public record ToolResult(
        String traceId,
        boolean success,
        Object data,
        ToolError error,
        int attempts,
        boolean idempotencyReplay
) {
    public static ToolResult success(String traceId, Object data, int attempts) {
        return new ToolResult(traceId, true, data, null, attempts, false);
    }

    public static ToolResult failure(String traceId, ToolError error, int attempts) {
        return new ToolResult(traceId, false, null, error, attempts, false);
    }

    public static ToolResult replay(String traceId, ToolResult cached) {
        return new ToolResult(traceId, cached.success(), cached.data(), cached.error(), 0, true);
    }
}
