package com.recruit.agent.retrieval.domain;

public record RetrievalChannelDiagnostic(
        boolean succeeded,
        int hitCount,
        String errorType,
        String errorMessage
) {
    public static RetrievalChannelDiagnostic success(int hitCount) {
        return new RetrievalChannelDiagnostic(true, Math.max(0, hitCount), null, null);
    }

    public static RetrievalChannelDiagnostic failure(RuntimeException exception) {
        String type = exception == null ? "UNKNOWN" : exception.getClass().getSimpleName();
        String message = exception == null || exception.getMessage() == null
                ? "Retrieval channel failed"
                : exception.getMessage();
        return new RetrievalChannelDiagnostic(false, 0, type, message);
    }
}
