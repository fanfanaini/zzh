package com.recruit.agent.conversation.infrastructure;

public class ContextWindowExceededException extends RuntimeException {
    public ContextWindowExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
