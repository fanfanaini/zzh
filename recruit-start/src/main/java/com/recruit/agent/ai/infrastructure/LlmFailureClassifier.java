package com.recruit.agent.ai.infrastructure;

import com.recruit.agent.ai.domain.LlmFailureCategory;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Locale;

public final class LlmFailureClassifier {
    public LlmFailureCategory classify(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof HttpTimeoutException || current instanceof SocketTimeoutException) {
                return LlmFailureCategory.TIMEOUT;
            }
            String message = current.getMessage();
            if (message == null) continue;
            String normalized = message.toLowerCase(Locale.ROOT);
            if (contains(normalized, "context_length", "context length", "context window",
                    "maximum context", "too many tokens")) return LlmFailureCategory.CONTEXT_OVERFLOW;
            if (contains(normalized, "http 429", "rate_limit", "rate limit", "too many requests")) {
                return LlmFailureCategory.RATE_LIMIT;
            }
            if (contains(normalized, "http 503", "service unavailable", "provider overloaded",
                    "temporarily unavailable", "overloaded")) return LlmFailureCategory.PROVIDER_OVERLOADED;
            if (contains(normalized, "timed out", "timeout")) return LlmFailureCategory.TIMEOUT;
            if (contains(normalized, "http 401", "http 403", "invalid api key", "unauthorized", "forbidden")) {
                return LlmFailureCategory.AUTHENTICATION;
            }
            if (contains(normalized, "http 400", "invalid_request", "invalid request", "bad request")) {
                return LlmFailureCategory.INVALID_REQUEST;
            }
        }
        return LlmFailureCategory.UNKNOWN;
    }

    private boolean contains(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) return true;
        }
        return false;
    }
}
