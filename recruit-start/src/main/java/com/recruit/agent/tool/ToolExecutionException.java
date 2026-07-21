package com.recruit.agent.tool;

public class ToolExecutionException extends Exception {
    private final ToolErrorCode code;
    private final boolean retryable;

    public ToolExecutionException(ToolErrorCode code, String message, boolean retryable) {
        super(message);
        this.code = code == null ? ToolErrorCode.EXECUTION_FAILED : code;
        this.retryable = retryable;
    }

    public ToolErrorCode code() {
        return code;
    }

    public boolean retryable() {
        return retryable;
    }
}
