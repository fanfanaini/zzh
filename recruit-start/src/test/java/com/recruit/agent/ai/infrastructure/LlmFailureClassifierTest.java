package com.recruit.agent.ai.infrastructure;

import com.recruit.agent.ai.domain.LlmFailureCategory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.http.HttpTimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LlmFailureClassifierTest {

    private final LlmFailureClassifier classifier = new LlmFailureClassifier();

    @ParameterizedTest
    @MethodSource("failures")
    void classifiesProviderFailures(Throwable failure, LlmFailureCategory expected) {
        assertThat(classifier.classify(failure)).isEqualTo(expected);
    }

    private static Stream<Arguments> failures() {
        return Stream.of(
                Arguments.of(new IllegalStateException("context_length_exceeded: too many tokens"),
                        LlmFailureCategory.CONTEXT_OVERFLOW),
                Arguments.of(new IllegalStateException("HTTP 429 rate limit"),
                        LlmFailureCategory.RATE_LIMIT),
                Arguments.of(new IllegalStateException("HTTP 503 service unavailable"),
                        LlmFailureCategory.PROVIDER_OVERLOADED),
                Arguments.of(new IllegalStateException("wrapped", new HttpTimeoutException("request timed out")),
                        LlmFailureCategory.TIMEOUT),
                Arguments.of(new IllegalStateException("HTTP 400 invalid request"),
                        LlmFailureCategory.INVALID_REQUEST),
                Arguments.of(new IllegalStateException("HTTP 403 forbidden"),
                        LlmFailureCategory.AUTHENTICATION),
                Arguments.of(new IllegalStateException("HTTP 500 internal server error"),
                        LlmFailureCategory.UNKNOWN)
        );
    }
}
