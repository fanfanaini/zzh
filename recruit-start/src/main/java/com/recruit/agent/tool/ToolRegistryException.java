package com.recruit.agent.tool;

public class ToolRegistryException extends RuntimeException {
    private final ToolErrorCode code;

    public ToolRegistryException(ToolErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ToolErrorCode code() {
        return code;
    }
}
