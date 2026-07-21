package com.recruit.agent.tool;

import java.util.Map;

public record ToolError(ToolErrorCode code, String message, Map<String, Object> details) {
    public ToolError {
        if (code == null) {
            throw new IllegalArgumentException("Tool error code must not be null");
        }
        message = message == null ? "" : message;
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
