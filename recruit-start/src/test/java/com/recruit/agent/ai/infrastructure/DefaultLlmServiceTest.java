package com.recruit.agent.ai.infrastructure;

import com.recruit.agent.ai.domain.LlmCallResult;
import com.recruit.agent.ai.domain.LlmFailureCategory;
import com.recruit.agent.ai.domain.LlmStructuredRequest;
import com.recruit.agent.ai.domain.ModelCallLog;
import com.recruit.agent.ai.domain.ModelCallStatus;
import com.recruit.agent.ai.domain.RawLlmClient;
import com.recruit.agent.trace.AgentTraceStatus;
import com.recruit.agent.trace.InMemoryAgentTraceSink;
import com.recruit.agent.trace.TraceContext;
import com.recruit.agent.trace.TraceContextHolder;
import com.recruit.agent.trace.UnifiedTraceRecorder;
import org.junit.jupiter.api.Test;

import java.net.http.HttpTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLlmServiceTest {

    @Test
    void parsesStructuredResponseAndSavesSuccessLog() {
        InMemoryModelCallLogRepository logs = new InMemoryModelCallLogRepository();
        DefaultLlmService service = new DefaultLlmService(
                new SequenceRawClient(result("{\"name\":\"Zhang San\",\"score\":0.91}")), logs);

        SampleResult result = service.structuredChat(request());

        assertThat(result.name()).isEqualTo("Zhang San");
        assertThat(result.score()).isEqualTo(0.91);
        assertThat(logs.findAll()).singleElement().satisfies(log -> {
            assertThat(log.status()).isEqualTo(ModelCallStatus.SUCCEEDED);
            assertThat(log.errorMessage()).isNull();
        });
    }

    @Test
    void redactsPromptsAndResponsesBeforeSavingModelLogs() {
        InMemoryModelCallLogRepository logs = new InMemoryModelCallLogRepository();
        DefaultLlmService service = new DefaultLlmService(
                new SequenceRawClient(result("{\"name\":\"13812345678\",\"score\":0.91}")), logs);
        LlmStructuredRequest<SampleResult> request = new LlmStructuredRequest<>(
                "TEST", "v1", "contact candidate@example.com",
                "Authorization: Bearer private-token", SampleResult.class);

        service.structuredChat(request);

        assertThat(logs.findAll()).singleElement().satisfies(log -> {
            assertThat(log.systemPrompt()).contains("[REDACTED_EMAIL]").doesNotContain("candidate@example.com");
            assertThat(log.userPrompt()).contains("[REDACTED_BEARER_TOKEN]").doesNotContain("private-token");
            assertThat(log.responseBody()).contains("[REDACTED_PHONE]").doesNotContain("13812345678");
        });
    }

    @Test
    void correlatesModelExecutionWithTheCurrentAgentTrace() {
        InMemoryAgentTraceSink traceSink = new InMemoryAgentTraceSink();
        DefaultLlmService service = new DefaultLlmService(
                new SequenceRawClient(result("{\"name\":\"ok\",\"score\":0.91}")),
                new InMemoryModelCallLogRepository(),
                1, 0, 0, ignored -> { }, () -> 0,
                new UnifiedTraceRecorder(traceSink));

        try (TraceContextHolder.Scope ignored = TraceContextHolder.open(
                new TraceContext("trace-llm", 91L, "generate-node"))) {
            service.structuredChat(request());
        }

        assertThat(traceSink.findByTraceId("trace-llm")).hasSize(2)
                .allSatisfy(event -> {
                    assertThat(event.conversationId()).isEqualTo(91L);
                    assertThat(event.details()).containsEntry("component", "MODEL")
                            .containsEntry("parentSpanId", "generate-node");
                });
        assertThat(traceSink.findByTraceId("trace-llm"))
                .extracting(event -> event.status())
                .containsExactly(AgentTraceStatus.STARTED, AgentTraceStatus.SUCCEEDED);
    }

    @Test
    void retriesTransientFailuresWithBoundedExponentialBackoffAndLogsEveryAttempt() {
        InMemoryModelCallLogRepository logs = new InMemoryModelCallLogRepository();
        List<Long> delays = new ArrayList<>();
        SequenceRawClient client = new SequenceRawClient(
                new IllegalStateException("HTTP 429 rate_limit_exceeded"),
                new IllegalStateException("HTTP 503 provider overloaded"),
                result("{\"name\":\"recovered\",\"score\":0.8}")
        );
        DefaultLlmService service = new DefaultLlmService(
                client, logs, 3, 100, 1_000, delays::add, () -> 0.0);

        SampleResult result = service.structuredChat(request());

        assertThat(result.name()).isEqualTo("recovered");
        assertThat(client.calls()).isEqualTo(3);
        assertThat(delays).containsExactly(100L, 200L);
        assertThat(logs.findAll()).hasSize(3);
        assertThat(failedCategories(logs)).containsExactlyInAnyOrder(
                LlmFailureCategory.RATE_LIMIT, LlmFailureCategory.PROVIDER_OVERLOADED);
    }

    @Test
    void capsTransientRetriesAndBackoff() {
        InMemoryModelCallLogRepository logs = new InMemoryModelCallLogRepository();
        List<Long> delays = new ArrayList<>();
        SequenceRawClient client = new SequenceRawClient(
                new HttpTimeoutException("request timed out"),
                new HttpTimeoutException("request timed out"),
                new HttpTimeoutException("request timed out"),
                result("{\"name\":\"must-not-run\",\"score\":1}")
        );
        DefaultLlmService service = new DefaultLlmService(
                client, logs, 3, 80, 100, delays::add, () -> 0.0);

        assertThatThrownBy(() -> service.structuredChat(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TIMEOUT");

        assertThat(client.calls()).isEqualTo(3);
        assertThat(delays).containsExactly(80L, 100L);
        assertThat(logs.findAll()).hasSize(3)
                .allSatisfy(log -> assertThat(log.errorMessage()).startsWith("[TIMEOUT]"));
    }

    @Test
    void doesNotRetryContextAuthenticationInvalidOrUnknownFailures() {
        for (RuntimeException failure : List.of(
                new IllegalStateException("maximum context length exceeded"),
                new IllegalStateException("HTTP 401 invalid api key"),
                new IllegalStateException("HTTP 400 invalid_request_error"),
                new IllegalStateException("HTTP 500 internal error")
        )) {
            InMemoryModelCallLogRepository logs = new InMemoryModelCallLogRepository();
            AtomicInteger sleeps = new AtomicInteger();
            SequenceRawClient client = new SequenceRawClient(failure, result("{\"name\":\"no\",\"score\":0}"));
            DefaultLlmService service = new DefaultLlmService(
                    client, logs, 3, 10, 100, ignored -> sleeps.incrementAndGet(), () -> 0.0);

            assertThatThrownBy(() -> service.structuredChat(request()))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(client.calls()).isEqualTo(1);
            assertThat(sleeps).hasValue(0);
            assertThat(logs.findAll()).hasSize(1);
        }
    }

    @Test
    void savesUnknownFailureWhenResponseCannotBeParsedWithoutRetrying() {
        InMemoryModelCallLogRepository logs = new InMemoryModelCallLogRepository();
        SequenceRawClient client = new SequenceRawClient(result("not json"));
        DefaultLlmService service = new DefaultLlmService(client, logs);

        assertThatThrownBy(() -> service.structuredChat(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UNKNOWN");

        assertThat(client.calls()).isEqualTo(1);
        assertThat(logs.findAll()).singleElement().satisfies(log -> {
            assertThat(log.status()).isEqualTo(ModelCallStatus.FAILED);
            assertThat(log.errorMessage()).startsWith("[UNKNOWN]");
        });
    }

    private List<LlmFailureCategory> failedCategories(InMemoryModelCallLogRepository logs) {
        return logs.findAll().stream()
                .filter(log -> log.status() == ModelCallStatus.FAILED)
                .map(ModelCallLog::errorMessage)
                .map(message -> LlmFailureCategory.valueOf(message.substring(1, message.indexOf(']'))))
                .toList();
    }

    private LlmStructuredRequest<SampleResult> request() {
        return new LlmStructuredRequest<>("TEST", "v1", "system", "user", SampleResult.class);
    }

    private static LlmCallResult result(String response) {
        return new LlmCallResult("test-model", response, 10, 20, 3);
    }

    private record SampleResult(String name, double score) {
    }

    private static class SequenceRawClient implements RawLlmClient {
        private final Queue<Object> outcomes;
        private int calls;

        private SequenceRawClient(Object... outcomes) {
            this.outcomes = new ArrayDeque<>(Arrays.asList(outcomes));
        }

        @Override
        public LlmCallResult structuredChat(String systemPrompt, String userPrompt) {
            calls++;
            Object outcome = outcomes.remove();
            if (outcome instanceof RuntimeException failure) throw failure;
            return (LlmCallResult) outcome;
        }

        @Override
        public String modelName() {
            return "test-model";
        }

        private int calls() {
            return calls;
        }
    }
}
