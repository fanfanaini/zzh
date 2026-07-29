package com.recruit.agent.mcp.api;

import com.recruit.agent.tool.ToolError;
import com.recruit.agent.tool.ToolResult;

public record McpToolCallResponse(
        String traceId,
        boolean success,
        Object data,
        ToolError error,
        int attempts,
        boolean idempotencyReplay
) {
    public static McpToolCallResponse from(ToolResult result) {
        return new McpToolCallResponse(result.traceId(), result.success(), result.data(), result.error(),
                result.attempts(), result.idempotencyReplay());
    }
}
