package com.recruit.agent.trace;

import com.recruit.agent.tool.ToolError;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ToolTrace(
        String traceId,
        String invocationId,
        String toolName,
        String principalId,
        Long conversationId,
        Map<String, Object> arguments,
        ToolTraceStatus status,
        int attempts,
        ToolError error,
        Instant startedAt,
        Instant finishedAt
) {
    public ToolTrace(
            String traceId,
            String invocationId,
            String toolName,
            String principalId,
            Map<String, Object> arguments,
            ToolTraceStatus status,
            int attempts,
            ToolError error,
            Instant startedAt,
            Instant finishedAt
    ) {
        this(traceId, invocationId, toolName, principalId, null, arguments, status, attempts, error,
                startedAt, finishedAt);
    }

    public ToolTrace {
        arguments = arguments == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
    }
}
