package com.recruit.agent.conversation.infrastructure;

import java.util.Locale;

public class ContextOverflowDetector {
    public boolean isContextOverflow(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) continue;
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("context_length")
                    || normalized.contains("context length")
                    || normalized.contains("context window")
                    || normalized.contains("maximum context")
                    || normalized.contains("too many tokens")
                    || normalized.contains("token limit")) return true;
        }
        return false;
    }
}
